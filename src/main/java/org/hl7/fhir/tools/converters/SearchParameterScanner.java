package org.hl7.fhir.tools.converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.SearchParameter;
import org.hl7.fhir.utilities.CommaSeparatedStringBuilder;
import org.hl7.fhir.utilities.Utilities;

public class SearchParameterScanner {

  public static void main(String[] args) throws IOException {
   new SearchParameterScanner().process("/Users/grahamegrieve/work/r5/publish",
       "Account", "AdverseEvent", "AllergyIntolerance", "Appointment", "AppointmentResponse", "AuditEvent", "Basic", "BodyStructure",
       "CarePlan", "CareTeam", "ChargeItem", "Claim", "ClaimResponse", "ClinicalImpression", "Communication", "CommunicationRequest", "Composition",
       "Condition", "Consent", "Contract", "Coverage", "CoverageEligibilityRequest", "CoverageEligibilityResponse", "DetectedIssue", "DeviceRequest",
       "DeviceUsage", "DiagnosticReport", "DocumentReference", "Encounter", "EnrollmentRequest", "EpisodeOfCare", "ExplanationOfBenefit", "FamilyMemberHistory",
       "Flag", "Goal", "GuidanceResponse", "ImagingSelection", "ImagingStudy", "Immunization", "ImmunizationEvaluation", "ImmunizationRecommendation", "Invoice",
       "List", "MeasureReport", "Medication", "MedicationAdministration", "MedicationDispense", "MedicationRequest", "MedicationStatement", "MolecularSequence",
       "NutritionIntake", "NutritionOrder", "Observation", "Person", "Procedure", "Provenance", "QuestionnaireResponse", "RelatedPerson", "RequestOrchestration",
       "ResearchSubject", "RiskAssessment", "ServiceRequest", "Specimen", "SupplyDelivery", "SupplyRequest", "Task", "VisionPrescription");
  }

  
  private void process(String folder, String... resources) throws IOException {
    System.out.println("Loading");
//    FilesystemPackageCacheManager pcm = new FilesystemPackageCacheManager(true);
//    NpmPackage npm = pcm.loadPackage("hl7.fhir.r5.core");
//    context = new SimpleWorkerContextBuilder().fromPackage(npm);
//    fpe = new FHIRPathEngine(context);
//    System.out.println("Loaded");
    scan(new File(folder), resources);
    System.out.println("Done");
  }
  private void scan(File file, String[] resources) {

    for (File f : file.listFiles()) {
      if (f.isDirectory()) {
        scan(f, resources);
      } else if (f.getName().endsWith(".xml")) {
        try {
          Resource res = new XmlParser().parse(new FileInputStream(f));
          scan(res, f.getAbsolutePath(), resources); 
        } catch (Exception e) {
        }
      } else if (f.getName().endsWith(".json")) {
        try {
          Resource res = new JsonParser().parse(new FileInputStream(f));
          scan(res, f.getAbsolutePath(), resources); 
        } catch (Exception e) {
        }
      }
    }    
  }


  private void scan(Resource res, String absolutePath, String[] resources) {
    if (res instanceof Bundle) {
      for (BundleEntryComponent be : ((Bundle) res).getEntry()) {
        if (be.hasResource()) {
          scan(be.getResource(), absolutePath, resources);
        }
      }
    } else if (res instanceof SearchParameter) {
      SearchParameter sp = (SearchParameter) res;
      boolean in = false;
      CommaSeparatedStringBuilder b = new CommaSeparatedStringBuilder();
      for (CodeType base : sp.getBase()) {
        b.append(base.toString());
        if (Utilities.existsInList(base.asStringValue(), resources)) {
          in = true;
        }
      }
      CommaSeparatedStringBuilder b2 = new CommaSeparatedStringBuilder();
      for (CodeType base : sp.getTarget()) {
        b2.append(base.toString());
        if (Utilities.existsInList(base.asStringValue(), resources)) {
          in = true;
        }
      }
      if (in) {
        if (Utilities.existsInList(sp.getCode(), "date", "code", "encounter", "identifier", "type")) {
          System.out.println(sp.getCode()+" : "+sp.getType().toCode()+"  ("+b.toString()+"/"+b2.toString()+") - "+sp.getUrl());
        }
      }
    }
  }
  
}
