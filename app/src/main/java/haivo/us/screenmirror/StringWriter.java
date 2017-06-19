package haivo.us.screenmirror;

import java.io.Serializable;
import java.io.Writer;

public class StringWriter extends Writer implements Serializable {
    private final StringBuilder stringBuilder;

    public StringWriter() {
        this.stringBuilder = new StringBuilder();
    }

    public StringWriter(int i) {
        this.stringBuilder = new StringBuilder(i);
    }

    public Writer append(char c) {
        this.stringBuilder.append(c);
        return this;
    }

    public Writer append(CharSequence charSequence) {
        this.stringBuilder.append(charSequence);
        return this;
    }

    public Writer append(CharSequence charSequence, int i, int i2) {
        this.stringBuilder.append(charSequence, i, i2);
        return this;
    }

    public void close() {
    }

    public void flush() {
    }

    public String toString() {
        return this.stringBuilder.toString();
    }

    public void write(String str) {
        if (str != null) {
            this.stringBuilder.append(str);
        }
    }

    public void write(char[] cArr, int i, int i2) {
        if (cArr != null) {
            this.stringBuilder.append(cArr, i, i2);
        }
    }
}
