package ai.libs.softwareconfiguration.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.components.api.IComponent;
import ai.libs.jaicore.components.api.IComponentRepository;
import ai.libs.jaicore.components.api.IParameter;
import ai.libs.jaicore.components.model.ComponentInstance;
import ai.libs.jaicore.components.model.ComponentUtil;
import ai.libs.jaicore.components.model.NumericParameterDomain;
import ai.libs.jaicore.components.serialization.ComponentSerialization;

/**
 * Tests whether the methods provided by the ComponentUtil work properly.
 *
 * @author wever
 */
public class ComponentUtilTest {

	/* Logging */
	private static final Logger L = LoggerFactory.getLogger(ComponentUtilTest.class);

	/* Component repository for components to be checked during the test. */
	private static final File COMPONENT_REPOSITORY = new File("testrsc/difficultproblem.json");

	/* Collection of components that may be used in the single unit tests. */
	private static IComponentRepository components;

	@BeforeAll
	public static void setup() throws IOException {
		components = new ComponentSerialization().deserializeRepository(COMPONENT_REPOSITORY);
	}

	@Test
	public void testDefaultParameterization() {
		for (IComponent component : components) {
			L.info("Testing default parameterization for component {}", component.getName());
			ComponentInstance ci = ComponentUtil.getDefaultParameterizationOfComponent(component);
			for (IParameter param : component.getParameters()) {
				if (param.getDefaultDomain() instanceof NumericParameterDomain) {
					double expected = (Double) param.getDefaultValue();
					double actual = Double.parseDouble(ci.getParameterValue(param.getName()));
					assertEquals("Parameter " + param.getName() + " does not have default value " + expected + " but instead has value " + actual, expected, actual, 1.0E-9);
				} else {
					String expected = param.getDefaultValue() + "";
					String actual = ci.getParameterValue(param.getName());
					assertEquals("Parameter " + param.getName() + " does not have default value " + expected + " but instead has value " + actual, expected, actual);
				}
			}
		}
	}

	@Test
	public void testRandomParameterization() {
		for (IComponent component : components) {
			L.info("Testing random parameterization for component {}", component.getName());
			ComponentInstance ci = ComponentUtil.getRandomParameterizationOfComponent(component, new Random());
			for (IParameter param : component.getParameters()) {
				if (param.getDefaultDomain() instanceof NumericParameterDomain) {
					assertTrue("Value for parameter " + param.getName() + " is not a correct value of its domain.", ((NumericParameterDomain) param.getDefaultDomain()).contains(Double.valueOf(ci.getParameterValue(param.getName()))));
				} else {
					assertTrue("Value for parameter " + param.getName() + " is not a correct value of its domain.", param.getDefaultDomain().contains(ci.getParameterValue(param.getName())));
				}
			}
		}
	}

}
