package com.devoxx.util.emoji;

import com.gluonhq.connect.converter.InputStreamIterableInputConverter;
import com.gluonhq.connect.converter.JsonIterableInputConverter;
import com.gluonhq.connect.provider.InputStreamListDataReader;
import com.gluonhq.connect.provider.ListDataReader;
import com.gluonhq.connect.source.BasicInputDataSource;
import com.gluonhq.connect.source.InputDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public class EmojiData {

    /**
     * Stores emojis with key's as their short name.
     */
    private static final Map<String, Emoji> EMOJI_UNICODE_MAP = new HashMap<>();

    static  {
        try (final InputStream emojiStream = EmojiData.class.getResourceAsStream("emoji.json")) {
            InputDataSource dataSource = new BasicInputDataSource(emojiStream);
            InputStreamIterableInputConverter<Emoji> converter = new JsonIterableInputConverter<>(Emoji.class);
            ListDataReader<Emoji> listDataReader = new InputStreamListDataReader<>(dataSource, converter);
            for (Iterator<Emoji> it = listDataReader.iterator(); it.hasNext();) {
                Emoji e = it.next();
                EMOJI_UNICODE_MAP.put(e.getUnified(), e);
            }
        } catch (IOException e) {
            System.err.println("Could not load emoji json file" + e.getMessage());
        }
    }
    
    public static Optional<Emoji> emojiFromUnicode(String unicodeText) {
        return Optional.ofNullable(EMOJI_UNICODE_MAP.get(unicodeText));
    }

}
