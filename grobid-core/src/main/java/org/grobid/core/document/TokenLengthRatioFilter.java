package org.grobid.core.document;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.grobid.core.layout.Block;
import org.grobid.core.layout.LayoutToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class TokenLengthRatioFilter {
    public static final Logger logger = LoggerFactory.getLogger(TokenLengthRatioFilter.class);

    public List<LayoutToken> getFlatNonBlankTokenList(List<Block> blocks) {
        if (blocks == null) {
            return Collections.emptyList();
        }
        return (
            blocks
            .stream()
            .flatMap(block -> block.getTokens().stream())
            .filter(layoutToken -> !StringUtils.isBlank(layoutToken.getText()))
            .collect(Collectors.toList())
        );
    }

    public double getTokenLengthRatio(List<LayoutToken> layoutTokens) {
        long totalCharacterCount = (
            layoutTokens
            .stream()
            .mapToLong(layoutToken -> layoutToken.getText().length())
            .sum()
        );
        double ratio = ((double) totalCharacterCount) / layoutTokens.size();
        logger.info(
            "total token characters: {}, number of tokens: {}, ratio: {}",
            totalCharacterCount, layoutTokens.size(), ratio
        );
        return ratio;
    }

    public double getNonBlankTokenLengthRatio(List<Block> blocks) {
        return this.getTokenLengthRatio(
            this.getFlatNonBlankTokenList(blocks)
        );
    }

    public void clearBlock(Block block) {
        block.resetTokens();
    }

    public void clearBlocks(List<Block> blocks) {
        if (blocks == null) {
            return;
        }
        for (Block block: blocks) {
            this.clearBlock(block);
        }
    }
}
