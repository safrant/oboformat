package org.obolibrary.obo2owl;

import static junit.framework.Assert.*;

import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * @author cjm
 */
public class SubsetTest extends OboFormatTestBasics {

	@BeforeClass
	public static void beforeClass(){
		Logger.getRootLogger().setLevel(Level.ERROR);
	}
	
	@Test
	public void testConvert() throws Exception {

		// PARSE TEST FILE
		OWLOntology ontology = convert(parseOBOFile("subset_test.obo"));
		
		Set<OWLAnnotation> anns = ontology.getAnnotations();
		for (OWLAnnotation ann : anns) {
			// TODO
			System.out.println("Ann="+ann);
		}
		OWLAnnotationSubject subj = IRI.create("http://purl.obolibrary.org/obo/GO_0000003");
		Set<OWLAnnotationAssertionAxiom> aas = ontology.getAnnotationAssertionAxioms(subj );
		boolean ok = false;
		for (OWLAnnotationAssertionAxiom aa : aas) {
			System.out.println(aa);
			if (aa.getProperty().getIRI().toString().equals("http://www.geneontology.org/formats/oboInOwl#inSubset")) {
				 OWLAnnotationValue v = aa.getValue();
				System.out.println("  dep="+v);
				ok = true;
			}
		}
		assertTrue(ok);

	}

}
