package org.hl7.fhir.tools.site;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.Utilities;

public class XmlJsonCloneMaker {

  public static void main(String[] args) throws IOException {
    new XmlJsonCloneMaker().checkXmlJsonClones(args[0]);
  }

  private int counter;
  private int clonedCount;
  private int clonedTotal;

  public void checkXmlJsonClones(String vf) throws IOException {
    clonedCount = 0;
    clonedTotal = 0;
    counter = 0;
    checkXmlJsonClones(new File(vf));
    System.out.println();
    System.out.println("Counter = "+counter);
    System.out.println("Cloned Count = "+clonedCount);
    System.out.println("Cloned Total = "+clonedTotal);
  }

  private void checkXmlJsonClones(File dir) throws IOException {
    for (File f : dir.listFiles()) {
      counter++;
      if (counter % 500 == 0) {
        System.out.print(".");
      }
      if (f.isDirectory()) {
        checkXmlJsonClones(f);
      } else if (f.getName().endsWith(".json")) {
        System.out.print(".");
        String src = TextFile.fileToString(f);
        if (src.contains("\"resourceType\"")) {
          checkUpdate(f, src, Utilities.changeFileExt(f.getAbsolutePath(), ".json1"));
          checkUpdate(f, src, Utilities.changeFileExt(f.getAbsolutePath(), ".json2"));
        } else {
          checkDeleteFile(Utilities.changeFileExt(f.getAbsolutePath(), ".json1"));
          checkDeleteFile(Utilities.changeFileExt(f.getAbsolutePath(), ".json2"));
        }
      } else if (f.getName().endsWith(".xml")) {
        clonedTotal++;
        String src = TextFile.fileToString(f);
        if (src.contains("xmlns=\"http://hl7.org/fhir\"")) {
          checkUpdate(f, src, Utilities.changeFileExt(f.getAbsolutePath(), ".xml1"));
          checkUpdate(f, src, Utilities.changeFileExt(f.getAbsolutePath(), ".xml2"));
        } else {
          checkDeleteFile(Utilities.changeFileExt(f.getAbsolutePath(), ".xml1"));
          checkDeleteFile(Utilities.changeFileExt(f.getAbsolutePath(), ".xml2"));          
        }
      }
    }
  }
  

  private void checkUpdate(File src, String cnt, String fn) throws FileNotFoundException, IOException {
    File dst = new File(fn);
    if (!dst.exists() || dst.lastModified() > dst.lastModified()) {
      clonedCount++;
      FileUtils.copyFile(src, dst);
    }
  }

  private void checkDeleteFile(String fn) {
    File f = new File(fn);
    if ((f.exists())) {
      clonedCount++;
      f.delete();
    }
  }

}
