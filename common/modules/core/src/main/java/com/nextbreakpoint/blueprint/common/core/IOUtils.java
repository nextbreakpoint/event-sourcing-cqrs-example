package com.nextbreakpoint.blueprint.common.core;

import java.io.*;
import java.nio.charset.Charset;

public final class IOUtils {
    public static String toString(InputStream inputStream, Charset charset) {
        if (inputStream == null) {
            return "";
        } else {
            try (StringWriter writer = new StringWriter()) {
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
                    char[] chars = new char[1024];

                    for (int readChars; (readChars = bufferedReader.read(chars)) != -1;) {
                        writer.write(chars, 0, readChars);
                    }
                }

                return writer.toString();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public static void copy(InputStream inputStream, OutputStream outputStream, Charset charset) {
        if (inputStream == null) {
            return;
        } else {
            try (OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
                    char[] chars = new char[1024];

                    for (int readChars; (readChars = bufferedReader.read(chars)) != -1;) {
                        writer.write(chars, 0, readChars);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public static String toString(InputStream inputStream) {
        return toString(inputStream, Charset.defaultCharset());
    }

    private IOUtils() {
    }
}
