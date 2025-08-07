package org.hl7.fhir.definitions.parsers;

/*-
 * #%L
 * org.hl7.fhir.publisher.core
 * %%
 * Copyright (C) 2014 - 2019 Health Level 7
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.hl7.fhir.r5.context.CanonicalResourceManager;
import org.hl7.fhir.r5.extensions.ExtensionUtilities;
import org.hl7.fhir.r5.formats.IParser;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.ContactDetail;
import org.hl7.fhir.r5.model.ContactPoint;
import org.hl7.fhir.r5.model.PackageInformation;
import org.hl7.fhir.r5.model.UsageContext;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.terminologies.CodeSystemUtilities;
import org.hl7.fhir.r5.utils.CanonicalResourceUtilities;
import org.hl7.fhir.r5.extensions.ExtensionDefinitions;
import org.hl7.fhir.utilities.FileUtilities;
import org.hl7.fhir.utilities.Utilities;

public class CodeSystemConvertor {

  private CanonicalResourceManager<CodeSystem> codeSystems;
  private OIDRegistry registry;

  public CodeSystemConvertor(CanonicalResourceManager<CodeSystem> codeSystems, OIDRegistry registry) {
    super();
    this.codeSystems = codeSystems;
    this.registry = registry;
  }

  public CodeSystem convert(IParser p, ValueSet vs, String name, PackageInformation packageInfo) throws Exception  {
    String nname = name.replace("valueset-", "codesystem-");
    if (nname.equals(name))
      nname = FileUtilities.changeFileExt(name, "-cs.xml");
    if (new File(nname).exists()) {
      FileInputStream input;
      try {
        input = new FileInputStream(nname);
        CodeSystem cs = CodeSystemUtilities.makeShareable((CodeSystem) p.parse(input));
        if (!cs.hasTitle())
          cs.setTitle(Utilities.capitalize(Utilities.unCamelCase(cs.getName())));

        populate(cs, vs);
        //      if (codeSystems.containsKey(cs.getUrl())) 
        //        throw new Exception("Duplicate Code System: "+cs.getUrl());
        if (!CodeSystemUtilities.hasOID(cs)) {
          String oid = registry.getOID(cs.getUrl());
          if (oid != null) {
            CodeSystemUtilities.setOID(cs, oid);
          }
        }
        codeSystems.see(cs, packageInfo);
        return cs;
      } catch (Exception e) {
        throw new Exception("Error parsing "+nname+": "+e.getMessage(), e);
      }
    }    
    return null;
  }

  public static void populate(CodeSystem cs, ValueSet vs) {
    if (!vs.hasName())
      throw new Error("No name vs "+vs.getUrl());
    if (!vs.hasDescription())
      throw new Error("No description vs "+vs.getUrl());
    
    if (cs.getUserData("conv-vs") != null)
      throw new Error("This code system has already been converted");
    cs.setUserData("conv-vs", "done");
    vs.setUserData("cs", cs);
    if (vs.hasUserData("filename"))
      cs.setUserData("filename", vs.getUserString("filename").replace("valueset-", "codesystem-"));
    if (vs.hasWebPath())
      cs.setWebPath(vs.getWebPath().replace("valueset-", "codesystem-"));
    if (vs.hasUserData("committee"))
      cs.setUserData("committee", vs.getUserData("committee"));
    cs.setId(vs.getId());
    cs.setVersion(vs.getVersion());
    if (!cs.hasName()) {
      cs.setName(vs.getName());
    }
    if (!cs.hasTitle()) {
      cs.setTitle(vs.getTitle());
    }
    if (!cs.hasStatus()) {
      cs.setStatus(vs.getStatus());
    }
    if (!cs.hasExperimental()) {
      cs.setExperimentalElement(vs.getExperimentalElement());
    }
    if (!cs.hasPublisher()) {
      cs.setPublisher(vs.getPublisher());
    }
    if (!cs.hasContact()) {
      for (ContactDetail csrc : vs.getContact()) {
        ContactDetail ctgt = cs.addContact();
        ctgt.setName(csrc.getName());
        for (ContactPoint cc : csrc.getTelecom())
          ctgt.addTelecom(cc);
      }
    }
    CanonicalResourceUtilities.setHl7WG(vs);
    String wg = ExtensionUtilities.readStringExtension(vs, ExtensionDefinitions.EXT_WORKGROUP);
    CanonicalResourceUtilities.setHl7WG(cs, wg);
    if (!cs.hasDate()) {
      cs.setDate(vs.getDate());
    }
    if (!cs.hasDescription()) {
      cs.setDescription(vs.getDescription());
      cs.getDescriptionElement().getExtension().addAll(vs.getDescriptionElement().getExtension());
    }
    if (!cs.hasUseContext()) {
      for (UsageContext cc : vs.getUseContext())
        cs.addUseContext(cc);
    }
    if (!cs.hasPurpose()) {
      cs.setPurpose(vs.getPurpose());
    }
    if (!cs.hasCopyright()) {
      cs.setCopyright(vs.getCopyright());
    }
    if (!cs.hasValueSet()) {
      if (vs.hasCompose() && vs.getCompose().getInclude().size() == 1 && vs.getCompose().getExclude().size() == 0
          && vs.getCompose().getInclude().get(0).getSystem().equals(cs.getUrl()) 
          && !vs.getCompose().getInclude().get(0).hasValueSet()
          && !vs.getCompose().getInclude().get(0).hasConcept()
          && !vs.getCompose().getInclude().get(0).hasFilter())
        cs.setValueSet(vs.getUrl());
      vs.setImmutable(true);
    }
  }

}
