package org.hl7.fhir.tools.converters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.utilities.CommaSeparatedStringBuilder;
import org.hl7.fhir.utilities.FileUtilities;
import org.hl7.fhir.utilities.Utilities;

public class DataTypeCrossLinkGenerator {

  public static void main(String[] args) throws FileNotFoundException, IOException {
    new DataTypeCrossLinkGenerator().generate(new File(args[0]), new File(args[1]));
  }

  private void generate(File core, File ig) throws FileNotFoundException, IOException {
    System.out.println("Process "+core.getAbsolutePath());
    for (File f : core.listFiles()) {
      if (f.getName().endsWith(".html")) {
        String source = FileUtilities.fileToString(f);
        List<String> lines = new ArrayList<>();
        for (String s : FileUtilities.fileToString(f).split("\\r?\\n|\\r")) {
          lines.add(s);
        };        
        if (scanLinesCore(f.getName().replace(".html", ""), lines)) {
          System.out.println("- saved "+f.getName());
          source = String.join("\r\n", lines);
          FileUtilities.stringToFile(source, f);
        }
      }
    }
    File res = new File(Utilities.path(core.getAbsolutePath(), "resource"));
    System.out.println("Process "+res.getAbsolutePath());
    for (File f : res.listFiles()) {
      if (f.getName().endsWith(".xml")) {
        String source = FileUtilities.fileToString(f);
        List<String> lines = new ArrayList<>();
        for (String s : FileUtilities.fileToString(f).split("\\r?\\n|\\r")) {
          lines.add(s);
        };        
        if (scanLinesCore(f.getName().replace(".html", ""), lines)) {
          System.out.println("- saved "+f.getName());
          source = String.join("\r\n", lines);
          FileUtilities.stringToFile(source, f);
        }
      }
    }
    System.out.println("Process "+ig.getAbsolutePath());
    for (File f : ig.listFiles()) {
      if (f.getName().endsWith(".xml")) {
        String source = FileUtilities.fileToString(f);
        List<String> lines = new ArrayList<>();
        for (String s : FileUtilities.fileToString(f).split("\\r?\\n|\\r")) {
          lines.add(s);
        };        
        if (scanLinesIG(f.getName().replace(".xml", ""), lines)) {
          source = String.join("\r\n", lines);
          FileUtilities.stringToFile(source, f);
          System.out.println(" - saved "+f.getName());
        } 
      }
    }
  }

  private boolean scanLinesCore(String name, List<String> lines) {
    String base = name.contains("-") ? name.substring(0, name.indexOf("-")) : name;
    boolean changed = false;
    for (int i = 0; i < lines.size(); i++) {
      String s = lines.get(i);
      if (s.startsWith("<!--xlp:")) {
        changed = true;
        String t = s.substring(0, s.indexOf("-->"));
        String type = t.substring(8);
        CommaSeparatedStringBuilder b = new CommaSeparatedStringBuilder(", ", " and ");
        if (name.contains("-")) {
          b.append("<a no-external=\"true\" href=\""+base+".html#"+type+"\">Base Definition</a>");
        }
        if (!name.contains("-examples") && !Utilities.existsInList(base, "types", "references", "resource")) {
          b.append("<a no-external=\"true\" href=\""+base+"-examples.html#"+type+"\">Examples</a>");          
        }
        if (!name.contains("-definitions")) {
          b.append("<a no-external=\"true\" href=\""+base+"-definitions.html#"+type+"\">Detailed Descriptions</a>");          
        }
        if (!name.contains("-mappings") && !Utilities.existsInList(base, "references")) {
          b.append("<a no-external=\"true\" href=\""+base+"-mappings.html#"+type+"\">Mappings</a>");          
        }
        if (!name.contains("-profiles") && !Utilities.existsInList(base, "narrative")) {
          b.append("<a no-external=\"true\" href=\""+base+"-profiles.html#"+type+"\">Profiles</a>");          
        }
        if (!name.contains("-extensions") && !Utilities.existsInList(base, "narrative")) {
          b.append("<a no-external=\"true\" href=\"[%extensions-location%]extensions-"+base+".html#"+type+"\">Extensions</a>");          
        }
        lines.set(i, "<!--xlp:"+type+"-->See also "+b.toString());
      }
    }
    return changed;
  }
  
  private boolean scanLinesIG(String name, List<String> lines) {
    String base = name.contains("-") ? name.substring(name.indexOf("-")+1) : name;
    boolean changed = false;
    for (int i = 0; i < lines.size(); i++) {
      String s = lines.get(i);
      if (s.startsWith("<!--xlp:")) {
        changed = true;
        String t = s.substring(0, s.indexOf("-->"));
        String type = t.substring(8);
        CommaSeparatedStringBuilder b = new CommaSeparatedStringBuilder(", ", " and ");
        if (name.contains("-")) {
          b.append("<a no-external=\"true\" href=\"{{site.data.fhir.path}}"+base+".html#"+type+"\">Base Definition</a>");
        }
        if (!name.contains("-examples") && !Utilities.existsInList(base, "types", "references")) {
          b.append("<a no-external=\"true\" href=\"{{site.data.fhir.path}}"+base+"-examples.html#"+type+"\">Examples</a>");          
        }
        if (!name.contains("-definitions")) {
          b.append("<a no-external=\"true\" href=\"{{site.data.fhir.path}}"+base+"-definitions.html#"+type+"\">Detailed Descriptions</a>");          
        }
        if (!name.contains("-mappings") && !Utilities.existsInList(base, "references")) {
          b.append("<a no-external=\"true\" href=\"{{site.data.fhir.path}}"+base+"-mappings.html#"+type+"\">Mappings</a>");          
        }
        if (!name.contains("-profiles") && !Utilities.existsInList(base, "narrative")) {
          b.append("<a no-external=\"true\" href=\"{{site.data.fhir.path}}"+base+"-profiles.html#"+type+"\">Profiles</a>");          
        }
        if (!name.contains("-extensions") && !Utilities.existsInList(base, "narrative")) {
          b.append("<a no-external=\"true\" href=\"extensions-"+base+".html#"+type+"\">Extensions</a>");          
        }
        lines.set(i, "<!--xlp:"+type+"-->See also "+b.toString());
      }
    }
    return changed;
  }
}
