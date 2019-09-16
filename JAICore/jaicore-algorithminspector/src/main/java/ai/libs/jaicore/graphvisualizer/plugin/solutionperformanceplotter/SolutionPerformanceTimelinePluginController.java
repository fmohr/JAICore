package ai.libs.jaicore.graphvisualizer.plugin.solutionperformanceplotter;

import org.api4.java.algorithm.events.ScoredSolutionCandidateFoundEvent;
import org.api4.java.algorithm.events.serializable.PropertyProcessedAlgorithmEvent;
import org.api4.java.common.control.ILoggingCustomizable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.graphvisualizer.events.gui.GUIEvent;
import ai.libs.jaicore.graphvisualizer.plugin.ASimpleMVCPluginController;
import ai.libs.jaicore.graphvisualizer.plugin.controlbar.ResetEvent;
import ai.libs.jaicore.graphvisualizer.plugin.timeslider.GoToTimeStepEvent;

public class SolutionPerformanceTimelinePluginController extends ASimpleMVCPluginController<SolutionPerformanceTimelinePluginModel, SolutionPerformanceTimelinePluginView> implements ILoggingCustomizable {

	private Logger logger = LoggerFactory.getLogger(SolutionPerformanceTimelinePlugin.class);

	public SolutionPerformanceTimelinePluginController(final SolutionPerformanceTimelinePluginModel model, final SolutionPerformanceTimelinePluginView view) {
		super(model, view);
	}

	@Override
	public void handleGUIEvent(final GUIEvent guiEvent) {
		if (guiEvent instanceof ResetEvent || guiEvent instanceof GoToTimeStepEvent) {
			this.getModel().clear();
		}
	}

	@Override
	public void handleAlgorithmEventInternally(final PropertyProcessedAlgorithmEvent algorithmEvent) {
		if (algorithmEvent.correspondsToEventOfClass(ScoredSolutionCandidateFoundEvent.class)) {

			this.logger.debug("Received solution event {}", algorithmEvent);

			Object rawScoredSolutionCandidateInfo = algorithmEvent.getProperty(ScoredSolutionCandidateInfoAlgorithmEventPropertyComputer.SCORED_SOLUTION_CANDIDATE_INFO_PROPERTY_NAME);
			if (rawScoredSolutionCandidateInfo != null) {
				ScoredSolutionCandidateInfo scoredSolutionCandidateInfo = (ScoredSolutionCandidateInfo) rawScoredSolutionCandidateInfo;

				try {
					this.logger.debug("Adding solution found at timestamp {} to model and updating view.", algorithmEvent.getTimestampOfEvent());
					this.getModel().addEntry(algorithmEvent.getTimestampOfEvent(), this.parseScoreToDouble(scoredSolutionCandidateInfo.getScore()));
					this.logger.debug("Added solution to model.");
				} catch (NumberFormatException exception) {
					this.logger.warn("Received processed SolutionCandidateFoundEvent, but the score {} cannot be parsed to a double.", scoredSolutionCandidateInfo.getScore());
				}
			}
		}
	}

	private double parseScoreToDouble(final String score) throws NumberFormatException {
		return Double.parseDouble(score);
	}

	@Override
	public String getLoggerName() {
		return this.logger.getName();
	}

	@Override
	public void setLoggerName(final String name) {
		this.logger = LoggerFactory.getLogger(name);
	}

}
