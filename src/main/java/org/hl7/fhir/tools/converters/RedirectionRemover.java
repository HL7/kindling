package org.hl7.fhir.tools.converters;

import java.io.File;

import org.hl7.fhir.utilities.Utilities;

public class RedirectionRemover {

  public static void main(String[] args) {
    new RedirectionRemover().execute(new File(args[0]));
  }

  private void execute(File folder) {
    for (File f : folder.listFiles()) {
      if (f.isDirectory()) {
        if (!Utilities.existsInList(f.getName(), "uv", "us", "smart-app-launch")) {
          execute(f);
        }
      } else if (f.getName().endsWith(".asp") || f.getName().equals("web.config")) {
        System.out.println("Delete "+f.getAbsolutePath());
        f.delete();
      }
    }
    boolean empty = true;
    for (File f : folder.listFiles()) {
      empty = false;
      break;
    }
    if (empty) {
      folder.delete();
    }
    
  }

}
