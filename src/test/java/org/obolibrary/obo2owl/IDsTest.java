package org.obolibrary.obo2owl;

import static junit.framework.Assert.*;

import org.junit.Test;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.Frame.FrameType;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class IDsTest {

	@Test
	public void testIDs() throws OWLOntologyCreationException{
		OBODoc doc = new OBODoc();
		Frame header = new Frame(FrameType.HEADER);
		Clause c = new Clause();
		c.setTag(OboFormatTag.TAG_ONTOLOGY.getTag());
		c.setValue("test");
		header.addClause(c);
		doc.setHeaderFrame(header);
		
		Obo2Owl obo2owl = new Obo2Owl();
		Owl2Obo owl2Obo= new Owl2Obo();
		
		OWLOntology ontology= obo2owl.convert(doc);
		
		
		owl2Obo.convert(ontology);
		
		//Obo 2 OWL
		IRI iri= obo2owl.oboIdToIRI("GO:001");
		assertTrue("http://purl.obolibrary.org/obo/GO_001".equals(iri.toString()));

		//OWL 2 obo 
		String oboId = Owl2Obo.getIdentifier(iri);
		assertTrue("GO:001".equals(oboId));
		
		
		iri= obo2owl.oboIdToIRI("My_Ont:FOO_002");
		assertTrue("http://purl.obolibrary.org/obo/My_Ont#_FOO_002".equals(iri.toString()));

		oboId = Owl2Obo.getIdentifier(iri);
				assertTrue("My_Ont:FOO_002".equals(oboId));
	
		

		iri= obo2owl.oboIdToIRI("My_Ont:002");
		assertTrue("http://purl.obolibrary.org/obo/My_Ont_002".equals(iri.toString()));


		
		//OWL 2 obo 
		oboId = Owl2Obo.getIdentifier(iri);
		System.out.println("roundtrip:"+oboId);
		assertTrue("My_Ont:002".equals(oboId));
		
		// unprefixed IDs are prefixed with the current ontology ID
		iri= obo2owl.oboIdToIRI("003");
		assertTrue("http://purl.obolibrary.org/obo/test#003".equals(iri.toString()));


		//OWL 2 obo 
		oboId = Owl2Obo.getIdentifier(iri);
		assertTrue("003".equals(oboId));
	
		// arbitrary URL to obo ID
		oboId = Owl2Obo.getIdentifier(IRI.create("http://purl.obolibrary.org/obo/alternate#abcdef"));
		// todo - test this
		//System.out.println("== "+oboId);
		
		iri= obo2owl.oboIdToIRI("part_of");
		assertTrue("http://purl.obolibrary.org/obo/test#part_of".equals(iri.toString()));

		//OWL 2 obo 
		oboId = Owl2Obo.getIdentifier(iri);
		assertTrue("part_of".equals(oboId));
		
		
		iri= obo2owl.oboIdToIRI("OBO_REL:part_of");
		assertTrue("http://purl.obolibrary.org/obo/OBO_REL#_part_of".equals(iri.toString()));

		//OWL 2 obo 
		oboId = Owl2Obo.getIdentifier(iri);
		assertTrue("OBO_REL:part_of".equals(oboId));
		
		
		iri= obo2owl.oboIdToIRI("http://purl.obolibrary.org/testont");
		assertTrue("http://purl.obolibrary.org/testont".equals(iri.toString()));
		
		//OWL 2 obo 
		oboId = Owl2Obo.getIdentifier(iri);
		assertTrue("http://purl.obolibrary.org/testont".equals(oboId));
		

		iri= obo2owl.oboIdToIRI("http://purl.obolibrary.org/obo/BFO_0000050");
		assertTrue("http://purl.obolibrary.org/obo/BFO_0000050".equals(iri.toString()));
		
		//OWL 2 obo 
		oboId = Owl2Obo.getIdentifier(iri);
		assertTrue("BFO:0000050".equals(oboId));
		
		// MGI IDs are perverse - they have a double-separator
		iri= obo2owl.oboIdToIRI("MGI:MGI:1");
		assertTrue("http://purl.obolibrary.org/obo/MGI_MGI%3A1".equals(iri.toString()));
		
		//OWL 2 obo 
		oboId = Owl2Obo.getIdentifier(iri);
		assertTrue("MGI:MGI:1".equals(oboId));
		
		
	}
	
}
