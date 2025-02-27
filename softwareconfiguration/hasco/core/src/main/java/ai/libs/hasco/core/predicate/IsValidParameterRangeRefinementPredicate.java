package ai.libs.hasco.core.predicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.math3.geometry.euclidean.oned.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.hasco.core.HASCOUtil;
import ai.libs.jaicore.basic.sets.SetUtil;
import ai.libs.jaicore.components.api.IComponent;
import ai.libs.jaicore.components.api.INumericParameterRefinementConfiguration;
import ai.libs.jaicore.components.api.INumericParameterRefinementConfigurationMap;
import ai.libs.jaicore.components.api.IParameter;
import ai.libs.jaicore.components.api.IParameterDomain;
import ai.libs.jaicore.components.model.CategoricalParameterDomain;
import ai.libs.jaicore.components.model.ComponentInstance;
import ai.libs.jaicore.components.model.NumericParameterDomain;
import ai.libs.jaicore.logic.fol.structure.ConstantParam;
import ai.libs.jaicore.logic.fol.structure.Literal;
import ai.libs.jaicore.logic.fol.structure.Monom;
import ai.libs.jaicore.logic.fol.theories.EvaluablePredicate;
import ai.libs.jaicore.logic.fol.util.LogicUtil;

public class IsValidParameterRangeRefinementPredicate implements EvaluablePredicate {

	private final Logger logger = LoggerFactory.getLogger(IsValidParameterRangeRefinementPredicate.class);

	private final Collection<IComponent> components;
	private final INumericParameterRefinementConfigurationMap refinementConfiguration;
	private final Map<ComponentInstance, Double> knownCompositionsAndTheirScore = new HashMap<>();

	public IsValidParameterRangeRefinementPredicate(final Collection<? extends IComponent> components, final INumericParameterRefinementConfigurationMap refinementConfiguration) {
		super();
		this.components = new ArrayList<>(components);
		this.refinementConfiguration = refinementConfiguration;
	}

