package org.hl7.fhir.tools.converters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.tools.converters.PageCrossLinker.EVersion;
import org.hl7.fhir.tools.converters.PageCrossLinker.PageUsage;
import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.Utilities;

public class PageCrossLinker {
  
  public enum EVersion {
    V2,V3,V4,V4B,V5;
  }

  public class PageUsage {
    boolean r2 = false;
    boolean r3 = false;
    boolean r4 = false;
    boolean r4b = false;
    boolean r5 = false;
    
    private void use(EVersion ver) {
      switch (ver) {
      case V2:
        r2 = true;
        break;
      case V3:
        r3 = true;
        break;
      case V4:
        r4 = true;
        break;
      case V4B:
        r4b = true;
        break;
      case V5:
        r5 = true;
        break;
      default:
        break;      
      }
    }

    public String summary() {
      return " "+r2+" "+r3+" "+r4+" "+r4b+" "+r5;
    }

    public boolean xlink() {
      int i = 0;
      if (r2 ) {i++;}
      if (r3 ) {i++;}
      if (r4 ) {i++;}
      if (r4b) {i++;}
      if (r5 ) {i++;}
      return i > 1;
    }
  }

  public static void main(String[] args) throws IOException {
    new PageCrossLinker().execute(new File(args[0]));
  }

  private void execute(File root) throws IOException { 
    System.out.println("Scan Versions");
    List<String> versions = scanVersions(root);
    
    Map<String, PageUsage> files = new HashMap<>();    
    scanPages(files, root, "DSTU2", EVersion.V2);
    scanPages(files, root, "STU3", EVersion.V3);
    scanPages(files, root, "R4", EVersion.V4);
    scanPages(files, root, "R4B", EVersion.V4B);
    scanPages(files, root, "R5", EVersion.V5);   
    
//    makeXLinks(root.getAbsolutePath(), files, EVersion.V5);
//    makeXLinks(Utilities.path(root.getAbsolutePath(), "DSTU2"), files, EVersion.V2);
//    makeXLinks(Utilities.path(root.getAbsolutePath(), "STU3"), files, EVersion.V3);
//    makeXLinks(Utilities.path(root.getAbsolutePath(), "R4"), files, EVersion.V4);
//    makeXLinks(Utilities.path(root.getAbsolutePath(), "R4B"), files, EVersion.V4B);
    makeXLinks(Utilities.path(root.getAbsolutePath(), "R5"), files, EVersion.V5);
    for (String s : versions) {
      if (!Utilities.existsInList(s, "DSTU2", "STU3", "R4", "R4B", "R5")) {
        makeXLinks(Utilities.path(root.getAbsolutePath(), s), files, null);
      }
    }
    System.out.println("Done!");
  }

  private void makeXLinks(String path, Map<String, PageUsage> files, EVersion ver) throws IOException {
    System.out.print("Make XLinks in "+path+" for version "+ver);
    int fcount = 0;
    int tcount = 0;
    for (String s : Utilities.sorted(files.keySet())) {
      PageUsage u = files.get(s);
      if (u.xlink()) {
        File fn = new File(Utilities.path(path, s));
        if (!fn.exists()) { 
          fn = new File(Utilities.path(path, s.replace(".html", ".htm")));
        }
        if (fn.exists()) {
          fcount++;
          String src = TextFile.fileToString(fn);
          int i = src.indexOf("<!--EndReleaseHeader");
          if (i < 1) {
            i = src.indexOf("<!-- EndReleaseHeader");
          }
          if (i < 1) {
            i = src.indexOf("<!--  EndReleaseHeader");
          }
          if (i > 0) {
            while (i > 0 && !"</p>".equals(src.substring(i,i+4))) {
              i--;
            }
            if ( i > 0) {
              tcount++;
              String sp = src.substring(0, i);
              int si = sp.indexOf("<!--pxl-->");
              if (si > 0) {
                sp = sp.substring(0, si-2);
              }
              src = sp+buildInsert(s, ver, u)+src.substring(i);
              TextFile.stringToFile(src, fn);
            }
          }
        }
      }
    }
    System.out.println(": "+fcount+" / "+tcount);    
  }

  private String buildInsert(String s, EVersion ver, PageUsage u) {
    StringBuilder b = new StringBuilder();
    b.append(". <!--pxl-->Page versions:");
    if (u.r5) {
      if (ver == EVersion.V5) {
        b.append(" <b>R5</b>");
      } else {
        b.append(" <a href=\"http://hl7.org/fhir/R5/"+s+"\">R5</a>");        
      }
    }
    if (u.r4b) {
      if (ver == EVersion.V4B) {
        b.append(" <b>R4B</b>");
      } else {
        b.append(" <a href=\"http://hl7.org/fhir/R4B/"+s+"\">R4B</a>");        
      }
    }
    if (u.r4) {
      if (ver == EVersion.V4) {
        b.append(" <b>R4</b>");
      } else {
        b.append(" <a href=\"http://hl7.org/fhir/R4/"+s+"\">R4</a>");        
      }
    }
    if (u.r3) {
      if (ver == EVersion.V3) {
        b.append(" <b>R3</b>");
      } else {
        b.append(" <a href=\"http://hl7.org/fhir/STU3/"+s+"\">R3</a>");        
      }
    }
    if (u.r2) {
      if (ver == EVersion.V2) {
        b.append(" <b>R2</b>");
      } else {
        b.append(" <a href=\"http://hl7.org/fhir/DSTU2/"+s+"\">R2</a>");        
      }
    }
    return b.toString();
  }

  private void scanPages(Map<String, PageUsage> files, File root, String folder, EVersion ver) throws IOException {
    System.out.println("Scan "+ver+" in "+folder);
    String base = Utilities.path(root.getAbsolutePath(), folder);
    File f = new File(base);
    scanPageFiles(files, base, f, ver);
  }

  private void scanPageFiles(Map<String, PageUsage> files, String base, File dir, EVersion ver) {
    for (File f : dir.listFiles()) {
      if (f.isDirectory()) {
        scanPageFiles(files, base, f, ver);
      } else if (f.getName().endsWith(".html")) {
        String relPath = f.getAbsolutePath().substring(base.length()+1);
        PageUsage usage = files.get(relPath); 
        if (usage == null) {
          usage = new PageUsage();
          files.put(relPath, usage);
        }
        usage.use(ver);
      }
    }
    
  }

  private List<String> scanVersions(File root) throws IOException {
    List<String> list = new ArrayList<>();
    for (File f : root.listFiles()) {
      if (f.isDirectory()) {
        File x = new File(Utilities.path(f.getAbsolutePath(), "xml.html"));
        if (!x.exists()) {
          x = new File(Utilities.path(f.getAbsolutePath(), "xml.htm"));        
        }
        if (x.exists()) {
          list.add(f.getName());
        }
      }
    }
    return list;
  }

}
