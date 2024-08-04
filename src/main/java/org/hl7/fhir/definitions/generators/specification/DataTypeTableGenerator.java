package org.hl7.fhir.definitions.generators.specification;

import java.util.Set;

import org.hl7.fhir.definitions.model.ElementDefn;
import org.hl7.fhir.r5.model.Enumerations.FHIRVersion;
import org.hl7.fhir.tools.publisher.PageProcessor;
import org.hl7.fhir.utilities.i18n.RenderingI18nContext;
import org.hl7.fhir.utilities.xhtml.HierarchicalTableGenerator;
import org.hl7.fhir.utilities.xhtml.HierarchicalTableGenerator.TableGenerationMode;
import org.hl7.fhir.utilities.xhtml.HierarchicalTableGenerator.TableModel;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

public class DataTypeTableGenerator extends TableGenerator {
  public DataTypeTableGenerator(String dest, PageProcessor page, String pageName, boolean inlineGraphics, FHIRVersion version, String linkPrefix) throws Exception {    
    super(dest, page, page.getDefinitions().getSrcFile(pageName)+"-definitions.html", inlineGraphics, version, linkPrefix);
  }

  public XhtmlNode generate(ElementDefn e, Set<String> outputTracker, boolean isActive) throws Exception {
    HierarchicalTableGenerator gen = new HierarchicalTableGenerator(new RenderingI18nContext(), dest, inlineGraphics, false);
    TableModel model = gen.initNormalTable("", false, true, e.getName(), isActive, isActive ? TableGenerationMode.XHTML : TableGenerationMode.XML);
    
    model.getRows().add(genElement(e, gen, false, e.getName(), false, "", RenderMode.DATATYPE, true, e.getStandardsStatus(), null, e.isAbstractType(), false));
    
    return gen.generate(model, "", 0, outputTracker);
  }

 
}
