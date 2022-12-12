package org.hl7.fhir.definitions.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;

public class ExampleTest {

    public static final String EXAMPLE_SLASHED_O = "exampleø";

    @Test
    public void Example_instance_worksForCurrentEncoding() throws Exception {
        String path = "src/test/resources/examples/observation-unicode-example.xml";

        File file = new File(path);
        Example example = new Example("dummyName",
               "dummyId",
               "dummyDescription",
               file,
                true, Example.ExampleType.XmlFile, true);
        String actualId = example.getXml().getFirstChild().getNextSibling().getFirstChild().getNextSibling().getAttributes().item(0).getNodeValue();
        assertEquals(EXAMPLE_SLASHED_O, actualId);

    }
}
