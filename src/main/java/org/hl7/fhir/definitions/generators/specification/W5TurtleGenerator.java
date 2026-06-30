package org.hl7.fhir.definitions.generators.specification;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;
import org.hl7.fhir.definitions.model.Definitions;
import org.hl7.fhir.definitions.model.W5Entry;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.rdf.RDFNamespace;
import org.hl7.fhir.tools.publisher.BuildWorkerContext;
import org.hl7.fhir.tools.publisher.PageProcessor;
import org.hl7.fhir.utilities.validation.ValidationMessage;

/**
 * Generator to create fhir "Ontology" -- a model of the various subjects, predicates and types in the FHIR spec
 */
public class W5TurtleGenerator {
    private OutputStream destination;
    private Definitions definitions;
    private BuildWorkerContext context;
    private List<ValidationMessage> issues;
    private String host;


    public W5TurtleGenerator(OutputStream destination, Definitions definitions, BuildWorkerContext context,
                             List<ValidationMessage> issues, String host) {
        this.destination = destination;
        this.definitions = definitions;
        this.context = context;
        this.issues = issues;
        this.host = host;
    }

    /**
     * Only produce the v3 vocabulary for appending to rim.ttl
     * Placeholder - has no effect in this generation
     */
    public void executeV3(Map<String, ValueSet> valuesets, Map<String, CodeSystem> codeSystems) throws Exception {

    }

    public void executeMain() throws Exception {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        model.setNsPrefix(RDFNamespace.W5.getPrefix(), RDFNamespace.W5.getURI());
        model.setNsPrefix(RDFNamespace.FHIR.getPrefix(), RDFNamespace.FHIR.getURI());

        SimpleDateFormat createdTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        createdTimestamp.setTimeZone(TimeZone.getTimeZone("UTC"));

        String canonicalIri = RDFNamespace.FHIR.getURI() + "w5.ttl";

        Ontology w5 = model.createOntology(canonicalIri);
        w5.addProperty(RDFS.label, "FHIR W5");
        w5.addProperty(RDFS.comment, "FHIR W5 (Who, What, When, Where, Why) taxonomy of terms used in FHIR StructureDefinition and ElementDefinition mappings");

        String versionedBase = getOntologyVersionIRI();
        String versionIri = versionedBase + "w5.ttl";
        String w5pageIri = versionedBase + "w5.html";
        var w5page = ResourceFactory.createResource(w5pageIri);

        w5.addProperty(OWL2.versionIRI, ResourceFactory.createResource(versionIri));
        w5.addProperty(OWL2.versionInfo, createdTimestamp.format(new Date()), XSDDatatype.XSDdateTime);
        w5.addProperty(RDFS.isDefinedBy, w5page);

        // The only way to differentiate predicates from classes is the existence of subclasses -- if something
        // has subclasses or is a subclass then it is a class.  Otherwise it is a predicate...
        Set<String> w5classes = new HashSet<String>();
        for (W5Entry e : definitions.getW5list()) {
            Set<String> escs = e.getSubClasses();
            if (!escs.isEmpty()) {
                w5classes.add(e.getCode());
                for (String s : e.getSubClasses()) {
                    if (!s.contains("."))
                        s = e.getCode() + '.' + s;
                    w5classes.add(s);
                }
            }
        }

        OntClass top_class = model.createClass(RDFNamespace.W5.uriFor("W5"));
        top_class.addProperty(RDFS.isDefinedBy, w5page);
        OntProperty top_property = model.createObjectProperty(RDFNamespace.W5.uriFor("w5"));
        top_property.addProperty(RDFS.isDefinedBy, w5page);

        // TODO: Temporary fix -- add 'when' element
        OntProperty wp = model.createObjectProperty(RDFNamespace.W5.uriFor("when"));
        wp.addSuperProperty(top_property);
        wp.addLabel("when", null);
        wp.addComment("point in time", null);

        // rdfs:isDefinedBy not needed on every term
        for (W5Entry e : definitions.getW5list()) {
            String es = e.getCode();
            if(w5classes.contains(es)) {
                OntClass ec = model.createClass(RDFNamespace.W5.uriFor(es));
                ec.addSuperClass(top_class);
                ec.addComment(e.getDescription(), null);
                for (String s : e.getSubClasses()) {
                    String s_uri = RDFNamespace.W5.uriFor(s.contains(".")? s : e.getCode() + "." + s);
                    OntClass c = model.createClass(s_uri);
                    c.addSuperClass(ec);
                    c.addLabel(s, null);
                }
            } else {
                OntProperty ep = model.createObjectProperty(RDFNamespace.W5.uriFor(es));
                ep.addLabel(es, null);
                ep.addComment(e.getDescription(), null);
                if (!es.contains(".")) {
                    ep.addSuperProperty(top_property);
                }
                String esroot = es;
                while(esroot.contains(".")) {
                    esroot = esroot.substring(0, esroot.lastIndexOf('.'));
                    ep.addSuperProperty(model.createProperty(RDFNamespace.W5.uriFor(esroot)));
                }
            }
        }

        commit(model, true);
    }


    private String getOntologyVersionIRI() {
        return host.startsWith("file:") ?
                PageProcessor.CI_LOCATION : host;
    }
    public void commit(OntModel model, boolean header) throws Exception {
        RDFDataMgr.write(destination, model, RDFFormat.TURTLE_PRETTY);
        destination.flush();
        destination.close();
    }
}


