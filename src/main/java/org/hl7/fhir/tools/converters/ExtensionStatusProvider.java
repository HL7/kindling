package org.hl7.fhir.tools.converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.hl7.fhir.model.ModelContext;
import org.hl7.fhir.model.core.formats.XmlParser;
import org.hl7.fhir.model.core.Resource;
import org.hl7.fhir.model.core.StructureDefinition;
import org.hl7.fhir.model.utilities.formats.OutputStyle;

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
          Resource res = new XmlParser(ModelContext.fullCoreContext()).parse(new FileInputStream(f));
          if (res instanceof StructureDefinition) {
            StructureDefinition sd = (StructureDefinition) res;
            if (!sd.hasTitle()) {
              if (!sd.hasName()) {
                sd.setName(sd.getId());
              }
              sd.setTitle(sd.getName());
              new XmlParser(ModelContext.fullCoreContext()).setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(f), sd);
            }
          }
        } catch (Exception e) {
          // nothing
        }
      }
    }
  }

}
