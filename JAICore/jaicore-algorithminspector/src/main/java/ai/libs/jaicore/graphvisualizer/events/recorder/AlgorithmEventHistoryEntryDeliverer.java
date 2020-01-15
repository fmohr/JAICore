package ai.libs.jaicore.graphvisualizer.events.recorder;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.api4.java.algorithm.events.serializable.IPropertyProcessedAlgorithmEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.graphvisualizer.events.graph.bus.AlgorithmEventListener;
import ai.libs.jaicore.graphvisualizer.events.graph.bus.HandleAlgorithmEventException;
import ai.libs.jaicore.graphvisualizer.events.gui.GUIEvent;
import ai.libs.jaicore.graphvisualizer.events.gui.GUIEventListener;
import ai.libs.jaicore.graphvisualizer.events.recorder.property.PropertyProcessedAlgorithmEventListener;
import ai.libs.jaicore.graphvisualizer.events.recorder.property.PropertyProcessedAlgorithmEventSource;
import ai.libs.jaicore.graphvisualizer.plugin.controlbar.PauseEvent;
import ai.libs.jaicore.graphvisualizer.plugin.controlbar.PlayEvent;
import ai.libs.jaicore.graphvisualizer.plugin.controlbar.ResetEvent;
import ai.libs.jaicore.graphvisualizer.plugin.speedslider.ChangeSpeedEvent;
import ai.libs.jaicore.graphvisualizer.plugin.timeslider.GoToTimeStepEvent;

/**
 * The {@link AlgorithmEventHistoryEntryDeliverer} is {@link Thread} constantly pulling events from a given {@link AlgorithmEventHistory} and sending these to all registered
 * {@link AlgorithmEventListener}s.
 *
 * @author atornede
 *
 */
public class AlgorithmEventHistoryEntryDeliverer extends Thread implements PropertyProcessedAlgorithmEventSource, GUIEventListener {

	private Logger logger = LoggerFactory.getLogger(AlgorithmEventHistoryEntryDeliverer.class);

	private Set<PropertyProcessedAlgorithmEventListener> algorithmEventListeners;
	private AlgorithmEventHistory eventHistory;
	private int maximumSleepTimeInMilliseconds;

	private int timestep;
	private boolean paused;
	private double sleepTimeMultiplier;

	/**
	 * Creates a new {@link AlgorithmEventHistoryEntryDeliverer} with the given {@link AlgorithmEventHistory} and the maximum sleep time between checking for new events from the history and sending them to the registered listeners.
	 *
	 * @param eventHistory The {@link AlgorithmEventHistory} from which the events are pulled.
	 * @param maximumSleepTimeInMilliseconds The maximum sleep time between checking for new events from the history and sending them to the registered listeners
	 */
	public AlgorithmEventHistoryEntryDeliverer(final AlgorithmEventHistory eventHistory, final int maximumSleepTimeInMilliseconds) {
		this.eventHistory = eventHistory;
		this.maximumSleepTimeInMilliseconds = maximumSleepTimeInMilliseconds;

		this.timestep = 0;
		this.paused = true;
		this.algorithmEventListeners = ConcurrentHashMap.newKeySet();
		this.sleepTimeMultiplier = 1;
		this.setDaemon(true);
		this.logger.info("{} started with thread {}", this.getClass().getSimpleName(), this.getName());
	}

	/**
	 * Creates a new {@link AlgorithmEventHistoryEntryDeliverer} with the given {@link AlgorithmEventHistory}.
	 *
	 * @param eventHistory The {@link AlgorithmEventHistory} from which the events are pulled.
	 */
	public AlgorithmEventHistoryEntryDeliverer(final AlgorithmEventHistory eventHistory) {
		this(eventHistory, 30);
	}

	@Override
	public void registerListener(final PropertyProcessedAlgorithmEventListener algorithmEventListener) {
		this.algorithmEventListeners.add(algorithmEventListener);
	}

