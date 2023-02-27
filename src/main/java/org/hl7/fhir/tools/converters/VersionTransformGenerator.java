package org.hl7.fhir.tools.converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.context.ContextUtilities;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.context.SimpleWorkerContext.SimpleWorkerContextBuilder;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.CanonicalResource;
import org.hl7.fhir.r5.model.ElementDefinition;
import org.hl7.fhir.r5.model.ElementDefinition.ElementDefinitionConstraintComponent;
import org.hl7.fhir.r5.model.ElementDefinition.TypeRefComponent;
import org.hl7.fhir.r5.model.EvidenceReport;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.StructureDefinition.StructureDefinitionKind;
import org.hl7.fhir.r5.model.StructureDefinition.TypeDerivationRule;
import org.hl7.fhir.r5.utils.FHIRPathEngine;
import org.hl7.fhir.utilities.TextFile;
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
    FilesystemPackageCacheManager pcm = new FilesystemPackageCacheManager(true);
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
    generate(Utilities.path(dir.getAbsolutePath(), "r5-2-r4", sd.getType()+".fml"), sd, "5to4", "R5", "R4", "", "4.0/");
    generate(Utilities.path(dir.getAbsolutePath(), "r5-2-r4b", sd.getType()+".fml"), sd, "5to4B", "R5", "R4B", "", "4.3/");

    generate(Utilities.path(dir.getAbsolutePath(), "r4-2-r5", sd.getType()+".fml"), sd, "4to5", "R4", "R5", "4.0/", "");
    generate(Utilities.path(dir.getAbsolutePath(), "r4b-2-r5", sd.getType()+".fml"), sd, "4Bto5", "R4B", "R5", "4.3/", "");
}

  private void generatePrimitives(File dir, List<String> primitives) throws IOException {
    generatePrimitives(Utilities.path(dir.getAbsolutePath(), "r5-2-r4", "primitives.fml"), primitives, "5to4", "R5", "R4", "", "4.0/");
    generatePrimitives(Utilities.path(dir.getAbsolutePath(), "r5-2-r4b", "primitives.fml"), primitives, "5to4B", "R5", "R4B", "", "4.3/");

    generatePrimitives(Utilities.path(dir.getAbsolutePath(), "r4-2-r5", "primitives.fml"), primitives, "4to5", "R4", "R5", "4.0/", "");
    generatePrimitives(Utilities.path(dir.getAbsolutePath(), "r4b-2-r5", "primitives.fml"), primitives, "4Bto5", "R4B", "R5", "4.3/", "");
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
    b.append("imports \"http://hl7.org/fhir/StructureMap/Element4to3\"\r\n");
    b.append("\r\n");

    for (String n : primitives) {
      b.append("group "+n+"(source src : "+n+""+sourceV+", target tgt : "+n+""+targetV+") extends Element <<type+>> {\r\n");
      b.append("  src.value as v -> tgt.value = v \""+n+"-value\";\r\n");
      b.append("}\r\n");
    }
    TextFile.stringToFile(b.toString(), path);
  }
  
  private void generate(String path, StructureDefinition sd, String suffix, String sourceV, String targetV, String srcVP, String tgtVP) throws IOException {
    StringBuilder b = new StringBuilder();
    b.append("/// url = 'http://hl7.org/fhir/StructureMap/"+sd.getType()+""+suffix+"'\r\n");
    b.append("/// name = '"+sd.getType()+""+suffix+"'\r\n");
    b.append("/// title = 'FML Conversion for "+sd.getType()+": "+sourceV+" to "+targetV+"'\r\n");
    b.append("\r\n");
    b.append("uses \"http://hl7.org/fhir/"+srcVP+"StructureDefinition/"+sd.getType()+"\" alias "+sd.getType()+""+sourceV+" as source\r\n");
    b.append("uses \"http://hl7.org/fhir/"+tgtVP+"StructureDefinition/"+sd.getType()+"\" alias "+sd.getType()+""+targetV+" as target\r\n");
    b.append("\r\n");
    b.append("imports \"http://hl7.org/fhir/StructureMap/*"+suffix+"\"\r\n");
    b.append("\r\n");
    List<StringBuilder> list = new ArrayList<>();
    generateGroup(list, sd, sd.getSnapshot().getElementFirstRep(), sd.getType(), sd.getType()+""+sourceV, sd.getType()+""+targetV);
    for (StringBuilder gb : list) {
      b.append(gb.toString());
    }
    TextFile.stringToFile(b.toString(), path);
  }

  private void generateGroup(List<StringBuilder> list, StructureDefinition sd, ElementDefinition ed, String name, String st, String tt) {
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
            generateGroup(list, sd, ted, gn, null, null);
          } else if (ted.getType().size() == 1) {
            b.append("  src."+ted.getNameBase()+" -> tgt."+ted.getNameBase()+";\r\n");
          } else {
            b.append("  src."+ted.getNameBase()+" : "+td.getWorkingCode()+" -> tgt."+ted.getNameBase()+";\r\n");
          }
        }
      }
    }
    b.append("}\r\n");
    b.append("\r\n");
  }

  private String tail(String url) {
    return url.contains("/") ? url.substring(url.lastIndexOf("/")+1) : url;
  }

}
