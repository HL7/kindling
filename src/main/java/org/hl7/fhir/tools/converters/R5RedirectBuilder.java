package org.hl7.fhir.tools.converters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.tools.publisher.SpecMapManager;
import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.json.model.JsonObject;
import org.hl7.fhir.utilities.json.parser.JsonParser;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.hl7.fhir.utilities.npm.NpmPackage.PackageResourceInformation;
import org.hl7.fhir.utilities.xhtml.XhtmlComposer;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.hl7.fhir.utilities.xhtml.XhtmlParser;

import com.google.gson.JsonSyntaxException;

public class R5RedirectBuilder {


  private String web;

  public R5RedirectBuilder(String web) {
    this.web = web;
  }

  public static void main(String[] args) throws FileNotFoundException, IOException {
//    new R5RedirectBuilder().buildMap("/Users/grahamegrieve/web/hl7.org/fhir");
//  new R5RedirectBuilder().processSP("/Users/grahamegrieve/web/hl7.org/fhir");
    new R5RedirectBuilder("/Users/grahamegrieve/web/hl7.org/fhir").buildRedirects("/Users/grahamegrieve/work/fhir-web-templates");
    System.out.println("done");
  }

  private void buildRedirects(String templates) throws IOException {
    Map<String, String> map = loadUrlMap();
    String wct = TextFile.fileToString(Utilities.path(templates, "redirects", "web.config"));
    wct = wct.replace("<%names%>", nameRedirects(map, templates));
    TextFile.stringToFile(wct, Utilities.path(web, "web.config"));
  }

  private Map<String, String> loadUrlMap() throws IOException {
    Map<String, String> res = new HashMap<>();
    File file = new File(Utilities.path(web, "url-map.csv"));  
    FileReader fr = new FileReader(file);   //reads the file  
    BufferedReader br=new BufferedReader(fr);  //creates a buffering character input stream  
    String line;  
    while((line=br.readLine())!=null)  {
      if (!Utilities.noString(Utilities.stripBOM(line.trim()))) {
        res.put(line.substring(0, line.indexOf(",")).substring(20), line.substring(line.indexOf(",")+1));
      }
    }
    return res;
  }

  private CharSequence nameRedirects(Map<String, String> map, String templates) throws IOException {
    Map<String, List<String>> uses = new HashMap<>();
    for (String m : map.keySet()) {
      String n = m;
      if (n.contains("/")) {
        n = n.substring(0, n.indexOf("/"));
        if (!uses.containsKey(n)) {
          uses.put(n, new ArrayList<>());
        }
        uses.get(n).add(m);
      } else {
        makeRedirect(n, map.get(m), templates);
      }
    }
    List<String> names = new ArrayList<>();
    for (String n : uses.keySet()) {
      if (!Utilities.noString(n)) {
        names.add(n);
        generateRedirect(n, uses.get(n), map);
      }
    }
    Collections.sort(names);
    StringBuilder b = new StringBuilder();
    for (String n : names) {
      b.append("        <rule name=\""+n+"\">\r\n");
      b.append("          <match url=\"^("+n+")/([A-Za-z0-9\\-\\.]{1,64})\" />\r\n");
      b.append("          <action type=\"Rewrite\" url=\"cr"+n+".asp?type={R:1}&amp;id={R:2}\" />\r\n");
      b.append("        </rule>\r\n");
    }
    return b.toString();
  }


  private void makeRedirect(String n, String tgt, String templates) throws FileNotFoundException, IOException {
    String asp;
    if (Utilities.isAbsoluteUrl(tgt)) {
      asp = TextFile.fileToString(Utilities.path(templates, "redirects", "redirect-external.asp"));
      asp = asp.replace("<%tgt%>", tgt);
    } else {
      asp = TextFile.fileToString(Utilities.path(templates, "redirects", "redirect.asp"));
      asp = asp.replace("<%tgt%>", tgt.replace(".html", ""));
    }
    String dir = Utilities.path(web, n);
    Utilities.createDirectory(dir);
    TextFile.stringToFile(asp, Utilities.path(dir, "index.asp"));    
  }

