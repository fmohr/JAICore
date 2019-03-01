package jaicore.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeoutTimer extends Timer {
	private final static Logger logger = LoggerFactory.getLogger(TimeoutTimer.class);
	private final static TimeoutTimer instance = new TimeoutTimer();
	private final List<TimeoutSubmitter> emittedSubmitters = new ArrayList<>();
	private final ConcurrentHashMap<Integer, TimerTask> tasks = new ConcurrentHashMap<>();

	private TimeoutTimer() {
		super(true);
	}

	public static TimeoutTimer getInstance() {
		return instance;
	}

	public TimeoutSubmitter getSubmitter() {
		TimeoutSubmitter submitter = new TimeoutSubmitter();
		return submitter;
	}

	@Override
	public String toString() {
		return this.tasks.toString();
	}

	public class TimeoutSubmitter {
		private TimeoutSubmitter() {
			synchronized (instance) {
				TimeoutTimer.this.emittedSubmitters.add(this);
			}
		}

		public int interruptMeAfterMS(final long delay) {
			logger.info("Scheduling interrupt for thread {} in {}ms", Thread.currentThread(), delay);
			return this.interruptThreadAfterMS(Thread.currentThread(), delay);
		}
		
		public int interruptMeAfterMS(final long delay, Runnable preInterruptionHook) {
			logger.info("Scheduling interrupt for thread {} in {}ms", Thread.currentThread(), delay);
			return this.interruptThreadAfterMS(Thread.currentThread(), delay, preInterruptionHook);
		}
		
		public int interruptThreadAfterMS(final Thread thread, final long delay) {
			return interruptThreadAfterMS(thread, delay, null);
		}

		public int interruptThreadAfterMS(final Thread thread, final long delay, Runnable preInterruptionHook) {
			return this.scheduleTask(new TimerTask() {
				@Override
				public void run() {
					final TimerTask thisTask = this;
					int taskId = tasks.keySet().stream().filter(id -> tasks.get(id) == thisTask).findFirst().get();
					logger.info("Job {} for interruption of {} after delay of {}ms triggered.", taskId, thread, delay);
					if (preInterruptionHook != null) {
						logger.debug("Job {} now invokes pre-interruption.", taskId);
						preInterruptionHook.run();
					}
					logger.debug("Job {} now interrupts thread {}", taskId, thread);
					thread.interrupt();
				}
			}, delay);
		}

		public int interruptJobAfterMS(final Future<?> job, final long delay) {
			return this.scheduleTask(new TimerTask() {

				@Override
				public void run() {
					logger.info("canceling job {} after delay {}", job, delay);
					job.cancel(true);
				}
			}, delay);
		}

		public void cancelTimeout(final int taskId) {
			if (!TimeoutTimer.this.tasks.containsKey(taskId)) {
				throw new IllegalArgumentException("Task with id " + taskId + " was not found.");
			}
			TimeoutTimer.this.tasks.get(taskId).cancel();
			TimeoutTimer.this.tasks.remove(taskId);
			logger.info("Timeout for {} has been canceled.", taskId);
		}

		private synchronized int scheduleTask(final TimerTask task, final long delay) {
			synchronized (instance) {
				if (!TimeoutTimer.this.emittedSubmitters.contains(this)) {
					throw new IllegalStateException("Cannot submit interrupt job to submitter " + this + " since it has already been closed!");
				}
				TimeoutTimer.this.schedule(task, delay);

				/* create id for job and return it */
				int id;
				do {
					id = (int) (Math.random() * 1000000);
				} while (TimeoutTimer.this.tasks.containsKey(id));
				TimeoutTimer.this.tasks.put(id, task);
				logger.info("Job {} scheduled for in {}ms.", id, delay);
				return id;
			}
		}

		public void close() {
			synchronized (instance) {
				TimeoutTimer.this.emittedSubmitters.remove(this);
				logger.info("Canceled timer");
			}
		}
	}
}
