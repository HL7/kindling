package org.hl7.fhir.definitions.generators.xsd;
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
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.definitions.Config;
import org.hl7.fhir.definitions.model.DefinedCode;
import org.hl7.fhir.definitions.model.Definitions;
import org.hl7.fhir.definitions.model.ResourceDefn;
import org.hl7.fhir.tools.publisher.BuildWorkerContext;
import org.hl7.fhir.utilities.IniFile;
import org.hl7.fhir.utilities.FileUtilities;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.filesystem.CSFile;

public class SchemaGenerator {

  private String genDate;
  private String version;
  private BuildWorkerContext workerContext;

  public void generate(Definitions definitions, IniFile ini, String tmpResDir, String xsdDir, String dstDir, String srcDir, String version, String genDate, boolean forCodeGeneration, BuildWorkerContext workerContext) throws Exception {
	  this.genDate = genDate;
	  this.version = version;
	  this.workerContext = workerContext;

	  if (!forCodeGeneration) {
	    File dir = new CSFile(xsdDir);
	    File[] list = dir.listFiles();
	    if (list != null) {
	      for (File f : list) {
	        if (!f.isDirectory())
	          f.delete();
	      }
	    }
	  }

    Set<String> allenums = new HashSet<String>();
	  XSDBaseGenerator xsdb = new XSDBaseGenerator(new OutputStreamWriter(new FileOutputStream(new CSFile(xsdDir+"fhir-base.xsd")), "UTF-8"), forCodeGeneration, workerContext, allenums, version);
	  xsdb.setDefinitions(definitions);
	  xsdb.generate(version, genDate, true);
	  xsdb.getWriter().close();

    List<String> names = new ArrayList<String>();
    names.addAll(definitions.getResources().keySet());
    names.add("Parameters");
    Collections.sort(names);
    for (String name : names) {
      ResourceDefn root = definitions.getResourceByName(name);
		  XSDGenerator sgen = new XSDGenerator(new OutputStreamWriter(new FileOutputStream(new CSFile(xsdDir+root.getName().toLowerCase()+".xsd")), "UTF-8"), definitions, forCodeGeneration, workerContext, allenums, version);
		  sgen.setDataTypes(definitions.getKnownTypes());
		  sgen.generate(root.getRoot(), version, genDate, true);
		  sgen.getWriter().close();
	  }

	  OutputStreamWriter single = new OutputStreamWriter(new FileOutputStream(new CSFile(xsdDir+"fhir-single.xsd")), "UTF-8");
	  
	  single.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
	  single.write("<!-- \r\n");
	  single.write(Config.FULL_LICENSE_CODE);
	  single.write("\r\n");
	  single.write("  Generated on " + genDate + " for FHIR v" + version + " \r\n");
	  single.write("-->\r\n");
	  single.write("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://hl7.org/fhir\" xmlns:xhtml=\"http://www.w3.org/1999/xhtml\" "
	      + "xmlns:xml=\"http://www.w3.org/XML/1998/namespace\" targetNamespace=\"http://hl7.org/fhir\" elementFormDefault=\"qualified\" version=\""+version+"\">\r\n");
	  single.write("  <!-- Note: When using this schema with some tools, it may also be necessary to declare xmlns:xml=\"http://www.w3.org/XML/1998/namespace\", however this causes performance issues with other tools and thus is not in the base schemas. -->\r\n");

    allenums = new HashSet<String>();
    xsdb = new XSDBaseGenerator(single, forCodeGeneration, workerContext, allenums, version);
    xsdb.setDefinitions(definitions);
    xsdb.generate(version, genDate, false);
 
//    single.write("  <xs:simpleType name=\"ResourceNamesPlusBinary\">\r\n");
//    single.write("    <xs:union memberTypes=\"ResourceType\">\r\n");
//    single.write("      <xs:simpleType>\r\n");
//    single.write("      <xs:restriction base=\"xs:NMTOKEN\">\r\n");
//    single.write("        <xs:enumeration value=\"Binary\"/>\r\n");
//    single.write("      </xs:restriction>\r\n");
//    single.write("    </xs:simpleType>\r\n");
//    single.write("  </xs:union>\r\n");
//    single.write("  </xs:simpleType>\r\n");
//    single.write("  <xs:complexType name=\"Binary\">\r\n");
//    single.write("    <xs:simpleContent>\r\n");
//    single.write("      <xs:extension base=\"xs:base64Binary\">\r\n");
//    single.write("        <xs:attribute name=\"contentType\" type=\"xs:string\" use=\"required\"/>\r\n");
//    single.write("        <xs:attribute name=\"id\" type=\"id-primitive\"/>\r\n");
//    single.write("      </xs:extension>\r\n");
//    single.write("    </xs:simpleContent>\r\n");
//    single.write("  </xs:complexType>\r\n");
//    single.write("  <xs:element name=\"Binary\" type=\"Binary\"/>\r\n");
//  
    for (String name : names) {
      ResourceDefn root = definitions.getResourceByName(name);
      XSDGenerator sgen = new XSDGenerator(single, definitions, forCodeGeneration, workerContext, allenums, version);
      sgen.setDataTypes(definitions.getKnownTypes());
      sgen.generate(root.getRoot(), version, genDate, false);
    }

    single.write("</xs:schema>\r\n");
    single.flush();
    single.close();
	  for (String n : ini.getPropertyNames("schema")) {
		  String xsd = FileUtilities.fileToString(srcDir + n);
		  xsd = processSchemaIncludes(definitions, n, xsd, false);
		  FileUtilities.stringToFile(xsd, xsdDir + n);
	  }

      produceCombinedSchema(definitions, xsdDir, dstDir, srcDir);
    if (!forCodeGeneration) {      
      File dir = new CSFile(xsdDir);
      File[] list = dir.listFiles();
      for (File f : list) {
        if (!f.isDirectory())
          FileUtilities.copyFile(f, new CSFile(dstDir+f.getName()));
      }
    }
  }

