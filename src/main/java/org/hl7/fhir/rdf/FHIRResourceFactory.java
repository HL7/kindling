package org.hl7.fhir.rdf;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;


public class FHIRResourceFactory {
    private final Model model;

    public FHIRResourceFactory() {
        model = ModelFactory.createDefaultModel();
        RDFNamespace.addFHIRNamespaces(model);
    }

    /**
     * Serialize the namespace instance in RDF Turtle
     *
     * @param writer
     */
    public void serialize(OutputStream writer) {
        RDFDataMgr.write(writer, model, RDFFormat.TURTLE_PRETTY);
    }


    /**
     * Add a new datatype to the model
     *
     * @param d resource to declare as a datatype
     * @return Resource in context of model
     */
    public Resource add_datatype(Resource d) {
        return model.createResource(d.getURI()).addProperty(RDF.type, RDFS.Datatype);
    }

    /**
     * Merge RDF from a different model
     */
    public void merge_rdf(Model m) {
        model.add(m);
    }

    /* =========================================================
     * FHIRResource factory methods
     * ========================================================= */

    /**
     * Create a Resource in the FHIR namespace
     *
     * @param name  resource name
     * @param type  resource type
     * @param label resource label
     * @return
     */
    public FHIRResource fhir_resource(String name, Resource type, String label) {
        return new FHIRResource(model, name, label).addType(type);
    }

    /**
     * Create an OWL Ontology in the FHIR namespace
     *
     * @param name  Ontology name
     * @param label Ontology label
     * @return Ontology resource
     */
    public FHIRResource fhir_ontology(String name, String label) {
        return fhir_resource(name, OWL2.Ontology, label);
    }

    /**
     * Create an anonymous resource in the FHIR namespace
     */
    public FHIRResource fhir_bnode() {
        return new FHIRResource(model);
    }

    /**
     * Create a new Class in the FHIR namespace
     *
     * @param name class name and label
     * @return
     */
    public FHIRResource fhir_class(String name) {
        return fhir_resource(getClassName(name), OWL2.Class, name);
    }

