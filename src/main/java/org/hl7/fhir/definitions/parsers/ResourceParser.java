package org.hl7.fhir.definitions.parsers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.definitions.generators.specification.ToolResourceUtilities;
import org.hl7.fhir.definitions.model.BindingSpecification;
import org.hl7.fhir.definitions.model.BindingSpecification.AdditionalBinding;
import org.hl7.fhir.definitions.model.BindingSpecification.BindingMethod;
import org.hl7.fhir.definitions.model.ConstraintStructure;
import org.hl7.fhir.definitions.model.Definitions;
import org.hl7.fhir.definitions.model.ElementDefn;
import org.hl7.fhir.definitions.model.Example;
import org.hl7.fhir.definitions.model.Example.ExampleType;
import org.hl7.fhir.definitions.model.ImplementationGuideDefn;
import org.hl7.fhir.definitions.model.Invariant;
import org.hl7.fhir.definitions.model.Operation;
import org.hl7.fhir.definitions.model.Operation.OperationExample;
import org.hl7.fhir.definitions.model.OperationParameter;
import org.hl7.fhir.definitions.model.Profile;
import org.hl7.fhir.definitions.model.ResourceDefn;
import org.hl7.fhir.definitions.model.ResourceDefn.SecurityCategorization;
import org.hl7.fhir.definitions.model.SearchParameterDefn;
import org.hl7.fhir.definitions.model.SearchParameterDefn.CompositeDefinition;
import org.hl7.fhir.definitions.model.SearchParameterDefn.SearchType;
import org.hl7.fhir.definitions.model.TypeDefn;
import org.hl7.fhir.definitions.model.TypeRef;
import org.hl7.fhir.definitions.model.W5Entry;
import org.hl7.fhir.definitions.model.WorkGroup;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.FHIRFormatError;
import org.hl7.fhir.r5.conformance.profile.ProfileUtilities;
import org.hl7.fhir.r5.context.CanonicalResourceManager;
import org.hl7.fhir.r5.extensions.ExtensionUtilities;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.ConceptMap;
import org.hl7.fhir.r5.model.ElementDefinition;
import org.hl7.fhir.r5.model.ElementDefinition.AdditionalBindingPurposeVS;
import org.hl7.fhir.r5.model.ElementDefinition.ElementDefinitionBindingAdditionalComponent;
import org.hl7.fhir.r5.model.ElementDefinition.ElementDefinitionBindingComponent;
import org.hl7.fhir.r5.model.ElementDefinition.ElementDefinitionConstraintComponent;
import org.hl7.fhir.r5.model.ElementDefinition.ElementDefinitionMappingComponent;
import org.hl7.fhir.r5.model.ElementDefinition.PropertyRepresentation;
import org.hl7.fhir.r5.model.ElementDefinition.TypeRefComponent;
import org.hl7.fhir.r5.model.Enumeration;
import org.hl7.fhir.r5.model.Enumerations.BindingStrength;
import org.hl7.fhir.r5.model.Enumerations.FHIRVersion;
import org.hl7.fhir.r5.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r5.model.Enumerations.SearchParamType;
import org.hl7.fhir.r5.model.Enumerations.VersionIndependentResourceTypesAll;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.ImplementationGuide;
import org.hl7.fhir.r5.model.ImplementationGuide.ImplementationGuideDefinitionResourceComponent;
import org.hl7.fhir.r5.model.ListResource;
import org.hl7.fhir.r5.model.ListResource.ListResourceEntryComponent;
import org.hl7.fhir.r5.model.OperationDefinition;
import org.hl7.fhir.r5.model.OperationDefinition.OperationDefinitionParameterBindingComponent;
import org.hl7.fhir.r5.model.OperationDefinition.OperationDefinitionParameterComponent;
import org.hl7.fhir.r5.model.OperationDefinition.OperationParameterScope;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.SearchParameter;
import org.hl7.fhir.r5.model.SearchParameter.SearchParameterComponentComponent;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.StructureDefinition.StructureDefinitionMappingComponent;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.terminologies.CodeSystemUtilities;
import org.hl7.fhir.r5.terminologies.ConceptMapUtilities;
import org.hl7.fhir.r5.terminologies.ValueSetUtilities;
import org.hl7.fhir.r5.utils.BuildExtensions;
import org.hl7.fhir.r5.utils.CanonicalResourceUtilities;
import org.hl7.fhir.r5.extensions.ExtensionDefinitions;
import org.hl7.fhir.tools.publisher.BuildWorkerContext;
import org.hl7.fhir.utilities.PathBuilder;
import org.hl7.fhir.utilities.StandardsStatus;
import org.hl7.fhir.utilities.FileUtilities;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.filesystem.CSFile;
import org.hl7.fhir.utilities.filesystem.CSFileInputStream;

public class ResourceParser {

  private static final int STAR_TYPES_COUNT = 51;
  private Definitions definitions;
  private BuildWorkerContext context;
  private String folder;
  private String srcDir;
  private Map<String, Invariant> invariants = new HashMap<>();
  private WorkGroup committee;
  private OIDRegistry registry;
  private Map<String, StructureDefinition> sdList = new HashMap<>();
  String version;
  private CanonicalResourceManager<ConceptMap> maps;

  public ResourceParser(String srcDir, Definitions definitions, BuildWorkerContext context, WorkGroup committee, OIDRegistry registry, String version, CanonicalResourceManager<ConceptMap> maps) {
    this.srcDir = srcDir;
    this.definitions = definitions;
    this.context = context;
    this.committee = committee;
    this.registry = registry;
    this.version = version;
    this.maps = maps;
  }

  public ResourceDefn parse(String n, String t) throws FHIRException, Exception {
    this.folder = Utilities.path(srcDir, n);
    // what to parse:
    ResourceDefn r = parseResource(t);
    parseSearchParameters(r, n, t);
    parseExamples(r, n, t);
    parseOperations(r, n, t);
    parsePacks(r, n, t);
    parseLiquid(r);
    File lt = new CSFile(Utilities.path(this.folder, r.getName()+"-release-notes.xml"));
    r.setNotes(lt.exists());
    return r;
  }

  private void parseLiquid(ResourceDefn r) throws IOException, FileNotFoundException {
    File lt = new CSFile(Utilities.path(this.folder, r.getName()+".liquid"));
    if (lt.exists()) {
      r.setLiquid(FileUtilities.fileToString(lt));
    }
    lt = new CSFile(Utilities.path(this.folder, r.getName()+".liquid.md"));
    if (lt.exists()) {
      r.setLiquidNotes(FileUtilities.fileToString(lt));
    }
  }

