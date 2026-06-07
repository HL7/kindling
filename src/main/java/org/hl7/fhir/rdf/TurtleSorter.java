package org.hl7.fhir.rdf;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.graph.GraphWrapper;
import org.apache.jena.sparql.util.NodeCmp;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.WrappedIterator;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;


/**
 * Applies deterministic ordering to the generated FHIR Turtle graph, 
 * which is otherwise not offered by Apache Jena.
 * @see org.apache.jena.sparql.graph.GraphWrapper
 */
public class TurtleSorter {

    /**
     * Serialize the given graph as pretty Turtle with deterministic subject ordering
     * and element-axiom ordering driven by the supplied index.
     */
    public static void serialize(
            Graph graph,
            OutputStream destination,
            Map<Node, OrderedClassExpressionOrder> orderedClassExpressionIndex) {
        RDFWriter.source(new SubjectSortedGraph(graph, orderedClassExpressionIndex))
                .format(RDFFormat.TURTLE_PRETTY)
                .output(destination);
    }

    /**
     * Serialize the given graph as pretty Turtle with deterministic subject ordering only.
     * Use this overload when there are no FHIR element-derived axioms to order
     * (for example, the W5 vocabulary).
     */
    public static void serialize(Graph graph, OutputStream destination) {
        serialize(graph, destination, Collections.emptyMap());
    }
    
    /**
     * Used for per-subject ordering of element-derived {@code rdfs:subClassOf} axioms,
     * so a resource class such as {@code fhir:Account} emits its
     * property restrictions in the same order as the corresponding elements in
     * the FHIR StructureDefinition.
     */
    public static final class OrderedClassExpressionOrder {
        private final int elementIndex;
        private final long registrationSequence;

        public OrderedClassExpressionOrder(int elementIndex, long registrationSequence) {
            this.elementIndex = elementIndex;
            this.registrationSequence = registrationSequence;
        }
    }

    /**
     * Primarily sorts the graph of triples by top-level subjects.
     */
    public static final class SubjectSortedGraph extends GraphWrapper {
        private final Map<Node, OrderedClassExpressionOrder> orderedClassExpressionIndex;

        public SubjectSortedGraph(Graph graph, Map<Node, OrderedClassExpressionOrder> orderedClassExpressionIndex) {
            super(graph);
            this.orderedClassExpressionIndex = orderedClassExpressionIndex;
        }

        /**
         * Reorders only the superclass axioms that were explicitly registered as
         * coming from FHIR elements.
         * Performs a stable subset replacement: only the indexed
         * {@code rdfs:subClassOf} triples move, while unrelated triples for the
         * same subject stay where they were.
         */
        private List<Triple> reorderElementRestrictions(List<Triple> subjectTriples) {
            List<Integer> indexedPositions = new ArrayList<>();
            List<Triple> indexedTriples = new ArrayList<>();

            // Scan once to find only the indexed element-derived subclass axioms.
            // This avoids sorting unrelated triples and preserves their original positions.
            for (int index = 0; index < subjectTriples.size(); index++) {
                Triple triple = subjectTriples.get(index);
                if (!RDFS.subClassOf.asNode().equals(triple.getPredicate())) {
                    continue;
                }
                if (!orderedClassExpressionIndex.containsKey(triple.getObject())) {
                    continue;
                }
                indexedPositions.add(index);
                indexedTriples.add(triple);
            }

            if (indexedTriples.isEmpty()) {
                return subjectTriples;
            }

            indexedTriples.sort((left, right) -> {
                OrderedClassExpressionOrder leftOrder = orderedClassExpressionIndex.get(left.getObject());
                OrderedClassExpressionOrder rightOrder = orderedClassExpressionIndex.get(right.getObject());
                int byElementIndex = Integer.compare(leftOrder.elementIndex, rightOrder.elementIndex);
                if (byElementIndex != 0) {
                    return byElementIndex;
                }
                return Long.compare(leftOrder.registrationSequence, rightOrder.registrationSequence);
            });

            // Write the sorted subset back into the original slots while keeping stable ordering for everything else.
            List<Triple> reorderedTriples = new ArrayList<>(subjectTriples);
            for (int index = 0; index < indexedPositions.size(); index++) {
                reorderedTriples.set(indexedPositions.get(index), indexedTriples.get(index));
            }
            return reorderedTriples;
        }

