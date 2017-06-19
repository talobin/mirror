package haivo.us.screenmirror;

import java.nio.charset.Charset;

public class CharacterSets {
    public static final Charset ISO;
    public static final Charset ASCII;
    public static final Charset UTF_16;
    public static final Charset UTF_16_BE;
    public static final Charset UTF_16_LE;
    public static final Charset UTF_8;

    static {
        ISO = Charset.forName("ISO-8859-1");
        ASCII = Charset.forName("US-ASCII");
        UTF_16 = Charset.forName("UTF-16");
        UTF_16_BE = Charset.forName("UTF-16BE");
        UTF_16_LE = Charset.forName("UTF-16LE");
        UTF_8 = Charset.forName("UTF-8");
    }

    public static Charset getCharset(Charset charset) {
        return charset == null ? Charset.defaultCharset() : charset;
    }
}
