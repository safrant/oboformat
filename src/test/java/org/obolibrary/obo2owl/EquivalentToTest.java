package org.obolibrary.obo2owl;

import static junit.framework.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * @author cjm
 * 
 * see 5.9.3 and 8.2.2 of spec
 * 
 * See http://code.google.com/p/oboformat/issues/detail?id=13
 *
 */
public class EquivalentToTest extends OboFormatTestBasics {

	@BeforeClass
	public static void beforeClass() {
		Logger.getRootLogger().setLevel(Level.ERROR);		
	}
	
	@Test
	public void testConvert() throws Exception {

		// PARSE TEST FILE
		OWLOntology ontology = convert(parseOBOFile("equivtest.obo"));

		// TEST CONTENTS OF OWL ONTOLOGY
		if (true) {
			Set<OWLEquivalentClassesAxiom> ecas = ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES);
			boolean ok = false;
			for (OWLEquivalentClassesAxiom eca : ecas) {
				System.out.println(eca);
			}
			//assertTrue(ecas.size() == 3);
		}

		// CONVERT BACK TO OBO
		Owl2Obo owl2obo = new Owl2Obo();
		OBODoc obodoc = owl2obo.convert(ontology);
		checkOBODoc(obodoc);

		// ROUNDTRIP AND TEST AGAIN
		String fn = "/tmp/equivtest.obo";
		OBOFormatWriter w = new OBOFormatWriter();
		FileOutputStream os = new FileOutputStream(new File(fn));
		OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
		BufferedWriter bw = new BufferedWriter(osw);
		w.write(obodoc, bw);
		bw.close();
		OBOFormatParser p = new OBOFormatParser();
		obodoc = p.parse(fn);
		checkOBODoc(obodoc);
		
	}
	
	public void checkOBODoc(OBODoc obodoc) {
		// OBODoc tests
		
		// test ECA between named classes is persisted using correct tag
		if (true) {
			Frame tf = obodoc.getTermFrame("X:1");
			Collection<Clause> cs = tf.getClauses(OboFormatTag.TAG_EQUIVALENT_TO);
			assertTrue(cs.size() == 1);
			Object v = cs.iterator().next().getValue();
			System.out.println("V="+v);
			assertTrue(v.equals("X:2")); 
		}
		// test ECA between named class and anon class is persisted as genus-differentia intersection_of tags
		if (true) {
			Frame tf = obodoc.getTermFrame("X:1");
			Collection<Clause> cs = tf.getClauses(OboFormatTag.TAG_INTERSECTION_OF);
			assertTrue(cs.size() == 2);
			boolean okGenus = false;
			boolean okDifferentia = false;
			for (Clause c : cs) {
				Collection<Object> vs = c.getValues();
				if (vs.size() == 2) {
					if (c.getValue().equals("R:1") && c.getValue2().equals("Z:1")) {
						okDifferentia = true;
					}
					
				}
				else if (vs.size() == 1) {
					if (c.getValue().equals("Y:1")) {
						okGenus = true;
					}
				}
				else {
					assertTrue(false);
				}
			}
			assertTrue(okGenus);
			assertTrue(okDifferentia);
		}
		// check reciprocal direction
		if (true) {
			Frame tf2 = obodoc.getTermFrame("X:2");
			System.out.println(tf2);
			Collection<Clause> cs2 = tf2.getClauses(OboFormatTag.TAG_EQUIVALENT_TO);
			Frame tf1 = obodoc.getTermFrame("X:1");
			System.out.println(tf1);
			Collection<Clause> cs1 = tf1.getClauses(OboFormatTag.TAG_EQUIVALENT_TO);
			boolean ok = false;
			if (cs2.size() == 1) {
				if (cs2.iterator().next().getValue(String.class).equals("X:1")) {
					ok = true;
				}
			}
			if (cs1.size() == 1) {
				if (cs1.iterator().next().getValue(String.class).equals("X:2")) {
					ok = true;
				}
			}
			assertTrue(ok);
		}	
	}

}
