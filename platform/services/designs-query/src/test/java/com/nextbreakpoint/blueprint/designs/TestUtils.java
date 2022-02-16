package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.core.model.messaging.Message;
import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.config.SSLConfig;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.KafkaRecord;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Tracing;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.model.Tiles;
import org.jetbrains.annotations.NotNull;
import rx.Observable;

import java.io.IOException;
import java.util.*;
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
    public static List<Tiles> getTiles(int levels, float completePercentage) {
        return IntStream.range(0, levels)
                .mapToObj(level -> makeTiles(level, completePercentage))
                .collect(Collectors.toList());
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

    @NotNull
    private static Tiles makeTiles(int level, float completePercentage) {
        final int requested = (int) Math.rint(Math.pow(2, level * 2));

        final int completedCount = (int) Math.rint(completePercentage * requested);

        final Set<Integer> completed = TestUtils.generateTiles(level)
                .stream()
                .limit(completedCount)
                .map(tile -> tile.getCol() << 16 | tile.getRow())
                .collect(Collectors.toSet());

        return new Tiles(level, requested, completed, new HashSet<>());
    }

    @NotNull
    public static OutputMessage toOutputMessage(Message message) {
        final KafkaRecord kafkaRecord = Json.decodeValue(message.contentsAsString(), KafkaRecord.class);
        return OutputMessage.from(kafkaRecord.getKey(), PayloadUtils.mapToPayload(kafkaRecord.getValue()), Tracing.from(kafkaRecord.getHeaders()));
    }
}
