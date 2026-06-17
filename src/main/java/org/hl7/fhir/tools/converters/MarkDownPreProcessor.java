package org.hl7.fhir.tools.converters;

import java.util.List;

import org.apache.jena.base.Sys;
import org.hl7.fhir.definitions.model.Definitions;
import org.hl7.fhir.r5.conformance.profile.ProfileUtilities;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r5.model.ValueSet.ValueSetExpansionContainsComponent;
import org.hl7.fhir.r5.terminologies.CodeSystemUtilities;
import org.hl7.fhir.r5.terminologies.expansion.ValueSetExpansionOutcome;
import org.hl7.fhir.tools.publisher.BuildWorkerContext;
import org.hl7.fhir.utilities.DebugUtilities;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueType;
import org.hl7.fhir.utilities.validation.ValidationMessage.Source;
import org.sqlite.core.Codes;

public class MarkDownPreProcessor {

  public static String process(Definitions definitions, BuildWorkerContext workerContext, List<ValidationMessage> validationErrors, String text, String location, String prefix) throws Exception {
    if (Utilities.noString(text))
      return "";
    
    text = text.replace("||", "\r\n\r\n");
    while (text.contains("[[[")) {
      String left = text.substring(0, text.indexOf("[[["));
      if (text.indexOf("]]]") < 0)
        throw new Error(location + ": Missing closing ]]] in markdown text: " + text);
      String linkText = text.substring(text.indexOf("[[[")+3, text.indexOf("]]]"));
      String right = text.substring(text.indexOf("]]]")+3);
      if (linkText.startsWith("valueset:")) {
        String vsid = linkText.substring(9);
        ValueSet vs = workerContext.fetchResource(ValueSet.class, "http://hl7.org/fhir/ValueSet/"+vsid);
        ValueSetExpansionOutcome exp = workerContext.expandVS(vs, true, false);
        if (exp.getValueset() != null)
          text = left+presentExpansion(exp.getValueset().getExpansion().getContains(), workerContext)+right;
        else
          text = left+"["+vs.getName()+"]("+vs.getWebPath()+")"+right;
      } else {
        String url = "";
        String target = null;
        String htmlText = linkText;
        String[] parts = linkText.split("\\#");

        if (parts[0].contains("/StructureDefinition/")) {
          StructureDefinition ed = workerContext.getExtensionStructure(null, parts[0]);
          if (ed == null)
            throw new Error(location + ": Unable to find extension "+parts[0]);
          url = ed.getWebPath();
          if (url == null && ed.hasUserData("filename")) {
            url = ed.getUserData("filename")+".html";
          }
          if (url == null) {
            System.out.println("Broken link for "+ed.getUrl());
          }
          htmlText = ed.present();
        }
        if (Utilities.noString(url)) {
          try {
            Resource cr = workerContext.fetchResourceWithException(Resource.class, parts[0]);
            if (cr != null) {
              url = cr.getWebPath();
              if (cr instanceof CanonicalResource) {
                htmlText = ((CanonicalResource) cr).present();
              }
              if (parts.length > 1) {
                if (cr instanceof CodeSystem) {
                  CodeSystem cs = (CodeSystem) cr;
                  ConceptDefinitionComponent cd = CodeSystemUtilities.findCode(cs.getConcept(), parts[1]);
                  if (cd != null) {
                    htmlText = cd.getCode();
                    if (!cd.getCode().equalsIgnoreCase(cd.getDisplay())) {
                      htmlText += " (" + cd.getDisplay() + ")";
                    }
                    target = cs.getId() + "-" + cd.getCode();
                  } else {
                    target = cr.getId() + "-" + parts[1];
                  }
                } else {
                  target = parts[1];
                }
              }
            }
          } catch (Exception e) {
            System.out.println("Broken link for "+parts[0]);
          }
        }
        if (Utilities.noString(url)) {
          String[] paths = parts[0].split("\\.");
          StructureDefinition p = new ProfileUtilities(workerContext, null, null).getProfile(null, new UriType(paths[0]));
          if (p != null) {
            String suffix = (paths.length > 1) ? "-definitions.html#"+parts[0] : ".html";
            if (p.getUserData("filename") == null)
              url = paths[0].toLowerCase()+suffix;
            else
              url = p.getUserData("filename")+suffix;
          } else if (definitions.hasResource(linkText)) {
            url = linkText.toLowerCase()+".html#";
          } else if (definitions.hasResource(paths[0])) {
            url = paths[0].toLowerCase()+"-definitions.html#"+linkText;
          } else if (definitions.hasElementDefn(linkText)) {
            url = definitions.getSrcFile(linkText)+".html#"+linkText;
          } else if (definitions.hasPrimitiveType(linkText)) {
            url = "datatypes.html#"+linkText;
          } else if (definitions.getPageTitles().containsKey(linkText)) {
            url = definitions.getPageTitles().get(linkText);
          } else if (definitions.getLogicalModel(linkText.toLowerCase()) != null) {
            url = definitions.getLogicalModel(linkText.toLowerCase()).getId()+".html";
          } else if (validationErrors != null) {
            validationErrors.add(
                new ValidationMessage(Source.Publisher, IssueType.BUSINESSRULE, -1, -1, location, "Unresolved logical URL '"+linkText+"'", IssueSeverity.WARNING));
            //        throw new Exception("Unresolved logical URL "+url);
          } else 
            url = "??";
        }
        if (target != null) {
          text = left + "[" + htmlText + "](" + url+ "#"+target+ ")" + right;
        } else {
          text = left + "[" + htmlText + "](" + url + ")" + right;
        }
      }
    }
    // 1. if prefix <> "", then check whether we need to insert the prefix
    if (!Utilities.noString(prefix)) {
      int i = text.length() - 3;
      while (i > 0) {
        if (text.substring(i, i+2).equals("](")) {
          if (!text.substring(i, i+7).equals("](http:")) { //  && !text.substring(i, i+8).equals("](https:"));
            text = text.substring(0, i)+"]("+prefix+text.substring(i+2);
          }
        }
        i--;
      }
    }
    
    return text;
  }

  private static String presentExpansion(List<ValueSetExpansionContainsComponent> contains, BuildWorkerContext workerContext) {
    StringBuilder b = new StringBuilder();
    for (ValueSetExpansionContainsComponent cc : contains) {
      b.append(" - **");
      b.append(cc.getCode());
      b.append("** (\"");
      b.append(cc.getDisplay());
      b.append("\"): ");
      ConceptDefinitionComponent definition = workerContext.getCodeDefinition(cc.getSystem(), cc.getCode());
      b.append(definition.getDefinition());
      b.append("\r\n");
    }
    return b.toString();
  }

}
