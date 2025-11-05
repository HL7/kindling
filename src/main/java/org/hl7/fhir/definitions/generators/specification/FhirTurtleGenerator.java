package org.hl7.fhir.definitions.generators.specification;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.hl7.fhir.definitions.model.DefinedCode;
import org.hl7.fhir.definitions.model.DefinedStringPattern;
import org.hl7.fhir.definitions.model.Definitions;
import org.hl7.fhir.definitions.model.ElementDefn;
import org.hl7.fhir.definitions.model.ProfiledType;
import org.hl7.fhir.definitions.model.ResourceDefn;
import org.hl7.fhir.definitions.model.TypeDefn;
import org.hl7.fhir.definitions.model.TypeRef;
import org.hl7.fhir.r5.context.CanonicalResourceManager;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.rdf.FHIRResource;
import org.hl7.fhir.rdf.FHIRResourceFactory;
import org.hl7.fhir.rdf.RDFNamespace;
import org.hl7.fhir.rdf.RDFTypeMap;
import org.hl7.fhir.tools.publisher.BuildWorkerContext;
import org.hl7.fhir.tools.publisher.PageProcessor;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.validation.ValidationMessage;

/**
 * Generator to create fhir "Ontology" -- a model of the various subjects, predicates and types in the FHIR spec
 */
public class FhirTurtleGenerator {
    private OutputStream destination;
    private Definitions definitions;
    private BuildWorkerContext context;
    private List<ValidationMessage> issues;
    private FHIRResourceFactory fact;
    private Resource value;
    private Resource v;
    private String host;
    private List<String> classHasModifierExtensions = new ArrayList<>();
    private static String fhirRdfPageUrl = "https://www.hl7.org/fhir/rdf.html";
    private static String fhirRdfLinkName = "l";
    private static List<String> referenceTypes = Arrays.asList("Reference", "canonical", "CodeableReference");

    // OWL doesn't recognize xsd:gYear, xsd:gYearMonth or xsd:date.  If true, map all three to xsd:datetime
    private boolean owlTarget = true;


    public FhirTurtleGenerator(OutputStream destination, Definitions definitions, BuildWorkerContext context,
                               List<ValidationMessage> issues, String host) {
        this.destination = destination;
        this.definitions = definitions;
        this.context = context;
        this.issues = issues;
        this.host = host;
        this.fact = new FHIRResourceFactory();
        this.v = fact.fhir_resource("v", OWL2.DatatypeProperty, "fhir:v")
                .addTitle("Terminal data value for primitive FHIR datatypes that can be represented as a RDF literal")
                .addProvenance(fhirRdfPageUrl)
                .resource;
    }

    /**
     * Only produce the v3 vocabulary for appending to rim.ttl
     * Placeholder for now - has no effect in this generation
     */
    public void executeV3(CanonicalResourceManager<ValueSet> valuesets, CanonicalResourceManager<CodeSystem> codeSystems) throws Exception {
//        for (String csName : codeSystems.keySet()) {
//            CodeSystem cs = codeSystems.get(csName);
//            if (cs == null) {
//                System.out.println("-----> " + csName);
//            } else {
//                new OWLCodeSystem(cs).commit(cs, destDir);
//            }
//        }
    }

