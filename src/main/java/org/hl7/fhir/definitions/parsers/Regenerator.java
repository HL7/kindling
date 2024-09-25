package org.hl7.fhir.definitions.parsers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.definitions.model.BindingSpecification.AdditionalBinding;
import org.hl7.fhir.definitions.model.BindingSpecification.BindingMethod;
import org.hl7.fhir.definitions.model.ConstraintStructure;
import org.hl7.fhir.definitions.model.Definitions;
import org.hl7.fhir.definitions.model.ElementDefn;
import org.hl7.fhir.definitions.model.Example;
import org.hl7.fhir.definitions.model.Invariant;
import org.hl7.fhir.definitions.model.Operation;
import org.hl7.fhir.definitions.model.Operation.OperationExample;
import org.hl7.fhir.definitions.model.Profile;
import org.hl7.fhir.definitions.model.ResourceDefn;
import org.hl7.fhir.definitions.model.SearchParameterDefn;
import org.hl7.fhir.definitions.model.SearchParameterDefn.CompositeDefinition;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.CanonicalResource;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.ElementDefinition;
import org.hl7.fhir.r5.model.ElementDefinition.ElementDefinitionConstraintComponent;
import org.hl7.fhir.r5.model.Enumerations.ListMode;
import org.hl7.fhir.r5.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r5.model.Enumerations.SearchParamType;
import org.hl7.fhir.r5.model.Enumerations.VersionIndependentResourceTypesAll;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.ImplementationGuide;
import org.hl7.fhir.r5.model.ImplementationGuide.ImplementationGuideDefinitionResourceComponent;
import org.hl7.fhir.r5.model.IntegerType;
import org.hl7.fhir.r5.model.ListResource;
import org.hl7.fhir.r5.model.ListResource.ListResourceEntryComponent;
import org.hl7.fhir.r5.model.ListResource.ListStatus;
import org.hl7.fhir.r5.model.MarkdownType;
import org.hl7.fhir.r5.model.OperationDefinition;
import org.hl7.fhir.r5.model.SearchParameter;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.StructureDefinition.StructureDefinitionDifferentialComponent;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r5.utils.BuildExtensions;
import org.hl7.fhir.r5.utils.ToolingExtensions;
import org.hl7.fhir.tools.publisher.KindlingUtilities;
import org.hl7.fhir.utilities.CommaSeparatedStringBuilder;
import org.hl7.fhir.utilities.Utilities;

public class Regenerator {

  private String srcFolder;
  private Definitions definitions;
  private IWorkerContext context;
  
  public Regenerator(String srcFolder, Definitions definitions, IWorkerContext context) {
    super();
    this.srcFolder = srcFolder;
    this.definitions = definitions;
    this.context = context;
  }
  
  public void generate(ResourceDefn r) throws FileNotFoundException, IOException {
  }
  
