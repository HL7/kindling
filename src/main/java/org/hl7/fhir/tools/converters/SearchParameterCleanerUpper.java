package org.hl7.fhir.tools.converters;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r5.extensions.ExtensionUtilities;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.DomainResource;
import org.hl7.fhir.r5.model.ElementDefinition;
import org.hl7.fhir.r5.model.ElementDefinition.TypeRefComponent;
import org.hl7.fhir.r5.model.Enumerations.SearchParamType;
import org.hl7.fhir.r5.model.SearchParameter;
import org.hl7.fhir.r5.model.SearchParameter.SearchProcessingModeType;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.extensions.ExtensionDefinitions;
import org.hl7.fhir.utilities.CommaSeparatedStringBuilder;
import org.hl7.fhir.utilities.IniFile;
import org.hl7.fhir.utilities.StandardsStatus;
import org.hl7.fhir.utilities.Utilities;

public class SearchParameterCleanerUpper {

  public class ResourceInfo {

    public String sdFilename;
    public StructureDefinition sd;
    public String bndFilename;
    public Bundle bnd;

  }

  public static void main(String[] args) throws IOException {
   new SearchParameterCleanerUpper().clean("/Users/grahamegrieve/work/r5/source");
  }

  private Map<String, ResourceInfo> resources = new HashMap<>();
  
  private void clean(String folder) throws IOException {
    IniFile ini = new IniFile(Utilities.path(folder, "fhir.ini"));
    for (String s : ini.getPropertyNames("resources")) {
      resources.put(s, loadResource(folder, s, ini.getStringProperty("resources", s)));
    }
    for (String s : resources.keySet()) {
      processResource(s, resources.get(s));
    }
    Bundle csp = (Bundle) new XmlParser().parse(new FileInputStream(Utilities.path(folder, "searchparameter", "common-search-parameters.xml")));
    for (BundleEntryComponent be : csp.getEntry()) {
      processCommonSearchParameter((SearchParameter) be.getResource());
    }
  }


  private void processCommonSearchParameter(SearchParameter sp) {
    
    
  }


  private ResourceInfo loadResource(String folder, String name, String title) throws IOException {
    ResourceInfo info = new ResourceInfo();
    info.sdFilename = Utilities.path(folder, name, "structuredefinition-"+title+".xml");
    info.sd = (StructureDefinition) new XmlParser().parse(new FileInputStream(info.sdFilename));
    info.bndFilename = Utilities.path(folder, name, "bundle-"+title+"-search-params.xml");
    info.bnd = (Bundle) new XmlParser().parse(new FileInputStream(info.bndFilename));
    return info;
  }


