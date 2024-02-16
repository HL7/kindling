package org.hl7.fhir.definitions.generators.specification;
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
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.definitions.model.BindingSpecification;
import org.hl7.fhir.definitions.model.BindingSpecification.AdditionalBinding;
import org.hl7.fhir.definitions.model.BindingSpecification.BindingMethod;
import org.hl7.fhir.definitions.model.ConstraintStructure;
import org.hl7.fhir.definitions.model.ElementDefn;
import org.hl7.fhir.r5.renderers.AdditionalBindingsRenderer;
import org.hl7.fhir.r5.model.ElementDefinition.ElementDefinitionBindingComponent;
import org.hl7.fhir.r5.model.Enumerations.BindingStrength;
import org.hl7.fhir.r5.renderers.AdditionalBindingsRenderer;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.terminologies.CodeSystemUtilities;
import org.hl7.fhir.r5.terminologies.CodeSystemUtilities.SystemReference;
import org.hl7.fhir.r5.terminologies.ValueSetUtilities;
import org.hl7.fhir.tools.publisher.PageProcessor;
import org.hl7.fhir.utilities.Utilities;

public class TerminologyNotesGenerator extends OutputStreamWriter {

  private PageProcessor page;
  
	public class CDUsage {
		public CDUsage(String path, ElementDefn element) {
			this.path = path;
			this.element = element;
		}
		private String path;
		private ElementDefn element;
	}

	public class MyCompare implements Comparator<BindingSpecification> {

		@Override
		public int compare(BindingSpecification arg0, BindingSpecification arg1) {
			return txusages.get(arg0).get(0).path.compareTo(txusages.get(arg1).get(0).path);
		}

	}


  protected String getBindingLink(BindingSpecification bs) throws Exception {
    if (bs.getValueSet() != null) 
      return bs.getValueSet().hasUserData("external.url") ? bs.getValueSet().getUserString("external.url") : bs.getValueSet().getWebPath();
    else if (bs.getReference() != null)
      return bs.getReference();      
    else 
      return "(unbound)";
  }
  
  
	char c = 'A';
	private Map<BindingSpecification, List<CDUsage>> txusages = new HashMap<BindingSpecification, List<CDUsage>>();
  private boolean noHeader; 
	
	public TerminologyNotesGenerator(OutputStream out, PageProcessor page) throws UnsupportedEncodingException {
		super(out, "UTF-8");
		this.page = page;
	}

	public void generateExtension(String prefix, StructureDefinition ed) throws Exception
	{
	  scanExtension(ed, ed.getUrl());
		gen(prefix, txusages);
		flush();
		close();
	}


  public void generate(String prefix, ElementDefn root) throws Exception
  {
    scan(root, root.getName());
    gen(prefix, txusages);
    flush();
    close();
  }

  public void generate(String prefix, StructureDefinition root) throws Exception
  {
//    scan(root, root.getName());
//    gen(prefix, txusages);
    flush();
    close();
  }

  public void generate(String prefix, ConstraintStructure profile) throws Exception
  {
//    write("<p>\r\nDefined Bindings\r\n</p>\r\n<ul>\r\n");
//    for (BindingSpecification b : profile.getBindings()) {
//      genBinding(b, "", false);
//    }
//    write("</ul>\r\n");
    scan(profile);
    gen(prefix, txusages);
    flush();
    close();
  }
	
  private void scanExtension(StructureDefinition exd, String url) throws Exception {
    // todo: figure out how to bridge this together 
//    for (ElementDefinition ed : exd.getSnapshot().getElement()) {
//      if (ed.hasBinding()) {
//        ElementDefinitionBindingComponent cd = makeBindingSpecification(ed.getBinding());
//        if (cd != null) {
//          if (!txusages.containsKey(cd)) {
//            txusages.put(cd, new ArrayList<CDUsage>());
//            c++;
//            txusages.get(cd).add(new CDUsage(String.valueOf(c), null));           
//          }
//          txusages.get(cd).add(new CDUsage(url, null));
//        }
//      }
//    }
  }


  private void scan(ConstraintStructure profile) throws Exception {
    // todo
	}

