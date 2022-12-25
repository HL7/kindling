package org.hl7.fhir.tools.converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.StructureDefinition.TypeDerivationRule;
import org.hl7.fhir.r5.utils.ToolingExtensions;
import org.hl7.fhir.utilities.StandardsStatus;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.Resource;

public class ExtensionStatusProvider {

  public static void main(String[] args) {
    new ExtensionStatusProvider().process(new File("/Users/grahamegrieve/work/r5/source")); 
  }

  private void process(File folder) { 
    for (File f : folder.listFiles()) {
      if (f.isDirectory()) {
        process(f);
      } else if (f.getName().endsWith(".xml")) {
        try {
          Resource res = new XmlParser().parse(new FileInputStream(f));
          if (res instanceof StructureDefinition) {
            StructureDefinition sd = (StructureDefinition) res;
            if (!sd.hasTitle()) {
              if (!sd.hasName()) {
                sd.setName(sd.getId());
              }
              sd.setTitle(sd.getName());
              new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(f), sd);
            }
          }
        } catch (Exception e) {
          // nothing
        }
      }
    }
  }

}
