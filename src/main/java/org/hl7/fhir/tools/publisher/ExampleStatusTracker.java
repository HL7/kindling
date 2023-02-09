package org.hl7.fhir.tools.publisher;

/**
 * Create tabs for examples on the fly 
 * 
 * @author grahamegrieve
 *
 */
public class ExampleStatusTracker {

  private int id = 0;
  
  public String start() {
    id++;
    return 
      "<div id=\"tabs-ex"+id+"\">\r\n"+
      "<ul>\r\n"+
      "  <li><a href=\"#tabs-ex"+id+"-xml\">XML</a></li>\r\n"+
      "  <li><a href=\"#tabs-ex"+id+"-json\">JSON</a></li>\r\n"+
      "</ul>\r\n"+
      "<div id=\"tabs-ex"+id+"-xml\">\r\n";
  }
  
  public String json() {
    return 
      "</div>\r\n"+
      "<div id=\"tabs-ex"+id+"-json\">\r\n";
  }
  
  public String end() {
    return 
      " </div>\r\n"+
      "</div>\r\n";
  }
  
  public String init() {
    StringBuilder b = new StringBuilder();
    for (int i = 1; i <= id; i++) {
      b.append("$( '#tabs-ex"+i+"' ).tabs({ active: currentTabIndex, activate: function( event, ui ) { store(ui.newTab.index()); } });\r\n");
    }
    return b.toString();
  }
  
  public String change() {
    StringBuilder b = new StringBuilder();
    for (int i = 1; i <= id; i++) {
      b.append("  $( '#tabs-ex"+i+"' ).tabs('option', 'active', currentTab);\r\n");
    }
    return b.toString();
  }

}