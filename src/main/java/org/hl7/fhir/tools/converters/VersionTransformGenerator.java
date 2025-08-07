package org.hl7.fhir.tools.converters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.context.ContextUtilities;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.context.SimpleWorkerContext.SimpleWorkerContextBuilder;
import org.hl7.fhir.r5.fhirpath.FHIRPathEngine;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r5.model.ElementDefinition;
import org.hl7.fhir.r5.model.ElementDefinition.TypeRefComponent;
import org.hl7.fhir.r5.model.Enumerations.BindingStrength;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.StructureDefinition.StructureDefinitionKind;
import org.hl7.fhir.r5.model.StructureDefinition.TypeDerivationRule;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r5.extensions.ExtensionDefinitions;
import org.hl7.fhir.utilities.FileUtilities;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;

// iterate the structure definitions
// collect the primitives
// generate the map file for other specializations

public class VersionTransformGenerator {

  public static void main(String[] args) throws FHIRException, IOException {
    new VersionTransformGenerator().run(new File(args[0]));
  }

  int count = 0;
  
  private void run(File file) throws FHIRException, IOException {
    System.out.println("Loading");
    FilesystemPackageCacheManager pcm = new FilesystemPackageCacheManager.Builder().build();
    NpmPackage npm = pcm.loadPackage("hl7.fhir.r5.core#current");
    context = new SimpleWorkerContextBuilder().fromPackage(npm);
    cu = new ContextUtilities(context);
    fpe = new FHIRPathEngine(context);
    System.out.println("Loaded");
    process(file);
    System.out.println(count);
  }


  private IWorkerContext context;
  private FHIRPathEngine fpe;
  private ContextUtilities cu;

  private void process(File dir) throws IOException {
    List<String> primitives = new ArrayList<>();
    
    for (StructureDefinition sd : cu.allStructures()) {
      if (sd.getDerivation() == TypeDerivationRule.SPECIALIZATION) {
        System.out.println(sd.getType());
        if (sd.getKind() == StructureDefinitionKind.PRIMITIVETYPE) {
          primitives.add(sd.getType());
        } else {
          generate(dir, sd);
        }
      }
    }
    Collections.sort(primitives);
    generatePrimitives(dir, primitives);
    System.out.println("Done");
  }

  private void generate(File dir, StructureDefinition sd) throws IOException {
    generate(Utilities.path(dir.getAbsolutePath(), "r5-2-r4", sd.getType()+"5to4.fml"), sd, "5to4", "R5", "R4", "", "4.0/");
    generate(Utilities.path(dir.getAbsolutePath(), "r5-2-r4b", sd.getType()+"5to4B.fml"), sd, "5to4B", "R5", "R4B", "", "4.3/");

    generate(Utilities.path(dir.getAbsolutePath(), "r4-2-r5", sd.getType()+"4to5.fml"), sd, "4to5", "R4", "R5", "4.0/", "");
    generate(Utilities.path(dir.getAbsolutePath(), "r4b-2-r5", sd.getType()+"4Bto5.fml"), sd, "4Bto5", "R4B", "R5", "4.3/", "");
}

  private void generatePrimitives(File dir, List<String> primitives) throws IOException {
    generatePrimitives(Utilities.path(dir.getAbsolutePath(), "r5-2-r4", "primitives5to4.fml"), primitives, "5to4", "R5", "R4", "", "4.0/");
    generatePrimitives(Utilities.path(dir.getAbsolutePath(), "r5-2-r4b", "primitives5to4B.fml"), primitives, "5to4B", "R5", "R4B", "", "4.3/");

    generatePrimitives(Utilities.path(dir.getAbsolutePath(), "r4-2-r5", "primitives4to5.fml"), primitives, "4to5", "R4", "R5", "4.0/", "");
    generatePrimitives(Utilities.path(dir.getAbsolutePath(), "r4b-2-r5", "primitives4Bto5.fml"), primitives, "4Bto5", "R4B", "R5", "4.3/", "");
}

  private void generatePrimitives(String path, List<String> primitives, String suffix, String sourceV, String targetV, String srcVP, String tgtVP) throws IOException {
    StringBuilder b = new StringBuilder();
    b.append("/// url = 'http://hl7.org/fhir/StructureMap/primitives"+suffix+"'\r\n");
    b.append("/// name = 'Primitives"+suffix+"'\r\n");
    b.append("/// title = 'FML Conversion for primtives: "+sourceV+" to "+targetV+"'\r\n");
    b.append("\r\n");
    for (String n : primitives) {
      b.append("uses \"http://hl7.org/fhir/"+srcVP+"StructureDefinition/"+n+"\" alias "+n+sourceV+" as source\r\n");
      b.append("uses \"http://hl7.org/fhir/"+tgtVP+"StructureDefinition/"+n+"\" alias "+n+targetV+" as target\r\n");      
    }
    b.append("\r\n");
    b.append("imports \"http://hl7.org/fhir/StructureMap/Element"+suffix+"\"\r\n");
    b.append("\r\n");

    for (String n : primitives) {
      b.append("group "+n+"(source src : "+n+""+sourceV+", target tgt : "+n+""+targetV+") extends Element <<type+>> {\r\n");
      b.append("  src.value as v -> tgt.value = v \""+n+"Value\";\r\n");
      b.append("}\r\n");
    }
    FileUtilities.stringToFile(b.toString(), path);
  }
  