    public void executeMain() throws Exception {
        genOntologyDefinition();
        genBaseMetadata();

        // Primitive types: string, code, etc.
        for (String pn : sorted(definitions.getPrimitives().keySet())) {
            if(isPrimitive(pn))
                genPrimitiveType(definitions.getPrimitives().get(pn));
        }

        // "Base, abstract types": Base, Element, BackboneElement, BackboneType, PrimitiveType, etc.
        for (String infn : sorted(definitions.getInfrastructure().keySet())) {
            TypeDefn defn = definitions.getInfrastructure().get(infn);
            if (defn.enablesModifierExtensions()) {
                //if original defn is a superclass that enables modifierExtensions, then generate fhir:_"defn"
                StructureDefinition profile = defn.getProfile();
                genBaseModifierExtensionCode(infn, profile.getBaseDefinitionElement().getValue(), profile.getUrl());
            }
            genElementDefn(definitions.getInfrastructure().get(infn));
        }

        // Complex types: Address, HumanName, Coding, Quantity, etc.
        for (String n : sorted(definitions.getTypes().keySet()))
            genElementDefn(definitions.getTypes().get(n));

        // Specializations: MoneyQuantity, SimpleQuantity, etc.
        for (String n : sorted(definitions.getConstraints().keySet())) {
            genProfiledType(definitions.getConstraints().get(n));
        }

        // Other grouping: CanonicalResource, DomainResource, MetadataResource, etc.
        for (String n : sorted(definitions.getBaseResources().keySet())) {
            ResourceDefn defn = definitions.getBaseResources().get(n);
            StructureDefinition profile = defn.getProfile();
            if(defn.getRoot().enablesModifierExtensions()) {
                genBaseModifierExtensionCode(n, profile.getBaseDefinitionElement().getValue(), profile.getUrl());
            }
            genResourceDefn(defn);
        }
        // Resources: Patient, Observation, Encounter, etc.
        for (String n : sorted(definitions.getResources().keySet()))
            genResourceDefn(definitions.getResources().get(n));

//        for (StructureDefinition sd : context.getExtensionDefinitions()) {
//             System.out.println("=====> " + sd.getName());
//             genStructure(context.getExtensionDefinitions().get(n));
//        }

        commit(true);
    }

   /**
    * Emit an ontology definition for the file
    */
    private void genOntologyDefinition() {
        SimpleDateFormat createdTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        createdTimestamp.setTimeZone(TimeZone.getTimeZone("UTC"));

        fact.fhir_ontology("fhir.ttl", "FHIR Model Ontology")
                .addDataProperty(RDFS.comment, "Formal model of FHIR Resources")
                .addObjectProperty(OWL2.versionIRI, ResourceFactory.createResource(getOntologyVersionIRI() +"fhir.ttl"))
                .addDataProperty(OWL2.versionInfo, createdTimestamp.format(new Date()), XSDDatatype.XSDdateTime)
                .addObjectProperty(OWL2.imports, ResourceFactory.createResource("http://hl7.org/fhir/w5.ttl"));
    }

    private String getOntologyVersionIRI() {
        return host.startsWith("file:") ?
                PageProcessor.CI_LOCATION : host;
    }

    /**
     * Emit all the basic atoms that are implicit in the actual model
     */
    private void genBaseMetadata() {
        // Declare these for now - they will get filled in more completely later on
        FHIRResource Resource = fact.fhir_class("Resource");
        addProvenanceForTypeName(Resource, "Resource");
        FHIRResource Element = fact.fhir_class("Element", "Base");
        addProvenanceForTypeName(Element, "Element");
        FHIRResource Reference = fact.fhir_class("Reference");
        addProvenanceForTypeName(Reference, "Reference");



        // A resource can have an optional nodeRole
        FHIRResource treeRoot = fact.fhir_individual("treeRoot")
                .addTitle("Root resource of FHIR RDF document")
                .addDataProperty(RDFS.comment, "Some resources can contain other resources. Given that the relationships can appear in any order in RDF, it cannot be assumed that the first encountered element represents the resource of interest that is being represented by the set of Turtle statements. The focal resource -- where to start when parsing -- is the resource with the relationship fhir:nodeRole to fhir:treeRoot. If there is more than one node labeled as a 'treeRoot' in a set of Turtle statements, it cannot be determined how to parse them as a single resource.");

        FHIRResource nodeRole = fact.fhir_objectProperty("nodeRole", fhirRdfPageUrl)
                .addTitle("Identifies role of subject in context of a given document")
                .domain(Resource)
                .rangeIndividual(treeRoot.resource);

        // Resource can have max 1 nodeRole
        Resource.restriction(fact.create_empty_owl_restriction(nodeRole.resource).addDataProperty(OWL2.maxCardinality, "1", XSDDatatype.XSDinteger).resource);
        // Resource nodeRole can only the individual treeRoot
        Resource.restriction(fact.create_empty_owl_restriction(nodeRole.resource).addObjectProperty(OWL2.allValuesFrom, treeRoot.oneOfIndividual(treeRoot.resource)).resource);
        
        // Any element can have an index to assign order in a list
//        FHIRResource index = fact.fhir_dataProperty("index")
//                .addTitle("Ordering value for list")
//                .domain(Element)
//                .range(XSD.nonNegativeInteger);
//        Element.restriction(fact.fhir_cardinality_restriction(index.resource, XSD.nonNegativeInteger, 0, 1));

        // References have an optional link
        FHIRResource link = fact.fhir_resource(fhirRdfLinkName, OWL2.ObjectProperty, "fhir:" + fhirRdfLinkName)
                                .addProvenance(fhirRdfPageUrl)
                                .addTitle("IRI of a reference");
        Reference.restriction(fact.fhir_class_cardinality_restriction(link.resource, Resource.resource, 0, 1));

        // XHTML is an XML Literal. Not available in Definitions.java, but see https://hl7.org/fhir/xhtml.profile.html
        String xhtmlCanonical = "http://hl7.org/fhir/StructureDefinition/xhtml";
        fact.fhir_class_with_provenance("xhtml", "Element", xhtmlCanonical)
            .restriction(fact.fhir_class_cardinality_restriction(v, RDF.xmlLiteral, 1, 1));

        addProvenanceForTypeName(fact.fhir_class("PrimitiveType"), "PrimitiveType");
    }

