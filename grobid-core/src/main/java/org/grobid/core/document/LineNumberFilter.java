package org.grobid.core.document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.grobid.core.layout.Block;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class LineNumberFilter {
    static class LineNumberToken {
        private Block block;
        private LayoutToken layoutToken;
        private int documentPosition;
        private int lineNumber;

        public LineNumberToken(
            Block block,
            LayoutToken layoutToken,
            int documentPosition,
            int lineNumber
        ) {
            this.block = block;
            this.layoutToken = layoutToken;
            this.documentPosition = documentPosition;
            this.lineNumber = lineNumber;
        }

        public String toString() {
            return String.format(
                "LineNumberToken(\"%s\", documentPosition=%s, x=%f, y=%f)",
                this.layoutToken, this.documentPosition,
                this.layoutToken.getX(), this.layoutToken.getY()
            );
        }

        public Block getBlock() {
            return this.block;
        }

        public LayoutToken getLayoutToken() {
            return this.layoutToken;
        }

        public int getDocumentPosition() {
            return this.documentPosition;
        }

        public int getPageNumber() {
            Page page = this.block.getPage();
            if (page == null) {
                return 0;
            }
            return page.getNumber();
        }

        public int getLineNumber() {
            return this.lineNumber;
        }
    }

    private Comparator<LineNumberToken> byPageAndLineNumber = (
        LineNumberToken token1, LineNumberToken token2
    ) -> (
        token1.getPageNumber() == token2.getPageNumber()
        ? Integer.valueOf(token1.getLineNumber()).compareTo(
            token2.getLineNumber()
        )
        : Integer.valueOf(token1.getPageNumber()).compareTo(
            token2.getPageNumber()
        )
    );

    public static final Logger logger = LoggerFactory.getLogger(LineNumberFilter.class);

    private static Pattern lineNumberPattern = Pattern.compile("\\d+");

    private int minLineNumbers = 10;
    private double minLineNumberRatioSimilarX = 0.8;

    public void setMinLineNumbers(int minLineNumbers) {
        this.minLineNumbers = minLineNumbers;
    }

    private List<LineNumberToken> getLineNumberTokenCandidates(List<Block> blocks) {
        List<LineNumberToken> lineNumberTokens = new ArrayList<>();
        if (blocks == null) {
            return lineNumberTokens;
        }
        int documentPosition = 1;
        for (Block block: blocks) {
            List<LayoutToken> tokens = block.getTokens();
            LayoutToken previousBlockToken = null;
            for (LayoutToken token: tokens) {
                if (token.getX() < 0 || token.getY() < 0) {
                    continue;
                }
                boolean newLine = (
                    (previousBlockToken == null)
                    || (previousBlockToken.getX() >= token.getX())
                );
                if (!newLine) {
                    continue;
                }
                String tokenText = token.getText();
                Matcher lineNumberMatcher = lineNumberPattern.matcher(tokenText);
                if (lineNumberMatcher.matches()) {
                    int lineNumber;
                    try {
                        lineNumber = Integer.parseInt(tokenText);
                    } catch (NumberFormatException e) {
                        // it's probably not a line number after all
                        continue;
                    }
                    LineNumberToken lineNumberToken = new LineNumberToken(
                        block, token, documentPosition++, lineNumber
                    );
                    logger.debug(
                        "adding lineNumberToken candidate: {}, newline: {}",
                        lineNumberToken, newLine
                    );
                    lineNumberTokens.add(lineNumberToken);
                }
                previousBlockToken = token;
            }
        }
        return lineNumberTokens;
    }

    private double getMedian(List<Double> list) {
        List<Double> sorted = new ArrayList<>(list);
        Collections.sort(sorted);
        int medianIndex = list.size() / 2;
        if (list.size() % 2 == 0) {
            return (sorted.get(medianIndex) + sorted.get(medianIndex + 1)) / 2.0;
        }
        return sorted.get(medianIndex);
    }

    private double getMedianTokenX(List<LineNumberToken> tokens) {
        return getMedian(tokens.stream().map(
            (LineNumberToken token) -> token.getLayoutToken().getX()
        ).collect(Collectors.toList()));
    }

    private double getMedianTokenWidth(List<LineNumberToken> tokens) {
        return getMedian(tokens.stream().map(
            (LineNumberToken token) -> token.getLayoutToken().getWidth()
        ).collect(Collectors.toList()));
    }

    private List<LineNumberToken> filterLineNumberTokensWithXBetween(
        List<LineNumberToken> tokens,
        double minX,
        double maxX
    ) {
        return (
            tokens
            .stream()
            .filter((LineNumberToken token) -> (
                token.getLayoutToken().getX() >= minX
                && token.getLayoutToken().getX() <= maxX
            )).collect(Collectors.toList())
        );
    }

    private long countTokensWithXBetween(
        List<Block> blocks,
        double minX,
        double maxX
    ) {
        return (
            blocks
            .stream()
            .flatMap((Block block) -> block.getTokens().stream())
            .filter((LayoutToken token) -> (
                token.getX() >= minX
                && token.getX() <= maxX
            )).collect(Collectors.counting())
        );
    }

    public List<LineNumberToken> getLineNumberTokens(List<Block> blocks) {
        List<LineNumberToken> lineNumberTokensCandidates = (
            this.getLineNumberTokenCandidates(blocks)
        );
        logger.debug(
            "line number candidates: {}",
            lineNumberTokensCandidates.stream()
            .map((LineNumberToken token) -> token.getLineNumber())
            .collect(Collectors.toList())
        );
        if (lineNumberTokensCandidates.size() < this.minLineNumbers) {
            return Collections.emptyList();
        }
        double medianTokenX = this.getMedianTokenX(lineNumberTokensCandidates);
        double medianTokenWidth = this.getMedianTokenWidth(lineNumberTokensCandidates);
        lineNumberTokensCandidates = this.filterLineNumberTokensWithXBetween(
            lineNumberTokensCandidates,
            medianTokenX - medianTokenWidth,
            medianTokenX + medianTokenWidth
        );
        long totalNumberOfTokensOverlappingLineNumbers = this.countTokensWithXBetween(
            blocks,
            medianTokenX - medianTokenWidth,
            medianTokenX + medianTokenWidth
        );
        logger.debug(
            "median x={}, width={}, remaining tokens={}, total tokens with similar x={}",
            medianTokenX, medianTokenWidth,
            lineNumberTokensCandidates.size(),
            totalNumberOfTokensOverlappingLineNumbers
        );
        Collections.sort(lineNumberTokensCandidates, this.byPageAndLineNumber);
        List<LineNumberToken> lineNumberTokens = new ArrayList<>(
            lineNumberTokensCandidates.size()
        );
        LineNumberToken previousToken = null;
        for (int i = 0; i < lineNumberTokensCandidates.size(); i++) {
            LineNumberToken currentToken = lineNumberTokensCandidates.get(i);
            if (previousToken == null || (
                (
                    previousToken.getPageNumber() < currentToken.getPageNumber()
                    || previousToken.getLineNumber() < currentToken.getLineNumber()
                )
                && previousToken.getDocumentPosition() < currentToken.getDocumentPosition()
            )) {
                logger.debug(
                    "adding lineNumberToken: {}, previous: {}",
                    currentToken, previousToken
                );
                lineNumberTokens.add(currentToken);
                previousToken = currentToken;
            }
        }
        double lineNumberRatio = (
            1.0 * lineNumberTokens.size() / totalNumberOfTokensOverlappingLineNumbers
        );
        logger.debug(
            "potential line numbers: {}, lineNumberRatio: {}",
            lineNumberTokens.size(),
            lineNumberRatio
        );
        if (
            lineNumberTokens.size() < this.minLineNumbers
            || lineNumberRatio < this.minLineNumberRatioSimilarX
        ) {
            return Collections.emptyList();
        }
        return lineNumberTokens;
    }

    public List<LayoutToken> getLineNumberLayoutTokens(List<Block> blocks) {
        List<LineNumberToken> lineNumberTokens = this.getLineNumberTokens(blocks);
        List<LayoutToken> tokens = new ArrayList<>(lineNumberTokens.size());
        for (LineNumberToken lineNumberToken: lineNumberTokens) {
            tokens.add(lineNumberToken.getLayoutToken());
        }
        return tokens;
    }

    public void removeTokenFromBlock(Block block, LayoutToken token) {
        if (block.getTokens() == null) {
            return;
        }
        List<LayoutToken> blockTokens = new ArrayList<>(block.getTokens());
        block.resetTokens();
        block.setText("");
        boolean removeNextSpace = false;
        boolean tokenFound = false;
        for (LayoutToken blockToken: blockTokens) {
            if (blockToken == token) {
                tokenFound = true;
                removeNextSpace = true;
                block.setStartToken(block.getStartToken() + 1);
                continue;
            }
            if (removeNextSpace && StringUtils.isBlank(blockToken.getText())) {
                continue;
            }
            block.addToken(blockToken);
            removeNextSpace = false;
        }
        if (!tokenFound) {
            throw new RuntimeException("token not found in block: " + token);
        }
        block.setText("removed line no: " + block.getText());
    }

    public void removeLineNumberTokens(
            List<Block> blocks,
            List<LineNumberToken> lineNumberTokens) {
        for (LineNumberToken lineNumberToken: lineNumberTokens) {
            this.removeTokenFromBlock(
                lineNumberToken.getBlock(),
                lineNumberToken.getLayoutToken()
            );
        }
    }

    public List<LayoutToken> recalculateDocumentTokenization(
            List<Block> blocks) {
        List<LayoutToken> tokenization = new ArrayList<>();
        for (Block block: blocks) {
            if (block.getTokens() == null) {
                continue;
            }
            block.setStartToken(tokenization.size());
            block.setEndToken(tokenization.size() + block.getTokens().size());
            tokenization.addAll(block.getTokens());
        }
        return tokenization;
    }

    public void findAndRemoveLineNumbers(List<Block> blocks) {
        List<LineNumberToken> lineNumberTokens = this.getLineNumberTokens(blocks);
        removeLineNumberTokens(blocks, lineNumberTokens);
        logger.info(
            "removed line numbers: {}",
            lineNumberTokens.stream()
            .map((LineNumberToken token) -> token.getLineNumber())
            .collect(Collectors.toList())
        );
    }
}
