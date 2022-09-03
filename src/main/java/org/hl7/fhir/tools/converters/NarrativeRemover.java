package org.hl7.fhir.tools.converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.FHIRFormatError;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.context.SimpleWorkerContext.SimpleWorkerContextBuilder;
import org.hl7.fhir.r5.elementmodel.Element;
import org.hl7.fhir.r5.elementmodel.ParserBase;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.Narrative.NarrativeStatus;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.utilities.json.JsonTrackingParser;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.hl7.fhir.utilities.npm.ToolsVersion;
import org.hl7.fhir.utilities.xml.XMLUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.gson.JsonObject;

public class NarrativeRemover {

  public static void main(String[] args) throws FHIRFormatError, FileNotFoundException, IOException {
    new NarrativeRemover().remove(new File("/Users/grahamegrieve/work/r5/source"));
  }

  private void remove(File file) throws FHIRException, IOException  {
    NpmPackage r4 = new FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION).loadPackage("hl7.fhir.r4.core");
    SimpleWorkerContext ctxt = new SimpleWorkerContextBuilder().fromPackage(r4);
    for (File f : file.listFiles()) {
      if (f.isDirectory()) {
        remove(f);
      } else if (f.getName().endsWith(".xml")) {
        checkXmlOid(f);
      } else if (f.getName().endsWith(".json")) {
        checkJsonOid(f);
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

  private void checkJsonOid(File f) {
    JsonObject doc;
    try {
      doc = JsonTrackingParser.parseJson(f);
      if (doc.has("resourceType")) {

      }
    } catch (Exception e) {
      System.out.println(f+": "+e.getMessage());
    }
  }

  private void checkXmlOid(File f) {
    Document doc;
    try {
      doc = XMLUtil.parseFileToDom(f.getAbsolutePath());
      org.w3c.dom.Element root = doc.getDocumentElement();
      if ("http://hl7.org/fhir".equals(root.getNamespaceURI())) {

      }
    } catch (Exception e) {
      System.out.println(f+": "+e.getMessage());
    }
  }

  private void checkOid(File f, ParserBase p) {
    try {
      Element r = p.parseSingle(new FileInputStream(f));
    } catch (Exception e) {
      if (!e.getMessage().contains("wrong namespace")) {
        System.out.println(f+": "+e.getMessage());
      }
    }
  }

}
