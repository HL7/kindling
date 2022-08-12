package org.hl7.fhir.tools.publisher;

import java.io.IOException;

import org.hl7.fhir.convertors.loaders.loaderR5.ILoaderKnowledgeProviderR5;
import org.hl7.fhir.r5.model.CanonicalResource;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.utilities.npm.NpmPackage;

import com.google.gson.JsonSyntaxException;

public class DICOMLoader implements ILoaderKnowledgeProviderR5 {

  private String version;
  
  public DICOMLoader(String version) {
    super();
    this.version = "current".equals(version) ? "1.0.0" : version;
  }

  @Override
  public String getResourcePath(Resource resource) {
    if (resource instanceof CanonicalResource) {
      resource.setUserData("external.url", ((CanonicalResource) resource).getUrl());
      return ((CanonicalResource) resource).getUrl();
    }
    String path = "http://unknown.org/dicom/"+resource.fhirType()+"/"+resource.getId();
    return path;
  }

  @Override
  public ILoaderKnowledgeProviderR5 forNewPackage(NpmPackage npm) throws JsonSyntaxException, IOException {
    return this;
  }

  @Override
  public String getWebRoot() {
    return "https://dicom.nema.org/medical/dicom/current/output/chtml/part16/chapter_Foreword.html";
  }

}
