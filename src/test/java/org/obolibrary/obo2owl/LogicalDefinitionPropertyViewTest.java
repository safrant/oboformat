package org.obolibrary.obo2owl;

import static junit.framework.Assert.*;

import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * @author cjm
 * 
 * see 5.9.3 and 8.2.2 of spec
 * 
 * See http://code.google.com/p/oboformat/issues/detail?id=13
 *
 */
public class LogicalDefinitionPropertyViewTest extends OboFormatTestBasics {

	// set to true if data should be printed to System.out
	private static boolean useSystemOut = false;
	
	@BeforeClass
	public static void beforeClass() {
		Logger.getRootLogger().setLevel(Level.ERROR);		
	}

	@Test
	public void testConvert() throws Exception {

		// PARSE TEST FILE

		OWLOntology owlOntology = convert(parseOBOFile("logical-definition-view-relation-test.obo"));

		if (useSystemOut) {
			for (OWLAnnotation ann : owlOntology.getAnnotations()) {
				System.out.println("Ann: " + ann);
			}
		}
		boolean ok = false;
		for (OWLEquivalentClassesAxiom eca : owlOntology.getAxioms(AxiomType.EQUIVALENT_CLASSES)) {
			if (useSystemOut) {
				System.out.println(eca);
			}
			for (OWLClassExpression x : eca.getClassExpressions()) {
				if (useSystemOut) {
					System.out.println("  " + x);
				}
				if (x instanceof OWLObjectSomeValuesFrom) {
					// fairly weak test - just ensure it's done _something_ here
					OWLObjectProperty p =   (OWLObjectProperty) ((OWLObjectSomeValuesFrom)x).getProperty();
					if (useSystemOut) {
						System.out.println("    " + p);
					}
					if (p.getIRI().toString().equals("http://purl.obolibrary.org/obo/BFO_0000050")) {
						ok = true;
					}
				}
			}
		}	
		assertTrue(ok);
		
		// reverse translation
		OBODoc obodoc = this.convert(owlOntology);
		Frame fr = obodoc.getTermFrame("X:1");
		if (useSystemOut) {
			System.out.println(fr);
		}
		Collection<Clause> clauses = fr.getClauses(OboFormatTag.TAG_INTERSECTION_OF);
		assertTrue(clauses.size() == 2);
		if (useSystemOut) {
			for (Clause c : clauses) {
				System.out.println(" c=" + c);
			}
		}
	}

}
