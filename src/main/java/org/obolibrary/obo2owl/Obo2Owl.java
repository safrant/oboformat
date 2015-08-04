package org.obolibrary.obo2owl;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.obolibrary.obo2owl.Obo2OWLConstants.Obo2OWLVocabulary;
import org.obolibrary.obo2owl.OwlStringTools.OwlStringException;
import org.obolibrary.oboformat.model.*;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.obolibrary.oboformat.parser.OBOFormatConstants;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.vocab.Namespaces;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

public class Obo2Owl {



	private static Logger LOG = Logger.getLogger(Obo2Owl.class);

	public static final boolean DEBUG = LOG.isDebugEnabled();

	/**
	 * Use conversion methods or Obo2OWLConstants.DEFAULT_IRI_PREFIX 
	 */
	@Deprecated
	public static final String DEFAULT_IRI_PREFIX = Obo2OWLConstants.DEFAULT_IRI_PREFIX;

	//	public static final String IRI_CLASS_SYNONYMTYPEDEF = Obo2OWLConstants.DEFAULT_IRI_PREFIX + "IAO_synonymtypedef";
	//	public static final String IRI_CLASS_SUBSETDEF = Obo2OWLConstants.DEFAULT_IRI_PREFIX + "IAO_subsetdef";

	public static final String IRI_PROP_isReversiblePropertyChain =  Obo2OWLConstants.DEFAULT_IRI_PREFIX + "IAO_isReversiblePropertyChain";

//	private String defaultIDSpace = "";
	OWLOntologyManager manager;
	OWLOntology owlOntology;
	OWLDataFactory fac;
	OBODoc obodoc;
	final Map<String,String> idSpaceMap;
	public static Map<String,IRI> annotationPropertyMap = initAnnotationPropertyMap();
	final Set<OWLAnnotationProperty> apToDeclare;
	private final Map<String, OWLClass> clsToDeclar;

	final private Map<String, OWLAnnotationProperty> typedefToAnnotationProperty;

	public Obo2Owl() {
		// as default create an empty manager
		this(OWLManager.createOWLOntologyManager());
	}

	public Obo2Owl(OWLOntologyManager manager) {
		idSpaceMap = new HashMap<String,String>();
		apToDeclare = new HashSet<OWLAnnotationProperty>();
		clsToDeclar = new Hashtable<String, OWLClass>();
		typedefToAnnotationProperty = new Hashtable<String, OWLAnnotationProperty>();
		init(manager);
	}

	private void init(OWLOntologyManager manager) {
		// use the given manager and its factory
		this.manager = manager;
		fac = this.manager.getOWLDataFactory();

		// clear all internal maps.
		idSpaceMap.clear();
		apToDeclare.clear();
		clsToDeclar.clear();
		typedefToAnnotationProperty.clear();
	}



	/**
	 * Static convenience method which:
	 *  (1) creates an Obo2Owl bridge object
	 *  (2) parses an obo file from a URL
	 *  (3) converts that to an OWL ontology
	 *  (4) saves the OWL ontology as RDF/XML
	 *  
	 * @param iri
	 * @param outFile
	 * @throws IOException
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 * @throws OBOFormatParserException
	 */
	public static void convertURL(String iri, String outFile) throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, OBOFormatParserException {
		Obo2Owl bridge = new Obo2Owl();
		OWLOntologyManager manager = bridge.getManager();
		OBOFormatParser p = new OBOFormatParser();
		OBODoc obodoc = p.parse(new URL(iri));
		OWLOntology ontology = bridge.convert(obodoc);
		IRI outputStream = IRI.create(outFile);
		OWLOntologyFormat format = new RDFXMLOntologyFormat();

		if(DEBUG)
			LOG.debug("saving to "+outputStream+" fmt="+format);

		manager.saveOntology(ontology, format, outputStream);

	}

	/**
	 * See {@link #convertURL(String iri, String outFile)}
	 * 
	 * @param iri
	 * @param outFile
	 * @param defaultOnt -- e.g. "go". If the obo file contains no "ontology:" header tag, this is added 
	 * @throws IOException
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 * @throws OBOFormatParserException 
	 */
	public static void convertURL(String iri, String outFile, String defaultOnt) throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, OBOFormatParserException {
		Obo2Owl bridge = new Obo2Owl();
		OWLOntologyManager manager = bridge.getManager();
		OBOFormatParser p = new OBOFormatParser();
		OBODoc obodoc = p.parse(new URL(iri));
		obodoc.addDefaultOntologyHeader(defaultOnt);
		OWLOntology ontology = bridge.convert(obodoc);
		IRI outputStream = IRI.create(outFile);
		OWLOntologyFormat format = new RDFXMLOntologyFormat();
		if(DEBUG)
			LOG.debug("saving to "+outputStream+" fmt="+format);
		manager.saveOntology(ontology, format, outputStream);

	}


	/**
	 * Table 5.8 Translation of Annotation Vocabulary.
	 * 
	 * @return property map
	 */
	private static HashMap<String, IRI> initAnnotationPropertyMap() {
//TODO  adjust the namespace and annotation declarations here?
		HashMap<String, IRI> map = new HashMap<String, IRI>();
		map.put(OboFormatTag.TAG_IS_OBSELETE.getTag(),OWLRDFVocabulary.OWL_DEPRECATED.getIRI());
		map.put(OboFormatTag.TAG_NAME.getTag(),OWLRDFVocabulary.RDFS_LABEL.getIRI());
		map.put(OboFormatTag.TAG_COMMENT.getTag(),OWLRDFVocabulary.RDFS_COMMENT.getIRI());

		for(Obo2OWLVocabulary vac: Obo2OWLVocabulary.values()){
			map.put(vac.getMappedTag(), vac.getIRI());
		}

		/*	map.put("expand_expression_to",Obo2OWLVocabulary.IRI_IAO_0000424.getIRI());
		map.put("expand_assertion_to",Obo2OWLVocabulary.IRI_IAO_0000425.getIRI());
		map.put("def",Obo2OWLVocabulary.IRI_IAO_0000115.getIRI());
		map.put("synonym",Obo2OWLVocabulary.IRI_IAO_0000118.getIRI());
		map.put("is_anti_symmetric",Obo2OWLVocabulary.IRI_IAO_0000427.getIRI());
		map.put("replaced_by", Obo2OWLVocabulary.IRI_IAO_0100001.getIRI());*/

		return map;
	}

	public OWLOntologyManager getManager() {
		return manager;
	}



	public void setManager(OWLOntologyManager manager) {
		this.manager = manager;
	}



	public OBODoc getObodoc() {
		return obodoc;
	}



	public void setObodoc(OBODoc obodoc) {
		this.obodoc = obodoc;
	}

	/**
	 * @return the owlOntology
	 */
	protected OWLOntology getOwlOntology() {
		return owlOntology;
	}

	/**
	 * @param owlOntology the owlOntology to set
	 */
	protected void setOwlOntology(OWLOntology owlOntology) {
		this.owlOntology = owlOntology;
	}

	/**
	 * Creates an OBOFormatParser object to parse a file and then converts it using
	 * the convert method
	 * 
	 * @param oboFile
	 * @return ontology
	 * @throws OWLOntologyCreationException
	 */
	public OWLOntology convert(String oboFile) throws OWLOntologyCreationException {

		try{
			OBOFormatParser p = new OBOFormatParser();

			OBODoc obodoc = p.parse(oboFile);

			return convert(obodoc);
		}catch(IOException ex){
			throw new OWLOntologyCreationException("Error Occured while parsing OBO '" + oboFile + "'", ex);
		} catch (OBOFormatParserException ex) {
			throw new OWLOntologyCreationException("Syntax error occured while parsing OBO '" + oboFile + "'", ex);
		}

	}

