package ai.libs.jaicore.timing;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.api4.java.algorithm.Timeout;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.concurrent.GlobalTimer;
import ai.libs.jaicore.interrupt.Interrupter;
import ai.libs.jaicore.interrupt.InterruptionTimerTask;
import ai.libs.jaicore.logging.LoggerUtil;

/**
 * This class is the single-thread pendant to asynchronous computations realized with Futures
 *
 * @author fmohr
 *
 */
public abstract class TimedComputation {

	private static final Logger logger = LoggerFactory.getLogger(TimedComputation.class);

	private TimedComputation() {
		/* no explicit instantiation allowed */
	}

	public static <T> T compute(final Callable<T> callable, final Timeout timeout, final String reasonToLogOnTimeout) throws ExecutionException, AlgorithmTimeoutedException, InterruptedException {

		/* schedule a timer that will interrupt the current thread and execute the task */
		GlobalTimer timer = GlobalTimer.getInstance();
		long start = System.currentTimeMillis();
		InterruptionTimerTask task = new InterruptionTimerTask("Timeout for timed computation with thread " + Thread.currentThread() + " at timestamp " + start + ": " + reasonToLogOnTimeout);
		logger.debug("Scheduling timer for interruption in {}ms with reason {}, i.e. timestamp {}.", timeout.milliseconds(), start + timeout.milliseconds(), reasonToLogOnTimeout);
		timer.schedule(task, timeout.milliseconds());
		Interrupter interrupter = Interrupter.get();
		logger.debug("Acquired interrupter {}.", interrupter);
		T output = null;
		Exception caughtException = null;
		try {
			logger.debug("Starting supervised computation of {}.", callable);
			output = callable.call();
		} catch (Exception e) {
			caughtException = e;
		} finally {
			task.cancel();
		}

		/* several circumstances define the state at this point:
		 * 1. the Callable has exited successfully or not
		 * 2. the thread is currently not interrupted or interrupted internally or externally
		 * 3. the time elapsed since calling the Callable is lower than the timeout or exceeds it
		 *
		 */
		int runtime = (int) (System.currentTimeMillis() - start);
		int delay = runtime - (int) timeout.milliseconds();
		boolean isInterrupted = Thread.currentThread().isInterrupted();
		if (caughtException != null) {
			logger.info("Timed computation has returned control after {}ms, i.e., with a delay of {}ms. Observed exception: {}. Thread interrupt flag is {}.", runtime, delay, caughtException.getClass().getName(), isInterrupted);
			if ((caughtException instanceof InterruptedException) && isInterrupted && interrupter.getAllUnresolvedInterruptsOfThread(Thread.currentThread()).size() == 1) { // only the reason belonging to our task is on the stack
				logger.warn("Timed computation has thrown an InterruptedException AND the thread is interrupted AND there are no other open interrupts on the thread! This should never happen! Here is the stack trace: \n\t{}",
						LoggerUtil.getExceptionInfo(caughtException));
			}
		} else {
			logger.info("Timed computation has returned control after {}ms, i.e., with a delay of {}ms. Observed regular output return value: {}. Thread interrupt flag is {}.", runtime, delay, output, isInterrupted);
		}

		/* now make sure that
		 * a) the timeoutTriggered flag is true iff the TimerTask for the timeout has been executed
		 * b) the interrupt-flag of the thread is true iff there has been another open (or untracked) interrupt
		 * */
		boolean timeoutTriggered = false;
		synchronized (interrupter) {

			/* if the timeout has been triggered (with caution) */
			logger.debug("Checking for an interruption and resolving potential interrupts.");
			if (interrupter.hasCurrentThreadBeenInterruptedWithReason(task)) {
				logger.info("Thread has been interrupted internally. Resolving the interrupt (this may throw an InterruptedException).");
				timeoutTriggered = true;
				Thread.interrupted(); // clear the interrupted field
				Interrupter.get().markInterruptOnCurrentThreadAsResolved(task);
			}

			/* otherwise, if the thread has been interrupted directly and not as a consequence of a shutdown, forward the interrupt */
			else if (task.isTriggered()) {
				interrupter.avoidInterrupt(Thread.currentThread(), task);
				logger.info("Interrupt is external, black-listed \"{}\" for interrupts on {} and re-throwing the exception.", task, Thread.currentThread());
			}
			assert !interrupter.hasCurrentThreadBeenInterruptedWithReason(task);
		}

		/* if there has been an exception, throw it if it is not the InterruptedException caused by the timeout (in this case, throw an AlgorithmTimeoutedException) */
		if (caughtException != null) {
			if (timeoutTriggered) {
				logger.info("Throwing TimeoutException");
				throw new AlgorithmTimeoutedException(delay);
			} else if (caughtException instanceof InterruptedException) {
				logger.debug("An InterruptedException was thrown during the timed execution: {}. Re-throwing it. Interrupt-flag is {}.", caughtException, Thread.currentThread().isInterrupted());
				throw (InterruptedException) caughtException;
			} else {
				logger.debug("Now re-throwing {}, which was caught in timed computation. Interrupt-flag is {}.", caughtException, Thread.currentThread().isInterrupted());
				throw new ExecutionException(caughtException);
			}
		}

		/* if no exception has been thrown, return the received output. Maybe the thread is interrupted, but this is then not due to our timeout */
		logger.debug("Finished timed computation of {} after {}ms where {}ms were allowed. Interrupt-flag is {}", callable, runtime, timeout, Thread.currentThread().isInterrupted());
		return output;
	}

	public static void computeWithTimeout(final Timeout timeout, final Runnable runnable) throws InterruptedException {
		Semaphore sync = new Semaphore(0);
		Thread t = new Thread(() -> {
			try {
				runnable.run();
			} catch (Exception e) {
				logger.info("Caught exception in timed computation thread", e);
			} finally {
				sync.release();
			}
		});
		t.start();

		try {
			if (timeout.milliseconds() > 0) {
				logger.info("Wait for a timeout of {}ms.", timeout.milliseconds());
				if (!sync.tryAcquire(timeout.milliseconds(), TimeUnit.MILLISECONDS)) {
					t.interrupt();
				}
			} else {
				logger.info("No timeout set, thus wait until the process is finished.");
				sync.acquire();
			}
		} catch (InterruptedException e) {
			logger.info("TimedComputation got interrupted or timeout has fired, thus interrupt nested thread.");
			t.interrupt();
			throw e;
		}
	}

	public static void computeWithTimeoutInParallel(final int numCPUs, final Timeout timeout, final List<Runnable> taskList) throws InterruptedException {
		ExecutorService pool = Executors.newFixedThreadPool(numCPUs);
		Semaphore sync = new Semaphore(0);
		taskList.stream().forEach(x -> pool.submit(() -> {
			try {
				x.run();
			} finally {
				sync.release();
			}
		}));
		pool.shutdown();

		try

		{
			logger.info("Wait for a timeout of {}ms.", timeout.milliseconds());
			if (timeout.milliseconds() > 0) {
				if (!sync.tryAcquire(taskList.size(), timeout.milliseconds(), TimeUnit.MILLISECONDS)) {
					logger.info("Timeout fired, shutdown pool right now.");
					pool.shutdownNow();
				}
			} else {
				sync.acquire(taskList.size());
			}
		} catch (InterruptedException e) {
			logger.info("TimedComputation got interrupted or timeout has fired, thus interrupt nested thread.");
			pool.shutdownNow();
			throw e;
		}
	}
}
