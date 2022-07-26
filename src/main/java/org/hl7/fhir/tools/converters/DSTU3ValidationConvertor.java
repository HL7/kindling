package org.hl7.fhir.tools.converters;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.hl7.fhir.convertors.conv30_50.resources30_50.Bundle30_50;
import org.hl7.fhir.convertors.factory.VersionConvertorFactory_30_40;
import org.hl7.fhir.convertors.factory.VersionConvertorFactory_30_50;
import org.hl7.fhir.convertors.factory.VersionConvertorFactory_40_50;
import org.hl7.fhir.dstu3.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Enumerations.FHIRVersion;
import org.hl7.fhir.utilities.VersionUtilities;

public class DSTU3ValidationConvertor {

  private FHIRVersion version;
  
  public DSTU3ValidationConvertor(FHIRVersion aVersion) {
    version = aVersion;
  }
  
  public void convert(String bundleSource, String bundleTarget) throws Exception {
    System.out.println("Convert "+bundleSource);
    
    try {
      org.hl7.fhir.dstu3.model.Bundle target = null;      
      if (VersionUtilities.isR4BVer(version.toCode())) {
        org.hl7.fhir.r4.model.Bundle source = (org.hl7.fhir.r4.model.Bundle) new org.hl7.fhir.r4.formats.XmlParser().parse(new FileInputStream(bundleSource));
        target = (org.hl7.fhir.dstu3.model.Bundle) VersionConvertorFactory_30_40.convertResource(source);
        
      } else {
        Bundle source = (Bundle) new XmlParser().parse(new FileInputStream(bundleSource));
        target = (org.hl7.fhir.dstu3.model.Bundle) VersionConvertorFactory_30_50.convertResource(source);
      }

      new org.hl7.fhir.dstu3.formats.XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(bundleTarget), target);     
    } catch (Exception e) {
      throw new Exception(e);
    } 
  }
  
  public void convertJ(String bundleSource, String bundleTarget) throws Exception {
    System.out.println("Convert "+bundleSource);
    
    try {
      org.hl7.fhir.dstu3.model.Bundle target = null;      
      if (VersionUtilities.isR4BVer(version.toCode())) {
        org.hl7.fhir.r4.model.Bundle source = (org.hl7.fhir.r4.model.Bundle) new org.hl7.fhir.r4.formats.XmlParser().parse(new FileInputStream(bundleSource));
        target = (org.hl7.fhir.dstu3.model.Bundle) VersionConvertorFactory_30_40.convertResource(source);
        
      } else {
        Bundle source = (Bundle) new XmlParser().parse(new FileInputStream(bundleSource));
        target = (org.hl7.fhir.dstu3.model.Bundle) VersionConvertorFactory_30_50.convertResource(source);
      }

      new org.hl7.fhir.dstu3.formats.JsonParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(bundleTarget), target);     
    } catch (Exception e) {
      throw new Exception(e);
    } 
  }
}
