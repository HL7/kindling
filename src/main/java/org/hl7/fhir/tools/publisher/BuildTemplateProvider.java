package org.hl7.fhir.tools.publisher;

import org.hl7.fhir.definitions.model.Definitions;
import org.hl7.fhir.definitions.model.ResourceDefn;
import org.hl7.fhir.model.core.DomainResource;
import org.hl7.fhir.services.renderers.utils.RenderingContext;
import org.hl7.fhir.services.renderers.utils.RenderingContext.ILiquidTemplateProvider;

public class BuildTemplateProvider implements ILiquidTemplateProvider {

  private Definitions definitions;

  public BuildTemplateProvider(Definitions definitions) {
    this.definitions = definitions;
  }

  @Override
  public String findTemplate(RenderingContext rcontext, DomainResource r) {
    return findTemplate(rcontext, r.fhirType());
  }

  @Override
  public String findTemplate(RenderingContext rcontext, String resourceName) {
    ResourceDefn r = definitions.getResourceByName(resourceName);
    if (r != null && r.hasLiquid()) {
      return r.getLiquid();
    } else {
      return null;
    }
  }

}
