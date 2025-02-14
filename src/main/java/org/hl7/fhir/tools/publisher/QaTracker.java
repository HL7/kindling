package org.hl7.fhir.tools.publisher;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.definitions.model.Definitions;
import org.hl7.fhir.definitions.model.ElementDefn;
import org.hl7.fhir.definitions.model.Example;
import org.hl7.fhir.definitions.model.ResourceDefn;
import org.hl7.fhir.utilities.FileUtilities;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.XsltUtilities;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;

public class QaTracker {

  private class SnapShot {
    private int resources; 
    private int types; 
    private int packs; 
    private int paths;
    private int valuesets;
    
    private int hints;
    private int warnings;
    private int errors;
  }
  
  
  private SnapShot current = new SnapShot();
  private Map<Date, SnapShot> records = new HashMap<Date, SnapShot>();
  private Definitions definitions;
  
  public void countDefinitions(Definitions definitions) throws Exception {
    this.definitions = definitions;
    current.resources = definitions.getResources().size();
    current.types = definitions.getResources().size() + definitions.getTypes().size()   
         + definitions.getShared().size() + definitions.getPrimitives().size()+ definitions.getInfrastructure().size();
    current.packs = definitions.getPackList().size();
    
    for (ResourceDefn r : definitions.getResources().values())
      countPaths(r.getRoot());
    for (ElementDefn e : definitions.getTypes().values())
      countPaths(e);
    for (String e : definitions.getShared())
      countPaths(definitions.getElementDefn(e));
    for (ElementDefn e : definitions.getInfrastructure().values())
      countPaths(e);
    
    current.valuesets = definitions.getValueSetCount();
  }

  private void countPaths(ElementDefn e) {
    current.paths++;
    for (ElementDefn c : e.getElements())
      countPaths(c);
  }

  public String report(PageProcessor page, List<ValidationMessage> errors) throws Exception {
    StringBuilder s = new StringBuilder();
    s.append("<h2>Build Stats</h2>\r\n");
    s.append("<table class=\"grid\">\r\n");
    s.append(" <tr><td>resources</td><td>"+Integer.toString(current.resources)+"</td></tr>\r\n");
    s.append(" <tr><td>types</td><td>"+Integer.toString(current.types)+"</td></tr>\r\n");
    s.append(" <tr><td>packs</td><td>"+Integer.toString(current.packs)+"</td></tr>\r\n");
    s.append(" <tr><td>paths</td><td>"+Integer.toString(current.paths)+"</td></tr>\r\n");
    s.append(" <tr><td>valuesets</td><td>"+Integer.toString(current.valuesets)+"</td></tr>\r\n");
    s.append(" <tr><td>errors</td><td>"+Integer.toString(current.errors)+"</td></tr>\r\n");
    s.append(" <tr><td>warnings</td><td>"+Integer.toString(current.warnings)+"</td></tr>\r\n");
    s.append(" <tr><td>information messages</td><td>"+Integer.toString(current.hints)+"</td></tr>\r\n");
    s.append("</table>\r\n");
    
    addExampleErrors(s, errors);
    
    String xslt = Utilities.path(page.getFolders().rootDir, "implementations", "xmltools", "WarningsToQA.xslt");
    s.append(XsltUtilities.saxonTransform(page.getFolders().dstDir + "work-group-warnings.xml", xslt));
    
    return s.toString(); 
  }
  
  private void addExampleErrors(StringBuilder s, List<ValidationMessage> errors) {
    s.append("<h2>Errors in Examples</h2>\r\n");
    s.append("<table border=\"1\" cellspacing=\"1\">\r\n");
    s.append(" <tr>\r\n");
    s.append("  <td>Path</td>\r\n");
    s.append("  <td>Type</td>\r\n");
    s.append("  <td>Message</td>\r\n");
    s.append(" </tr>\r\n");
    for (String n: definitions.sortedResourceNames()) {
      ResourceDefn rd = definitions.getResourceByName(n);
      int e = 0;
      for (Example ex : rd.getExamples()) {
        for (ValidationMessage vm : ex.getErrors()) {
          if (vm.getLevel() == IssueSeverity.ERROR) {
            e++;
          }
        }
      }
      if (e == 0) {
      } else {
        s.append(" <tr>\r\n");
        s.append("  <td colspan=\"3\"><b>"+n+"</b></td>\r\n");
        s.append(" </tr>\r\n");
        for (Example ex : rd.getExamples()) {
          e = 0;
          for (ValidationMessage vm : ex.getErrors()) {
            if (vm.getLevel() == IssueSeverity.ERROR) {
              e++;
            }
          }
          if (e > 0) {
            s.append(" <tr>\r\n");
            s.append("  <td colspan=\"3\">"+ex.getId()+" ("+ex.getTitle()+")</td>\r\n");
            s.append(" </tr>\r\n");
            for (ValidationMessage vm : ex.getErrors()) {
              if (vm.getLevel() == IssueSeverity.ERROR) {
                s.append(" <tr>\r\n");
                s.append("  <td>"+vm.getLocation()+"</td>\r\n");
                s.append("  <td>"+vm.getType().toString()+"</td>\r\n");
                s.append("  <td>"+Utilities.escapeXml(vm.getMessage())+"</td>\r\n");
                s.append(" </tr>\r\n");                
              }
            }
          }
        }
      }
    }
    s.append("</table>\r\n");            
  }

  public void commit(String rootDir) throws IOException {
    String src = FileUtilities.fileToString(rootDir+"records.csv");
    
    Calendar c = new GregorianCalendar();
    c.set(Calendar.HOUR_OF_DAY, 0); //anything 0 - 23
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    Date d = c.getTime();
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    StringBuilder b = new StringBuilder();
    b.append(sdf.format(d));
    b.append(",");
    b.append(current.resources);   
    b.append(",");
    b.append(current.types);   
    b.append(",");
    b.append(current.packs); // need to maintain the old word here   
    b.append(",");
    b.append(current.paths);   
    b.append(",");
    // bindings
    b.append(",");
    // code lists
    b.append(",");
    b.append(current.valuesets);   
    b.append(",");
    // codes
    b.append(",");
    b.append(current.hints);   
    b.append(",");
    b.append(current.warnings);   
    b.append(",");
    // uncovered
    b.append(",");
    // broken links
    FileUtilities.stringToFile(src+"\r\n"+b.toString(), rootDir+"records.csv");
  }

  public void setCounts(int e, int w, int i) {
    current.errors = e;
    current.hints = i;
    current.warnings = w;
  }

}
