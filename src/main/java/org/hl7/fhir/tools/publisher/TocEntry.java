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
package org.hl7.fhir.tools.publisher;

import org.hl7.fhir.utilities.StandardsStatus;

public class TocEntry {

  private String value;
  private String text;
  private String link;
  private StandardsStatus status;
  
  public TocEntry(String value, String text, String link, StandardsStatus status) {
    super();
    this.value = value;
    this.text = text;
    this.link = link;
    this.status = status;
  }
  public String getValue() {
    return value;
  }
  public String getText() {
    return text;
  }
  public String getLink() {
    return link;
  }
  public StandardsStatus getStatus() {
    return status;
  }
  public void setText(String string) {
   text = string;    
  }
  public String getIcon() {
    if (status == null) {
      return "icon_page_0.gif";
    }
    switch (status) {
    case DEPRECATED:
      return "icon_page_dep.gif";
    case DRAFT:
      return "icon_page_d.gif";
    case EXTERNAL:
      return "icon_page.gif";
    case INFORMATIVE:
      return "icon_page_i.gif";
    case NORMATIVE:
      return "icon_page_n.gif";
    case TRIAL_USE:
      return "icon_page_tu.gif";
    default:
      return "icon_page_0.gif";
    }
  }
}
