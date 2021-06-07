package org.hl7.fhir.rdf;

import cats.effect.IO;
import es.weso.rdf.PrefixMap;
import es.weso.rdf.RDFBuilder;
import es.weso.rdf.RDFReader;
import es.weso.rdf.jena.RDFAsJenaModel;
import es.weso.rdf.jena.RDFAsJenaModel$;
import es.weso.rdf.locations.Location;
import es.weso.rdf.nodes.IRI;
import es.weso.rdf.triples.RDFTriple;
import es.weso.schema.Result;
import es.weso.schema.Schema;
import es.weso.schema.ShExSchema$;
import org.apache.jena.rdf.model.Model;
import es.weso.rdf.nodes.RDFNode;
import scala.Option;
import scala.collection.immutable.Map;
import scala.collection.immutable.Map$;
import scala.collection.immutable.Set;
import scala.Predef$;
import cats.effect.IO;

import java.nio.file.Files;
import java.nio.file.Paths;
import static cats.effect.unsafe.implicits.*;

public class ShExValidator {

    private final Schema schema;

    public ShExValidator(String schemaFile) throws Exception {
        // load shex from the path
        schema = readSchema(schemaFile);
    }

    public Schema readSchema(String schemaFile) throws Exception {
        // Create a none, see: http://stackoverflow.com/questions/1997433/how-to-use-scala-none-from-java-code
        Option<String> none = Option.apply(null); // Create a none
        String contents = new String(Files.readAllBytes(Paths.get(schemaFile)));
        return ShExSchema$.MODULE$.fromString(contents, "SHEXC", none).unsafeRunSync(global());
    }

    public void validate(Model dataModel) {
        Option<String> noneStr = Option.empty();
        Option<IRI> noneIri = Option.empty();
        Option<RDFBuilder> noneBuilder = Option.empty();
        new java.util.HashMap<RDFNode, Set<Location>>();
        Map<RDFNode, Set<Location>> nodesLocations = Map$.MODULE$.empty() ;
        Map<RDFTriple, Set<Location>> tripleLocations = Map$.MODULE$.empty() ;
        IO<Result> r = null ;
        PrefixMap pm = PrefixMap.empty();
        IO<PrefixMap> ioPm = IO.pure(pm);
        RDFAsJenaModel rdf = RDFAsJenaModel.fromModel(dataModel, noneIri, noneIri, nodesLocations, tripleLocations).unsafeRunSync(global());
        IO<Result> ioResult = schema.validate(rdf, "TARGETDECLS", "", noneStr, noneStr, pm, schema.pm(), noneBuilder);
        Result result = ioResult.unsafeRunSync(global());
        if (!result.isValid()) System.out.println("Not valid");
    }
}
