package org.hl7.fhir.tools.converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.CanonicalResource;
import org.hl7.fhir.r5.model.EvidenceReport;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.utilities.Utilities;

public class TerminologyResourceFixer {

  public static void main(String[] args) {
    new TerminologyResourceFixer().fix(new File(args[0]));
  }

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
    if (res instanceof CanonicalResource) {
      boolean changed = false;
      CanonicalResource cr = (CanonicalResource) res;
      if (!Utilities.existsInList(cr.fhirType(), "EvidenceReport", "SubscriptionTopic")) {
        if (!cr.hasTitle()) {
          if (!cr.hasName()) {
            System.out.println("No name or title: "+name);
          } else if (cr.getName().contains(" ")) {
            cr.setTitle(cr.getName());
            changed = true;
          } else {
            cr.setTitle(Utilities.unCamelCaseKeepCapitals(cr.getName()));
            changed = true;
          }
        } else if (!cr.hasName()) {
          cr.setName(Utilities.camelCase(cr.getTitle()));
          changed = true;
        } else if (cr.getName().equals(cr.getTitle())) {
          cr.setTitle(Utilities.unCamelCaseKeepCapitals(cr.getName()));
          changed = true;
        }
        if (!cr.hasDescription()) {
          cr.setDescription(cr.getTitle());
          changed = true;
        }
      }
      if (!cr.hasName()) {
        if (cr.hasTitle()) {
          System.out.println("No name: "+name);
        }
      } else {
        if (cr.getName().contains(" ")) {
          cr.setName(Utilities.camelCase(cr.getName()));   
          changed = true;
        }
        if (Character.isLowerCase(cr.getName().charAt(0))) {
          cr.setName(Character.toUpperCase(cr.getName().charAt(0))+cr.getName().substring(1));   
          changed = true;
        }
        changed = checkCRName(cr, " ") || changed;
        changed = checkCRName(cr, "-") || changed;
        changed = checkCRName(cr, "—") || changed;
        changed = checkCRName(cr, ".") || changed;
        changed = checkCRName(cr, "#") || changed;
        changed = checkCRName(cr, "/") || changed;
        changed = checkCRName(cr, "\\") || changed;
        changed = checkCRName(cr, "`") || changed;
        changed = checkCRName(cr, "(") || changed;
        changed = checkCRName(cr, ")") || changed;
        changed = checkCRName(cr, ":") || changed;
        changed = checkCRName(cr, "’") || changed;
        changed = checkCRName(cr, "≥") || changed;
        if (!Utilities.isValidCRName(cr.getName())) {
          System.out.println("Problem name: "+name+" ("+cr.getName()+")");                
        }
      }
      return changed;
    } else {
      return false;
    }
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
