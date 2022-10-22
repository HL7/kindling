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
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.context.SimpleWorkerContext.SimpleWorkerContextBuilder;
import org.hl7.fhir.r5.elementmodel.Element;
import org.hl7.fhir.r5.elementmodel.ParserBase;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.CanonicalResource;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.Narrative.NarrativeStatus;
import org.hl7.fhir.tools.converters.NarrativeRemover.TempLoader;
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

public class NarrativeRemover {

  public class TempLoader implements ILoaderKnowledgeProviderR5 {

    @Override
    public String getResourcePath(Resource resource) {
      return "nowhere";
    }

    @Override
    public ILoaderKnowledgeProviderR5 forNewPackage(NpmPackage npm) throws JsonSyntaxException, IOException {
      return null;
    }

    @Override
    public String getWebRoot() {
      return "nowhere";
    }

  }

  Map<String, String> allOids = new HashMap<>();
  private NpmPackage r4;
  private IniFile ini;
  private SimpleWorkerContext ctxt;
  
  public static void main(String[] args) throws FHIRFormatError, FileNotFoundException, IOException {
    new NarrativeRemover().start(new File("/Users/grahamegrieve/work/r5/source"));
  }

  private void start(File file) throws FHIRException, IOException  {
    ini = new IniFile("/Users/grahamegrieve/work/r5/source/oids.ini");
    checkIni();
    r4 = new FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION).loadPackage("hl7.fhir.r4.core");
    ctxt = new SimpleWorkerContextBuilder().fromPackage(r4, new R4ToR5Loader(BuildWorkerContext.defaultTypesToLoad(), new TempLoader(), "4.0.0"));
    remove(file);
  }
  
  private void remove(File file) throws FHIRException, IOException  {
    for (File f : file.listFiles()) {
      if (f.isDirectory()) {
        if (!f.getName().equals("invariant-tests")) {
          remove(f);
        }
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
//    ini.save();
  }

  private void checkIni() {
    for (String s : ini.getPropertyNames("Key")) {
      ini.setIntegerProperty("Key", s, checkIniSection(s), null);
    }
    ini.save();
  }

  private int checkIniSection(String section) {
    Set<String> oids = new HashSet<>();
    int max = 0;

    String[] list = ini.getPropertyNames(section);
    if (list != null) {
      for (String s : list) {
        String oid = ini.getStringProperty(section, s);
        if (oids.contains(oid)) {
          throw new Error("duplicate OID "+oid);
        }
        oids.add(oid);
        allOids.put(oid, s);
        int key = Integer.parseInt(tail(oid));
        if (key > max) {
          max = key;
        }
      }
    }
    return max;
  }
  

  private String tail(String oid) {
    return oid.substring(oid.lastIndexOf(".")+1);
  }

  private void checkJsonOid(File f) {
    JsonObject doc;
    try {
      doc = JsonTrackingParser.parseJson(f);
      if (doc.has("resourceType")) {
        String url = JsonUtilities.str(doc, "url");
        if (!Utilities.noString(url)) {
          String oid = null;
          for (JsonObject id : JsonUtilities.objects(doc, "identifier")) {
            String system = JsonUtilities.str(id, "system");
            String value = JsonUtilities.str(id, "value");
            if ("urn:ietf:rfc:3986".equals(system) && value != null && value.startsWith("urn:oid:")) {
              oid = value.substring(8);
            }
          }
          String newOid = analyseOid(url, oid, JsonUtilities.str(doc, "resourceType"));
          if (newOid != null) {
           System.out.println("JSON Edit: set oid to "+newOid+" in "+f.getAbsolutePath());
          }
        }
      }
    } catch (Exception e) {
//      System.out.println(f+": "+e.getMessage());
    }
  }

  private String lookupUrl(String url) {
    for (org.hl7.fhir.r5.model.CanonicalResource cr : ctxt.fetchResourcesByType(CanonicalResource.class)) {
      if (url.equals(cr.getUrl())) {
        return cr.getOid();
      }
    }
    return null;
  }

  private void checkXmlOid(File f) {
    Document doc;
    try {
      doc = XMLUtil.parseFileToDom(f.getAbsolutePath(), true);
      org.w3c.dom.Element root = doc.getDocumentElement();
      if ("http://hl7.org/fhir".equals(root.getNamespaceURI())) {
        String url = XMLUtil.getNamedChildValue(root, "url");
        if (!Utilities.noString(url)) {
          String oid = null;
          for (org.w3c.dom.Element id : XMLUtil.getNamedChildren(root, "identifier")) {
            String system = XMLUtil.getNamedChildValue(id, "system");
            String value = XMLUtil.getNamedChildValue(id, "value");
            if ("urn:ietf:rfc:3986".equals(system) && value != null && value.startsWith("urn:oid:")) {
              oid = value.substring(8);
            }
          }
          String newOid = analyseOid(url, oid, root.getLocalName());
          if (newOid != null) {
            boolean done = false;
            for (org.w3c.dom.Element id : XMLUtil.getNamedChildren(root, "identifier")) {
              String system = XMLUtil.getNamedChildValue(id, "system");
              String value = XMLUtil.getNamedChildValue(id, "value");
              if ("urn:ietf:rfc:3986".equals(system) && value != null && value.startsWith("urn:oid:")) {
                XMLUtil.setNamedChildValue(id, "value", "urn:oid:"+newOid);
                done = true;
              }
            }
            if (!done) {
              org.w3c.dom.Element urlE = XMLUtil.getNextSibling(XMLUtil.getNamedChild(root, "url"));
              org.w3c.dom.Element idE = doc.createElement("identifier");
              root.insertBefore(idE, urlE);
              root.insertBefore(doc.createTextNode("\n  "), urlE);
              org.w3c.dom.Element sysE = doc.createElement("system");
              idE.appendChild(doc.createTextNode("\n    "));
              idE.appendChild(sysE);
              sysE.setAttribute("value", "urn:ietf:rfc:3986");
              org.w3c.dom.Element valE = doc.createElement("value");
              idE.appendChild(doc.createTextNode("\n    "));
              idE.appendChild(valE);
              valE.setAttribute("value", "urn:oid:"+newOid);
              idE.appendChild(doc.createTextNode("\n  "));              
            }
            XMLUtil.writeDomToFile(doc, f.getAbsolutePath());
          }
        }
      }
    } catch (Exception e) {
//      System.out.println(f+": "+e.getMessage());
    }
  }

  private String analyseOid(String url, String oid, String rn) {
    String oldOid = lookupUrl(url);
    if (oldOid == null) {
      if (oid == null) {
        if (Utilities.existsInList(rn, ini.getPropertyNames("Key"))) {
          String newOID = getNextOid(rn, url);
          System.out.println(Utilities.padRight(url, ' ', 70)+": Assiging OID "+newOID);   
          return newOID;
        } else {
          System.out.println(Utilities.padRight(url, ' ', 70)+": Needs OID ("+rn+")");
        }
      } else {
        if (isValidOid(rn, oid)) {
          if (allOids.containsKey(oid)) {
            if (allOids.get(oid).equals(url)) {
              // found self - nothing
            } else if (Utilities.existsInList(rn, ini.getPropertyNames("Key"))) {
              String newOID = getNextOid(rn, url);
              System.out.println(Utilities.padRight(url, ' ', 70)+": Has new OID: "+oid+" but OID is already used by "+allOids.get(oid)+" assigning new OID "+newOID);
              return newOID;
            } else {
              System.out.println(Utilities.padRight(url, ' ', 70)+": Has new OID: "+oid+" but OID is already used by "+allOids.get(oid));
            }
          } else {
            int key = Integer.parseInt(tail(oid));
            if (key > ini.getIntegerProperty("Key", rn)) {
              ini.setIntegerProperty("Key", rn, key, null);
            }
            ini.setStringProperty(rn, url, oid, null);
            ini.save();
            allOids.put(oid,  url);
            System.out.println(Utilities.padRight(url, ' ', 70)+": Confirming OID "+oid);   
          }
        } else {
          String newOID = getNextOid(rn, url);
          System.out.println(Utilities.padRight(url, ' ', 70)+": Has new OID: "+oid+" that is not valid so assigned "+newOID);
          return newOID;
        }
      }
    } else {
      if (oid == null) {
        if (allOids.containsKey(oldOid)) {
          if (allOids.get(oldOid).equals(url)) {
            System.out.println(Utilities.padRight(url, ' ', 70)+": Reassigning Old OID ("+oldOid+")");          
            return oldOid;
          } else {
            String newOID = getNextOid(rn, url);
            System.out.println(Utilities.padRight(url, ' ', 70)+": Needs OID reassigned ("+oldOid+") but OID is reused for "+allOids.get(oldOid)+" so assigned "+newOID);
            return newOID;
          }
        } else {
          System.out.println(Utilities.padRight(url, ' ', 70)+": Reassigning Old OID ("+oldOid+")");          
          ini.setStringProperty(rn, url, oldOid, null);
          ini.save();
          allOids.put(oldOid, url);
          return oldOid;
        }
      } else if (oid.equals(oldOid)) {
        if (isValidOid(rn, oid)) {
          if (!oid.equals(ini.getStringProperty(rn, url))) {
            ini.setStringProperty(rn, url, oid, null);
            ini.save();
          }
          allOids.put(oid,  url);
        } else {
          if (oid.startsWith(ini.getStringProperty("Root", "Any"))) {
            System.out.println(Utilities.padRight(url, ' ', 70)+": OID is the same ("+oid+") but not correct");
          } else {
            // System.out.println(Utilities.padRight(url, ' ', 70)+": OID is the same ("+oid+") but not correct");
          }
        }
      } else {
        if (isValidOid(rn, oldOid)) {
          System.out.println(Utilities.padRight(url, ' ', 70)+": OIDs differ ("+oldOid+" -> "+oid+") : changing!");            
          return oldOid;
        } else {
          System.out.println(Utilities.padRight(url, ' ', 70)+": OIDs differ ("+oldOid+" -> "+oid+")");            
        }
      }
    }
    return null;
  }

  private String getNextOid(String rn, String url) {
    int key = ini.getIntegerProperty("Key", rn);
    key++;
    ini.setIntegerProperty("Key", rn, key, null);
    String oid = base(rn)+"."+key;
    if (allOids.containsKey(oid)) {
      throw new Error("Double use of OID "+oid);
    }
    allOids.put(oid, rn);
    ini.setStringProperty(rn, url, oid, null);
    ini.save();
    return oid;
  }
  

  private String base(String rn) {
    if (ini.hasProperty("Roots", rn)) {
      return ini.getStringProperty("Roots", rn);
    }
    throw new Error("Error");
  }


  private boolean isValidOid(String rn, String oid) {
    if (ini.hasProperty("Roots", rn)) {
      return oid.startsWith(ini.getStringProperty("Roots", rn)+".");
    }
    return false;
  }

}
