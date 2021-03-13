package com.nextbreakpoint.blueprint.common.core;

import java.io.*;
import java.nio.charset.Charset;

public final class IOUtils {
    private static final int EOF = -1;
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    public static String toString(InputStream inputStream, Charset charset) {
        if (inputStream == null) {
            return "";
        } else {
            try {
                StringWriter writer = new StringWriter();

                String result;
                try {
                    InputStreamReader reader = new InputStreamReader(inputStream, charset);

                    try {
                        BufferedReader bufferedReader = new BufferedReader(reader);

                        try {
                            char[] chars = new char[1024];

                            while (true) {
                                int readChars;
                                if ((readChars = bufferedReader.read(chars)) == -1) {
                                    result = writer.toString();
                                    break;
                                }

                                writer.write(chars, 0, readChars);
                            }
                        } catch (Throwable e) {
                            try {
                                bufferedReader.close();
                            } catch (Throwable x) {
                                e.addSuppressed(x);
                            }

                            throw e;
                        }

                        bufferedReader.close();
                    } catch (Throwable e) {
                        try {
                            reader.close();
                        } catch (Throwable x) {
                            e.addSuppressed(x);
                        }

                        throw e;
                    }

                    reader.close();
                } catch (Throwable e) {
                    try {
                        writer.close();
                    } catch (Throwable x) {
                        e.addSuppressed(x);
                    }

                    throw e;
                }

                writer.close();
                return result;
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
