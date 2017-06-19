package haivo.us.screenmirror;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

public class ReadAndWrite {
    public static final char separator;
    public static final String stringResult;

    static {
        separator = File.separatorChar;
        Writer stringWriter = new StringWriter(4);
        PrintWriter printWriter = new PrintWriter(stringWriter);
        printWriter.println();
        stringResult = stringWriter.toString();
        printWriter.close();
    }

    public static int write(Reader reader, Writer writer) {
        long b = ReadAndWrite.writeLong(reader, writer);
        return b > 2147483647L ? -1 : (int) b;
    }

    public static long writeLong(Reader reader, Writer writer) {
        return ReadAndWrite.write(reader, writer, new char[4096]);
    }

    public static long write(Reader reader, Writer writer, char[] cArr) {
        long j = 0;
        while (true) {
            int read = 0;
            try {
                read = reader.read(cArr);
                if (-1 == read) {
                    return j;
                }
                writer.write(cArr, 0, read);
                j += (long) read;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static String read(InputStream inputStream) {
        return ReadAndWrite.read(inputStream, Charset.defaultCharset());
    }

    public static String read(InputStream inputStream, Charset charset) {
        Writer stringWriter = new StringWriter();
        ReadAndWrite.read(inputStream, stringWriter, charset);
        return stringWriter.toString();
    }

    public static void read(InputStream inputStream, Writer writer, Charset charset) {
        ReadAndWrite.write(new InputStreamReader(inputStream, CharacterSets.getCharset(charset)), writer);
    }


}