  private void parsePacks(ResourceDefn r, String n, String t) throws Exception {
    Set<String> codes = new HashSet<>();
    ListResource list = (ListResource) parseXml("list-"+t+"-packs.xml");
    for (ListResourceEntryComponent le : list.getEntry()) {
      String id = le.getItem().getReference().substring(le.getItem().getReference().indexOf("/")+1);
      if (codes.contains(id)) {
        throw new FHIRException("Duplicate code "+id+" for resource "+n);
      }
      codes.add(id);
      ImplementationGuide ig = (ImplementationGuide) parseXml("implementationguide-"+id+".xml");
      if (!id.startsWith(r.getName()+"-")) {
        throw new FHIRException("Illegal ig name "+id+" - should start with "+r.getName()+"-");
      }
      id = id.substring(t.length()+1);
      r.getConformancePackages().add(convertPack(ig, id, r.getWg()));
    }
  }

  
  private Profile convertPack(ImplementationGuide ig, String id, WorkGroup wg) throws Exception {
    Profile p = new Profile(id.substring(id.indexOf("-")+1));
    p.setTitle(ig.getTitle());
    p.forceMetadata("id", ig.getId());
    if (ig.hasName()) {
      p.forceMetadata("name", ig.getName());
    }
    if (ig.hasPublisher()) {
      p.forceMetadata("author.name", ig.getPublisher());
    }
    if (ig.hasExtension(BuildExtensions.EXT_CODE)) {
      p.forceMetadata("code", ExtensionUtilities.readStringExtension(ig, BuildExtensions.EXT_CODE));
    }
    if (ig.hasExtension(BuildExtensions.EXT_FMM_LEVEL)) {
      p.forceMetadata("fmm", ExtensionUtilities.readStringExtension(ig, BuildExtensions.EXT_FMM_LEVEL));      
    }
    if (ig.hasExtension(BuildExtensions.EXT_STANDARDS_STATUS)) {
      p.forceMetadata("standards-status", ExtensionUtilities.readStringExtension(ig, BuildExtensions.EXT_STANDARDS_STATUS));      
    }
    if (ig.hasDescription()) {
      p.forceMetadata("description", ig.getDescription());
    }
    if (ig.hasStatus()) {
      p.forceMetadata("status", ig.getStatus().toCode());
    }
    if (ig.hasVersion()) {
      p.forceMetadata("version", ig.getVersion());
    }
    if (ig.hasTitle()) {
      p.forceMetadata("display", ig.getTitle());
    }
    if (ig.hasExtension(BuildExtensions.EXT_WORKGROUP)) {
      p.forceMetadata("workgroup", ExtensionUtilities.readStringExtension(ig, BuildExtensions.EXT_WORKGROUP));      
    }
    if (ig.hasDate()) {
      p.forceMetadata("date", ig.getDateElement().primitiveValue());
    }
    if (ig.hasExperimental()){
      p.forceMetadata("Experimental", ig.getExperimentalElement().primitiveValue());
    }

    if (ig.hasExtension(BuildExtensions.EXT_INTRODUCTION)) {
      p.setIntroduction(Utilities.path(folder, ExtensionUtilities.readStringExtension(ig, BuildExtensions.EXT_INTRODUCTION)));
    }    

    if (ig.hasExtension(BuildExtensions.EXT_NOTES)) {
      p.setNotes(Utilities.path(folder, ExtensionUtilities.readStringExtension(ig, BuildExtensions.EXT_NOTES)));
    }    

    for (ImplementationGuideDefinitionResourceComponent res : ig.getDefinition().getResource()) {
      String ref = res.getReference().getReference();
      String type = ref.substring(0, ref.indexOf("/"));
      String rid = ref.substring(ref.indexOf("/")+1); 
      if ("StructureDefinition".equals(type)) {
        StructureDefinition sd;
        if (new File(Utilities.path(folder, "structuredefinition-extension-"+rid+".xml")).exists()) {
          sd = (StructureDefinition) parseXml("structuredefinition-extension-"+rid+".xml");
          sd.setWebPath("extension-"+sd.getId()+".html");
          sd.setVersion(context.getVersion());
          p.getExtensions().add(sd);
          if (!sd.hasExtension(ExtensionDefinitions.EXT_WORKGROUP)) {
            CanonicalResourceUtilities.setHl7WG(sd, wg.getCode());
          }
          context.cacheResource(sd);
        } else {
          ConstraintStructure tp = processProfile(rid, ig.getId().substring(ig.getId().indexOf("-")+1), res, wg);
          sd = tp.getResource();
          sd.setVersion(context.getVersion());
          sd.setWebPath(sd.getId()+".html");
          if (!sd.hasExtension(ExtensionDefinitions.EXT_WORKGROUP)) {
            CanonicalResourceUtilities.setHl7WG(sd, wg.getCode());
          }
          p.getProfiles().add(tp); 
        }
        if (ProfileUtilities.isExtensionDefinition(sd)) {
          throw new FHIRException("Extension found at "+res.getReference().getReference()+" - this is an error since extensions have moved to https://github.com/HL7/fhir-extensions");
        }
        sd.setUserData(ToolResourceUtilities.NAME_RES_IG, id);
        sd.setVersion(version);
        sd.setFhirVersion(FHIRVersion.fromCode(version));
        for (ElementDefinition ed : sd.getDifferential().getElement()) {
          if (ed.hasBinding() && ed.getBinding().hasValueSet()) { 
            loadValueSet(ed.getBinding().getValueSet(), true, ed.getBinding().getStrength());
          }
        }
      } else if ("SearchParameter".equals(type)) {
        SearchParameter sp = processSearchParameter(rid);
        p.getSearchParameters().add(sp); 
      } else {
        // example
        p.getExamples().add(makeExample(res, ref, type, rid));
      }
    }      
    return p;
  }

  private Example makeExample(ImplementationGuideDefinitionResourceComponent res, String ref, String type, String rid) throws IOException, Exception {
    Example ex = new Example(context, res.getName(), rid, res.getDescription(), new File(Utilities.path(folder, type.toLowerCase()+"-"+rid+".xml")), true, ExampleType.XmlFile, false);     
    return ex;
  }

  private ConstraintStructure processProfile(String rid, String igId, ImplementationGuideDefinitionResourceComponent res, WorkGroup wg) throws FHIRFormatError, FileNotFoundException, IOException {
    StructureDefinition sd = (StructureDefinition) parseXml("structuredefinition-profile-"+rid+".xml");
    ImplementationGuideDefn ig = definitions.getIgs().get(ExtensionUtilities.readStringExtension(res, igId));
    if (ig == null ) {
      ig = definitions.getIgs().get("core");
    }
    sd.setVersion(version);
    sd.setFhirVersion(FHIRVersion.fromCode(version));

    ConstraintStructure cs = new ConstraintStructure(sd, ig, wg == null || sd.hasExtension(ExtensionDefinitions.EXT_WORKGROUP) ? wg(sd) : wg, fmm(sd), false);
    return cs;
  }

