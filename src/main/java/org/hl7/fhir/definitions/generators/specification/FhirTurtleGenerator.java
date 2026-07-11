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
    private Resource v;
    private Property sdDescription;
    private Property edDefinition;
    private String host;
    private String fhirRdfPageUrl;

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
        this.fhirRdfPageUrl = getOntologyVersionIRI() + "rdf.html";

        this.fact = new FHIRResourceFactory();
        this.v = fact.fhir_resource("v", OWL2.DatatypeProperty, "fhir:v")
                .addComment("Terminal data value for primitive FHIR datatypes that can be represented as an RDF literal")
                .addProvenance(fhirRdfPageUrl)
                .resource;

        this.sdDescription = RDFS.comment;
        this.edDefinition = RDFS.comment;
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

        String versionedBase = getOntologyVersionIRI();
        String versionedIri = versionedBase + "fhir.ttl";
        String versionedW5 = versionedBase + "w5.ttl";

        fact.fhir_ontology("fhir.ttl", "FHIR Ontology")
                .addLiteral(RDFS.comment, "Formal model of FHIR resources. Classes are mapped from StructureDefinitions and complex ElementDefinitions. Object properties are mapped from ElementDefinitions and reused across different types of FHIR RDF resources for more terse serialization; these may be disambiguated within each context of usage.")
                .addObjectProperty(OWL2.versionIRI, ResourceFactory.createResource(versionedIri))
                .addDataProperty(OWL2.versionInfo, createdTimestamp.format(new Date()), XSDDatatype.XSDdateTime)
                .addObjectProperty(OWL2.imports, ResourceFactory.createResource(versionedW5))
                .addProvenance(fhirRdfPageUrl);
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
        FHIRResource nodeRole = fact.fhir_objectProperty("nodeRole", fhirRdfPageUrl)
                .addComment("Role of resource in FHIR RDF document. Example: fhir:treeRoot for top-level resource which may contain other resources.");

        // Resource can have max 1 nodeRole
        Resource.restriction(fact.create_empty_owl_restriction(nodeRole.resource).addDataProperty(OWL2.maxCardinality, "1", XSDDatatype.XSDinteger).resource);
        // Any element can have an index to assign order in a list
