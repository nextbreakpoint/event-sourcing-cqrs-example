package com.nextbreakpoint.blueprint.designs.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextbreakpoint.Try;
import com.nextbreakpoint.nextfractal.core.common.Bundle;
import com.nextbreakpoint.nextfractal.core.common.CoreFactory;
import com.nextbreakpoint.nextfractal.core.common.FileManagerEntry;
import com.nextbreakpoint.nextfractal.core.common.FileManifest;
import com.nextbreakpoint.nextfractal.core.common.Plugins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.nextbreakpoint.nextfractal.core.common.Plugins.tryFindFactory;

public class BundleUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    private BundleUtils() {}

    public static Try<Bundle, Exception> createBundle(String manifest, String metadata, String script) {
        final FileManagerEntry manifestEntry = new FileManagerEntry("manifest", manifest.getBytes());
        final FileManagerEntry metadataEntry = new FileManagerEntry("metadata", metadata.getBytes());
        final FileManagerEntry scriptEntry = new FileManagerEntry("script", script.getBytes());

        final List<FileManagerEntry> entries = Arrays.asList(manifestEntry, metadataEntry, scriptEntry);

        return Try.of(() -> mapper.readValue(manifestEntry.getData(), FileManifest.class))
                .flatMap(decodedManifest -> createBundle(decodedManifest, entries));
    }

    public static Try<byte[], Exception> writeBundle(Bundle bundle) {
        return Try.of(() -> {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                writeBundle(bundle, baos).orThrow();
                return baos.toByteArray();
            }
        });
    }

    public static Try<Bundle, Exception> writeBundle(Bundle bundle, OutputStream stream) {
        return Try.of(() -> {
            try (ZipOutputStream os = new ZipOutputStream(stream)) {
                writeBundle(os, bundle).orThrow();
                return bundle;
            }
        });
    }

    private static Try<Bundle, Exception> createBundle(FileManifest manifest, List<FileManagerEntry> entries) {
        return Plugins.tryFindFactory(manifest.getPluginId())
                .flatMap(factory -> factory.createFileManager().loadEntries(entries));
    }

    private static Try<Bundle, Exception> writeBundle(ZipOutputStream os, Bundle bundle) {
        return tryFindFactory(bundle.getSession().getPluginId()).map(CoreFactory::createFileManager)
                .flatMap(manager -> manager.saveEntries(bundle).flatMap(entries -> putEntries(os, bundle, entries)));
    }

    private static Try<Bundle, Exception> putEntries(ZipOutputStream os, Bundle bundle, List<FileManagerEntry> entries) {
        return entries.stream().map(entry -> Try.of(() -> putEntry(os, bundle, entry)))
                .filter(Try::isFailure).findFirst().orElse(Try.success(bundle));
    }

    private static Bundle putEntry(ZipOutputStream os, Bundle bundle, FileManagerEntry entry) throws IOException {
        ZipEntry zipEntry = new ZipEntry(entry.getName());
        os.putNextEntry(zipEntry);
        os.write(entry.getData());
        os.closeEntry();
        return bundle;
    }
}
