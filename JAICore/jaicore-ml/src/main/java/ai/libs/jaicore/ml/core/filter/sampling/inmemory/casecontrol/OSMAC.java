package ai.libs.jaicore.ml.core.filter.sampling.inmemory.casecontrol;

import java.util.ArrayList;
import java.util.Random;

import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledInstance;

import ai.libs.jaicore.basic.sets.Pair;
import ai.libs.jaicore.ml.weka.dataset.WekaInstance;
import weka.classifiers.Classifier;
import weka.core.Instance;

public class OSMAC<I extends ILabeledInstance, D extends ILabeledDataset<I>> extends PilotEstimateSampling<I, D> {

	public OSMAC(final Random rand, final int preSampleSize, final D input) {
		super(input);
		this.rand = rand;
		this.preSampleSize = preSampleSize;
	}

	@Override
	protected ArrayList<Pair<I, Double>> calculateFinalInstanceBoundaries(final D instances, final Classifier pilotEstimator) {
		double boundaryOfCurrentInstance = 0.0;
		ArrayList<Pair<I, Double>> probabilityBoundaries = new ArrayList<>();
		ArrayList<Pair<I, Double>> instanceProbabilityBoundaries = new ArrayList<>();
		double sumOfDistributionLosses = 0;
		int vectorLength;
		double loss;
		for (I instance : instances) {
			Instance wekaInstance = ((WekaInstance) instance).getElement();
			vectorLength = 0;
			for (double dimensionLength : wekaInstance.toDoubleArray()) {
				vectorLength += dimensionLength;
			}
			try {
				loss = 1 - pilotEstimator.distributionForInstance(wekaInstance)[(int) wekaInstance.classValue()];
			} catch (Exception e) {
				loss = 1;
			}
			sumOfDistributionLosses += loss * vectorLength;
		}
		for (I instance : instances) {
			Instance wekaInstance = ((WekaInstance) instance).getElement();
			vectorLength = 0;
			for (double dimensionLength : wekaInstance.toDoubleArray()) {
				vectorLength += dimensionLength;
			}
			try {
				loss = 1 - pilotEstimator.distributionForInstance(wekaInstance)[(int) wekaInstance.classValue()];
			} catch (Exception e) {
				loss = 1;
			}
			boundaryOfCurrentInstance += loss * vectorLength / sumOfDistributionLosses;
			instanceProbabilityBoundaries.add(new Pair<I, Double>(instance, boundaryOfCurrentInstance));
		}
		int iterator = 0;
		for (I instance : instances) {
			probabilityBoundaries.add(new Pair<>(instance, instanceProbabilityBoundaries.get(iterator).getY()));
			iterator++;
		}
		return probabilityBoundaries;
	}
}