  private void gen(String prefix, Map<BindingSpecification, List<CDUsage>> txusages2) throws Exception {
		List<BindingSpecification> cds = new ArrayList<BindingSpecification>();
		cds.addAll(txusages.keySet());
		if (cds.size() == 0)
			return;
		
		Collections.sort(cds, new MyCompare());
		if (noHeader)
	    write("<p>\r\n<b>Terminology Bindings</b>\r\n</p>\r\n");
		else
		  write("<h3>\r\nTerminology Bindings\r\n</h3>\r\n");
		// 1. new form
    write("<table class=\"grid\">\r\n");
    write(" <tr><th>Path</th><th>ValueSet</th><th>Type</th><th>Documentation</th></tr>\r\n");
    for (BindingSpecification cd : cds) {
      String path;
      List<CDUsage> list = txusages.get(cd);
      for (int i = 2; i < list.size(); i++) {
        if (!matchesTypes(list.get(i).element.typeCodeNoParams(), list.get(1).element.typeCodeNoParams()))
          throw new Exception("Mixed types on one concept domain in one type - not yet supported by the build process for binding "+cd.getName()+" ("+list.get(i).element.typeCode()+" vs "+list.get(1).element.typeCode()+")");
      }
      String name = cd.getValueSet() != null ? cd.getValueSet().present() : cd.getName();
      write(" <tr><td "+(cd.getAdditionalBindings().size() == 0 ? "" : "style=\"border-bottom: none\" ")+"valign=\"top\" title=\""+name+"\">");
      boolean first = true;
      for (int i = 1; i < list.size(); i++) {
        if (!first)
          write("<br/>");
        first = false;
        write(list.get(i).path);          
      }
      write(" </td>");
      if (cd.getBinding() == BindingMethod.Unbound)
        write("<td>Unknown</td><td valign=\"top\">No details provided yet</td>");
      else { 
        write("<td "+(cd.getAdditionalBindings().size() == 0 ? "" : "style=\"border-bottom: 1px silver dotted\" ")+"valign=\"top\">");
        if (cd.getBinding() == BindingSpecification.BindingMethod.Special) {
          if (name.equals("MessageEvent"))
            write("<a href=\""+prefix+"valueset-message-events.html\">Message Events</a>");
          else if (name.equals("ResourceType"))
            write("<a href=\""+prefix+"valueset-resource-types.html\">Resource Types</a>");
          else if (name.equals("DataType"))
            write("<a href=\""+prefix+"valueset-data-types.html\">Datatypes</a>");
          else if (name.equals("FHIRDefinedType"))
            write("<a href=\""+prefix+"valueset-defined-types.html\">Defined Types</a>");
          else if (name.equals("FHIRAllTypes"))
            write("<a href=\""+prefix+"valueset-all-types.html\">All Types</a>");
          else 
            throw new Exception("Unknown special type "+name);
        } else if (cd.getValueSet() != null) {
          ValueSet vs = cd.getValueSet();
          String pp = vs.hasUserData("external.url") ? vs.getUserString("external.url") : vs.getWebPath();
          if (pp == null)
            throw new Exception("unknown path on "+cd.getReference());
          write("<a href=\""+prefix+pp.replace(File.separatorChar, '/')+"\">"+vs.getName()+"</a><!-- b -->");
          String system = ValueSetUtilities.getAllCodesSystem(vs);
          if (system != null) {
            SystemReference sr = CodeSystemUtilities.getSystemReference(system, page.getWorkerContext());
            if (sr == null) {
              write(" (a valid code from <code>"+system+"</code>)");
            } else if (sr.getText() != null && textSame(sr.getText(), vs.getName())) {
              write(" ");
            } else if (sr.getLink() == null) {
              write(" (a valid code from "+sr.getText()+" (<code>"+system+"</code>)");
            } else {
              write(" (a valid code from <a href=\""+sr.getLink()+"\">"+sr.getText()+"</a>)");
            }
          }
        } else if (cd.getBinding() == BindingSpecification.BindingMethod.ValueSet) {
          if (Utilities.noString(cd.getReference())) 
            write("??");
          else if (cd.getReference().startsWith("valueset-"))
            write("<a href=\""+prefix+cd.getReference()+".html\">http://hl7.org/fhir/ValueSet/"+cd.getReference().substring(9)+"</a><!-- a -->");            
          else if (cd.getReference().startsWith("http://hl7.org/fhir")) {
            if (cd.getReference().startsWith("http://terminology.hl7.org/ValueSet/v3-")) {
              ValueSet vs = page.getValueSets().get(cd.getReference());
              String pp = vs.hasUserData("external.url") ? vs.getUserString("external.url") : vs.getWebPath();
              if (pp == null)
                throw new Exception("unknown path on "+cd.getReference());
              write("<a href=\""+prefix+pp.replace(File.separatorChar, '/')+"\">"+cd.getReference()+"</a><!-- b -->");
            } else if (cd.getReference().startsWith("http://terminology.hl7.org/ValueSet/v2-")) {
                ValueSet vs = page.getValueSets().get(cd.getReference());
                String pp = vs.hasUserData("external.url") ? vs.getUserString("external.url") : vs.getWebPath();
                write("<a href=\""+prefix+pp.replace(File.separatorChar, '/')+"\">"+cd.getReference()+"</a><!-- c -->");
            } else if (cd.getReference().startsWith("http://hl7.org/fhir/ValueSet/")) {
              String ref = getBindingLink(cd);
              write("<a href=\""+prefix+ref+"\">"+cd.getReference()+"</a><!-- d -->");
//              BindingSpecification bs1 = page.getDefinitions().getBindingByURL(cd.getReference());
//              if (bs1 != null)
//                write("<a href=\""+cd.getReference().substring(23)+".html\">"+cd.getReference()+"</a><!-- d -->");
//              else
//                write("<a href=\"valueset-"+cd.getReference().substring(23)+".html\">"+cd.getReference()+"</a><!-- d -->");
            } else
              throw new Exception("Internal reference "+cd.getReference()+" not handled yet");
          } else if (cd.getReference().startsWith("http:"))
            write("<a href=\""+cd.getReference()+"\">"+cd.getReference()+"</a><!-- e -->");            
          else
            write("<a href=\""+prefix+"valueset-"+cd.getReference()+".html\">http://hl7.org/fhir/"+cd.getReference()+"</a><!-- e -->");            
        } else if (cd.getBinding() == BindingSpecification.BindingMethod.CodeList) {
          write("<a href=\""+prefix+"valueset-"+cd.getReference().substring(1)+".html\">http://hl7.org/fhir/"+cd.getReference().substring(1)+"</a><!-- f -->");            
        } 

        write("</td>");
      }
      write("<td"+(cd.getAdditionalBindings().size() == 0 ? "" : " style=\"border-bottom: 1px silver dotted\"")+"><a href=\""+prefix+"terminologies.html#"+cd.getStrength().toCode()+"\">"+cd.getStrength().getDisplay()+"</a></td>");
      if (cd.getValueSet() != null) {
        write("<td"+(cd.getAdditionalBindings().size() == 0 ? "" : " style=\"border-bottom: 1px silver dotted\"")+" valign=\"top\">");
        write(page.processMarkdown("vs.description", cd.getValueSet().getDescription(), ""));
        write("</td>");
      } else {
        write("<td"+(cd.getAdditionalBindings().size() == 0 ? "" : " style=\"border-bottom: 1px silver dotted\"")+" valign=\"top\">"+Utilities.escapeXml(cd.getDefinition())+"</td>");
      }
      write(" </tr>\r\n");
      for (AdditionalBinding ab : cd.getAdditionalBindings()) {
        write(" <tr>\r\n");
        write("   <td style=\"border-top: none\">&nbsp;</td>\r\n");
        if (ab.getValueSet() != null) {          
          write("   <td"+(cd.getAdditionalBindings().size() == 0 ? "" : " style=\"border-top: 1px silver dotted\"")+"><a href=\""+ab.getValueSet().getWebPath()+"\">"+Utilities.escapeXml(ab.getValueSet().present())+"</a></td>\r\n");
        } else {
          write("   <td"+(cd.getAdditionalBindings().size() == 0 ? "" : " style=\"border-top: 1px silver dotted\"")+"><code>"+Utilities.escapeXml(ab.getRef())+"</code></td>\r\n");
        }
        write("   <td"+(cd.getAdditionalBindings().size() == 0 ? "" : " style=\"border-top: 1px silver dotted\"")+"><a href=\"codesystem-additional-binding-purpose.html#additional-binding-purpose-"+ab.getPurpose()+"\">"+ab.getPurpose()+"</a></td>\r\n");
        write("<td"+(cd.getAdditionalBindings().size() == 0 ? "" : " style=\"border-top: 1px silver dotted\"")+" valign=\"top\">");
        write(page.processMarkdown("vs.description", ab.getDoco(), ""));
        write("</td>");
        write(" </tr>\r\n");
      } 

    }
    write("</table>\r\n<p> </p>\r\n");		
	}

