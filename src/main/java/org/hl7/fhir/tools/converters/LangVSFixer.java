package org.hl7.fhir.tools.converters;

import org.hl7.fhir.model.ModelContext;
import org.hl7.fhir.model.core.formats.XmlParser;
import org.hl7.fhir.model.core.ValueSet;
import org.hl7.fhir.model.utilities.formats.OutputStyle;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class LangVSFixer {
  public static void main(String[] args) throws IOException {
    ValueSet vs = (ValueSet) new XmlParser(ModelContext.fullCoreContext()).parse(new FileInputStream("/Users/grahamegrieve/work/r6/source/terminologies/valueset-languages.xml"));
    Map<String, ValueSet.ConceptReferenceComponent> correct = new HashMap<>();
    List<ValueSet.ConceptReferenceComponent> toDelete = new ArrayList<>();


    for (ValueSet.ConceptReferenceComponent cr : vs.getCompose().getIncludeFirstRep().getConceptList()) {
      cr.getDesignationList().removeIf(t -> t == null);
      Collections.sort(cr.getDesignationList(), new VSDesignationSorter());
      if (correct.containsKey(cr.getCode())) {
        System.out.println("duplicate: "+cr.getCode());
        toDelete.add(cr);
        mergeDesignations(correct.get(cr.getCode()), cr);
        Collections.sort(correct.get(cr.getCode()).getDesignationList(), new VSDesignationSorter());
      } else {
        correct.put(cr.getCode(), cr);
      }
    }
    vs.getCompose().getIncludeFirstRep().getConceptList().removeAll(toDelete);
    Collections.sort(vs.getCompose().getIncludeFirstRep().getConceptList(), new VSConceptSorter());
    new XmlParser(ModelContext.fullCoreContext()).setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream("/Users/grahamegrieve/work/r6/source/terminologies/valueset-languages-new.xml"), vs);
  }

  private static void mergeDesignations(ValueSet.ConceptReferenceComponent master, ValueSet.ConceptReferenceComponent deleting) {
    for (ValueSet.ConceptReferenceDesignationComponent t1 : deleting.getDesignationList()) {
      ValueSet.ConceptReferenceDesignationComponent t2 = null;
      for (ValueSet.ConceptReferenceDesignationComponent t3 : master.getDesignationList()) {
          if (t1.getLanguage().equals(t3.getLanguage())) {
            t2 = t3;
          }
      }
      if (t2 == null) {
        master.getDesignationList().add(t1);
      } else if (!t1.getValue().equals(t2.getValue())) {
        System.out.println("language "+t1.getLanguage()+": two values are not the same: "+t2.getValue()+", "+t1.getValue());
      }
    }
  }

  private static class VSConceptSorter implements Comparator<ValueSet.ConceptReferenceComponent> {
    @Override
    public int compare(ValueSet.ConceptReferenceComponent o1, ValueSet.ConceptReferenceComponent o2) {
      return o1.getCode().compareTo(o2.getCode());
    }
  }


  private static class VSDesignationSorter implements Comparator<ValueSet.ConceptReferenceDesignationComponent> {
    @Override
    public int compare(ValueSet.ConceptReferenceDesignationComponent o1, ValueSet.ConceptReferenceDesignationComponent o2) {
      return o1.getLanguage().compareTo(o2.getLanguage());
    }
  }
}
