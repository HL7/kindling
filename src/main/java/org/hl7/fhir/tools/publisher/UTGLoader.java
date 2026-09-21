package org.hl7.fhir.tools.publisher;

import java.io.IOException;

import org.hl7.fhir.convertors.loaders.loaderR5.ILoaderKnowledgeProviderR5;
import org.hl7.fhir.convertors.loaders.loaderRN.ILoaderKnowledgeProviderRN;
import org.hl7.fhir.model.core.Resource;
import org.hl7.fhir.utilities.npm.NpmPackage;

import com.google.gson.JsonSyntaxException;

public class UTGLoader implements ILoaderKnowledgeProviderRN {

  private String version;
  
  public UTGLoader(String version) {
    super();
    this.version = version;
  }

  @Override
  public String getResourcePath(Resource resource) {
    String path = "http://terminology.hl7.org/"+version+"/"+resource.fhirType()+"-"+resource.getId()+".html";
    resource.setUserData("external.url", path);
    return path;
  }

  @Override
  public ILoaderKnowledgeProviderRN forNewPackage(NpmPackage npm) throws JsonSyntaxException, IOException {
    return this;
  }

  @Override
  public String getWebRoot() {
    return "http://terminology.hl7.org/"+version;
  }

}
