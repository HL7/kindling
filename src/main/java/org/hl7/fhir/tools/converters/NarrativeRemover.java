package org.hl7.fhir.tools.converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.hl7.fhir.exceptions.FHIRFormatError;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.Narrative.NarrativeStatus;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.ValueSet;

public class NarrativeRemover {

  public static void main(String[] args) throws FHIRFormatError, FileNotFoundException, IOException {
    new NarrativeRemover().remove(new File("/Users/grahamegrieve/work/r5/source"));
  }

  private void remove(File file)  {
    for (File f : file.listFiles()) {
      if (f.isDirectory()) {
        remove(f);
      } else if (f.getName().startsWith("codesystem-")) {
//        System.out.println("Check "+f.getAbsolutePath());
        try {
          Resource res = new XmlParser().parse(new FileInputStream(f));
          if (res instanceof CodeSystem) {
            CodeSystem cs = (CodeSystem) res;
            if (cs.hasText() && cs.getText().getStatus() == NarrativeStatus.GENERATED) {
//              cs.setText(null);
//              cs.setMeta(null);
//              new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(f), cs);
//              System.out.println(f+": has narrative");
            }
          }
        } catch (Exception e) {
//          System.out.println(f+": error:  "+e.getMessage());
        }
      } else if (f.getName().startsWith("valueset-")) {
//        System.out.println("Check "+f.getAbsolutePath());
        try {
          Resource res = new XmlParser().parse(new FileInputStream(f));
          if (res instanceof ValueSet) {
            ValueSet vs = (ValueSet) res;
            if (vs.hasText() && vs.getText().getStatus() == NarrativeStatus.GENERATED) {
//              vs.setText(null);
//              vs.setMeta(null);
//              new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(f), vs);
              System.out.println(f+": has narrative");
            }
//            if (vs.hasExpansion()) {
//              System.out.println(f+": has expansion");
//            }
          }
        } catch (Exception e) {
//          System.out.println("  "+e.getMessage());
        }
      }
    }
    
  }

}
