package org.hl7.fhir.definitions.model;

import lombok.Getter;

public class ParameterisedType {
  @Getter private String name;
  @Getter private String profile;

  public ParameterisedType(String p) {
    if (p.contains("{")) {
      int startPos = p.indexOf("{");
      int endPos = p.indexOf("}");
      profile = p.substring(startPos + 1, endPos).trim();
      name = p.substring(0, startPos);
    } else {
      name = p;
    }
  }
}