    private String getResourceNameFromCanonical(String canonicalUrl) {
        return canonicalUrl.substring(canonicalUrl.lastIndexOf("/")+1);
    }

    /**
     * Generates Modifier Extension Code for superclass
     * At current time these superclasses are: DomainResource, BackboneElement, BackboneType
     */
    private void genBaseModifierExtensionCode(String className, String baseDefinitionUrl, String definitionCanonical) throws Exception {
            classHasModifierExtensions.add(className);  // keep track of which classes enable Modifier extensions

            FHIRResource originalResource = fact.fhir_class(className);

            FHIRResource modResource = fact.fhir_class_with_provenance("_"+className, definitionCanonical);

            if(baseDefinitionUrl != null) {
                String baseName = getResourceNameFromCanonical(baseDefinitionUrl);
                Resource baseRes = RDFNamespace.FHIR.resourceRef(baseName);
                modResource.addObjectProperty(RDFS.subClassOf, baseRes);
            }

            FHIRResource extensionResource = fact.fhir_class("Extension");

            Resource modifierExtensionProperty = RDFNamespace.FHIR.resourceRef("modifierExtension");

            FHIRResource cardRestriction = fact.fhir_bnode().addType(OWL2.Restriction).addDataProperty(OWL2.minCardinality, "1", XSDDatatype.XSDinteger)
                    .addObjectProperty(OWL2.onProperty, modifierExtensionProperty);
            modResource.restriction(cardRestriction.resource);
            FHIRResource extRestriction = fact.fhir_bnode().addType(OWL2.Restriction)
                    .addObjectProperty(OWL2.onProperty, modifierExtensionProperty)
                    .addObjectProperty(OWL2.allValuesFrom, extensionResource);
            modResource.restriction(extRestriction.resource);

            FHIRResource floatingBNode = fact.fhir_bnode().addType(OWL2.AllDisjointClasses);
            List<Resource> disjointedList = new ArrayList<>(Arrays.asList(originalResource.resource, modResource.resource));
            floatingBNode.addObjectProperty(OWL2.members, fact.fhir_list(disjointedList));
    }

  /* ==============================================
     Generators for various FHIR types
   * ============================================== */