	/**
	 * @param obodoc
	 * @param useFreshManager
	 * @return ontology
	 * @throws OWLOntologyCreationException
	 */
	public OWLOntology convert(OBODoc obodoc, boolean useFreshManager) throws OWLOntologyCreationException {
		this.obodoc = obodoc;
		if (useFreshManager)
			init(OWLManager.createOWLOntologyManager());
		else
			init(manager);
		return tr();
	}


	/**
	 * Converts an OBO document to an OWL ontology 
	 * 
	 * @param obodoc
	 * @return ontology
	 * @throws OWLOntologyCreationException
	 */
	public OWLOntology convert(OBODoc obodoc) throws OWLOntologyCreationException {
		// always create a new manager, when loading a new obo document - TODO - why?
		//                         creates problems for sharing ontologies
		return convert(obodoc, false);
	}



	private OWLOntology tr() throws OWLOntologyCreationException {

		Frame hf = obodoc.getHeaderFrame();
		Clause ontClause = hf.getClause( OboFormatTag.TAG_ONTOLOGY);
		if (ontClause != null) {
			String ontOboId = (String) ontClause.getValue();
//			defaultIDSpace = ontOboId;
			IRI ontIRI;
			if (ontOboId.contains(":")) {
				ontIRI = IRI.create(ontOboId);
			}
			else { //This is where the default OWL namespace is created
				ontIRI = IRI.create(Obo2OWLConstants.OIOVOCAB_IRI_PREFIX);
//				ontIRI = IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+ontOboId);
				
			}

			Clause dvclause = hf.getClause(OboFormatTag.TAG_DATA_VERSION);
			if (dvclause != null) {
				String dv = dvclause.getValue().toString();
//TODO this version is a little ugly.  Will want to edit this
//				IRI vIRI = IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+ontOboId+"/"+dv+"/"+ontOboId+".owl");
				IRI vIRI = IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+ontOboId+"/"+dv+"/"+ontOboId);
				OWLOntologyID oid = new OWLOntologyID(ontIRI, vIRI);
				owlOntology = manager.createOntology(oid);
			}
			else {

				owlOntology = manager.createOntology(ontIRI);
			}
		}
		else {
//			defaultIDSpace = "TEMP";
//			IRI ontIRI = IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX + defaultIDSpace);
			IRI ontIRI = IRI.create(Obo2OWLConstants.OIOVOCAB_IRI_PREFIX);

			// TODO - warn
			owlOntology = manager.createOntology(ontIRI);

		}
		trHeaderFrame(hf);

		for (Frame f : obodoc.getTypedefFrames()) {
			trTypedefToAnnotationProperty(f);
		}


		for (Frame f : obodoc.getTypedefFrames()) {
			trTypedefFrame(f);
		}

		for (Frame f : obodoc.getTermFrames()) {
			trTermFrame(f);
		}
		// TODO - individuals

		for(Clause cl: hf.getClauses(OboFormatTag.TAG_IMPORT)){
			String path = getURI(cl.getValue().toString());

			IRI importIRI = IRI.create(path);
			manager.loadOntology(importIRI);
			AddImport ai = new AddImport(this.owlOntology, fac.getOWLImportsDeclaration(importIRI));
			manager.applyChange(ai);
		}

		postProcess(owlOntology);

