package com.nextbreakpoint.blueprint.designs.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextbreakpoint.Try;
import com.nextbreakpoint.nextfractal.core.common.Bundle;
import com.nextbreakpoint.nextfractal.core.common.CoreFactory;
import com.nextbreakpoint.nextfractal.core.common.FileManagerEntry;
import com.nextbreakpoint.nextfractal.core.common.FileManifest;
import com.nextbreakpoint.nextfractal.core.common.Plugins;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.nextbreakpoint.nextfractal.core.common.Plugins.tryFindFactory;

public class BundleUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    private BundleUtils() {}

    public static Try<Bundle, Exception> createBundle(String manifest, String metadata, String script) {
        return Try.of(() -> mapper.readValue(manifest, FileManifest.class))
                .flatMap(decodedManifest -> createBundle(decodedManifest, createEntries(manifest, metadata, script)));
    }

    public static Try<byte[], Exception> writeBundle(Bundle bundle) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            writeBundle(bundle, baos).execute();
            return Try.success(baos.toByteArray());
        } catch (Exception e) {
            return Try.failure(e);
        }
    }

    public static Try<Bundle, Exception> writeBundle(Bundle bundle, OutputStream stream) {
        try (ZipOutputStream os = new ZipOutputStream(stream)) {
            saveBundle(os, bundle).execute();
            return Try.success(bundle);
        } catch (Exception e) {
            return Try.failure(e);
        }
    }

    public static Try<Bundle, Exception> readBundle(byte[] data) {
        try (ZipInputStream is = new ZipInputStream(new ByteArrayInputStream(data))) {
            final var bundle = readBundle(is).execute().orThrow();
            return Try.success(bundle);
        } catch (Exception e) {
            return Try.failure(e);
        }
    }

    public static Try<Bundle, Exception> readBundle(InputStream is) {
        try (ZipInputStream zis = new ZipInputStream(is)) {
            final var bundle = loadBundle(zis).execute().orThrow();
            return Try.success(bundle);
        } catch (Exception e) {
            return Try.failure(e);
        }
    }

    private static Try<Bundle, Exception> loadBundle(ZipInputStream is) {
        return Try.of(() -> readEntries(is)).flatMap(BundleUtils::readManifest)
                .flatMap(result -> tryFindFactory((String) result[1]).map(CoreFactory::createFileManager)
                        .flatMap(manager -> manager.loadEntries((List<FileManagerEntry>) result[0])));
    }

    private static List<FileManagerEntry> readEntries(ZipInputStream is) throws IOException {
        LinkedList<FileManagerEntry> entries = new LinkedList<>();
        for (ZipEntry entry = is.getNextEntry(); entry != null; entry = is.getNextEntry())
            entries.add(readEntry(is, entry));
        return entries;
    }

    private static FileManagerEntry readEntry(ZipInputStream is, ZipEntry entry) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            copyBytes(is, baos);
            return new FileManagerEntry(entry.getName(), baos.toByteArray());
        }
    }

    private static Try<Object[], Exception> readManifest(List<FileManagerEntry> entries) {
        return entries.stream().filter(entry -> entry.getName().equals("manifest"))
                .findFirst().map(manifest -> parseManifest(entries, manifest.getData()))
                .orElseGet(() -> Try.failure(new Exception("Manifest is required")));
    }

    private static Try<Object[], Exception> parseManifest(List<FileManagerEntry> entries, byte[] data) {
        try {
            Map properties = mapper.readValue(data, HashMap.class);
            String pluginId = (String) properties.get("pluginId");
            return Try.success(new Object[]{entries, Objects.requireNonNull(pluginId)});
        } catch (Exception e) {
            return Try.failure(new Exception("Plugin id not defined"));
        }
    }

    private static Try<Bundle, Exception> saveBundle(ZipOutputStream os, Bundle bundle) {
        return tryFindFactory(bundle.getSession().getPluginId()).map(CoreFactory::createFileManager)
                .flatMap(manager -> manager.saveEntries(bundle).flatMap(entries -> writeEntries(os, bundle, entries)));
    }

    private static Try<Bundle, Exception> writeEntries(ZipOutputStream os, Bundle bundle, List<FileManagerEntry> entries) {
        return entries.stream().map(entry -> Try.of(() -> writeEntry(os, bundle, entry)))
                .filter(Try::isFailure).findFirst().orElse(Try.success(bundle));
    }

    private static Bundle writeEntry(ZipOutputStream os, Bundle bundle, FileManagerEntry entry) throws IOException {
        ZipEntry zipEntry = new ZipEntry(entry.getName());
        os.putNextEntry(zipEntry);
        os.write(entry.getData());
        os.closeEntry();
        return bundle;
    }

    private static Try<Bundle, Exception> createBundle(FileManifest manifest, List<FileManagerEntry> entries) {
        return Plugins.tryFindFactory(manifest.getPluginId())
                .flatMap(factory -> factory.createFileManager().loadEntries(entries));
    }

    private static List<FileManagerEntry> createEntries(String manifest, String metadata, String script) {
        final FileManagerEntry manifestEntry = new FileManagerEntry("manifest", manifest.getBytes());
        final FileManagerEntry metadataEntry = new FileManagerEntry("metadata", metadata.getBytes());
        final FileManagerEntry scriptEntry = new FileManagerEntry("script", script.getBytes());
        return Arrays.asList(manifestEntry, metadataEntry, scriptEntry);
    }

    private static void copyBytes(InputStream is, OutputStream os) throws IOException {
        byte[] data = new byte[4096];
        int length;
        while ((length = is.read(data)) > 0) {
            os.write(data, 0, length);
        }
    }
}