  private boolean textSame(String t1, String t2) {
    return t1.toLowerCase().replace(" ", "").equals(t2.toLowerCase().replace(" ", ""));
  }

  private boolean matchesTypes(String t1, String t2) {
    if (t1.equals(t2)) {
      return true;
    }
    if (Utilities.existsInList(t1, "CodeableConcept", "CodeableReference") && Utilities.existsInList(t2, "CodeableConcept", "CodeableReference")) {
      return true;
    }
    return false;
  }

  public static String describeBinding(String prefix, ElementDefinitionBindingComponent def, PageProcessor page) throws Exception {
    if (!def.hasValueSet()) 
      return def.getDescription();
    String ref = def.getValueSet();
    ValueSet vs = page.getValueSets().get(ref);
    if (vs != null) {
      String pp = vs.hasUserData("external.url") ? vs.getUserString("external.url") : vs.getWebPath();
      return def.getDescription()+"<br/>"+conf(def)+ "<a href=\""+(Utilities.isAbsoluteUrl(pp) ? "" : prefix)+pp.replace(File.separatorChar, '/')+"\">"+vs.present()+"</a>"+confTail(def);
    }
    if (ref.startsWith("http:") || ref.startsWith("https:"))
      return def.getDescription()+"<br/>"+conf(def)+" <a href=\""+ref+"\">"+ref+"</a>"+confTail(def);
    else
      return def.getDescription()+"<br/>"+conf(def)+" ?? Broken Reference to "+ref+" ??"+confTail(def);
  }
  