  private void generateRedirect(String rt, List<String> urls, Map<String, String> map) throws IOException {
    StringBuilder b = new StringBuilder();

    String root = Utilities.pathURL("https://hl7.org/fhir", rt);
    b.append("<%@ language=\"javascript\"%>\r\n" + 
        "\r\n" + 
        "<%\r\n" + 
        "  var s = String(Request.ServerVariables(\"HTTP_ACCEPT\"));\r\n" +
        "  var id = Request.QueryString(\"id\");\r\n"+
        "  if (s.indexOf(\"application/json+fhir\") > -1) \r\n" + 
        "    Response.Redirect(\""+root+"-\"+id+\".json2\");\r\n" + 
        "  else if (s.indexOf(\"application/fhir+json\") > -1) \r\n" + 
        "    Response.Redirect(\""+root+"-\"+id+\".json1\");\r\n" + 
        "  else if (s.indexOf(\"application/xml+fhir\") > -1) \r\n" + 
        "    Response.Redirect(\""+root+"-\"+id+\".xml2\");\r\n" + 
        "  else if (s.indexOf(\"application/fhir+xml\") > -1) \r\n" + 
        "    Response.Redirect(\""+root+"-\"+id+\".xml1\");\r\n" + 
        "  else if (s.indexOf(\"json\") > -1) \r\n" + 
        "    Response.Redirect(\""+root+"-\"+id+\".json\");\r\n" + 
        "  else if (s.indexOf(\"html\") == -1) \r\n" + 
        "    Response.Redirect(\""+root+"-\"+id+\".xml\");\r\n" );
    for (String s : urls) {
      String id = s.substring(rt.length()+1);
      String link = map.get(s);
      b.append("  else if (id == \""+id+"\")\r\n" + 
          "    Response.Redirect(\"https://hl7.org/fhir/"+link+"\");\r\n");
    }
    b.append("  else if (id == \"index\")\r\n" + 
        "    Response.Redirect(\""+root+".html\");\r\n");
    b.append(     
        "\r\n" + 
            "%>\r\n" + 
            "\r\n" + 
            "<!DOCTYPE html>\r\n" + 
            "<html>\r\n" + 
            "<body>\r\n" + 
            "Internal Error - unknown id <%= Request.QueryString(\"id\") %> (from "+"cr"+rt+".asp"+") .\r\n" + 
            "</body>\r\n" + 
        "</html>\r\n");

    String asp = b.toString();
    File f = new File(Utilities.path(web, "cr"+rt+".asp"));
    if (f.exists()) {
      String aspc = TextFile.fileToString(f);
      if (aspc.equals(asp))
        return;
    }
    TextFile.stringToFile(b.toString(), f);       
  }

  
  private void processSP(String web) throws FHIRException, FileNotFoundException, IOException {
    XhtmlNode x = new XhtmlParser().parseFragment(TextFile.fileToString(Utilities.path(web, "searchparameter-registry.html")));
    XhtmlNode tbl = x.getElementById("sp");
    for (XhtmlNode tr : tbl.getChildren("tr")) {
      List<XhtmlNode> tdl = tr.getChildren("td");
      if (tdl.size() == 5) {
        XhtmlNode td3 = tdl.get(2);
        String id = td3.allText();
        if (id.contains("-")) {
          td3.an(id).tx(" ");
        }
      }
    }
    new XhtmlComposer(false).composeDocument(new FileOutputStream(Utilities.path(web, "searchparameter-registry.out.html")), x);
  }

