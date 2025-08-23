package org.hl7.fhir.tools.converters;

import org.hl7.fhir.exceptions.FHIRFormatError;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.extensions.ExtensionDefinitions;
import org.hl7.fhir.r5.extensions.ExtensionUtilities;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.CompartmentDefinition;
import org.hl7.fhir.r5.model.ElementDefinition;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.utilities.FileUtilities;
import org.hl7.fhir.utilities.IniFile;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.json.model.JsonArray;
import org.hl7.fhir.utilities.json.model.JsonObject;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlComposer;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MaturityEvaluator {

  private static final int COL_COUNT = 11;
  private Map<String, JsonObject> edgesMap = new HashMap<>();

  public static void main(String[] args) throws FHIRFormatError, IOException, XmlPullParserException, SAXException, ParserConfigurationException, SQLException {
    new MaturityEvaluator().execute();
  }

  private class StructureDefinitionAnalysis {
    private final StructureDefinition sd;
    private List<StructureDefinition> usersSingle = new ArrayList<>();
    private List<StructureDefinition> usersGroup = new ArrayList<>();
    private List<String> compartments = new ArrayList<>();
    private int jira;
    private int profileCount;
    private int score;
    private int instances;
    private int commits;

    public StructureDefinitionAnalysis(StructureDefinition sd) {
      this.sd = sd;
    }

    public String highestFMMRef() {
      String res = null;
      String src = null;
      for (StructureDefinition sd : usersSingle) {
        String fmm = ExtensionUtilities.readStringExtension(sd, ExtensionDefinitions.EXT_FMM_LEVEL);
        if (res == null) {
          res = fmm;
          src = sd.getType();
        } else if (fmm.compareTo(res) > 0) {
          res = fmm;
          src = sd.getType();
        }
      }
      for (StructureDefinition sd : usersGroup) {
        String fmm = ExtensionUtilities.readStringExtension(sd, ExtensionDefinitions.EXT_FMM_LEVEL);
        if (res == null) {
          res = fmm;
          src = sd.getType();
        } else if (fmm.compareTo(res) > 0) {
          res = fmm;
          src = sd.getType();
        }
      }
      return res == null ? "--" : res+" ("+src+")";
    }

    public void score(int jira, int profiles, int instances, int commits) {
      this.jira = jira;
      this.profileCount = profiles;
      this.instances = instances;
      this.commits = commits;
      int score = Utilities.parseInt(ExtensionUtilities.readStringExtension(sd, ExtensionDefinitions.EXT_FMM_LEVEL), 0) * 10;
      score += countLinks(this.usersSingle, 1);
      score += countLinks(this.usersSingle, 0.4);
      score += compartments.size() * 3;
      score += profileCount * 5;
      score += instances * 2;
      score += commits / 10;
      score += jira;
      this.score = score;
    }

    private int countLinks(List<StructureDefinition> list, double i) {
      int score = 0;
      for (StructureDefinition sd : usersSingle) {
        int fmm = Utilities.parseInt(ExtensionUtilities.readStringExtension(sd, ExtensionDefinitions.EXT_FMM_LEVEL), 0);
        score =+ fmm;
      }
      return (int) Math.round(score * i);
    }

    public String recommendation() {
      if (score <= 52) {
        return "AdditionalResource";
      }
      if (score <= 110) {
        return "Consult Committee";
      }
      return "Main Specification";
    }
  }



  private void execute() throws IOException, SQLException {
    System.out.println("Loading structure definitions...");
    IniFile ini = new IniFile("/Users/grahamegrieve/work/r6/source/fhir.ini");
    IniFile iniJira = new IniFile("/Users/grahamegrieve/work/r6/source/jira-count.ini");

    FilesystemPackageCacheManager pcm = new FilesystemPackageCacheManager.Builder().build();
    NpmPackage r6 = pcm.loadPackage("hl7.fhir.r6.core", "dev");
    SimpleWorkerContext ctxt = new SimpleWorkerContext.SimpleWorkerContextBuilder().withAllowLoadingDuplicates(true).fromPackage(r6);
    Map<String, StructureDefinitionAnalysis> resMap = new HashMap<>();
    List<StructureDefinition> reslist = new ArrayList<>();
    Connection xig = DriverManager.getConnection("jdbc:sqlite:/Users/grahamegrieve/work/nodeserver/xig/data/xig.db");

    for (StructureDefinition sd : ctxt.fetchResourcesByType(StructureDefinition.class)) {
      if (sd.getDerivation() == StructureDefinition.TypeDerivationRule.SPECIALIZATION) {
        StructureDefinitionAnalysis analysis = new StructureDefinitionAnalysis(sd);
        resMap.put(sd.getUrl(), analysis);
        resMap.put(sd.getType(), analysis);
        reslist.add(sd);
      }
    }
    System.out.println("Processing...");

    for (StructureDefinition sd : reslist) {
      for (ElementDefinition ed : sd.getSnapshot().getElement()) {
        for (ElementDefinition.TypeRefComponent t : ed.getType()) {
          for (CanonicalType cr : t.getTargetProfile()) {
            StructureDefinitionAnalysis analysis = resMap.get(cr.primitiveValue());
            if (t.getTargetProfile().size() == 1) {
              analysis.usersSingle.add(sd);
            } else {
              analysis.usersGroup.add(sd);
            }
          }
        }
      }
    }

    for (String n : r6.listResources("CompartmentDefinition")) {
      CompartmentDefinition cd = (CompartmentDefinition) new JsonParser().parse(r6.loadResource(n));
      for (CompartmentDefinition.CompartmentDefinitionResourceComponent r : cd.getResource()) {
        if (r.getParam().size() > 0) {
          StructureDefinitionAnalysis analysis = resMap.get(r.getCode());
          analysis.compartments.add(cd.getCode().toCode());
        }
      }
    }

    for (StructureDefinitionAnalysis analysis : resMap.values()) {
      Integer jira = iniJira.getIntegerProperty("jira", analysis.sd.getType());
      int j = jira == null ? 0 : jira;
      Integer git = iniJira.getIntegerProperty("git", analysis.sd.getType().toLowerCase());
      int g = git == null ? 0 : git;

      Integer prof = iniJira.getIntegerProperty("s-profiles", analysis.sd.getType());
      int p = prof == null ? 0 : prof;
      Integer inst = iniJira.getIntegerProperty("s-instances", analysis.sd.getType());
      int i = inst == null ? 0 : inst;
      analysis.score(j, countProfiles(analysis.sd.getType(), xig)+p, countInstances(analysis.sd.getType(), xig)+i, g);
    }

    System.out.println("Saving..");

    JsonArray nodes = new JsonArray();
    JsonArray edges = new JsonArray();
    for (StructureDefinition sd : reslist) {
      JsonObject node = new JsonObject();
      nodes.add(node);
      node.add("key", sd.getType());
      node.add("category", ExtensionUtilities.readStringExtension(sd, ExtensionDefinitions.EXT_RESOURCE_CATEGORY));
      node.add("maturity", ExtensionUtilities.readStringExtension(sd, ExtensionDefinitions.EXT_FMM_LEVEL));
      node.add("description", sd.getDescription());
      if (resMap.containsKey(sd.getBaseDefinition())) {
//        node.add("baseResource", resMap.get(sd.getBaseDefinition()).sd.getType());
        addLink(edges, sd.getType(), resMap.get(sd.getBaseDefinition()).sd.getType(), "base", "1..1", "single", "BaseDefinition");
      }
//      node.add("isAbstract", sd.getAbstract());


      for (ElementDefinition ed : sd.getSnapshot().getElement()) {
        for (ElementDefinition.TypeRefComponent t : ed.getType()) {
          for (CanonicalType cr : t.getTargetProfile()) {
            addLink(edges, sd.getType(), resMap.get(cr.primitiveValue()).sd.getType(), ed.getName(), ""+ed.getMin()+".."+ed.getMax(), t.getTargetProfile().size() == 1 ? "single" : "choice", ed.getDefinition());
          }
        }
      }
    }
    String sNodes = org.hl7.fhir.utilities.json.parser.JsonParser.compose(nodes, true);
    String sEdges = org.hl7.fhir.utilities.json.parser.JsonParser.compose(edges, true);

    XhtmlNode tbl = new XhtmlNode(NodeType.Element, "table").clss("grid");
    XhtmlNode tr = tbl.tr();
    tr.th().tx("Resource");
    tr.th().tx("WG");
    tr.th().tx("# Refs");
    tr.th().tx("# Grp Refs");
    tr.th().tx("Compartments");
    tr.th().tx("Profiles");
    tr.th().tx("Instances");
    tr.th().tx("Jira Tasks");
    tr.th().tx("Commits");
    tr.th().tx("Score");
    tr.th().tx("Recommendation");

    Collections.sort(reslist, new FmmSorter());
    String fmmLast = null;

    for (StructureDefinition sd : reslist) {
      if (sd.getKind() == StructureDefinition.StructureDefinitionKind.RESOURCE && !sd.getAbstract()) {
        StructureDefinitionAnalysis analysis = resMap.get(sd.getUrl());

        String fmm = ExtensionUtilities.readStringExtension(sd, ExtensionDefinitions.EXT_FMM_LEVEL);
        if ("4".equals(fmm)) {
          break;
        }
        if (!fmm.equals(fmmLast)) {
          tbl.tr().style("background-color: #DDDDDD").td().colspan(COL_COUNT).b().tx("FMM " + fmm);
          fmmLast = fmm;
        }
        makeRow(sd, tbl, analysis, ini, resMap);
      }
    }
    String table1 = new XhtmlComposer(false, true).compose(tbl);

    tbl = new XhtmlNode(NodeType.Element, "table").clss("grid");
    tr = tbl.tr();
    tr.th().tx("Resource");
    tr.th().tx("WG");
    tr.th().tx("# Refs");
    tr.th().tx("# Grp Refs");
    tr.th().tx("Compartments");
    tr.th().tx("Profiles");
    tr.th().tx("Instances");
    tr.th().tx("Jira Tasks");
    tr.th().tx("Commits");
    tr.th().tx("Score");
    tr.th().tx("Recommendation");

    Collections.sort(reslist, new ScoreSorter(resMap));
    String decisionLast = null;

    for (StructureDefinition sd : reslist) {
      if (sd.getKind() == StructureDefinition.StructureDefinitionKind.RESOURCE && !sd.getAbstract()) {
        StructureDefinitionAnalysis analysis = resMap.get(sd.getUrl());

        int fmm = Integer.parseInt(ExtensionUtilities.readStringExtension(sd, ExtensionDefinitions.EXT_FMM_LEVEL));
        if (fmm >= 4) {
          continue;
        }
        String decision = analysis.recommendation();
        if (!decision.equals(decisionLast)) {
          tbl.tr().style("background-color: #DDDDDD").td().colspan(COL_COUNT).b().tx(decision);
          decisionLast = decision;
        }
        makeRow(sd, tbl, analysis, ini, resMap);
      }
    }
    String table2 = new XhtmlComposer(false, true).compose(tbl);
    String src = FileUtilities.fileToString("/Users/grahamegrieve/temp/maturity-evaluation-template.html");
    for (StructureDefinition sd : reslist) {
      if (sd.getKind() == StructureDefinition.StructureDefinitionKind.RESOURCE && !sd.getAbstract()) {
        src = src.replace(sd.getType(), "<a href=\"http://build.fhir.org/" + sd.getType().toLowerCase() + ".html\">" + sd.getType() + "</a>");
      }
    }
    src = src.replace("$table1$", table1);
    src = src.replace("$table2$", table2);
    src = src.replace("$nodes$", sNodes);
    src = src.replace("$edges$", sEdges);
    FileUtilities.stringToFile(src, "/Users/grahamegrieve/temp/maturity-evaluation.html");
    FileUtilities.stringToFile(sNodes, "/Users/grahamegrieve/temp/maturity-nodes.json");
    FileUtilities.stringToFile(sEdges, "/Users/grahamegrieve/temp/maturity-edges.json");
    System.out.println("Done.");
  }

  private void addLink(JsonArray edges, String source, String target, String property, String cardinality, String type, String description) {
    int weight = "choice".equals(type) ? 2 : "Resource".equals(target) ? 0 : 5;
    if (edgesMap.containsKey(source+":"+target)) {
      JsonObject link = edgesMap.get(source+":"+target);
      link.set("weight", weight+link.getJsonNumber("weight").getInteger());
    } else {
      JsonObject link = new JsonObject();
      edges.add(link);
      link.add("source", source);
      link.add("target", target);
      link.add("relationship", property);
      //link.add("cardinality", cardinality);
      link.add("required", cardinality.startsWith("1"));
      link.add("weight", weight);
//    link.add("description", description);
      edgesMap.put(source+":"+target, link);
    }
  }

  private static void makeRow(StructureDefinition sd, XhtmlNode tbl, StructureDefinitionAnalysis analysis, IniFile ini, Map<String, StructureDefinitionAnalysis> resMap) {
    XhtmlNode tr;
    tr = tbl.tr();
    tr.td().ah("http://build.fhir.org/"+sd.getType().toLowerCase() + ".html").tx(sd.getType());
    tr.td().tx(ExtensionUtilities.readStringExtension(sd, ExtensionDefinitions.EXT_WORKGROUP));
    listRefs(analysis.usersSingle, sd.getType(), tr, ini, resMap);
    listRefs(analysis.usersGroup, sd.getType(), tr, ini, resMap);
    XhtmlNode td = tr.td();
    for (String s : analysis.compartments) {
      td.sep(", ");
      String tla = ini.getStringProperty("tla", s);
      td.ah("http://build.fhir.org/"+s.toLowerCase() + ".html", s).tx(tla);
    }

    tr.td().ahOrNot(analysis.profileCount == 0 ? null : "https://packages2.fhir.org/xig?type=rp&rt="+ sd.getType()+"&text=").tx(analysis.profileCount);
    tr.td().tx(analysis.instances);
    tr.td().tx(analysis.jira);
    tr.td().tx(analysis.commits);
    tr.td().tx(analysis.score);
    tr.td().tx(analysis.recommendation());
  }

  private int countProfiles(String type, Connection db) throws SQLException {
    ResultSet rs = db.createStatement().executeQuery("select count(*) from Resources where Type = '"+type+"'");
    return rs.getInt(1);
  }
  private int countInstances(String type, Connection db) throws SQLException {
    ResultSet rs = db.createStatement().executeQuery("select count(*) from Resources where ResourceType = '"+type+"'");
    return rs.getInt(1);
  }

  private static void listRefs(List<StructureDefinition> list, String type, XhtmlNode tr, IniFile ini, Map<String, StructureDefinitionAnalysis> map) {
    Set<String> ul = new HashSet<>();
    for (StructureDefinition t : list) {
      if (!t.getType().equals(type)) {
        ul.add(t.getType());
      }
    }
    XhtmlNode td = tr.td();
    int i = 0;
    for (String s : Utilities.sorted(ul)) {
      i++;
      if (i % 4 == 0) {
        td.sepBr();
      }
      String tla = ini.getStringProperty("tla", s);
      if (Utilities.noString(tla)) {
        System.out.println("no TLA: "+s);
      }
      StructureDefinition sd = map.get(s).sd;
      String fmm = ExtensionUtilities.readStringExtension(sd, ExtensionDefinitions.EXT_FMM_LEVEL);
      XhtmlNode a = td.ah("http://build.fhir.org/"+s.toLowerCase() + ".html", s);
      a.tx(tla);
      a.tx( " ");
      a.b().tx(fmm);
    }
  }

  private class FmmSorter implements Comparator<StructureDefinition> {
    @Override
    public int compare(StructureDefinition o1, StructureDefinition o2) {
      String fmm1 = ExtensionUtilities.readStringExtension(o1, ExtensionDefinitions.EXT_FMM_LEVEL);
      String fmm2 = ExtensionUtilities.readStringExtension(o2, ExtensionDefinitions.EXT_FMM_LEVEL);
      if (fmm1 == null) {
        fmm1 = "0";
      }
      if (fmm2 == null) {
        fmm2 = "0";
      }
      if (fmm1.equals(fmm2)) {
        String wg1 = ExtensionUtilities.readStringExtension(o1, ExtensionDefinitions.EXT_WORKGROUP);
        String wg2 = ExtensionUtilities.readStringExtension(o2, ExtensionDefinitions.EXT_WORKGROUP);
        if (!wg1.equals(wg2)) {
          return wg1.compareTo(wg2);
        } else {
          return o1.getType().compareTo(o2.getType());
        }
      } else {
        return fmm1.compareTo(fmm2);
      }
    }
  }

  private class ScoreSorter implements Comparator<StructureDefinition> {
    Map<String, StructureDefinitionAnalysis> map;

    public ScoreSorter(Map<String, StructureDefinitionAnalysis> resMap) {
      this.map = resMap;
    }

    @Override
    public int compare(StructureDefinition o1, StructureDefinition o2) {
      int score1 = map.get(o1.getType()).score;
      int score2 = map.get(o2.getType()).score;
      return Integer.compare(score1, score2);
    }
  }
}
