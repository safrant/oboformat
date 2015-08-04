package org.obolibrary.obo2owl;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Tests for the handling of axioms, which cannot be translated to OBO.
 * Such axioms will be added in a tag in the ontology header. 
 */
public class UntranslatableAxiomsInHeaderTest extends OboFormatTestBasics {

	public static boolean USE_SYSTEM_OUT = false;
	
	@Test
	public void testUntranslatableAxioms() throws Exception {
		final OWLOntology original = parseOWLFile("untranslatable_axioms.owl");
		
		Owl2Obo owl2Obo = new Owl2Obo();
		
		OBODoc obo = owl2Obo.convert(original);
		
		String oboString = renderOboToString(obo);
		if (USE_SYSTEM_OUT) {
			System.out.println("----------------");
			System.out.println(oboString);
			System.out.println("----------------");
		}
		
		Frame headerFrame = obo.getHeaderFrame();
		String owlAxiomString = headerFrame.getTagValue(OboFormatTag.TAG_OWL_AXIOMS, String.class);
		assertNotNull(owlAxiomString);
		
		Obo2Owl obo2Owl = new Obo2Owl();
		
		OWLOntology converted = obo2Owl.convert(obo);
		
		Set<OWLEquivalentClassesAxiom> originalEqAxioms = original.getAxioms(AxiomType.EQUIVALENT_CLASSES);
		Set<OWLEquivalentClassesAxiom> convertedEqAxioms = converted.getAxioms(AxiomType.EQUIVALENT_CLASSES);
		
		assertEquals(originalEqAxioms.size(), convertedEqAxioms.size());
		assertTrue(originalEqAxioms.containsAll(convertedEqAxioms));
		assertTrue(convertedEqAxioms.containsAll(originalEqAxioms));
	}
	
	@Test
	public void testUntranslatableAxioms2() throws Exception {
		final OWLOntology original = parseOWLFile("untranslatable_axioms2.owl");
		
		Owl2Obo owl2Obo = new Owl2Obo();
		
		OBODoc obo = owl2Obo.convert(original);
		
		String oboString = renderOboToString(obo);
		if (USE_SYSTEM_OUT) {
			System.out.println("----------------");
			System.out.println(oboString);
			System.out.println("----------------");
		}
		
		Frame headerFrame = obo.getHeaderFrame();
		String owlAxiomString = headerFrame.getTagValue(OboFormatTag.TAG_OWL_AXIOMS, String.class);
		assertNotNull(owlAxiomString);
		
		Obo2Owl obo2Owl = new Obo2Owl();
		
		OWLOntology converted = obo2Owl.convert(obo);
		
		Set<OWLEquivalentClassesAxiom> originalEqAxioms = original.getAxioms(AxiomType.EQUIVALENT_CLASSES);
		Set<OWLEquivalentClassesAxiom> convertedEqAxioms = converted.getAxioms(AxiomType.EQUIVALENT_CLASSES);
		
		assertEquals(originalEqAxioms.size(), convertedEqAxioms.size());
		assertTrue(originalEqAxioms.containsAll(convertedEqAxioms));
		assertTrue(convertedEqAxioms.containsAll(originalEqAxioms));
	}
}