    /**
     * PrimitiveType Generator
     *
     * @param pt FHIR Primitive Type (e.g. int, string, dateTime)
     */
    // Note: For unknown reasons, getPrimitives returns DefinedCodes, not PrimitiveTypes...
    private void genPrimitiveType(DefinedCode pt) {
        String ptName = pt.getCode();
        FHIRResource ptRes = fact.fhir_class_with_provenance(ptName, "PrimitiveType", pt.getProfile().getUrl())
                .addDefinition(pt.getDefinition());
        Resource simpleRdfType = RDFTypeMap.xsd_type_for(ptName, owlTarget);
            if(RDFTypeMap.unionTypesMap.containsKey(ptName)) {  // complex types like dateTime that are a union of types
                ptRes.restriction(fact.fhir_cardinality_restriction(v, RDFTypeMap.unionTypesMap.get(ptName), 1, 1));
            } else if (simpleRdfType != null) {
                ptRes.restriction(fact.fhir_class_cardinality_restriction(v, simpleRdfType, 1, 1));
            }
    }


    /**
     * DefinedStringPattern Generator
     *
     * @param dsp FHIR DefinedStringPattern Type (e.g. id, oid, uuid)
     * @throws Exception
     */
    private void genDefinedStringPattern(DefinedStringPattern dsp) throws Exception {
        String dspType = dsp.getSchema();
        String dspTypeName = dspType.endsWith("+")? dspType.substring(0, dspType.length() - 1) : dspType;
        Resource dspTypeRes = RDFTypeMap.xsd_type_for(dspTypeName, owlTarget);

        FHIRResource dspRes = fact.fhir_class(dsp.getCode(), dsp.getBase())
                .addDefinition(dsp.getDefinition());

        if(dspRes != null) {
            if (dspType.endsWith("+")) {
                List<Resource> facets = new ArrayList<Resource>(1);
                facets.add(fact.fhir_pattern(dsp.getRegex()));
                dspRes.restriction(fact.fhir_restriction(v,
                        fact.fhir_datatype_restriction(dspTypeRes == XSD.xstring ? XSD.normalizedString : dspTypeRes, facets)));
            } else
                dspRes.restriction(fact.fhir_restriction(v, dspTypeRes));
        }
    }



    /**
     * TypeDefinition generator (e.g. code, id, markdown, uuid)
     *
     * @param td definition to generate
     * @throws Exception
     */
    private void genElementDefn(TypeDefn td) throws Exception {
        String typeName = td.getName();
        StructureDefinition typeSd = td.getProfile();
        String parentURL = typeSd.getBaseDefinitionElement().getValue();
        String parentName = null;
        // TODO: Figure out how to do this properly
        if (parentURL != null)
            parentName = getResourceNameFromCanonical(parentURL);
        FHIRResource typeRes =
                (td.getTypes().isEmpty() ? fact.fhir_class(typeName) : fact.fhir_class(typeName, parentName))
                        .addTitle(td.getShortDefn())
                        .addDefinition(td.getDefinition());
        // Add provenance directly from the TypeDefn's StructureDefinition if available
        if (typeSd != null && !Utilities.noString(typeSd.getUrl())) {
            typeRes.addProvenance(typeSd.getUrl());
        }
        String definitionCanonical = typeSd != null ? typeSd.getUrl() : null;
        processTypes(typeName, typeRes, td, typeName, false, definitionCanonical);
        if(classHasModifierExtensions.contains(parentName)) {
            genModifierExtensions(typeName, typeRes, parentName, definitionCanonical);
        }
    }

    /**
     * ProfiledType generator
     */
    private void genProfiledType(ProfiledType pt) throws Exception {
        fact.fhir_class_with_provenance(pt.getName(), pt.getBaseType(), pt.getProfile().getUrl())
                              .addTitle(pt.getDefinition())
                              .addDefinition(pt.getDescription());
        if (!Utilities.noString(pt.getInvariant().getTurtle())) {
            Model model = ModelFactory.createDefaultModel();
            model.read(pt.getInvariant().getTurtle());
            fact.merge_rdf(model);
        }
    }


