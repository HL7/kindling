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
        this.value = fact.fhir_resource("value", OWL2.ObjectProperty, "fhir:value")
                .addTitle("Terminal data value")
                .resource;
        this.v = fact.fhir_resource("v", OWL2.DatatypeProperty, "fhir:v")
                .addTitle("Terminal data value for primitive FHIR datatypes that can be represented as a RDF literal")
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
                genBaseModifierExtensionCode(infn, defn.getProfile().getBaseDefinitionElement().getValue());
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
            if(defn.getRoot().enablesModifierExtensions()) {
                genBaseModifierExtensionCode(n, defn.getProfile().getBaseDefinitionElement().getValue());
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
                .addDataProperty(RDFS.comment, "Formal model of FHIR Clinical Resources")
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
        FHIRResource Element = fact.fhir_class("Element");
        FHIRResource Reference = fact.fhir_class("Reference");

        // Primitive isn't in the actual model - added here
        fact.fhir_class("Primitive")
                .addTitle("Types with only a value")
                .addDefinition("Types with only a value and no additional elements as children")
                .restriction(fact.fhir_restriction(v, RDFS.Literal));

        // A resource can have an optional nodeRole
        FHIRResource treeRoot = fact.fhir_class("treeRoot")
                .addTitle("Class of FHIR base documents");
        FHIRResource nodeRole = fact.fhir_objectProperty("nodeRole")
                .addTitle("Identifies role of subject in context of a given document")
                .domain(Resource)
                .range(treeRoot.resource);
        Resource.restriction(fact.fhir_cardinality_restriction(nodeRole.resource, treeRoot.resource, 0, 1));


        // Any element can have an index to assign order in a list
//        FHIRResource index = fact.fhir_dataProperty("index")
//                .addTitle("Ordering value for list")
//                .domain(Element)
//                .range(XSD.nonNegativeInteger);
//        Element.restriction(fact.fhir_cardinality_restriction(index.resource, XSD.nonNegativeInteger, 0, 1));

        // References have an optional link
        FHIRResource link = fact.fhir_objectProperty("link").addTitle("URI of a reference");
        Reference.restriction(fact.fhir_cardinality_restriction(link.resource, Resource.resource, 0, 1));

        // XHTML is an XML Literal. Not available in Definitions.java, but see https://hl7.org/fhir/xhtml.profile.html
        fact.fhir_class("xhtml", "Element")
            .restriction(fact.fhir_cardinality_restriction(v, RDF.xmlLiteral, 1, 1));
    }

    /**
     * Generates Modifier Extension Code for superclass
     * At current time these superclasses are: DomainResource, BackboneElement, BackboneType
     */
    private void genBaseModifierExtensionCode(String className, String parentUrl) throws Exception {
            classHasModifierExtensions.add(className);  // keep track of which classes enable Modifier extensions

            FHIRResource originalResource = fact.fhir_class(className);

            FHIRResource modResource = fact.fhir_class("_"+className);

            if(parentUrl != null) {
                String parentName = parentUrl.substring(parentUrl.lastIndexOf("/")+1);
                Resource parentRes = RDFNamespace.FHIR.resourceRef(parentName);
                modResource.addObjectProperty(RDFS.subClassOf, parentRes);
            }

            Resource modifierExtensionProperty = RDFNamespace.FHIR.resourceRef(("modifierExtension"));

            FHIRResource cardRestriction = fact.fhir_bnode().addType(OWL2.Restriction).addDataProperty(OWL2.minCardinality, "1", XSDDatatype.XSDinteger)
                    .addObjectProperty(OWL2.onProperty, modifierExtensionProperty);
            modResource.restriction(cardRestriction.resource);
            FHIRResource extRestriction = fact.fhir_bnode().addType(OWL2.Restriction)
                    .addObjectProperty(OWL2.onProperty, modifierExtensionProperty)
                    .addObjectProperty(OWL2.allValuesFrom, fact.fhir_class("Extension"));
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
        FHIRResource ptRes = fact.fhir_class(ptName, "Primitive")
                .addDefinition(pt.getDefinition());
        Resource simpleRdfType = RDFTypeMap.xsd_type_for(ptName, owlTarget);
            if(RDFTypeMap.unionTypesMap.containsKey(ptName)) {  // complex types like dateTime that are a union of types
                ptRes.restriction(fact.fhir_cardinality_restriction(v, RDFTypeMap.unionTypesMap.get(ptName), 1, 1));
            } else if (simpleRdfType != null) {
                ptRes.restriction(fact.fhir_cardinality_restriction(v, simpleRdfType, 1, 1));
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
            parentName = parentURL.substring(parentURL.lastIndexOf("/")+1);
        FHIRResource typeRes =
                (td.getTypes().isEmpty() ? fact.fhir_class(typeName) : fact.fhir_class(typeName, parentName))
                        .addTitle(td.getShortDefn())
                        .addDefinition(td.getDefinition());
        processTypes(typeName, typeRes, td, typeName, false);
        if(classHasModifierExtensions.contains(parentName)) {
            genModifierExtensions(typeName, typeRes, parentName);
        }
    }

    /**
     * ProfiledType generator
     */
    private void genProfiledType(ProfiledType pt) throws Exception {
        fact.fhir_class(pt.getName(), pt.getBaseType()).addTitle(pt.getDefinition()).addDefinition(pt.getDescription());
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
        Resource resource = resourceType.getTypes().isEmpty() ? OWL2.Thing : RDFNamespace.FHIR.resourceRef(resourceType.typeCode());
        FHIRResource rdRes =
                fact.fhir_class(resourceName, resource)
                        .addDefinition(rd.getDefinition());
        processTypes(resourceName, rdRes, resourceType, resourceName, true);
        if(!Utilities.noString(resourceType.getW5()))
            rdRes.addObjectProperty(RDFS.subClassOf, RDFNamespace.W5.resourceRef(resourceType.getW5()));
        if(definitions.getResources().containsKey(resourceName)  && classHasModifierExtensions.contains(resource.getLocalName())) { 
            //Bundle, Binary, Parameters, DomainResource should be excluded from this clause and not get modifier extensions here 
            // since they are under fhir:Resource instead of fhir:DomainResource
            genModifierExtensions(resourceName, rdRes, resource.getLocalName());
        }
    }

    /**
     * Generates corresponding ontology for Modifier Extensions of fhir:OriginalClass as fhir:_OriginalClass
     */
    private void genModifierExtensions(String baseName, FHIRResource baseFR, String parentName) throws Exception {

            // could change to instantiate only once
            FHIRResource modifierExtensionClass = fact.fhir_resource("modifierExtensionClass", OWL2.AnnotationProperty, "modifierExtensionClass").addDataProperty(RDFS.comment, "has modifier extension class");
            Property modifierExtensionClassProperty = ResourceFactory.createProperty(modifierExtensionClass.resource.toString());

            FHIRResource modRes = fact.fhir_class("_" + baseName)
                    .addObjectProperty(RDFS.subClassOf, RDFNamespace.FHIR.resourceRef("_" + parentName));
            modRes.addDataProperty(RDFS.comment, "(Modified) " + baseName);
            baseFR.addObjectProperty(modifierExtensionClassProperty, modRes);

    }

    /**
     * Generates corresponding ontology for Modifier Extensions of fhir:OriginalProperty as fhir:_OriginalProperty
     */
    private void genPropertyModifierExtensions(String baseName, FHIRResource baseFR, String label) throws Exception {
        if(baseName.equals("modifierExtension")) return; //skip the special case of fhir:modifierExtension

        // could change to instantiate only once
        FHIRResource hasExt = fact.fhir_resource("modifierExtensionProperty", OWL2.AnnotationProperty,"modifierExtensionProperty").addDataProperty(RDFS.comment, "has modifier extension property");
        Property extProp = ResourceFactory.createProperty(hasExt.resource.toString());  

        FHIRResource modRes = fact.fhir_objectProperty("_" + baseName);
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
    HashSet<String> processing = new HashSet<String>();
    private void processTypes(String baseResourceName, FHIRResource baseResource, ElementDefn td, String predicateBase, boolean innerIsBackbone)
            throws Exception {

        for (ElementDefn ed : td.getElements()) {
            String predicateName = predicateBase + "." + (ed.getName().endsWith("[x]")?
                    ed.getName().substring(0, ed.getName().length() - 3) : ed.getName());
            String shortenedPropertyName = shortenName(predicateName);
            FHIRResource predicateResource;

            if (ed.getName().endsWith("[x]")) {
                predicateResource = fact.fhir_objectProperty(shortenedPropertyName);
                genPropertyModifierExtensions(shortenedPropertyName, predicateResource, predicateName);

                // Choice entry
                if (ed.typeCode().equals("*")) {
                    // Wild card -- any element works (probably should be more restrictive but...)
                    Resource targetResource = RDFNamespace.FHIR.resourceRef("Element");
                    baseResource.restriction(
                            fact.fhir_cardinality_restriction(
                                    predicateResource.resource,
                                    targetResource,
                                    ed.getMinCardinality(),
                                    ed.getMaxCardinality()));
                } else {
                    // Create a restriction on the union of possible types
                    List<Resource> typeOpts = new ArrayList<Resource>();
                    for (TypeRef tr : ed.getTypes()) {
                        Resource targetRes = fact.fhir_class(tr.getName()).resource;
//                        FHIRResource shortPredicate = fact.fhir_objectProperty(shortenedPropertyName, predicateResource.resource, predicateName);
                        FHIRResource shortPredicate = fact.fhir_objectProperty(shortenedPropertyName, predicateResource.resource).addDataProperty(RDFS.comment, predicateName);
                        typeOpts.addAll(
                                fact.fhir_cardinality_restriction(shortPredicate.resource,
                                        targetRes,
                                        ed.getMinCardinality(),
                                        ed.getMaxCardinality()));
                    }
                    baseResource.restriction(fact.fhir_union(typeOpts));
                }
            } else {  // does not end with [x]
                FHIRResource baseDef;
                if (ed.getTypes().isEmpty()) {  //subnodes
                    predicateResource = fact.fhir_objectProperty(shortenedPropertyName);
                    genPropertyModifierExtensions(shortenedPropertyName, predicateResource, predicateName);
                    String targetClassName = mapComponentName(baseResourceName, ed.getDeclaredTypeName());
                    String shortedClassName = shortenName(targetClassName);
                    baseDef = fact.fhir_class(shortedClassName, innerIsBackbone ? "BackboneElement" : "Element")
                            .addDefinition(targetClassName + ": " + ed.getDefinition());
                    processTypes(targetClassName, baseDef, ed, predicateName, innerIsBackbone);
                } else {
                    TypeRef targetType = ed.getTypes().get(0);
                    String targetName = targetType.getName();
                    if (targetName.startsWith("@")) {        // Link to earlier definition
                        ElementDefn targetRef = getElementForPath(targetName.substring(1));
                        String targetRefName = targetRef.getName();
                        String targetClassName = baseResourceName +
                                Character.toUpperCase(targetRefName.charAt(0)) + targetRefName.substring(1);
                        baseDef = fact.fhir_class(targetClassName, innerIsBackbone ? "BackboneElement" : "Element")
                                .addDefinition(ed.getDefinition())
                                .addTitle(ed.getShortDefn());
                        if (!processing.contains(targetRefName)) {
                            processing.add(targetRefName);
                            processTypes(targetClassName, baseDef, targetRef, predicateName, innerIsBackbone);
                            processing.remove(targetRefName);
                        }
                    } else { // doesn't start with "@"
                        // A placeholder entry.  The rest of the information will be supplied elsewhere
                        baseDef = fact.fhir_class(targetName);
                    }
                    // XHTML the exception, in that the html doesn't derive from Primitive
                    if (targetName.equals("xhtml"))
                        predicateResource = fact.fhir_objectProperty(shortenedPropertyName);
                    else
                        predicateResource = fact.fhir_objectProperty(shortenedPropertyName);
                    genPropertyModifierExtensions(shortenedPropertyName, predicateResource, predicateName);
                }
                predicateResource.addTitle(predicateName + ": " + ed.getShortDefn())
                        .addDefinition(predicateName + ": " + ed.getDefinition());

                if(ed.getName().equals("modifierExtension") && ed.hasModifier()) {
                    // special case for modifierExtensions on original Resources having a cardinality of zero
                    baseResource.restriction(fact.fhir_cardinality_restriction(predicateResource.resource, baseDef.resource, 0, 0));
                } else {
                    baseResource.restriction(
                            fact.fhir_cardinality_restriction(predicateResource.resource, baseDef.resource, ed.getMinCardinality(), ed.getMaxCardinality()));
                }
                if(!Utilities.noString(ed.getW5()))
                    predicateResource.addObjectProperty(RDFS.subPropertyOf, RDFNamespace.W5.resourceRef(ed.getW5()));
            }
        }
    }

    private String mapComponentName(String baseResourceName, String componentName) {
        return componentName.startsWith(baseResourceName)? componentName : baseResourceName + "." + componentName;
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

    // used for shortening property names
    private static String shortenName(String qualifiedName) {
        if(qualifiedName.contains(".")) {
            return qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1);
        }
        return qualifiedName;
    }
}