  private void generate(String path, StructureDefinition sd, String suffix, String sourceV, String targetV, String srcVP, String tgtVP) throws IOException {
    StringBuilder b = new StringBuilder();
    b.append("/// url = 'http://hl7.org/fhir/StructureMap/"+sd.getType()+""+suffix+"'\r\n");
    b.append("/// name = '"+sd.getType()+""+suffix+"'\r\n");
    b.append("/// title = 'FML Conversion for "+sd.getType()+": "+sourceV+" to "+targetV+"'\r\n");
    b.append("\r\n");
    List<StringBuilder> list = new ArrayList<>();
    Map<String, StringBuilder> conceptMaps = new HashMap<>(); 
    generateGroup(list, conceptMaps, sd, sd.getSnapshot().getElementFirstRep(), sd.getType(), sd.getType()+""+sourceV, sd.getType()+""+targetV, srcVP, tgtVP);
    for (String cmName : Utilities.sorted(conceptMaps.keySet())) {
      b.append(conceptMaps.get(cmName).toString());
    }
    b.append("uses \"http://hl7.org/fhir/"+srcVP+"StructureDefinition/"+sd.getType()+"\" alias "+sd.getType()+""+sourceV+" as source\r\n");
    b.append("uses \"http://hl7.org/fhir/"+tgtVP+"StructureDefinition/"+sd.getType()+"\" alias "+sd.getType()+""+targetV+" as target\r\n");
    b.append("\r\n");
    b.append("imports \"http://hl7.org/fhir/StructureMap/*"+suffix+"\"\r\n");
    b.append("\r\n");
    for (StringBuilder gb : list) {
      b.append(gb.toString());
    }
    FileUtilities.stringToFile(b.toString(), path);
  }

  private void generateGroup(List<StringBuilder> list, Map<String, StringBuilder> conceptMaps, StructureDefinition sd, ElementDefinition ed, String name, String st, String tt, String sv, String tv) {
    StringBuilder b = new StringBuilder();
    list.add(b);
    if (st != null) {
      b.append("group "+name+"(source src : "+st+", target tgt : "+tt+") extends "+tail(sd.getBaseDefinition())+" <<type+>> {\r\n");
    } else {
      b.append("group "+name+"(source src, target tgt) extends "+ed.typeSummary()+" {\r\n");
    }
    List<ElementDefinition> children = fpe.getProfileUtilities().getChildList(sd, ed);
    for (ElementDefinition ted : children) {
      if (ted.getBase().getPath().startsWith(sd.getType()+".")) {
        for (TypeRefComponent td : ted.getType()) {
          if (Utilities.existsInList(td.getWorkingCode(), "Element", "BackboneElement")) {
            String gn = name+Utilities.capitalize(ted.getName());
            b.append("  src."+ted.getNameBase()+" as s -> tgt."+ted.getNameBase()+" as t then "+gn+"(s,t);\r\n");
            generateGroup(list, conceptMaps, sd, ted, gn, null, null, sv, tv);
          } else if (ted.getType().size() > 1) {
            b.append("  src."+ted.getNameBase()+" : "+td.getWorkingCode()+" -> tgt."+ted.getNameBase()+";\r\n");
          } else {
            if (ted.getBinding().getStrength() == BindingStrength.REQUIRED) {            
              ValueSet vs = context.fetchResource(ValueSet.class, ted.getBinding().getValueSet());
              CodeSystem cs = vs == null ? null : getCodeSystemForValueSet(vs);
              if (vs != null && cs != null) {
                String cmName = ted.getBinding().hasExtension(ExtensionDefinitions.EXT_BINDING_NAME) ? ted.getBinding().getExtensionString(ExtensionDefinitions.EXT_BINDING_NAME) : vs.getId();
                genConceptMap(conceptMaps, cmName, vs, cs, sv, tv);
                b.append("  src."+ted.getNameBase()+" as v -> tgt."+ted.getNameBase()+" = translate(v, '#"+cmName+"', 'code');\r\n");
                break;
              }
            }
            b.append("  src."+ted.getNameBase()+" -> tgt."+ted.getNameBase()+";\r\n");
          }
        }
      }
    }
    b.append("}\r\n");
    b.append("\r\n");
  }

  private CodeSystem getCodeSystemForValueSet(ValueSet vs) {
    if (vs.getCompose().getExclude().isEmpty() && vs.getCompose().getInclude().size() == 1) {
      ConceptSetComponent inc = vs.getCompose().getIncludeFirstRep();
      if (inc.getConcept().isEmpty() && inc.getFilter().isEmpty() && inc.getSystem().startsWith("http://hl7.org/fhir/")) {
        return context.fetchResource(CodeSystem.class, inc.getSystem());
      }
    }
    return null;
  }

  private void genConceptMap(Map<String, StringBuilder> conceptMaps, String cmName, ValueSet vs, CodeSystem cs, String sv, String tv) {
    if (!conceptMaps.containsKey(cmName)) {
      StringBuilder b = new StringBuilder();
      conceptMaps.put(cmName, b);

      b.append("conceptmap \""+cmName+"\" {\r\n");
      b.append("  prefix s = \""+cs.getUrl().replace("http://hl7.org/fhir/", "http://hl7.org/fhir/"+sv)+"\"\r\n");
      b.append("  prefix t = \""+cs.getUrl().replace("http://hl7.org/fhir/", "http://hl7.org/fhir/"+tv)+"\"\r\n\r\n");

      genConcepts(cs.getConcept(), b);
      b.append("}\r\n\r\n");
    }
  }

  private void genConcepts(List<ConceptDefinitionComponent> list, StringBuilder b) {
    for (ConceptDefinitionComponent cd : list) {
      b.append("  s:\""+cd.getCode()+"\" - t:\""+cd.getCode()+"\"\r\n");
      genConcepts(cd.getConcept(), b);
    }
  }

  private String tail(String url) {
    return url.contains("/") ? url.substring(url.lastIndexOf("/")+1) : url;
  }

}