    /**
     * Resource Definition generator
     *
     * @param rd Resource Definition to emit
     * @throws Exception
     */
    private void genResourceDefn(ResourceDefn rd) throws Exception {
        String resourceName = rd.getName();
        ElementDefn resourceType = rd.getRoot();
        String superClassName = resourceType.typeCode();
        String definitionCanonical = null;
        
        StructureDefinition rdProfile = rd.getProfile();
        if (rdProfile != null && !Utilities.noString(rdProfile.getUrl())) {
            definitionCanonical = rdProfile.getUrl();
        }

        Resource superClass = resourceType.getTypes().isEmpty() ? OWL2.Thing : RDFNamespace.FHIR.resourceRef(superClassName);
        
        FHIRResource rdRes = fact.fhir_class_with_provenance(resourceName, superClass, definitionCanonical)
                        .addDefinition(rd.getDefinition());

        processTypes(resourceName, rdRes, resourceType, resourceName, true, definitionCanonical);

        if(!Utilities.noString(resourceType.getW5()))
            rdRes.addObjectProperty(RDFS.subClassOf, RDFNamespace.W5.resourceRef(resourceType.getW5()));
        if(definitions.getResources().containsKey(resourceName)  && classHasModifierExtensions.contains(superClass.getLocalName())) { 
            //Bundle, Binary, Parameters, DomainResource should be excluded from this clause and not get modifier extensions here 
            // since they are under fhir:Resource instead of fhir:DomainResource
            genModifierExtensions(resourceName, rdRes, superClass.getLocalName(), definitionCanonical);
        }
    }

    /**
     * Generates corresponding ontology for Modifier Extensions of fhir:OriginalClass as fhir:_OriginalClass
     */
    private void genModifierExtensions(String baseName, FHIRResource baseFR, String parentName, String definitionCanonical) throws Exception {

            // could change to instantiate only once
            FHIRResource modifierExtensionClass = fact.fhir_resource("modifierExtensionClass", OWL2.AnnotationProperty, "modifierExtensionClass").addDataProperty(RDFS.comment, "has modifier extension class");
            Property modifierExtensionClassProperty = ResourceFactory.createProperty(modifierExtensionClass.resource.toString());

            FHIRResource modRes = fact.fhir_class_with_provenance("_" + baseName, definitionCanonical)
                    .addObjectProperty(RDFS.subClassOf, RDFNamespace.FHIR.resourceRef("_" + parentName));
            modRes.addDataProperty(RDFS.comment, "(Modified) " + baseName);
            baseFR.addObjectProperty(modifierExtensionClassProperty, modRes);

    }

    /**
     * Generates corresponding ontology for Modifier Extensions of fhir:OriginalProperty as fhir:_OriginalProperty
     */
    private void genPropertyModifierExtensions(String baseName, FHIRResource baseFR, String label, String definitionCanonical) throws Exception {
        if(baseName.equals("modifierExtension")) return; //skip the special case of fhir:modifierExtension

        // could change to instantiate only once
        FHIRResource hasExt = fact.fhir_resource("modifierExtensionProperty", OWL2.AnnotationProperty,"modifierExtensionProperty").addDataProperty(RDFS.comment, "has modifier extension property");
        Property extProp = ResourceFactory.createProperty(hasExt.resource.toString());  

        FHIRResource modRes = fact.fhir_objectProperty("_" + baseName, definitionCanonical);
        modRes.addDataProperty(RDFS.comment, "(Modified) " + label);
        baseFR.addObjectProperty(extProp, modRes);
    }


    /**
     * Iterate over the Element Definitions in baseResource generating restrictions and properties
     * @param baseResourceName Name of base resource
     * @param baseResource FHIRResource for base resource
     * @param td Inner type definitions
     * @param predicateBase Root name for predicate
     * @param innerIsBackbone True if we're processing a backbone element
     */
    private void processTypes(String baseResourceName, FHIRResource baseResource, ElementDefn td, String predicateBase, boolean innerIsBackbone, String definitionCanonical) throws Exception {
        for (ElementDefn ed : td.getElements()) {
            generateElementClasses(baseResourceName, baseResource, ed, predicateBase, innerIsBackbone, definitionCanonical);
        }
    }

