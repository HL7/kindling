package org.hl7.fhir.definitions.parsers;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.tools.publisher.Publisher;
import org.hl7.fhir.utilities.IniFile;
import org.hl7.fhir.utilities.Utilities;
/*
Copyright (c) 2011+, HL7, Inc
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this 
   list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
   this list of conditions and the following disclaimer in the documentation 
   and/or other materials provided with the distribution.
 * Neither the name of HL7 nor the names of its contributors may be used to 
   endorse or promote products derived from this software without specific 
   prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.

*/

public class OIDRegistry {

//  private boolean forPublication;
  private IniFile ini;
  private Map<String, String> oids = new HashMap<>();
  private Map<String, String> urls = new HashMap<>();

  public OIDRegistry(String srcDir) throws IOException {
    ini = new IniFile(Utilities.path(srcDir, "source", "oids.ini"));
    for (String s : ini.getPropertyNames("Key")) {
      loadIniSection(s);
    }
  }
//
//  public String idForUri(String url) {
//    if (Utilities.noString(url))
//      throw new Error("Request for id for null url");
//    if (ini.getIntegerProperty("URLs", url) != null)
//      return ini.getIntegerProperty("URLs", url).toString();
//    else if (!forPublication)
//      return "0";
//    else {
//      Integer last;
//      if (ini.getIntegerProperty("Management", "last") != null)
//        last = ini.getIntegerProperty("Management", "last")+1;
//      else 
//        last = 1;
//      ini.setIntegerProperty("Management", "last", last, null);
//      ini.setIntegerProperty("URLs", url, last, null);
//      return last.toString();
//    }
//  }
//
//  public void commit() {
//    if (forPublication) {
//      ini.save();
//    }
//  }

  private void loadIniSection(String section) {
    String[] list = ini.getPropertyNames(section);
    if (list != null) {
      for (String s : list) {
        String oid = ini.getStringProperty(section, s);
        if (oids.containsKey(oid)) {
          throw new Error("duplicate OID "+oid);
        }
        oids.put(oid, s);
        if (oids.containsKey(s)) {
          throw new Error("duplicate url "+s);
        }
        urls.put(s, oid);
      }
    }
  }

  public String checkOid(String oid) {
    return oids.get(oid);
  }

  public String getOID(String url) {
    if (!urls.containsKey(url) && Publisher.WANT_REQUIRE_OIDS) {
      System.out.println("Url '"+url+"' has no assigned OID");
    }
    return urls.get(url);
  }

}
