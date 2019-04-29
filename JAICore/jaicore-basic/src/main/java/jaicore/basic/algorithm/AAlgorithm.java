package jaicore.basic.algorithm;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.aeonbits.owner.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import jaicore.basic.ILoggingCustomizable;
import jaicore.basic.TimeOut;
import jaicore.basic.algorithm.events.AlgorithmEvent;
import jaicore.basic.algorithm.events.AlgorithmFinishedEvent;
import jaicore.basic.algorithm.events.AlgorithmInitializedEvent;
import jaicore.basic.algorithm.exceptions.AlgorithmException;
import jaicore.basic.algorithm.exceptions.AlgorithmTimeoutedException;
import jaicore.concurrent.GlobalTimer;
import jaicore.concurrent.InterruptionTimerTask;
import jaicore.interrupt.Interrupt;
import jaicore.interrupt.Interrupter;

public abstract class AAlgorithm<I, O> implements IAlgorithm<I, O>, ILoggingCustomizable {

	/* Logger variables */
	private Logger logger = LoggerFactory.getLogger(AAlgorithm.class);
	private String loggerName;

	/* Parameters of the algorithm. */
	private IAlgorithmConfig config;

	/* Semantic input to the algorithm. */
	private final I input;

	/* State and event bus for sending algorithm events. */
	private long shutdownInitialized = -1; // timestamp for when the shutdown has been initialized
	private long activationTime = -1; // timestamp of algorithm activation

	private String id;
	private long deadline = -1; // timestamp when algorithm must terminate due to timeout
	private long timeOfTimeoutDetection = -1; // timestamp for when timeout has been triggered
	private long canceled = -1; // timestamp for when the algorithm has been canceled
	private final Set<Thread> activeThreads = new HashSet<>();
	private AlgorithmState state = AlgorithmState.created;
	private final EventBus eventBus = new EventBus();

	private int timeoutPrecautionOffset = 100; // this offset is substracted from the true remaining time whenever a timer is scheduled to ensure that the timeout is respected
	private static final int MIN_RUNTIME_FOR_OBSERVED_TASK = 50;

	private static final String INTERRUPT_NAME_SUFFIX = "-shutdown";

	/**
	 * C'tor providing the input for the algorithm already.
	 *
	 * @param input
	 *            The input for the algorithm.
	 */
	protected AAlgorithm(final I input) {
		this.input = input;
		this.config = ConfigFactory.create(IAlgorithmConfig.class);
	}

	/**
	 * Internal c'tore overwriting the internal configuration and setting the input.
	 *
	 * @param input
	 *            The input for the algorithm.
	 * @param config
	 *            The configuration to take as the internal configuration object.
	 */
	protected AAlgorithm(final IAlgorithmConfig config, final I input) {
		this.config = config;
		this.input = input;
		if (this.config == null)
			throw new IllegalArgumentException("Algorithm configuration must not be null!");
	}

