package org.hl7.fhir.definitions.generators.xsd;

public abstract class XSDRootGenerator {
  
  protected String namify(String name) {
    StringBuilder b = new StringBuilder();
    boolean ws = false;
    for (char c : name.toCharArray()) {
      if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
        if (ws) {
          ws = false;
          b.append(Character.toUpperCase(c));
        } else 
          b.append(c);          
      } else 
        ws = true;        
    }
    return b.toString();
  }


}
