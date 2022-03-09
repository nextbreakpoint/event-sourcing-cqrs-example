package com.nextbreakpoint.blueprint.designs.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextbreakpoint.Try;
import com.nextbreakpoint.nextfractal.core.common.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.nextbreakpoint.nextfractal.core.common.Plugins.tryFindFactory;

public class BundleUtils {
    private BundleUtils() {}

    public static Bundle createBundle(String manifest, String metadata, String script) throws Exception {
        final FileManagerEntry manifestEntry = new FileManagerEntry("manifest", manifest.getBytes());
        final FileManagerEntry metadataEntry = new FileManagerEntry("metadata", metadata.getBytes());
        final FileManagerEntry scriptEntry = new FileManagerEntry("script", script.getBytes());

        final List<FileManagerEntry> entries = Arrays.asList(manifestEntry, metadataEntry, scriptEntry);

        final ObjectMapper mapper = new ObjectMapper();

        final FileManifest decodedManifest = mapper.readValue(manifestEntry.getData(), FileManifest.class);

        return Plugins.tryFindFactory(decodedManifest.getPluginId())
                .flatMap(factory -> factory.createFileManager().loadEntries(entries)).orThrow();
    }

    public static Try<byte[], Exception> writeBundle(Bundle bundle) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream os = new ZipOutputStream(baos)) {
            writeBundle(os, bundle).execute();
        } catch (Exception e) {
            return Try.failure(e);
        }

        return Try.of(baos::toByteArray);
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
