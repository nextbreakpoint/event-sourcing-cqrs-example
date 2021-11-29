package com.nextbreakpoint.blueprint.designs;

import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.config.SSLConfig;
import org.jetbrains.annotations.NotNull;
import rx.Observable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestUtils {
    private TestUtils() {}

    @NotNull
    public static RestAssuredConfig getRestAssuredConfig() {
        final SSLConfig sslConfig = new SSLConfig().allowAllHostnames().and().relaxedHTTPSValidation();
        final RedirectConfig redirectConfig = new RedirectConfig().followRedirects(false);
        final LogConfig logConfig = new LogConfig().enableLoggingOfRequestAndResponseIfValidationFails();
        return RestAssuredConfig.newConfig().redirect(redirectConfig).sslConfig(sslConfig).logConfig(logConfig);
    }

    @NotNull
    public static Map<String, Object> createPostData(String manifest, String metadata, String script) {
        final Map<String, Object> data = new HashMap<>();
        data.put("manifest", manifest);
        data.put("metadata", metadata);
        data.put("script", script);
        return data;
    }

    @NotNull
    public static String createBucketKey(Design design, Tile tile) {
        return String.format("%s/%d/%04d%04d.png", design.getChecksum(), tile.getLevel(), tile.getRow(), tile.getCol());
    }

    @NotNull
    public static byte[] makeImage(int size) {
        try {
            return Objects.requireNonNull(TestUtils.class.getResourceAsStream("/" + size + ".png")).readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public static Observable<String> generateKeys(Design design) {
        return generateKeys(design, 0)
                .concatWith(generateKeys(design, 1))
                .concatWith(generateKeys(design, 2))
                .concatWith(generateKeys(design, 3))
                .concatWith(generateKeys(design, 4))
                .concatWith(generateKeys(design, 5))
                .concatWith(generateKeys(design, 6))
                .concatWith(generateKeys(design, 7));
    }

    @NotNull
    private static Observable<String> generateKeys(Design design, int level) {
        if (design.getLevels() > level) {
            return rx.Observable.from(generateTiles(level))
                    .map(tile -> TestUtils.createBucketKey(design, tile));
        } else {
            return Observable.empty();
        }
    }

    @NotNull
    private static List<Tile> generateTiles(int level) {
        final int size = (int) Math.rint(Math.pow(2, level));
        return IntStream.range(0, size)
                .boxed()
                .flatMap(row ->
                        IntStream.range(0, size)
                                .boxed()
                                .map(col -> new Tile(level, row, col))
                )
                .collect(Collectors.toList());
    }
}
