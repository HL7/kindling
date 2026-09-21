package org.hl7.fhir.tools.publisher;

import java.io.IOException;

import org.hl7.fhir.convertors.loaders.loaderR5.ILoaderKnowledgeProviderR5;
import org.hl7.fhir.convertors.loaders.loaderRN.ILoaderKnowledgeProviderRN;
import org.hl7.fhir.model.core.Resource;
import org.hl7.fhir.utilities.npm.NpmPackage;

import com.google.gson.JsonSyntaxException;

public class ExtensionsLoader implements ILoaderKnowledgeProviderRN {

  private String version;
  private String path;
  
  public ExtensionsLoader(String version, String path) {
    super();
    this.version = version;
    this.path = path;
  }

  @Override
  public String getResourcePath(Resource resource) {
    String spath = path+resource.fhirType()+"-"+resource.getId()+".html";
    resource.setUserData("external.url", spath);
    return spath;
  }

  @Override
  public ILoaderKnowledgeProviderRN forNewPackage(NpmPackage npm) throws JsonSyntaxException, IOException {
    return this;
  }

  @Override
  public String getWebRoot() {
    return path;
  }

}
