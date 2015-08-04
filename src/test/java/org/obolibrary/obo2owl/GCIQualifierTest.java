package org.obolibrary.obo2owl;

import static junit.framework.Assert.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

/**
 * @author cjm
 * 
 * see 5.9.3 and 8.2.2 of spec
 *
 */
public class GCIQualifierTest extends OboFormatTestBasics  {

	@BeforeClass
	public static void beforeClass() {
		Logger.getRootLogger().setLevel(Level.ERROR);
	}

	@Test
	public void testConvert() throws Exception {

		// PARSE TEST FILE, CONVERT TO OWL, AND WRITE TO OWL FILE 
		OWLOntology ontology = convert(parseOBOFile("gci_qualifier_test.obo"), "x.owl");


		if (true) {
			Set<OWLSubClassOfAxiom> scas = ontology.getAxioms(AxiomType.SUBCLASS_OF);
			boolean ok = false;
			for (OWLSubClassOfAxiom sca : scas) {
				System.out.println(sca);
				OWLClassExpression sub = sca.getSubClass();
				ok = true;
			}
			assertTrue(ok);
		}

		// CONVERT BACK TO OBO
		OBODoc obodoc = convert(ontology);

		// test that relation IDs are converted back to symbolic form
		Frame tf = obodoc.getTermFrame("X:1");
		Collection<Clause> clauses = tf.getClauses(OboFormatTag.TAG_RELATIONSHIP);
		assertEquals(2, clauses.size());
		Iterator<Clause> iterator = clauses.iterator();

		Clause c1 = iterator.next();
		Clause c2 = iterator.next();
		System.out.println("c1="+c1);
		System.out.println("c2="+c2);

	}

}