	@Override
	public Iterator<AlgorithmEvent> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		return this.state != AlgorithmState.inactive;
	}

	@Override
	public AlgorithmEvent next() {
		try {
			return this.nextWithException();
		} catch (Exception e) {
			this.unregisterThreadAndShutdown();
			throw new ExceptionInAlgorithmIterationException(e);
		}
	}

	@Override
	public I getInput() {
		return this.input;
	}

	@Override
	public void registerListener(final Object listener) {
		this.eventBus.register(listener);
	}

	@Override
	public int getNumCPUs() {
		return this.getConfig().cpus();
	}

	@Override
	public void setNumCPUs(final int numberOfCPUs) {
		this.getConfig().setProperty(IAlgorithmConfig.K_CPUS, numberOfCPUs + "");
	}

	@Override
	public void setMaxNumThreads(final int maxNumberOfThreads) {
		this.getConfig().setProperty(IAlgorithmConfig.K_THREADS, maxNumberOfThreads + "");
	}

	@Override
	public void setTimeout(final long timeout, final TimeUnit timeUnit) {
		this.setTimeout(new TimeOut(timeout, timeUnit));
	}

	@Override
	public void setTimeout(final TimeOut timeout) {
		this.logger.info("Setting timeout to {}ms", timeout.milliseconds());
		this.getConfig().setProperty(IAlgorithmConfig.K_TIMEOUT, timeout.milliseconds() + "");
	}

	public int getTimeoutPrecautionOffset() {
		return this.timeoutPrecautionOffset;
	}

	public void setTimeoutPrecautionOffset(final int timeoutPrecautionOffset) {
		this.timeoutPrecautionOffset = timeoutPrecautionOffset;
	}

	@Override
	public TimeOut getTimeout() {
		return new TimeOut(this.getConfig().timeout(), TimeUnit.MILLISECONDS);
	}

	public boolean isTimeouted() {
		if (this.timeOfTimeoutDetection > 0) {
			return true;
		}
		if (this.deadline > 0 && System.currentTimeMillis() >= this.deadline) {
			this.timeOfTimeoutDetection = System.currentTimeMillis();
			return true;
		}
		return false;
	}

	protected TimeOut getRemainingTimeToDeadline() {
		if (this.deadline < 0) {
			return new TimeOut(Integer.MAX_VALUE, TimeUnit.SECONDS);
		}
		return new TimeOut(this.deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}

	public boolean isStopCriterionSatisfied() {
		return this.isCanceled() || this.isTimeouted() || Thread.currentThread().isInterrupted();
	}

	/**
	 * @return Flag denoting whether this algorithm has been canceled.
	 */
	public boolean isCanceled() {
		return this.canceled > 0;
	}

	@Override
	public String getId() {
		if (this.id == null) {
			this.id = this.getClass().getName() + "-" + System.currentTimeMillis();
		}
		return this.id;
	}

	protected void checkTermination(final boolean shutdownOnStoppingCriterion) throws InterruptedException, AlgorithmExecutionCanceledException, AlgorithmTimeoutedException {
		this.logger.debug("Checking Termination");
		Thread t = Thread.currentThread();

		/* check whether the thread has been interrupted for another reason than during shutdown */
		if (t.isInterrupted() && !this.hasThreadBeenInterruptedDuringShutdown(t)) {
			this.logger.info("Interruption detected for {}. Resetting interrupted-flag.", this.getId());
			Thread.interrupted(); // clear the interrupt-field. This is necessary, because otherwise some shutdown-activities (like waiting for pool shutdown) might fail
			if (shutdownOnStoppingCriterion) {
				this.logger.debug("Invoking shutdown");
				this.unregisterThreadAndShutdown();
			} else {
				this.logger.debug("Not shutting down, because shutdown-on-stop-criterion has been set to false");
			}
			this.logger.debug("Throwing InterruptedException to communicate the interrupt to the invoker.");
			throw new InterruptedException(); // if the thread itself was actively interrupted by somebody
		}

		if (this.isTimeouted()) {
			this.logger.info("Timeout detected for {}", this.getId());
			if (shutdownOnStoppingCriterion) {
				this.logger.debug("Invoking shutdown");
				this.unregisterThreadAndShutdown();
			} else {
				this.logger.debug("Not shutting down, because shutdown-on-stop-criterion has been set to false");
			}
			this.logger.debug("Throwing TimeoutException");
			throw new AlgorithmTimeoutedException(this.timeOfTimeoutDetection - this.deadline);
		}

		if (this.isCanceled()) { // for a cancel, we assume that the shutdown has already been triggered by the canceler
			this.logger.info("Cancel detected for {}.", this.getId());
			if (Thread.interrupted()) { // reset the flag
				this.logger.debug("Thread has been interrupted during shutdown. Resetting the flag and not invoking shutdown again.");
			}
			this.logger.debug("Throwing AlgorithmExecutionCanceledException.");
			throw new AlgorithmExecutionCanceledException(System.currentTimeMillis() - this.canceled);
		}
		this.logger.debug("No termination condition observed.");
	}

	protected void checkAndConductTermination() throws InterruptedException, AlgorithmExecutionCanceledException, AlgorithmTimeoutedException {
		this.checkTermination(true);
	}

	/**
	 * This method does two things:
	 * 1. it interrupts all threads that are registered to be active inside this algorithm
	 * 2. it cancels the (possibly created) timeout thread
	 *
	 * This method should be called ALWAYS when the algorithm activity ceases.
	 *
	 * This method takes effect only once. Further invocations will be ignored.
	 */
	protected void shutdown() {
		synchronized (this) {
			if (this.shutdownInitialized > 0) {
				this.logger.info("Tried to enter shutdown for {}, but the shutdown has already been initialized in the past, so exiting the shutdown block.", this);
				return;
			}
			this.shutdownInitialized = System.currentTimeMillis();
		}
		this.logger.info("Entering shutdown procedure for {}. Setting algorithm state from {} to inactive and interrupting {} active threads.", this.getId(), this.getState(), this.activeThreads.size());
		for (Thread t : this.activeThreads) {
			this.interruptThreadAsPartOfShutdown(t);
		}
		this.logger.info("Shutdown of {} completed.", this.getId());
	}

	protected void interruptThreadAsPartOfShutdown(final Thread t) {
		Interrupter.get().interruptThread(t, this.getId() + INTERRUPT_NAME_SUFFIX);
	}

	public boolean hasThreadBeenInterruptedDuringShutdown(final Thread t) {
		return Interrupter.get().hasThreadBeenInterruptedWithReason(t, this.getId() + INTERRUPT_NAME_SUFFIX);
	}

	protected void resolveShutdownInterruptOnCurrentThread() throws InterruptedException {
		Interrupter.get().markInterruptOnCurrentThreadAsResolved(this.getId() + INTERRUPT_NAME_SUFFIX);
	}

	public boolean isShutdownInitialized() {
		return this.shutdownInitialized > 0;
	}

	protected void unregisterThreadAndShutdown() {
		this.unregisterActiveThread();
		this.shutdown();
	}

	protected void registerActiveThread() {
		this.activeThreads.add(Thread.currentThread());
	}

	protected void unregisterActiveThread() {
		this.logger.trace("Unregistering current thread {}", Thread.currentThread());
		this.activeThreads.remove(Thread.currentThread());
	}

	public long getActivationTime() {
		return this.activationTime;
	}

	/**
	 * @return The current state of the algorithm.
	 */
	public AlgorithmState getState() {
		return this.state;
	}

	/**
	 * @param state
	 *            The new state of the algorithm.
	 */
	protected void setState(final AlgorithmState state) {
		if (state == AlgorithmState.active) {
			throw new IllegalArgumentException("Cannot switch state to active. Use \"activate\" instead, which will set the state to active and provide the AlgorithmInitializedEvent.");
		} else if (state == AlgorithmState.inactive) {
			throw new IllegalArgumentException("Cannot switch state to inactive. Use \"terminate\" instead, which will set the state to inactive and provide the AlgorithmFinishedEvent.");
		}
		this.state = state;
	}

	@Override
	public synchronized void cancel() {
		this.logger.info("Received cancel for algorithm {}.", this.getId());
		if (this.isCanceled()) {
			this.logger.debug("Ignoring cancel command since the algorithm has been canceled before.");
			return;
		}
		this.canceled = System.currentTimeMillis();
		if (this.isShutdownInitialized()) {
			this.logger.debug("Ignoring cancel command since the algorithm has already been shutdown before.");
			return;
		}
		this.logger.info("Executing cancel on {}. Have set the cancel flag and will now invoke shutdown procedure.", this.getId());
		this.shutdown();
	}

	/**
	 * This method
	 * - defines the definite deadline for when the algorithm must have finished
	 * - sets the algorithm state to ACTIVE
	 * - sends the mandatory AlgorithmInitializedEvent over the event bus.
	 * Should only be called once and as before the state is set to something else.
	 */
	protected AlgorithmInitializedEvent activate() {
		assert this.state == AlgorithmState.created : "Can only activate an algorithm as long as its state has not been changed from CREATED to something else. It is currently " + this.state;
		this.activationTime = System.currentTimeMillis();
		if (this.getTimeout().milliseconds() > 0) {
			this.deadline = this.activationTime + this.getTimeout().milliseconds();
		}
		this.state = AlgorithmState.active;
		AlgorithmInitializedEvent event = new AlgorithmInitializedEvent(this.getId());
		this.eventBus.post(event);
		this.logger.trace("Starting algorithm {} with problem {} and config {}", this.getId(), this.input, this.config);
		return event;
	}

	/**
	 * This methods terminates the algorithm, setting the internal state to inactive and emitting the mandatory AlgorithmFinishedEvent over the event bus.
	 *
	 * @return The algorithm finished event.
	 */
	protected AlgorithmFinishedEvent terminate() {
		this.logger.info("Terminating algorithm {}.", this.getId());
		this.state = AlgorithmState.inactive;
		AlgorithmFinishedEvent finishedEvent = new AlgorithmFinishedEvent(this.getId());
		this.unregisterThreadAndShutdown();
		this.eventBus.post(finishedEvent);
		return finishedEvent;
	}

	/**
	 * This methods allows for posting an event on the algorithm's event bus.
	 *
	 * @param e
	 *            The event to post on the event bus.
	 */
	protected void post(final Object e) {
		this.eventBus.post(e);
	}

	@Override
	public IAlgorithmConfig getConfig() {
		return this.config;
	}

	/**
	 * Sets the config object to the new config object.
	 *
	 * @param config
	 *            The new config object.
	 */
	public void setConfig(final IAlgorithmConfig config) {
		this.config = config;
	}

	@Override
	public void setLoggerName(final String name) {
		this.logger.info("Switching logger to {}", name);
		this.loggerName = name;
		this.logger = LoggerFactory.getLogger(name);
		this.logger.info("Switched to logger {}", name);
	}

	@Override
	public String getLoggerName() {
		return this.loggerName;
	}
	
	protected void announceTimeoutDetected() {
		this.timeOfTimeoutDetection = System.currentTimeMillis(); // artificially set the timeout detected variable
	}

	protected <T> T computeTimeoutAware(final Callable<T> r, final boolean shutdownOnStoppingCriterionSatisfied) throws InterruptedException, AlgorithmException, AlgorithmExecutionCanceledException, AlgorithmTimeoutedException {
		this.logger.debug("Received request to execute {} with awareness of timeout {}. Currently active threads: {}. Currently active tasks in global timer: {}", r, this.getTimeout(), activeThreads, GlobalTimer.getInstance().getActiveTasks());
		
		/* if no timeout is sharp, just execute the task */
		if (this.getTimeout().milliseconds() < 0) {
			try {
				return r.call();
			} catch (InterruptedException e) { // the fact that we are interrupted here can have several reasons. Could be an interrupt from the outside, a cancel, or a timeout by the above timer
				boolean interruptedDueToShutdown = this.hasThreadBeenInterruptedDuringShutdown(Thread.currentThread());
				this.logger.info("Received interrupt. Cancel flag is {}. Thread contained in interrupted by shutdown: {}", this.isCanceled(), interruptedDueToShutdown);
				if (!interruptedDueToShutdown) {
					throw e;
				}
				this.resolveShutdownInterruptOnCurrentThread();
				this.checkTermination(shutdownOnStoppingCriterionSatisfied);
				throw new IllegalStateException("Received an interrupt and checked termination, thus, termination routine should have thrown an exception which it apparently did not!");
			} catch (AlgorithmExecutionCanceledException e) { // these exceptions should just be forwarded
				throw e;
			} catch (Exception e) {
				throw new AlgorithmException(e, "The algorithm has failed due to an exception of a Callable.");
			}
		}

		/* if the remaining time is not sufficient to conduct further calculation, cancel at this point */
		long remainingTime = this.getRemainingTimeToDeadline().milliseconds();
		if (remainingTime < this.timeoutPrecautionOffset + MIN_RUNTIME_FOR_OBSERVED_TASK) {
			this.logger.debug("Only {}ms left, which is not enough to reliably continue computation. Terminating algorithm at this point, throwing an AlgorithmTimeoutedException.", remainingTime);
			announceTimeoutDetected();
			this.checkTermination(shutdownOnStoppingCriterionSatisfied);
		}

		/* schedule a timer that will interrupt the current thread and execute the task */
		long timeToInterrupt = remainingTime - this.timeoutPrecautionOffset;
		GlobalTimer timer = GlobalTimer.getInstance();
		TimerTask task = new InterruptionTimerTask("Timeout for timed execution of callable " + r,
				() -> this.logger.debug("Timeout detected at timestamp {}. This is  {} prior to deadline, interrupting successor generation.", System.currentTimeMillis(), this.getRemainingTimeToDeadline()));
		this.logger.debug("Scheduling timer for interruption in {}ms, i.e. timestamp {}. Remaining time to deadline: {}. List of active tasks is now: {}", timeToInterrupt, System.currentTimeMillis() + timeToInterrupt, this.getRemainingTimeToDeadline(), timer.getActiveTasks());
		timer.schedule(task, timeToInterrupt);
		try {
			this.logger.debug("Starting supervised computation of {}.", r);
			T result = r.call();
			task.cancel();
			return result;
		} catch (InterruptedException e) { // the fact that we are interrupted here can have several reasons. Could be an interrupt from the outside, a cancel, or a timeout by the above timer
			this.logger.info("Received interrupt for {}. Cancel flag is {}", this.getId(), this.isCanceled());

			/* if the timeout has been triggered (with caution) */
			remainingTime = this.getRemainingTimeToDeadline().milliseconds();
			if (Interrupter.get().hasCurrentThreadBeenInterruptedWithReason(task)) {
				this.logger.info("Interrupt is internal.");
				Thread.interrupted(); // clear the interrupted field
				Interrupter.get().markInterruptOnCurrentThreadAsResolved(task);
				if (remainingTime > 0) {
					this.logger.debug("Artificially sleeping {}ms to trigger the correct behavior in the checker.", remainingTime);
					this.timeOfTimeoutDetection = System.currentTimeMillis();
					this.checkTermination(shutdownOnStoppingCriterionSatisfied);
				} else {
					this.logger.debug("Gained back control from successor generation, but remaining time is now only {}ms. Algorithm should terminate now.", remainingTime);
				}
				assert !Interrupter.get().hasCurrentThreadBeenInterruptedWithReason(task);
			}

			/* otherwise, if the thread has been interrupted directly and not as a consequence of a shutdown, forward the interrupt */
			else {
				boolean interruptedDueToShutdown = this.hasThreadBeenInterruptedDuringShutdown(Thread.currentThread());
				Optional<Interrupt> latestInterrupt = Interrupter.get().getLatestUnresolvedInterruptOfCurrentThread();
				assert latestInterrupt.isPresent() : "No interrupt present!";
				this.logger.info("Interrupt is external (reason is {}). Cancel flag is now {}. Thread contained in interrupted by shutdown: {}", latestInterrupt.get().getReasonForInterruption(), this.isCanceled(), interruptedDueToShutdown);
				if (!interruptedDueToShutdown) {
					throw e;
				}
				this.resolveShutdownInterruptOnCurrentThread();
			}
			this.checkTermination(shutdownOnStoppingCriterionSatisfied);
			throw new IllegalStateException("termination routine should have thrown an exception!");
		} catch (Exception e) {
			throw new AlgorithmException(e, "The algorithm has failed due to an exception of a Callable.");
		} finally {
			task.cancel();
		}
	}
}