  private String fmm(StructureDefinition sd) {
    return ExtensionUtilities.readStringExtension(sd, ExtensionDefinitions.EXT_FMM_LEVEL);
  }

  private WorkGroup wg(StructureDefinition ed) {
    return definitions.getWorkgroups().get(ExtensionUtilities.readStringExtension(ed, ExtensionDefinitions.EXT_WORKGROUP));
  }

  private SearchParameter processSearchParameter(String rid) throws FHIRFormatError, FileNotFoundException, IOException {
    return (SearchParameter) parseXml("searchparameter-profile-"+rid+".xml");
  }

  private void parseOperations(ResourceDefn r, String n, String t) throws FHIRException, Exception {
    ListResource list = (ListResource) parseXml("list-"+t+"-operations.xml");
    for (ListResourceEntryComponent le : list.getEntry()) {
      String id = le.getItem().getReference().substring(le.getItem().getReference().indexOf("/")+1);
      OperationDefinition opd = (OperationDefinition) parseXml("operationdefinition-"+id+".xml");
      opd.setVersion(version);
      Operation op = convertOperation(opd);
      r.getOperations().add(op);
      if (r.getName().equals("Resource")) {
        definitions.genOpList().add(op);    
      }
    }
  }

  private Operation convertOperation(OperationDefinition src) throws FileNotFoundException, FHIRException, IOException, Exception {

    List<OperationExample> reqExamples = new ArrayList<>();
    List<OperationExample> respExamples = new ArrayList<>();
    List<OperationExample> examples2 = new ArrayList<>();
    for (Extension ex : src.getExtensionsByUrl(BuildExtensions.EXT_OP_EXAMPLE)) {
      boolean isResp = "true".equals(ex.getExtensionString(BuildExtensions.EXT_OP_EXAMPLE_RESPONSE));
      if ("2".equals(ex.getExtensionString(BuildExtensions.EXT_OP_EXAMPLE_LIST))) {
        processExample(examples2, ex.getExtensionString(BuildExtensions.EXT_OP_EXAMPLE_CONTENT), isResp);
      } else {
        processExample(isResp ? respExamples : reqExamples, ex.getExtensionString(BuildExtensions.EXT_OP_EXAMPLE_CONTENT), isResp);
      }
    }
    Operation op = new Operation(src.getCode(), src.getSystem(), src.getType(), src.getInstance(), src.getKind().toCode(), src.getTitle(), src.getDescription(), 
        ExtensionUtilities.readStringExtension(src, BuildExtensions.EXT_FOOTER), reqExamples, respExamples, !src.getAffectsState());
    op.getExamples2().addAll(examples2);
    op.setResource(src);
    op.setStandardsStatus(StandardsStatus.fromCode(ExtensionUtilities.readStringExtension(src, BuildExtensions.EXT_STANDARDS_STATUS)));
    op.setFmm(ExtensionUtilities.readStringExtension(src, BuildExtensions.EXT_FMM_LEVEL));
    op.setFooter2(ExtensionUtilities.readStringExtension(src, BuildExtensions.EXT_FOOTER2));
    for (OperationDefinitionParameterComponent psrc : src.getParameter()) {
      op.getParameters().add(convertOperationParameter(psrc, false));
    }
    return op;
  }

  private void processExample(List<OperationExample> examples, String file, boolean response) throws FileNotFoundException, IOException, Exception {
    for (String s : FileUtilities.fileToString(Utilities.path(folder, file)).split("\r?\n--------------------------------------\r?\n"))
      examples.add(convertToExample(s, response));
  }

  private OperationExample convertToExample(String s, boolean resp) throws Exception {
    String[] lines = s.trim().split("\\r?\\n");
    StringBuilder content = new StringBuilder();
    String comment = null;
    for (String l : lines)
      if (l.startsWith("//"))
        comment = l.substring(2).trim();
      else if (l.startsWith("$bundle ")) {
        content.append(l);
        content.append("\r\n");
      } else if (l.startsWith("$link ")) {
        String url = l.substring(6);
        String title = url.substring(url.indexOf(" ")+1);
        url= url.substring(0, url.indexOf(" "));
        content.append("<a href=\""+url+"\">See "+Utilities.escapeXml(title)+"</a>\r\n");
      } else if (l.startsWith("$includexj ")) {
        int indent = 0;
        String filename = l.substring(11).trim();
        if (filename.contains(" ")) {
          indent = Integer.parseInt(filename.substring(0, filename.indexOf(" ")));
          filename = filename.substring(filename.indexOf(" ")).trim();
        }
        String json = new JsonParser().setOutputStyle(OutputStyle.PRETTY).composeString(new XmlParser().parse(new FileInputStream(PathBuilder.getPathBuilder().withRequiredTarget(srcDir).buildPath(folder, filename))));
        process(content, indent, json);
      } else if (l.startsWith("$include ")) {
        int indent = 0;
        String filename = l.substring(9).trim();
        if (filename.contains(" ")) {
          indent = Integer.parseInt(filename.substring(0, filename.indexOf(" ")));
          filename = filename.substring(filename.indexOf(" ")).trim();
        }
        process(content, indent, FileUtilities.fileToString(PathBuilder.getPathBuilder().withRequiredTarget(srcDir).buildPath(folder, filename)));
      } else {
        content.append(Utilities.escapeXml(l));
        content.append("\r\n");
      }
    return new OperationExample(content.toString(), comment, resp);
  }

  private void process(StringBuilder content, int indent, String s) {
    String pfx = Utilities.padLeft("",  ' ', indent);
    String[] lines = s.trim().split("\\r?\\n");
    for (String l : lines)
      if (l.contains("xmlns:xsi=")) {
        content.append(pfx+Utilities.escapeXml(l.substring(0, l.indexOf("xmlns:xsi=")-1)));
        content.append(">\r\n");
      } else {
        content.append(pfx+Utilities.escapeXml(l));
        content.append("\r\n");
      }
  }
  
  public OperationParameter convertOperationParameter(OperationDefinitionParameterComponent psrc, boolean part) throws IOException {
    List<String> pl = new ArrayList<>(); 
    for (CanonicalType u : psrc.getTargetProfile()) {
      pl.add(u.asStringValue().replace("http://hl7.org/fhir/StructureDefinition/", ""));
    }
    String t;
    if (psrc.hasExtension(BuildExtensions.EXT_ALLOWED_TYPE) ) {
      for (Extension e : psrc.getExtensionsByUrl(BuildExtensions.EXT_ALLOWED_TYPE)) {
        pl.add(e.getValue().primitiveValue());
      }
      t = String.join(" | ", pl);
    } else if (psrc.hasType()) {
      t =  psrc.getType().toCode();
      if (pl.size() > 0) {
        t = t+"("+String.join("|", pl)+")";
      }        
    } else {
      t = "Tuple";
    }
    OperationParameter p = new OperationParameter(psrc.getName(), part ? null : psrc.getUse().toCode(), psrc.getDocumentation(), psrc.getMin(), psrc.getMax(), t, psrc.hasSearchType() ? psrc.getSearchType().toCode() : null, null,
        ExtensionUtilities.getStandardsStatus(psrc));
    if (psrc.hasBinding()) {
      p.setBs(parseBinding(psrc.getBinding()));
    }
    for (Enumeration<OperationParameterScope> s : psrc.getScope()) {
      p.getScopes().add(s.asStringValue());
    }
    for (OperationDefinitionParameterComponent pc : psrc.getPart()) {
      p.getParts().add(convertOperationParameter(pc, true));
    }
    return p;
  }



