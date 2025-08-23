package org.hl7.fhir.tools.publisher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.hl7.fhir.definitions.model.Definitions;
import org.hl7.fhir.definitions.model.Example;
import org.hl7.fhir.definitions.model.Invariant;
import org.hl7.fhir.definitions.model.ResourceDefn;
import org.hl7.fhir.definitions.model.SearchParameterDefn;
import org.hl7.fhir.definitions.model.TypeDefn;
import org.hl7.fhir.definitions.validation.XmlValidator;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.PathEngineException;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.elementmodel.Element;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.elementmodel.Manager.FhirFormat;
import org.hl7.fhir.r5.fhirpath.FHIRPathEngine;
import org.hl7.fhir.r5.fhirpath.TypeDetails;
import org.hl7.fhir.r5.fhirpath.IHostApplicationServices;
import org.hl7.fhir.r5.fhirpath.FHIRPathUtilityClasses.FunctionDetails;
import org.hl7.fhir.r5.elementmodel.ObjectConverter;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.model.Enumerations.FHIRVersion;
import org.hl7.fhir.r5.utils.validation.IMessagingServices;
import org.hl7.fhir.r5.utils.validation.IResourceValidator;
import org.hl7.fhir.r5.utils.validation.IValidationPolicyAdvisor;
import org.hl7.fhir.r5.utils.validation.IValidatorResourceFetcher;
import org.hl7.fhir.r5.utils.validation.constants.BestPracticeWarningLevel;
import org.hl7.fhir.r5.utils.validation.constants.BindingKind;
import org.hl7.fhir.r5.utils.validation.constants.ContainedReferenceValidationPolicy;
import org.hl7.fhir.r5.utils.validation.constants.IdStatus;
import org.hl7.fhir.r5.utils.validation.constants.ReferenceValidationPolicy;
import org.hl7.fhir.rdf.ModelComparer;
import org.hl7.fhir.utilities.*;
import org.hl7.fhir.utilities.Logger.LogMessageType;
import org.hl7.fhir.utilities.fhirpath.FHIRPathConstantEvaluationMode;
import org.hl7.fhir.utilities.filesystem.CSFileInputStream;
import org.hl7.fhir.utilities.http.ManagedWebAccess;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueType;
import org.hl7.fhir.utilities.validation.ValidationMessage.Source;
import org.hl7.fhir.validation.ValidatorSettings;
import org.hl7.fhir.validation.instance.InstanceValidator;
import org.hl7.fhir.validation.instance.advisor.BasePolicyAdvisorForFullValidation;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xml.sax.SAXException;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class ExampleInspector implements IValidatorResourceFetcher, IValidationPolicyAdvisor, IHostApplicationServices {

  public static class EValidationFailed extends Exception {
    private static final long serialVersionUID = 1538324138218778487L;
    public EValidationFailed(String arg0) {
      super(arg0);
    }
  }

  private class ExampleHostServices implements IHostApplicationServices {

    @Override
    public List<Base> resolveConstant(FHIRPathEngine engine, Object appContext, String name, FHIRPathConstantEvaluationMode mode) throws PathEngineException {
      return new ArrayList<>();
    }

    @Override
    public TypeDetails resolveConstantType(FHIRPathEngine engine, Object appContext, String name, FHIRPathConstantEvaluationMode mode) throws PathEngineException {
      return null;
    }

    @Override
    public boolean log(String argument, List<Base> focus) {
//      System.out.println("FHIRPath log :"+focus.toString());
      return false;
    }

    @Override
    public FunctionDetails resolveFunction(FHIRPathEngine engine, String functionName) {
      return null;
    }

    @Override
    public TypeDetails checkFunction(FHIRPathEngine engine, Object appContext, String functionName, TypeDetails focus, List<TypeDetails> parameters) throws PathEngineException {
      return null;
    }

    @Override
    public List<Base> executeFunction(FHIRPathEngine engine, Object appContext, List<Base> focus, String functionName, List<List<Base>> parameters) {
      return null;
    }

    @Override
    public Base resolveReference(FHIRPathEngine engine, Object appContext, String url, Base refContext) {
      try {
        String[] s = url.split("/");
        if (s.length != 2 || !definitions.getResources().containsKey(s[0]))
          return null;
        String fn = Utilities.path(rootDir, s[0].toLowerCase()+"-"+s[1]+".xml");
        File f = new File(fn);
        if (!f.exists())
          return null;
        XmlParser xml = new XmlParser();
        return xml.parse(new FileInputStream(f));
      } catch (Exception e) {
        return null;
      }
    }

    @Override
    public boolean conformsToProfile(FHIRPathEngine engine, Object appContext, Base item, String url) throws FHIRException {
      IResourceValidator val = context.newValidator();
      List<ValidationMessage> valerrors = new ArrayList<ValidationMessage>();
      if (item instanceof org.hl7.fhir.r5.model.Resource) {
        val.validate(appContext, valerrors, (org.hl7.fhir.r5.model.Resource) item, url);
        boolean ok = true;
        for (ValidationMessage v : valerrors)
          ok = ok && v.getLevel().isError();
        return ok;
      }
      throw new NotImplementedException("Not done yet (IGPublisherHostServices.conformsToProfile), when item is element");
    }

    @Override
    public ValueSet resolveValueSet(FHIRPathEngine engine, Object appContext, String url) {
      return null;
    }

    @Override
    public boolean paramIsType(String name, int index) {
      return false;
    }
  }
  
  private static final boolean VALIDATE_CONFORMANCE_REFERENCES = true;
  private static final boolean VALIDATE_BY_PROFILE = true;
  private static final boolean VALIDATE_BY_SCHEMATRON = false;
  private static final boolean VALIDATE_BY_JSON_SCHEMA = false;

  private IWorkerContext context;
  private String rootDir;
  private String xsltDir;
  private List<ValidationMessage> errorsInt;
  private List<ValidationMessage> errorsExt;
  private Logger logger;
  private Definitions definitions;
  private boolean byProfile = VALIDATE_BY_PROFILE;
  private boolean bySchematron = VALIDATE_BY_SCHEMATRON;
  private boolean byJsonSchema = VALIDATE_BY_JSON_SCHEMA;
  private ExampleHostServices hostServices;
  
  public ExampleInspector(IWorkerContext context, Logger logger, String rootDir, String xsltDir, List<ValidationMessage> errors, Definitions definitions, FHIRVersion version) throws JsonSyntaxException, FileNotFoundException, IOException {
    super();
    this.context = context;
    this.logger = logger;
    this.rootDir = rootDir;
    this.xsltDir = xsltDir;
    this.errorsExt = errors;
    this.errorsInt = new ArrayList<ValidationMessage>();
    this.definitions = definitions;
    this.version = version;
    hostServices = new ExampleHostServices();
  }

  private XmlValidator xml;
  private InstanceValidator validator;
  private int errorCount = 0;
  private int warningCount = 0;
  private int informationCount = 0;

  private org.everit.json.schema.Schema jschema;
  private FHIRPathEngine fpe;
  private JsonObject jsonLdDefns;

  private FHIRVersion version;
  
  public void prepare() throws Exception {
    validator = new InstanceValidator(context, hostServices, null, null, new ValidatorSettings());
    validator.setSuppressLoincSnomedMessages(true);
    validator.setResourceIdRule(IdStatus.REQUIRED);
    validator.setBestPracticeWarningLevel(BestPracticeWarningLevel.Warning);
    validator.getExtensionDomains().add("http://hl7.org/fhir/us");
    validator.setFetcher(this);
    validator.setAllowExamples(true);
    validator.getSettings().setDebug(false);
    validator.setForPublication(true);
    validator.setPolicyAdvisor(new BasePolicyAdvisorForFullValidation(ReferenceValidationPolicy.CHECK_TYPE_IF_EXISTS));
    
    fpe = new FHIRPathEngine(context);
    fpe.setHostServices(this);
  }

  public void prepare2() throws Exception {
    jsonLdDefns = (JsonObject) new com.google.gson.JsonParser().parse(FileUtilities.fileToString(Utilities.path(rootDir, "fhir.jsonld")));
    xml = new XmlValidator(errorsInt, loadSchemas(), loadTransforms());

    if (VALIDATE_BY_JSON_SCHEMA) {
      String source = FileUtilities.fileToString(Utilities.path(rootDir, "fhir.schema.json"));
      JSONObject rawSchema = new JSONObject(new JSONTokener(source));
      jschema = SchemaLoader.load(rawSchema);
    }

    try {
      checkJsonLd();    
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private void checkJsonLd() throws IOException {
    String s1 = "{\r\n"+
        "  \"@type\": \"fhir:Claim\",\r\n"+
        "  \"@id\": \"http://hl7.org/fhir/Claim/760152\",\r\n"+
        "  \"decimal\": 123.45,\r\n"+
        "  \"@context\": {\r\n"+
        "    \"fhir\": \"http://hl7.org/fhir/\",\r\n"+
        "    \"xsd\": \"http://www.w3.org/2001/XMLSchema#\",\r\n"+
        "    \"decimal\": {\r\n"+
        "      \"@id\": \"fhir:value\",\r\n"+
        "      \"@type\": \"xsd:decimal\"\r\n"+
        "    }\r\n"+
        "  }\r\n"+
      "}\r\n";
    String s2 = "{\r\n"+
        "  \"@type\": \"fhir:Claim\",\r\n"+
        "  \"@id\": \"http://hl7.org/fhir/Claim/760152\",\r\n"+
        "  \"decimal\": \"123.45\",\r\n"+
        "  \"@context\": {\r\n"+
        "    \"fhir\": \"http://hl7.org/fhir/\",\r\n"+
        "    \"xsd\": \"http://www.w3.org/2001/XMLSchema#\",\r\n"+
        "    \"decimal\": {\r\n"+
        "      \"@id\": \"fhir:value\",\r\n"+
        "      \"@type\": \"xsd:decimal\"\r\n"+
        "    }\r\n"+
        "  }\r\n"+
        "}\r\n";
    Model m1 = ModelFactory.createDefaultModel();
    Model m2 = ModelFactory.createDefaultModel();
    m1.read(new StringReader(s1), null, "JSON-LD");
    m2.read(new StringReader(s2), null, "JSON-LD");
    List<String> diffs = new ModelComparer().setModel1(m1, "j1").setModel2(m2, "j2").compare();
    if (!diffs.isEmpty()) {
      System.out.println("not isomorphic");
      for (String s : diffs) {
        System.out.println("  "+s);
      }
    }
  }

  private Map<String, byte[]> loadTransforms() throws FileNotFoundException, IOException {
    Map<String, byte[]> res = new HashMap<String, byte[]>();
    for (String s : new File(xsltDir).list()) {
      if (s.endsWith(".xslt"))
        res.put(s, FileUtilities.fileToBytes(Utilities.path(xsltDir, s)));
    }
    return res;
  }

  private Map<String, byte[]> loadSchemas() throws FileNotFoundException, IOException {
    Map<String, byte[]> res = new HashMap<String, byte[]>();
    res.put("fhir-single.xsd", FileUtilities.fileToBytes(Utilities.path(rootDir, "fhir-single.xsd")));
    res.put("fhir-xhtml.xsd", FileUtilities.fileToBytes(Utilities.path(rootDir, "fhir-xhtml.xsd")));
    res.put("xml.xsd", FileUtilities.fileToBytes(Utilities.path(rootDir, "xml.xsd")));
    for (String s : new File(rootDir).list()) {
      if (s.endsWith(".sch"))
        res.put(s, FileUtilities.fileToBytes(Utilities.path(rootDir, s)));
    }
    return res;
  }

//  static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
//  static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
//  static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

  public void validate(String n, String rt, StructureDefinition profile) {
    if (VALIDATE_BY_PROFILE)
      doValidate(n, rt, profile);
  }
  
  public void validate(String n, String rt) {
    doValidate(n, rt, null);    
  }
  
  public void doValidate(String n, String rt, StructureDefinition profile) {
    errorsInt.clear();
    System.out.print(" validate: " + Utilities.padRight(n, ' ', 50));

    long t = System.currentTimeMillis();
    validator.resetTimes();
    try {
      Element e = validateLogical(Utilities.path(rootDir, n+".json"), profile, FhirFormat.JSON);
//      org.w3c.dom.Element xe = validateXml(Utilities.path(rootDir, n+".xml"), profile == null ? null : profile.getId());

//      validateLogical(Utilities.path(rootDir, n+".json"), profile, FhirFormat.JSON);
//      validateJson(Utilities.path(rootDir, n+".json"), profile == null ? null : profile.getId());
//      validateRDF(Utilities.path(rootDir, n+".ttl"), Utilities.path(rootDir, n+".jsonld"), rt);

//      if (new File(Utilities.path(rootDir, n+".ttl")).exists()) {
//        validateLogical(Utilities.path(rootDir, n+".ttl"), profile, FhirFormat.TURTLE);
//      }

      checkSearchParameters(e, e);
    } catch (Exception e) {
      e.printStackTrace();
      errorsInt.add(new ValidationMessage(Source.InstanceValidator, IssueType.STRUCTURE, -1, -1, n, e.getMessage(), IssueSeverity.ERROR));
    }

    long size = fileSize(n);
    t =  System.currentTimeMillis() - t;
    logger.log(": "+
      Utilities.padLeft(Long.toString(t)+"ms ", ' ', 8)+
      Utilities.padLeft(Utilities.describeSize(size), ' ', 7)+" (" +
      validator.reportTimesShort()+")", LogMessageType.Process);
    for (ValidationMessage m : errorsInt) {
      if (!m.getLevel().equals(IssueSeverity.INFORMATION) && !m.getLevel().equals(IssueSeverity.WARNING)) {
        m.setMessage(n+":: "+m.getLocation()+": "+m.getMessage());
        errorsExt.add(m);
        logger.log(m.getMessage()+" ["+m.getMessageId()+"]", LogMessageType.Error);
      }
      if (m.getLevel() == IssueSeverity.WARNING)
        warningCount++;
      else if (m.getLevel() == IssueSeverity.INFORMATION)
        informationCount++;
      else
        errorCount++;
    }
    Runtime.getRuntime().gc();
  }
 
  private long fileSize(String n) {
    try {
      return new File(Utilities.path(rootDir, n+".xml")).length();
    } catch (IOException e) {
      return 0;
    }
  }

  private Element validateLogical(String f, StructureDefinition profile, FhirFormat fmt) throws Exception {
    Element e = Manager.parseSingle(context, new CSFileInputStream(f), fmt);
    new DefinitionsUsageTracker(definitions).updateUsage(e);
    validator.validate(null, errorsInt, null, e);
    if (profile != null) {
      List<StructureDefinition> list = new ArrayList<StructureDefinition>();
      list.add(profile);
      validator.validate(null, errorsInt, null, e, list);
    }
    return e;
  }


  private org.w3c.dom.Element validateXml(String f, String profile) throws FileNotFoundException, IOException, ParserConfigurationException, SAXException, FHIRException  {
    org.w3c.dom.Element e = xml.checkBySchema(f, false);
    if (VALIDATE_BY_SCHEMATRON) {
      xml.checkBySchematron(f, "fhir-invariants.sch", false);
      if (profile != null && new File(Utilities.path(rootDir, profile+".sch")).exists()) {
        xml.checkBySchematron(f, profile+".sch", false);
      }
    }
    return e;
  }

  private void validateJson(String f, String profile) throws FileNotFoundException, IOException {
    if (VALIDATE_BY_JSON_SCHEMA) {
      JSONObject jo = new JSONObject(new JSONTokener(new CSFileInputStream(f)));
      try {
        jschema.validate(jo);
      } catch (ValidationException e) {
        System.out.println(e.getMessage());
//        e.getCausingExceptions().stream()
//            .map(ValidationException::getMessage)
//            .forEach(System.out::println);
        throw e;
      }
    }
  }

  public void summarise() throws EValidationFailed {
    logger.log("Summary: Errors="+Integer.toString(errorCount)+", Warnings="+Integer.toString(warningCount)+", Information messages="+Integer.toString(informationCount), LogMessageType.Error);
    if (errorCount > 0) {
      throw new EValidationFailed("Resource Examples failed instance validation");
    }
  }


  private void checkSearchParameters(Element xe, Element e) throws FHIRException {
    // test the base
    testSearchParameters(xe, xe.getName(), false);
    testSearchParameters(e);
    
    if (e.fhirType().equals("Bundle")) {
      for (Element be : e.getChildrenByName("entry")) {
        Element res = be.getNamedChild("resource");
        if (res != null)
          testSearchParameters(res);
      }
      // XPath is turned off. We don't really care about this; ust that the xpaths compile, which is otherwise checked
//      // for ZXath, iterating the entries running xpaths takes too long. What we're going to do
//      // is list all the resources, and then evaluate all the paths...
//      Set<String> names = new HashSet<String>();
//      org.w3c.dom.Element child = XMLUtil.getFirstChild(xe);
//      while (child != null) {
//        if (child.getNodeName().equals("entry")) {
//          org.w3c.dom.Element grandchild = XMLUtil.getFirstChild(child);
//          while (grandchild != null) {
//            if (grandchild.getNodeName().equals("resource"))
//              names.add(XMLUtil.getFirstChild(grandchild).getNodeName());
//            grandchild = XMLUtil.getNextSibling(grandchild);
//          }
//        }
//        child = XMLUtil.getNextSibling(child);
//      }
//      for (String name : names)
//        testSearchParameters(xe, name, true);
    }
  }

  private void testSearchParameters(Element e) throws FHIRException {
    ResourceDefn r = definitions.getResources().get(e.fhirType());
    if (r != null) {
      for (SearchParameterDefn sp : r.getSearchParams().values()) {
        if (!Utilities.noString(sp.getExpression())) {
          if (sp.getExpressionNode() == null) {
            sp.setExpressionNode(fpe.parse(sp.getExpression()));
          }
          if (fpe.evaluate(e, sp.getExpressionNode()).size() > 0) {
            sp.setWorks(true);
          }
        }
      }
    }
  }
  
  private void testSearchParameters(Element xe, String rn, boolean inBundle) throws FHIRException {
    ResourceDefn r = definitions.getResources().get(rn);
    for (SearchParameterDefn sp : r.getSearchParams().values()) {
      if (!Utilities.noString(sp.getExpression())) {
        try {
          sp.setTested(true);
          List<Base> nodes = fpe.evaluate(xe, sp.getExpression());
          if (nodes.size() > 0) {
            sp.setWorks(true);
          }
        } catch (Exception e1) {
          throw new FHIRException("Expression \"" + sp.getExpression() + "\" execution failed: " + e1.getMessage(), e1);
        }
      }
    }
  }

  public boolean isByProfile() {
    return byProfile;
  }


  public void setByProfile(boolean byProfile) {
    this.byProfile = byProfile;
  }


  public boolean isBySchematron() {
    return bySchematron;
  }


  public void setBySchematron(boolean bySchematron) {
    this.bySchematron = bySchematron;
  }


  public boolean isByJsonSchema() {
    return byJsonSchema;
  }


  public void setByJsonSchema(boolean byJsonSchema) {
    this.byJsonSchema = byJsonSchema;
  }

  @Override
  public Element fetch(IResourceValidator validator,Object appContext, String url) throws IOException, FHIRException {
    if (url.contains("/_history/")) {
      url = url.substring(0, url.indexOf("/_history"));
    }
    String[] parts = url.split("\\/");
    if (parts.length == 2 && definitions.hasResource(parts[0])) {
      ResourceDefn r = definitions.getResourceByName(parts[0]);
      try {
        for (Example e : r.getExamples()) {
          if (e.getElement() != null) {
            if (e.getElement().fhirType().equals("Bundle")) {
              for (Base b : e.getElement().listChildrenByName("entry")) {
                if (b.getChildByName("resource").hasValues()) {
                  Element res = (Element) b.getChildByName("resource").getValues().get(0);
                  if (res.fhirType().equals(parts[0]) && parts[1].equals(res.getChildValue("id"))) {
                    return res;
                  }
                }
              }
            } else  if (e.getElement().fhirType().equals(parts[0]) && e.getId().equals(parts[1])) {
              return e.getElement();
            }
          }
        }
      } catch (Exception e) {
        throw new FHIRException(e);
      }
      try {
        if (parts[0].equals("StructureDefinition"))  
          return new ObjectConverter(context).convert(context.fetchResourceWithException(StructureDefinition.class, "http://hl7.org/fhir/"+parts[0]+"/"+parts[1]));
        if (parts[0].equals("OperationDefinition")) 
          return new ObjectConverter(context).convert(context.fetchResourceWithException(OperationDefinition.class, "http://hl7.org/fhir/"+parts[0]+"/"+parts[1]));
        if (parts[0].equals("SearchParameter")) 
          return new ObjectConverter(context).convert(context.fetchResourceWithException(SearchParameter.class, "http://hl7.org/fhir/"+parts[0]+"/"+parts[1]));
        if (parts[0].equals("ValueSet"))
          return new ObjectConverter(context).convert(context.fetchResourceWithException(ValueSet.class, "http://hl7.org/fhir/"+parts[0]+"/"+parts[1]));
        if (parts[0].equals("CodeSystem"))
          return new ObjectConverter(context).convert(context.fetchResourceWithException(CodeSystem.class, "http://hl7.org/fhir/"+parts[0]+"/"+parts[1]));
      } catch (Exception e) {
        return null;
      }
      return null;
    } else
      return null;
  }


 
  @Override
  public boolean resolveURL(IResourceValidator validator,Object appContext, String path, String url, String type, boolean canonical, List<CanonicalType> targets) throws IOException, FHIRException {
    if (path.endsWith(".fullUrl"))
      return true;
    if (context.hasResource(org.hl7.fhir.r5.model.Resource.class, url)) {
      return true;
    }
    if (url.startsWith("http://hl7.org/fhir")) {
      if (url.contains("#"))
        url = url.substring(0, url.indexOf("#"));
      String[] parts = url.split("\\/");
      if (parts.length >= 5 &&  definitions.hasResource(parts[4])) {
        if ("DataElement".equals(parts[4]))
          return true;
//        Element res = fetch(validator, appContext, url.substring(20));
        return true; // disable this test. Try again for R4. res != null || Utilities.existsInList(parts[4], "NamingSystem", "CapabilityStatement", "CompartmentDefinition", "ConceptMap");
      } else if (context.fetchCodeSystem(url) != null)
        return true;
      else if (definitions.getMapTypes().containsKey(url))
        return true;
      else if (SIDUtilities.isKnownSID(url) || Utilities.existsInList(url, "http://hl7.org/fhir/ConsentPolicy/opt-in", "http://hl7.org/fhir/ConsentPolicy/opt-out", "http://hl7.org/fhir/api", 
          "http://hl7.org/fhir/terminology-server", "http://hl7.org/fhir/knowledge-repository", "http://hl7.org/fhir/measure-processor"))
        return true;
      else
        return true; // disable this test. Try again for R4
    } else
      return true;
  }

  @Override
  public IValidatorResourceFetcher setLocale(Locale locale) {
    // don't need to do anything here 
    return null;
  }


  @Override
  public byte[] fetchRaw(IResourceValidator validator, String source) throws MalformedURLException, IOException {
    org.hl7.fhir.utilities.http.HTTPResult res = ManagedWebAccess.get(Arrays.asList("web"), source);
    res.checkThrowException();
    return res.getContent();
  }


  @Override
  public CanonicalResource fetchCanonicalResource(IResourceValidator validator, Object appContext, String url) throws URISyntaxException {
    for (CanonicalResource t : context.fetchResourcesByType(CanonicalResource.class)) {
      if (t.getUrl().equals(url)) {
        return t;
      }
    }
    return null;
  }


  @Override
  public boolean fetchesCanonicalResource(IResourceValidator validator, String url) {
    return true;
  }


  @Override
  public ReferenceValidationPolicy policyForReference(IResourceValidator validator, Object appContext, String path, String url, ReferenceDestinationType refType) {
    String[] parts = url.split("\\/");
    if (VALIDATE_CONFORMANCE_REFERENCES) {
      if (Utilities.existsInList(url, "ValueSet/LOINCDepressionAnswersList", "ValueSet/LOINCDifficultyAnswersList", "CodeSystem/npi-taxonomy", "ValueSet/1.2.3.4.5", "StructureDefinition/daf-patient", "ValueSet/zika-affected-area"))
        return ReferenceValidationPolicy.IGNORE;
      if (parts.length == 2 && definitions.hasResource(parts[0])) {
        if (Utilities.existsInList(parts[0], "StructureDefinition", "StructureMap", "DataElement", "CapabilityStatement", "MessageDefinition", "OperationDefinition", "SearchParameter", "CompartmentDefinition", "ImplementationGuide", "CodeSystem", "ValueSet", "ConceptMap", "ExpansionProfile", "NamingSystem"))
          return ReferenceValidationPolicy.CHECK_EXISTS_AND_TYPE;
      }    
    }
    return ReferenceValidationPolicy.CHECK_TYPE_IF_EXISTS;
  }


  @Override
  public ContainedReferenceValidationPolicy policyForContained(IResourceValidator validator,
      Object appContext,
      StructureDefinition structure,
      ElementDefinition element,
      String containerType,
      String containerId,
      Element.SpecialElement containingResourceType,
      String path,
      String url) {
    return ContainedReferenceValidationPolicy.CHECK_VALID;
  }


  @Override
  public EnumSet<CodedContentValidationAction> policyForCodedContent(IResourceValidator validator,
      Object appContext,
      String stackPath,
      ElementDefinition definition,
      StructureDefinition structure,
      BindingKind kind,
      AdditionalBindingPurpose purpose,
      ValueSet valueSet,
      List<String> systems) {
    return EnumSet.allOf(CodedContentValidationAction.class);
  }

  @Override
  public SpecialValidationAction policyForSpecialValidation(IResourceValidator iResourceValidator, Object o, SpecialValidationRule specialValidationRule, String s, Element element, Element element1) {
    throw new NotImplementedException();
  }


  @Override
  public EnumSet<ResourceValidationAction> policyForResource(IResourceValidator validator, Object appContext,
      StructureDefinition type, String path) {
    return EnumSet.allOf(ResourceValidationAction.class);
  }

  @Override
  public EnumSet<ElementValidationAction> policyForElement(IResourceValidator validator, Object appContext,
      StructureDefinition structure, ElementDefinition element, String path) {
    return EnumSet.allOf(ElementValidationAction.class);
  }
  public List<ValidationMessage> getErrors() {
    return errorsInt;
  }


  public boolean testInvariants(String srcDir, ResourceDefn rd, ZipGenerator zip, Set<String> invsTested) throws IOException {
    boolean result = true;
    File testsDir = new File(Utilities.path(srcDir, rd.getName().toLowerCase(), "invariant-tests"));
    if (testsDir.exists()) {
      result = testInvariants(rd, result, testsDir, zip, invsTested);
    }
    return result;
  }

  private boolean testInvariants(ResourceDefn rd, boolean result, File testsDir, ZipGenerator zip, Set<String> invsTested) throws IOException {
    System.out.print(".");
    for (File f : testsDir.listFiles()) {
      zip.addFileName(Utilities.path(rd.getName(), f.getName()), f.getAbsolutePath(), false);
      if (f.getName().endsWith(".json")) {
        invsTested.add(f.getName().substring(0, f.getName().indexOf(".")));
        result = testInvariant(rd, f, FhirFormat.JSON) && result;
      }
      if (f.getName().endsWith(".xml")) {
        invsTested.add(f.getName().substring(0, f.getName().indexOf(".")));
        result = testInvariant(rd, f, FhirFormat.XML) && result;
      }
    }
    return result;
  }


  private boolean testInvariant(ResourceDefn rd, File f, FhirFormat fmt) throws FHIRException, FileNotFoundException {
    String inv = f.getName();
    inv = inv.substring(0, inv.indexOf("."));
    Invariant con = rd.findInvariant(inv);
    if (con == null) {
      con = findInvInAnyDefinition(inv);
    }

    if (con != null) {
      List<ValidationMessage> errs = new ArrayList<>();
      validator.setAllowXsiLocation(true);
      validator.validate(null, errs, new FileInputStream(f), fmt);

      boolean fail = false;
      for (ValidationMessage vm : errs) {
        if (vm.getMessage().contains(inv+":")) {
          fail = true;
        }
      }
      con.setTestOutcome(f.getName().contains(".pass") ? !fail : fail);
      if (!con.getTestOutcome()) {
        System.out.println("Test case '"+f.getName()+"' Invariant failed: "+con.getId());
        for (ValidationMessage vm : errs) {
          System.out.println("  -- "+vm.summary());
        }
      }
      return con.getTestOutcome();
    } else {
      System.out.println("Didn't find invariant for "+f.getName());
      return false;
    }
  }

  private Invariant findInvInAnyDefinition(String inv) {
    for (ResourceDefn r : definitions.getResources().values()) {
      Invariant con = r.findInvariant(inv);
      if (con != null) {
        return con;
      }
    }
    for (TypeDefn r : definitions.getTypes().values()) {
      Invariant con = r.findInvariant(inv);
      if (con != null) {
        return con;
      }
    }
    return null;
  }

  @Override
  public List<Base> resolveConstant(FHIRPathEngine engine, Object appContext, String name, FHIRPathConstantEvaluationMode mode) throws PathEngineException {
    throw new NotImplementedException();
  }

  @Override
  public TypeDetails resolveConstantType(FHIRPathEngine engine, Object appContext, String name, FHIRPathConstantEvaluationMode mode) throws PathEngineException {
    throw new NotImplementedException();
  }

  @Override
  public boolean log(String argument, List<Base> focus) {
    throw new NotImplementedException();
  }

  @Override
  public FunctionDetails resolveFunction(FHIRPathEngine engine, String functionName) {
    throw new NotImplementedException();
  }

  @Override
  public TypeDetails checkFunction(FHIRPathEngine engine, Object appContext, String functionName, TypeDetails focus, List<TypeDetails> parameters) throws PathEngineException {
    throw new NotImplementedException();
  }

  @Override
  public List<Base> executeFunction(FHIRPathEngine engine, Object appContext, List<Base> focus, String functionName, List<List<Base>> parameters) {
    throw new NotImplementedException();
  }

  @Override
  public Base resolveReference(FHIRPathEngine engine, Object appContext, String url, Base refContext) throws FHIRException {
    if (Utilities.charCount(url, '/') == 1) {
     String type = url.substring(0, url.indexOf("/"));
     String id = url.substring(url.indexOf("/")+1);
     ResourceDefn rd = definitions.getResourceByName(type);
     if (rd != null) {
       for (Example ex : rd.getExamples()) {
         if (ex.getId().equals(id)) {
           if (ex.getResource() != null) {
             return ex.getResource();
           } else {
             try {
               return ex.getElement(); // new XmlParser().parse(ex.getXml());
             } catch (Exception e) {
               throw new FHIRException(e);
             }
           }
         }
       }
     }
     return null;
    } else {
      throw new NotImplementedException();
    }
  }

  @Override
  public boolean conformsToProfile(FHIRPathEngine engine, Object appContext, Base item, String url) throws FHIRException {
    throw new NotImplementedException();
  }

  @Override
  public ValueSet resolveValueSet(FHIRPathEngine engine, Object appContext, String url) {
    throw new NotImplementedException();
  }

  @Override
  public boolean paramIsType(String name, int index) {
    throw new NotImplementedException();
  }

  @Override
  public Set<String> fetchCanonicalResourceVersions(IResourceValidator validator, Object appContext, String url) {
    return new HashSet<>();
  }

  @Override
  public List<StructureDefinition> getImpliedProfilesForResource(IResourceValidator validator, Object appContext,
      String stackPath, ElementDefinition definition, StructureDefinition structure, Element resource, boolean valid,
      IMessagingServices msgServices, List<ValidationMessage> messages) {
    return new BasePolicyAdvisorForFullValidation(ReferenceValidationPolicy.CHECK_TYPE_IF_EXISTS).getImpliedProfilesForResource(validator, appContext, stackPath, 
        definition, structure, resource, valid, msgServices, messages);
  }

  @Override
  public boolean isSuppressMessageId(String path, String messageId) {
    return false;
  }

  @Override
  public ReferenceValidationPolicy getReferencePolicy() {
    return ReferenceValidationPolicy.IGNORE;
  }

  @Override
  public IValidationPolicyAdvisor getPolicyAdvisor() {
    return null;
  }

  @Override
  public IValidationPolicyAdvisor setPolicyAdvisor(IValidationPolicyAdvisor policyAdvisor) {
    return null;
  }


}