  public void generateInner(ResourceDefn r) throws FileNotFoundException, IOException {
    String root = Utilities.path(srcFolder, r.getName());
    
    generateSearchParams(root, r);
    generateExamples(root, r);
    generateOperations(r, root);
    generatePacks(r, root);
//    private List<Profile> conformancePackages = new ArrayList<Profile>();
//    private Profile conformancePack;
    
    StructureDefinition sd = r.getProfile().copy();
    sd.setSnapshot(null);
    sd.setText(null);

    if (r.getEnteredInErrorStatus() != null) {
      sd.addExtension(BuildExtensions.EXT_ENTERED_IN_ERROR_STATUS, new CodeType(r.getEnteredInErrorStatus()));
    }
    if (r.getProposedOrder() != null) {
      sd.addExtension(BuildExtensions.EXT_PROPOSED_ORDER, new StringType(r.getProposedOrder()));
    }
    if (r.getTemplate() != null) {
      sd.addExtension(BuildExtensions.EXT_TEMPLATE, new StringType(r.getTemplate().getName()));
    }
    // now, walk through the elements. 
    generateElement(root, r.getName(), sd.getDifferential(), r, r.getRoot());
    
    File fn = new File(Utilities.path(root, sd.fhirType().toLowerCase()+"-"+sd.getId()+".gen.xml"));
    new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(fn), sd);
    fn.setLastModified(r.getTimestamp());    
  }

  public void generatePacks(ResourceDefn r, String root) throws IOException {
    ListResource list = new ListResource();
    list.setId(r.getName()+"-packs");
    list.setStatus(ListStatus.CURRENT);
    list.setMode(ListMode.WORKING);
//    if (r.getConformancePack() != null) {
//      ListResourceEntryComponent li = list.addEntry();
//      li.getItem().setReference("ImplementationGuide/"+r.getName()+"-"+r.getConformancePack().getCategory());
//    }
    for (Profile p : r.getConformancePackages()) {
      ListResourceEntryComponent li = list.addEntry();
      li.getItem().setReference("ImplementationGuide/"+r.getName()+"-"+p.getCategory());
    }
    
    File fn = new File(Utilities.path(root, list.fhirType().toLowerCase()+"-"+list.getId()+".gen.xml"));
    new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(fn), list);
    fn.setLastModified(r.getTimestamp());
    