  private void parseSearchParameters(ResourceDefn r, String n, String t) throws FHIRFormatError, FileNotFoundException, IOException {
    Bundle b = (Bundle) parseXml("bundle-"+t+"-search-params.xml");
    for (BundleEntryComponent be : b.getEntry()) {
      parseSearchParameter(r, (SearchParameter) be.getResource());
    }
  }

  private void parseSearchParameter(ResourceDefn r, SearchParameter src) {
    if (!src.hasName()) {
      src.setName(src.getCode());
    }
    src.setVersion(version);
    SearchParameterDefn sp = new SearchParameterDefn(src.getCode(), src.getDescription(), type(src.getType()), src.getProcessingMode(), 
        StandardsStatus.fromCode(ExtensionUtilities.readStringExtension(src, "http://hl7.org/fhir/StructureDefinition/structuredefinition-standards-status")));
    r.getSearchParams().put(sp.getCode(), sp);
    sp.setExpression(src.getExpression());
    sp.setResource(src);
    sp.setBase(r.getName());
    String s = ExtensionUtilities.readStringExtension(src, BuildExtensions.EXT_PATH);
    if (!Utilities.noString(s)) {
      for (String p : s.split("\\,")) {
        String v = p.trim();
        if (!Utilities.noString(v)) {
          sp.getPaths().add(v);
        }
      }
    }
    
    for (SearchParameterComponentComponent comp : src.getComponent()) {
      sp.getComposites().add(new CompositeDefinition(comp.getDefinition(), comp.getExpression()));
    }
    for (Enumeration<VersionIndependentResourceTypesAll> target : src.getTarget()) {
      sp.getManualTargets().add(target.asStringValue());
    }
  }

  private SearchType type(SearchParamType type) {
    switch (type) {
    case COMPOSITE: return SearchType.composite;
    case DATE: return SearchType.date;
    case NUMBER: return SearchType.number;
    case QUANTITY: return SearchType.quantity;
    case REFERENCE: return SearchType.reference;
    case SPECIAL: return SearchType.special;
    case RESOURCE: return SearchType.resource;
    case STRING: return SearchType.string;
    case TOKEN: return SearchType.token;
    case URI: return SearchType.uri;
    }
    return null;
  }

  private ResourceDefn parseResource(String t) throws FHIRFormatError, FileNotFoundException, IOException {
    StructureDefinition sd = (StructureDefinition) parseXml("structuredefinition-"+t+".xml");
    sdList.put(sd.getUrl(), sd);
    sd.setVersion(version);
    sd.setFhirVersion(FHIRVersion.fromCode(version));

    ResourceDefn r = new ResourceDefn();
    r.setName(sd.getName());
    r.setEnteredInErrorStatus(ExtensionUtilities.readStringExtension(sd, BuildExtensions.EXT_ENTERED_IN_ERROR_STATUS));
    r.setStatus(StandardsStatus.fromCode(ExtensionUtilities.readStringExtension(sd, ExtensionDefinitions.EXT_STANDARDS_STATUS)));
    r.setAbstract(sd.getAbstract());
    r.setInterface(ExtensionUtilities.readBoolExtension(sd, BuildExtensions.EXT_RESOURCE_INTERFACE));
    r.setWg(definitions.getWorkgroups().get(ExtensionUtilities.readStringExtension(sd, ExtensionDefinitions.EXT_WORKGROUP)));
    r.setFmmLevel(ExtensionUtilities.readStringExtension(sd, ExtensionDefinitions.EXT_FMM_LEVEL));
    r.setProposedOrder(ExtensionUtilities.readStringExtension(sd, BuildExtensions.EXT_PROPOSED_ORDER));
    r.setSecurityCategorization(SecurityCategorization.fromCode(ExtensionUtilities.readStringExtension(sd, "http://hl7.org/fhir/StructureDefinition/structuredefinition-security-category")));
    r.setRequirements(sd.getPurpose());
    r.setDefinition(sd.getDescription());
    if (sd.hasExtension(BuildExtensions.EXT_TEMPLATE)) {
      String tname = ExtensionUtilities.readStringExtension(sd, BuildExtensions.EXT_TEMPLATE);  
      ResourceDefn template = definitions.getResourceByName(tname);
      r.setTemplate(template.getRoot());
    }
    // private long timestamp; only set in regenerator...?
    // todo:  
    //   private List<InheritedMapping> inheritedMappings = new ArrayList<InheritedMapping>();
    //   private ElementDefn template;
    //   private Map<String, PointSpec> layout = new HashMap<String, PointSpec>();
    
    ProfileUtilities pu = new ProfileUtilities(context, null, null);
    r.setRoot(parseTypeDefinition(pu, sd.getDifferential().getElementFirstRep(), sd));
    r.getRoot().setFmmLevel(r.getFmmLevel());
    r.getRoot().setRequirements(r.getRequirements());
    if (r.isAbstract()) {
      r.getRoot().setAbstractType(true);
    }
    if (sd.hasBaseDefinition()) {
      r.getRoot().getTypes().add(new TypeRef(sd.getBaseDefinition().replace("http://hl7.org/fhir/StructureDefinition/", "")));
    }
//    r.setProfile(sd);
    
    return r;
  }


  private TypeDefn parseTypeDefinition(ProfileUtilities pu, ElementDefinition focus, StructureDefinition sd) throws IOException {
    for (ElementDefinition edt : sd.getDifferential().getElement()) {
      for (ElementDefinitionConstraintComponent cst : edt.getConstraint()) {
        Invariant inv = new Invariant();
        inv.setContext(focus.getPath());
        inv.setEnglish(cst.getHuman());
        if (cst.hasExtension(BuildExtensions.EXT_OCL)) {
          inv.setOcl(cst.getExtensionString(BuildExtensions.EXT_OCL));        
        }
        inv.setId(cst.getKey());
        if (cst.hasExtension(BuildExtensions.EXT_FIXED_NAME)) {
          inv.setFixedName(cst.getExtensionString(BuildExtensions.EXT_FIXED_NAME));        
        }
        inv.setSeverity(cst.getSeverity().toCode());
        if (cst.hasExtension(BuildExtensions.EXT_BEST_PRACTICE)) {
          inv.setSeverity("best-practice");
        }
        if (cst.hasExtension(BuildExtensions.EXT_TURTLE)) {
          inv.setTurtle(cst.getExtensionString(BuildExtensions.EXT_TURTLE));        
        }
        inv.setRequirements(cst.getRequirements());
        inv.setExpression(cst.getExpression());
        if (cst.hasExtension(BuildExtensions.EXT_BEST_PRACTICE_EXPLANATION)) {
          inv.setExplanation(cst.getExtensionString(BuildExtensions.EXT_BEST_PRACTICE_EXPLANATION));        
        }
        invariants.put(inv.getId(), inv);
      }
    }
    TypeDefn ed = new TypeDefn(null);
    parseED(pu, ed, focus, sd, "");
    return ed;
  }

