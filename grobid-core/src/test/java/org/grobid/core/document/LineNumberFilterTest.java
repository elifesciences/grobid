package org.grobid.core.document;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.ListUtils;
import org.grobid.core.layout.Block;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.Page;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LineNumberFilterTest {
    public static final Logger logger = LoggerFactory.getLogger(LineNumberFilterTest.class);

    private LineNumberFilter filter = new LineNumberFilter();

    private double lineHeight = 10.0;
    private double tokenWidth = 10.0;

    public LineNumberFilterTest() {
    }

    private List<LayoutToken> getLineNumberLayoutTokens(List<Block> blocks) {
        List<LayoutToken> actualLineNumberTokens = this.filter.getLineNumberLayoutTokens(blocks);
        logger.debug("line number tokens: {}", actualLineNumberTokens);
        return actualLineNumberTokens;
    }

    private LayoutToken createLayoutToken(
        String text,
        double x,
        double y
    ) {
        LayoutToken token = new LayoutToken(text);
        token.setX(x);
        token.setY(y);
        token.setWidth(this.lineHeight);
        token.setHeight(this.tokenWidth);
        return token;
    }

    private List<LayoutToken> createLineNumberTokens(
        int start,
        int count,
        double x,
        double startY
    ) {
        List<LayoutToken> tokens = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            tokens.add(createLayoutToken(
                String.valueOf(start + i),
                x,
                startY + (i + this.lineHeight)
            ));
        }
        return tokens;
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
        this.filter.getLineNumberLayoutTokens(null);
    }

    @Test
    public void shouldNotFailOnVeryLargeNumberToken() {
        List<LayoutToken> largeNumberTokens = this.createLineNumberTokens(
            1, 1, 10.0, 10.0
        );
        // change the text to a large number
        for (LayoutToken largeNumberToken: largeNumberTokens) {
            largeNumberToken.setText("12345678901");
        }
        Block block = createBlock(largeNumberTokens);
        assertThat(
            "lineNumberTokens",
            this.getLineNumberLayoutTokens(Arrays.asList(block)),
            empty()
        );
    }

    @Test
    public void shouldMatchLeftLineNumbers() {
        List<LayoutToken> lineNumberTokens = this.createLineNumberTokens(
            1, 10, 10.0, 10.0
        );
        Block block = createBlock(lineNumberTokens);
        assertThat(
            "lineNumberTokens",
            this.filter.getLineNumberLayoutTokens(Arrays.asList(block)),
            is(lineNumberTokens)
        );
    }

    @Test
    public void shouldOnlyMatchContineousLineNumbersOnTheLeft() {
        List<LayoutToken> lineNumberTokens = this.createLineNumberTokens(
            1, 10, 10.0, 10.0
        );
        LayoutToken otherSameLineNumberToken = createLayoutToken("2", 20.0, 10.0);
        Block block = createBlock(Arrays.asList(
            lineNumberTokens.get(0),
            otherSameLineNumberToken
        ));
        for (LayoutToken token: lineNumberTokens.subList(1, lineNumberTokens.size())) {
            block.addToken(token);
        }
        assertThat(
            "lineNumberTokens",
            this.getLineNumberLayoutTokens(Arrays.asList(block)),
            is(lineNumberTokens)
        );
    }

    @Test
    public void shouldNotMatchLineNumbersInReverseOrder() {
        List<LayoutToken> lineNumberTokens = this.createLineNumberTokens(
            1, 10, 10.0, 10.0
        );
        // make the numbers go from 10 to 1 instead
        for (int i = 0; i < lineNumberTokens.size(); i++) {
            lineNumberTokens.get(i).setText(String.valueOf(
                lineNumberTokens.size() - i
            ));
        }
        Block block = createBlock(lineNumberTokens);
        assertThat(
            "lineNumberTokens",
            this.getLineNumberLayoutTokens(Arrays.asList(block)),
            empty()
        );
    }

    @Test
    public void shouldNotMatchNumbersOnADifferentXAxis() {
        List<LayoutToken> lineNumberTokens = this.createLineNumberTokens(
            1, 10, 10.0, 10.0
        );
        List<LayoutToken> otherTokens = this.createLineNumberTokens(
            11, 5, 100.0, 100.0 + 10 * this.lineHeight
        );
        Block block = createBlock(ListUtils.union(
            lineNumberTokens,
            otherTokens
        ));
        assertThat(
            "lineNumberTokens",
            this.getLineNumberLayoutTokens(Arrays.asList(block)),
            is(lineNumberTokens)
        );
    }

    @Test
    public void shouldNotMatchLineNumbersMixedWithOtherTokens() {
        List<LayoutToken> lineNumberTokens = this.createLineNumberTokens(
            1, 10, 10.0, 10.0
        );
        List<LayoutToken> otherTokens = this.createLineNumberTokens(
            1, 10, 10.0, 10.0 + 10 * this.lineHeight
        );
        // change the text to "other" to not match line number pattern anymore
        for (LayoutToken otherToken: otherTokens) {
            otherToken.setText("other");
        }
        Block block = createBlock(ListUtils.union(
            lineNumberTokens,
            otherTokens
        ));
        assertThat(
            "lineNumberTokens",
            this.getLineNumberLayoutTokens(Arrays.asList(block)),
            empty()
        );
    }

    @Test
    public void shouldAllowResetOfLineNumbersPerPage() {
        List<LayoutToken> pageOneLineNumberTokens = this.createLineNumberTokens(
            1, 10, 10.0, 10.0
        );
        List<LayoutToken> pageTwoLineNumberTokens = this.createLineNumberTokens(
            1, 10, 10.0, 10.0
        );
        Block pageOneBlock = createBlock(pageOneLineNumberTokens);
        pageOneBlock.setPage(new Page(1));
        Block pageTwoBlock = createBlock(pageTwoLineNumberTokens);
        pageTwoBlock.setPage(new Page(2));
        assertThat(
            "lineNumberTokens",
            this.getLineNumberLayoutTokens(Arrays.asList(pageOneBlock, pageTwoBlock)),
            is(ListUtils.union(pageOneLineNumberTokens, pageTwoLineNumberTokens))
        );
    }

    @Test
    public void shouldUpdateBlockTextWhenRemovingLineNumbers() {
        LayoutToken lineNumberToken = createLayoutToken("1", 10, 10);
        LayoutToken spaceToken = createLayoutToken(" ", 20, 10);
        List<LayoutToken> otherTokens = Arrays.asList(
            createLayoutToken("other", 30, 10),
            createLayoutToken(" ", 40, 10),
            createLayoutToken("text", 50, 10)
        );
        Block block = createBlock(ListUtils.union(
            ListUtils.union(
                Arrays.asList(lineNumberToken),
                Arrays.asList(spaceToken)
            ),
            otherTokens
        ));
        block.setPage(new Page(1));
        block.setText("1 other text");
        assertThat("block.text (before)", block.getText(), is("1 other text"));
        this.filter.removeLineNumberTokens(Arrays.asList(new LineNumberFilter.LineNumberToken(
            block,
            lineNumberToken,
            1,
            1
        )));
        assertThat("block.text (after)", block.getText(), is("other text"));
    }
}
