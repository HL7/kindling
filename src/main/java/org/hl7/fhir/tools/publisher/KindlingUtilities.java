package org.hl7.fhir.tools.publisher;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.model.core.CanonicalResource;
import org.hl7.fhir.model.core.CodeSystem;
import org.hl7.fhir.model.core.Enumerations;
import org.hl7.fhir.model.core.ValueSet;
import org.hl7.fhir.model.extensions.ExtensionDefinitions;
import org.hl7.fhir.model.extensions.ExtensionUtilities;
import org.hl7.fhir.model.utilities.CanonicalResourceUtilities;
import org.hl7.fhir.model.utilities.CodeSystemUtilities;
import org.hl7.fhir.services.context.IWorkerContext;
import org.hl7.fhir.utilities.StandardsStatus;
import org.hl7.fhir.utilities.UserDataNames;
import org.hl7.fhir.utilities.Utilities;

public class KindlingUtilities {

  public static void makeUniversal(CanonicalResource cr) {
    if (!cr.hasJurisdiction()) {
      cr.addJurisdiction().addCoding().setSystem("http://unstats.un.org/unsd/methods/m49/m49.htm").setCode("001").setDisplay("World");
    }    
  }

  public static void markStatus(ValueSet vs, String wg, StandardsStatus status, String fmm, IWorkerContext context, String normativeVersion, String thisVersion) throws FHIRException {
    if (vs.hasUserData(UserDataNames.render_external_link))
      return;

    if (wg != null) {
      if (!ExtensionUtilities.hasExtension(vs, ExtensionDefinitions.EXT_WORKGROUP) ||
              (!Utilities.existsInList(ExtensionUtilities.readStringExtension(vs, ExtensionDefinitions.EXT_WORKGROUP), "fhir", "vocab") && Utilities.existsInList(wg, "fhir", "vocab"))) {
        CanonicalResourceUtilities.setHl7WG(vs, wg);
      }
    }
    if (status != null) {
      StandardsStatus ss = ExtensionUtilities.getStandardsStatus(vs);
      if (ss == null || ss.isLowerThan(status))
        ExtensionUtilities.setStandardsStatus(vs, status, normativeVersion, context.getFHIRVersion());
      if (status == StandardsStatus.NORMATIVE) {
        vs.setStatus(Enumerations.PublicationStatus.ACTIVE);
      }
    }
    if (fmm != null) {
      String sfmm = ExtensionUtilities.readStringExtension(vs, ExtensionDefinitions.EXT_FMM_LEVEL);
      if (Utilities.noString(sfmm) || Integer.parseInt(sfmm) < Integer.parseInt(fmm))  {
        ExtensionUtilities.setIntegerExtension(vs, ExtensionDefinitions.EXT_FMM_LEVEL, Integer.parseInt(fmm));
      }
    }
    if (vs.hasUserData(UserDataNames.TX_ASSOCIATED_CODESYSTEM)) {
      CodeSystemUtilities.markStatus((CodeSystem) vs.getUserData(UserDataNames.TX_ASSOCIATED_CODESYSTEM), wg, status, fmm, normativeVersion, thisVersion);
    } else if (status == StandardsStatus.NORMATIVE && context != null) {
      for (ValueSet.ConceptSetComponent csc : vs.getCompose().getIncludeList()) {
        if (csc.hasSystem()) {
          CodeSystem cs = context.fetchCodeSystem(csc.getSystem(), ExtensionUtilities.getVersionResolutionRules(csc.getSystemElement()));
          if (cs != null) {
            CodeSystemUtilities.markStatus(cs, wg, status, fmm, normativeVersion, context.getFHIRVersion());
          }
        }
      }
    }
  }


}