	@Override
	public Collection<List<ConstantParam>> getParamsForPositiveEvaluation(final Monom state, final ConstantParam... partialGrounding) {

		this.logger.info("Computing params that evaluate isValidParameterRangeRefinement positively in state with hash code {}.", state.hashCode());

		/* determine the context for which the interval refinement should be oracled */
		if (partialGrounding.length != 6) {
			throw new IllegalArgumentException("The interpreted predicate " + this.getClass().getName() + " requires 6 arguments when oracled but " + partialGrounding.length + " have been provided!");
		}
		String componentName = partialGrounding[0].getName();
		String componentIdentifier = partialGrounding[1].getName();
		String parameterName = partialGrounding[2].getName();

		IComponent component;
		Optional<IComponent> searchedComponent = this.components.stream().filter(c -> c.getName().equals(componentName)).findAny();
		if (searchedComponent.isPresent()) {
			component = searchedComponent.get();
		} else {
			throw new IllegalArgumentException("Could not find matching component.");
		}

		Optional<IParameter> optParam = component.getParameters().stream().filter(p -> p.getName().equals(parameterName)).findAny();
		IParameter param;
		if (optParam.isPresent()) {
			param = optParam.get();
		} else {
			throw new IllegalArgumentException("Could not find required parameter");
		}

		List<ConstantParam> partialGroundingAsList = Arrays.asList(partialGrounding);
		String containerName = partialGrounding[3].getName();
		String currentParamValue = partialGrounding[4].getName(); // this is not really used, because the current value is again read from the state
		this.logger.info("Determining positive evaluations for isValidParameterRangeRefinementPredicate({},{},{},{},{},{})", componentName, componentIdentifier, parameterName, containerName, currentParamValue, partialGrounding[5]);
		boolean hasBeenSetBefore = state.contains(new Literal("overwritten('" + containerName + "')"));

		/* determine component instance and the true domain of parameter */
		ComponentInstance instance = HASCOUtil.getComponentInstanceFromState(this.components, state, componentIdentifier, false);
		this.logger.debug("Derived component instance to be refined: {}. Parameter to refine: {}. Current value of parameter: {}", instance, param, currentParamValue);
		try {
			Map<IParameter, IParameterDomain> paramDomains = HASCOUtil.getUpdatedDomainsOfComponentParameters(instance);
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Parameter domains are: {}", paramDomains.keySet().stream().map(k -> "\n\t" + k + ": " + paramDomains.get(k)).collect(Collectors.joining()));
			}

			/* determine refinements for numeric parameters */
			if (param.isNumeric()) {
				NumericParameterDomain currentlyActiveDomain = (NumericParameterDomain) paramDomains.get(param);
				Interval currentInterval = new Interval(currentlyActiveDomain.getMin(), currentlyActiveDomain.getMax());
				assert (!hasBeenSetBefore || (currentInterval.getInf() == Double.valueOf(SetUtil.unserializeList(currentParamValue).get(0)) && currentInterval.getSup() == Double
						.valueOf(SetUtil.unserializeList(currentParamValue).get(1)))) : "The derived currently active domain of an explicitly set parameter deviates from the domain specified in the state!";
				INumericParameterRefinementConfiguration refinementConfig = this.refinementConfiguration.getRefinement(component, param);
				if (refinementConfig == null) {
					throw new IllegalArgumentException("No refinement configuration for parameter \"" + parameterName + "\" of component \"" + componentName + "\" has been supplied!");
				}
				if (refinementConfig.getIntervalLength() <= 0) {
					throw new IllegalArgumentException("The interval length configured for param " + parameterName + " of component " + componentName + " must be strictly positive but is " + refinementConfig.getIntervalLength());
				}

				/* if the interval is under the distinction threshold, return an empty list of possible refinements (predicate will always be false here) */
				double relativeLength = (currentInterval.getSup() / currentInterval.getInf() - 1);
				double absoluteLength = currentInterval.getSup() - currentInterval.getInf();
				boolean isAtomicInterval = refinementConfig.isInitRefinementOnLogScale() && relativeLength <= refinementConfig.getIntervalLength()
						|| !refinementConfig.isInitRefinementOnLogScale() && absoluteLength <= refinementConfig.getIntervalLength();
				if (isAtomicInterval) {
					this.logger.info("Returning an empty list as this is a numeric parameter that has been narrowed sufficiently. Required interval length is {}, and actual interval length is {}", refinementConfig.getIntervalLength(),
							currentInterval.getSup() - currentInterval.getInf());
					if (!hasBeenSetBefore) {
						List<Interval> unmodifiedRefinement = new ArrayList<>();
						unmodifiedRefinement.add(currentInterval);
						return this.getGroundingsForIntervals(unmodifiedRefinement, partialGroundingAsList);
					}
					return new ArrayList<>();
				} else {
					this.logger.debug("Current interval [{},{}] is not considered atomic. Relative length is {}, and absolute length is {}", currentInterval.getInf(), currentInterval.getSup(), relativeLength, absoluteLength);
				}

				/* if this is an integer and the number of comprised integers are at most as many as the branching factor, enumerate them */
				if (currentlyActiveDomain.isInteger() && (Math.floor(currentInterval.getSup()) - Math.ceil(currentInterval.getInf()) + 1 <= refinementConfig.getRefinementsPerStep())) {
					List<Interval> proposedRefinements = new ArrayList<>();
					for (int i = (int) Math.ceil(currentInterval.getInf()); i <= (int) Math.floor(currentInterval.getSup()); i++) {
						proposedRefinements.add(new Interval(i, i));
					}
					this.logger.info("Ultimate level of integer refinement reached. Returning refinements: {}.", proposedRefinements.stream().map(i -> "[" + i.getInf() + ", " + i.getSup() + "]").collect(Collectors.toList()));
					return this.getGroundingsForIntervals(proposedRefinements, partialGroundingAsList);
				}

				/* if this parameter is to be refined on a linear scale, enter this block */
				if (hasBeenSetBefore || !refinementConfig.isInitRefinementOnLogScale()) {
					List<Interval> proposedRefinements = this.refineOnLinearScale(currentInterval, refinementConfig.getRefinementsPerStep(), refinementConfig.getIntervalLength(), refinementConfig.isInitRefinementOnLogScale(),
							refinementConfig.isInitWithExtremalPoints() && !hasBeenSetBefore);
					for (Interval proposedRefinement : proposedRefinements) {
						assert proposedRefinement.getInf() >= currentInterval.getInf() && proposedRefinement.getSup() <= currentInterval.getSup() : "The proposed refinement [" + proposedRefinement.getInf() + ", "
								+ proposedRefinement.getSup() + "] is not a sub-interval of " + currentParamValue + "].";
						assert !hasBeenSetBefore || !proposedRefinement.equals(currentInterval) : "No real refinement of parameter " + parameterName + " of component " + componentName + " with min interval size " + refinementConfig.getIntervalLength() + "! Intervals [" + currentInterval.getInf() + ", " + currentInterval.getSup() + "] and [" + proposedRefinement.getInf() + ", " + proposedRefinement.getSup() + "] are identical. State description: " + LogicUtil.getSortedLiteralSetDescription(state);
					}
					this.logger.info("Returning linear refinements: {}.", proposedRefinements.stream().map(i -> "[" + i.getInf() + ", " + i.getSup() + "]").collect(Collectors.toList()));
					return this.getGroundingsForIntervals(proposedRefinements, partialGroundingAsList);
				}

				/* if this is a log-scale parameter, compute the focus value and the other intervals */
				Optional<Literal> focusPredicate = state.stream()
						.filter(l -> l.getPropertyName().equals("parameterFocus") && l.getParameters().get(0).getName().equals(componentIdentifier) && l.getParameters().get(1).getName().equals(parameterName)).findAny();
				if (!focusPredicate.isPresent()) {
					throw new IllegalArgumentException("The given state does not specify a parameter focus for the log-scale parameter " + parameterName + " on object \"" + componentIdentifier + "\"");
				}
				double focus = Double.parseDouble(focusPredicate.get().getParameters().get(2).getName());
				if (refinementConfig.getLogBasis() <= 1) {
					throw new UnsupportedOperationException(
							"The basis for log-scaled parameter " + param.getName() + " of component " + instance.getComponent().getName() + " must be strictly greater than 1 (but is " + refinementConfig.getLogBasis() + ").");
				}
				List<Interval> proposedRefinements = this.refineOnLogScale(currentInterval, refinementConfig.getRefinementsPerStep(), refinementConfig.getLogBasis(), focus, refinementConfig.isInitWithExtremalPoints() && !hasBeenSetBefore);
				for (Interval proposedRefinement : proposedRefinements) {
					double epsilon = 1E-7;
					assert proposedRefinement.getInf() + epsilon >= currentInterval.getInf() && proposedRefinement.getSup() <= currentInterval.getSup() + epsilon : "The proposed refinement [" + proposedRefinement.getInf() + ", "
							+ proposedRefinement.getSup() + "] is not a sub-interval of " + currentParamValue + "].";
					assert !proposedRefinement.equals(currentInterval) : "No real refinement! Intervals are identical.";
				}
				this.logger.info("Returning log-scale refinements with focus point {}: {}.", focus, proposedRefinements.stream().map(i -> "[" + i.getInf() + ", " + i.getSup() + "]").collect(Collectors.toList()));
				return this.getGroundingsForIntervals(proposedRefinements, partialGroundingAsList);
			} else if (param.isCategorical()) {
				List<String> possibleValues = new ArrayList<>();
				if (hasBeenSetBefore) {
					this.logger.info("Returning empty list since param has been set before.");
					return new ArrayList<>();
				}
				for (Object valAsObject : ((CategoricalParameterDomain) paramDomains.get(param)).getValues()) {
					possibleValues.add(valAsObject.toString());
				}
				this.logger.info("Returning possible values {}.", possibleValues);
				return this.getGroundingsForOracledValues(possibleValues, partialGroundingAsList);
			} else {
				throw new UnsupportedOperationException("Currently no support for parameters of class \"" + param.getClass().getName() + "\"");
			}
		} catch (Exception e) {
			this.logger.error("Unexpected exception observed", e);
		}
		return new ArrayList<>();
	}

	private Collection<List<ConstantParam>> getGroundingsForIntervals(final List<Interval> refinements, final List<ConstantParam> partialGrounding) {
		List<String> paramValues = new ArrayList<>();
		for (Interval oracledInterval : refinements) {
			paramValues.add("[" + oracledInterval.getInf() + ", " + oracledInterval.getSup() + "]");
		}
		return this.getGroundingsForOracledValues(paramValues, partialGrounding);
	}

	private Collection<List<ConstantParam>> getGroundingsForOracledValues(final List<String> refinements, final List<ConstantParam> partialGrounding) {
		Collection<List<ConstantParam>> groundings = new ArrayList<>();
		for (String oracledValue : refinements) {
			List<ConstantParam> grounding = new ArrayList<>(partialGrounding);
			grounding.set(5, new ConstantParam(oracledValue));
			groundings.add(grounding);
		}
		return groundings;
	}

	public void informAboutNewSolution(final ComponentInstance solution, final double score) {
		this.knownCompositionsAndTheirScore.put(solution, score);
	}

	@Override
	public boolean isOracable() {
		return true;
	}

	@Override
	public Collection<List<ConstantParam>> getParamsForNegativeEvaluation(final Monom state, final ConstantParam... partialGrounding) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean test(final Monom state, final ConstantParam... params) {
		throw new NotImplementedException("Testing the validity-predicate is currently not supported. This is indirectly possible using the oracle.");
	}

	public List<Interval> refineOnLinearScale(final Interval interval, final int maxNumberOfSubIntervals, final double minimumLengthOfIntervals, final boolean wasInitiallyLogarithmic, final boolean createPointIntervalsForExtremalValues) {
		double min = interval.getInf();
		double max = interval.getSup();
		double length = max - min;
		double logLength = max / min - 1;
		double relevantLength = wasInitiallyLogarithmic ? logLength : length;
		List<Interval> intervals = new ArrayList<>();
		this.logger.debug("Refining interval [{}, {}] in a linear fashion. Was initially refined on log-scale: {}", min, max, wasInitiallyLogarithmic);

		/* if no refinement is possible, return just the interval itself */
		if (relevantLength <= minimumLengthOfIntervals) {
			intervals.add(interval);
			if (createPointIntervalsForExtremalValues) {
				intervals.add(0, new Interval(min, min));
				intervals.add(new Interval(max, max));
			}
			return intervals;
		}
		/* otherwise compute the sub-intervals */
		int numberOfIntervals = Math.min((int) Math.ceil(relevantLength / minimumLengthOfIntervals), maxNumberOfSubIntervals);
		if (createPointIntervalsForExtremalValues) {
			numberOfIntervals -= 2;
		}
		numberOfIntervals = Math.max(numberOfIntervals, 1);
		this.logger.trace("Splitting interval of length {} and log-length {} into {} sub-intervals.", length, logLength, numberOfIntervals);
		double stepSize = length / numberOfIntervals;
		for (int i = 0; i < numberOfIntervals; i++) {
			intervals.add(new Interval(Math.max(min, min + i * stepSize), Math.min(max, min + ((i + 1) * stepSize))));
		}
		if (createPointIntervalsForExtremalValues) {
			intervals.add(0, new Interval(min, min));
			intervals.add(new Interval(max, max));
		}
		this.logger.trace("Derived sub-intervals {}", intervals.stream().map(i -> "[" + i.getInf() + ", " + i.getSup() + "]").collect(Collectors.toList()));
		return intervals;
	}

	public List<Interval> refineOnLogScale(final Interval interval, final int numSubIntervals, final double basis, final double pointOfConcentration, final boolean createPointIntervalsForExtremalValues) {
		List<Interval> list = new ArrayList<>();
		double min = interval.getInf();
		double max = interval.getSup();
		this.logger.debug("Received call to create {} log-scaled sub-intervals for interval [{},{}] to the basis {}.", numSubIntervals, min, max, basis);
		double length = max - min;

		/* if the point of concentration is exactly on the left or the right of the interval, conduct the standard technique */
		if (pointOfConcentration <= min || pointOfConcentration >= max) {
			int numOfGeneratedSubIntervals = numSubIntervals;
			if (createPointIntervalsForExtremalValues) {
				numOfGeneratedSubIntervals -= 2;
			}
			if (numOfGeneratedSubIntervals <= 0) {
				throw new IllegalArgumentException("Number of created sub-intervals must be strictly positive but is " + numOfGeneratedSubIntervals + ".");
			}
			double lengthOfShortestInterval = length * (1 - basis) / (1 - Math.pow(basis, numOfGeneratedSubIntervals));
			while (lengthOfShortestInterval < 1.0E-10) {
				this.logger.trace("Initial interval would have size {} for a total number of {} sub-intervals, but length must be at least 10^-10. Reducing the number by 1.", lengthOfShortestInterval, numOfGeneratedSubIntervals);
				numOfGeneratedSubIntervals--;
				lengthOfShortestInterval = length * (1 - basis) / (1 - Math.pow(basis, numOfGeneratedSubIntervals));
			}

			this.logger.debug("Generating {} log-scaled sub-intervals for interval [{},{}] to the basis {}. Length of shortest interval is {}", numOfGeneratedSubIntervals, min, max, basis, lengthOfShortestInterval);
			if (pointOfConcentration <= min) {
				double endOfLast = min;
				for (int i = 0; i < numOfGeneratedSubIntervals; i++) {
					double start = endOfLast;
					assert start >= min;
					endOfLast = start + Math.pow(basis, i) * lengthOfShortestInterval;
					assert endOfLast <= max : "Sub-Interval must not assume values greater than a vaule of the original interval.";
					if (endOfLast <= start) {
						throw new IllegalArgumentException("Interval size for [" + start + ", " + (start + Math.pow(basis, i) * lengthOfShortestInterval) + "] is not positive.");
					}
					list.add(new Interval(start, endOfLast));
					this.logger.trace("Added interval [{}, {}]", start, endOfLast);
				}
			} else {
				double endOfLast = max;
				for (int i = 0; i < numOfGeneratedSubIntervals; i++) {
					double start = endOfLast;
					endOfLast = start - Math.pow(basis, i) * lengthOfShortestInterval;
					list.add(new Interval(endOfLast, start));
				}
				Collections.reverse(list);
			}
			if (createPointIntervalsForExtremalValues) {
				list.add(0, new Interval(min, min));
				list.add(new Interval(max, max));
			}
			return list;
		}

		/* if the point of concentration is in the inner of the interval, split the interval correspondingly and recursively solve the problem */
		double distanceFromMinToFocus = Math.abs(interval.getInf() - pointOfConcentration);
		int segmentsForLeft = (int) Math.max(1, Math.floor(numSubIntervals * distanceFromMinToFocus / length));
		if (createPointIntervalsForExtremalValues) {
			segmentsForLeft += 2;
		}
		int segmentsForRight = numSubIntervals - segmentsForLeft;
		assert segmentsForRight >= 1;
		if (!createPointIntervalsForExtremalValues || segmentsForRight < 3) {
			throw new IllegalArgumentException("No refinement possible if interval points are not included or segments for the right are less than 3");
		}
		this.logger.debug("Focus {} is inside the given interval. Create two partitions, one on the left ({} segments), and one on the right ({} segments).", pointOfConcentration, segmentsForLeft, segmentsForRight);
		list.addAll(this.refineOnLogScale(new Interval(min, pointOfConcentration), segmentsForLeft, basis, pointOfConcentration, createPointIntervalsForExtremalValues));
		list.addAll(this.refineOnLogScale(new Interval(pointOfConcentration, max), segmentsForRight, basis, pointOfConcentration, createPointIntervalsForExtremalValues));
		return list;
	}
}