    /**
     * Generate classes and properties for an element definition
     * @param baseResourceName Name of base resource
     * @param baseResource FHIRResource for base resource
     * @param ed Element definition to process
     * @param predicateBase Root name for predicate
     * @param innerIsBackbone True if we're processing a backbone element
     */
    private void generateElementClasses(String baseResourceName, FHIRResource baseResource, ElementDefn ed, String predicateBase, boolean innerIsBackbone, String definitionCanonical) throws Exception {
        // Example: ValueSet.compose + include -> ValueSet.compose.include
        String targetClassName = predicateBase + "." + (ed.getName().endsWith("[x]")?
                ed.getName().substring(0, ed.getName().length() - 3) : ed.getName());
        String shortenedPropertyName = shortenName(targetClassName);

        FHIRResource predicateResource = fact.fhir_objectProperty(shortenedPropertyName, definitionCanonical);

        genPropertyModifierExtensions(shortenedPropertyName, predicateResource, targetClassName, definitionCanonical);
        
        // Polymorphic / Choice types
        if (ed.getName().endsWith("[x]")) {
            baseResource = getChoiceElementRestriction(baseResource, ed, shortenedPropertyName, predicateResource, definitionCanonical);
            return;
        }

        // Monomorphic types
        FHIRResource targetElementClass;
        String targetTypeName = targetClassName;

        if (ed.getTypes().isEmpty()) {  //subnodes
            // Monomorphic complex type (no type specified, but has sub-elements)
            targetElementClass = fact.fhir_class(targetClassName, innerIsBackbone ? "BackboneElement" : "Element");

            // Recursively process sub-elements
            processTypes(targetClassName, targetElementClass, ed, targetClassName, innerIsBackbone, definitionCanonical);

        } else {
            // Monomorphic simple type
            TypeRef targetType = ed.getTypes().get(0);
            String targetName = targetType.getName();
            if (targetName.startsWith("@")) {        // Link to earlier definition
                // "Content reference" to another defined type
                ElementDefn targetRef = getElementForPath(targetName.substring(1));
                // Target type name includes @
                targetTypeName = targetRef.getName();
                
                // Remove @ from start of targetName (example: @ValueSet.compose.include)
                targetClassName = targetName.charAt(0) == '@' ? targetName.substring(1) : targetName;
                targetElementClass = fact.fhir_class(targetClassName, innerIsBackbone ? "BackboneElement" : "Element");

            } else { // doesn't start with "@"
                // A placeholder entry.  The rest of the information will be supplied elsewhere
                targetElementClass = fact.fhir_class(targetName);
                targetTypeName = targetName;

                // Update property restriction target if type can have a reference OR is not a primitive type ("canonical" is both primitive and referenceable)
                if (referenceTypes.contains(targetName) || isPrimitive(targetName)) {
                    targetElementClass = getPropertyRestriction(targetType);
                }
            }

            // XHTML the exception, in that the html doesn't derive from Primitive
            if (targetName.equals("xhtml")) {
                predicateResource = fact.fhir_objectProperty(shortenedPropertyName, definitionCanonical);
            } else {
                predicateResource = fact.fhir_objectProperty(shortenedPropertyName, definitionCanonical);
                genPropertyModifierExtensions(shortenedPropertyName, predicateResource, targetClassName, definitionCanonical);
            }
        }

        // Add provenance & definition annotations from source StructureDefinition for this element, except if it's a DataType (used in too many places)
        if (!isDataType(targetTypeName)) {
            targetElementClass.addProvenance(definitionCanonical);
            targetElementClass.addDefinition(ed.getDefinition()).addDefinition(ed.getShortDefn());
        }

        // Annotate object with disambiguating title
        predicateResource.addTitle(targetClassName + ": " + ed.getShortDefn());

        // Add property restrictions
        if(ed.getName().equals("modifierExtension") && ed.hasModifier()) {
            // special case for modifierExtensions on original Resources having a cardinality of zero
            baseResource.restriction(fact.fhir_class_cardinality_restriction(predicateResource.resource, targetElementClass.resource, 0, 0));
        } else {
            baseResource.restriction(
                fact.fhir_class_cardinality_restriction(predicateResource.resource, 
                    targetElementClass.resource, 
                    ed.getMinCardinality(), 
                    ed.getMaxCardinality()
                )
            );
        }
        if(!Utilities.noString(ed.getW5()))
            predicateResource.addObjectProperty(RDFS.subPropertyOf, RDFNamespace.W5.resourceRef(ed.getW5()));
    }

