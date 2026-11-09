package eu.amadeos.games.cruise.ui.game;

import com.sun.jna.IntegerType;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import eu.amadeos.gameengine.audio.Sound;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tracker modules (MOD, XM, S3M, IT and their cousins) through libopenmpt, the library of OpenMPT, called
 * with JNA. When the system has no libopenmpt, modules are left out of the playlist and nothing else
 * changes.
 */
// --8<-- [start:modules]
final class Modules {
    /** The few functions of libopenmpt's C interface the game needs. */
    interface OpenMpt extends Library {
        Pointer openmpt_module_create_from_memory2(byte[] data, SizeT size, Pointer logFunc, Pointer logUser,
                                                  Pointer errorFunc, Pointer errorUser, Pointer error,
                                                  Pointer errorMessage, Pointer initialCtls);

        int openmpt_module_set_repeat_count(Pointer module, int count);

        SizeT openmpt_module_read_mono(Pointer module, int sampleRate, SizeT count, short[] mono);

        void openmpt_module_destroy(Pointer module);
    }

    /** C's {@code size_t}: as wide as a pointer. */
    public static final class SizeT extends IntegerType {
        public SizeT() {
            this(0);
        }

        public SizeT(long value) {
            super(Native.SIZE_T_SIZE, value, true);
        }
    }

    private static final OpenMpt LIBRARY = load();

    private Modules() {
    }

    private static OpenMpt load() {
        for (String name : new String[]{"openmpt", "libopenmpt.so.0"}) {
            try {
                return Native.load(name, OpenMpt.class);
            } catch (UnsatisfiedLinkError | RuntimeException e) {
                // try the next name
            }
        }
        return null;
    }

    /** True when libopenmpt was found. */
    static boolean available() {
        return LIBRARY != null;
    }

    /** Plays the module once through into the stream, at the mixer's rate. */
    static void decode(Path file, Playlist.Sink stream) throws Exception {
        if (LIBRARY == null) throw new IllegalStateException("libopenmpt is not installed");
        byte[] data = Files.readAllBytes(file);
        Pointer module = LIBRARY.openmpt_module_create_from_memory2(data, new SizeT(data.length),
                null, null, null, null, null, null, null);
        if (module == null) throw new IllegalArgumentException("not a module libopenmpt can read");
        try {
            LIBRARY.openmpt_module_set_repeat_count(module, 0);
            short[] block = new short[4096];
            for (long n; (n = LIBRARY.openmpt_module_read_mono(module, Sound.RATE, new SizeT(block.length), block).longValue()) > 0; ) {
                if (!stream.write(block, (int) n)) return;
            }
        } finally {
            LIBRARY.openmpt_module_destroy(module);
        }
    }
}
// --8<-- [end:modules]