//    if (r.getConformancePack() != null) {
//      generateCP(root, r, r.getConformancePack());
//    }
    for (Profile p : r.getConformancePackages()) {
      generateCP(root, r, p);
    }
  }

  public void generateOperations(ResourceDefn r, String root) throws IOException {
    ListResource list = new ListResource();
    list.setId(r.getName()+"-operations");
    list.setStatus(ListStatus.CURRENT);
    list.setMode(ListMode.WORKING);
    for (Operation op : r.getOperations()) {
      ListResourceEntryComponent li = list.addEntry();
      li.getItem().setReference("OperationDefinition/"+r.getName()+"-"+op.getName());
    }
    
    File fn = new File(Utilities.path(root, list.fhirType().toLowerCase()+"-"+list.getId()+".gen.xml"));
    new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(fn), list);
    fn.setLastModified(r.getTimestamp());
    
    for (Operation op : r.getOperations()) {
      generateOperation(root, r, op);
    }
  }
  
  private void generateCP(String root, ResourceDefn r, Profile p) throws IOException {
    ImplementationGuide ig = new ImplementationGuide();
    ig.setId(r.getName()+"-"+p.getCategory());
    ig.setTitle(p.getTitle());
    ig.setStatus(PublicationStatus.ACTIVE);
    ig.setDate(new Date(r.getTimestamp()));
    for (String s : p.getMetadata().keySet()) {
      List<String> vl = p.getMetadata().get(s);
      for (String v : vl) {
        if (!Utilities.noString(v)) {
          switch (s) {
//          case "id": ig.setId(v); break;
          case "name": ig.setName(v); break;
          case "author.name": ig.setPublisher(v); break;
          case "code": ig.addExtension(BuildExtensions.EXT_CODE, new CodeType(v)); break;
          case "fmm": ig.addExtension(ToolingExtensions.EXT_FMM_LEVEL, new CodeType(v)); break;
          case "description": ig.setDescription(v); break;
          case "status": ig.setStatus(PublicationStatus.fromCode(v)); break;
          case "publication.status": break;
          case "version": ig.setVersion(v); break;
          case "display": ig.setTitle(v); break;
          case "workgroup": ig.addExtension(ToolingExtensions.EXT_WORKGROUP, new CodeType(v)); break;
          case "date": ig.getDateElement().setValueAsString(v); break;
          case "Experimental": ig.setExperimental(Utilities.existsInList(v.toLowerCase(), "y", "yes", "1", "true")); break;
          default:
            if (s.startsWith("fmm-") || Utilities.existsInList(s, "extension.uri", "author.reference", "published.structure", "notes", "introduction") || s.startsWith("!")) {
              // ignore these
            } else if (s.startsWith("summary-")) {
              String n = s.substring(8);
              for (ConstraintStructure cs : p.getProfiles()) {
                if (n.equals(cs.getTitle()) || n.equals(cs.getId())) {
                  cs.setSummary(v);
                }
              }
            } else {
              System.out.println("Unknown metadata item: "+s+"="+v);
            }
          }
        }
      }
    }
    if (!Utilities.noString(p.getIntroduction())) {
      ig.addExtension(BuildExtensions.EXT_INTRODUCTION, new StringType(p.getIntroduction()));
    }
    if (!Utilities.noString(p.getNotes())) {
      ig.addExtension(BuildExtensions.EXT_NOTES, new StringType(p.getNotes()));
    }

    for (ConstraintStructure cs : p.getProfiles()) {
      CanonicalResource cr = generateProfile(root, r, p, cs);
      ImplementationGuideDefinitionResourceComponent res = ig.getDefinition().addResource();
      res.getReference().setReference(cr.fhirType()+"/"+cr.getId());
    }
 
    for (StructureDefinition cs : p.getExtensions()) {
      CanonicalResource cr = generateExtension(root, r, p, cs.copy());
      ImplementationGuideDefinitionResourceComponent res = ig.getDefinition().addResource();
      res.getReference().setReference(cr.fhirType()+"/"+cr.getId());
    }
 
    for (SearchParameter cs : p.getSearchParameters()) {
      CanonicalResource cr = generateSearchParameter(root, r, p, cs);
      ImplementationGuideDefinitionResourceComponent res = ig.getDefinition().addResource();
      res.getReference().setReference(cr.fhirType()+"/"+cr.getId());
    }
 
    for (Example cs : p.getExamples()) {
      ImplementationGuideDefinitionResourceComponent res = ig.getDefinition().addResource();
      generateExample(root, r, p, cs, res);
    }

    File fn = new File(Utilities.path(root, ig.fhirType().toLowerCase()+"-"+ig.getId()+".gen.xml"));
    new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(fn), ig);
    fn.setLastModified(r.getTimestamp());        
  }

  private void generateExample(String root, ResourceDefn r, Profile p, Example ex, ImplementationGuideDefinitionResourceComponent igr) {
    igr.setName(ex.getName());
    igr.setDescription(ex.getDescription());
    igr.getReference().setReference(ex.fhirType()+"/"+ex.getId());
    switch (ex.getType()) {
    case Container:
      igr.addExtension(BuildExtensions.EXT_TYPE, new CodeableConcept(new Coding(BuildExtensions.EXT_EXAMPLE_TYPE, "container", null)));
      break;
    case CsvFile:
      igr.addExtension(BuildExtensions.EXT_TYPE, new CodeableConcept(new Coding(BuildExtensions.EXT_EXAMPLE_TYPE, "csv", null)));
      break;
    case Tool:
      igr.addExtension(BuildExtensions.EXT_TYPE, new CodeableConcept(new Coding(BuildExtensions.EXT_EXAMPLE_TYPE, "tool", null)));
      break;
    default:
      break;
    }    
  }
  
  private CanonicalResource generateSearchParameter(String root, ResourceDefn r, Profile p, SearchParameter sp) throws IOException {
    File fn = new File(Utilities.path(root, sp.fhirType().toLowerCase()+"-profile-"+sp.getId()+".gen.xml"));
    new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(fn), sp);
    fn.setLastModified(r.getTimestamp());
    return sp;
  }

  private CanonicalResource generateExtension(String root, ResourceDefn r, Profile p, StructureDefinition sd) throws IOException {
    File fn = new File(Utilities.path(root,  sd.fhirType().toLowerCase()+"-extension-"+sd.getId()+".gen.xml"));
    sd.setSnapshot(null);
    new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(fn), sd);
    fn.setLastModified(r.getTimestamp());    
    return sd;
  }

  private CanonicalResource generateProfile(String root, ResourceDefn r, Profile p, ConstraintStructure cs) throws IOException {
    StructureDefinition sd = cs.getResource().copy();
    sd.setSnapshot(null);
    if (!Utilities.noString(cs.getSummary())) {
      sd.addExtension(ToolingExtensions.EXT_SUMMARY, new MarkdownType(cs.getSummary()));
    }
    File fn = new File(Utilities.path(root,  sd.fhirType().toLowerCase()+"-profile-"+sd.getId()+".gen.xml"));
    new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(fn), sd);
    fn.setLastModified(r.getTimestamp());
    return sd;
  }

  private void generateElement(String root, String path, StructureDefinitionDifferentialComponent differential, ResourceDefn r, ElementDefn ed) throws FileNotFoundException, IOException {
    ElementDefinition defn = getByPath(differential, path);
    if (defn == null) {
      throw new Error(path+" has no definition element");
    }
    
    if (ed.getDisplayHint() != null) {      
      defn.addExtension(BuildExtensions.EXT_HINT, new StringType(ed.getDisplayHint()));
    }
    if (!Utilities.noString(ed.getUmlDir())) {
      defn.addExtension(BuildExtensions.EXT_UML_DIR, new CodeType(ed.getUmlDir()));      
    }
    if (ed.isUmlBreak()) {
      defn.addExtension(BuildExtensions.EXT_UML_BREAK, new BooleanType(ed.isUmlBreak()));      
    }
    if (ed.getSvgLeft() != ElementDefn.MAX_NEG || ed.getSvgTop() != ElementDefn.MAX_NEG) {
      if (ed.getSvgWidth() != ElementDefn.MAX_NEG) {
        defn.addExtension(BuildExtensions.EXT_SVG, new CodeType(""+ed.getSvgLeft()+","+ed.getSvgTop()+"; w="+ed.getSvgWidth()));
      } else {
        defn.addExtension(BuildExtensions.EXT_SVG, new CodeType(""+ed.getSvgLeft()+","+ed.getSvgTop()));
      }
    }

    if (ed.getTodo() != null) {
      defn.addExtension(BuildExtensions.EXT_TODO, new StringType(ed.getTodo()));
    }
    if (ed.getCommitteeNotes() != null) {
      defn.addExtension(BuildExtensions.EXT_COMMITTEE_NOTES, new StringType(ed.getCommitteeNotes()));
    }    
      
    for (Invariant inv : ed.getStatedInvariants()) {
      checkInvariant(defn, inv);
    }

    if (ed.hasBinding()) {
      checkBinding(root, path, r, ed, defn);
    }
    // todo: chase vocab: private BindingSpecification binding;
    
    for (ElementDefn c : ed.getElements()) {
      generateElement(root, path+"."+c.getName(), differential, r, c);
    }
  }

  private void checkBinding(String root, String path, ResourceDefn r, ElementDefn ed, ElementDefinition defn) throws FileNotFoundException, IOException {
    if (!defn.hasBinding()) {
      throw new Error(path+": binding but no binding");
    }
    
    if (!Utilities.noString(ed.getBinding().getName())) {
      defn.getBinding().addExtension(BuildExtensions.EXT_NAME, new StringType(ed.getBinding().getName()));
    }
    if (!Utilities.noString(ed.getBinding().getV2Map())) {
      defn.getBinding().addExtension(BuildExtensions.EXT_V2_MAP, new StringType(ed.getBinding().getV2Map()));
    }
    if (!Utilities.noString(ed.getBinding().getV3Map())) {
      defn.getBinding().addExtension(BuildExtensions.EXT_V3_MAP, new StringType(ed.getBinding().getV3Map()));
    }

    if (!Utilities.noString(ed.getBinding().getUri())) {
      defn.getBinding().addExtension(BuildExtensions.EXT_URI, new StringType(ed.getBinding().getUri()));
    }
    if (!Utilities.noString(ed.getBinding().getWebSite())) {
      defn.getBinding().addExtension(BuildExtensions.EXT_WEBSITE, new StringType(ed.getBinding().getWebSite()));
    }
    if (!Utilities.noString(ed.getBinding().getEmail())) {
      defn.getBinding().addExtension(BuildExtensions.EXT_EMAIL, new StringType(ed.getBinding().getEmail()));
    }
    if (!Utilities.noString(ed.getBinding().getCopyright())) {
      defn.getBinding().addExtension(BuildExtensions.EXT_COPYRIGHT, new StringType(ed.getBinding().getCopyright()));
    }
    
    if (ed.getBinding().getStatus() != null) {
      defn.getBinding().addExtension(BuildExtensions.EXT_STATUS, new StringType(ed.getBinding().getStatus().toCode()));
    }

    if (ed.getBinding().getBinding() == BindingMethod.CodeList) {
      ValueSet vs = ed.getBinding().getValueSet();
      File fn = new File(Utilities.path(root, vs.fhirType().toLowerCase()+"-"+vs.getId()+".gen.xml"));
      new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(fn), vs);
      fn.setLastModified(r.getTimestamp());    
      
      List<CodeSystem> csl = fetchCodeSystemsForValueSet(vs);
      for (CodeSystem cs : csl) {
        fn = new File(Utilities.path(root, cs.fhirType().toLowerCase()+"-"+cs.getId()+".gen.xml"));
        new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(fn), cs);
        fn.setLastModified(r.getTimestamp());            
      }

      for (AdditionalBinding vsc : ed.getBinding().getAdditionalBindings()) {
        if (vsc.getValueSet() != null) {
          vs = vsc.getValueSet();
          fn = new File(Utilities.path(root, vs.fhirType().toLowerCase()+"-"+vs.getId()+".gen.xml"));
          new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(fn), vs);
          fn.setLastModified(r.getTimestamp());        
          csl = fetchCodeSystemsForValueSet(vs);
          for (CodeSystem cs : csl) {
            fn = new File(Utilities.path(root, cs.fhirType().toLowerCase()+"-"+cs.getId()+".gen.xml"));
            new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(fn), cs);
            fn.setLastModified(r.getTimestamp());            
          }
        }
      }
    }
    
  }

  private List<CodeSystem> fetchCodeSystemsForValueSet(ValueSet vs) {
    List<CodeSystem> result = new ArrayList<>();
    for (ConceptSetComponent inc : vs.getCompose().getInclude()) {
      CodeSystem cs = context.fetchCodeSystem(inc.getSystem());
      if (cs != null && !result.contains(cs)) {
        result.add(cs);
      }
    }
    return result;    
  }

  private ElementDefinition getByPath(StructureDefinitionDifferentialComponent differential, String path) {
    for (ElementDefinition d : differential.getElement()) {
      if (d.getPath().equals(path)) {
        return d;
      }
    }
    return null;
  }

  private void checkInvariant(ElementDefinition ed, Invariant inv) {
    ElementDefinitionConstraintComponent constrant = getConstraint(ed, inv); 
    if (constrant != null) {
      if (!Utilities.noString(inv.getOcl())) {
        constrant.addExtension(BuildExtensions.EXT_OCL, new StringType(inv.getOcl()));
      }
      if (!Utilities.noString(inv.getFixedName())) {
        constrant.addExtension(BuildExtensions.EXT_FIXED_NAME, new StringType(inv.getFixedName()));
      }
      if (!Utilities.noString(inv.getTurtle())) {
        constrant.addExtension(BuildExtensions.EXT_TURTLE, new StringType(inv.getTurtle()));
      }
      if (!Utilities.noString(inv.getExplanation())) {
        constrant.addExtension(BuildExtensions.EXT_BEST_PRACTICE_EXPLANATION, new StringType(inv.getExplanation()));
      }
    }
  }

  
  
  private ElementDefinitionConstraintComponent getConstraint(ElementDefinition ed, Invariant inv) {
    for (ElementDefinitionConstraintComponent c : ed.getConstraint()) {
      if (inv.getId().equals(inv.getId())) {
        return c;
      }
    }
    return null;
  }

  private void generateOperation(String root, ResourceDefn r, Operation op) throws IOException {
    OperationDefinition opd = op.getResource().copy();
    opd.setId(r.getName()+"-"+op.getName());
    opd.setUrl("http://hl7.org/fhir/build/OperationDefinition/"+opd.getId());
    if (!Utilities.noString(op.getFooter())) {
      opd.addExtension(BuildExtensions.EXT_FOOTER, new MarkdownType(op.getFooter()));
    }
    if (!Utilities.noString(op.getFooter2())) {
      opd.addExtension(BuildExtensions.EXT_FOOTER2, new MarkdownType(op.getFooter2()));
    }
    
    for (OperationExample opex : op.getAllExamples1()) {
      Extension ex = new Extension(BuildExtensions.EXT_OP_EXAMPLE);
      if (!Utilities.noString(opex.getContent())) {
        ex.addExtension(BuildExtensions.EXT_OP_EXAMPLE_CONTENT, new StringType(opex.getContent()));
      }
      if (!Utilities.noString(opex.getComment())) {
        ex.addExtension(BuildExtensions.EXT_OP_EXAMPLE_COMMENT, new StringType(opex.getComment()));
      }
      ex.addExtension(BuildExtensions.EXT_OP_EXAMPLE_RESPONSE, new BooleanType(opex.isResponse()));
      ex.addExtension(BuildExtensions.EXT_OP_EXAMPLE_LIST, new IntegerType(1));
      opd.addExtension(ex);
    }
    for (OperationExample opex : op.getExamples2()) {
      Extension ex = new Extension(BuildExtensions.EXT_OP_EXAMPLE);
      if (!Utilities.noString(opex.getContent())) {
        ex.addExtension(BuildExtensions.EXT_OP_EXAMPLE_CONTENT, new StringType(opex.getContent()));
      }
      if (!Utilities.noString(opex.getComment())) {
        ex.addExtension(BuildExtensions.EXT_OP_EXAMPLE_COMMENT, new StringType(opex.getComment()));
      }
      ex.addExtension(BuildExtensions.EXT_OP_EXAMPLE_RESPONSE, new BooleanType(opex.isResponse()));
      ex.addExtension(BuildExtensions.EXT_OP_EXAMPLE_LIST, new IntegerType(2));
      opd.addExtension(ex);
    }

    File fn = new File(Utilities.path(root, opd.fhirType().toLowerCase()+"-"+opd.getId()+".gen.xml"));
    new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(fn), opd);
    fn.setLastModified(r.getTimestamp());    
  }

  private void generateSearchParams(String root, ResourceDefn r) throws FileNotFoundException, IOException {
    Bundle bnd = new Bundle();
    bnd.setId(r.getName()+"-search-params");
    for (String s : sorted(r.getSearchParams().keySet())) {
      SearchParameterDefn spd = r.getSearchParams().get(s);
      SearchParameter sp = new SearchParameter();
      sp.setId(r.getName()+"-"+spd.getCode());
      bnd.addEntry().setResource(sp);
      KindlingUtilities.makeUniversal(sp);
      
      sp.setUrl("http://hl7.org/fhir/build/SearchParameter/"+sp.getId());
      sp.setCode(spd.getCode());
      sp.setDescription(spd.getDescription());
//      sp.setXpath(spd.getXPath());
      switch (spd.getType()) {
      case composite: sp.setType(SearchParamType.COMPOSITE); break;
      case date: sp.setType(SearchParamType.DATE); break;
      case number: sp.setType(SearchParamType.NUMBER); break;
      case quantity: sp.setType(SearchParamType.QUANTITY); break;
      case reference: sp.setType(SearchParamType.REFERENCE); break;
      case special: sp.setType(SearchParamType.SPECIAL); break;
      case string: sp.setType(SearchParamType.STRING); break;
      case token: sp.setType(SearchParamType.TOKEN); break;
      case uri: sp.setType(SearchParamType.URI); break;
      }
      sp.setProcessingMode(spd.getProcessingMode());
      CommaSeparatedStringBuilder b = new CommaSeparatedStringBuilder(",");
      for (String p : spd.getPaths()) {
        b.append(p);
      }
      if (!Utilities.noString(b.toString())) {
        sp.addExtension(BuildExtensions.EXT_PATH, new StringType(b.toString()));
      }
      sp.setExpression(spd.getExpression());
      if (spd.hasManualTypes()) {
        for (String t : sorted(spd.getManualTypes())) {
          sp.addTarget(VersionIndependentResourceTypesAll.fromCode(t));
        }
      }
      if (spd.getStandardsStatus() != null) {
        sp.addExtension(ToolingExtensions.EXT_STANDARDS_STATUS, new CodeType(spd.getStandardsStatus().toCode()));
      }
      for (CompositeDefinition c : spd.getComposites()) {
        sp.addComponent().setExpression(c.getExpression()).setDefinition(c.getDefinition());
      }
    }
    
    File fn = new File(Utilities.path(root, bnd.fhirType().toLowerCase()+"-"+bnd.getId()+".gen.xml"));
    new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(fn), bnd);
    fn.setLastModified(r.getTimestamp());    
  }
    
  private List<String> sorted(Set<String> keySet) {
    List<String> list = new ArrayList<String>();
    list.addAll(keySet);
    Collections.sort(list);
    return list;
  }

  private void generateExamples(String root, ResourceDefn r) throws FileNotFoundException, IOException {
    ListResource list = new ListResource();
    list.setId(r.getName()+"-examples");
    list.setStatus(ListStatus.CURRENT);
    list.setMode(ListMode.WORKING);
    for (Example ex : r.getExamples()) {
      ListResourceEntryComponent li = list.addEntry();
      li.getItem().setReference(ex.getResourceName()+"/"+ex.getId());
      li.getItem().setDisplay(ex.getName());
      if (ex.getDescription() != null) {
        li.addExtension(BuildExtensions.EXT_DESCRIPTION, new StringType(ex.getDescription()));
      }
      if (ex.getTitle() != null) {
        li.addExtension(BuildExtensions.EXT_TITLE, new StringType(ex.getTitle()));
      }
      switch (ex.getType()) {
      case Container:
        li.getFlag().addCoding(BuildExtensions.EXT_EXAMPLE_TYPE, "container", null);
        break;
      case CsvFile:
        li.getFlag().addCoding(BuildExtensions.EXT_EXAMPLE_TYPE, "csv", null);
        break;
      case Tool:
        li.getFlag().addCoding(BuildExtensions.EXT_EXAMPLE_TYPE, "tool", null);
        break;
      default:
        break;
      }
      if (!ex.isRegistered()) {
        li.addExtension(BuildExtensions.EXT_NOT_REGISTERED, new BooleanType(!ex.isRegistered()));
      }
      if (ex.getIg() != null) {
        li.addExtension(BuildExtensions.EXT_IG, new CodeType(ex.getIg()));
      }
      if (ex.getExampleFor() != null) {
        li.addExtension(BuildExtensions.EXT_EXAMPLE_FOR, new StringType(ex.getExampleFor()));
      }
    }
    
    File fn = new File(Utilities.path(root, list.fhirType().toLowerCase()+"-"+list.getId()+".gen.xml"));
    new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(fn), list);
    fn.setLastModified(r.getTimestamp());
  }

  public void generate() throws FileNotFoundException, IOException {
    for (ResourceDefn r : definitions.getBaseResources().values()) {
      generate(r);
    }
    for (ResourceDefn r : definitions.getResources().values()) {
      generate(r);
    }
  }
  
}
