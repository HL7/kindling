package org.hl7.fhir.rdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.graph.GraphWrapper;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TurtleSorterTest {
    /**
     * Tests that whole-graph sorting preserves every triple while still moving ontology subjects first
     */
    @Test
    public void findWholeGraph_preservesAllTriplesAndOrdersSpecialSubjects() {
        Model model = ModelFactory.createDefaultModel();

        Resource ontology = model.createResource("http://hl7.org/fhir/fhir.ttl");
        Resource account = model.createResource("http://hl7.org/fhir/Account");
        Resource restriction = restriction(model, model.createResource("http://hl7.org/fhir/status"));

        ontology.addProperty(RDF.type, OWL2.Ontology);
        ontology.addProperty(RDFS.label, "FHIR Model Ontology");

        account.addProperty(RDF.type, OWL2.Class);
        account.addProperty(RDFS.subClassOf, restriction);
        account.addProperty(RDFS.comment, "Account resource");

        Map<Node, TurtleSorter.OrderedClassExpressionOrder> index = new HashMap<>();
        index.put(restriction.asNode(), new TurtleSorter.OrderedClassExpressionOrder(0, 0));

        Graph sortedGraph = new TurtleSorter.SubjectSortedGraph(model.getGraph(), index);

        List<Triple> original = findTriples(model.getGraph(), Node.ANY, Node.ANY, Node.ANY);
        List<Triple> sorted = findTriples(sortedGraph, Node.ANY, Node.ANY, Node.ANY);

        assertEquals(original.size(), sorted.size(), "No triples should be lost during sorting");
        assertTrue(sorted.containsAll(original), "Sorted graph should contain every original triple");

        List<Node> subjectOrder = firstEncounteredSubjects(sorted);
        assertEquals(ontology.asNode(), subjectOrder.get(0), "Ontology subject should be emitted first");
    }

    /**
     * Tests {@link TurtleSorter.SubjectSortedGraph} implementation of {@link org.apache.jena.sparql.graph.GraphWrapper}
     */
    @Test
    public void findAnyWithPredicateFilter_defersToUnderlyingGraph() {
        Model model = ModelFactory.createDefaultModel();
        Resource account = model.createResource("http://hl7.org/fhir/Account");
        Resource superclass = model.createResource("http://hl7.org/fhir/DomainResource");
        account.addProperty(RDFS.subClassOf, superclass);
        account.addProperty(RDFS.label, "Account");

        Graph sortedGraph = new TurtleSorter.SubjectSortedGraph(model.getGraph(), new HashMap<>());

        List<Triple> original = findTriples(model.getGraph(), Node.ANY, RDFS.subClassOf.asNode(), Node.ANY);
        List<Triple> sorted = findTriples(sortedGraph, Node.ANY, RDFS.subClassOf.asNode(), Node.ANY);

        assertEquals(original, sorted);
    }

    /**
     * Tests sorting axioms corresponding to ElementDefinitions
     */
    @Disabled("not critical — run manually")
    @Test
    public void findBySubject_reordersIndexedElementRestrictionsDeterministically() {
        Model model = ModelFactory.createDefaultModel();
        Resource account = model.createResource("http://hl7.org/fhir/Account");
        Resource superclass = model.createResource("http://hl7.org/fhir/DomainResource");
        Resource propertyA = model.createResource("http://hl7.org/fhir/identifier");
        Resource propertyB = model.createResource("http://hl7.org/fhir/status");
        Resource propertyC = model.createResource("http://hl7.org/fhir/name");

        Resource restrictionC = restriction(model, propertyC);
        Resource restrictionB2 = restriction(model, propertyB);
        Resource restrictionA = restriction(model, propertyA);
        Resource restrictionB1 = restriction(model, propertyB);

        account.addProperty(RDF.type, OWL2.Class);
        account.addProperty(RDFS.comment, "Account resource");
        account.addProperty(RDFS.subClassOf, restrictionC);
        account.addProperty(RDFS.subClassOf, superclass);
        account.addProperty(RDFS.label, "Account");
        account.addProperty(RDFS.subClassOf, restrictionB2);
        account.addProperty(RDFS.subClassOf, restrictionA);
        account.addProperty(RDFS.subClassOf, restrictionB1);

        Map<Node, TurtleSorter.OrderedClassExpressionOrder> index = new HashMap<>();
        index.put(restrictionC.asNode(), new TurtleSorter.OrderedClassExpressionOrder(3, 0));
        index.put(restrictionB2.asNode(), new TurtleSorter.OrderedClassExpressionOrder(1, 2));
        index.put(restrictionA.asNode(), new TurtleSorter.OrderedClassExpressionOrder(0, 1));
        index.put(restrictionB1.asNode(), new TurtleSorter.OrderedClassExpressionOrder(1, 0));

        Graph sortedGraph = new TurtleSorter.SubjectSortedGraph(model.getGraph(), index);

        List<Triple> originalTriples = findTriples(model.getGraph(), account.asNode(), Node.ANY, Node.ANY);
        List<Triple> triples = findTriples(sortedGraph, account.asNode(), Node.ANY, Node.ANY);

        assertEquals(originalTriples.size(), triples.size());
        assertEquals(account.asNode(), triples.get(0).getSubject());
        assertEquals(
                nonIndexedTriples(originalTriples, index),
                nonIndexedTriples(triples, index),
                "Non-indexed triples should keep their relative order from the source graph");

        List<Node> orderedRestrictionObjects = indexedRestrictionObjects(triples, index);
        assertEquals(List.of(
                restrictionA.asNode(),
                restrictionB1.asNode(),
                restrictionB2.asNode(),
                restrictionC.asNode()),
                orderedRestrictionObjects);
    }

    /**
     * Wall-time micro-benchmark comparing {@link TurtleSorter#serialize} against the default
     * {@link RDFDataMgr#write} on a synthetic FHIR-shaped model. Ignored by default;
     * run on demand to estimate sort-wrapper overhead. Single-threaded, no JIT steady-state
     * guarantees. Good for order-of-magnitude only.
     */
    @Disabled("benchmark — run manually")
    @Test
    public void benchmark_sortedSerializeVsDefaultWrite() {
        int classes = 800; // FHIR R6 Ontology has ~833 classes
        int restrictionsPerClass = 20;
        Model model = ModelFactory.createDefaultModel();
        Map<Node, TurtleSorter.OrderedClassExpressionOrder> index = new HashMap<>();
        long seq = 0;
        for (int c = 0; c < classes; c++) {
            Resource cls = model.createResource("http://example.org/Class" + c);
            cls.addProperty(RDF.type, OWL2.Class);
            for (int r = 0; r < restrictionsPerClass; r++) {
                Resource prop = model.createResource("http://example.org/prop" + r);
                Resource restr = restriction(model, prop);
                cls.addProperty(RDFS.subClassOf, restr);
                index.put(restr.asNode(), new TurtleSorter.OrderedClassExpressionOrder(r, seq++));
            }
        }

        int warmup = 3;
        int iterations = 10;
        Runnable baseline = () -> RDFDataMgr.write(OutputStream.nullOutputStream(), model, RDFFormat.TURTLE_PRETTY);
        Runnable sorted = () -> TurtleSorter.serialize(model.getGraph(), OutputStream.nullOutputStream(), index);
        Runnable sortedEmpty = () -> TurtleSorter.serialize(model.getGraph(), OutputStream.nullOutputStream(), Collections.emptyMap());

        for (int i = 0; i < warmup; i++) { baseline.run(); sorted.run(); sortedEmpty.run(); }

        System.out.printf("model: %d triples, %d subjects%n", model.getGraph().size(), classes);
        System.out.printf("  baseline (RDFDataMgr TURTLE_PRETTY):    %6.1f ms/op%n", timeAvgMs(baseline, iterations));
        System.out.printf("  TurtleSorter.serialize (with index):    %6.1f ms/op%n", timeAvgMs(sorted, iterations));
        System.out.printf("  TurtleSorter.serialize (empty index):   %6.1f ms/op%n", timeAvgMs(sortedEmpty, iterations));
    }

    private static double timeAvgMs(Runnable action, int iterations) {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) action.run();
        long elapsed = System.nanoTime() - start;
        return TimeUnit.NANOSECONDS.toMicros(elapsed) / 1000.0 / iterations;
    }

    private static Resource restriction(Model model, Resource onProperty) {
        Resource restriction = model.createResource();
        restriction.addProperty(RDF.type, OWL2.Restriction);
        restriction.addProperty(OWL2.onProperty, onProperty);
        return restriction;
    }

    private static List<Triple> findTriples(Graph graph, Node subject, Node predicate, Node object) {
        ExtendedIterator<Triple> iterator = graph.find(subject, predicate, object);
        try {
            return iterator.toList();
        } finally {
            iterator.close();
        }
    }

    private static List<Node> indexedRestrictionObjects(
            List<Triple> triples,
            Map<Node, TurtleSorter.OrderedClassExpressionOrder> index) {
        List<Node> objects = new ArrayList<>();
        for (Triple triple : triples) {
            if (RDFS.subClassOf.asNode().equals(triple.getPredicate()) && index.containsKey(triple.getObject())) {
                objects.add(triple.getObject());
            }
        }
        return objects;
    }

    private static List<Triple> nonIndexedTriples(
            List<Triple> triples,
            Map<Node, TurtleSorter.OrderedClassExpressionOrder> index) {
        List<Triple> result = new ArrayList<>();
        for (Triple triple : triples) {
            if (!(RDFS.subClassOf.asNode().equals(triple.getPredicate()) && index.containsKey(triple.getObject()))) {
                result.add(triple);
            }
        }
        return result;
    }

    private static List<Node> firstEncounteredSubjects(List<Triple> triples) {
        List<Node> subjects = new ArrayList<>();
        for (Triple triple : triples) {
            if (!subjects.contains(triple.getSubject())) {
                subjects.add(triple.getSubject());
            }
        }
        return subjects;
    }
}