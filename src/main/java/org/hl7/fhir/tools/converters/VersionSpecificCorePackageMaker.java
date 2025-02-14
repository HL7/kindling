package org.hl7.fhir.tools.converters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.hl7.fhir.utilities.FileUtilities;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.json.model.JsonObject;
import org.hl7.fhir.utilities.json.parser.JsonParser;

public class VersionSpecificCorePackageMaker {

  public static void main(String[] args) throws IOException {
    new VersionSpecificCorePackageMaker().trim(new File(args[0]), args[1]);
  }

  private void trim(File file, String insert) throws IOException {
    for (File f : file.listFiles()) {
      if (!f.getName().endsWith(".json")) {
        if (f.isDirectory()) {
          FileUtilities.clearDirectory(f.getAbsolutePath());
        }
        f.delete();
      } else if (!"package.json".equals(f.getName())) {
        JsonObject json = JsonParser.parseObject(f);
        if (json.asString("resourceType") == null) {
          f.delete();
        } else if (!json.asString("resourceType").equals("StructureDefinition")) {
          f.delete();
        } else if ("constraint".equals(json.asString("derivation"))) { 
          f.delete();
        } else if (!json.asString("kind").equals("resource")) { 
          f.delete();
        } else if (json.asString("abstract").equals("true")) { 
          f.delete();
        } else {
          json.set("url", json.asString("url").replace("http://hl7.org/fhir", "http://hl7.org/fhir/"+insert));
          JsonParser.compose(json, new FileOutputStream(f), false);
        }
      }
    }
  }

}
