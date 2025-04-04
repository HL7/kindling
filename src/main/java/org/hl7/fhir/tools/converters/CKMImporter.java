package org.hl7.fhir.tools.converters;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.hl7.fhir.utilities.FileUtilities;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.http.HTTPResult;
import org.hl7.fhir.utilities.http.ManagedWebAccess;
import org.hl7.fhir.utilities.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class CKMImporter {

	private String ckm;
	private String dest;
	private String config;
	private String info;

	public static void main(String[] args) throws Exception {
		CKMImporter self = new CKMImporter();
  	self.ckm = getParam(args, "ckm");
  	self.dest = getParam(args, "dest");
  	self.config = getParam(args, "config");
  	self.info = getParam(args, "info");
	  if (self.ckm == null || self.dest == null || self.config == null) {
	  	System.out.println("ADL to FHIR StructureDefinition Converter");
	  	System.out.println("This tool takes 4 parameters:");
	  	System.out.println("-ckm: Baase URL of CKM");
	  	System.out.println("-dest: folder for output");
	  	System.out.println("-config: filename of OpenEHR/FHIR knowlege base (required)");
	  	System.out.println("-info: folder for additional knowlege of archetypes");
	  } else {
	  	self.execute();
	  }
	}

	private static String getParam(String[] args, String name) {
	  for (int i = 0; i < args.length - 1; i++) {
	  	if (args[i].equals("-"+name)) {
	  		return args[i+1];
	  	}
	  }
	  return null;
	}


	private void execute() throws Exception {
		List<String> ids = new ArrayList<String>();
		Document xml = loadXml(ckm + "/services/ArchetypeFinderBean/getAllArchetypeIds");
		Element e = XMLUtil.getFirstChild(xml.getDocumentElement());
		while (e != null) {
			ids.add(e.getTextContent());
			e = XMLUtil.getNextSibling(e);
		}
//		for (String id : ids) {
//			downloadArchetype(id);
//		}
		for (String id : ids) {
			processArchetype(id);
		}
	}

	private void downloadArchetype(String id) throws Exception {
	  String destFn = Utilities.path(dest, id+".xml");
	  if (new File(destFn).exists())
	    System.out.println(id+" already fetched");
	  else {
      System.out.println("Fetch "+id);
  		Document sxml = loadXml(ckm+"/services/ArchetypeFinderBean/getArchetypeInXML?archetypeId="+id);
  		Element e = XMLUtil.getFirstChild(sxml.getDocumentElement());
  		String src = Utilities.path("[tmp]", id+".xml");
  		FileUtilities.stringToFile(e.getTextContent(), src);
	  }
	}

	private void processArchetype(String id) throws Exception {
		
		String cfg = info == null ? null : Utilities.path(info, id+".config");
		String src = Utilities.path(dest, id+".xml");
		String dst = Utilities.path(dest, id.substring(id.indexOf(".")+1)+".xml");

		if (new File(dst).exists())
	    System.out.println(id+" Already Processed");
		else {
		  System.out.print("Process "+id+": ");
  		try {  
  		if (!new File(src).exists())
  			downloadArchetype(id);
  		if (cfg != null && new File(cfg).exists())
  			ADLImporter.main(new String[] {"-source", src, "-dest", dst, "-config", config, "-info", cfg});
  		else
  			ADLImporter.main(new String[] {"-source", src, "-dest", dst, "-config", config});
        System.out.println("  ok");
  		} catch (Exception e) {
        System.out.println("  error - "+e.getMessage());
  		}
		}
	}

	private Document loadXml(String address) throws Exception {
	  HTTPResult res = ManagedWebAccess.get(Arrays.asList("web"), "application/xml");
    res.checkThrowException();
    InputStream xml = new ByteArrayInputStream(res.getContent());

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		return db.parse(xml);
	}
}
