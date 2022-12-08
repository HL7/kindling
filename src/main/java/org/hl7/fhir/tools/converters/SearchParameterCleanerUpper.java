package org.hl7.fhir.tools.converters;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.DomainResource;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.SearchParameter;
import org.hl7.fhir.r5.utils.ToolingExtensions;
import org.hl7.fhir.tools.converters.SearchParameterCleanerUpper.ResourceInfo;
import org.hl7.fhir.utilities.IniFile;
import org.hl7.fhir.utilities.StandardsStatus;
import org.hl7.fhir.utilities.Utilities;

public class SearchParameterCleanerUpper {

  public class ResourceInfo {

    public String sdFilename;
    public Resource sd;
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
    info.sd = new XmlParser().parse(new FileInputStream(info.sdFilename));
    info.bndFilename = Utilities.path(folder, name, "bundle-"+title+"-search-params.xml");
    info.bnd = (Bundle) new XmlParser().parse(new FileInputStream(info.bndFilename));
    return info;
  }


  private void processResource(String rn, ResourceInfo info) throws FileNotFoundException, IOException {
    StandardsStatus rstatus = ToolingExtensions.getStandardsStatus((DomainResource) info.sd);
    int c = 0;
    if (rstatus == StandardsStatus.NORMATIVE) {
      // nothing
    } else if (rstatus == StandardsStatus.TRIAL_USE) {
      c = fixSearchResources(info.bnd, StandardsStatus.TRIAL_USE, StandardsStatus.NORMATIVE);
    } else if (rstatus == StandardsStatus.INFORMATIVE) {
      c = fixSearchResources(info.bnd, StandardsStatus.INFORMATIVE, StandardsStatus.TRIAL_USE, StandardsStatus.NORMATIVE);
    }
    System.out.println(rn+": "+rstatus.toCode()+". "+c+" search parameters fixed");
    if (c > 0) {
      new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(info.bndFilename), info.bnd);
    }
  }


  private int fixSearchResources(Bundle bnd, StandardsStatus value, StandardsStatus... existing) {
    int c = 0;
    for (BundleEntryComponent be : bnd.getEntry()) {
       if (be.getResource() instanceof SearchParameter) {
         SearchParameter sp = (SearchParameter) be.getResource();
         StandardsStatus spstatus = ToolingExtensions.getStandardsStatus(sp);
         if (spstatus != null) {
           boolean inlist = true;
           for (StandardsStatus s : existing) {
             inlist = inlist && spstatus == s;
           }
           if (inlist) {
            c++;
            ToolingExtensions.setStandardsStatus(sp, value, null);
           }
         }
       }
    }
    return c;
  }
  
}
