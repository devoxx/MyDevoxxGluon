package com.devoxx.util.emoji;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class EmojiData {

    /**
     * Stores emojis with key's as their short name.
     */
    private static final Map<String, Emoji> EMOJI_UNICODE_MAP = new HashMap<>();

    static  {
        try (final InputStream emojiStream = EmojiData.class.getResourceAsStream("emoji.json")) {
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new InputStreamReader(Objects.requireNonNull(emojiStream)));

            List<Emoji> emojis = gson.fromJson(reader, new TypeToken<List<Emoji>>(){}.getType());
            emojis.forEach(e -> {
                EMOJI_UNICODE_MAP.put(e.getUnified(), e);
                if (e.getSkin_variations() != null) {
                    e.getSkin_variations().values()
                            .forEach(v -> EMOJI_UNICODE_MAP.put(v.getUnified(), v));
                }
            });
        } catch (IOException e) {
            System.err.println("Could not load emoji json file" + e.getMessage());
        }
    }
    
    public static Optional<Emoji> emojiFromUnicode(String unicodeText) {
        return Optional.ofNullable(EMOJI_UNICODE_MAP.get(unicodeText));
    }

}
