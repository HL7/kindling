package org.hl7.fhir;

import org.junit.jupiter.api.Test;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerFactory;

public class XMLUtilTest {
    @Test
    public void test() {
        TransformerFactory f = TransformerFactory.newInstance();
        //f.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");

    }
}
