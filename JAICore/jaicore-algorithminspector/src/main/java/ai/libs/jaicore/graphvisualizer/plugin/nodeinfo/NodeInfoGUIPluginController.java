package ai.libs.jaicore.graphvisualizer.plugin.nodeinfo;

import org.api4.java.algorithm.events.serializable.IPropertyProcessedAlgorithmEvent;

import ai.libs.jaicore.graphvisualizer.events.graph.bus.HandleAlgorithmEventException;
import ai.libs.jaicore.graphvisualizer.events.gui.GUIEvent;
import ai.libs.jaicore.graphvisualizer.plugin.ASimpleMVCPluginController;
import ai.libs.jaicore.graphvisualizer.plugin.graphview.NodeClickedEvent;

public class NodeInfoGUIPluginController extends ASimpleMVCPluginController<NodeInfoGUIPluginModel, NodeInfoGUIPluginView> {

	private NodeInfoGenerator<?> infoGenerator;

	public NodeInfoGUIPluginController(final NodeInfoGUIPluginModel model, final NodeInfoGUIPluginView view) {
		super(model, view);
	}

	@Override
	public void handleAlgorithmEventInternally(final IPropertyProcessedAlgorithmEvent algorithmEvent) throws HandleAlgorithmEventException {
		Object rawNodeDisplayInfoProperty = algorithmEvent.getProperty(NodeDisplayInfoAlgorithmEventPropertyComputer.NODE_DISPLAY_INFO_PROPERTY_NAME + "_" + this.infoGenerator.getName());
		Object rawNodeInfoProperty = algorithmEvent.getProperty(NodeInfoAlgorithmEventPropertyComputer.NODE_INFO_PROPERTY_NAME);
		if (rawNodeDisplayInfoProperty != null && rawNodeInfoProperty != null) {
			NodeInfo nodeInfo = (NodeInfo) rawNodeInfoProperty;
			String nodeInfoText = (String) rawNodeDisplayInfoProperty;
			this.getModel().addNodeIdToNodeInfoMapping(nodeInfo.getMainNodeId(), nodeInfoText);
		}
	}

	@Override
	public void handleGUIEvent(final GUIEvent guiEvent) {
		if (guiEvent instanceof NodeClickedEvent) {
			NodeClickedEvent nodeClickedEvent = (NodeClickedEvent) guiEvent;
			String searchGraphNodeCorrespondingToClickedViewGraphNode = nodeClickedEvent.getSearchGraphNode();
			this.getModel().setCurrentlySelectedNode(searchGraphNodeCorrespondingToClickedViewGraphNode);
		}
	}

	public NodeInfoGenerator<?> getInfoGenerator() {
		return this.infoGenerator;
	}

	public void setInfoGenerator(final NodeInfoGenerator<?> infoGenerator) {
		this.infoGenerator = infoGenerator;
	}
}
