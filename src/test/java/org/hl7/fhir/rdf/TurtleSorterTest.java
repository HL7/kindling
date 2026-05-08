package org.hl7.fhir.rdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.graph.GraphWrapper;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Test;

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

        assertEquals("No triples should be lost during sorting", original.size(), sorted.size());
        assertTrue("Sorted graph should contain every original triple", sorted.containsAll(original));

        List<Node> subjectOrder = firstEncounteredSubjects(sorted);
        assertEquals("Ontology subject should be emitted first", ontology.asNode(), subjectOrder.get(0));
    }


    /**
     * Tests sorting axioms corresponding to ElementDefinitions
     */
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

        List<Triple> triples = findTriples(sortedGraph, account.asNode(), Node.ANY, Node.ANY);

        assertEquals(model.getGraph().find(account.asNode(), Node.ANY, Node.ANY).toList().size(), triples.size());
        assertEquals(account.asNode(), triples.get(0).getSubject());
        assertEquals("Named superclass triple should remain in its original slot", superclass.asNode(), triples.get(3).getObject());
        assertEquals("Label triple should remain in its original slot", RDFS.label.asNode(), triples.get(4).getPredicate());

        List<Node> orderedRestrictionObjects = indexedRestrictionObjects(triples, index);
        assertEquals(List.of(
                restrictionA.asNode(),
                restrictionB1.asNode(),
                restrictionB2.asNode(),
                restrictionC.asNode()),
                orderedRestrictionObjects);
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