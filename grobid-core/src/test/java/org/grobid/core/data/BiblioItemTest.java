package org.grobid.core.data;

import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.grobid.core.utilities.GrobidProperties;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static org.hamcrest.CoreMatchers.is;


public class BiblioItemTest {

    public static final Logger LOGGER = LoggerFactory.getLogger(BiblioItemTest.class);

    @BeforeClass
    public static void init() {
        GrobidProperties.getInstance();
    }

    private static Document parseXml(String xml) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private static List<String> getXpathStrings(
        Document doc, String xpath_expr
    ) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xpath.compile(xpath_expr);
        NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        ArrayList<String> matchingStrings = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            matchingStrings.add(nodes.item(i).getNodeValue()); 
        }
        return matchingStrings;
    }

    @Test
    public void shouldGnerateRawAffiliationText() throws Exception {
        Affiliation aff = new Affiliation();
        aff.setRawAffiliationString("raw affiliation 1");
        Person author = new Person();
        author.setLastName("Smith");
        author.setAffiliations(Arrays.asList(aff));
        BiblioItem biblioItem = new BiblioItem();
        biblioItem.setFullAuthors(Arrays.asList(author));
        biblioItem.setFullAffiliations(Arrays.asList(aff));
        String tei = biblioItem.toTEI(2);
        LOGGER.debug("tei: {}", tei);
        Document doc = parseXml(tei);
        assertThat(
            "raw_affiliation",
            getXpathStrings(doc, "//note[@type=\"raw_affiliation\"]/text()"),
            is(Arrays.asList("raw affiliation 1"))
        );
    }
}
