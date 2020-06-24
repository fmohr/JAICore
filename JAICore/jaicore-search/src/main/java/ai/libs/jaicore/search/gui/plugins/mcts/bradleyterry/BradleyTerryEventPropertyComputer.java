package ai.libs.jaicore.search.gui.plugins.mcts.bradleyterry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.api4.java.algorithm.events.IAlgorithmEvent;

import ai.libs.jaicore.graphvisualizer.events.recorder.property.AlgorithmEventPropertyComputer;
import ai.libs.jaicore.graphvisualizer.events.recorder.property.PropertyComputationFailedException;
import ai.libs.jaicore.graphvisualizer.plugin.nodeinfo.NodeInfoAlgorithmEventPropertyComputer;
import ai.libs.jaicore.search.algorithms.mdp.mcts.comparison.ObservationsUpdatedEvent;

public class BradleyTerryEventPropertyComputer implements AlgorithmEventPropertyComputer {

	public static final String UPDATE_PROPERTY_NAME = "bt_update";

	private NodeInfoAlgorithmEventPropertyComputer nodeInfoAlgorithmEventPropertyComputer;

	public BradleyTerryEventPropertyComputer(final NodeInfoAlgorithmEventPropertyComputer nodeInfoAlgorithmEventPropertyComputer) {
		this.nodeInfoAlgorithmEventPropertyComputer = nodeInfoAlgorithmEventPropertyComputer;
	}

	@Override
	public Object computeAlgorithmEventProperty(final IAlgorithmEvent algorithmEvent) throws PropertyComputationFailedException {
		if (algorithmEvent instanceof ObservationsUpdatedEvent) {
			ObservationsUpdatedEvent<?> btEvent = (ObservationsUpdatedEvent<?>) algorithmEvent;
			return new BradleyTerryUpdate(this.nodeInfoAlgorithmEventPropertyComputer.getIdOfNodeIfExistent(btEvent.getNode()), btEvent.getVisits(), btEvent.getWinsLeft(), btEvent.getWinsRight(), new ArrayList<>(btEvent.getScoresLeft()), new ArrayList<>(btEvent.getScoresRight()), btEvent.getpLeft(), btEvent.getpRight(), btEvent.getpLeftScaled(), btEvent.getpRightScaled());
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
