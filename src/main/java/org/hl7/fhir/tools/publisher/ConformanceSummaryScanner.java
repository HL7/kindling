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
      XhtmlNode li = list.li();
      li.ah("#fcs"+i).tx("§");
      li.tx(" ");
      li.add(stmt);
      stmt.an("fcs"+i).style("display: none").tx(" ");
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
