package com.nextbreakpoint.blueprint.common.test;

import java.io.*;
import java.util.*;

public class TestUtils {
    private TestUtils() {}

    public static void copyBytes(InputStream in, OutputStream out, int buffSize) throws IOException {
        PrintStream ps = out instanceof PrintStream ? (PrintStream)out : null;
        byte[] buf = new byte[buffSize];
        for(int bytesRead = in.read(buf); bytesRead >= 0; bytesRead = in.read(buf)) {
            out.write(buf, 0, bytesRead);
            if (ps != null && ps.checkError()) {
                throw new IOException("Unable to write to output stream.");
            }
        }
    }

    public static String getVariable(String name, String defaultValue) {
        return Optional.ofNullable(System.getenv(name)).orElse(defaultValue);
    }

    public static String getVariable(String name) {
        return Optional.ofNullable(System.getenv(name)).orElseThrow(() -> new RuntimeException("Undefined variable: " + name));
    }
}