//        FHIRResource index = fact.fhir_dataProperty("index")
//                .addTitle("Ordering value for list")
//                .domain(Element)
//                .range(XSD.nonNegativeInteger);
//        Element.restriction(fact.fhir_cardinality_restriction(index.resource, XSD.nonNegativeInteger, 0, 1));

        // References have an optional link
        FHIRResource link = fact.fhir_resource(fhirRdfLinkName, OWL2.ObjectProperty, "fhir:" + fhirRdfLinkName)
                                .addProvenance(fhirRdfPageUrl)
                                .addComment("RDF IRI corresponding to a FHIR reference or URI");
        Reference.restriction(fact.fhir_cardinality_restriction(link.resource, 0, 1));

        // XHTML is an XML Literal. Not available in Definitions.java, but see https://hl7.org/fhir/xhtml.profile.html
        String xhtmlCanonical = "http://hl7.org/fhir/StructureDefinition/xhtml";
        var xhtmlClass = fact.fhir_class_with_provenance("xhtml", "PrimitiveType", xhtmlCanonical);
        // xhtml.value min 1, max 1
        xhtmlClass.restriction(fact.fhir_class_cardinality_restriction(v, RDF.xmlLiteral, 1, 1));
        // xhtml.extension max 0
        var extensionCardinality = fact.create_empty_owl_restriction(RDFNamespace.FHIR.resourceRef("extension"))
                                        .addDataProperty(OWL2.maxCardinality, "0", XSDDatatype.XSDinteger)
                                        .resource;
        xhtmlClass.restriction(extensionCardinality);
    }

    private String getResourceNameFromCanonical(String canonicalUrl) {
        return canonicalUrl.substring(canonicalUrl.lastIndexOf("/")+1);
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
        // `org.hl7.fhir.definitions.model.DefinedCode.definition` actually comes from StructureDefinition.description (roughly same as root ElementDefinition.definition)
        FHIRResource ptRes = fact.fhir_class_with_provenance(ptName, "PrimitiveType", pt.getProfile().getUrl())
                .addLiteral(this.sdDescription, pt.getDefinition());
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
                .addComment(dsp.getDefinition());

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
                (td.getTypes().isEmpty() ? fact.fhir_class(typeName) : fact.fhir_class(typeName, parentName));

        // Add provenance directly from the TypeDefn's StructureDefinition if available
        if (!Utilities.noString(typeSd.getUrl())) {
            typeRes.addProvenance(typeSd.getUrl());
        }

        // `org.hl7.fhir.definitions.model.TypeDefn.definition` actually comes from StructureDefinition.description (roughly same as root ElementDefinition.definition)
        var structureDefinitionDescription = td.getDefinition();
        typeRes.addLiteral(this.sdDescription, structureDefinitionDescription);

        String definitionCanonical = typeSd != null ? typeSd.getUrl() : null;
        processTypes(typeRes, td, typeName, false, definitionCanonical);
    }

    /**
     * ProfiledType generator
     */
    private void genProfiledType(ProfiledType pt) {
        fact.fhir_class_with_provenance(pt.getName(), pt.getBaseType(), pt.getProfile().getUrl())
                              .addComment(pt.getDescription());
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
        
        // `org.hl7.fhir.definitions.model.ResourceDefn.definition` actually comes from StructureDefinition.description (roughly same as root ElementDefinition.definition)
        var structureDefinitionDescription = rd.getDefinition();
        FHIRResource rdRes = fact.fhir_class_with_provenance(resourceName, superClass, definitionCanonical)
                        .addLiteral(this.sdDescription, structureDefinitionDescription);

        processTypes(rdRes, resourceType, resourceName, true, definitionCanonical);

        addW5Mapping(resourceType.getW5(), rdRes, RDFS.subClassOf);
    }

    private void addW5Mapping(String w5mapping, FHIRResource fhirResource, Property property ) {
        if(!Utilities.noString(w5mapping)) {
            for (String w5 : w5mapping.split(",\\s*")) {
                fhirResource.addObjectProperty(property, RDFNamespace.W5.resourceRef(w5));
            }
        }
    }

    /**
     * Iterate over the Element Definitions in baseResource generating restrictions and properties
     * @param baseResourceName Name of base resource
     * @param baseResource FHIRResource for base resource
     * @param td Inner type definitions
     * @param predicateBase Root name for predicate
     * @param innerIsBackbone True if we're processing a backbone element
     */
    private void processTypes(FHIRResource baseResource, ElementDefn td, String predicateBase, boolean innerIsBackbone, String definitionCanonical) throws Exception {
        List<ElementDefn> elements = td.getElements();
        for (int index = 0; index < elements.size(); index++) {
            // Track element index for sorting later
            ElementDefn ed = elements.get(index);
            generateElementClasses(index, baseResource, ed, predicateBase, innerIsBackbone, definitionCanonical);
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
    private void generateElementClasses(int index, FHIRResource baseResource, ElementDefn ed, String predicateBase, boolean innerIsBackbone, String definitionCanonical) throws Exception {
        // Example: ValueSet.compose + include -> ValueSet.compose.include
        String targetClassName = predicateBase + "." + (ed.getName().endsWith("[x]")?
                ed.getName().substring(0, ed.getName().length() - 3) : ed.getName());
        String shortenedPropertyName = shortenName(targetClassName);

        String elementPath = targetClassName;
        String elementDefinitionCanonical = definitionCanonical + "#" + elementPath;
        FHIRResource predicateResource = fact.fhir_objectProperty(shortenedPropertyName, elementDefinitionCanonical);
        
        // Polymorphic / Choice types
        if (ed.getName().endsWith("[x]")) {
            baseResource = getChoiceElementRestriction(baseResource, ed, shortenedPropertyName, predicateResource, definitionCanonical, index);
            return;
        }

        // Monomorphic types
        FHIRResource targetElementClass;
        String targetTypeName = targetClassName;
        boolean isContentReference = false;

        boolean isComplexElement = isComplexElement(ed);
        if (isComplexElement) {  //subnodes
            // Monomorphic complex type (no type specified, but has sub-elements) -- example: Patient.contact
            targetElementClass = fact.fhir_class(targetClassName, innerIsBackbone ? "BackboneElement" : "Element");

            // Recursively process sub-elements
            processTypes(targetElementClass, ed, targetClassName, innerIsBackbone, definitionCanonical);

        } else {
            // Monomorphic simple type -- example: Patient.active
            TypeRef targetType = ed.getTypes().get(0);
            String targetName = targetType.getName();
            isContentReference = targetName.startsWith("@");
            if (isContentReference) {        // Link to earlier definition
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
                predicateResource = fact.fhir_objectProperty(shortenedPropertyName, elementDefinitionCanonical);
            } else {
                predicateResource = fact.fhir_objectProperty(shortenedPropertyName, elementDefinitionCanonical);
            }
        }

        // BackboneElements only: Add provenance & definition annotations from source StructureDefinition (example: Patient.contact class annotated from ElementDefinition)
        // Don't do this for other kinds of Elements like DataTypes (used in too many places)
        if (isComplexElement) {
            if (!isContentReference) {
                targetElementClass.addProvenance(elementDefinitionCanonical);
                targetElementClass.addLiteral(this.edDefinition, ed.getDefinition());
            }
        }

        // Annotate object with disambiguating title
        predicateResource.addComment(targetClassName + ": " + ed.getShortDefn());

        // Add property restrictions
        // baseResource.restriction(
        //     fact.fhir_class_cardinality_restriction(predicateResource.resource, 
        //         targetElementClass.resource, 
        //         ed.getMinCardinality(), 
        //         ed.getMaxCardinality()
        //     )
        // );
        List<Resource> restrictions = fact.fhir_class_cardinality_restriction(
                predicateResource.resource,
                targetElementClass.resource,
                ed.getMinCardinality(),
                ed.getMaxCardinality()
        );
        addElementOrderedRestrictions(baseResource, restrictions, index);

        addW5Mapping(ed.getW5(), predicateResource, RDFS.subPropertyOf);
    }

    /**
     * Add set of restrictions to a class and track their order for sorting later during serialization
     */
    private void addElementOrderedRestrictions(FHIRResource baseResource, List<Resource> classExpressions, int elementIndex) {
        fact.registerOrderedClassExpressions(classExpressions, elementIndex);
        baseResource.restriction(classExpressions);
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
    private FHIRResource getChoiceElementRestriction(FHIRResource baseResource, ElementDefn ed, String shortenedPropertyName, FHIRResource predicateResource, String definitionCanonical, int index) throws Exception {
        // Choice entry
        List<Resource> restrictions;
        if (ed.typeCode().equals("*")) {
            // Wild card -- any element works (probably should be more restrictive but...)
            Resource targetResource = RDFNamespace.FHIR.resourceRef("Element");
            restrictions = fact.fhir_class_cardinality_restriction(
                    predicateResource.resource,
                    targetResource,
                    ed.getMinCardinality(),
                    ed.getMaxCardinality());
        } else {
            // Create a restriction on the union of possible types
            List<Resource> typeRestrictions = new ArrayList<Resource>();
            for (TypeRef tr : ed.getTypes()) {
                Resource typeRestriction = getChoiceTypeRestriction(shortenedPropertyName, predicateResource, tr, definitionCanonical);
                typeRestrictions.add(typeRestriction);
            }
            restrictions = new ArrayList<Resource>();
            restrictions.add(fact.fhir_union(typeRestrictions));
            restrictions.addAll(fact.build_cardinality_restrictions(
                    predicateResource.resource,
                    ed.getMinCardinality(),
                    ed.getMaxCardinality()));
        }
        addElementOrderedRestrictions(baseResource, restrictions, index);
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
        // Create a union node only when more than one target type is allowed.
        Resource allowedTargetResource = targetClasses.size() == 1
            ? targetClasses.get(0)
            : fact.owl_class_union(targetClasses).resource;

        // Build the restriction on fhir:link whose allValuesFrom points at the union class
        Resource linkProp = RDFNamespace.FHIR.resourceRef(fhirRdfLinkName);
        Resource linkRestriction = fact.fhir_restriction(linkProp)
                .addObjectProperty(OWL2.allValuesFrom, allowedTargetResource)
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

    protected boolean isComplexElement(ElementDefn ed) {
        return ed.getTypes().isEmpty();
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