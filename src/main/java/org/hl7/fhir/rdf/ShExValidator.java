package org.hl7.fhir.rdf;

import es.weso.rdf.RDFReader;
import es.weso.rdf.jena.RDFAsJenaModel;
import es.weso.schema.Result;
import es.weso.schema.Schema;
import es.weso.schema.ShExSchema$;
import org.apache.jena.rdf.model.Model;
import scala.Option;

import java.nio.file.Files;
import java.nio.file.Paths;

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
        return ShExSchema$.MODULE$.fromString(contents, "SHEXC", none).get();
    }

    public void validate(Model dataModel) {
        Option<String> none = Option.apply(null); // Create a none
        RDFReader rdf = new RDFAsJenaModel(dataModel);
        Result result = schema.validate(rdf, "TARGETDECLS", none, none, rdf.getPrefixMap(), schema.pm());
        if (!result.isValid()) System.out.println("Not valid");
    }
}
