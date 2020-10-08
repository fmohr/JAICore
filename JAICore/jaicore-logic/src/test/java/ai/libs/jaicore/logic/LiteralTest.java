package ai.libs.jaicore.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ai.libs.jaicore.logic.fol.structure.Literal;
import ai.libs.jaicore.logic.fol.structure.LiteralParam;
import ai.libs.jaicore.logic.fol.structure.Type;
import ai.libs.jaicore.logic.fol.structure.TypeModule;
import ai.libs.jaicore.logic.fol.structure.VariableParam;

/**
 * Test case for the Literal class.
 *
 * @author mbunse
 */
public class LiteralTest {

	/**
	 * Tests, if the constructor "Literal(Literal literal, Map<VariableParam, VariableParam> mapping)" produces a correctly mapped version of the literal parameter.
	 */
	@Test
	public void testMappingConstructor() {

		TypeModule typeModule = new TypeModule();

		// create original literal
		List<LiteralParam> params = new LinkedList<>();
		Type dummyType = typeModule.getType("http://dummy.type");
		params.add(new VariableParam("v1", dummyType));
		params.add(new VariableParam("v2", dummyType));

		Literal orig = new Literal("p", params);

		// create mapped version of original
		Map<VariableParam, VariableParam> mapping = new HashMap<>();
		mapping.put(new VariableParam("v1", dummyType), new VariableParam("s", dummyType));

		Literal mappedOrig = orig.clone(mapping);

		// check mapped version
		assertEquals("Properties of original and mapped version are unequal!", orig.getProperty(), mappedOrig.getProperty());
		assertEquals("Mapped version has other number of parameters!", mappedOrig.getParameters().size(), orig.getParameters().size());
		assertTrue("Mapped version does not contain variable map target!", mappedOrig.getParameters().contains(new VariableParam("s", dummyType)));
		assertTrue("Mapped version does not contain unmapped parameter!", mappedOrig.getParameters().contains(new VariableParam("v2", dummyType)));
		assertTrue("Mapped version contains the substituted parameter!", !mappedOrig.getParameters().contains(new VariableParam("v1", dummyType)));
	}
}
