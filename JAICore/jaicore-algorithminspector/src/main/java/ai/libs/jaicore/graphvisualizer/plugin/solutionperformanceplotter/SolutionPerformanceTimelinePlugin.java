package ai.libs.jaicore.graphvisualizer.plugin.solutionperformanceplotter;

import java.util.Arrays;
import java.util.Collection;

import ai.libs.jaicore.graphvisualizer.events.recorder.property.AlgorithmEventPropertyComputer;
import ai.libs.jaicore.graphvisualizer.plugin.ASimpleMVCPlugin;

public class SolutionPerformanceTimelinePlugin extends ASimpleMVCPlugin<SolutionPerformanceTimelinePluginModel, SolutionPerformanceTimelinePluginView, SolutionPerformanceTimelinePluginController> {

	private final SolutionCandidateRepresenter solutionRepresenter;

	public SolutionPerformanceTimelinePlugin(final SolutionCandidateRepresenter solutionRepresenter) {
		this("Solution Performance Timeline", solutionRepresenter);
	}

	public SolutionPerformanceTimelinePlugin(final String title, final SolutionCandidateRepresenter solutionRepresenter) {
		super(title);
		this.solutionRepresenter = solutionRepresenter;
	}

	@Override
	public Collection<AlgorithmEventPropertyComputer> getPropertyComputers() {
		return Arrays.asList(new ScoredSolutionCandidateInfoAlgorithmEventPropertyComputer(this.solutionRepresenter));
	}

}