  private void parseED(ProfileUtilities pu, ElementDefn ed, ElementDefinition focus, StructureDefinition sd, String parentName) throws IOException {
    ed.setMinCardinality(focus.getMin());
    ed.setMaxCardinality("*".equals(focus.getMax()) ? Integer.MAX_VALUE : Integer.parseInt(focus.getMax()));
    ed.setIsModifier(focus.getIsModifier());
    ed.setModifierReason(focus.getIsModifierReason());
    ed.setMustSupport(focus.getMustSupport());
    ed.setSummaryItem(focus.getIsSummary());
    ed.setRegex(ExtensionUtilities.readStringExtension(focus, ExtensionDefinitions.EXT_REGEX));
    ed.setXmlAttribute(focus.hasRepresentation(PropertyRepresentation.XMLATTR));

    if (ExtensionUtilities.hasExtension(focus, BuildExtensions.EXT_UML_DIR)) {
      ed.setUmlDir(ExtensionUtilities.readStringExtension(focus, BuildExtensions.EXT_UML_DIR));
    }
    if (ExtensionUtilities.hasExtension(focus, BuildExtensions.EXT_UML_BREAK)) {
      ed.setUmlBreak(ExtensionUtilities.readBoolExtension(focus, BuildExtensions.EXT_UML_BREAK));
    }
    if (ExtensionUtilities.hasExtension(focus, BuildExtensions.EXT_SVG)) {
      String svg = ExtensionUtilities.readStringExtension(focus, BuildExtensions.EXT_SVG);
      if (svg.contains("w=")) {
        ed.setSvgWidth(Integer.parseInt(svg.substring(svg.indexOf("w=")+2)));
        svg = svg.substring(0, svg.indexOf(";"));
      }
      ed.setSvgLeft(Integer.parseInt(svg.substring(0, svg.indexOf(","))));
      ed.setSvgTop(Integer.parseInt(svg.substring(svg.indexOf(",")+1)));      
    }
    ed.setName(tail(focus.getPath()));
    ed.setShortDefn(focus.getShort());
    ed.setDefinition(focus.getDefinition());
    ed.setRequirements(focus.getRequirements());
    ed.setComments(focus.getComment());
    if (ExtensionUtilities.hasExtension(focus, BuildExtensions.EXT_TODO)) {
      ed.setTodo(ExtensionUtilities.readStringExtension(focus, BuildExtensions.EXT_TODO));
    }
    if (ExtensionUtilities.hasExtension(focus, BuildExtensions.EXT_COMMITTEE_NOTES)) {
      ed.setCommitteeNotes(ExtensionUtilities.readStringExtension(focus, BuildExtensions.EXT_COMMITTEE_NOTES));
    }
    if (ExtensionUtilities.hasExtension(focus, BuildExtensions.EXT_HINT)) {
      ed.setDisplayHint(ExtensionUtilities.readStringExtension(focus, BuildExtensions.EXT_HINT));
    }

    if (focus.hasExtension(BuildExtensions.EXT_NO_BINDING) || focus.hasExtension(ExtensionDefinitions.EXT_NO_BINDING)) {
      ed.setNoBindingAllowed(focus.getExtensionString(BuildExtensions.EXT_NO_BINDING).equals("true"));
    }    
    
    for (StringType t : focus.getAlias()) {
      ed.getAliases().add(t.getValue());
    }
    if (focus.hasMaxLength()) {
      ed.setMaxLength(Integer.toString(focus.getMaxLength()));
    }
    ed.setExample(focus.getExampleFirstRep().getValue());
    ed.setMeaningWhenMissing(focus.getMeaningWhenMissing());
    if (ExtensionUtilities.hasExtension(focus, BuildExtensions.EXT_TRANSLATABLE)) {
      ed.setTranslatable(ExtensionUtilities.readBoolExtension(focus, BuildExtensions.EXT_TRANSLATABLE));
    }
    ed.setOrderMeaning(focus.getOrderMeaning());
    if (ExtensionUtilities.hasExtension(focus, BuildExtensions.EXT_STANDARDS_STATUS)) {
      Extension sse = ExtensionUtilities.getExtension(focus, BuildExtensions.EXT_STANDARDS_STATUS);
      ed.setStandardsStatus(StandardsStatus.fromCode(sse.getValue().primitiveValue()));
      Extension ssr = sse.getValue().getExtensionByUrl(ExtensionDefinitions.EXT_STANDARDS_STATUS_REASON);
      if (ssr != null) {
        ed.setStandardsStatusReason(ssr.getValue().primitiveValue());
      }
    }
    if (ExtensionUtilities.hasExtension(focus, BuildExtensions.EXT_NORMATIVE_VERSION)) {
      ed.setNormativeVersion(ExtensionUtilities.readStringExtension(focus, BuildExtensions.EXT_NORMATIVE_VERSION));
    }

    for (ElementDefinitionConstraintComponent cst : focus.getConstraint()) {
      Invariant inv = invariants.get(cst.getKey());
      ed.getInvariants().put(inv.getId(), inv);
    }

    for (IdType cnd : focus.getCondition()) {
      Invariant inv = invariants.get(cnd.primitiveValue());
      if (inv == null) {
        System.out.println("Unable to find invariant "+cnd.primitiveValue()+" at "+focus.getName());
      } else {
        ed.getStatedInvariants().add(inv);
      }
    }
    
    for (ElementDefinitionMappingComponent map : focus.getMapping()) {
      String uri = getMappingUri(sd, map.getIdentity());
      if ("http://hl7.org/fhir/fivews".equals(uri)) {
        ed.setW5(reverseW5(map.getMap()));
      }
      ed.getMappings().put(uri, map.getMap());
    }
    
    if (focus.hasContentReference()) {
      ed.getTypes().add(new TypeRef("@"+focus.getContentReference().substring(1)));      
    } else {
      for (TypeRefComponent tr : focus.getType()) {
        if (!Utilities.existsInList(tr.getCode(), "Element", "BackboneElement")) {
          TypeRef t = new TypeRef();
          ed.getTypes().add(t);
          if (tr.hasExtension(ExtensionDefinitions.EXT_FHIR_TYPE)) {
            t.setName(tr.getExtensionString(ExtensionDefinitions.EXT_FHIR_TYPE));
          } else { 
            t.setName(tr.getCode());
          }
          if (ExtensionUtilities.hasExtension(tr, BuildExtensions.EXT_HIERARCHY)) {
            ed.setHierarchy(ExtensionUtilities.readBoolExtension(tr, BuildExtensions.EXT_HIERARCHY));
          }

          for (CanonicalType u : tr.getProfile()) {
            t.setProfile(u.getValue().replace("http://hl7.org/fhir/StructureDefinition/", ""));
          }
          for (CanonicalType u : tr.getTargetProfile()) {
            String s = u.getValue().replace("http://hl7.org/fhir/StructureDefinition/", "");
            if ("Resource".equals(s)) {
              t.getParams().add("Any");
            } else {
              t.getParams().add(s);
            }
          }
          if (t.getName().equals("Quantity") && "SimpleQuantity".equals(t.getProfile())) {
            t.setName("SimpleQuantity");
            t.setProfile(null);
          }
          if ("Resource".equals(t.getProfile())) {
            t.setProfile("Any");            
          }
          if (t.getParams().toString().equals("[ActivityDefinition, EventDefinition, EvidenceVariable, Measure, OperationDefinition, PlanDefinition, Questionnaire, SubscriptionTopic]")) {
            t.getParams().clear();
            t.getParams().add("Definition");
          }
        }
      }
      if (ed.getTypes().size() == STAR_TYPES_COUNT) {
        ed.getTypes().clear();
        ed.getTypes().add(new TypeRef("*"));
      }
    }

    String name = parentName + Utilities.capitalize(ed.getName());
    if (focus.hasExtension(ExtensionDefinitions.EXT_EXPLICIT_TYPE)) {
      ed.setStatedType(focus.getExtensionString(ExtensionDefinitions.EXT_EXPLICIT_TYPE));
      ed.setDeclaredTypeName(ed.getStatedType());
    } else if (ed.getTypes().isEmpty() && !focus.hasContentReference()) {      
      ed.setDeclaredTypeName(name+"Component");
    }
    
    if (focus.hasBinding()) {
      ed.setBinding(parseBinding(focus.getBinding()));      
    }
    for (ElementDefinition child : pu.getChildList(sd, focus, true, false)) {
      ElementDefn c = new ElementDefn();
      ed.getElements().add(c);
      parseED(pu, c, child, sd, name);
    }
    
    // todo:
    //   private ElementDefinition derivation;
  }

