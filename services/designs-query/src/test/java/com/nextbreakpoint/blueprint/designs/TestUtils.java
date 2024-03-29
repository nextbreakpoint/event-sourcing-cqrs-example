package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.core.model.V4Interaction;
import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Time;
import com.nextbreakpoint.blueprint.common.events.avro.Tile;
import com.nextbreakpoint.blueprint.common.events.avro.Tiles;
import com.nextbreakpoint.blueprint.common.test.KafkaRecord;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.model.LevelTiles;
import io.restassured.config.LogConfig;
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import org.apache.avro.specific.SpecificRecord;
import org.jetbrains.annotations.NotNull;
import rx.Observable;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_1;

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
    public static String createTileKey(Design design, Tile tile) {
        return String.format("tiles/%s/%d/%04d%04d.png", design.getChecksum(), tile.getLevel(), tile.getRow(), tile.getCol());
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
                .concatWith(generateKeys(design, 2));
    }

    @NotNull
    public static List<Tiles> getTiles(int levels, float completePercentage) {
        return IntStream.range(0, levels)
                .mapToObj(level -> makeTiles(level, completePercentage))
                .collect(Collectors.toList());
    }

    @NotNull
    private static Tiles makeTiles(int level, float completePercentage) {
        final int total = (int) Math.rint(Math.pow(2, level * 2));
        final int completed = (int) Math.rint((completePercentage * total) / 100f);
        return new Tiles(level, total, completed);
    }

    @NotNull
    private static Observable<String> generateKeys(Design design, int level) {
        if (design.getLevels() > level) {
            return rx.Observable.from(generateTiles(level))
                    .map(tile -> TestUtils.createTileKey(design, tile));
        } else {
            return Observable.empty();
        }
    }

    @NotNull
    private static List<Tile> generateTiles(int level) {
        return makeAll(level, (int) Math.rint(Math.pow(2, level))).collect(Collectors.toList());
    }

    private static Stream<Tile> makeAll(int level, int size) {
        return IntStream.range(0, size)
                .boxed()
                .flatMap(row -> makeRow(level, row, size));
    }

    private static Stream<Tile> makeRow(int level, int row, int size) {
        return IntStream.range(0, size)
                .boxed()
                .map(col -> new Tile(level, row, col));
    }

    @NotNull
    public static <T extends SpecificRecord> OutputMessage<T> toOutputMessage(V4Interaction.AsynchronousMessage message, Class<T> clazz) {
        final String json = message.getContents().getContents().valueAsString();
        final KafkaRecord kafkaRecord = Json.decodeValue(json, KafkaRecord.class);

        return OutputMessage.<T>builder()
                .withKey(kafkaRecord.getKey())
                .withValue(PayloadUtils.mapToPayload(kafkaRecord.getValue(), clazz))
                .build();
    }

    @NotNull
    public static List<LevelTiles> getLevelTiles(List<Tiles> tiles) {
        return tiles.stream().map(TestUtils::getLevelTiles).toList();
    }

    @NotNull
    public static LevelTiles getLevelTiles(Tiles tile) {
        return LevelTiles.builder()
                .withLevel(tile.getLevel())
                .withTotal(tile.getTotal())
                .withCompleted(tile.getCompleted())
                .build();
    }

    @NotNull
    public static Design aPublishedDesign(UUID designId, UUID commandId, String revision, Instant created, Instant updated, String data) {
        return aDesign(designId, commandId, revision, created, updated, 8, 100, true, data);
    }

    @NotNull
    public static Design aDraftDesign(UUID designId, UUID commandId, String revision, Instant created, Instant updated, String data) {
        return aDesign(designId, commandId, revision, created, updated, 3, 100, false, data);
    }

    @NotNull
    public static Design aDesign(UUID designId, UUID commandId, String revision, Instant created, Instant updated, int levels, int percentage, boolean published, String data) {
        return Design.builder()
                .withDesignId(designId)
                .withCommandId(commandId)
                .withUserId(USER_ID_1)
                .withData(data)
                .withChecksum(Checksum.of(data))
                .withRevision(revision)
                .withLevels(levels)
                .withTiles(TestUtils.getLevelTiles(TestUtils.getTiles(levels, percentage)))
                .withStatus("CREATED")
                .withPublished(published)
                .withCreated(Time.format(created))
                .withUpdated(Time.format(updated))
                .build();
    }
}
