package com.devoxx.util.emoji;

import javafx.scene.Node;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TextUtils {

    private static final double imageSize = 20;
    private static double imageOffset = -1;

    private static Text getTextNode(String text) {
        Text node = new Text(text);
        node.getStyleClass().add("text");
        return node;
    }

    private static double getImageOffset(double size) {
        if (imageOffset == -1) {
            Text node = getTextNode("Dummy");
            double textOffset = node.getBaselineOffset() - node.getLayoutBounds().getHeight() / 2;
            imageOffset = textOffset / size + 0.5;
        }
        return imageOffset;
    }

    public static List<Node> textEmojiHelper(String text) {
        List<Node> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        StringBuilder sbChain = new StringBuilder();
        List<Integer> codePoints = text.codePoints().boxed().collect(Collectors.toList());
        for (int i = 0; i < codePoints.size(); i++) {
            int ch = codePoints.get(i);
            if (isEmoji(ch) || isEmojiConnector(ch)) {
                if (sb.length() > 0) {
                    list.add(getTextNode(sb.toString()));
                    sb.setLength(0);
                }

                if (sbChain.length() > 0) {
                    sbChain.append("-");
                }
                sbChain.append(Integer.toHexString(ch));
                // stop and search emoji if, current codepoint is an emoji or emoji connector and:
                // we are at last codepoint or
                // next codepoint is not an emoji nor a connector
                // next codepoint is an emoji but current codepoint is not a connector

                if ((i == codePoints.size() - 1) ||
                        (!isEmoji(codePoints.get(i + 1)) && !isEmojiConnector(codePoints.get(i + 1))) ||
                        (isEmoji(codePoints.get(i + 1)) && !isEmojiConnector(ch))) {
                    EmojiData.emojiFromUnicode(sbChain.toString().toUpperCase(Locale.ROOT))
                            .ifPresent(e -> list.add(EmojiImageUtils.emojiView(e, imageSize, getImageOffset(imageSize))));
                    sbChain.setLength(0);
                }
            } else {
                sb.appendCodePoint(ch);
            }
        }
        if (sb.length() > 0) {
            list.add(getTextNode(sb.toString()));
            sb.setLength(0);
        }
        return list;
    }

    private static boolean isEmoji(int ch) {
        return  (127744 <= ch && ch <= 127994) || (128000 <= ch && ch <= 129782) ||
                (126980 <= ch && ch <= 127462) || (127488 <= ch && ch <= 127569) ||
                (169 <= ch && ch <= 174) || (8205 < ch && ch <= 12953);
    }

    private static boolean isEmojiConnector(int ch) {
        // skin tone 127995 - 127999, Zero width joiner 8205, invisible 65039, flags, regional indicators
        return  (0x1F3FB <= ch && ch <= 0x1F3FF) || ch == 0x200D || ch == 0xFE0F
                || (ch >= 0xE0062 && ch <= 0xE007F) || (ch >= 0x1F1E7 && ch <= 0x1F1FF);
    }
}