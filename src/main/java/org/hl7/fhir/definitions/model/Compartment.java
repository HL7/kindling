package org.hl7.fhir.definitions.model;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class Compartment {

  private String name;
  private String title;
  private String description;
  private String identity;
  private String membership;
  private Map<ResourceDefn, StringTriple> resources = new HashMap<ResourceDefn, StringTriple>();
  
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }
  public Map<ResourceDefn, StringTriple> getResources() {
    return resources;
  }
  public String getIdentity() {
    return identity;
  }
  public void setIdentity(String identity) {
    this.identity = identity;
  }
  public String getMembership() {
    return membership;
  }
  public void setMembership(String membership) {
    this.membership = membership;
  }
  public String getPathForName(String name) {
    for (ResourceDefn r : resources.keySet()) {
      if (r.getName().equals(name)) 
        return resources.get(r).getParameter();
    }
    return "";
  }
  public String getUri() {
    return "http://hl7.org/fhir/compartment/"+getTitle();

  }


  public class StringTriple {
    @Getter private final String parameter;
    @Getter private final String start;
    @Getter private final String end;


    public StringTriple(String parameter, String start, String end) {
      this.parameter = parameter;
      this.start = start;
      this.end = end;
    }
  }
}