  private void produceCombinedSchema(Definitions definitions, String xsdDir, String dstDir, String srcDir) throws Exception {
    String src = FileUtilities.fileToString(srcDir + "fhir-all.xsd");
    src = processSchemaIncludes(definitions, "fhir-all.xsd", src, false);
    FileUtilities.stringToFile(src, xsdDir + "fhir-all.xsd");
  }

  private String processSchemaIncludes(Definitions definitions, String filename, String src, boolean singleMode) throws Exception {
    List<String> names = new ArrayList<String>();
    names.addAll(definitions.getResources().keySet());
    names.add("Parameters");
    Collections.sort(names);
    while (src.contains("<!--%") || src.contains("<%"))
    {
      int i2;
      String s1;
      String s2;
      String s3;

      int i1 = src.indexOf("<!--%");
      if (i1 > -1) {
        i2 = src.indexOf("%-->");
        s1 = src.substring(0, i1);
        s2 = src.substring(i1 + 5, i2).trim();
        s3 = src.substring(i2 + 4);
      } else {
        i1 = src.indexOf("<%");
        i2 = src.indexOf("%>");
        s1 = src.substring(0, i1);
        s2 = src.substring(i1 + 2, i2).trim();
        s3 = src.substring(i2 + 2);
      } 

      String[] com = s2.split(" ");
      if (com[0].equals("genDate"))
        src = s1+ genDate+s3;
      else if (com[0].equals("version"))
        src = s1+version+s3;
      else if (com[0].equals("includes")) {
        if (singleMode)
          src = s1+"<xs:import namespace=\"http://hl7.org/fhir\" schemaLocation=\"fhir-single.xsd\"/>"+s3;
        else
          src = s1+"<xs:import namespace=\"http://hl7.org/fhir\" schemaLocation=\"fhir-all.xsd\"/>"+s3;
      } else if (com[0].equals("resources")) {
        StringBuilder includes = new StringBuilder();
        for (String n : names) 
          includes.append("  <xs:include schemaLocation=\""+n.toLowerCase()+".xsd\"/>\r\n");
        src = s1+includes.toString()+s3;
      }
      else if (com[0].equals("atom.imports")) {
        StringBuilder includes = new StringBuilder();
        for (String n : names) 
          includes.append("  <xs:import namespace=\"http://hl7.org/fhir\" schemaLocation=\""+n+".xsd\"/>\r\n");
        src = s1+includes.toString()+s3;
      }
      else if (com[0].equals("atom.elements")) {
        StringBuilder includes = new StringBuilder();
        for (String n : names) 
          includes.append("      <xs:element ref=\"fhir:"+n+"\"/>\r\n");
        src = s1+includes.toString()+s3;
      }
      else if (com[0].equals("enum")) {
        Collection<DefinedCode> values;
        if (com[1].equals("resource")) {
          values = definitions.getKnownResources().values();          
        } else {
          throw new Error("fix this");
//          values = definitions.getBindingByName(com[1]).getCodes();
        }
        StringBuilder enums = new StringBuilder();
        for (DefinedCode c : values) {
        	if (!c.getAbstract()) {
	          enums.append("        <xs:enumeration value=\""+c.getCode()+"\">\r\n");
	          enums.append("          <xs:annotation>\r\n");
	          enums.append("            <xs:documentation xml:lang=\"en\">"+Utilities.escapeXml(c.getDefinition())+"</xs:documentation>\r\n");
	          for (String l : c.getLangs().keySet()) {
	            enums.append("            <xs:documentation xml:lang=\""+l+"\">"+Utilities.escapeXml(c.getLangs().get(l))+"</xs:documentation>\r\n");
	          }
	          enums.append("          </xs:annotation>\r\n");
	          enums.append("        </xs:enumeration>\r\n");
	        }
        }
        src = s1+enums.toString()+s3;
      }
      else 
        throw new Exception("Instruction <%"+s2+"%> not understood parsing page "+filename);
    }
    return src;
  }  
}
