package org.grobid.core.document;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.grobid.core.layout.Block;
import org.grobid.core.layout.LayoutToken;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TokenLengthRatioFilterTest {
    public static final Logger LOGGER = LoggerFactory.getLogger(TokenLengthRatioFilterTest.class);

    private TokenLengthRatioFilter filter = new TokenLengthRatioFilter();

    public TokenLengthRatioFilterTest() {
    }

    private List<LayoutToken> createTokens(List<String> textList) {
        return (
            textList
            .stream()
            .map(text -> new LayoutToken(text))
            .collect(Collectors.toList())
        );
    }

    private Block createBlock(List<LayoutToken> tokens) {
        Block block = new Block();
        for (LayoutToken token: tokens) {
            block.addToken(token);
        }
        return block;
    }

    @Test
    public void shouldNotFailOnNullBlocks() {
        double ratio = this.filter.getNonBlankTokenLengthRatio(null);
        assertThat("ratio", ratio, is(Double.NaN));
        assertThat("ratio < 2", ratio < 2, is(false));
        this.filter.clearBlocks(null);
    }

    @Test
    public void shouldCalculateRatioForSingleBlock() {
        Block block = createBlock(this.createTokens(Arrays.asList(
            "12345678", "1234"
        )));
        assertThat(
            "ratio",
            this.filter.getNonBlankTokenLengthRatio(Arrays.asList(block)),
            is(6.0)  // (8 + 4) / 2
        );
    }

    @Test
    public void shouldCalculateFractionalRatioForSingleBlock() {
        Block block = createBlock(this.createTokens(Arrays.asList(
            "1234567", "1234"
        )));
        assertThat(
            "ratio",
            this.filter.getNonBlankTokenLengthRatio(Arrays.asList(block)),
            is(5.5)  // (7 + 4) / 2
        );
    }
}