  private BindingSpecification parseBinding(OperationDefinitionParameterBindingComponent binding) throws IOException {
    BindingSpecification bs = new BindingSpecification("core", binding.getExtensionString(BuildExtensions.EXT_BINDING_NAME), 
        "true".equals(binding.getExtensionString("http://hl7.org/fhir/StructureDefinition/elementdefinition-isCommonBinding")));
    bs.setStrength(binding.getStrength());
    bs.setBindingMethod(BindingMethod.ValueSet);
    bs.setReference(binding.getValueSet());
    if (bs.hasReference()) {
      bs.setValueSet(loadValueSet(bs.getReference(), false, binding.getStrength()));
    }

    if (binding.hasExtension(ExtensionDefinitions.EXT_MAX_VALUESET)) {
      bs.getAdditionalBindings().add(new AdditionalBinding("maximum", binding.getExtensionString(ExtensionDefinitions.EXT_MAX_VALUESET), loadValueSet(binding.getExtensionString(ExtensionDefinitions.EXT_MAX_VALUESET), false, BindingStrength.REQUIRED)));
    }
    
    if (binding.hasExtension(BuildExtensions.EXT_V2_MAP)) {
      bs.setV2Map(binding.getExtensionString(BuildExtensions.EXT_V2_MAP));
    }
    if (binding.hasExtension(BuildExtensions.EXT_V3_MAP)) {
      bs.setV3Map(binding.getExtensionString(BuildExtensions.EXT_V3_MAP));
    }
    if (binding.hasExtension(BuildExtensions.EXT_BINDING_DEFINITION)) {
      bs.setDefinition(binding.getExtensionString(BuildExtensions.EXT_BINDING_DEFINITION));
    }
    if (binding.hasExtension(BuildExtensions.EXT_URI)) {
      bs.setUri(binding.getExtensionString(BuildExtensions.EXT_URI));
    }
    if (binding.hasExtension(BuildExtensions.EXT_WEBSITE)) {
      bs.setWebSite(binding.getExtensionString(BuildExtensions.EXT_WEBSITE));
    }
    if (binding.hasExtension(BuildExtensions.EXT_EMAIL)) {
      bs.setEmail(binding.getExtensionString(BuildExtensions.EXT_EMAIL));
    }
    if (binding.hasExtension(BuildExtensions.EXT_COPYRIGHT)) {
      bs.setCopyright(binding.getExtensionString(BuildExtensions.EXT_COPYRIGHT));
    }      
    if (binding.hasExtension(BuildExtensions.EXT_STATUS)) {
      bs.setStatus(PublicationStatus.fromCode(binding.getExtensionString(BuildExtensions.EXT_STATUS)));
    }
    return bs;
  }
  
