package ai.libs.jaicore.ml;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.reflect.MethodUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;

import ai.libs.jaicore.basic.algorithm.AlgorithmExecutionCanceledException;
import ai.libs.jaicore.basic.algorithm.exceptions.AlgorithmTimeoutedException;
import ai.libs.jaicore.basic.sets.CartesianProductComputationProblem;
import ai.libs.jaicore.basic.sets.LDSRelationComputer;
import ai.libs.jaicore.ml.cache.ReproducibleInstances;
import ai.libs.jaicore.ml.cache.SplitInstruction;
import ai.libs.jaicore.ml.core.SimpleInstanceImpl;
import ai.libs.jaicore.ml.core.SimpleInstancesImpl;
import ai.libs.jaicore.ml.core.SimpleLabeledInstanceImpl;
import ai.libs.jaicore.ml.core.WekaCompatibleInstancesImpl;
import ai.libs.jaicore.ml.interfaces.LabeledInstance;
import ai.libs.jaicore.ml.interfaces.LabeledInstances;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.MultipleClassifiersCombiner;
import weka.classifiers.SingleClassifierEnhancer;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.InstanceComparator;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.json.JSONInstances;
import weka.core.json.JSONNode;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class WekaUtil {

	private WekaUtil() {
		/* avoid instantiation */
	}

	private static final String MSG_SUM1 = "Portions must sum up to at most 1.";
	private static final String MSG_DEVIATING_NUMBER_OF_INSTANCES = "The number of instances in the folds does not equal the number of instances in the original dataset";

	private static final String NAME_LABEL = "label";
	private static final String NAME_INSTANCES = "JAICore-extracted dataset";

	private static boolean debug = false;

	public static Collection<String> getBasicLearners() {
		Collection<String> classifiers = new ArrayList<>();

		classifiers.add("weka.classifiers.bayes.BayesNet");
		classifiers.add("weka.classifiers.bayes.NaiveBayes");
		classifiers.add("weka.classifiers.bayes.NaiveBayesMultinomial");
		classifiers.add("weka.classifiers.functions.Logistic");
		classifiers.add("weka.classifiers.functions.MultilayerPerceptron");
		classifiers.add("weka.classifiers.functions.SimpleLinearRegression");
		classifiers.add("weka.classifiers.functions.SimpleLogistic");
		classifiers.add("weka.classifiers.functions.SMO");
		classifiers.add("weka.classifiers.functions.VotedPerceptron");
		classifiers.add("weka.classifiers.lazy.IBk");
		classifiers.add("weka.classifiers.lazy.KStar");
		classifiers.add("weka.classifiers.rules.JRip");
		classifiers.add("weka.classifiers.rules.M5Rules");
		classifiers.add("weka.classifiers.rules.OneR");
		classifiers.add("weka.classifiers.rules.PART");
		classifiers.add("weka.classifiers.rules.ZeroR");
		classifiers.add("weka.classifiers.trees.DecisionStump");
		classifiers.add("weka.classifiers.trees.J48");
		classifiers.add("weka.classifiers.trees.LMT");
		classifiers.add("weka.classifiers.trees.M5P");
		classifiers.add("weka.classifiers.trees.RandomForest");
		classifiers.add("weka.classifiers.trees.RandomTree");
		classifiers.add("weka.classifiers.trees.REPTree");
		return classifiers;
	}

	public static Collection<String> getNativeMultiClassClassifiers() {
		Collection<String> classifiers = new ArrayList<>();

		classifiers.add("weka.classifiers.bayes.BayesNet");
		classifiers.add("weka.classifiers.bayes.NaiveBayes");
		classifiers.add("weka.classifiers.bayes.NaiveBayesMultinomial");
		classifiers.add("weka.classifiers.functions.Logistic");
		classifiers.add("weka.classifiers.functions.MultilayerPerceptron");
		classifiers.add("weka.classifiers.functions.SimpleLogistic");
		classifiers.add("weka.classifiers.lazy.IBk");
		classifiers.add("weka.classifiers.lazy.KStar");
		classifiers.add("weka.classifiers.rules.JRip");
		classifiers.add("weka.classifiers.rules.M5Rules");
		classifiers.add("weka.classifiers.rules.OneR");
		classifiers.add("weka.classifiers.rules.PART");
		classifiers.add("weka.classifiers.rules.ZeroR");
		classifiers.add("weka.classifiers.trees.DecisionStump");
		classifiers.add("weka.classifiers.trees.J48");
		classifiers.add("weka.classifiers.trees.LMT");
		classifiers.add("weka.classifiers.trees.M5P");
		classifiers.add("weka.classifiers.trees.RandomForest");
		classifiers.add("weka.classifiers.trees.RandomTree");
		classifiers.add("weka.classifiers.trees.REPTree");
		return classifiers;
	}

	public static Collection<String> getBinaryClassifiers() {
		Collection<String> classifiers = new ArrayList<>();
		classifiers.add("weka.classifiers.functions.SMO");
		classifiers.add("weka.classifiers.functions.VotedPerceptron");
		return classifiers;
	}

	public static Collection<String> getFeatureEvaluators() {
		Collection<String> preprocessors = new ArrayList<>();

		preprocessors.add("weka.attributeSelection.CfsSubsetEval");
		preprocessors.add("weka.attributeSelection.CorrelationAttributeEval");
		preprocessors.add("weka.attributeSelection.GainRatioAttributeEval");
		preprocessors.add("weka.attributeSelection.InfoGainAttributeEval");
		preprocessors.add("weka.attributeSelection.OneRAttributeEval");
		preprocessors.add("weka.attributeSelection.PrincipalComponents");
		preprocessors.add("weka.attributeSelection.ReliefFAttributeEval");
		preprocessors.add("weka.attributeSelection.SymmetricalUncertAttributeEval");
		return preprocessors;
	}

	public static Collection<String> getSearchers() {
		Collection<String> preprocessors = new ArrayList<>();

		preprocessors.add("weka.attributeSelection.Ranker");
		preprocessors.add("weka.attributeSelection.BestFirst");
		preprocessors.add("weka.attributeSelection.GreedyStepwise");
		return preprocessors;
	}

	public static Collection<String> getMetaLearners() {
		Collection<String> classifiers = new ArrayList<>();
		classifiers.add("weka.classifiers.meta.AdaBoostM1");
		classifiers.add("weka.classifiers.meta.AdditiveRegression");
		classifiers.add("weka.classifiers.meta.AttributeSelectedClassifier");
		classifiers.add("weka.classifiers.meta.Bagging");
		classifiers.add("weka.classifiers.meta.ClassificationViaRegression");
		classifiers.add("weka.classifiers.meta.LogitBoost");
		classifiers.add("weka.classifiers.meta.MultiClassClassifier");
		classifiers.add("weka.classifiers.meta.RandomCommittee");
		classifiers.add("weka.classifiers.meta.RandomSubspace");
		classifiers.add("weka.classifiers.meta.Stacking");
		classifiers.add("weka.classifiers.meta.Vote");
		return classifiers;
	}

	public static boolean isValidPreprocessorCombination(final String searcher, final String evaluator) {
		boolean isSetEvaluator = evaluator.toLowerCase().matches(".*(relief|gainratio|principalcomponents|onerattributeeval|infogainattributeeval|correlationattributeeval|symmetricaluncertattributeeval).*");
		boolean isRanker = searcher.toLowerCase().contains("ranker");
		boolean isNonRankerEvaluator = evaluator.toLowerCase().matches(".*(cfssubseteval).*");
		return !(isSetEvaluator && !isRanker || isNonRankerEvaluator && isRanker);
	}

	/**
	 * Determines all attribute selection variants (search/evaluator combinations with default parametrization)
	 *
	 * @return
	 * @throws AlgorithmExecutionCanceledException
	 * @throws InterruptedException
	 * @throws AlgorithmTimeoutedException
	 */
	public static Collection<List<String>> getAdmissibleSearcherEvaluatorCombinationsForAttributeSelection() throws AlgorithmTimeoutedException, InterruptedException, AlgorithmExecutionCanceledException {
		Collection<List<String>> preprocessors = new ArrayList<>();
		List<Collection<String>> sets = new ArrayList<>();
		sets.add(getSearchers());
		sets.add(getFeatureEvaluators());
		CartesianProductComputationProblem<String> problem = new CartesianProductComputationProblem<>(sets);
		List<List<String>> combinations = new LDSRelationComputer<>(problem).call();
		for (List<String> combo : combinations) {
			if (isValidPreprocessorCombination(combo.get(0), combo.get(1))) {
				preprocessors.add(combo);
			}
		}
		return preprocessors;
	}

	public static Instances fromJAICoreInstances(final WekaCompatibleInstancesImpl instances) {

		/* create basic attribute entries */
		ArrayList<Attribute> attributes = new ArrayList<>();
		int numAttributes = instances.getNumberOfColumns();
		for (int i = 1; i <= numAttributes; i++) {
			attributes.add(new Attribute("a" + i));
		}

		/*
		 * if the instances object is labeled, create the label entry and create a list
		 * of all the possible labels
		 */
		Map<Object, Double> labelMap = new HashMap<>();
		int c = 0;
		boolean isNominal = false;
		for (Object o : instances.getDeclaredClasses()) {
			labelMap.put(o, (double) (c++));
			if (!Double.class.isInstance(o)) {
				isNominal = true;
			}
		}

		/* if the feature is */
		if (isNominal) {
			attributes.add(new Attribute(NAME_LABEL, instances.getDeclaredClasses()));
		} else {
			attributes.add(new Attribute(NAME_LABEL));
		}

		/* create instances object and insert the data points */
		Instances wekaInstances = new Instances(NAME_INSTANCES, attributes, instances.size());
		wekaInstances.setClassIndex(numAttributes);
		for (ai.libs.jaicore.ml.interfaces.Instance instance : instances) {
			Instance wekaInstance = new DenseInstance(numAttributes + 1);
			wekaInstance.setDataset(wekaInstances);
			int att = 0;
			for (Double val : instance) {
				wekaInstance.setValue(att++, val);
			}
			wekaInstance.setClassValue(labelMap.get(((LabeledInstance<?>) instance).getLabel()));
			wekaInstances.add(wekaInstance);
		}
		return wekaInstances;
	}

	public static Instances fromJAICoreInstances(final ai.libs.jaicore.ml.interfaces.Instances<ai.libs.jaicore.ml.interfaces.Instance> instances) {

		/* create basic attribute entries */
		ArrayList<Attribute> attributes = new ArrayList<>();
		int numAttributes = instances.getNumberOfColumns();
		for (int i = 1; i <= numAttributes; i++) {
			attributes.add(new Attribute("a" + i));
		}

		/* create instances object and insert the data points */
		Instances wekaInstances = new Instances(NAME_INSTANCES, attributes, 0);
		for (ai.libs.jaicore.ml.interfaces.Instance instance : instances) {
			wekaInstances.add(fromJAICoreInstance(instance));
		}
		return wekaInstances;
	}

	public static Instances getEmptyDatasetForJAICoreInstance(final ai.libs.jaicore.ml.interfaces.Instance instance) {
		ArrayList<Attribute> attributes = new ArrayList<>();
		int numAttributes = instance.getNumberOfColumns();
		for (int i = 1; i <= numAttributes; i++) {
			attributes.add(new Attribute("a" + i));
		}
		return new Instances(NAME_INSTANCES, attributes, 0);
	}

	public static Instance fromJAICoreInstance(final ai.libs.jaicore.ml.interfaces.Instance instance) {
		Instances emptyDataset = getEmptyDatasetForJAICoreInstance(instance);
		Instance wekaInstance = new DenseInstance(instance.getNumberOfColumns());
		int att = 0;
		for (Double val : instance) {
			wekaInstance.setValue(att++, val);
		}
		emptyDataset.add(wekaInstance);
		return emptyDataset.iterator().next();
	}

	public static Instance fromJAICoreInstance(final LabeledInstance<String> instance) {

		/* create basic attribute entries */
		ArrayList<Attribute> attributes = new ArrayList<>();
		int numAttributes = instance.getNumberOfColumns();
		for (int i = 1; i <= numAttributes; i++) {
			attributes.add(new Attribute("a" + i));
		}
		List<String> possibleValues = new ArrayList<>();
		possibleValues.add(instance.getLabel());
		Attribute classAttribute = new Attribute(NAME_LABEL, possibleValues);
		attributes.add(classAttribute);
		Instances instances = new Instances(NAME_INSTANCES, attributes, 1);
		instances.setClassIndex(attributes.size() - 1);

		double[] values = new double[numAttributes + 1];
		for (int i = 0; i < numAttributes; i++) {
			values[i] = instance.get(i);
		}
		Instance inst = new DenseInstance(1., values);
		instances.add(inst);
		Instance addedInstance = instances.get(0);
		addedInstance.setClassValue(instance.getLabel());
		return addedInstance;
	}

	public static Instances fromJAICoreInstances(final LabeledInstances<String> labeledInstances) {
		int attributeCount = labeledInstances.getNumberOfColumns() + 1; // the amount of attributes including the class
		// label.
		int dataSize = labeledInstances.getNumberOfRows();

		/* create basic attribute entries */
		ArrayList<Attribute> attributeList = new ArrayList<>(attributeCount);
		for (int i = 1; i < attributeCount; i++) {
			attributeList.add(new Attribute("a" + i));
		}
		/* create class attribute */
		List<String> classes = labeledInstances.getOccurringLabels();
		Attribute classAttribute = new Attribute(NAME_LABEL, classes);

		attributeList.add(classAttribute);

		weka.core.Instances wekaInstances = new Instances(NAME_INSTANCES, attributeList, dataSize);
		wekaInstances.setClassIndex(wekaInstances.numAttributes() - 1); // the last item is the class attribute.

		for (ai.libs.jaicore.ml.interfaces.LabeledInstance<String> labeledInstance : labeledInstances) {
			double[] values = new double[attributeCount];
			for (int i = 0; i < attributeCount - 1; i++) {
				values[i] = labeledInstance.get(i);
			}
			weka.core.Instance wekaInstance = new DenseInstance(1.0, values);
			String label = labeledInstance.getLabel();

			wekaInstance.setDataset(wekaInstances);
			double classIndex = classAttribute.indexOfValue(label);
			wekaInstance.setClassValue(classIndex);
			wekaInstances.add(wekaInstance);
		}
		return wekaInstances;
	}

	public static WekaCompatibleInstancesImpl toJAICoreLabeledInstances(final Instances wekaInstances) {
		WekaCompatibleInstancesImpl labeledInstances = new WekaCompatibleInstancesImpl(getClassesDeclaredInDataset(wekaInstances));
		for (Instance inst : wekaInstances) {
			labeledInstances.add(toJAICoreLabeledInstance(inst));
		}
		return labeledInstances;
	}

	/**
	 * Returns true if there is at least one nominal attribute in the given dataset that has more than 2 values.
	 *
	 * @param wekaInstances
	 *            dataset that is checked
	 * @param ignoreClassAttribute
	 *            if true class attribute is ignored.
	 */
	public static boolean needsBinarization(final Instances wekaInstances, final boolean ignoreClassAttribute) {
		Attribute classAttribute = wekaInstances.classAttribute();
		if (!ignoreClassAttribute && classAttribute.isNominal() && classAttribute.numValues() >= 3) {
			return true;
		}
		// iterate over every attribute and check.
		for (Enumeration<Attribute> attributeEnum = wekaInstances.enumerateAttributes(); attributeEnum.hasMoreElements();) {
			Attribute currentAttr = attributeEnum.nextElement();
			if (!currentAttr.isNominal()) {
				continue; // ignore attributes that aren't nominal.
			}
			if (currentAttr == classAttribute) {
				// ignore class attribute (already checked in case ignoreClassAttribute==true):
				continue;
			}
			if (currentAttr.numValues() >= 3) {
				return true;
			}
		}
		return false;
	}

	public static ai.libs.jaicore.ml.interfaces.LabeledInstance<String> toJAICoreLabeledInstance(final Instance wekaInst) {
		ai.libs.jaicore.ml.interfaces.LabeledInstance<String> inst = new SimpleLabeledInstanceImpl();
		for (int att = 0; att < wekaInst.numAttributes(); att++) {
			if (att == wekaInst.classIndex()) {
				continue;
			}
			inst.add(wekaInst.value(att));
		}
		inst.setLabel(wekaInst.classAttribute().value((int) wekaInst.classValue()));
		return inst;
	}

	public static ai.libs.jaicore.ml.interfaces.Instances<SimpleInstanceImpl> toJAICoreInstances(final Instances wekaInstances) {
		ai.libs.jaicore.ml.interfaces.Instances<SimpleInstanceImpl> instances = new SimpleInstancesImpl(wekaInstances.size());
		for (Instance inst : wekaInstances) {
			instances.add(new SimpleInstanceImpl(inst.toDoubleArray()));
		}
		return instances;
	}

	public static Collection<String> getPossibleClassValues(final Instance instance) {
		Collection<String> labels = new ArrayList<>();
		Attribute classAttr = instance.classAttribute();
		for (int i = 0; i < classAttr.numValues(); i++) {
			labels.add(classAttr.value(i));
		}
		return labels;
	}

	public static ai.libs.jaicore.ml.interfaces.Instance toJAICoreInstance(final Instance wekaInst) {
		ai.libs.jaicore.ml.interfaces.Instance inst = new SimpleInstanceImpl();
		for (int att = 0; att < wekaInst.numAttributes(); att++) {
			inst.add(wekaInst.value(att));
		}
		return inst;
	}

	public static String getClassifierDescriptor(final Classifier c) {
		StringBuilder sb = new StringBuilder();
		sb.append(c.getClass().getName());
		if (c instanceof OptionHandler) {
			sb.append("- [");
			int i = 0;
			for (String s : ((OptionHandler) c).getOptions()) {
				if (i++ > 0) {
					sb.append(", ");
				}
				sb.append(s);
			}
			sb.append("]");
		}
		return sb.toString();
	}

	public static Collection<Option> getOptionsOfWekaAlgorithm(final Object o) {
		List<Option> options = new ArrayList<>();
		if (!(o instanceof OptionHandler)) {
			return options;
		}
		OptionHandler oh = (OptionHandler) o;
		Enumeration<Option> optionEnum = oh.listOptions();
		while (optionEnum.hasMoreElements()) {
			options.add(optionEnum.nextElement());
		}
		return options;
	}

	public static List<String> getClassNames(final Instance instance) {
		List<String> names = new ArrayList<>();
		Enumeration<Object> namesEnumration = instance.classAttribute().enumerateValues();
		while (namesEnumration.hasMoreElements()) {
			names.add((String) namesEnumration.nextElement());
		}
		return names;
	}

	public static Map<String, Integer> getClassNameToIDMap(final Instance instance) {
		Map<String, Integer> map = new HashMap<>();
		List<String> classNames = getClassNames(instance);
		for (int i = 0; i < classNames.size(); i++) {
			map.put(classNames.get(i), i);
		}
		return map;
	}

	public static int getIntValOfClassName(final Instance instance, final String className) {
		Map<String, Integer> map = getClassNameToIDMap(instance);
		return map.containsKey(className) ? map.get(className) : -1;
	}

	public static String getClassSplitAssignments(final List<Instances> split) {
		StringBuilder sb = new StringBuilder();
		Map<String, Instances> firstSet = getInstancesPerClass(split.get(0));
		for (String cl : firstSet.keySet()) {
			sb.append(cl);
			sb.append(": ");
			int i = 0;
			for (Instances set : split) {
				Map<String, Instances> map = getInstancesPerClass(set);
				sb.append(map.containsKey(cl) ? map.get(cl).size() : 0);
				if (i < split.size() - 1) {
					sb.append("/");
					i++;
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public static Instances getInstancesOfClass(final Instances data, final Collection<String> classNames) {
		Instances newInstances = new Instances(data);
		newInstances.removeIf(i -> !classNames.contains(WekaUtil.getClassName(i)));
		return newInstances;
	}

	public static Instances getInstancesOfClass(final Instances data, final String className) {
		Instances newInstances = new Instances(data);
		newInstances.removeIf(i -> !WekaUtil.getClassName(i).equals(className));
		return newInstances;
	}

	public static String getClassName(final Instance instance) {
		return getClassNames(instance).get((int) instance.classValue());
	}

	public static Map<String, Instances> getInstancesPerClass(final Instances data) {
		Instances emptyInstances = new Instances(data);
		emptyInstances.clear();
		Map<String, Instances> classWiseSeparation = new HashMap<>();
		for (Instance i : data) {
			String assignedClass = data.classAttribute().value((int) i.classValue());
			if (!classWiseSeparation.containsKey(assignedClass)) {
				Instances inst = new Instances(emptyInstances);
				classWiseSeparation.put(assignedClass, inst);
			}
			classWiseSeparation.get(assignedClass).add(i);
		}
		return classWiseSeparation;
	}

	public static Map<String, Integer> getNumberOfInstancesPerClass(final Instances data) {
		Map<String, Instances> instancesPerClass = getInstancesPerClass(data);
		Map<String, Integer> counter = new HashMap<>();
		for (Entry<String, Instances> classWithInstances : instancesPerClass.entrySet()) {
			counter.put(classWithInstances.getKey(), classWithInstances.getValue().size());
		}
		return counter;
	}

	public static int getNumberOfInstancesFromClass(final Instances data, final String c) {
		return getInstancesOfClass(data, c).size();
	}

	public static int getNumberOfInstancesFromClass(final Instances data, final Collection<String> cs) {
		Map<String, Integer> map = getNumberOfInstancesPerClass(data);
		int sum = 0;
		for (String c : cs) {
			if (map.containsKey(c)) {
				sum += map.get(c);
			}
		}
		return sum;
	}

	public static double getRelativeNumberOfInstancesFromClass(final Instances data, final String c) {
		if (data.isEmpty()) {
			return 0;
		}
		return getNumberOfInstancesFromClass(data, c) / (1f * data.size());
	}

	public static double getRelativeNumberOfInstancesFromClass(final Instances data, final Collection<String> cs) {
		return getNumberOfInstancesFromClass(data, cs) / (1f * data.size());
	}

	public static Collection<Integer>[] getArbitrarySplit(final Instances data, final Random rand, final double... portions) {

		/* check that portions sum up to s.th. smaller than 1 */
		double sum = 0;
		for (double p : portions) {
			sum += p;
		}
		if (sum > 1) {
			throw new IllegalArgumentException(MSG_SUM1);
		}

		LinkedList<Integer> indices = new LinkedList<>(ContiguousSet.create(Range.closed(0, data.size() - 1), DiscreteDomain.integers()).asList());
		Collections.shuffle(indices, rand);

		@SuppressWarnings("unchecked")
		Collection<Integer>[] folds = new ArrayList[portions.length + 1];
		Instances emptyInstances = new Instances(data);
		emptyInstances.clear();

		/* distribute instances over the folds */
		for (int i = 0; i <= portions.length; i++) {
			double portion = i < portions.length ? portions[i] : 1 - sum;
			int numberOfItems = (int) Math.floor(data.size() * portion);
			Collection<Integer> fold = new ArrayList<>(numberOfItems);
			for (int j = 0; j < numberOfItems; j++) {
				fold.add(indices.poll());
			}
			folds[i] = fold;
		}

		/* distribute remaining ones over the folds */
		while (!indices.isEmpty()) {
			folds[rand.nextInt(folds.length)].add(indices.poll());
		}

		if (debug && Arrays.asList(folds).stream().mapToInt(Collection::size).sum() != data.size()) {
			throw new IllegalStateException(MSG_DEVIATING_NUMBER_OF_INSTANCES);
		}
		return folds;
	}

	public static List<Instances> realizeSplit(final Instances data, final Collection<Integer>[] split) {
		return realizeSplitAsCopiedInstances(data, split);
	}

	public static List<Instances> realizeSplit(final Instances data, final List<List<Integer>> split) {
		return realizeSplitAsCopiedInstances(data, split);
	}

	public static List<Instances> realizeSplitAsCopiedInstances(final Instances data, final List<List<Integer>> split) {
		List<Instances> folds = new ArrayList<>();
		for (Collection<Integer> foldIndices : split) {
			Instances fold = new Instances(data, 0);
			foldIndices.stream().forEach(i -> fold.add(data.get(i)));
			folds.add(fold);
		}
		return folds;
	}

	public static List<Instances> realizeSplitAsCopiedInstances(final Instances data, final Collection<Integer>[] split) {
		List<Instances> folds = new ArrayList<>();
		for (Collection<Integer> foldIndices : split) {
			Instances fold = new Instances(data, 0);
			foldIndices.stream().forEach(i -> fold.add(data.get(i)));
			folds.add(fold);
		}
		return folds;
	}

	public static List<Instances> realizeSplitAsSubInstances(final Instances data, final Collection<Integer>[] split) {
		List<Instances> folds = new ArrayList<>();
		for (Collection<Integer> foldIndices : split) {
			int[] indices = new int[foldIndices.size()];
			int i = 0;
			for (Integer x : foldIndices) {
				indices[i++] = x;
			}
			folds.add(new SubInstances(data, indices));
		}
		return folds;
	}

	public static Collection<Integer>[] getStratifiedSplitIndices(final Instances data, final Random rand, final double... pPortions) {

		/* check that portions sum up to s.th. smaller than 1 */
		double sum = 0;
		double[] portions = new double[pPortions.length + 1];
		for (int i = 0; i < pPortions.length; i++) {
			sum += pPortions[i];
			portions[i] = pPortions[i];
		}
		if (sum > 1) {
			throw new IllegalArgumentException(MSG_SUM1);
		}
		portions[pPortions.length] = 1 - sum;

		/* determine how many instance of each class should be in each fold */
		Map<String, Integer> numberOfInstancesPerClass = getNumberOfInstancesPerClass(data);
		Map<String, Map<Integer, Integer>> numberOfInstancesPerClassAndFold = new HashMap<>();
		for (Entry<String, Integer> classNameWithNumberOfInstancesPerClassAndFold : numberOfInstancesPerClass.entrySet()) {
			String className = classNameWithNumberOfInstancesPerClassAndFold.getKey();
			numberOfInstancesPerClassAndFold.put(className, new HashMap<>());
			for (int foldId = 0; foldId < portions.length; foldId++) {
				numberOfInstancesPerClassAndFold.get(className).put(foldId, ((int) Math.ceil(classNameWithNumberOfInstancesPerClassAndFold.getValue() * portions[foldId])) + 1);
			}
		}

		/* compute basic structures for iteration over all instances */
		Map<String, Integer> nextBinForClass = new HashMap<>();
		numberOfInstancesPerClass.keySet().forEach(c -> nextBinForClass.put(c, 0));
		@SuppressWarnings("unchecked")
		Collection<Integer>[] folds = new ArrayList[portions.length];
		LinkedList<Integer> indices = new LinkedList<>(ContiguousSet.create(Range.closed(0, data.size() - 1), DiscreteDomain.integers()).asList());
		Collections.shuffle(indices, rand);

		/* first assign one item of each class to each fold */
		while (!indices.isEmpty()) {
			int index = indices.poll();

			/* determine fold where to place this instance */
			String assignedClass = WekaUtil.getClassName(data.get(index));
			int foldId = nextBinForClass.get(assignedClass);
			if (folds[foldId] == null) {
				folds[foldId] = new ArrayList<>();
			}
			Collection<Integer> fold = folds[foldId];
			fold.add(index);

			/* update point for class */
			numberOfInstancesPerClassAndFold.get(assignedClass).put(foldId, numberOfInstancesPerClassAndFold.get(assignedClass).get(foldId) - 1);

			do {
				foldId++;
				if (foldId >= portions.length) {
					foldId = 0;
				}
			} while (numberOfInstancesPerClassAndFold.get(assignedClass).get(foldId) <= 0);
			nextBinForClass.put(assignedClass, foldId);
		}

		if (debug && Arrays.asList(folds).stream().mapToInt(Collection::size).sum() != data.size()) {
			throw new IllegalStateException(MSG_DEVIATING_NUMBER_OF_INSTANCES);
		}
		return folds;
	}

	public static List<List<Integer>> getStratifiedSplitIndicesAsList(final Instances data, final Random rand, final double... portions) {
		/* check that portions sum up to s.th. smaller than 1 */
		double sum = 0;
		for (double p : portions) {
			sum += p;
		}
		if (sum > 1) {
			throw new IllegalArgumentException(MSG_SUM1);
		}

		Instances shuffledData = new Instances(data);
		shuffledData.randomize(rand);
		List<List<Integer>> instances = new ArrayList<>();
		Instances emptyInstances = new Instances(shuffledData);
		emptyInstances.clear();

		/* compute instances per class */
		Map<String, List<Integer>> classWiseSeparation = new HashMap<>();

		for (int i = 0; i < data.size(); i++) {
			String assignedClass = data.classAttribute().value((int) data.get(i).classValue());
			if (!classWiseSeparation.containsKey(assignedClass)) {
				classWiseSeparation.put(assignedClass, new LinkedList<>());
			}
			classWiseSeparation.get(assignedClass).add(i);
		}

		Map<String, Integer> classCapacities = new HashMap<>();
		for (Entry<String, List<Integer>> classWithItsIndices : classWiseSeparation.entrySet()) {
			classCapacities.put(classWithItsIndices.getKey(), classWithItsIndices.getValue().size());
		}

		/* first assign one item of each class to each fold */
		for (int i = 0; i <= portions.length; i++) {
			List<Integer> instancesForSplit = new LinkedList<>();
			for (List<Integer> availableInstances : classWiseSeparation.values()) {
				if (!availableInstances.isEmpty()) {
					instancesForSplit.add(availableInstances.get(0));
					availableInstances.remove(0);
				}
			}
			instances.add(instancesForSplit);
		}

		/* now distribute remaining instances over the folds */
		for (int i = 0; i <= portions.length; i++) {
			double portion = i < portions.length ? portions[i] : 1 - sum;
			List<Integer> instancesForSplit = instances.get(i);
			for (Entry<String, List<Integer>> classWithItsIndices : classWiseSeparation.entrySet()) {
				List<Integer> availableInstances = classWithItsIndices.getValue();
				int items = (int) Math.min(availableInstances.size(), Math.ceil(portion * classCapacities.get(classWithItsIndices.getKey())));
				for (int j = 0; j < items; j++) {
					instancesForSplit.add(availableInstances.get(0));
					availableInstances.remove(0);
				}
			}

			Collections.shuffle(instancesForSplit, rand);
		}
		if (debug && instances.stream().mapToInt(List::size).sum() != data.size()) {
			throw new IllegalStateException(MSG_DEVIATING_NUMBER_OF_INSTANCES);
		}
		return instances;
	}

	public static ArrayNode splitToJsonArray(final Collection<Integer>[] splitDecision) {
		ObjectMapper om = new ObjectMapper();
		ArrayNode an = om.createArrayNode();
		splitDecision[0].stream().sorted().forEach(an::add);
		return an;
	}

	public static List<Instances> getStratifiedSplit(final Instances data, final long seed, final double... portions) {
		// if data should be reproducible use other method.
		if (data instanceof ReproducibleInstances) {
			List<ReproducibleInstances> reproducibleInstancesResult = getStratifiedSplit((ReproducibleInstances) data, seed, portions);
			ArrayList<Instances> result = new ArrayList<>(reproducibleInstancesResult.size());
			for (int i = 0; i < reproducibleInstancesResult.size(); i++) {
				result.add(reproducibleInstancesResult.get(i));
			}
			return result;
		}

		/* check that portions sum up to s.th. smaller than 1 */
		double sum = 0;
		for (double p : portions) {
			sum += p;
		}
		if (sum > 1) {
			throw new IllegalArgumentException(MSG_SUM1);
		}

		Instances shuffledData = new Instances(data);
		Random rand = new Random(seed);
		shuffledData.randomize(rand);
		List<Instances> instances = new ArrayList<>();
		Instances emptyInstances = new Instances(shuffledData);
		emptyInstances.clear();

		/* compute instances per class */
		Map<String, Instances> classWiseSeparation = getInstancesPerClass(shuffledData);

		Map<String, Integer> classCapacities = new HashMap<>(classWiseSeparation.size());
		for (Entry<String, Instances> classWithItsInstances : classWiseSeparation.entrySet()) {
			classCapacities.put(classWithItsInstances.getKey(), classWithItsInstances.getValue().size());
		}

		/* first assign one item of each class to each fold */
		for (int i = 0; i <= portions.length; i++) {
			Instances instancesForSplit = new Instances(emptyInstances);
			for (Instances availableInstances : classWiseSeparation.values()) {
				if (!availableInstances.isEmpty()) {
					instancesForSplit.add(availableInstances.get(0));
					availableInstances.remove(0);
				}
			}
			instances.add(instancesForSplit);
		}

		/* now distribute remaining instances over the folds */
		for (int i = 0; i <= portions.length; i++) {
			double portion = i < portions.length ? portions[i] : 1 - sum;
			Instances instancesForSplit = instances.get(i);
			for (Entry<String, Instances> classNamesWithItsInstances : classWiseSeparation.entrySet()) {
				Instances availableInstances = classNamesWithItsInstances.getValue();
				int items = (int) Math.min(availableInstances.size(), Math.ceil(portion * classCapacities.get(classNamesWithItsInstances.getKey())));
				for (int j = 0; j < items; j++) {
					instancesForSplit.add(availableInstances.get(0));
					availableInstances.remove(0);
				}
			}
			instancesForSplit.randomize(rand);
		}
		if (debug && Arrays.asList(instances).stream().mapToInt(Collection::size).sum() != data.size()) {
			throw new IllegalStateException(MSG_DEVIATING_NUMBER_OF_INSTANCES);
		}
		return instances;
	}

	/**
	 * Creates a stratified split for a given {@link ReproducibleInstances} Object. The history will be updated to track the split.
	 *
	 * @param data
	 *            - Input data
	 * @param rand
	 *            - random used to get a seed, which can be used and saved
	 * @param portions
	 *            - ratios to split
	 * @return a list of {@link ReproducibleInstances}. For each of them the history will be updated to track the split
	 */
	public static List<ReproducibleInstances> getStratifiedSplit(final ReproducibleInstances data, final Random rand, final double... portions) {
		return getStratifiedSplit(data, rand.nextLong(), portions);
	}

	/**
	 * Creates a StratifiedSplit for a given {@link ReproducibleInstances} Object. THe History will be updated to track the split.
	 *
	 * @param data
	 *            - Input data
	 * @param seed
	 *            - random seed
	 * @param portions
	 *            - ratios to split
	 * @return a List of {@link ReproducibleInstances}. For each of them the history will be updated to track the split
	 */
	public static List<ReproducibleInstances> getStratifiedSplit(final ReproducibleInstances data, final long seed, final double... portions) {

		/* check that portions sum up to s.th. smaller than 1 */
		double sum = 0;
		for (double p : portions) {
			sum += p;
		}
		if (sum > 1) {
			throw new IllegalArgumentException(MSG_SUM1);
		}

		Instances shuffledData = new Instances(data);
		Random rand = new Random(seed);
		shuffledData.randomize(rand);
		List<ReproducibleInstances> instances = new ArrayList<>();
		ReproducibleInstances emptyInstances = new ReproducibleInstances(data);
		emptyInstances.clear(); // leaves History untouched

		/* compute instances per class */
		Map<String, Instances> classWiseSeparation = getInstancesPerClass(shuffledData);

		Map<String, Integer> classCapacities = new HashMap<>(classWiseSeparation.size());
		for (Entry<String, Instances> classNamesWithItsInstances : classWiseSeparation.entrySet()) {
			classCapacities.put(classNamesWithItsInstances.getKey(), classNamesWithItsInstances.getValue().size());
		}

		/* first assign one item of each class to each fold */
		for (int i = 0; i <= portions.length; i++) {
			ReproducibleInstances instancesForSplit = new ReproducibleInstances(emptyInstances); // Will have the same history as data but is empty
			for (Instances availableInstances : classWiseSeparation.values()) {
				if (!availableInstances.isEmpty()) {
					instancesForSplit.add(availableInstances.get(0));
					availableInstances.remove(0);
				}
			}
			instances.add(instancesForSplit);
		}

		/* now distribute remaining instances over the folds */
		for (int i = 0; i <= portions.length; i++) {
			double portion = i < portions.length ? portions[i] : 1 - sum;
			ReproducibleInstances instancesForSplit = instances.get(i);
			for (Entry<String, Instances> classNameWithInstances : classWiseSeparation.entrySet()) {
				Instances availableInstances = classNameWithInstances.getValue();
				int items = (int) Math.min(availableInstances.size(), Math.ceil(portion * classCapacities.get(classNameWithInstances.getKey())));
				for (int j = 0; j < items; j++) {
					instancesForSplit.add(availableInstances.get(0));
					availableInstances.remove(0);
				}
			}
			instancesForSplit.randomize(rand);
		}
		if (debug && Arrays.asList(instances).stream().mapToInt(Collection::size).sum() != data.size()) {
			throw new IllegalStateException(MSG_DEVIATING_NUMBER_OF_INSTANCES);
		}

		/* update ReproducibleInstanes history */
		String ratiosAsString = Arrays.toString(portions);
		for (int i = 0; i < instances.size(); i++) {
			instances.get(i).addInstruction(new SplitInstruction(ratiosAsString, seed, i));
		}
		return instances;
	}

	public static List<File> getDatasetsInFolder(final File folder) throws IOException {
		List<File> files = new ArrayList<>();
		try (Stream<Path> paths = Files.walk(folder.toPath())) {
			paths.filter(f -> f.getParent().toFile().equals(folder) && f.toFile().getAbsolutePath().endsWith(".arff")).forEach(f -> files.add(f.toFile()));
		}
		return files.stream().sorted().collect(Collectors.toList());
	}

	public static Instances getRefactoredInstances(final Instances data, final Map<String, String> classMap) {

		List<String> targetClasses = new ArrayList<>(new HashSet<>(classMap.values()));
		Instances childData = WekaUtil.getEmptySetOfInstancesWithRefactoredClass(data, targetClasses);
		for (Instance i : data) {
			String className = i.classAttribute().value((int) Math.round(i.classValue()));
			if (classMap.containsKey(className)) {
				Instance iNew = WekaUtil.getRefactoredInstance(i, targetClasses);
				iNew.setClassValue(classMap.get(className));
				iNew.setDataset(childData);
				childData.add(iNew);
			}
		}
		return childData;
	}

	public static Instance getRefactoredInstance(final Instance instance) {
		return getRefactoredInstance(instance, Arrays.asList("0.0", "1.0"));
	}

	public static Instance getRefactoredInstance(final Instance instance, final List<String> classes) {

		/* modify instance */
		Instances dataset = WekaUtil.getEmptySetOfInstancesWithRefactoredClass(instance.dataset(), classes);
		int numAttributes = instance.numAttributes();
		int classIndex = instance.classIndex();
		Instance iNew = new DenseInstance(numAttributes);
		for (int i = 0; i < numAttributes; i++) {
			Attribute a = instance.attribute(i);
			if (i != classIndex) {
				iNew.setValue(a, instance.value(a));
			} else {
				iNew.setValue(a, 0.0); // the value does not matter since this should only be used for TESTING
			}
		}
		dataset.add(iNew);
		iNew.setDataset(dataset);
		return iNew;
	}

	public static Instances getEmptySetOfInstancesWithRefactoredClass(final Instances instances) {
		return getEmptySetOfInstancesWithRefactoredClass(instances, Arrays.asList("0.0", "1.0"));
	}

	public static Instances getEmptySetOfInstancesWithRefactoredClass(final Instances instances, final List<String> classes) {
		List<Attribute> newAttributes = getAttributes(instances, false);
		newAttributes.add(instances.classIndex(), getNewClassAttribute(instances.classAttribute(), classes));
		Instances newData = new Instances("split", (ArrayList<Attribute>) newAttributes, 0);
		newData.setClassIndex(instances.classIndex());
		return newData;
	}

	public static List<Attribute> getAttributes(final Instances inst, final boolean includeClassAttribute) {
		List<Attribute> attributes = new ArrayList<>();
		Enumeration<Attribute> e = inst.enumerateAttributes();
		while (e.hasMoreElements()) {
			attributes.add(e.nextElement());
		}
		if (includeClassAttribute) {
			attributes.add(inst.classAttribute());
		}
		return attributes;
	}

	public static List<Attribute> getAttributes(final Instance inst) {
		List<Attribute> attributes = new ArrayList<>();
		Enumeration<Attribute> e = inst.enumerateAttributes();
		while (e.hasMoreElements()) {
			attributes.add(e.nextElement());
		}
		return attributes;
	}

	public static boolean hasOnlyNumericAttributes(final Instances instances) {
		for (Attribute a : getAttributes(instances, false)) {
			if (!a.isNumeric()) {
				return false;
			}
		}
		return true;
	}

	public static Attribute getNewClassAttribute(final Attribute attribute) {
		List<String> vals = Arrays.asList("0.0", "1.0");
		return getNewClassAttribute(attribute, vals);
	}

	public static Attribute getNewClassAttribute(final Attribute attribute, final List<String> classes) {
		return new Attribute(attribute.name(), classes);
	}

	public static List<Attribute> getReplacedAttributeList(final List<Attribute> attributes, final Attribute classAttribute) {
		ArrayList<Attribute> newAttributes = new ArrayList<>();
		for (Attribute a : attributes) {
			if (classAttribute != a) {
				newAttributes.add(a);
			} else {
				newAttributes.add(getNewClassAttribute(classAttribute));
			}
		}
		return newAttributes;
	}

	public static Instances mergeClassesOfInstances(final Instances data, final Collection<String> cluster1, final Collection<String> cluster2) {
		Instances newData = WekaUtil.getEmptySetOfInstancesWithRefactoredClass(data);
		for (Instance i : data) {
			Instance iNew = (Instance) i.copy();
			String className = i.classAttribute().value((int) Math.round(i.classValue()));
			if (cluster1.contains(className)) {
				iNew.setClassValue(0.0);
				newData.add(iNew);
			} else if (cluster2.contains(className)) {
				iNew.setClassValue(1.0);
				newData.add(iNew);
			}
		}
		return newData;
	}

	public static Instances mergeClassesOfInstances(final Instances data, final List<Set<String>> instancesCluster) {
		List<String> classes = new LinkedList<>();
		IntStream.range(0, instancesCluster.size()).forEach(x -> classes.add("C" + ((double) x)));

		Instances newData = WekaUtil.getEmptySetOfInstancesWithRefactoredClass(data, classes);

		for (Instance i : data) {
			Instance iNew = (Instance) i.copy();
			String className = i.classAttribute().value((int) Math.round(i.classValue()));
			for (Set<String> cluster : instancesCluster) {
				if (cluster.contains(className)) {
					iNew.setClassValue(instancesCluster.indexOf(cluster));
					iNew.setDataset(newData);
					newData.add(iNew);
				}
			}
		}
		return newData;
	}

	public static List<String> getClassesDeclaredInDataset(final Instances data) {
		List<String> classes = new ArrayList<>();
		Attribute classAttribute = data.classAttribute();
		for (int i = 0; i < classAttribute.numValues(); i++) {
			classes.add(classAttribute.value(i));
		}
		return classes;
	}

	public static Collection<String> getClassesActuallyContainedInDataset(final Instances data) {
		Map<String, Integer> counter = getNumberOfInstancesPerClass(data);
		return counter.keySet().stream().filter(k -> counter.get(k) != 0).collect(Collectors.toList());
	}

	public static double[] getClassesAsArray(final Instances inst) {
		int n = inst.size();
		double[] vec = new double[n];
		for (int i = 0; i < n; i++) {
			vec[i] = inst.get(i).classValue();
		}
		return vec;
	}

	public static List<Double> getClassesAsList(final Instances inst) {
		return inst.stream().map(Instance::classValue).collect(Collectors.toList());
	}

	public static String instancesToJsonString(final Instances data) {
		StringBuilder sb = new StringBuilder();
		JSONNode json = JSONInstances.toJSON(data);
		json.getChild("header").removeFromParent();
		StringBuffer buffer = new StringBuffer();
		json.toString(buffer);
		sb.append(buffer.toString());
		sb.append("\n");
		return sb.toString();
	}

	public static Instances jsonStringToInstances(final String json) throws Exception {
		JSONNode node = JSONNode.read(new BufferedReader(new StringReader(json)));
		return JSONInstances.toInstances(node);
	}

	/**
	 * Compute indices of instances of the original data set that are contained in the given subset. This does only work for data sets that contain an instance at most once!
	 *
	 * @param dataset
	 * @param subset
	 * @return
	 */
	public static int[] getIndicesOfContainedInstances(final Instances dataset, final Instances subset) {
		int[] indices = new int[subset.size()];
		InstanceComparator comp = new InstanceComparator();
		for (int i = 0; i < indices.length; i++) {
			Instance inst = subset.get(i);
			int index = -1;
			for (int j = 0; j < dataset.size(); j++) {
				if (comp.compare(inst, dataset.get(j)) == 0) {
					index = j;
					break;
				}
			}
			if (index == -1) {
				throw new IllegalArgumentException("The instance " + inst + " is not contained in the given dataset.");
			}
			indices[i] = index;
		}
		return indices;
	}

	public static Instance useFilterOnSingleInstance(final Instance instance, final Filter filter) throws Exception {
		Instances data = new Instances(instance.dataset());
		data.clear();
		data.add(instance);
		Instances filteredInstances = Filter.useFilter(data, filter);
		return filteredInstances.firstInstance();
	}

	public static Instances removeClassAttribute(final Instances data) throws Exception {
		if (data.classIndex() < 0) {
			throw new IllegalArgumentException("Class index of data is not set!");
		}
		Remove remove = new Remove();
		remove.setAttributeIndices("" + (data.classIndex() + 1));
		remove.setInputFormat(data);
		return Filter.useFilter(data, remove);
	}

	public static Instance removeClassAttribute(final Instance inst) throws Exception {
		Remove remove = new Remove();
		remove.setAttributeIndices("" + (inst.classIndex() + 1));
		remove.setInputFormat(inst.dataset());
		return useFilterOnSingleInstance(inst, remove);
	}

	public static Classifier cloneClassifier(final Classifier c) throws Exception {
		Method cloneMethod = MethodUtils.getAccessibleMethod(c.getClass(), "clone");
		if (cloneMethod != null) {
			return (Classifier) cloneMethod.invoke(c);
		}
		return AbstractClassifier.makeCopy(c);
	}

	public static int[] getIndicesOfSubset(final Instances data, final Instances subset) {

		InstanceComparator comp = new InstanceComparator();
		List<Instance> copy = new ArrayList<>(subset);

		/* init rows object */
		int[] result = new int[subset.size()];
		int row = 0;
		int i = 0;
		for (Instance ref : data) {
			for (int j = 0; j < copy.size(); j++) {
				Instance inst = copy.get(j);
				if (inst != null && comp.compare(inst, ref) == 0) {
					result[i++] = row;
					copy.remove(inst);
				}
			}
			row++;
		}
		return result;
	}

	public static String printNestedWekaClassifier(final Classifier c) {
		StringBuilder sb = new StringBuilder();
		sb.append(c.getClass().getName());
		sb.append("(");

		if (c instanceof SingleClassifierEnhancer) {
			sb.append(printNestedWekaClassifier(((SingleClassifierEnhancer) c).getClassifier()));
		} else if (c instanceof SMO) {
			sb.append(((SMO) c).getKernel().getClass().getName());
		} else if (c instanceof MultipleClassifiersCombiner) {
			sb.append(printNestedWekaClassifier(((MultipleClassifiersCombiner) c).getClassifier(0)));
		}

		sb.append(")");

		return sb.toString();
	}

	public static boolean isDebug() {
		return debug;
	}

	public static void setDebug(final boolean debug) {
		WekaUtil.debug = debug;
	}
}
