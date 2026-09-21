package org.hl7.fhir.tools.publisher;

import java.io.IOException;

import org.hl7.fhir.convertors.loaders.loaderR5.ILoaderKnowledgeProviderR5;
import org.hl7.fhir.convertors.loaders.loaderRN.ILoaderKnowledgeProviderRN;
import org.hl7.fhir.model.core.Resource;
import org.hl7.fhir.utilities.npm.NpmPackage;

import com.google.gson.JsonSyntaxException;

public class IHELoader implements ILoaderKnowledgeProviderRN {

  @Override
  public String getResourcePath(Resource resource) {
    if (resource.getId().equals("formatcode")) {
      resource.setUserData("external.url", "https://profiles.ihe.net/fhir/ihe.formatcode.fhir/CodeSystem-formatcode.html");
    } else {
      resource.setUserData("external.url", "something-ihe-"+resource.getId());
    }
    return resource.getUserString("external.url");
  }

  @Override
  public ILoaderKnowledgeProviderRN forNewPackage(NpmPackage npm) throws JsonSyntaxException, IOException {
    return null;
  }

  @Override
  public String getWebRoot() {
    return "https://profiles.ihe.net/fhir/ihe.formatcode.fhir/index.html";
  }

}
