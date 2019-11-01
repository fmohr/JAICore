package ai.libs.mlplan.multiclass.wekamlplan;

import org.api4.java.ai.ml.core.learner.ISupervisedLearner;

import ai.libs.hasco.optimizingfactory.BaseFactory;

public interface ILearnerFactory<L extends ISupervisedLearner<?, ?>> extends BaseFactory<L> {

}
