package org.grobid.core.engines;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import java.util.Collections;
import java.util.List;

import org.grobid.core.data.Affiliation;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidProperties;
import org.hamcrest.collection.IsEmptyCollection;


public class AffiliationAddressParserTest {

    private static boolean NO_USE_PRELABEL = false;

    private AffiliationAddressParser target;

    @Before
    public void setUp() throws Exception {
        target = new AffiliationAddressParser();
    }

    @BeforeClass
    public static void init() {
        LibraryLoader.load();
        GrobidProperties.getInstance();
    }

    @AfterClass
    public static void tearDown() {
        GrobidFactory.reset();
    }

    @Test
    public void shouldNotFailOnEmptyLabelResult() throws Exception {
        String labelResult = "";
        List<LayoutToken> tokenizations = Collections.emptyList();
        List<Affiliation> result = this.target.resultBuilder(
            labelResult,
            tokenizations,
            NO_USE_PRELABEL
        );
        assertThat("affiliations should be empty", result, is(not(empty())));
    }

}