    /**
     * Generate restrictions for a choice element (e.g., value[x])
     * @param baseResource FHIRResource for base resource
     * @param ed Element definition to process
     * @param shortenedPropertyName Shortened property name
     * @param predicateResource FHIRResource for predicate
     * @param definitionCanonical Canonical URL of the relevant StructureDefinition
     * @return
     * @throws Exception
     */
    private FHIRResource getChoiceElementRestriction(FHIRResource baseResource, ElementDefn ed, String shortenedPropertyName, FHIRResource predicateResource, String definitionCanonical) throws Exception {
        // Choice entry
        if (ed.typeCode().equals("*")) {
            // Wild card -- any element works (probably should be more restrictive but...)
            Resource targetResource = RDFNamespace.FHIR.resourceRef("Element");
            baseResource.restriction(
                    fact.fhir_class_cardinality_restriction(
                            predicateResource.resource,
                            targetResource,
                            ed.getMinCardinality(),
                            ed.getMaxCardinality()));
        } else {
            // Create a restriction on the union of possible types
            List<Resource> typeRestrictions = new ArrayList<Resource>();
            for (TypeRef tr : ed.getTypes()) {
                Resource typeRestriction = getChoiceTypeRestriction(shortenedPropertyName, predicateResource, tr, definitionCanonical);
                typeRestrictions.add(typeRestriction);
            }
            // Add the type restrictions
            baseResource.restriction(fact.fhir_union(typeRestrictions));
            // Add the cardinality restrictions separately here
            baseResource.restriction(
                fact.build_cardinality_restrictions(predicateResource.resource, 
                    ed.getMinCardinality(), 
                    ed.getMaxCardinality())
            );
        }
        return baseResource;
    }

    /**
     * Generate the restriction for one of the choice types
     * @param shortenedPropertyName Shortened property name
     * @param predicateResource FHIRResource for predicate
     * @param typeRef TypeRef for the choice type
     * @param definitionCanonical Canonical URL of the relevant StructureDefinition
     * @return
     * @throws Exception
     */
    private Resource getChoiceTypeRestriction(String shortenedPropertyName, FHIRResource predicateResource, TypeRef typeRef, String definitionCanonical) throws Exception {
        FHIRResource targetRes = getPropertyRestriction(typeRef);

        FHIRResource shortPredicate = fact.fhir_objectProperty(shortenedPropertyName, predicateResource.resource, definitionCanonical);

        return fact.create_empty_owl_restriction(shortPredicate.resource)
                    .addObjectProperty(OWL2.allValuesFrom, targetRes.resource)
                    .resource;
    }