    public static String getClassName(String name) {
        // Uppercase the first letter of any class, including primitive types, to avoid ambiguity with properties
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
     * Create a new Class in the FHIR namespace
     *
     * @param name       class name and label
     * @param superClass superclass name
     * @return
     */
    public FHIRResource fhir_class(String name, Resource superClass) {
        return fhir_class(name).addObjectProperty(RDFS.subClassOf, superClass);
    }

    /**
     * Create a new Class in the FHIR namespace
     *
     * @param name       class name and label
     * @param superClass superclass
     * @return
     */
    public FHIRResource fhir_class(String name, String superClass) {
        return fhir_class(name, RDFNamespace.FHIR.resourceRef(superClass));
    }


    /**
     * Create a new ObjectProperty in the FHIR namespace
     *
     * @param name property name and label
     * @return
     */
    public FHIRResource fhir_objectProperty(String name) {
        return fhir_resource(name, OWL2.ObjectProperty, name);
    }

    /**
     * Create a new ObjectProperty in the FHIR namespace
     *
     * @param name          property name and label
     * @param superProperty parent property
     * @return
     */
    public FHIRResource fhir_objectProperty(String name, Resource superProperty) {
        if(superProperty.getLocalName().equals(name))
            return fhir_objectProperty(name); // do not add something as a subProperty of itself
        return fhir_objectProperty(name).addObjectProperty(RDFS.subPropertyOf, superProperty);
    }

    /**
     * Create a new ObjectProperty in the FHIR namespace
     *
     * @param name          property name and label
     * @param superProperty parent property name
     * @return
     */
    public FHIRResource fhir_objectProperty(String name, String superProperty) {
        return fhir_objectProperty(name, RDFNamespace.FHIR.resourceRef(superProperty));
    }

    /**
     * Create a new DataProperty in the FHIR namespace
     *
     * @param name property name and label
     * @return
     */
    public FHIRResource fhir_dataProperty(String name) {
        return fhir_resource(name, OWL2.DatatypeProperty, name);
    }

    /**
     * Create a new DataProperty in the FHIR namespace
     *
     * @param name          property name and label
     * @param superProperty parent property
     * @return
     */
    public FHIRResource fhir_dataProperty(String name, Resource superProperty) {
        return fhir_dataProperty(name).addObjectProperty(RDFS.subPropertyOf, superProperty);
    }

    /**
     * Create a new DataProperty in the FHir namespace
     *
     * @param name          property name and label
     * @param superProperty parent property name
     * @return
     */
    public FHIRResource fhir_dataProperty(String name, String superProperty) {
        return fhir_dataProperty(name, RDFNamespace.FHIR.resourceRef(superProperty));
    }

    /**
     * Create a new OWL Restriction
     *
     * @param onProperty
     * @return
     */
    public FHIRResource fhir_restriction(Resource onProperty) {
        return fhir_bnode()
                .addType(OWL2.Restriction)
                .addObjectProperty(OWL2.onProperty, onProperty);
    }

    /**
     * Create a new OWL Restriction to attach a property to via addObjectProperty or addDataProperty
     *
     * @param onProperty
     * @return
     */
    public FHIRResource create_empty_owl_restriction(Resource onProperty) {  //changed name for clarity to avoid confusion
        return fhir_bnode()
                .addType(OWL2.Restriction)
                .addObjectProperty(OWL2.onProperty, onProperty);
    }

    /**
     * Create a new OWL restriction with the appropriate cardinality
     *
     * @param onProperty property to apply the restriction to
     * @param from       only/some target
     * @param min        min cardinality
     * @param max        max cardinality
     * @return restriction list of resources
     */
    public List<Resource> fhir_cardinality_restriction(Resource onProperty, Resource from, int min, int max) {
        ArrayList<Resource> restrictions = new ArrayList<>();
        restrictions.add(create_empty_owl_restriction(onProperty).addObjectProperty(OWL2.allValuesFrom, from).resource);
        if (min == max)
            restrictions.add(create_empty_owl_restriction(onProperty).addDataProperty(OWL2.cardinality, Integer.toString(min), XSDDatatype.XSDinteger).resource);
        else {
            if (min > 0)
                restrictions.add(create_empty_owl_restriction(onProperty).addDataProperty(OWL2.minCardinality, Integer.toString(min), XSDDatatype.XSDinteger).resource);
                //restrictions.add(create_empty_owl_restriction(onProperty).addObjectProperty(OWL2.someValuesFrom, from).resource);  // use minCardinality instead
            if (max < Integer.MAX_VALUE)
                restrictions.add(create_empty_owl_restriction(onProperty).addDataProperty(OWL2.maxCardinality, Integer.toString(max), XSDDatatype.XSDinteger).resource);  //TODO:add fix to normal String instead of Binary as a String (9 was previously  owl:maxCardinality  1001 ;) also changed to maxQualifiedCardinality
        }
        return restrictions;
    }

    /**
     * Create a new OWL restriction with the appropriate cardinality.  Creates a union of the various types from "from"
     *
     * @param onProperty property to apply the restriction to
     * @param from       multiple targets
     * @param min        min cardinality
     * @param max        max cardinality
     * @return restriction list of resources
     */
    public List<Resource> fhir_cardinality_restriction(Resource onProperty, List<Resource> from, int min, int max) {
        Resource targetFrom = fhir_bnode().addObjectProperty(OWL2.unionOf, new FHIRResource(model, from))
                .addObjectProperty(RDF.type, RDFS.Datatype).resource;
        return fhir_cardinality_restriction(onProperty, targetFrom, min, max);
    }


    /**
     * Return a generic restriction
     *
     * @param onProperty
     * @param from
     * @return
     */
    public List<Resource> fhir_restriction(Resource onProperty, Resource from) {
        return fhir_cardinality_restriction(onProperty, from, 0, Integer.MAX_VALUE);
    }


    /**
     * Return a union of the supplied members
     *
     * @param members
     * @return Resource representing union
     */
    public Resource fhir_union(List<Resource> members) {
        return fhir_bnode()
                .addObjectProperty(OWL2.unionOf, new FHIRResource(model, members))
                .resource;
    }

    /**
     * Returns a reference to an RDF List based on a supplied list of members
     * @param members members to add to list
     * @return Resource of List
     */
    public Resource fhir_list(List<Resource> members) {
        return new FHIRResource(model, members).resource;
    }



    /**
     * Return a simple datatype restriction
     *
     * @param dataType data type to be restricted
     * @return
     */
    public FHIRResource fhir_datatype(Resource dataType) {
        return fhir_bnode()
                .addType(RDFS.Datatype)
                .addObjectProperty(OWL2.onDatatype, dataType);
    }

    /**
     * Return a datatype restriction
     *
     * @param dataType data type to be restricted
     * @param facets   List of facets
     * @return
     */
    public Resource fhir_datatype_restriction(Resource dataType, List<Resource> facets) {
        return fhir_datatype(dataType)
                .addObjectProperty(OWL2.withRestrictions, new FHIRResource(model, facets))
                .resource;
    }

    /**
     * Return a pattern BNode
     *
     * @param pattern string pattern
     * @return
     */
    public Resource fhir_pattern(String pattern) {
        return fhir_bnode()
                .addDataProperty(RDFNamespace.XSDpattern, pattern).resource;
    }
}
