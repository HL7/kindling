package org.hl7.fhir.tools.converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.CanonicalResource;
import org.hl7.fhir.r5.model.ElementDefinition;
import org.hl7.fhir.r5.model.ElementDefinition.TypeRefComponent;
import org.hl7.fhir.r5.model.EvidenceReport;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.utilities.Utilities;

public class StructureDefinitionScanner {

  public static void main(String[] args) {
    new StructureDefinitionScanner().run(new File(args[0]));
  }
  
  private void run(File file) {
    fix(file);
    report(sdExt, "Extensions on StructureDefinition");
    report(sdExtM, "Modifier Extensions on StructureDefinition");
    report(edExt, "Extensions on ElementDefinition");
    report(edExtM, "Modifier Extensions on ElementDefinition");
    report(tdExt, "Extensions on Type Definition");
    report(sdExt, "Extensions on Binding Definition");
  }

  private void report(Set<String> set, String title) {
    System.out.println(title);
    if (set.size() == 0) {
      System.out.println("  (none)");      
    } else {
      for (String s : set) {
        System.out.println("* "+s);            
      }
    }    
    System.out.println("");      
  }

  private Set<String> sdExt = new HashSet<>();
  private Set<String> edExt = new HashSet<>();
  private Set<String> sdExtM = new HashSet<>();
  private Set<String> edExtM = new HashSet<>();
  private Set<String> tdExt = new HashSet<>();
  private Set<String> bdExt = new HashSet<>();

  private void fix(File file) {
    
    for (File f : file.listFiles()) {
      if (f.isDirectory()) {
        fix(f);
      } else if (f.getName().endsWith(".xml")) {
        try {
          Resource res = new XmlParser().parse(new FileInputStream(f));
          if (fixResource(res, f.getAbsolutePath())) {
            new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(f), res); 
          }
        } catch (Exception e) {
          // nothing
        }
      } else if (f.getName().endsWith(".json")) {
        try {
          Resource res = new JsonParser().parse(new FileInputStream(f));
          if (fixResource(res, f.getAbsolutePath())) {
            new JsonParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(f), res); 
          }
        } catch (Exception e) {
          // nothing
        }
      }
    }    
  }

  private boolean fixResource(Resource res, String name) {
    if (res instanceof StructureDefinition) {
      StructureDefinition sd = (StructureDefinition) res;
      for (Extension ext : sd.getExtension()) {
        sdExt.add(ext.getUrl());
      }
      for (Extension ext : sd.getModifierExtension()) {
        sdExtM.add(ext.getUrl());
      }
      for (ElementDefinition ed : sd.getSnapshot().getElement()) {
        for (Extension ext : ed.getExtension()) {
          edExt.add(ext.getUrl());
        }
        for (Extension ext : ed.getModifierExtension()) {
          edExtM.add(ext.getUrl());
        }
        for (Extension ext : ed.getBinding().getExtension()) {
          bdExt.add(ext.getUrl());
        }
        for (TypeRefComponent t : ed.getType()) {
          for (Extension ext : t.getExtension()) {
            tdExt.add(ext.getUrl());
          }
        }
      }
    } 
    return false;
  }

  private boolean checkCRName(CanonicalResource cr, String bit) {
    if (cr.getName().contains(bit)) {
      cr.setName(cr.getName().replace(bit, ""));
      return true;
    } else {
      return false;
    }
  }

}
