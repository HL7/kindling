package org.hl7.fhir.tools.converters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.utilities.TextFile;

public class JsonExampleFixer {

  public static void main(String[] args) throws FileNotFoundException, IOException {
    new JsonExampleFixer().process(new File(args[0]));
  }

  private void process(File file) throws FileNotFoundException, IOException {
    List<String> lines = new ArrayList<>();
    for (String s : TextFile.fileToString(file).split("\\r?\\n|\\r")) {
      lines.add(s);
    }
    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).trim().startsWith("<pre")) {
        lines.set(i, lines.get(i).trim());
      }
      if (lines.get(i).trim().startsWith("</pre>")) {
        lines.set(i, lines.get(i).trim());
      }
    }

    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).startsWith("<pre class=\"xml\"")) {
        List<String> pre = new ArrayList<>();
        pre.add(lines.get(i).replace("xml", "json"));
        for (int j = i+1; j < lines.size(); j++) {
          pre.add(lines.get(j));
          if (lines.get(j).trim().startsWith("</pre>")) {
            break;
          }
        }
        lines.addAll(i+pre.size(), pre);
      }
    }

    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).startsWith("<pre class=\"json\"")) {
        for (int j = i; j < lines.size(); j++) {
          if (lines.get(j).trim().startsWith("</pre>")) {
            break;
          }
          lines.set(j, lines.get(j).replace("&lt;", "\"").replace(" /&gt;", "").replace("&quot;", "\"")
              .replace("/&gt;", "").replace(" value=", "\" : ").replace("&gt;", "\" : {").replace("\"extension url=\"", "\"extension\" : { \"url\" : \""));
          if (lines.get(j).trim().startsWith("\"/")) {
            String s = "";
            for (char c : lines.get(j).toCharArray()) {
              if (c == ' ') {
                s = s + " ";
              }
            }
            if (s.length() > 2) {
              lines.set(j, s.substring(2) + "}");
            } else {
              lines.set(j, s + "}");
            }
          } 
        }
        for (int j = i+1; j < lines.size(); j++) {
          if (lines.get(j).trim().startsWith("</pre>")) {
            break;
          }
          if (!lines.get(j).trim().endsWith("{") && !lines.get(j+1).trim().startsWith("</pre") && !lines.get(j).trim().endsWith(",") && !lines.get(j+1).trim().startsWith("}")) {
            lines.set(j, lines.get(j)+",");
          }
        }
      }
    }
    String s = String.join("\r\n", lines);
    TextFile.stringToFile(s, file.getAbsolutePath());
  }

}