  public BindingSpecification parseBinding(ElementDefinitionBindingComponent binding) throws IOException {
    BindingSpecification bs = new BindingSpecification("core", binding.getExtensionString("http://hl7.org/fhir/StructureDefinition/elementdefinition-bindingName"), 
        "true".equals(binding.getExtensionString("http://hl7.org/fhir/StructureDefinition/elementdefinition-isCommonBinding")));
    bs.setStrength(binding.getStrength());
    bs.setBindingMethod(BindingMethod.ValueSet);
    bs.setDescription(binding.getDescription());
    bs.setReference(binding.getValueSet());
    if (bs.hasReference()) {
      bs.setValueSet(loadValueSet(bs.getReference(), false, binding.getStrength()));
    }
    if (binding.hasExtension(ExtensionDefinitions.EXT_MAX_VALUESET)) {
      bs.getAdditionalBindings().add(new AdditionalBinding("maximum", binding.getExtensionString(ExtensionDefinitions.EXT_MAX_VALUESET), loadValueSet(binding.getExtensionString(ExtensionDefinitions.EXT_MAX_VALUESET), false, BindingStrength.REQUIRED)));
    }
    for (ElementDefinitionBindingAdditionalComponent add : binding.getAdditional()) {
      bs.getAdditionalBindings().add(new AdditionalBinding(add.getPurpose().toCode(), add.getValueSet(), loadValueSet(add.getValueSet(), false, add.getPurpose() == AdditionalBindingPurposeVS.REQUIRED ? BindingStrength.REQUIRED : BindingStrength.EXTENSIBLE)).setDoco(add.getDocumentation()));      
    }

    if (binding.hasExtension(BuildExtensions.EXT_V2_MAP)) {
      bs.setV2Map(binding.getExtensionString(BuildExtensions.EXT_V2_MAP));
    }
    if (binding.hasExtension(BuildExtensions.EXT_V3_MAP)) {
      bs.setV3Map(binding.getExtensionString(BuildExtensions.EXT_V3_MAP));
    }
    if (binding.hasExtension(BuildExtensions.EXT_BINDING_DEFINITION)) {
      bs.setDefinition(binding.getExtensionString(BuildExtensions.EXT_BINDING_DEFINITION));
      if (!binding.hasDescription()) {
        bs.setDescription(bs.getDefinition());
      }
    }
    if (binding.hasExtension(BuildExtensions.EXT_URI)) {
      bs.setUri(binding.getExtensionString(BuildExtensions.EXT_URI));
    }
    if (binding.hasExtension(BuildExtensions.EXT_WEBSITE)) {
      bs.setWebSite(binding.getExtensionString(BuildExtensions.EXT_WEBSITE));
    }
    if (binding.hasExtension(BuildExtensions.EXT_EMAIL)) {
      bs.setEmail(binding.getExtensionString(BuildExtensions.EXT_EMAIL));
    }
    if (binding.hasExtension(BuildExtensions.EXT_COPYRIGHT)) {
      bs.setCopyright(binding.getExtensionString(BuildExtensions.EXT_COPYRIGHT));
    }      
    if (binding.hasExtension(BuildExtensions.EXT_STATUS)) {
      bs.setStatus(PublicationStatus.fromCode(binding.getExtensionString(BuildExtensions.EXT_STATUS)));
    }
    
    return bs;
  }

  private ValueSet loadValueSet(String reference, boolean ext, BindingStrength strength) throws IOException {
    if (reference == null) {
      return null;
    }
    if (reference.contains("|")) {
      reference = reference.substring(0, reference.indexOf("|"));
    }

    if (definitions.getValuesets().has(reference)) {
      return definitions.getValuesets().get(reference);
    }
    if (definitions.getExtraValuesets().containsKey(reference)) {
      return definitions.getExtraValuesets().get(reference);
    }
    if (definitions.getBoundValueSets().containsKey(reference)) {
      return definitions.getBoundValueSets().get(reference);
    }
    
    String id = reference.substring(reference.lastIndexOf("/")+1);
    String csfn = Utilities.path(folder, "codesystem-"+id+".xml");
    if (new File(csfn).exists()) {
      CodeSystem cs = (CodeSystem) parseXml("codesystem-"+id+".xml");
      if ((strength == BindingStrength.REQUIRED || strength == BindingStrength.EXTENSIBLE) && cs.getExperimental()) {
        cs.setExperimental(false);
        saveXml(cs, "codesystem-"+id+".xml");
      }
      if (!cs.hasId()) {
        cs.setId(id);
      }
      if (!id.equals(cs.getId())) {
        throw new FHIRException("Error loading "+csfn+": id mismatch. Expected "+id+" but found "+cs.getId());        
      }
      boolean save = CodeSystemUtilities.checkMakeShareable(cs);
      if (!cs.hasUrl()) {
        cs.setUrl("http://hl7.org/fhir/"+cs.getId());
        save = true;
      }
      cs.setUserData("filename", "codesystem-"+cs.getId());
      cs.setWebPath("codesystem-"+cs.getId()+".html");

      if (!cs.hasContent()) {
        System.out.println("The CodeSystem "+csfn+" doesn't have a content element");
      }
      if (!cs.hasCaseSensitive()) {
        System.out.println("The CodeSystem "+csfn+" doesn't have a caseSensitive element");
      }
      if (!cs.hasHierarchyMeaningElement() && CodeSystemUtilities.hasHierarchy(cs)) {
        System.out.println("The CodeSystem "+csfn+" doesn't have a hierarchyMeaning element");
      }
      if (cs.hasStatus()) {
        cs.setStatus(PublicationStatus.ACTIVE);
      }
      if (!cs.hasPublisher()) {
        cs.setPublisher("HL7 International");
      }
      cs.setVersion(version);
      if (!cs.hasExtension(ExtensionDefinitions.EXT_WORKGROUP)) {
        cs.addExtension().setUrl(ExtensionDefinitions.EXT_WORKGROUP).setValue(new CodeType(committee.getCode()));
        save = true;
      } else {
        String ec = ExtensionUtilities.readStringExtension(cs, ExtensionDefinitions.EXT_WORKGROUP);
        if (!ec.equals(committee.getCode()))
          System.out.println("CodeSystem "+cs.getUrl()+" WG mismatch 4a: is "+ec+", want to set to "+committee.getCode());
      } 
      if (ext && !cs.hasCaseSensitive()) {
        cs.setCaseSensitive(true);
        save = true;
      }
      if (!cs.hasStatus()) {
        cs.setStatus(PublicationStatus.DRAFT);
      }
      if (!CodeSystemUtilities.hasOID(cs)) {
        String oid = registry.getOID(cs.getUrl());
        if (oid != null) {
          save = true;
          CodeSystemUtilities.setOID(cs, "urn:oid:"+oid);
        }
      }
      CodeSystemUtilities.makeShareable(cs);
      if (save) {
        saveXml(cs, "codesystem-"+id+".xml");
      }
      definitions.getCodeSystems().see(cs, null);
    }
    
    String cmfn = Utilities.path(folder, "conceptmap-cm-"+id);
    for (File f : new File(folder).listFiles()) {
      if (f.getAbsolutePath().startsWith(cmfn) && f.getName().endsWith(".xml")) {
        String cmid = f.getName().substring(11).replace(".xml", "");
        ConceptMap cm = (ConceptMap) parseXml(f.getName());
        if ((strength == BindingStrength.REQUIRED || strength == BindingStrength.EXTENSIBLE) && cm.getExperimental()) {
          cm.setExperimental(false);
          saveXml(cm, f.getName());
        }
        if (!cm.hasId()) {
          cm.setId(cmid);
        }
        cm.setVersion(version);
        if (!cmid.equals(cm.getId())) {
          throw new FHIRException("Error loading "+f.getAbsolutePath()+": id mismatch. Expected "+cmid+" but found "+cm.getId());        
        }
        if (!cm.hasUrl()) {
          cm.setUrl(reference);
        }
        if (!cm.getUrl().equals("http://hl7.org/fhir/ConceptMap/"+cmid)) {
          throw new FHIRException("Error loading "+f.getAbsolutePath()+": URL mismatch. Expected http://hl7.org/fhir/ConceptMap/"+cmid+" but found "+cm.getUrl());
        }
        cm.setUserData("filename", cmid);
        cm.setWebPath(cmid+".html");
        cm.setUserData("generate", "true");
        ConceptMapUtilities.makeShareable(cm);
        if (!ConceptMapUtilities.hasOID(cm)) {
          String oid = registry.getOID(cm.getUrl());
          if (oid != null) {
            ConceptMapUtilities.setOID(cm, "urn:oid:"+oid);
          }
        }
        if (!cm.hasPublisher()) {
          cm.setPublisher("HL7 International");
        }
        maps.see(cm, null);
      }
    }

    String vsfn = Utilities.path(folder, "valueset-"+id+".xml");
    if (new File(vsfn).exists() ) {
      ValueSet vs = (ValueSet) parseXml("valueset-"+id+".xml");
      if ((strength == BindingStrength.REQUIRED || strength == BindingStrength.EXTENSIBLE) && vs.getExperimental()) {
        vs.setExperimental(false);
        saveXml(vs, "valueset-"+id+".xml");
      }
      if (!vs.hasId()) {
        vs.setId(id);
      }
      vs.setVersion(version);
      if (!id.equals(vs.getId())) {
        throw new FHIRException("Error loading "+vsfn+": id mismatch. Expected "+id+" but found "+vs.getId());        
      }
      if (!vs.hasUrl()) {
        vs.setUrl(reference);
      }
      if (!reference.equals(vs.getUrl())) {
        throw new FHIRException("Error loading "+vsfn+": URL mismatch. Expected "+reference+" but found "+vs.getUrl());
      }
      boolean save = ValueSetUtilities.makeVSShareable(vs);
      vs.setUserData("filename", "valueset-"+vs.getId());
      vs.setWebPath("valueset-"+vs.getId()+".html");
      vs.setExperimental(false);
      if (!vs.hasDescription()) {
        vs.setDescription("Description Needed Here");
        save = true;
      }
      if (!vs.hasStatus()) {
        vs.setStatus(PublicationStatus.ACTIVE);
      }
      if (!vs.hasExtension(ExtensionDefinitions.EXT_WORKGROUP)) {
        vs.addExtension().setUrl(ExtensionDefinitions.EXT_WORKGROUP).setValue(new CodeType(committee.getCode()));
        save = true;
      } else {
        String ec = ExtensionUtilities.readStringExtension(vs, ExtensionDefinitions.EXT_WORKGROUP);
        if (!ec.equals(committee.getCode()))
          System.out.println("ValueSet "+vs.getUrl()+" WG mismatch 4b: is "+ec+", want to set to "+committee.getCode());
      } 
      if (!vs.hasPublisher()) {
        vs.setPublisher("HL7 International");
      }
      ValueSetUtilities.makeShareable(vs);

      vs.setWebPath("valueset-"+vs.getId()+".html");
      if (!ValueSetUtilities.hasOID(vs)) {
        String oid = registry.getOID(vs.getUrl());
        if (oid != null) {
          save = true;
          ValueSetUtilities.setOID(vs, "urn:oid:"+oid);
        }
      }
      if (save) {
        saveXml(vs, "valueset-"+id+".xml");
      }
      definitions.getBoundValueSets().put(vs.getUrl(), vs);
      definitions.getValuesets().see(vs, null);
      
      return vs;
    }
    return null; /// try again later
  }