  private static String confTail(ElementDefinitionBindingComponent def) {
    if (def.getStrength() == BindingStrength.EXTENSIBLE)
      return "; other codes may be used where these codes are not suitable";
    else
      return "";
  }

  private static String conf(ElementDefinitionBindingComponent def) {
    switch (def.getStrength()) {
    case EXAMPLE:
      return "For example codes, see ";
    case PREFERRED:
      return "The codes SHOULD be taken from ";
    case EXTENSIBLE:
      return "The codes SHALL be taken from ";
    case REQUIRED:
      return "The codes SHALL be taken from ";
    default:
      return "??";
    }
  }

  public static String describeBinding(String prefix, BindingSpecification cd, PageProcessor page) throws Exception {
    if (cd.getBinding() == BindingSpecification.BindingMethod.Unbound) 
      return cd.getDefinition();
    if (cd.getBinding() == BindingSpecification.BindingMethod.Special) {
      if (cd.getValueSet().getName().equals("MessageEvent"))
        return "the <a href=\""+prefix+"valueset-message-events.html\">Event List in the messaging framework</a>";
      else if (cd.getValueSet().getName().equals("ResourceType"))
        return "<a href=\""+prefix+"valueset-resource-types.html\">Any defined Resource Type name</a>";
      else if (cd.getValueSet().getName().equals("DataType"))
        return "<a href=\""+prefix+"valueset-data-types.html\">Any defined Datatype name</a>";
      else if (cd.getValueSet().getName().equals("FHIRDefinedType"))
        return "<a href=\""+prefix+"valueset-defined-types.html\">Any defined Resource or Datatype name</a>";
      else if (cd.getValueSet().getName().equals("FHIRAllTypes"))
        return "<a href=\""+prefix+"valueset-all-types.html\">Any defined Resource or Datatype name (including \"Any\" and \"Type\")</a>";
      else 
        throw new Exception("Unknown special type "+cd.getValueSet().getName());
    }
    String mx = "";
    if (cd.getAdditionalBindings().size() > 0) {
      AdditionalBindingsRenderer abr = new AdditionalBindingsRenderer(page, "", null, "", page.getRc(), page, null);
      for (AdditionalBinding ab : cd.getAdditionalBindings()) {
        if (ab.getValueSet() != null) {
          abr.seeAdditionalBinding(ab.getPurpose(), ab.getDoco(), ab.getValueSet());
        } else {
          abr.seeAdditionalBinding(ab.getPurpose(), ab.getDoco(), ab.getRef());
        }
      }
      mx = abr.render();
    }

    String bs = "<a href=\""+prefix+"terminologies.html#"+cd.getStrength().toCode()+"\">"+cd.getStrength().getDisplay()+"</a>";
    if (cd.getValueSet() != null) {
      ValueSet vs = cd.getValueSet();
      String pp = vs.hasUserData("external.url") ? vs.getUserString("external.url") : vs.getWebPath();
      return "<a href=\""+prefix+pp.replace(File.separatorChar, '/')+"\">"+cd.getValueSet().present()+"</a> ("+bs+")"+mx;      
    } else if (cd.getBinding() == BindingSpecification.BindingMethod.ValueSet) {
      if (Utilities.noString(cd.getReference())) 
        return cd.getDescription();
      else if (cd.getValueSet() == null)
        return bs+": <a href=\""+(cd.getReference().startsWith("http") ? cd.getReference() : prefix+cd.getReference()+".html")+"\">See "+cd.getDescription()+"</a> ("+cd.getDefinition()+")"+mx;
      else
        return bs+": <a href=\""+prefix+cd.getReference()+".html\">See "+cd.getValueSet().getUrl()+"</a> ("+cd.getDefinition()+mx+")";
    } else if (cd.getBinding() == BindingSpecification.BindingMethod.CodeList) {
      if (Utilities.noString(cd.getReference())) 
        return bs+": "+cd.getDescription()+" ("+cd.getDefinition()+")"+mx;
      else
        return bs+": <a href=\""+prefix+"valueset-"+cd.getReference().substring(1)+".html\">http://hl7.org/fhir/"+cd.getReference().substring(1)+"</a> ("+cd.getDefinition()+")"+mx;
    }
    return "??";
  }