  private void processResource(String rn, ResourceInfo info) throws FileNotFoundException, IOException {
    StandardsStatus rstatus = ExtensionUtilities.getStandardsStatus((DomainResource) info.sd);
    int c = 0;
    // first pass: lower everything to the status of the resource 
    if (rstatus == StandardsStatus.NORMATIVE) {
      // nothing
    } else if (rstatus == StandardsStatus.TRIAL_USE) {
      c = fixSearchResources(info.bnd, StandardsStatus.TRIAL_USE, StandardsStatus.NORMATIVE);
    } else if (rstatus == StandardsStatus.INFORMATIVE) {
      c = fixSearchResources(info.bnd, StandardsStatus.INFORMATIVE, StandardsStatus.TRIAL_USE, StandardsStatus.NORMATIVE);
    }
    // second pass: lift everything to the status of the resource/field
    c = c + liftSearchResources(info.bnd, rstatus, info.sd);
    c = c + fixSearchResourcesTypeFilters(info.bnd, rstatus, info.sd);
    
    System.out.println(rn+": "+rstatus.toCode()+". "+c+" search parameters fixed");
    if (c > 0) {
      new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(info.bndFilename), info.bnd);
    }
  }


  private int fixSearchResourcesTypeFilters(Bundle bnd, StandardsStatus rstatus, StructureDefinition sd) {
    int c = 0;
    for (BundleEntryComponent be : bnd.getEntry()) {
      if (be.getResource() instanceof SearchParameter) {
        SearchParameter sp = (SearchParameter) be.getResource();
        ElementDefinition ed = getED(sd, sp.getExpression());
        if (ed != null && ed.getType().size() > 1) {
          if (ed.getPath().equals(sp.getExpression()+"[x]")) {
            Set<String> types = new HashSet<>();
            for (TypeRefComponent tr : ed.getType()) {
              boolean ok = isCompatible(tr.getWorkingCode(), sp.getType());
              if (ok) {
                types.add(tr.getWorkingCode());
              }
            }
            if (types.size() == 0) {
              System.out.println(" !!! ILLEGAL SEARCH PARAMTER "+sp.getId()+": type = "+sp.getType().toCode()+" and types = "+ed.typeSummary());
            } else {
              CommaSeparatedStringBuilder b = new CommaSeparatedStringBuilder(" | ");
              for (String t : types) {
                b.append(sp.getExpression()+".as("+t+")");
              }
              sp.setExpression(b.toString());
              sp.setProcessingMode(SearchProcessingModeType.NORMAL);
              c++;
            }
          } else {
            System.out.println(" !!! UNPROCESSIBLE SEARCH PARAMTER "+sp.getId()+": type = "+sp.getType().toCode()+" and expression = "+sp.getExpression());            
          }
        }
      }
    }
    return c;
  }


  private boolean isCompatible(String t, SearchParamType type) {
    switch (type) {
    case COMPOSITE: 
      return false;
    case DATE:
      return Utilities.existsInList(t, "date", "dateTime", "instant", "Period", "Timing");
    case NUMBER:
      return Utilities.existsInList(t, "integer", "decimal", "positiveInt", "UnsignedInt");
    case QUANTITY:
      return Utilities.existsInList(t, "Quantity", "Age", "Duration", "Money", "Range");
    case REFERENCE:
      return Utilities.existsInList(t, "canonical", "uri", "Reference");
    case SPECIAL:
      return false; // Utilities.existsInList(t, "Quantity");
    case STRING:
      return Utilities.existsInList(t, "string", "Address", "HumanName");
    case TOKEN:
      return Utilities.existsInList(t, "booolean", "canonical", "code", "id", "Coding", "CodeableConcept", "Quantity", "ContactPoint", "Identifier");
    case URI:
      return Utilities.existsInList(t, "canonical", "oid", "url", "uri", "uuid");
    default:
      return false;    
    }
  }


  private int liftSearchResources(Bundle bnd, StandardsStatus rstatus, StructureDefinition sd) {
    int c = 0;
    for (BundleEntryComponent be : bnd.getEntry()) {
      if (be.getResource() instanceof SearchParameter) {
        SearchParameter sp = (SearchParameter) be.getResource();
        ElementDefinition ed = getED(sd, sp.getExpression());
        StandardsStatus spstatus = ExtensionUtilities.getStandardsStatus(sp);
        StandardsStatus nstatus = ExtensionUtilities.getStandardsStatus(ed);
        nstatus = nstatus == null ? rstatus : nstatus;
        if (nstatus != null && nstatus != spstatus) {
          c++;
          ExtensionUtilities.setStandardsStatus(sp, nstatus, null);          
        }
      }
    }
    return c;
  }


  private ElementDefinition getED(StructureDefinition sd, String expression) {
    if (expression == null) {
      return null;
    }
    for (ElementDefinition ed : sd.getDifferential().getElement()) {
      if (expression.equals(ed.getPath()) || (expression+"[x]").equals(ed.getPath())) {
        return ed;
      }
    }
    if (expression.contains(".")) {
      return getED(sd, expression.substring(0, expression.lastIndexOf(".")));
    } else {
      return null;
    }
  }


  private int fixSearchResources(Bundle bnd, StandardsStatus value, StandardsStatus... existing) {
    int c = 0;
    for (BundleEntryComponent be : bnd.getEntry()) {
       if (be.getResource() instanceof SearchParameter) {
         SearchParameter sp = (SearchParameter) be.getResource();
         StandardsStatus spstatus = ExtensionUtilities.getStandardsStatus(sp);
         if (spstatus != null) {
           boolean inlist = true;
           for (StandardsStatus s : existing) {
             inlist = inlist && spstatus == s;
           }
           if (inlist) {
            c++;
            ExtensionUtilities.setStandardsStatus(sp, value, null);
           }
         }
       }
    }
    return c;
  }
  
}