    /**
     * Generate the appropriate restriction for a property
     * @param typeRef
     * @param definitionCanonical
     * @return
     */
    private FHIRResource getPropertyRestriction(TypeRef typeRef) {
        String typeRefName = typeRef.getName();

        // If not a reference type, or a reference type with no target types specified, just return the base class
        if (!referenceTypes.contains(typeRefName) || !typeRef.hasParams()) {
            return fact.fhir_class(typeRefName);
        }

        // Build desired shape: intersectionOf( fhir:Reference/canonical , Restriction( onProperty fhir:link allValuesFrom ( Class unionOf ( targets ) ) ) )
        Resource referenceClass = fact.fhir_class(typeRefName).resource;
        List<Resource> targetClasses = new ArrayList<>();
        List<String> typeRefParams = typeRef.getParams();
        if (typeRefParams.isEmpty()) {
            // No target types specified, so just return fhir:Reference
            return fact.fhir_class(typeRefName);
        }
        for (String typeParam : typeRefParams) {
            // Map target profile URL to its local name class (e.g., Patient, Group)
            String tn = getResourceNameFromCanonical(typeParam);
            
            if ("Any".equals(tn)) {
                // Reference can be any type, so skip the rest and just return fhir:Reference
                return fact.fhir_class(typeRefName);
            }

            targetClasses.add(fact.fhir_class(tn).resource);
        }
        // Create a union class of all allowed target resource types
        FHIRResource union = fact.owl_class_union(targetClasses);
        // Build the restriction on fhir:link whose allValuesFrom points at the union class
        Resource linkProp = RDFNamespace.FHIR.resourceRef(fhirRdfLinkName);
        Resource linkRestriction = fact.fhir_restriction(linkProp)
                .addObjectProperty(OWL2.allValuesFrom, union.resource)
                .resource;
        // Finally intersect fhir:Reference with that restriction
        List<Resource> members = new ArrayList<>();
        members.add(referenceClass);
        // If it's a CodeableReference, the link restriction is on the inner Reference (fhir:reference)
        if (typeRefName.equals("CodeableReference")) {
            // Create an intersection class of fhir:Reference and the link restriction
            Resource innerReference = fact.fhir_class("Reference").resource;
            FHIRResource innerLinkIntersection = fact.owl_class_intersection(Arrays.asList(innerReference, linkRestriction));
            // Create a restriction on fhir:reference whose allValuesFrom points at that intersection
            Resource innerLinkProp = RDFNamespace.FHIR.resourceRef("reference");
            Resource innerLinkRestriction = fact.fhir_restriction(innerLinkProp)
                    .addObjectProperty(OWL2.allValuesFrom, innerLinkIntersection.resource)
                    .resource;
            members.add(innerLinkRestriction);
        } else {
            // Otherwise add directly on the reference type
            members.add(linkRestriction);
        }
        return fact.owl_class_intersection(members);
    }

    private ElementDefn getElementForPath(String pathname) throws Exception {
        String[] path = pathname.split("\\.");
        ElementDefn res = definitions.getElementDefn(path[0]);
        for (int i = 1; i < path.length; i++)
        {
            String en = path[i];
            if (en.length() == 0)
                throw new Exception("Improper path "+pathname);
            ElementDefn t = res.getElementByName(definitions, en, true, false, null);
            if (t == null) {
                throw new Exception("unable to resolve "+pathname);
            }
            res = t;
        }
        return res;
    }


    public void commit(boolean header) throws Exception {

        fact.serialize(destination);
        destination.flush();
        destination.close();
   }

    protected List<String> sorted(Set<String> keys) {
        List<String> names = new ArrayList<String>();
        names.addAll(keys);
        Collections.sort(names);
        return names;
    }

    protected boolean isPrimitive(String name) {
        return definitions.hasPrimitiveType(name)
                || (name.endsWith("Type")
                && definitions.getPrimitives().containsKey(name.substring(0, name.length()-4)));
    }

    protected boolean isDataType(String name) {
        return definitions.getTypes().containsKey(name) || isPrimitive(name);
    }

    // used for shortening property names
    private static String shortenName(String qualifiedName) {
        if(qualifiedName.contains(".")) {
            return qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1);
        }
        return qualifiedName;
    }

    private void addProvenanceForTypeName(FHIRResource fr, String typeName) {
        try {
            String url = null;
            ElementDefn ed = definitions.getElementDefn(typeName);
            if (ed instanceof TypeDefn) {
                StructureDefinition sdx = ((TypeDefn) ed).getProfile();
                if (sdx != null && !Utilities.noString(sdx.getUrl())) {
                    url = sdx.getUrl();
                }
            }
            if (!Utilities.noString(url)) {
                fr.addProvenance(url);
            }
        } catch (Throwable t) {
            // ignore – best-effort only
        }
    }

}