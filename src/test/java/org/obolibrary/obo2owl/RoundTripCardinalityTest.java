package org.obolibrary.obo2owl;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.obo2owl.Owl2Obo;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.model.QualifierValue;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

public class RoundTripCardinalityTest extends RoundTripTest {
	
	private static final boolean RENDER_OWL = false;
	private static final boolean RENDER_OBO = false;

	@Test
	public void testRoundTripCardinality() throws Exception {
		Logger.getRootLogger().setLevel(Level.DEBUG);
		// create minimal ontology
		OBODoc oboDocSource = super.parseOBOFile("roundtrip_cardinality.obo");

		// convert to OWL and retrieve def
		Obo2Owl bridge = new Obo2Owl();
		OWLOntology owlOntology = bridge.convert(oboDocSource);
		if (RENDER_OWL) {
			System.out.println("------------");
			renderOWL(owlOntology);
			System.out.println("------------");
		}
		final OWLDataFactory factory = owlOntology.getOWLOntologyManager()
				.getOWLDataFactory();

		OWLClass c = factory.getOWLClass(bridge.oboIdToIRI("PR:000027136"));

		// Relations
		boolean foundRel1 = false;
		boolean foundRel2 = false;
		Set<OWLSubClassOfAxiom> axioms = owlOntology.getSubClassAxiomsForSubClass(c);
		assertEquals(3, axioms.size());
		for (OWLSubClassOfAxiom axiom : axioms) {
			OWLClassExpression superClass = axiom.getSuperClass();
			if (superClass instanceof OWLObjectExactCardinality) {
				OWLObjectExactCardinality cardinality = (OWLObjectExactCardinality) superClass;
				OWLClassExpression filler = cardinality.getFiller();
				assertFalse(filler.isAnonymous());
				IRI iri = filler.asOWLClass().getIRI();
				if (iri.equals(bridge.oboIdToIRI("PR:000005116"))) {
					foundRel1 = true;
					assertEquals(1, cardinality.getCardinality());
				}
				else if (iri.equals(bridge.oboIdToIRI("PR:000027122"))) {
					foundRel2 = true;
					assertEquals(2, cardinality.getCardinality());
				}
			}
			
		}
		
		assertTrue(foundRel1);
		assertTrue(foundRel2);


		// convert back to OBO
		Owl2Obo owl2Obo = new Owl2Obo();
		OBODoc convertedOboDoc = owl2Obo.convert(owlOntology);

		Frame convertedFrame = convertedOboDoc.getTermFrame("PR:000027136");
		Collection<Clause> clauses = convertedFrame.getClauses(OboFormatTag.TAG_RELATIONSHIP);

		// check that round trip still contains relationships
		assertEquals(2, clauses.size());

		for (Clause clause : clauses) {
			Collection<QualifierValue> qualifierValues = clause.getQualifierValues();
			assertEquals(1, qualifierValues.size());
			QualifierValue value = qualifierValues.iterator().next();
			assertEquals("cardinality", value.getQualifier());
			if (clause.getValue2().equals("PR:000005116")) {
				assertEquals("1", value.getValue());	
			}
			else if (clause.getValue2().equals("PR:000027122")) {
				assertEquals("2", value.getValue());	
			}
			
		}
		if (RENDER_OBO) {
			System.out.println("------------");
			renderOBO(convertedOboDoc);
			System.out.println("------------");
		}
	}
	
	@Test
	public void roundTrip() throws Exception {
		roundTripOBOFile("roundtrip_cardinality.obo", true);
	}
}
