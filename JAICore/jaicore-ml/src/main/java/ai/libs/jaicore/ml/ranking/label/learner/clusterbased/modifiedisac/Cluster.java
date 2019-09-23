package ai.libs.jaicore.ml.ranking.label.learner.clusterbased.modifiedisac;

import java.util.List;

import ai.libs.jaicore.ml.ranking.label.learner.clusterbased.customdatatypes.Group;
import ai.libs.jaicore.ml.ranking.label.learner.clusterbased.customdatatypes.GroupIdentifier;
import ai.libs.jaicore.ml.ranking.label.learner.clusterbased.customdatatypes.ProblemInstance;
import weka.core.Instance;

public class Cluster extends Group<double[], Instance> {

	/**  Saves a cluster in two components. First, a list of the elements in the cluster
	 * 	 here in form of list of problem instnaces. Second, the identifier of the cluster
	 * 	 in form of the cluster center as a point.
	 * @param instanlist
	 * @param id
	 */
	Cluster(final List<ProblemInstance<Instance>> instanlist, final GroupIdentifier<double[]> id) {
		super(instanlist, id);
	}
}