         /**
         * Lazily emits triples subject-by-subject in the precomputed order,
         * avoiding a second full-graph list during serialization.
         */
        private ExtendedIterator<Triple> orderedSubjectTriplesIterator(
                List<Node> orderedSubjects,
                Map<Node, List<Triple>> triplesBySubject) {
            Iterator<Node> subjectIterator = orderedSubjects.iterator();

            return WrappedIterator.create(new Iterator<Triple>() {
                private Iterator<Triple> currentTriples = Collections.emptyIterator();

                @Override
                public boolean hasNext() {
                    while (!currentTriples.hasNext() && subjectIterator.hasNext()) {
                        currentTriples = reorderElementRestrictions(triplesBySubject.get(subjectIterator.next())).iterator();
                    }
                    return currentTriples.hasNext();
                }

                @Override
                public Triple next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return currentTriples.next();
                }
            });
        }

        /**
         * Implementation for {@link GraphWrapper}.
         */
        @Override
        public ExtendedIterator<Triple> find(Node subject, Node predicate, Node object) {
            if (subject == Node.ANY && (predicate != Node.ANY || object != Node.ANY)) {
                return super.find(subject, predicate, object);
            }

            ExtendedIterator<Triple> triples = super.find(subject, predicate, object);

            if (subject != Node.ANY) {
                List<Triple> collectedTriples;
                try {
                    collectedTriples = triples.toList();
                } finally {
                    triples.close();
                }

                // When the writer asks for one subject at a time, still enforce
                // FHIR element order for that resource/type's superclass axioms.
                return WrappedIterator.create(reorderElementRestrictions(collectedTriples).iterator());
            }

            // For whole-graph iteration, first group by subject so we can place
            // ontology metadata first, keep generated disjoint-class helpers near
            // the end, and then apply per-subject FHIR element ordering.
            Node ontologySubject = null;
            Set<Node> allDisjointClassSubjects = new HashSet<>();
            Map<Node, List<Triple>> triplesBySubject = new LinkedHashMap<>();
            
            // Build per-subject buckets and special-case subject metadata in one pass.
            // Precomputing this here keeps the later subject sort cheap and avoids rescanning triples in the comparator.
            try {
                while (triples.hasNext()) {
                    Triple triple = triples.next();
                    Node tripleSubject = triple.getSubject();
                    triplesBySubject.computeIfAbsent(tripleSubject, ignored -> new ArrayList<>()).add(triple);

                    if (RDF.type.asNode().equals(triple.getPredicate()) && OWL2.Ontology.asNode().equals(triple.getObject())) {
                        ontologySubject = tripleSubject;
                    }
                    if (RDF.type.asNode().equals(triple.getPredicate()) && OWL2.AllDisjointClasses.asNode().equals(triple.getObject())) {
                        allDisjointClassSubjects.add(tripleSubject);
                    }
                }
            } finally {
                triples.close();
            }

            final Node orderedOntologySubject = ontologySubject;
            List<Node> orderedSubjects = new ArrayList<>(triplesBySubject.keySet());
            orderedSubjects.sort((left, right) -> {
                // Move ontology declaration to top
                boolean leftIsOntology = orderedOntologySubject != null && orderedOntologySubject.equals(left);
                boolean rightIsOntology = orderedOntologySubject != null && orderedOntologySubject.equals(right);
                if (leftIsOntology != rightIsOntology) {
                    return leftIsOntology ? -1 : 1;
                }

                // Move disjoint axioms to bottom
                boolean leftIsAllDisjoint = allDisjointClassSubjects.contains(left);
                boolean rightIsAllDisjoint = allDisjointClassSubjects.contains(right);
                if (leftIsAllDisjoint != rightIsAllDisjoint) {
                    return leftIsAllDisjoint ? 1 : -1;
                }

                return NodeCmp.compareRDFTerms(left, right);
            });

            // Flatten ordered subject groups lazily so whole-graph writes avoid
            // materializing an extra copy of all triples.
            return orderedSubjectTriplesIterator(orderedSubjects, triplesBySubject);
        }
    }
}