  private String reverseW5(String mapList) {
    List<String> maps = new ArrayList<String>();
    for (String map: mapList.split(",\\s*")) {
      String mapCode = null;
      for (W5Entry w5 : definitions.getW5list()) {
        if (w5.getFiveWs().equals(map)) {
          mapCode = w5.getCode();
          break;
        }
      }
      maps.add(mapCode==null ? map : mapCode);
    }
    return String.join(",", maps);
  }

  private String getMappingUri(StructureDefinition sd, String identity) {
    for (StructureDefinitionMappingComponent md : sd.getMapping()) {
      if (identity.equals(md.getIdentity())) {
        return md.getUri();
      }
    }
    return null;
  }

  private String tail(String path) {
    return path.contains(".") ? path.substring(path.lastIndexOf(".")+1) : path;
  }

  private Resource parseXml(String name) throws FHIRFormatError, FileNotFoundException, IOException {
    String fn = Utilities.path(folder, name);
    File f = new CSFile(fn);
    long d = f.lastModified();
    if (d == 0) {
      f.setLastModified(new Date().getTime());
    }
    try {
      return new XmlParser().parse(new CSFileInputStream(f));
    } catch (Exception e) {
      throw new IOException("Error parsing "+name+": "+e.getMessage(), e);
    }
  }

  private void saveXml(Resource res, String name) throws FHIRFormatError, FileNotFoundException, IOException {
    new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(Utilities.path(folder, name)), res);
  }

  private void parseExamples(ResourceDefn r, String n, String t) throws FHIRException, Exception {
    ListResource list = (ListResource) parseXml("list-"+t+"-examples.xml");
    for (ListResourceEntryComponent le : list.getEntry()) {
      boolean reg = le.hasExtension(BuildExtensions.EXT_NOT_REGISTERED) ? !ExtensionUtilities.readBoolExtension(le, BuildExtensions.EXT_NOT_REGISTERED) : true;
      ExampleType type = ExampleType.XmlFile;
      if (le.getFlag().hasCoding(BuildExtensions.EXT_EXAMPLE_TYPE, "container")) {
        type = ExampleType.Container;
      } else if (le.getFlag().hasCoding(BuildExtensions.EXT_EXAMPLE_TYPE, "csv")) {
        type = ExampleType.CsvFile;
      } else if (le.getFlag().hasCoding(BuildExtensions.EXT_EXAMPLE_TYPE, "tool")) {
        type = ExampleType.Tool;
      }
      String id = le.getItem().getReference().substring(le.getItem().getReference().lastIndexOf("/")+1);
      String ig = le.getExtensionString(BuildExtensions.EXT_IG);
      CSFile path = Utilities.noString(ig) ?  new CSFile(Utilities.path(folder, le.getExtensionString(BuildExtensions.EXT_TITLE)+".xml")) :  new CSFile(Utilities.path(rootFolder(), "guides", ig, le.getExtensionString(BuildExtensions.EXT_TITLE)+"."+ext(type)));
      Example ex = new Example(context, le.getItem().getDisplay(), id, le.getExtensionString(BuildExtensions.EXT_DESCRIPTION), path, reg, type, false);
      r.getExamples().add(ex);
    }
  }

  private String ext(ExampleType type) {
    switch (type) {
//    case Container:
    case CsvFile: return "csv";
//    case Tool:
    default: return "xml";    
    }
  }

  private String rootFolder() throws IOException {
    return FileUtilities.getDirectoryForFile(srcDir);
  }
}
