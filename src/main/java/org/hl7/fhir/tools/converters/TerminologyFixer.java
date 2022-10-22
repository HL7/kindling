package org.hl7.fhir.tools.converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.hl7.fhir.convertors.loaders.loaderR5.ILoaderKnowledgeProviderR5;
import org.hl7.fhir.convertors.loaders.loaderR5.R4ToR5Loader;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.FHIRFormatError;
import org.hl7.fhir.r4b.model.CanonicalResource;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.context.SimpleWorkerContext.SimpleWorkerContextBuilder;
import org.hl7.fhir.r5.elementmodel.Element;
import org.hl7.fhir.r5.elementmodel.ParserBase;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.Narrative.NarrativeStatus;
import org.hl7.fhir.tools.publisher.BuildWorkerContext;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.utilities.IniFile;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.json.JsonTrackingParser;
import org.hl7.fhir.utilities.json.JsonUtilities;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.hl7.fhir.utilities.npm.ToolsVersion;
import org.hl7.fhir.utilities.xml.XMLUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class TerminologyFixer {
  
  public static void main(String[] args) throws FHIRFormatError, FileNotFoundException, IOException {
    new TerminologyFixer().start(new File("/Users/grahamegrieve/work/r5/source"));
  }

  private void start(File file) throws FHIRException, IOException  {
    fix(file);
  }
  
  private void fix(File file) throws FHIRException, IOException  {
    for (File f : file.listFiles()) {
      if (f.isDirectory()) {
        if (!f.getName().equals("invariant-tests")) {
          fix(f);
        }
      } else if (f.getName().startsWith("codesystem-")) {
        try {
          Resource res = new XmlParser().parse(new FileInputStream(f));
          if (res instanceof CodeSystem) {
            CodeSystem cs = (CodeSystem) res;
            boolean changed = false;
            if (cs.hasName()) {
              String n = cs.getName();
              if (!Utilities.isToken(n)) {
                n = n.replace("-", " ").replace("/", " ").replace("\\", " ").replace("'", " ").replace("(", " ").replace(")", " ").replace("  ", " ");
                if (Utilities.charCount(n, ' ') <= 4) {
                  n = Utilities.upperCamelCase(n);
                }
                if (Utilities.isToken(n)) {
                  if (!cs.hasTitle()) {
                    cs.setTitle(cs.getName());
                  }
                  cs.setName(n);
                  changed = true;
                } else {
                  System.out.println("illegal name: "+cs.getName());
                }
              }
            }
            if (changed) {
              new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(f), cs);
              System.out.println(f+" fixed");              
            }
          }
        } catch (Exception e) {
//          System.out.println(f+": error:  "+e.getMessage());
        }
      } else if (f.getName().startsWith("valueset-")) {
        try {
          Resource res = new XmlParser().parse(new FileInputStream(f));
          if (res instanceof ValueSet) {
            ValueSet vs = (ValueSet) res;
            boolean changed = false;
            if (vs.hasName()) {
              String n = vs.getName();
              if (!Utilities.isToken(n)) {
                n = n.replace("-", " ").replace("/", " ").replace("\\", " ").replace("'", " ").replace("(", " ").replace(")", " ").replace("  ", " ");
                if (Utilities.charCount(n, ' ') <= 4) {
                  n = Utilities.upperCamelCase(n);
                }
                if (Utilities.isToken(n)) {
                  if (!vs.hasTitle()) {
                    vs.setTitle(vs.getName());
                  }
                  vs.setName(n);
                  changed = true;
                } else {
                  System.out.println("illegal name: "+vs.getName());
                }
              }
            }
            if (changed) {
              new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(f), vs);
              System.out.println(f+" fixed");              
            }
          }
        } catch (Exception e) {
//          System.out.println("  "+e.getMessage());
        }
      }
    }
//    ini.save();
  }

}
