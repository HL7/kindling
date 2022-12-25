package org.hl7.fhir.tools.publisher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.utilities.Utilities;

public class ReferenceTracker {

  public class RefDetailsSorter implements Comparator<RefDetails> {

    @Override
    public int compare(RefDetails arg0, RefDetails arg1) {
      return arg0.getText().compareTo(arg1.getText());
    }

  }

  public enum RefType {
    PATTERN_IMPL, INHERITS, RESOURCE_REF, RESOURCE_IMPL, EXTENSION_REF, PROFILE_REF
  }

  public class RefDetails {
    private String id;
    private String ref;
    private String text;
    private List<String> hints;
    public RefDetails(String id, String ref, String text) {
      super();
      this.id = id;
      this.ref = ref;
      this.text = text;
      this.hints = new ArrayList<>();
    }
    public String getId() {
      return id;
    }
    public String getRef() {
      return ref;
    }
    public String getText() {
      return text;
    }
    public List<String> getHints() {
      return hints;
    }    
  }

  private static final int MAX_DEF_SHOW = 6;

  private Map<RefType, List<RefDetails>> list = new HashMap<ReferenceTracker.RefType, List<RefDetails>>();
  
  
  public ReferenceTracker() {
    super();
    for (RefType t : RefType.values()) {
      list.put(t, new ArrayList<>());
    }
  }

  public void link(RefType type, String id, String ref, String text) {
    link(type, id, ref, text, null);
  }

  public void link(RefType type, String id, String ref, String text, String hint) {
    RefDetails rd = null;
    for (RefDetails t : list.get(type)) {
      if (t.getId().equals(id)) {
        rd = t;
      }
    }
    if (rd == null) {
      rd = new RefDetails(id, ref, text);
      list.get(type).add(rd);
    }
    if (hint != null) {
      rd.getHints().add(hint);
    }
  }

  public int count() {
    int c = 0;
    for (RefType t : RefType.values()) {
      c = c + list.get(t).size();
    }
    return c;
  }

  public void clear() {
    for (RefType t : RefType.values()) {
      list.get(t).clear();
    }
  }

  public String render(String title, String name) {
    StringBuilder b = new StringBuilder();
    b.append("<h2>References to this "+title+"</h2>");
    
    if (count() == 0) {
      b.append("<p>No references for this "+title+".</p>");
    } else {
      b.append("<ul>");
      build(RefType.INHERITS, "Inherits From", b, name);
      build(RefType.RESOURCE_IMPL, "Implements", b, name);
      build(RefType.PATTERN_IMPL, "Implemented by", b, name);
      build(RefType.RESOURCE_REF, "Resource References", b, name);
      build(RefType.EXTENSION_REF, "Extension References", b, name);
      build(RefType.PROFILE_REF, "Profile References", b, name);
      b.append("</ul>");
    }
    return b.toString();    
  }

  private void build(RefType key, String title, StringBuilder b, String name) {
    List<RefDetails> l = list.get(key);
    if (l.size() > 0) {
      l.sort(new RefDetailsSorter());
      b.append("<li>"+title+": ");
      int c = 0;
      for (RefDetails rd : l) {
        String t = rd.getHints().size() > 0 ? "title=\""+String.join(",", rd.getHints())+"\" " : "";

        c++;
        if (c == MAX_DEF_SHOW && l.size() > MAX_DEF_SHOW) {
          b.append("<span id=\"rr_"+key+"\" onClick=\"document.getElementById('rr_"+key+"').innerHTML = document.getElementById('rr2_"+key+"').innerHTML\">..."+
              " <span style=\"cursor: pointer; border: 1px grey solid; background-color: #fcdcb3; padding-left: 3px; padding-right: 3px; color: black\">"+
              "Show "+(l.size()-MAX_DEF_SHOW+1)+" more</span></span><span id=\"rr2_"+key+"\" style=\"display: none\">");


        }
        if (c == l.size() && c != 1) {
          b.append(" and ");
        } else if (c > 1) {
          b.append(", ");
        }
        if (rd.getText().equals(name)) {
          b.append("<span "+t+">itself</span>");          
        } else {
          b.append("<a "+t+" href=\""+rd.getRef()+"\">"+Utilities.escapeXml(rd.getText())+"</a>");
        }
      }
      if (c >= MAX_DEF_SHOW && l.size() > MAX_DEF_SHOW) {
        b.append("</span>");
      }
      b.append("</li>");
    }
  }

  public boolean hasLink(RefType type, String id) {
    for (RefDetails t : list.get(type)) {
      if (t.getId().equals(id)) {
        return true;
      }
    }
    return false;
  }

//  public boolean contains(RefType resource, String rn) {
//    // TODO Auto-generated method stub
//    return false;
//  }
//  private String renderRef(String ref, String name) {
//    if (ref.equals(name))
//      return "itself";
//    else
//      return "<a href=\""+definitions.getSrcFile(ref)+".html#"+ref+"\">"+ref+"</a>";
//  }
//
//
//  private String asLinks(ReferenceTracker refs, String name) {
//    StringBuilder b = new StringBuilder();
//    for (int i = 0; i < refs.size(); i++) {
//      if (i == refs.size() - 1)
//        b.append(" and ");
//      else if (i > 0)
//        b.append(", ");
//      b.append(renderRef(refs.get(i), name));
//    }
//    return b.toString();
//  }

}