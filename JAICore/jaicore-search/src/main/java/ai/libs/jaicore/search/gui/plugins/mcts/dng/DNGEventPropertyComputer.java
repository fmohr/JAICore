package ai.libs.jaicore.search.gui.plugins.mcts.dng;

import java.util.Arrays;
import java.util.List;

import org.api4.java.algorithm.events.IAlgorithmEvent;

import ai.libs.jaicore.graphvisualizer.events.recorder.property.AlgorithmEventPropertyComputer;
import ai.libs.jaicore.graphvisualizer.events.recorder.property.PropertyComputationFailedException;
import ai.libs.jaicore.graphvisualizer.plugin.nodeinfo.NodeInfoAlgorithmEventPropertyComputer;
import ai.libs.jaicore.search.algorithms.mdp.mcts.thompson.DNGBeliefUpdateEvent;
import ai.libs.jaicore.search.algorithms.mdp.mcts.thompson.DNGQSampleEvent;

public class DNGEventPropertyComputer implements AlgorithmEventPropertyComputer {

	public static final String UPDATE_PROPERTY_NAME = "dng_update";

	private NodeInfoAlgorithmEventPropertyComputer nodeInfoAlgorithmEventPropertyComputer;

	public DNGEventPropertyComputer(final NodeInfoAlgorithmEventPropertyComputer nodeInfoAlgorithmEventPropertyComputer) {
		this.nodeInfoAlgorithmEventPropertyComputer = nodeInfoAlgorithmEventPropertyComputer;
	}

	@Override
	public Object computeAlgorithmEventProperty(final IAlgorithmEvent algorithmEvent) throws PropertyComputationFailedException {
		if (algorithmEvent instanceof DNGQSampleEvent) {
			DNGQSampleEvent<?,?> dngEvent = (DNGQSampleEvent<?,?>) algorithmEvent;
			String idOfNode = this.nodeInfoAlgorithmEventPropertyComputer.getIdOfNodeIfExistent(dngEvent.getNode());
			return new DNGQSample(idOfNode, dngEvent.getAction().toString(), dngEvent.getScore());
		}
		if (algorithmEvent instanceof DNGBeliefUpdateEvent) {
			DNGBeliefUpdateEvent<?> dngEvent = (DNGBeliefUpdateEvent<?>) algorithmEvent;
			String idOfNode = this.nodeInfoAlgorithmEventPropertyComputer.getIdOfNodeIfExistent(dngEvent.getNode());
			return new DNGBeliefUpdate(idOfNode, dngEvent.getMu(), dngEvent.getAlpha(), dngEvent.getBeta(), dngEvent.getLambda());
		}
		return null;
	}

	@Override
	public String getPropertyName() {
		return UPDATE_PROPERTY_NAME;
	}

	@Override
	public List<AlgorithmEventPropertyComputer> getRequiredPropertyComputers() {
		return Arrays.asList(this.nodeInfoAlgorithmEventPropertyComputer);
	}

	@Override
	public void overwriteRequiredPropertyComputer(final AlgorithmEventPropertyComputer computer) {
		this.nodeInfoAlgorithmEventPropertyComputer = (NodeInfoAlgorithmEventPropertyComputer)computer;
	}

}
