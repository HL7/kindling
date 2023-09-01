package org.hl7.fhir.tools.converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.FHIRFormatError;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.Base;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;

public class TerminologyFixer {
  
  public class LoadedCodeSystem {

    private File file;
    private CodeSystem cs;

    public LoadedCodeSystem(File f, CodeSystem cs) {
      this.file = f;
      this.cs = cs;
    }

  }

  public static void main(String[] args) throws FHIRFormatError, FileNotFoundException, IOException {
    new TerminologyFixer().start(new File("/Users/grahamegrieve/work/r5/source"));
  }

  List<LoadedCodeSystem> list = new ArrayList<>();
  Map<String, CodeSystem> tho = new HashMap<>();
  
  private void start(File file) throws FHIRException, IOException  {
    load(file);
    
    NpmPackage npm = new FilesystemPackageCacheManager(true).loadPackage("hl7.terminology");
    for (String s : npm.listResources("CodeSystem")) {
      CodeSystem cs = (CodeSystem) new JsonParser().parse(npm.load("package", s));
      tho.put(cs.getUrl(), cs);
    }

    for (LoadedCodeSystem lcs : list) {
      CodeSystem t = tho.get(lcs.cs.getUrl());
      if (t != null) {
        boolean ok = true;
        System.out.println(lcs.cs.getUrl());
//        if (!Base.compareDeep(t.getIdentifier(), lcs.cs.getIdentifier(), true)) {
//          System.out.println("  identifier: "+t.getIdentifier().toString());
//          System.out.println("          vs: "+lcs.cs.getIdentifier().toString());
//        }
//        if (!Base.compareDeep(t.getVersion(), lcs.cs.getVersion(), true)) {
//          System.out.println("  version: "+t.getVersion().toString()+" vs "+lcs.cs.getVersion().toString());
//        }
//        
//        if (!Base.compareDeep(t.getName(), lcs.cs.getName(), true)) {
//          System.out.println("  Name: "+t.getName().toString()+" vs "+lcs.cs.getName().toString());
//        }
//        
//        if (!Base.compareDeep(t.getTitle(), lcs.cs.getTitle(), true)) {
//          System.out.println("  Title: "+t.getTitle().toString()+" vs "+lcs.cs.getTitle().toString());
//        }
        
        if (!Base.compareDeep(t.getStatusElement(), lcs.cs.getStatusElement(), true)) {
          System.out.println("  Status: "+t.getStatusElement().toString()+" vs "+lcs.cs.getStatusElement().toString());
        }
        
        if (!Base.compareDeep(t.getExperimentalElement(), lcs.cs.getExperimentalElement(), true)) {
          System.out.println("  Experimental: "+t.getExperimentalElement().toString()+" vs "+lcs.cs.getExperimentalElement().toString());
        }
        
        if (!Base.compareDeep(t.getJurisdiction(), lcs.cs.getJurisdiction(), true)) {
          System.out.println("  Jurisdiction: "+t.getJurisdiction().toString()+" vs "+lcs.cs.getJurisdiction().toString());
        }
        
        if (!Base.compareDeep(t.getPurposeElement(), lcs.cs.getPurposeElement(), true)) {
          System.out.println("  purpose: "+t.getPurposeElement().toString()+" vs "+lcs.cs.getPurposeElement().toString());
        }
        
        if (!Base.compareDeep(t.getCopyrightElement(), lcs.cs.getCopyrightElement(), true)) {
          System.out.println("  Copyright: "+t.getCopyrightElement().toString()+" vs "+lcs.cs.getCopyrightElement().toString());
        }
        
        if (!Base.compareDeep(t.getDescriptionElement(), lcs.cs.getDescriptionElement(), true)) {
          System.out.println("  desc: "+t.getDescriptionElement().toString()+" vs "+lcs.cs.getDescriptionElement().toString());
        }
        

        if (!Base.compareDeep(t.getCaseSensitiveElement(), lcs.cs.getCaseSensitiveElement(), true)) {
          System.out.println("  CaseSensitive: "+t.getCaseSensitiveElement().toString()+" vs "+lcs.cs.getCaseSensitiveElement().toString());
        }

        if (!Base.compareDeep(t.getHierarchyMeaningElement(), lcs.cs.getHierarchyMeaningElement(), true)) {
          System.out.println("  hierarchyMeaningElement: "+t.getHierarchyMeaningElement().toString()+" vs "+lcs.cs.getHierarchyMeaningElement().toString());
        }
        
        if (!Base.compareDeep(t.getCompositionalElement(), lcs.cs.getCompositionalElement(), true)) {
          System.out.println("  compositionalElement: "+t.getCompositionalElement().toString()+" vs "+lcs.cs.getCompositionalElement().toString());
        }
        
        if (!Base.compareDeep(t.getVersionNeededElement(), lcs.cs.getVersionNeededElement(), true)) {
          System.out.println("  versionNeeded: "+t.getVersionNeededElement().toString()+" vs "+lcs.cs.getVersionNeededElement().toString());
        }

        if (!Base.compareDeep(t.getContentElement(), lcs.cs.getContentElement(), true)) {
          System.out.println("  content: "+t.getContentElement().toString()+" vs "+lcs.cs.getContentElement().toString());
        }
        
        if (!Base.compareDeep(t.getSupplementsElement(), lcs.cs.getSupplementsElement(), true)) {
          System.out.println("  supplements: "+t.getSupplementsElement().toString()+" vs "+lcs.cs.getSupplementsElement().toString());
        }
        
        if (!Base.compareDeep(t.getCountElement(), lcs.cs.getCountElement(), true)) {
          System.out.println("  count: "+t.getCountElement().toString()+" vs "+lcs.cs.getCountElement().toString());
        }

        if (!Base.compareDeep(t.getFilter(), lcs.cs.getFilter(), true)) {
          System.out.println("  filter: "+t.getFilter().toString()+" vs "+lcs.cs.getFilter().toString());
        }
        
        if (!Base.compareDeep(t.getProperty(), lcs.cs.getProperty(), true)) {
          System.out.println("  property: "+t.getProperty().toString()+" vs "+lcs.cs.getProperty().toString());
          ok = false;
        }
        
        if (!Base.compareDeep(t.getConcept(), lcs.cs.getConcept(), true)) {
          System.out.println("  concept: "+t.getConcept().toString());
          System.out.println("       vs: "+lcs.cs.getConcept().toString());
          ok = false;
        }
        if (ok) {
          lcs.file.delete();
        }
        
//        System.out.println(lcs.file.getName()+": "+lcs.cs.getUrl() +" = x");        
      } else {
//        System.out.println(lcs.file.getName()+": "+lcs.cs.getUrl() +" = nil");        
      }
    }
  }
  
  private void load(File file) throws FHIRException, IOException  {
    for (File f : file.listFiles()) {
      if (f.isDirectory()) {
        if (!f.getName().equals("invariant-tests")) {
          load(f);
        }
      } else if (f.getName().endsWith(".xml")) {
        try {
          Resource res = new XmlParser().parse(new FileInputStream(f));
          if (res instanceof CodeSystem) {
            CodeSystem cs = (CodeSystem) res;
            if (cs.getUrl().startsWith("http://terminology.hl7.org/")) {
              list.add(new LoadedCodeSystem(f, cs));
            }
          }
        } catch (Exception e) {
        }
      } else if (f.getName().endsWith(".json")) {
        try {
          Resource res = new JsonParser().parse(new FileInputStream(f));
          if (res instanceof CodeSystem) {
            CodeSystem cs = (CodeSystem) res;
            if (cs.getUrl().startsWith("http://terminology.hl7.org/")) {
              list.add(new LoadedCodeSystem(f, cs));
            }
          }
        } catch (Exception e) {
        }
      }
    }
//    ini.save();
  }

}
