package org.hl7.fhir.definitions.generators.specification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hl7.fhir.r5.elementmodel.Element;
import org.hl7.fhir.r5.model.BackboneElement;
import org.hl7.fhir.r5.model.BackboneType;
import org.hl7.fhir.r5.model.Base;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.DomainResource;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.Property;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.json.model.JsonObject;
import org.hl7.fhir.utilities.json.parser.JsonParser;

public class SDUsageMapper {

  private Set<String> paths = new HashSet<>();
  private Map<String, Map<String, String>> usages = new HashMap<>();

  public void seeResource(String name, String path, Resource resource) {
    if (!paths.contains(path)) {
      paths.add(path);
      process(name, path, resource);
    }
  }

  private void process(String name, String path, Resource resource) {
    if (resource.hasMeta()) {
      for (CanonicalType c : resource.getMeta().getProfile()) {
        see(name, path, c.primitiveValue());
      }
    }
    if (resource instanceof DomainResource) {
      for (Extension ex : ((DomainResource) resource).getExtension()) {
        see(name, path, ex.getUrl());
      }      
      for (Extension ex : ((DomainResource) resource).getModifierExtension()) {
        see(name, path, ex.getUrl());
      }      
    }
    for (Property p : resource.children()) {
      for (Base b : p.getValues()) {
        if (b instanceof Resource) {
          process(name, path, (Resource) b); 
        } else if (b instanceof org.hl7.fhir.r5.model.Element) {
          walkChildren(name, path, (org.hl7.fhir.r5.model.Element) b);
        }
      }
    }
  }

  private void walkChildren(String name, String path, org.hl7.fhir.r5.model.Element element) {
    for (Extension ex : element.getExtension()) {
      see(name, path, ex.getUrl());
    }
    if (element instanceof BackboneElement) {
      for (Extension ex : ((BackboneElement) element).getModifierExtension()) {
        see(name, path, ex.getUrl());
      }  
    }
    if (element instanceof BackboneType) {
      for (Extension ex : ((BackboneType) element).getModifierExtension()) {
        see(name, path, ex.getUrl());
      }  
    }
    for (Property p : element.children()) {
      for (Base b : p.getValues()) {
        if (b instanceof Resource) {
          process(name, path, (Resource) b); 
        } else if (b instanceof org.hl7.fhir.r5.model.Element) {
          walkChildren(name, path, (org.hl7.fhir.r5.model.Element) b);
        }
      }
    }
  }

  private void see(String name, String path, String url) {
    if (Utilities.isAbsoluteUrl(url)) {
      Map<String, String> uses = usages.get(url);
      if (uses == null) {
        uses = new HashMap<>();
        usages.put(url, uses);
      }    
      uses.put(path, name);
    }
  }

  public void seeResource(String name, String path, Element resource) {
    if (!paths.contains(path)) {
      paths.add(path);
      process(name, path, resource);
    }    
  }

  private void process(String name, String path, Element resource) {
    if (resource.hasChild("meta")) {
      for (Element c : resource.getNamedChild("meta").getChildren("profile")) {
        see(name, path, c.getNamedChildValue("url"));
      }
    }
    walkChildren(name, path, resource);
  }

  private void walkChildren(String name, String path, Element element) {
    for (Element ex : element.getChildren("extension")) {
      see(name, path, ex.getNamedChildValue("url"));
    }
    for (Element ex : element.getChildren("modifierExtension")) {
      see(name, path, ex.getNamedChildValue("url"));
    }
    for (Element c : element.getChildren()) {
      if (c.isResource()) {
        process(name, path, c); 
      } else if (!c.isPrimitive()) {
        walkChildren(name, path, c);
      }
    }
  }

  public String asJson() {
    JsonObject json = new JsonObject();
    for (String s : usages.keySet()) {
      JsonObject uses = new JsonObject();
      json.add(s, uses);
      for (Entry<String, String> e : usages.get(s).entrySet()) {
        uses.add(e.getKey(), e.getValue());
      }
    }
    return JsonParser.compose(json, true);
  }
}