  private void genBinding(BindingSpecification cd, String path, boolean isCode) throws Exception {
    if (cd.getName().equals("*unbound*")) {
    	write("  <li>"+path+" (Error!!!)</li>\r\n");
    } else if (cd.getBinding() == BindingSpecification.BindingMethod.Unbound) {
      write("  <li>"+path+" <i>"+Utilities.escapeXml(cd.getName())+"</i>: \""+Utilities.escapeXml(cd.getDefinition())+"\". (not bound to any codes)</li>\r\n");
    } else if (cd.getBinding() == BindingSpecification.BindingMethod.CodeList) {
      String sid = "";
      if (cd.getAdditionalBindings().size() > 0)
        throw new Error("Additional bindings not handled yet");
      String bs = "<a href=\"terminologies.html#"+cd.getStrength().toCode()+"\">"+cd.getStrength().getDisplay()+"</a>";
      if (!isCode) {
        sid = "\"<a href=\""+cd.getReference().substring(1)+".html\">http://hl7.org/fhir/"+cd.getReference().substring(1)+"\"</a>";
        //					if (!sids.contains(sid))
        //						sids.put(sid, new DefinedCode())
        sid = " system "+sid+"";
        write("  <li>"+path+" <i>"+Utilities.escapeXml(cd.getValueSet().present())+"</i>: \""+Utilities.escapeXml(cd.getDefinition())+"\". "+bs+". See "+sid+".\r\n");
      } else {
        write("  <li>"+path+" <i>"+Utilities.escapeXml(cd.getValueSet().present())+"</i>: \""+Utilities.escapeXml(cd.getDefinition())+"\" "+bs+". "+sid+". Example values:\r\n");
        write("  <li>this list is todo:\r\n");
     // bscodes  
//        write("    <table class=\"codes\">\r\n");
//        boolean hasComment = false;
//        boolean hasDefinition = false;
//        for (DefinedCode c : cd.getCodes()) {
//          hasComment = hasComment || c.hasComment();
//          hasDefinition = hasDefinition || c.hasDefinition();
//        }
//        //				if (hasComment)
//        //					write("    <tr><td><b>Code</b></td><td><b>Title</b></td><td><b>Comment</b></td></tr>");
//        //				else if (hasDefinition)
//        //					write("    <tr><td><b>Code</b></td><td colspan=\"2\"><b>Title</b></td></tr>");
//
//
//        for (DefinedCode c : cd.getCodes()) {
//          if (hasComment)
//            write("    <tr><td>"+Utilities.escapeXml(c.getCode())+"</td><td>"+Utilities.escapeXml(c.getDefinition())+"</td><td>"+Utilities.escapeXml(c.getComment())+"</td></tr>");
//          else if (hasDefinition)
//            write("    <tr><td>"+Utilities.escapeXml(c.getCode())+"</td><td colspan=\"2\">"+Utilities.escapeXml(c.getDefinition())+"</td></tr>");
//          else
//            write("    <tr><td colspan=\"3\">"+Utilities.escapeXml(c.getCode())+"</td></tr>");
//        }
//        write("    </table>\r\n");
      }
    	write("  </li>\r\n");
    	
    } else if (cd.getBinding() == BindingSpecification.BindingMethod.Special) {
      if (cd.getValueSet().getName().equals("MessageEvent"))
        write("<li>"+path+" of the <a href=\"message.html#Events\"> Event List in the messaging framework</a></li>\r\n");
      else if (cd.getValueSet().getName().equals("ResourceType"))
        write("  <li>"+path+" of <a href=\"terminologies.html#ResourceType\"> any defined Resource Type name</a></li>\r\n");
      else if (cd.getValueSet().getName().equals("FHIRContentType"))
        write("  <li>"+path+" of <a href=\"terminologies.html#fhircontenttypes\"> any defined Resource or Datatype name</a></li>\r\n");
      else 
        write("  <li>"+path+" of <a href=\"datatypes.html\"> any defined datatype name</a> (including <a href=\"resource.html#Resource\">Resource</a>)</li>\r\n");
      
    } else {
      String bs = "<a href=\"terminologies.html#"+cd.getStrength().toCode()+"\">"+cd.getStrength().getDisplay()+"</a>";
      write("  <li>"+path+" <i>"+Utilities.escapeXml(cd.getValueSet().getName())+"</i>: \""+Utilities.escapeXml(cd.getDefinition())+"\". "+bs+". See "+ref(cd)+"</li>\r\n");
    }
  }

	

  private String ref(BindingSpecification cd) {
    if (!cd.hasReference())
      return Utilities.escapeXml(cd.getDescription());
    else if (cd.getValueSet() != null)
      return "<a href=\""+cd.getReference()+".html\">"+Utilities.escapeXml(cd.getValueSet().present())+"</a>";      
    else
      return "<a href=\""+cd.getReference()+"\">"+Utilities.escapeXml(cd.getDescription())+"</a>";
  }


	private void scan(ElementDefn e, String path) throws Exception {
		if (e.hasBinding()) {
			BindingSpecification cd = e.getBinding();
			if (!txusages.containsKey(cd)) {
				txusages.put(cd, new ArrayList<CDUsage>());
				c++;
				txusages.get(cd).add(new CDUsage(String.valueOf(c), null));						
			}
			txusages.get(cd).add(new CDUsage(path, e));			
		}
		for (ElementDefn c : e.getElements()) {
			scan(c, path+"."+c.getName());
		}		
	}

  public void setNoHeader(boolean noHeader) {
    this.noHeader = noHeader;
    
  }  
	
}