		return owlOntology;
	}

	/**
	 * perform any necessary post-processing
	 * 
	 * currently this only includes the experimental logical-definitions-view-property
	 * 
	 * @param owlOntology
	 */
	private void postProcess(OWLOntology owlOntology) {
		IRI pIRI = null;
		for (OWLAnnotation ann : owlOntology.getAnnotations()) {
			if (ann.getProperty().getIRI().equals(Obo2OWLVocabulary.IRI_OIO_LogicalDefinitionViewRelation.getIRI())) {
				OWLAnnotationValue v = ann.getValue();
				if (v instanceof OWLLiteral) {
					String rel = ((OWLLiteral)v).getLiteral();
					pIRI  = this.oboIdToIRI(rel);
				}
				else {
					pIRI = (IRI) v;
				}
				break;
			}
		}
		if (pIRI != null) {
			OWLObjectProperty vp = fac.getOWLObjectProperty(pIRI);
			Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
			Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();

			for (OWLEquivalentClassesAxiom eca : owlOntology.getAxioms(AxiomType.EQUIVALENT_CLASSES)) {
				int numNamed = 0;
				Set<OWLClassExpression> xs = new HashSet<OWLClassExpression>();
				for (OWLClassExpression x : eca.getClassExpressions()) {
					if (x instanceof OWLClass) {
						xs.add(x);
						numNamed ++;
						continue;
					}
					// anonymous class expressions are 'prefixed' with view property
					xs.add(fac.getOWLObjectSomeValuesFrom(vp, x));
				}
				if (numNamed == 1) {
					rmAxioms.add(eca);
					newAxioms.add(fac.getOWLEquivalentClassesAxiom(xs));
				}
			}
			manager.removeAxioms(owlOntology, rmAxioms);
			manager.addAxioms(owlOntology, newAxioms);
		}
	}

	private  String getURI(String path){
		if(path.startsWith("http://") || path.startsWith("file:"))
			return  path;

		File f = new File(path);

		return f.toURI().toString();

	}


	public void trHeaderFrame(Frame headerFrame) {
		
		for (String t: headerFrame.getTags()) {
			OboFormatTag tag = OBOFormatConstants.getTag(t);

			if (tag == OboFormatTag.TAG_ONTOLOGY) {

				// already processed
			}
			else if (tag == OboFormatTag.TAG_IMPORT) {
				// TODO
				//fac.getOWLImportsDeclaration(importedOntologyIRI);
				//	manager.applyChange(new AddImport(baseOnt, manager.getOWLDataFactory()
				//		.getOWLImportsDeclaration(importedIRI)));
			} else if (tag == OboFormatTag.TAG_SUBSETDEF){

				OWLAnnotationProperty parentAnnotProp = trTagToAnnotationProp(t);
				/*
				OWLClass cls = clsToDeclar.get(t);
				if(cls == null){
					cls = trClass(trTagToIRI(t).toString());
					add(fac.getOWLDeclarationAxiom(cls));
					clsToDeclar.put(t, cls);
				}
				 */

				for(Clause clause: headerFrame.getClauses(t)){

					OWLAnnotationProperty childAnnotProp = trAnnotationProp( clause.getValue(String.class) );
					Set<OWLAnnotation> annotations = trAnnotations(clause);
					add(fac.getOWLSubAnnotationPropertyOfAxiom(childAnnotProp, parentAnnotProp, annotations));
					//OWLIndividual indv= trIndividual(  clause.getValue().toString() );
					//add (fac.getOWLClassAssertionAxiom(cls, indv) );

					OWLAnnotationProperty ap = trTagToAnnotationProp(OboFormatTag.TAG_COMMENT.getTag());
					add(fac.getOWLAnnotationAssertionAxiom(ap, childAnnotProp.getIRI(), trLiteral(clause.getValue2())));
				}
			} else if (tag == OboFormatTag.TAG_SYNONYMTYPEDEF){
				OWLAnnotationProperty parentAnnotProp = this.trTagToAnnotationProp(t);

				for(Clause clause: headerFrame.getClauses(t)){
					Object values[] = clause.getValues().toArray();
					OWLAnnotationProperty childAnnotProp = this.trAnnotationProp( values[0].toString() );
					IRI childIRI = childAnnotProp.getIRI();
					
					Set<OWLAnnotation> annotations = trAnnotations(clause);
					add(fac.getOWLSubAnnotationPropertyOfAxiom(childAnnotProp, parentAnnotProp, annotations));

					OWLAnnotationProperty ap = trTagToAnnotationProp(OboFormatTag.TAG_NAME.getTag());
					add(fac.getOWLAnnotationAssertionAxiom(ap, childIRI, trLiteral( values[1] )));

					if(values.length>2 && values[2].toString().length() > 0){
						ap = trTagToAnnotationProp(OboFormatTag.TAG_SCOPE.getTag());
						add (fac.getOWLAnnotationAssertionAxiom(ap, childIRI, 
								trTagToAnnotationProp( values[2].toString() ).getIRI()
						));
					}
				}

			} else if (tag == OboFormatTag.TAG_DATE) {
				Clause clause = headerFrame.getClause(tag);
				Object value = clause.getValue();
				String dateString = null;
				if (value instanceof Date) {
					dateString = OBOFormatConstants.headerDateFormat.get().format((Date) value);
				}
				else if (value instanceof String) {
					dateString = (String) value;
				}
				if (dateString != null) {
					addOntologyAnnotation(trTagToAnnotationProp(t), trLiteral(dateString), trAnnotations(clause));
				}
				else {
					// TODO: Throw Exceptions
					LOG.warn("Cannot translate: "+clause);
				}
			} else if (tag == OboFormatTag.TAG_PROPERTY_VALUE) {
				addPropertyValueHeaders(headerFrame.getClauses(OboFormatTag.TAG_PROPERTY_VALUE));
			} else if (tag == OboFormatTag.TAG_DATA_VERSION) {
				/*
				Clause clause = headerFrame.getClause(tag);
				String dv = clause.getValue().toString();

				String ontOboId = headerFrame.getClause(OboFormatTag.TAG_ONTOLOGY).getValue().toString();
				IRI vIRI = IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+ontOboId+"/"+dv+"/"+ontOboId+".owl");
				System.out.println("Adding versionIRI "+vIRI);
				addOntologyAnnotation(fac.getOWLVersionInfo(), fac.getOWLLiteral(vIRI.toString(), OWL2Datatype.XSD_ANY_URI));
				 */

			} else if ( tag == OboFormatTag.TAG_REMARK) {
				// translate remark as rdfs:comment
				Collection<Clause> clauses = headerFrame.getClauses(t);
				for(Clause clause: clauses){
					addOntologyAnnotation(fac.getRDFSComment(), trLiteral(clause.getValue()), trAnnotations(clause));
				}
			
			} else if ( tag == OboFormatTag.TAG_IDSPACE) {
				// do not translate, as they are just directives? TODO ask Chris
			
			} else if ( tag == OboFormatTag.TAG_OWL_AXIOMS ) {
				// in theory, there should only be one tag
				// but we can silently collapse multiple tags
				Collection<String> axiomStrings = headerFrame.getTagValues(tag, String.class);
				if (axiomStrings != null) {
					try {
						for (String axiomString : axiomStrings) {
							Set<OWLAxiom> axioms = OwlStringTools.translate(axiomString);
							if (axioms != null) {
								manager.addAxioms(owlOntology, axioms);
							}
						}
					} catch (OwlStringException e) {
						throw new RuntimeException(e);
					}
				}
			
			} else{
				Collection<Clause> clauses = headerFrame.getClauses(t);

				for(Clause clause: clauses){
					addOntologyAnnotation(trTagToAnnotationProp(t), trLiteral(clause.getValue()), trAnnotations(clause));
				}
			}
		}		
	}

	private void addPropertyValueHeaders(Collection<Clause> clauses) {
		for (Clause clause : clauses) {
			final Set<OWLAnnotation> annotations = trAnnotations(clause);
			Collection<Object> values = clause.getValues();
			Object v = clause.getValue();
			Object v2 = clause.getValue2();
			if (v == null) {
				// TODO: Throw Exceptions
				LOG.warn("Cannot translate: "+clause);
			} 
			else if (values.size() == 2) {
				// property_value(Rel-ID Entity-ID Qualifiers)
				OWLAnnotationProperty prop = trAnnotationProp((String)v);
				OWLAnnotationValue value = trAnnotationProp(v2.toString()).getIRI();
				OWLAnnotation ontAnn = fac.getOWLAnnotation(prop, value, annotations);
				AddOntologyAnnotation addAnn = new AddOntologyAnnotation(owlOntology, ontAnn);
				apply(addAnn);
			} 
			else if (values.size() == 3) {
				// property_value(Rel-ID Value XSD-Type Qualifiers)
				Iterator<Object> it = clause.getValues().iterator();
				it.next();
				it.next();
				String v3String = (String) it.next();
				IRI valueIRI;
				if (v3String.startsWith("xsd:")) {
					valueIRI = IRI.create(Namespaces.XSD + v3String.substring(4));
				} else {
					valueIRI = IRI.create(v3String);
				}
				OWLAnnotationValue value = fac.getOWLLiteral((String) v2, OWL2Datatype.getDatatype(valueIRI));
				OWLAnnotationProperty prop = trAnnotationProp((String)v);
				OWLAnnotation ontAnn = fac.getOWLAnnotation(prop, value, annotations);
				AddOntologyAnnotation addAnn = new AddOntologyAnnotation(owlOntology, ontAnn);
				apply(addAnn);
			}
			else {
				LOG.warn("Cannot translate: "+clause);
				// TODO 
			}
		}
	}

	protected void addOntologyAnnotation(OWLAnnotationProperty ap, OWLAnnotationValue v, Set<OWLAnnotation> annotations) {
		OWLAnnotation ontAnn = fac.getOWLAnnotation(ap, v, annotations);
		AddOntologyAnnotation addAnn = new AddOntologyAnnotation(owlOntology, ontAnn);
		apply(addAnn);
	}

	public OWLClassExpression trTermFrame(Frame termFrame) {
		OWLClass cls = EVS_trClass(termFrame.getId());
		add(fac.getOWLDeclarationAxiom(cls));
		for (String t : termFrame.getTags()) {
			//System.out.println("tag:"+tag);
			Collection<Clause> clauses = termFrame.getClauses(t);
			Set<OWLAxiom> axioms = trTermFrameClauses(cls, clauses, t);
			if (axioms.isEmpty() == false) {
				add(axioms);
			}
		}
		return cls;
	}
	//TODO - Do we need to adjust equivalent classes?
	public Set<OWLAxiom> trTermFrameClauses(OWLClass cls, Collection<Clause> clauses, String t) {
		OboFormatTag tag = OBOFormatConstants.getTag(t);
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		if (tag == OboFormatTag.TAG_INTERSECTION_OF) {
			axioms.add(trIntersectionOf(cls,clauses));
		}
		else if (tag == OboFormatTag.TAG_UNION_OF) {
			axioms.add(trUnionOf(cls,clauses));
		}
		else {
			for (Clause clause : clauses) {
				axioms.add(trTermClause(cls,t,clause));
			}
		}
		return axioms;
	}


	private OWLNamedObject trTypedefToAnnotationProperty(Frame typedefFrame){

		if (typedefFrame.getTagValue(OboFormatTag.TAG_IS_METADATA_TAG) != null &&
				(Boolean)typedefFrame.getTagValue(OboFormatTag.TAG_IS_METADATA_TAG)) {

			final String id = typedefFrame.getId();
			OWLAnnotationProperty p = trAnnotationProp(id);
			add(fac.getOWLDeclarationAxiom(p));

			// handle xrefs also for meta data tags
			final String xid = translateShorthandIdToExpandedId(id);
			if (id.equals(xid) == false) {
				OWLAxiom ax = fac.getOWLAnnotationAssertionAxiom(
						trTagToAnnotationProp("shorthand"),
						p.getIRI(), 
						trLiteral(id), 
						new HashSet<OWLAnnotation>());

				add(ax);				
			}

			typedefToAnnotationProperty.put(p.getIRI().toString(), p);

			for (String tag : typedefFrame.getTags()) {
				OboFormatTag _tag = OBOFormatConstants.getTag(tag);

				if (_tag == OboFormatTag.TAG_IS_A) {
					// todo - subAnnotationProperty
					/*
					OWLAxiom ax = fac.getOWLSubAnnotationPropertyOfAxiom(
							p,
							trObjectProp((String)typedefFrame.getC), 
							annotations);
					 */
				}
				else {
					for (Clause clause : typedefFrame.getClauses(tag)) {
						//System.out.println(p+" p "+tag+" t "+clause);
						add(trGenericClause(p, tag, clause));					
					}
				}
			}
			return p;
		}		

		return null;
	}


	public OWLNamedObject trTypedefFrame(Frame typedefFrame) {
		// TODO - annotation props
		if (typedefFrame.getTagValue(OboFormatTag.TAG_IS_METADATA_TAG) != null &&
				(Boolean)typedefFrame.getTagValue(OboFormatTag.TAG_IS_METADATA_TAG)) {

			// already handled
			// see: trTypedefToAnnotationProperty(Frame typedefFrame)
			return null;
		}
		else {
			final String id = typedefFrame.getId();
			OWLObjectProperty p = trObjectProp(id);
			add(fac.getOWLDeclarationAxiom(p));

			

			String xid = translateShorthandIdToExpandedId(id);
			if (xid.equals(id) == false) {
				OWLAxiom ax = fac.getOWLAnnotationAssertionAxiom(
						trTagToAnnotationProp("shorthand"),
						p.getIRI(), 
						trLiteral(id), 
						new HashSet<OWLAnnotation>());

				add(ax);				
			}

			/*
			// See 5.9.3 Special Rules for Relations
			Collection<Xref> xrefs = typedefFrame.getTagValues(OboFormatTag.TAG_XREF, Xref.class);
			String xrefStr = null;
			for (Xref xref: xrefs) {
				if (xref != null) {
					String xid = xref.getIdref();

					if ((xid.startsWith("RO") ||
							xid.startsWith("BFO")) &&
							!xid.equals(id)) {
						// RO and BFO have special status. 
						// avoid cycles (in case of self-xref)

						//	fac.getOWLAnnotationAssertionAxiom(prop, p.getIRI(), trLiteral(id), new HashSet<OWLAnnotation>());
						OWLAxiom ax = fac.getOWLAnnotationAssertionAxiom(
								trTagToAnnotationProp("shorthand"),
								p.getIRI(), 
								trLiteral(id), 
								new HashSet<OWLAnnotation>());

						add(ax);


						//	return oboIdToIRI(xid);
					}
				}
			}
			 */



			for (String tag : typedefFrame.getTags()) {
				Collection<Clause> clauses = typedefFrame.getClauses(tag);

				OboFormatTag _tag = OBOFormatConstants.getTag(tag);

				if (_tag == OboFormatTag.TAG_INTERSECTION_OF) {
					final OWLAxiom axiom = trRelationIntersectionOf(id, p,clauses);
					if (axiom != null) {
						add(axiom);
					}
				}
				else if (_tag == OboFormatTag.TAG_UNION_OF) {
					final OWLAxiom axiom = trRelationUnionOf(id, p,clauses);
					if (axiom != null) {
						add(axiom);
					}
				}
				else {
					for (Clause clause : clauses) {
						add(trTypedefClause(p,tag,clause));
					}
				}
			}
			return p;
		}
	}


	private OWLAxiom trRelationUnionOf(String id, OWLProperty<?,?> p, Collection<Clause> clauses) {
		// TODO not expressible in OWL - use APs. SWRL?
		LOG.warn("The relation union_of for "+id+" is currently non-translatable to OWL. Ignoring clauses: "+clauses);
		return null;
	}

	private OWLAxiom trRelationIntersectionOf(String id, OWLProperty<?,?> p, Collection<Clause> clauses) {
		// TODO not expressible in OWL - use APs. SWRL?
		LOG.warn("The relation intersection_of for "+id+" is currently non-translatable to OWL. Ignoring clauses: "+clauses);
		return null;
	}

	private OWLAxiom trUnionOf(OWLClass cls, Collection<Clause> clauses) {
		Set<? extends OWLAnnotation> annotations = trAnnotations(clauses);

		Set<OWLClassExpression> eSet;
		eSet = new HashSet<OWLClassExpression>();
		eSet.add(cls);

		Set<OWLClassExpression> iSet;
		iSet = new HashSet<OWLClassExpression>();
		for (Clause clause: clauses) {
			Collection<QualifierValue> qvs = clause.getQualifierValues();
			// TODO - quals
			if (clause.getValues().size() == 1) {
				iSet.add(trClass(clause.getValue()));
			}
			else {
				LOG.error("union_of n-ary slots not is standard - converting anyway");
				//System.err.println("union_of n-ary slots not is standard - converting anyway");
				iSet.add(trRel((String)clause.getValue(),
						(String)clause.getValue2(),
						qvs));

			}
		}
		//out.println(cls+" CL:"+clauses+" I:"+iSet+" E:"+eSet);
		eSet.add(fac.getOWLObjectUnionOf(iSet));
		// TODO - fix this
		if (annotations == null || annotations.size() == 0)
			return fac.getOWLEquivalentClassesAxiom(eSet);
		else
			return fac.getOWLEquivalentClassesAxiom(eSet, annotations);
	}

	private OWLAxiom trIntersectionOf(OWLClass cls, Collection<Clause> clauses) {
		Set<? extends OWLAnnotation> annotations = trAnnotations(clauses);

		Set<OWLClassExpression> eSet;
		eSet = new HashSet<OWLClassExpression>();
		eSet.add(cls);

		Set<OWLClassExpression> iSet;
		iSet = new HashSet<OWLClassExpression>();
		for (Clause clause: clauses) {
			Collection<QualifierValue> qvs = clause.getQualifierValues();
			if (clause.getValues().size() == 1) {
				iSet.add(trClass(clause.getValue()));
			}
			else {
				iSet.add(trRel((String)clause.getValue(),
						(String)clause.getValue2(),
						qvs));

			}
		}
		//out.println(cls+" CL:"+clauses+" I:"+iSet+" E:"+eSet);
		eSet.add(fac.getOWLObjectIntersectionOf(iSet));
		// TODO - fix this
		if (annotations == null || annotations.size() == 0)
			return fac.getOWLEquivalentClassesAxiom(eSet);
		else
			return fac.getOWLEquivalentClassesAxiom(eSet, annotations);
	}


	private void add(OWLAxiom axiom) {
		if (axiom == null) {
			LOG.error("no axiom");
			return;
		}
		add(Collections.singleton(axiom));
	}
	
	protected void add(Set<OWLAxiom> axioms) {
		if (axioms == null || axioms.isEmpty()) {
			LOG.error("no axiom");
			return;
		}
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>(axioms.size());
		for (OWLAxiom axiom : axioms) {
			AddAxiom addAx = new AddAxiom(owlOntology, axiom);
			changes.add(addAx);
		}
		apply(changes);
	}

	private void apply(OWLOntologyChange change) {
		apply(Collections.singletonList(change));
	}
	
	protected void apply(List<OWLOntologyChange> changes) {
		try {
			manager.applyChanges(changes);
		}
		catch (Exception e) {			
			LOG.error(e+"\nCOULD NOT TRANSLATE AXIOM");
		}

	}

	/**
	 * #5.2
	 * 
	 * @param cls
	 * @param tag
	 * @param clause
	 * @return axiom
	 */
	private OWLAxiom trTermClause(OWLClass cls, String tag, Clause clause) {
		OWLAxiom ax;
		Collection<QualifierValue> qvs = clause.getQualifierValues();
		Set<? extends OWLAnnotation> annotations = trAnnotations(clause);
		OboFormatTag _tag = OBOFormatConstants.getTag(tag);

		// 5.2.2
		// The gci_relation qualifier translate cls to a class expression
		OWLClassExpression clsx = cls;
		String gciRel = getQVString("gci_relation", qvs);
		if (gciRel != null && !gciRel.equals("")) {
			String gciFiller = getQVString("gci_filler", qvs);
			OWLClassExpression r = trRel(gciRel, gciFiller, new HashSet<QualifierValue>());
			clsx = fac.getOWLObjectIntersectionOf(cls, r);
		}

		if (_tag == OboFormatTag.TAG_IS_A) {
			ax = fac.getOWLSubClassOfAxiom(clsx, 
					trClass((String)clause.getValue()), 
					annotations);
		}
		else if (_tag == OboFormatTag.TAG_RELATIONSHIP) {
			// TODO

			IRI relId = oboIdToIRI((String)clause.getValue());

			OWLAnnotationProperty prop = typedefToAnnotationProperty.get(relId.toString());

			if(prop != null){
				ax = fac.getOWLAnnotationAssertionAxiom(prop, 
						cls.getIRI(),  
						oboIdToIRI((String)clause.getValue2()), 
						annotations);
			}else{
				ax = fac.getOWLSubClassOfAxiom(clsx, 
						trRel((String)clause.getValue(),
								(String)clause.getValue2(),
								qvs),
								annotations);
			}
		}
		else if (_tag == OboFormatTag.TAG_DISJOINT_FROM) {

			Set<OWLClassExpression> cSet;
			cSet = new HashSet<OWLClassExpression>();
			cSet.add(clsx);
			cSet.add(trClass((String)clause.getValue()));
			ax = fac.getOWLDisjointClassesAxiom(
					cSet, 
					annotations);
		}
		else if (_tag == OboFormatTag.TAG_EQUIVALENT_TO) {

			Set<OWLClassExpression> cSet;
			cSet = new HashSet<OWLClassExpression>();
			cSet.add(clsx);
			cSet.add(trClass((String)clause.getValue()));
			ax = fac.getOWLEquivalentClassesAxiom(
					cSet, 
					annotations);
		}
		
		else if(_tag == OboFormatTag.TAG_IS_OBSELETE && "true".equals(clause.getValue().toString())) {
			ax=fac.getDeprecatedOWLAnnotationAssertionAxiom(cls.getIRI());
		}
		else {
			return trGenericClause(cls, tag, clause);
		}
		return ax;
	}

	// no data properties in obo
	private OWLAxiom trTypedefClause(OWLObjectProperty p, String tag, Clause clause) {
		OWLAxiom ax = null;
		Object v = clause.getValue();
		Set<OWLAnnotation> annotations = trAnnotations(clause);
		OboFormatTag _tag = OBOFormatConstants.getTag(tag);
		if (_tag == OboFormatTag.TAG_IS_A) {
			ax = fac.getOWLSubObjectPropertyOfAxiom(
					p,
					trObjectProp((String)v), 
					annotations);
		}
		else if (_tag == OboFormatTag.TAG_RELATIONSHIP) {
			IRI relId = oboIdToIRI((String)clause.getValue());

			OWLAnnotationProperty metaProp = typedefToAnnotationProperty.get(relId.toString());

			if(metaProp != null){
				ax = fac.getOWLAnnotationAssertionAxiom(metaProp, 
						p.getIRI(),  
						oboIdToIRI((String)clause.getValue2()), 
						annotations);
			}else{
				//System.err.println("no annotation prop:"+relId);
				ax = null; // TODO
			}
		}
		else if (_tag == OboFormatTag.TAG_DISJOINT_FROM) {

			Set<OWLObjectPropertyExpression> cSet;
			cSet = new HashSet<OWLObjectPropertyExpression>();
			cSet.add( p);
			cSet.add( trObjectProp((String)v));
			ax = fac.getOWLDisjointObjectPropertiesAxiom(
					cSet, 
					annotations);
		}
		else if (_tag == OboFormatTag.TAG_INVERSE_OF) {

			Set<OWLObjectPropertyExpression> cSet;
			cSet = new HashSet<OWLObjectPropertyExpression>();
			cSet.add( p);
			cSet.add( trObjectProp((String)v));
			ax = fac.getOWLInverseObjectPropertiesAxiom(p, trObjectProp((String)v), annotations);
		}
		else if (_tag == OboFormatTag.TAG_EQUIVALENT_TO) {

			Set<OWLObjectPropertyExpression> cSet;
			cSet = new HashSet<OWLObjectPropertyExpression>();
			cSet.add(p);
			cSet.add( trObjectProp((String)v));
			ax = fac.getOWLEquivalentObjectPropertiesAxiom(
					cSet, 
					annotations);
		}
		else if (_tag == OboFormatTag.TAG_DOMAIN) {
			ax = fac.getOWLObjectPropertyDomainAxiom(p,
					trClass(v), 
					annotations);
		}
		else if (_tag == OboFormatTag.TAG_RANGE) {
			ax = fac.getOWLObjectPropertyRangeAxiom(p,
					trClass(v), 
					annotations);
		}
		else if (_tag == OboFormatTag.TAG_TRANSITIVE_OVER) {
			List<OWLObjectPropertyExpression> chain = new ArrayList<OWLObjectPropertyExpression>(2);
			chain.add(p);
			chain.add(trObjectProp(v));
			ax = fac.getOWLSubPropertyChainOfAxiom(chain, p, annotations);
		}
		else if (_tag == OboFormatTag.TAG_HOLDS_OVER_CHAIN || _tag == OboFormatTag.TAG_EQUIVALENT_TO_CHAIN) {

			if(_tag == OboFormatTag.TAG_EQUIVALENT_TO_CHAIN){
				OWLAnnotation ann = fac.getOWLAnnotation(trAnnotationProp(IRI_PROP_isReversiblePropertyChain), trLiteral("true"));
				annotations.add(ann);
				// isReversiblePropertyChain
			}

			List<OWLObjectPropertyExpression> chain =
				new Vector<OWLObjectPropertyExpression>();
			chain.add(trObjectProp(v));
			chain.add(trObjectProp(clause.getValue2()));
			ax = fac.getOWLSubPropertyChainOfAxiom(chain , p, annotations);
			//System.out.println("chain:"+ax);
			// TODO - annotations for equivalent to
		}else if (_tag == OboFormatTag.TAG_IS_TRANSITIVE && "true".equals(clause.getValue().toString())) {
			ax = fac.getOWLTransitiveObjectPropertyAxiom(p, annotations);
		}
		else if (_tag == OboFormatTag.TAG_IS_REFLEXIVE && "true".equals(clause.getValue().toString())) {
			ax = fac.getOWLReflexiveObjectPropertyAxiom(p, annotations);
		}
		else if (_tag == OboFormatTag.TAG_IS_SYMMETRIC && "true".equals(clause.getValue().toString())) {
			ax = fac.getOWLSymmetricObjectPropertyAxiom(p, annotations);
		}
		else if (_tag == OboFormatTag.TAG_IS_ASYMMETRIC  && "true".equals(clause.getValue().toString())) {
			ax = fac.getOWLAsymmetricObjectPropertyAxiom(p, annotations);
		}
		else if (_tag == OboFormatTag.TAG_IS_FUNCTIONAL && "true".equals(clause.getValue().toString())) {
			ax = fac.getOWLFunctionalObjectPropertyAxiom(p, annotations);
		}
		else if (_tag == OboFormatTag.TAG_IS_INVERSE_FUNCTIONAL && "true".equals(clause.getValue().toString())) {
			ax = fac.getOWLInverseFunctionalObjectPropertyAxiom(p, annotations);
		}

		else {
			return trGenericClause(p, tag, clause);
		}
		// TODO - disjointOver
		return ax;
	}


	private OWLAxiom trGenericClause(OWLNamedObject e, String tag, Clause clause) {
		/*Collection<QualifierValue> qvs = clause.getQualifierValues();
		Set<? extends OWLAnnotation> annotations = trAnnotations(clause);

		OWLAnnotationSubject sub = (OWLAnnotationSubject) e.getIRI();
		//System.out.println(e+" ==> "+sub);
		if (clause.getValue() == null) {
			System.err.println("Problem:"+clause);
		}

		OWLAxiom ax = null;
		if (tag.equals("name")) {
			ax = fac.getOWLAnnotationAssertionAxiom(
					trTagToAnnotationProp(tag),
					sub, 
					trLiteral(clause.getValue()), 
					annotations);
		}
		else if (tag.equals("def")) {
			// TODO
			ax = fac.getOWLAnnotationAssertionAxiom(
					trTagToAnnotationProp(tag),
					sub, 
					trLiteral(clause.getValue()), 
					annotations);
		}
		else {
			// generic
			//System.out.println("generic clause:"+clause);
			ax = fac.getOWLAnnotationAssertionAxiom(
					trTagToAnnotationProp(tag),
					sub, 
					trLiteral(clause.getValue()), 
					annotations);
		}
		// TODO synonyms
		return ax;*/

		return trGenericClause(e.getIRI(), tag, clause);
	}


	protected OWLAxiom trGenericClause(OWLAnnotationSubject sub, String tag, Clause clause) {
		Set<OWLAnnotation> annotations = trAnnotations(clause);

		if (clause.getValue() == null) {
			LOG.error("Problem: "+clause);
		}

		OWLAxiom ax = null;
		OboFormatTag _tag = OBOFormatConstants.getTag(tag);
		//System.out.println("CLAUSE: "+clause+" // TAG="+_tag);
		if (_tag == OboFormatTag.TAG_NAME) {
			ax = fac.getOWLAnnotationAssertionAxiom(
					trTagToAnnotationProp(tag),
					sub, 
					trLiteral(clause.getValue()), 
					annotations);
//			ax = fac.getOWLAnnotationAssertionAxiom(
//					trTagToAnnotationProp("Preferred_Name"),
//					sub, 
//					trLiteral(clause.getValue()),
//					annotations);
		}
		else if (_tag == OboFormatTag.TAG_DEF) {
			ax = fac.getOWLAnnotationAssertionAxiom(
					trTagToAnnotationProp(tag),
					sub, 
					trLiteral(clause.getValue()), 
					annotations);
		}
		else if (_tag == OboFormatTag.TAG_SUBSET) {
			Object v = clause.getValue();
			if (v == null) {
				// TODO: Throw Exceptions
				LOG.warn("Cannot translate: "+clause);
			}
			else{
				ax = fac.getOWLAnnotationAssertionAxiom(
						trTagToAnnotationProp(tag),
						sub, 
						trAnnotationProp(v.toString()).getIRI(), 
						annotations);
			}
		}
		else if (_tag == OboFormatTag.TAG_PROPERTY_VALUE) {
			Collection<Object> values = clause.getValues();
			Object v = clause.getValue();
			Object v2 = clause.getValue2();
			if (v == null) {
				// TODO: Throw Exceptions
				LOG.warn("Cannot translate: "+clause);
			} 
			else if (values.size() == 2) {
				// property_value(Rel-ID Entity-ID Qualifiers)
				ax = fac.getOWLAnnotationAssertionAxiom(
						trAnnotationProp((String)v),
						sub, 
						trAnnotationProp(v2.toString()).getIRI(), 
						annotations);
			} 
			else if (values.size() == 3) {
				// property_value(Rel-ID Value XSD-Type Qualifiers)
				Iterator<Object> it = clause.getValues().iterator();
				it.next();
				it.next();
				String v3String = (String) it.next();
				IRI valueIRI;
				if (v3String.startsWith("xsd:")) {
					valueIRI = IRI.create(Namespaces.XSD + v3String.substring(4));
				} else {
					valueIRI = IRI.create(v3String);
				}
				OWLAnnotationValue value = fac.getOWLLiteral((String) v2, OWL2Datatype.getDatatype(valueIRI));
				ax = fac.getOWLAnnotationAssertionAxiom(
						trAnnotationProp((String)v),
						sub, 
						value,
						annotations);
			}
			else {
				LOG.warn("Cannot translate: "+clause);
				// TODO 
			}
		}
		else if (_tag == OboFormatTag.TAG_SYNONYM) {
			ax = EVSSynonym(clause, sub, annotations);
//TODO it is excluding the qualifiers.  Add  clause.getXrefs()
//			Object[] xrefs = clause.getXrefs().toArray();
//			Object[] values= clause.getValues().toArray(); 
////TODO - this is the offending line that changes the synonym type
//			String synType;
//			if(values.length>1){
//				synType = values[1].toString();
//				if(values.length>2){
//					OWLAnnotation ann= 
//						fac.getOWLAnnotation(trTagToAnnotationProp(OboFormatTag.TAG_HAS_SYNONYM_TYPE.getTag()), 
//								trAnnotationProp(values[2].toString()).getIRI());
//					annotations.add(ann);
//
//				}
//			}
//			else {
//				LOG.warn("Assume 'RELATED'for missing scope in synonym clause: "+clause);
//				// we make allowances for obof1.0, where the synonym scope is optional
//				synType = OboFormatTag.TAG_RELATED.getTag();
//			}
//			ax = fac.getOWLAnnotationAssertionAxiom(
//					trSynonymType(synType),
//					sub, 
//					trLiteral(clause.getValue()), 
//					annotations);
		}
		else if (_tag == OboFormatTag.TAG_XREF) {
			Xref xref = (Xref) clause.getValue();
			String xrefAnnotation = xref.getAnnotation();
			if (xrefAnnotation != null) {
				OWLAnnotation owlAnnotation = fac.getOWLAnnotation(fac.getRDFSLabel(), fac.getOWLLiteral(xrefAnnotation));
				annotations.add(owlAnnotation);
			}
			ax = fac.getOWLAnnotationAssertionAxiom(
					trTagToAnnotationProp(tag),
					sub, 
					trLiteral(clause.getValue()), 
					annotations);
		}
		else if (_tag == OboFormatTag.TAG_NAMESPACE){
			@SuppressWarnings("unused")
			String debug = "Stop here";
			ax = fac.getOWLAnnotationAssertionAxiom(
					trTagToAnnotationProp(tag),
					sub, 
					trLiteral(clause.getValue()), 
					annotations);
		}
		else {
			// generic
			//System.out.println("generic clause:"+clause);
			ax = fac.getOWLAnnotationAssertionAxiom(
					trTagToAnnotationProp(tag),
					sub, 
					trLiteral(clause.getValue()), 
					annotations);
		}
		return ax;
	}

	private OWLAnnotationProperty trSynonymType(String type) {
		if (type.equals(OboFormatTag.TAG_RELATED.getTag()) 
				|| type.equals(OboFormatTag.TAG_EXACT.getTag())
				|| type.equals(OboFormatTag.TAG_NARROW.getTag())
				|| type.equals(OboFormatTag.TAG_BROAD.getTag()))
		{
			return trTagToAnnotationProp(type);
		}
		return trAnnotationProp(type);
	}


	private Set<OWLAnnotation> trAnnotations(Clause clause) {
		Set<OWLAnnotation> anns = new HashSet<OWLAnnotation>();
		trAnnotations(clause, anns);
		return anns;
	}
	//Adding annotation axiom.  TODO hijack this for our purposes
	private void trAnnotations(Clause clause, Set<OWLAnnotation> anns) {
		Collection<Xref> xrefs = clause.getXrefs();
		if (xrefs != null) {
			for (Xref x : xrefs) {
				if(x.getIdref() != null && x.getIdref().length()>0){
					OWLAnnotationProperty ap = trTagToAnnotationProp(OboFormatTag.TAG_XREF.getTag());
					OWLAnnotation ann = fac.getOWLAnnotation(ap, trLiteral(x));
					anns.add(ann);
				}
			}
		}
		Collection<QualifierValue> qvs = clause.getQualifierValues();
		if (qvs != null) {
			for (QualifierValue qv : qvs) {
				String qTag = qv.getQualifier();
				if (qTag.equals("gci_relation") ||
						qTag.equals("gci_filler") ||
						qTag.equals("cardinality") ||
						qTag.equals("minCardinality") ||
						qTag.equals("maxCardinality") ||
						qTag.equals("all_some") ||
						qTag.equals("all_only")) {
					continue;
				}
				OWLAnnotationProperty ap = trTagToAnnotationProp(qTag);
				OWLAnnotation ann = fac.getOWLAnnotation(ap, trLiteral(qv.getValue()));
				anns.add(ann);
			}
		}
	}

	private Set<? extends OWLAnnotation> trAnnotations(Collection<Clause> clauses) {
		if (clauses != null) {
			Set<OWLAnnotation> anns = new HashSet<OWLAnnotation>();
			for(Clause clause : clauses) {
				trAnnotations(clause, anns);
			}
			return anns;
		}
		return null;
	}


	public OWLClassExpression trRel(String relId, String classId, Collection<QualifierValue> quals) {
		OWLClassExpression ex;
		Frame relFrame = obodoc.getTypedefFrame(relId);
		OWLObjectPropertyExpression pe = trObjectProp(relId);
		OWLClassExpression ce = trClass(classId);
		Integer exact = getQVInt("cardinality", quals);
		Integer min = getQVInt("minCardinality", quals);
		Integer max = getQVInt("maxCardinality", quals);
		Boolean allSome = getQVBoolean("all_some", quals);
		Boolean allOnly = getQVBoolean("all_only", quals);

		// obo-format allows dangling references to classes in class expressions;
		// create an explicit class declaration to be sure
		if (ce instanceof OWLClass) {
			add(fac.getOWLDeclarationAxiom((OWLClass)ce));
		}

		if (exact != null && exact > 0) {
			ex = fac.getOWLObjectExactCardinality(exact,pe,ce);
		}
		else if ((exact != null && exact == 0) ||
				(max != null && max == 0)) {
			OWLObjectComplementOf ceCompl = fac.getOWLObjectComplementOf(ce);
			ex = fac.getOWLObjectAllValuesFrom(pe,
					ceCompl);
		}
		else if (max != null && min != null) {
			ex = fac.getOWLObjectIntersectionOf(
					fac.getOWLObjectMinCardinality(min, pe, ce),
					fac.getOWLObjectMaxCardinality(max, pe, ce));
		}
		else if (min != null) {
			ex = fac.getOWLObjectMinCardinality(min, pe, ce);
		}
		else if (max != null) {
			ex = fac.getOWLObjectMaxCardinality(max, pe, ce);
		}
		else if (allSome != null && allSome &&
				allOnly != null && allOnly) {
			ex = fac.getOWLObjectIntersectionOf(
					fac.getOWLObjectSomeValuesFrom(pe, ce),
					fac.getOWLObjectAllValuesFrom(pe, ce));			
		}
		else if (allOnly != null && allOnly) {
			ex = fac.getOWLObjectAllValuesFrom(pe, ce);

		}
		else if (relFrame != null && relFrame.getTagValue(OboFormatTag.TAG_IS_CLASS_LEVEL_TAG) != null &&
				(Boolean)relFrame.getTagValue(OboFormatTag.TAG_IS_CLASS_LEVEL_TAG)) {
			// pun
			ex = fac.getOWLObjectHasValue(pe,trIndividual(classId));
		}
		else {
			// default
			ex = fac.getOWLObjectSomeValuesFrom(pe,ce);
		}
		return ex;
	}


	private String getQVString(String q, Collection<QualifierValue> quals) {
		for (QualifierValue qv : quals) {
			if (qv.getQualifier().equals(q)) {
				Object v = qv.getValue();
				return (String) v;
			}
		}
		return null;
	}

	private Boolean getQVBoolean(String q, Collection<QualifierValue> quals) {
		for (QualifierValue qv : quals) {
			if (qv.getQualifier().equals(q)) {
				Object v = qv.getValue();
				return Boolean.valueOf((String) v);
			}
		}
		return null;
	}

	private Integer getQVInt(String q, Collection<QualifierValue> quals) {
		for (QualifierValue qv : quals) {
			if (qv.getQualifier().equals(q)) {
				Object v = qv.getValue();
				return Integer.valueOf((String) v);
			}
		}
		return null;
	}


	private OWLClass trClass(String classId) {
		IRI iri = oboIdToIRI(classId);
		return fac.getOWLClass(iri);
	}
	
	private OWLClass EVS_trClass(String classId){
		IRI iri = EVS_GO_oboIdToIRI(classId);
		return fac.getOWLClass(iri);
	}

	private OWLClassExpression trClass(Object v) {
		return trClass((String)v);
	}

	/**
	 * See section "header macros" and treat-xrefs-as-equivalent
	 * 
	 * @param id 
	 * @return mapped id
	 */
	private String mapPropId(String id) {
		Frame f = obodoc.getTypedefFrame(id);
		if (f != null) {
			Collection<Xref> xrefs = f.getTagValues(OboFormatTag.TAG_XREF, Xref.class);
			for (Xref x : xrefs) {
				String xid = x.getIdref();
				if (obodoc.isTreatXrefsAsEquivalent(getIdPrefix(xid))) {
					return xid;
				}
			}
		}
		return id;
	}

	private String getIdPrefix(String x) {
		String[] parts = x.split(":", 2);
		return parts[0];
	}

	private OWLIndividual trIndividual(String instId) {
		IRI iri = oboIdToIRI(instId);
		return fac.getOWLNamedIndividual(iri);
	}

	public static IRI trTagToIRI(String tag){
		IRI  iri = null;
		if (annotationPropertyMap.containsKey(tag)) {
			iri = annotationPropertyMap.get(tag);
		}
		else {  //TODO here is where the obo2owl namespace gets pulled in
			//iri = IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+"IAO_"+tag);
			iri = IRI.create(Obo2OWLConstants.OIOVOCAB_IRI_PREFIX+tag);

		}

		return iri;

	}

	private OWLAnnotationProperty trTagToAnnotationProp(String tag) {
		IRI iri = trTagToIRI(tag);
		OWLAnnotationProperty ap = fac.getOWLAnnotationProperty(iri);


		if (!apToDeclare.contains(ap)) {
			apToDeclare.add(ap);
			add(fac.getOWLDeclarationAxiom(ap));

			Obo2OWLVocabulary vocab = Obo2OWLConstants.getVocabularyObj(tag);
			if(vocab != null){
				add (fac.getOWLAnnotationAssertionAxiom(fac.getRDFSLabel(), iri, trLiteral( vocab.getLabel()) ) );
			}

		}

		return ap;
	}
	
	protected void addDeclaredAnnotationProperties(Collection<OWLAnnotationProperty> declaredProperties) {
		if (declaredProperties != null) {
			apToDeclare.addAll(declaredProperties);
		}
	}

	private OWLAnnotationProperty trAnnotationProp(String relId) {
		IRI iri = oboIdToIRI(mapPropId(relId));
		return fac.getOWLAnnotationProperty(iri);	
	}


	private OWLObjectProperty trObjectProp(String relId) {
		IRI iri = oboIdToIRI(mapPropId(relId));
		return fac.getOWLObjectProperty(iri);	
	}


	private OWLObjectPropertyExpression trObjectProp(Object v) {
		IRI iri = oboIdToIRI(mapPropId((String) v));
		return fac.getOWLObjectProperty(iri);	
	}



	protected OWLAnnotationValue trLiteral(Object value) {
		if (value instanceof Xref) {
			value = ((Xref)value).getIdref();
		}
		else if (value instanceof Date) {
			// use proper OWL2 data type, write lexical value as ISO 8601 date string 
			String lexicalValue = Obo2OWLConstants.OWL2_DATE_LITERAL_FORMAT.get().format((Date)value);
			return fac.getOWLLiteral(lexicalValue, OWL2Datatype.XSD_DATE_TIME);
		}
		else if (value instanceof Boolean) {
			return fac.getOWLLiteral((Boolean)value);
		}
		else if (! (value instanceof String)) {
			// TODO
			// e.g. boolean
			value = value.toString();
		}
		//System.out.println("v="+value);
		return fac.getOWLLiteral((String)value); // TODO
	}

	public IRI oboIdToIRI(String id) {
		if (id.contains(" ")) {
			LOG.error("id contains space: \""+id+"\"");
			//throw new UnsupportedEncodingException();
			return null;
		}

		// No conversion is required if this is already an IRI (ID-as-URI rule)
		if (id.startsWith("http:")) { // TODO - roundtrip from other schemes
			return IRI.create(id);
		}
		else if (id.startsWith("https:")) { // TODO - roundtrip from other schemes
			return IRI.create(id);
		}
		else if (id.startsWith("ftp:")) { // TODO - roundtrip from other schemes
			return IRI.create(id);
		}
		else if (id.startsWith("urn:")) { // TODO - roundtrip from other schemes
			return IRI.create(id);
		}

		// TODO - treat_xrefs_as_equivalent
		// special case rule for relation xrefs:
		// 5.9.3. Special Rules for Relations
		if (!id.contains(":")) {
			String xid = translateShorthandIdToExpandedId(id);
			if (!xid.equals(id))
				return oboIdToIRI(xid);
		}

		String[] idParts = id.split(":", 2);
		String db;
		String localId;
		if (idParts.length > 1) {
			db = idParts[0];
			localId = idParts[1];
			if(localId.contains("_")){
				db += "#_"; // NonCanonical-Prefixed-ID
			}else
				db += "_";
		}
		else if (idParts.length == 0) {
//			db = getDefaultIDSpace()+"#"; 
//			db = Obo2OWLConstants.OIOVOCAB_IRI_PREFIX ;
			db="";
			localId = id;
		}
		else { // == 1
			// todo use owlOntology IRI
//			db = getDefaultIDSpace()+"#";
//			db = Obo2OWLConstants.OIOVOCAB_IRI_PREFIX ;
			db="";
			//	if(id.contains("_"))
			//	db += "_";

			localId = idParts[0]; // Unprefixed-ID
		}


		String uriPrefix = Obo2OWLConstants.OIOVOCAB_IRI_PREFIX  +db;
		if (idSpaceMap.containsKey(db)) {
			uriPrefix = idSpaceMap.get(db);
		}

		String safeId;
		try {
			safeId = java.net.URLEncoder.encode(localId,"US-ASCII");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			return null;
		}

		if (safeId.contains(" "))
			safeId = safeId.replace(" ", "_");
		IRI iri = null;
		try {
			iri = IRI.create(uriPrefix + safeId);
		} catch (IllegalArgumentException e) {
			// TODO - define new exception class for this
			// throw new UnsupportedEncodingException();
			return null;
		}

		return  iri;	
	}

	// 5.9.3. Special Rules for Relations
	private String translateShorthandIdToExpandedId(String id) {
		if (id.contains(":"))
			return id;
		Frame tdf = obodoc.getTypedefFrame(id);
		if (tdf == null)
			return id;
		Collection<Xref> xrefs = tdf.getTagValues(OboFormatTag.TAG_XREF, Xref.class);
		String matchingExpandedId = null;
		for (Xref xref : xrefs) {
			//System.err.println("ID:"+id+" xref:"+xref);

			if (xref != null) {
				String xid = xref.getIdref();
				//System.err.println(" ID:"+id+" xid:"+xid);
				if (xid.equals(id))
					continue;
				if (matchingExpandedId == null) {
					matchingExpandedId = xid;
				}
				else {
					// RO and BFO take precedence over others
					if ((xid.startsWith("RO") ||
							xid.startsWith("BFO"))) {
						matchingExpandedId = xid;
					}
				}
			}
		}
		if (matchingExpandedId == null)
			return id;
		//System.err.println("  ID:"+id+" matching:"+matchingExpandedId);
		return matchingExpandedId;
	}

