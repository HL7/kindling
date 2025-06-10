package org.hl7.fhir.tools.publisher;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

public class ConformanceSummaryScanner {

  private XhtmlNode list;
  private List<XhtmlNode> statements = new ArrayList<>();

  public void scan(XhtmlNode doc) {
    scanElement(doc);

    if (list == null) {
      throw new FHIRException("List element not found");
    }
    int i = 0;
    for (XhtmlNode stmt : statements) {
      XhtmlNode li = list.li().id("fcss"+i);
      XhtmlNode ah = li.ah("#fcs"+i);
      ah.tx("§");
      ah.supr(""+i);
      li.tx(" ");
      li.add(stmt.copy());
      ah = stmt.id("fcs"+i).ah("#fcss"+i);
      ah.tx("§");
      ah.supr(""+i);
      i++;
    }
  }

  private void scanElement(XhtmlNode node) {
    if ("ul".equals(node.getName()) && "conf-summary".equals(node.getAttribute("id"))) {
      list = node;
    }
    if ("span".equals(node.getName()) && "fhir-conformance".equals(node.getAttribute("class"))) {
      statements.add(node);
    }
    if (node.hasChildren()) {
      for (XhtmlNode child : node.getChildNodes()) {
        scanElement(child);
      }
    }
  }

}
