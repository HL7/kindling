package org.hl7.fhir.tools.converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.utilities.CSVReader;
import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.Utilities;

public class R4B1Fixer {

  public class OIDMappingsSorter implements Comparator<OIDMapping> {

    @Override
    public int compare(OIDMapping arg0, OIDMapping arg1) {
      return arg0.index - arg1.index;
    }

  }

  public class OIDMapping {
    private String url;
    private String newOid;
    private List<String> oldOids = new ArrayList<>();
    private int index;
  }

  public static void main(String[] args) throws Exception {
    new R4B1Fixer().fix(new File("/Users/grahamegrieve/web/hl7.org/fhir/R4B.1"));    
  }

  private void fix(File folder) throws IOException {
    List<OIDMapping> mappings = loadList(Utilities.path(folder.getAbsolutePath(), "oid-map.csv"));
    int c = 0;
    int t = 0;
    for (File f : folder.listFiles()) {
      t++;
      if (t % 100 == 0) {
        System.out.print(".");
      }
      if (!f.isDirectory() && Utilities.endsWithInList(f.getName(), ".xml", ".json", ".json1", ".json2", ".xml1", ".xml2", ".html", ".ttl", 
          ".graphql", ".sch", ".svg", ".shex", ".xsd", ".jsonld")) {
        String src = TextFile.fileToString(f);
        String src2 = fixSource(src, f, mappings);
        if (!src2.equals(src)) {
          c++;
          TextFile.stringToFile(src2, f);
        }
      }
    }
    System.out.println("");
    System.out.println("Done. processed "+t+" files, fixed "+c+" files");
    
  }

  private List<OIDMapping> loadList(String src) throws FHIRException, FileNotFoundException, IOException {
    List<OIDMapping> mappings = new ArrayList<>();
    CSVReader csv = new CSVReader(new FileInputStream(src));
    csv.readHeaders();
    while (csv.line()) {
      OIDMapping m = new OIDMapping();
      m.url = csv.cell("URL");
      m.newOid = csv.cell("New OID");
      for (String s : csv.cell("Old OIDS").split("\\,")) {
        m.oldOids.add(s.trim());
      }
      mappings.add(m);
    }
    return mappings;
  }

  private String fixSource(String src, File f, List<OIDMapping> mappings) {
    if (src.contains("4.3.0")) {
      src = src.replace("4.3.0", "4.3.1");
    }
    List<OIDMapping> found = new ArrayList<>();
    for (OIDMapping om : mappings) {
      if (src.contains(om.url)) {
        found.add(om);
      }
    }
    if (found.size() > 1) {
      List<OIDMapping> found2 = new ArrayList<>();
      for (OIDMapping om : found) {
        int i = src.indexOf(om.oldOids.get(0));
        if (i >= 0) {
          om.index = i;
          found2.add(om);
        }
      }
      if (found2.size() > 0) {
        if (found2.size() == 1) {
          src = src.replace(found2.get(0).oldOids.get(0), found2.get(0).newOid);
        } else {
          Set<String> oids = new HashSet<>();
          Set<String> doids = new HashSet<>();
          for (OIDMapping om : found2) {
            if (!oids.contains(om.oldOids.get(0))) {
              oids.add(om.oldOids.get(0));
            } else {
              doids.add(om.oldOids.get(0));
            }
          }
          for (OIDMapping om : found2) {
            if (!doids.contains(om.oldOids.get(0))) {
              src = src.replace(om.oldOids.get(0), om.newOid);
            }
          }
          if (!doids.isEmpty()) {
            for (OIDMapping om : found2) {
              om.index = src.indexOf(om.url);
            }
            Collections.sort(found2, new OIDMappingsSorter());
            for (String oid : doids) {
              int i = src.indexOf(oid);
              while (i >= 0) {
                OIDMapping om = findMapping(found2, i, oid);
                src = src.substring(0, i) + om.newOid+src.substring(i+oid.length());
                i = src.indexOf(oid);
              }
            }
//            System.out.println();
//            System.out.println("More than one matching OID found in "+f.getAbsolutePath()+":");
//            for (OIDMapping om : found2) {
//              if (doids.contains(om.oldOids.get(0))) {
//                System.out.println("  "+om.url+": "+om.oldOids.get(0)+" -> "+om.newOid+" @ "+om.index);
//              }
//            }
          }
        }
      }
    } else  if (found.size() == 1) {
      src = src.replace(found.get(0).oldOids.get(0), found.get(0).newOid);
    }
    return src;
  }

  private OIDMapping findMapping(List<OIDMapping> list, int i, String oid) {
    OIDMapping m = null;
    for (OIDMapping t : list) {
      if (i > t.index && t.oldOids.get(0).equals(oid)) {
        t = m;
      }
    }
    return m == null ? list.get(0) : m;
  }

}
