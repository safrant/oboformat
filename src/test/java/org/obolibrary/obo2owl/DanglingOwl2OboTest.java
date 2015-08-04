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

public class DanglingOwl2OboTest extends OboFormatTestBasics {

	@BeforeClass
	public static void beforeClass() {
		Logger.getRootLogger().setLevel(Level.ERROR);		
	}
	
	@Test
	public void testConversion() throws Exception{
		OBODoc doc = convert(parseOWLFile("dangling_owl2_obo_test.owl"));
		
		Frame f = doc.getTermFrame("UBERON:0000020");
		System.out.println("F="+f);
		Clause rc = f.getClause(OboFormatTag.TAG_NAME);
		assertTrue(rc.getValue().equals("sense organ"));
		Collection<Clause> ics = f.getClauses(OboFormatTag.TAG_INTERSECTION_OF);
		assertTrue(ics.size() == 2);
		
		writeOBO(doc, "dangling_owl2_obo_test.owl.obo");
	}
}
