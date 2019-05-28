package autofe.algorithm.hasco.evaluation;

import java.util.Collection;

import autofe.algorithm.hasco.filter.meta.FilterPipeline;
import hasco.core.Util;
import hasco.exceptions.ComponentInstantiationFailedException;
import hasco.model.Component;
import hasco.model.ComponentInstance;
import hasco.optimizingfactory.BaseFactory;
import jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd.TFDNode;
import jaicore.search.algorithms.standard.bestfirst.exceptions.NodeEvaluationException;
import jaicore.search.algorithms.standard.bestfirst.nodeevaluation.INodeEvaluator;
import jaicore.search.model.travesaltree.Node;

public abstract class AbstractHASCOFENodeEvaluator extends AbstractHASCOFEEvaluator implements INodeEvaluator<TFDNode, Double> {

    private Collection<Component> components;
    private BaseFactory<FilterPipeline> factory;

    // Maximum size of a pipeline
    protected int maxPipelineSize;

    protected AbstractHASCOFENodeEvaluator(final int maxPipelineSize) {
        this.maxPipelineSize = maxPipelineSize;
    }

    public void setComponents(final Collection<Component> components) {
        this.components = components;
    }

    public Collection<Component> getComponents() {
        return components;
    }

    public void setFactory(final BaseFactory<FilterPipeline> factory) {
        this.factory = factory;
    }

    public BaseFactory<FilterPipeline> getFactory() {
        return factory;
    }

    FilterPipeline getPipelineFromNode(final Node<TFDNode, ?> node) throws ComponentInstantiationFailedException {
        if (components == null || factory == null) {
            throw new IllegalArgumentException("Collection of components and factory need to be set to make node evaluators work.");
        }

        ComponentInstance ci = Util.getSolutionCompositionFromState(getComponents(), node.getPoint().getState(), true);
        if (ci == null) {
            return null;
        }
        return getFactory().getComponentInstantiation(ci);
    }

    protected FilterPipeline extractPipelineFromNode(final Node<TFDNode, ?> node) throws NodeEvaluationException {
        FilterPipeline pipe;
        try {
            pipe = getPipelineFromNode(node);
        } catch (ComponentInstantiationFailedException e1) {
            throw new NodeEvaluationException(e1, "Could not evaluate pipeline.");
        }
        return pipe;
    }

    boolean hasNodeEmptyParent(final Node<TFDNode, ?> node) {
        return node.getParent() == null;
    }

    boolean hasPathExceededPipelineSize(final Node<TFDNode, ?> node) {
        return node.path().size() > maxPipelineSize;
    }
}