  private void buildMap(String web) throws IOException {
    Map<String, String> files = new HashMap<>();
    for (File f : new File(web).listFiles()) {
      if (f.getName().endsWith("json")) {
        String src = readJsonFile(f);
        if (src != null) {
          String fn = Utilities.changeFileExt(f.getName(), ".html");
          File f2 = new File(Utilities.path(web, fn));
          if (f2.exists()) {
            files.put(src, f2.getName());
          }
        }
      }
    }
    FilesystemPackageCacheManager pcm = new FilesystemPackageCacheManager.Builder().build();
    Map<String, String> urlMap = new HashMap<>();
    loadFromPackage(files, urlMap, "", pcm.loadPackage("hl7.fhir.r5.core"));
    loadFromPackage(files, urlMap, "", pcm.loadPackage("hl7.fhir.r5.examples"));
    loadFromPackage(files, urlMap, "extensions/", pcm.loadPackage("hl7.fhir.uv.extensions"));

    StringBuilder b = new StringBuilder();
    int i = 0;
    for (String s : Utilities.sorted(urlMap.keySet())) {
      String path = urlMap.get(s);
      if (path.startsWith("http://hl7.org/fhir")) {
        path = path.substring(19);
      }
      if (Utilities.isAbsoluteUrl(path)) {
        System.out.println(s+" : " +path);
      } else {
        if (path.contains("#")) {
          path = path.substring(0, path.indexOf("#"));
        }
        File f = new File(Utilities.path("/Users/grahamegrieve/web/hl7.org/fhir", path));
        if (!f.exists()) {
          i++;
          System.out.println(s+" : " +urlMap.get(s)+" ==> "+f.getAbsolutePath());
        }
      }
      b.append(s);
      b.append(",");
      b.append(urlMap.get(s));
      b.append("\r\n");
    }
    TextFile.stringToFile(b.toString(), Utilities.path(web, "url-map.csv"));
    System.out.println("Broken Links: "+i);
  }

  private String readJsonFile(File f) {
    try {
      JsonObject j = JsonParser.parseObject(f);
      if (j.has("resourceType") && j.has("id")) {
        return j.asString("resourceType")+"/"+j.asString("id");
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }

  private void loadFromPackage(Map<String, String> files, Map<String, String> urlMap, String prefix, NpmPackage npm) throws JsonSyntaxException, IOException {
    System.out.println("Load from "+npm.id());
    if (npm.hasFile("other", "spec.internals")) {
      SpecMapManager smm = new SpecMapManager(TextFile.streamToBytes(npm.load("other", "spec.internals")), npm.fhirVersion());
      for (String s : smm.getPathUrls()) {
        if (isRelevantURL(s)) {
          if (s.contains("|")) {
            s = s.substring(0, s.indexOf("|"));
          }
          if (!urlMap.containsKey(s)) {
            urlMap.put(s, prefix+smm.getPath(s));
          }
        }
      }
    }
    for (PackageResourceInformation pri : npm.listIndexedResources()) {
      String url = pri.getUrl() != null ? pri.getUrl() : "http://hl7.org/fhir/"+pri.getResourceType()+"/"+pri.getId();
      if (url != null && !urlMap.containsKey(url) && isRelevantURL(url)) {
        String path = urlMap.get(url);
        if (path == null) {
          if (files.containsKey(pri.getResourceType()+"/"+pri.getId())) {
            path = files.get(pri.getResourceType()+"/"+pri.getId());
          } else {
            path = pathForResourcel(pri);
          }
          if (path != null) {
            urlMap.put(url, path);
          }
        }
      }
    }
  }

  private boolean isRelevantURL(String s) {
    return s.startsWith("http://hl7.org/fhir") && !s.startsWith("http://hl7.org/fhir/extensions") 
        && !s.contains("qgen") && !s.contains("to4")&& !s.contains("to5") && !s.contains("qgen") ;
  }

  private String pathForResourcel(PackageResourceInformation pri) {
    String id = pri.getId();
    if (id == null) {
      return null;
    }
    switch (pri.getResourceType()) {
    case "Bundle":
      return "bundle-examples.html";
    case "ImplementationGuide":
      return "implementationguide-fhir.html";
    case "CapabilityStatement":
      return pri.getResourceType().toLowerCase()+"-"+id.toLowerCase()+".json.html";
    case "StructureDefinition":
      return id.toLowerCase()+".html";
    case "SearchParameter":
      if (id.contains("-")) {
        return "searchparameter-registry.html#"+id;
      } else {
        return "searchparameter-"+id+".html";
      }
    case "OperationDefinition":
      if (id.contains("-")) {
        return id.substring(0, id.indexOf("-")).toLowerCase()+"-operation-"+id.substring(id.indexOf("-")+1)+".html";
      } else {
        return "operationdefinition-"+id+".html";
      }
    default: 
      return pri.getResourceType().toLowerCase()+"-"+id.toLowerCase()+".html";
    }
  }

}