//	private String getDefaultIDSpace() {
//		return defaultIDSpace;
//	}

	private OWLAxiom EVSSynonym(Clause clause, OWLAnnotationSubject sub, Set<OWLAnnotation> annotations){
		//TODO it is excluding the qualifiers.  Add  clause.getXrefs()
		Object[] xrefs = clause.getXrefs().toArray();
		Object[] values= clause.getValues().toArray(); 
//TODO - this is the offending line that changes the synonym type
		String synType = "FULL_SYN";
		if(values.length>1){
//			synType = values[1].toString();
//			if(values.length>1){
			for(int i=1; i<values.length; i++){
				OWLAnnotation ann= 
					fac.getOWLAnnotation(trTagToAnnotationProp(OboFormatTag.TAG_HAS_SYNONYM_TYPE.getTag()), 
							trLiteral(values[i].toString()));
				annotations.add(ann);
			}
//			}
		}

		if(xrefs.length>0){
			for(int i=0; i<xrefs.length; i++){
				OWLAnnotation ann =
						fac.getOWLAnnotation(trTagToAnnotationProp(OboFormatTag.TAG_XREF.getTag()), trLiteral(xrefs[i].toString()));
				annotations.add(ann);
			}
		}
		
		OWLAxiom ax = fac.getOWLAnnotationAssertionAxiom(
				trTagToAnnotationProp(synType),
				sub, 
				trLiteral(clause.getValue()), 
				annotations);
		
		return ax;
	}
	
	public IRI EVS_GO_oboIdToIRI(String id) {
		String[] idParts = id.split(":", 2);
		String db;
		String localId;
		if (idParts.length > 1) {
			db = idParts[0];
			localId = idParts[1];
			if(localId.contains("_")){
				db += "#_"; // NonCanonical-Prefixed-ID
			}else
				db += "_";
		}
		else if (idParts.length == 0) {
//			db = getDefaultIDSpace()+"#"; 
			db = Obo2OWLConstants.OIOVOCAB_IRI_PREFIX;
			localId = id;
		}
		else { // == 1
			// todo use owlOntology IRI
//			db = getDefaultIDSpace()+"#";
			db = Obo2OWLConstants.OIOVOCAB_IRI_PREFIX;
			//	if(id.contains("_"))
			//	db += "_";

			localId = idParts[0]; // Unprefixed-ID
		}
		
//		db =  getDefaultIDSpace()+"#" + db; 
		String uriPrefix = Obo2OWLConstants.OIOVOCAB_IRI_PREFIX + db;
		if (idSpaceMap.containsKey(db)) {
			uriPrefix = idSpaceMap.get(db);
		}

		String safeId;
		try {
			safeId = java.net.URLEncoder.encode(localId,"US-ASCII");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			return null;
		}

		if (safeId.contains(" "))
			safeId = safeId.replace(" ", "_");
		IRI iri = null;
		try {
			iri = IRI.create(uriPrefix + safeId);
		} catch (IllegalArgumentException e) {
			// TODO - define new exception class for this
			// throw new UnsupportedEncodingException();
			return null;
		}

		return  iri;	
	}
	
}
