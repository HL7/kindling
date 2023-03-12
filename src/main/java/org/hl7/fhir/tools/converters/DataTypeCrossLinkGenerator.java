package org.hl7.fhir.tools.converters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.utilities.CommaSeparatedStringBuilder;
import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.Utilities;

public class DataTypeCrossLinkGenerator {

  public static void main(String[] args) throws FileNotFoundException, IOException {
    new DataTypeCrossLinkGenerator().generate(new File(args[0]));
  }

  private void generate(File file) throws FileNotFoundException, IOException {
    for (File f : file.listFiles()) {
      if (f.getName().endsWith(".html")) {
        System.out.print(f.getName());
        String source = TextFile.fileToString(f);
        List<String> lines = new ArrayList<>();
        for (String s : TextFile.fileToString(f).split("\\r?\\n|\\r")) {
          lines.add(s);
        };        
        if (scanLines(f.getName().replace(".html", ""), lines)) {
          System.out.println("... saved");
          source = String.join("\r\n", lines);
          TextFile.stringToFile(source, f);
        } else {
          System.out.println("... no change");
        }
      }
    }
  }

  private boolean scanLines(String name, List<String> lines) {
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
          b.append("<a href=\""+base+".html#"+type+"\">Base Definition</a>");
        }
        if (!name.contains("-examples") && !Utilities.existsInList(base, "types")) {
          b.append("<a href=\""+base+"-examples.html#"+type+"\">Examples</a>");          
        }
        if (!name.contains("-definitions")) {
          b.append("<a href=\""+base+"-definitions.html#"+type+"\">Detailed Descriptions</a>");          
        }
        if (!name.contains("-mappings")) {
          b.append("<a href=\""+base+"-mappings.html#"+type+"\">Mappings</a>");          
        }
        if (!name.contains("-profiles") && !Utilities.existsInList(base, "narrative")) {
          b.append("<a href=\""+base+"-profiles.html#"+type+"\">Profiles</a>");          
        }
        if (!name.contains("-extensions") && !Utilities.existsInList(base, "narrative")) {
          b.append("<a href=\""+base+"-extensions.html#"+type+"\">Extensions</a>");          
        }
        if (!name.contains("-version-maps")) {
          b.append("<a href=\""+base+"-version-maps.html#"+type+"\">R4 Conversions</a>");          
        }
        lines.set(i, "<!--xlp:"+type+"-->See also "+b.toString());
      }
    }
    return changed;
  }
}
