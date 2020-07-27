package org.grobid.core.features;

import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;


public class FeaturesVectorSegmentationTest {

    private FeaturesVectorSegmentation features = new FeaturesVectorSegmentation();

    public FeaturesVectorSegmentationTest() {
        this.features.line = "this is the whole line";
        this.features.string = "this";
        this.features.secondString = "is";
        this.features.digit = "";
    }

    @Test
    public void test_formatFeatureText_shouldReplaceSpaceWithNonBreakingSpace() {
        List<String> tokens = Arrays.asList("token1", "token2", "token3");
        String line = String.join(" ", tokens);
        assertThat(
            "formatted feature text",
            FeaturesVectorSegmentation.formatFeatureText(line),
            is(String.join(FeaturesVectorSegmentation.NBSP, tokens))
        );
    }

    @Test
    public void test_formatFeatureText_shouldReplaceTabsWithNonBreakingSpace() {
        List<String> tokens = Arrays.asList("token1", "token2", "token3");
        String line = String.join("\t", tokens);
        assertThat(
            "formatted feature text",
            FeaturesVectorSegmentation.formatFeatureText(line),
            is(String.join(FeaturesVectorSegmentation.NBSP, tokens))
        );
    }

    @Test
    public void shouldNotAddWholeLineFeatureIfEnabled() {
        this.features.wholeLineFeatureEnabled = false;
        String[] featuresVector = features.printVector().trim().split(" ");
        String lastVectorString = featuresVector[featuresVector.length - 1];
        assertThat(
            "last feature",
            lastVectorString,
            is(not(FeaturesVectorSegmentation.formatFeatureText(features.line)))
        );
    }

    @Test
    public void shouldAddWholeLineFeatureIfEnabled() {
        this.features.wholeLineFeatureEnabled = true;
        String[] featuresVector = features.printVector().trim().split(" ");
        String lastVectorString = featuresVector[featuresVector.length - 1];
        assertThat(
            "last feature",
            lastVectorString,
            is(FeaturesVectorSegmentation.formatFeatureText(features.line))
        );
    }
}