	@Override
	public void unregisterListener(final PropertyProcessedAlgorithmEventListener algorithmEventListener) {
		this.algorithmEventListeners.remove(algorithmEventListener);
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			if (!this.paused && this.timestep < this.eventHistory.getLength()) {
				AlgorithmEventHistoryEntry historyEntry = this.eventHistory.getEntryAtTimeStep(this.timestep);
				IPropertyProcessedAlgorithmEvent algorithmEvent = historyEntry.getAlgorithmEvent();
				this.logger.debug("Pulled event entry {} associated with event {} at position {}.", historyEntry, algorithmEvent, this.timestep);

				this.sendAlgorithmEventToListeners(algorithmEvent);
				this.timestep++;
			} else if (this.paused) {
				this.logger.debug("Not processing events since visualization is paused.");
			} else if (this.timestep >= this.eventHistory.getLength()) {
				this.logger.debug("Not processing events since no unpublished events are known.");
			}

			this.goToSleep();
		}
	}

	private void goToSleep() {
		try {
			int sleepTime = (int) (this.sleepTimeMultiplier * this.maximumSleepTimeInMilliseconds);
			this.logger.trace("Sleeping {}ms.", sleepTime);
			sleep(sleepTime);
		} catch (InterruptedException e) {
			this.logger.info("{} was interrupted due to exception: {}.", this.getClass().getSimpleName(), e);
			Thread.currentThread().interrupt();
		}
	}

	private void sendAlgorithmEventToListeners(final IPropertyProcessedAlgorithmEvent algorithmEvent) {
		for (PropertyProcessedAlgorithmEventListener eventListener : this.algorithmEventListeners) {
			try {
				this.sendAlgorithmEventToListener(algorithmEvent, eventListener);
			} catch (Exception e) {
				this.logger.error("Error in dispatching event {} due to error.", algorithmEvent, e);
			}
		}
		this.logger.info("Pulled and sent event {} as entry at time step {}.", algorithmEvent, this.timestep);
	}

	private void sendAlgorithmEventToListener(final IPropertyProcessedAlgorithmEvent algorithmEvent, final PropertyProcessedAlgorithmEventListener eventListener) throws HandleAlgorithmEventException {
		this.logger.debug("Sending event {} to listener {}.", algorithmEvent, eventListener);

		long startTime = System.currentTimeMillis();
		eventListener.handleSerializableAlgorithmEvent(algorithmEvent);
		long dispatchTime = System.currentTimeMillis() - startTime;

		if (dispatchTime > 10) {
			this.logger.warn("Dispatch time for event {} to listener {} took {}ms!", algorithmEvent, eventListener, dispatchTime);
		}
	}

	@Override
	public void handleGUIEvent(final GUIEvent guiEvent) {
		if (guiEvent instanceof PauseEvent) {
			this.pause();
		} else if (guiEvent instanceof PlayEvent) {
			this.unpause();
		} else if (guiEvent instanceof ResetEvent) {
			this.handleResetEvent();
		} else if (guiEvent instanceof GoToTimeStepEvent) {
			this.handleGoToTimeStepEvent(guiEvent);
		} else if (guiEvent instanceof ChangeSpeedEvent) {
			this.handleChangeSpeedEvent(guiEvent);
		}
	}

	private void pause() {
		this.paused = true;
	}

	private void unpause() {
		this.paused = false;
	}

	private void handleResetEvent() {
		this.resetTimeStep();
		this.pause();
	}

	private void resetTimeStep() {
		this.timestep = 0;
	}

	private void handleGoToTimeStepEvent(final GUIEvent guiEvent) {
		this.resetTimeStep();
		GoToTimeStepEvent goToTimeStepEvent = (GoToTimeStepEvent) guiEvent;
		while (this.timestep < goToTimeStepEvent.getNewTimeStep() && this.timestep < this.eventHistory.getLength()) {
			IPropertyProcessedAlgorithmEvent algorithmEvent = this.eventHistory.getEntryAtTimeStep(this.timestep).getAlgorithmEvent();
			this.sendAlgorithmEventToListeners(algorithmEvent);
			this.timestep++;
		}
	}

	private void handleChangeSpeedEvent(final GUIEvent guiEvent) {
		ChangeSpeedEvent changeSpeedEvent = (ChangeSpeedEvent) guiEvent;
		this.sleepTimeMultiplier = 1 - changeSpeedEvent.getNewSpeedPercentage() / 100.0;
	}

}
