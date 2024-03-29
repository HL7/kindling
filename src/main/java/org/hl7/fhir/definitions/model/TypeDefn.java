package org.hl7.fhir.definitions.model;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.utilities.Utilities;

public class TypeDefn extends ElementDefn {
  private StructureDefinition profile;
  private String fmmLevel = "1";
  private List<String> characteristics = new ArrayList<>();
  
  public TypeDefn(String charList) {
   loadCharacteristics(charList);
  }

  public void loadCharacteristics(String charList) {
    if (charList != null) {
      for (String c : charList.split("\\,")) {
        if (!Utilities.noString(c)) {
          characteristics.add(c);
        }
      }
    }
  }
  
  public StructureDefinition getProfile() {
    return profile;
  }

  public void setProfile(StructureDefinition profile) {
    this.profile = profile;
  }

  public String getFmmLevel() {
    return fmmLevel;
  }

  public void setFmmLevel(String fmmLevel) {
    this.fmmLevel = fmmLevel;
  }

  public List<String> getCharacteristics() {
    return characteristics;
  }

  
}
