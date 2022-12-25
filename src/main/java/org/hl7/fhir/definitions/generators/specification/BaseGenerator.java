package org.hl7.fhir.definitions.generators.specification;

import java.util.HashSet;
import java.util.Set;

import org.hl7.fhir.definitions.model.BindingSpecification;
import org.hl7.fhir.definitions.model.Definitions;
import org.hl7.fhir.definitions.model.ElementDefn;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.tools.publisher.PageProcessor;
import org.hl7.fhir.utilities.Utilities;

public class BaseGenerator {
  protected PageProcessor page;
  protected Definitions definitions;

  private static Set<String> notified = new HashSet<>();

  public static String getBindingLink(String prefix, ElementDefn e, PageProcessor page) throws Exception {
    BindingSpecification bs = e.getBinding();
    return getBindingLink(prefix, bs, page);
  }
  
  public static String getBindingLink(String prefix, BindingSpecification bs, PageProcessor page) throws Exception {
    if (bs.getValueSet() != null) 
      return bs.getValueSet().hasUserData("external.url") ? bs.getValueSet().getUserString("external.url") :  prefix+bs.getValueSet().getUserString("path");
    else if (bs.getReference() != null) {
      ValueSet vs = page.getWorkerContext().fetchResource(ValueSet.class, bs.getReference());
      if (vs == null) {
        vs = page.getDefinitions().getValuesets().get(bs.getReference());
      }
      if (vs == null) {
        vs = page.getDefinitions().getBoundValueSets().get(bs.getReference());
      }
      if (vs == null) {
        if (bs.getReference().startsWith("http://hl7.org/fhir")) {
          if (!notified.contains(bs.getReference())) {
            notified.add(bs.getReference());
            System.out.println("Broken ValueSet reference "+bs.getReference());
          }
          if (isKnownBrokenVS(bs.getReference())) {
            return "http://this-is-a-broken-link";
          } else {
            throw new Exception("Broken ValueSet reference "+bs.getReference());            
          }
        } else {
          return bs.getReference();
        }
      }
      if (vs.hasUserData("external.url")) {
        return vs.getUserString("external.url");
      }
      if (vs.hasUserData("v")) {
        return prefix+vs.getUserString("path");
      }
      System.out.println("Found direct reference to weird value set "+bs.getReference());
      return bs.getReference();
    } else 
      return null;
  }

  private static boolean isKnownBrokenVS(String reference) {
    return Utilities.existsInList(reference, "http://hl7.org/fhir/ValueSet/biologicallyderivedproduct-property-type-codes",
        "http://hl7.org/fhir/ValueSet/clinical-use-definition-category",
        "http://hl7.org/fhir/ValueSet/disease-symptom-procedure",
        "http://hl7.org/fhir/ValueSet/disease-status",
        "http://hl7.org/fhir/ValueSet/disease-symptom-procedure",
        "http://hl7.org/fhir/ValueSet/therapy",
        "http://hl7.org/fhir/ValueSet/disease-symptom-procedure",
        "http://hl7.org/fhir/ValueSet/disease-status",
        "http://hl7.org/fhir/ValueSet/disease-symptom-procedure",
        "http://hl7.org/fhir/ValueSet/interactant",
        "http://hl7.org/fhir/ValueSet/interaction-type",
        "http://hl7.org/fhir/ValueSet/interaction-effect",
        "http://hl7.org/fhir/ValueSet/interaction-incidence",
        "http://hl7.org/fhir/ValueSet/interaction-management",
        "http://hl7.org/fhir/ValueSet/undesirable-effect-symptom",
        "http://hl7.org/fhir/ValueSet/undesirable-effect-classification",
        "http://hl7.org/fhir/ValueSet/undesirable-effect-frequency",
        "http://hl7.org/fhir/ValueSet/medicationrequest-category",
        "http://hl7.org/fhir/ValueSet/devicedispense-status-reason",
        "http://hl7.org/fhir/ValueSet/deviceusage-adherence-code",
        "http://hl7.org/fhir/ValueSet/deviceusage-adherence-reason",
        "http://hl7.org/fhir/ValueSet/product-classification-codes",
        "http://hl7.org/fhir/ValueSet/research-subject-progress");
  }

}
