package org.obolibrary.obo2owl;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Set;

import org.coode.owlapi.functionalparser.OWLFunctionalSyntaxOWLParser;
import org.coode.owlapi.functionalrenderer.OWLFunctionalSyntaxRenderer;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.io.OWLRendererException;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.UnloadableImportException;

/**
 * Tools to read and write a set of owl axioms to/from a string.
 * Used to preserve untranslatable axioms in an owl2obo conversion.
 */
public class OwlStringTools {
	
	/**
	 * Exception indicating an un-recoverable error during the handling of axiom strings.
	 */
	public static class OwlStringException extends Exception {

		// generated
		private static final long serialVersionUID = 5909865427436329918L;

		/**
		 * @param cause
		 */
		protected OwlStringException(Throwable cause) {
			super(cause);
		}
		
	}

	/**
	 * Create a string for the given set of axioms. Return null for empty sets
	 * or if the set is null.
	 * 
	 * @param axioms
	 * @return string or null
	 * @throws OwlStringException
	 * 
	 * @see #translate(String)
	 */
	public static String translate(Set<OWLAxiom> axioms) throws OwlStringException {
		if (axioms == null || axioms.isEmpty()) {
			return null;
		}
		try {
			OWLOntologyManager translationManager = OWLManager.createOWLOntologyManager();
			OWLOntology ontology = translationManager.createOntology();
			translationManager.addAxioms(ontology, axioms);
			OWLFunctionalSyntaxRenderer r = new OWLFunctionalSyntaxRenderer();
			Writer writer = new StringWriter();
			r.render(ontology, writer);
			return writer.toString();
		} catch (OWLRendererException e) {
			throw new OwlStringException(e);
		} catch (OWLOntologyCreationException e) {
			throw new OwlStringException(e);
		}
	}

	/**
	 * Parse the axioms from the given axiom string. Returns null for empty and null strings.
	 * 
	 * @param axioms
	 * @return set of axioms or null
	 * @throws OwlStringException
	 * 
	 * @see #translate(Set)
	 */
	public static Set<OWLAxiom> translate(String axioms) throws OwlStringException {
		if (axioms == null || axioms.isEmpty()) {
			return null;
		}
		try {
			OWLOntologyManager translationManager = OWLManager.createOWLOntologyManager();
			OWLFunctionalSyntaxOWLParser p = new OWLFunctionalSyntaxOWLParser();
			OWLOntologyDocumentSource documentSource = new StringDocumentSource(axioms);
			OWLOntology ontology = translationManager.createOntology();
			p.parse(documentSource, ontology);
			return ontology.getAxioms();
		} catch (UnloadableImportException e) {
			throw new OwlStringException(e);
		} catch (OWLOntologyCreationException e) {
			throw new OwlStringException(e);
		} catch (OWLParserException e) {
			throw new OwlStringException(e);
		} catch (IOException e) {
			throw new OwlStringException(e);
		}

	}
}
