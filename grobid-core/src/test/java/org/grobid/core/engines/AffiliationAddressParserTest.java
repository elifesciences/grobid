package org.grobid.core.engines;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Joiner;

import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.Affiliation;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.features.FeaturesVectorAffiliationAddress;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.OffsetPosition;


public class AffiliationAddressParserTest {

	public static final Logger LOGGER = LoggerFactory.getLogger(AffiliationAddressParserTest.class);

    private static boolean NO_USE_PRELABEL = false;
    private static List<List<OffsetPosition>> NO_PLACES_POSITIONS = Arrays.asList(
        Collections.emptyList()
    );

    private AffiliationAddressParser target;
    private GrobidAnalyzer analyzer;

    @Before
    public void setUp() throws Exception {
        this.target = new AffiliationAddressParser();
        this.analyzer = GrobidAnalyzer.getInstance();
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
        assertThat("affiliations should be null", result, is(nullValue()));
    }

    private static String addLabelsToFeatures(String header, List<String> labels) {
        String[] headerLines = header.split("\n");
        if (headerLines.length != labels.size()) {
            throw new IllegalArgumentException(String.format(
                "number of header lines and labels must match, %d != %d",
                headerLines.length, labels.size()
            ));
        }
        ArrayList<String> resultLines = new ArrayList<>(headerLines.length);
        for (int i = 0; i < headerLines.length; i++) {
            resultLines.add(headerLines[i] + " " + labels.get(i));
        }
        return Joiner.on("\n").join(resultLines);
    }

    private List<Affiliation> processLabelResults(
        List<String> tokens,
        List<String> labels
    ) throws Exception {
        List<LayoutToken> tokenizations = this.analyzer.getLayoutTokensForTokenizedText(tokens);
        List<String> affiliationBlocks = AffiliationAddressParser.getAffiliationBlocks(tokenizations);
        String header = FeaturesVectorAffiliationAddress.addFeaturesAffiliationAddress(
            affiliationBlocks, Arrays.asList(tokenizations), NO_PLACES_POSITIONS
        );
        String labelResult = addLabelsToFeatures(header, labels);
        LOGGER.debug("labelResult: {}", labelResult);
        return this.target.resultBuilder(
            labelResult,
            tokenizations,
            NO_USE_PRELABEL
        );
    }

    @Test
    public void shouldExtractSimpleAffiliation() throws Exception {
        List<String> tokens = Arrays.asList("1", " ", "University", " ", "of", " ", "Science");
        List<String> labels = Arrays.asList("I-<marker>", "I-<institution>", "<institution>", "<institution>");
        List<Affiliation> result = this.processLabelResults(
            tokens,
            labels
        );
        assertThat("should have one affiliation", result, is(hasSize(1)));
        Affiliation affiliation = result.get(0);
        assertThat("institution.marker", affiliation.getMarker(), is("1"));
        assertThat(
            "institution.institutions",
            affiliation.getInstitutions(),
            is(Arrays.asList("University of Science"))
        );
    }
}
