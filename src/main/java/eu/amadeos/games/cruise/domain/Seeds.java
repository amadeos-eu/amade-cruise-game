package eu.amadeos.games.cruise.domain;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** A world comes from a word: the same word (whatever its case and spaces around it) gives the same world. */
public final class Seeds {
    private Seeds() {
    }

    // --8<-- [start:seed]
    /** The 64-bit FNV-1a hash of the word in lower case: a fixed number, the same on every computer. */
    public static long of(String word) {
        long h = 0xcbf29ce484222325L;
        for (byte b : word.strip().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)) {
            h ^= b & 0xff;
            h *= 0x100000001b3L;
        }
        return h;
    }
    // --8<-- [end:seed]
}
