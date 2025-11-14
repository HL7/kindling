package org.hl7.fhir.tools.converters;

import org.hl7.fhir.r5.extensions.ExtensionUtilities;
import org.hl7.fhir.r5.formats.IParser;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.utilities.DebugUtilities;
import org.hl7.fhir.utilities.StandardsStatus;
import org.hl7.fhir.utilities.Utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TerminologyStatusRefresher {

  public static class LoadedResource {
    private File source;
    private CanonicalResource resource;
    private boolean example;
    private boolean real;
    private boolean json;

    public LoadedResource(File f, CanonicalResource r, boolean json) {
      source = f;
      resource = r;
      this.json = json;
    }

    public void see(boolean example) {
      if (example) {
        this.example = true;
      } else {
        this.real = true;
      }

    }
  }

  public static void main(String[] args) throws IOException {
    new TerminologyStatusRefresher().process(new File(args[0]));
  }

  private void process(File src) throws IOException {
    Map<String, LoadedResource> resources = new HashMap<>();
    // load all the terminology resources
    loadResources(resources, new File(Utilities.path(src, "source")));
    System.out.println(resources.size() + " resources loaded");

    // iterate all the structure definitions
    iterateTypes(resources, new File(Utilities.path(src, "publish")));
    int none = 0;
    int both = 0;
    int ex = 0;
    int real = 0;
    for (LoadedResource resource : resources.values()) {
      if (resource.example && resource.real) {
        both++;
      } else if (resource.example) {
        ex++;
      } else if (resource.real) {
        real++;
      } else {
        none++;
      }
    }
    for (LoadedResource resource : resources.values()) {
      if (resource.example && !resource.real) {
        ExtensionUtilities.setStandardsStatus((DomainResource) resource.resource, StandardsStatus.INFORMATIVE, null);
//        resource.resource.setExperimental(true);
        resource.resource.setStatus(Enumerations.PublicationStatus.ACTIVE);
        if (resource.json) {
          new JsonParser().setOutputStyle(IParser.OutputStyle.PRETTY).compose(new FileOutputStream(resource.source), resource.resource);
        } else {
          new XmlParser().setOutputStyle(IParser.OutputStyle.PRETTY).compose(new FileOutputStream(resource.source), resource.resource);
        }
      }
      if (resource.real) {
        ExtensionUtilities.setStandardsStatus((DomainResource) resource.resource, StandardsStatus.NORMATIVE, null);
//        resource.resource.setExperimental(false);
        resource.resource.setStatus(Enumerations.PublicationStatus.ACTIVE);
        if (resource.json) {
          new JsonParser().setOutputStyle(IParser.OutputStyle.PRETTY).compose(new FileOutputStream(resource.source), resource.resource);
        } else {
          new XmlParser().setOutputStyle(IParser.OutputStyle.PRETTY).compose(new FileOutputStream(resource.source), resource.resource);
        }
      }
    }
    System.out.println("None: "+none + ", Both: " + both + ", Example: " + ex + ", Real: " + real);
  }

  private void iterateTypes(Map<String, LoadedResource> resources, File publish) {
    for (File f : publish.listFiles()) {
      if (f.getName().endsWith(".json")) {
        checkType(resources, f);
      }
    }
  }

  private void checkType(Map<String, LoadedResource> resources, File f) {
    Resource r = null;
    try {
      r = new JsonParser().parse(new FileInputStream(f));
    } catch (Exception e) {
//      System.out.println("Error parsing " + f.getAbsolutePath());
    }
    if (r != null && r instanceof StructureDefinition) {
      StructureDefinition sd = (StructureDefinition) r;
      checkType(resources, sd);
    }

  }

  private void checkType(Map<String, LoadedResource> resources, StructureDefinition sd) {
    for (ElementDefinition ed : sd.getSnapshot().getElement()) {
      if (ed.hasBinding()) {
        boolean example = ed.getBinding().getStrength() == Enumerations.BindingStrength.EXAMPLE;
        String vs = ed.getBinding().getValueSet();
        if (vs != null) {
          if (vs.contains("|")) {
            vs = vs.substring(0, vs.indexOf("|"));
          }
          LoadedResource res = resources.get(vs);
          if (res != null) {
            checkVS(resources, res, example);
          } else {
            // System.out.println(vs);
          }
        }
      }
    }
  }

  private void checkVS(Map<String, LoadedResource> resources, LoadedResource res, boolean example) {
    res.see(example);
    ValueSet vs = (ValueSet) res.resource;
    for (ValueSet.ConceptSetComponent inc : vs.getCompose().getInclude()) {
      LoadedResource cres = resources.get(inc.getSystem());
      if (cres != null) {
        cres.see(example);
      }
      for (CanonicalType ct : inc.getValueSet()) {
        LoadedResource vres = resources.get(ct.primitiveValue());
        if (vres != null) {
          checkVS(resources, vres, example);
        }
      }
    }
  }

  private void loadResources(Map<String, LoadedResource> resources, File dir) {
    for (File f : dir.listFiles()) {
      if (f.isDirectory()) {
        loadResources(resources, f);
      } else if (f.getName().endsWith(".xml")) {
        loadResourceXML(resources, f);
      } else if (f.getName().endsWith(".json")) {
        loadResourceJSON(resources, f);
      }
    }
  }

  private void loadResourceXML(Map<String, LoadedResource> resources, File f) {
    try {
      Resource r = new XmlParser().parse(new FileInputStream(f));
      if (r instanceof ValueSet || r instanceof CodeSystem) {
        resources.put(((CanonicalResource) r).getUrl(), new LoadedResource(f, (CanonicalResource) r, false));
      }

    } catch (Exception e) {
      // nothing
    }
  }


  private void loadResourceJSON(Map<String, LoadedResource> resources, File f) {
    try {
      Resource r = new JsonParser().parse(new FileInputStream(f));
      if (r instanceof ValueSet || r instanceof CodeSystem) {
        resources.put(((CanonicalResource) r).getUrl(), new LoadedResource(f, (CanonicalResource) r, true));
      }

    } catch (Exception e) {
      // nothing
    }
  }

}
