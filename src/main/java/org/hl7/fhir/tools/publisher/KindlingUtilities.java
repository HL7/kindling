package org.hl7.fhir.tools.publisher;

import org.hl7.fhir.r5.model.CanonicalResource;

public class KindlingUtilities {

  public static void makeUniversal(CanonicalResource cr) {
    if (!cr.hasJurisdiction()) {
      cr.addJurisdiction().addCoding().setSystem("http://unstats.un.org/unsd/methods/m49/m49.htm").setCode("001").setDisplay("World");
    }    
  }

}
