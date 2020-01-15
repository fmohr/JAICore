package ai.libs.hasco.gui.statsplugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.UnparametrizedComponentInstance;
import ai.libs.jaicore.basic.sets.Pair;
import ai.libs.jaicore.graphvisualizer.plugin.solutionperformanceplotter.ScoredSolutionCandidateInfo;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TreeItem;

/**
 * @author fmohr
 *
 *         This class represents a logical entry of the tree.
 *
 *         It holds a listener for its combo box that updates the sub-tree and the histogram if a filter is set.
 */
public class HASCOModelStatisticsComponentSelector extends TreeItem<HASCOModelStatisticsComponentSelector> {

	private static final Logger logger = LoggerFactory.getLogger(HASCOModelStatisticsComponentSelector.class);
	private final HASCOModelStatisticsComponentSelector parent;
	private final String requiredInterface;
	private final ComboBox<String> componentSelector;
	private final HASCOModelStatisticsPluginModel model;

	public HASCOModelStatisticsComponentSelector(final HASCOModelStatisticsPluginView rootView, final HASCOModelStatisticsPluginModel model) {
		this(rootView, null, null, model);
	}

	public HASCOModelStatisticsComponentSelector(final HASCOModelStatisticsPluginView rootView, final HASCOModelStatisticsComponentSelector parent, final String requiredInterface, final HASCOModelStatisticsPluginModel model) {
		this.parent = parent;
		this.requiredInterface = requiredInterface;
		this.model = model;
		this.componentSelector = new ComboBox<>();
		this.componentSelector.getItems().add("*");
		this.componentSelector.setValue("*");
		this.componentSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
			HASCOModelStatisticsComponentSelector.this.getChildren().clear();
			if (!newValue.equals("*")) {
				Map<String, String> requiredInterfacesOfThisChoice = model.getKnownComponents().get(newValue).getRequiredInterfaces();
				for (String requiredInterfaceId : requiredInterfacesOfThisChoice.keySet()) {
					HASCOModelStatisticsComponentSelector.this.getChildren().add(new HASCOModelStatisticsComponentSelector(rootView, HASCOModelStatisticsComponentSelector.this, requiredInterfaceId, model));
				}
			}
			rootView.updateHistogram();
		});
		this.update();
		this.setValue(this); // set the value to itself. This is necessary so that the cell factory really retrieves this object as the node
		this.setExpanded(true);
	}

	/**
	 * This recursively updates the whole tree view under this node with respect to the current selections.
	 *
	 * This method is currently not too efficient, because it always iterates over all solutions, but it is still fast enough.
	 */
	public void update() {
		long start = System.currentTimeMillis();
		List<Pair<String, String>> selectionPath = this.getSelectionsOnPathToRoot();
		List<String> reqInterfacePath = selectionPath.stream().map(Pair::getX).collect(Collectors.toList());
		reqInterfacePath.remove(0); // this is null and only needed as a selector in the selectionPath
		ObservableList<String> items = this.componentSelector.getItems();
		for (ScoredSolutionCandidateInfo scoredSolutionCandidateInfo : this.model.getAllSeenSolutionCandidateFoundInfosUnordered()) {
			ComponentInstance ci = this.model.deserializeComponentInstance(scoredSolutionCandidateInfo.getSolutionCandidateRepresentation());
			if (!ci.matchesPathRestriction(selectionPath)) {
				continue;
			}

			/* determine sub-component relevant for this path and add the respective component lexicographically correctly (unless it is already in the list) */
			UnparametrizedComponentInstance uci = new UnparametrizedComponentInstance(ci).getSubComposition(reqInterfacePath);
			if (this.componentSelector.getItems().contains(uci.getComponentName())) {
				continue;
			}
			logger.trace("Relevant UCI of {} for path {} is {}", ci, reqInterfacePath, uci);
			int n = items.size();
			String nameOfNewComponent = uci.getComponentName();
			for (int i = 0; i <= n; i++) {
				if (i == n || items.get(i).compareTo(nameOfNewComponent) >= 0) {
					items.add(i, nameOfNewComponent);
					break;
				}
			}
		}
		this.getChildren().forEach(ti -> ti.getValue().update());
		long duration = System.currentTimeMillis() - start;
		logger.debug("Update of {} took {}ms", this, duration);
	}

	/**
	 * Resets the combo box to the wild-card and removes all child nodes.
	 */
	public void clear() {
		this.componentSelector.getItems().removeIf(s -> !s.equals("*"));
		this.getChildren().clear();
	}

	/**
	 * Gets the choices made in the combo boxes on the path from the root to here. The first entry has a null-key just saying what the choice for the root component has been.
	 *
	 * @return List of choices.
	 */
	public List<Pair<String, String>> getSelectionsOnPathToRoot() {
		List<Pair<String, String>> path = this.parent != null ? this.parent.getSelectionsOnPathToRoot() : new ArrayList<>();
		path.add(new Pair<>(this.requiredInterface, this.componentSelector.getValue()));
		return path;
	}

	/**
	 * Determines the set of all selection paths from here to a any leaf. For the root node, this is the set of constraints specified in the combo boxes.
	 *
	 * @return Collection of paths to leafs.
	 */
	public Collection<List<Pair<String, String>>> getAllSelectionsOnPathToAnyLeaf() {
		Collection<List<Pair<String, String>>> subPaths = new ArrayList<>();
		if (this.getChildren().isEmpty()) {
			List<Pair<String, String>> leafRestriction = new ArrayList<>();
			leafRestriction.add(new Pair<>(this.requiredInterface, this.componentSelector.getValue()));
			subPaths.add(leafRestriction);
			return subPaths;
		}
		for (TreeItem<HASCOModelStatisticsComponentSelector> child : this.getChildren()) {
			subPaths.addAll(child.getValue().getAllSelectionsOnPathToAnyLeaf());
		}
		return subPaths.stream().map(p -> {
			p.add(0, new Pair<>(this.requiredInterface, this.componentSelector.getValue()));
			return p;
		}).collect(Collectors.toList());
	}

	public String getRequiredInterface() {
		return this.requiredInterface;
	}

	public ComboBox<String> getComponentSelector() {
		return this.componentSelector;
	}

	@Override
	public String toString() {
		return "HASCOModelStatisticsComponentSelector";
	}
}
