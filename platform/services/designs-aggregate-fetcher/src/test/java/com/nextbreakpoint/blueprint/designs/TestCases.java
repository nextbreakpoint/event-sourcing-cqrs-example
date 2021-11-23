package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.vertx.CassandraClientFactory;
import io.vertx.rxjava.cassandra.CassandraClient;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import org.jetbrains.annotations.NotNull;
import rx.Observable;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestCases {
    private final TestScenario scenario = new TestScenario();

    private final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private final Environment environment = Environment.getDefaultEnvironment();

    private TestCassandra testCassandra;

    private S3Client s3Client;

    public TestCases() {}

    public String getVersion() {
        return scenario.getVersion();
    }

    public TestScenario getScenario() {
        return scenario;
    }

    public void before() throws IOException, InterruptedException {
        scenario.before();

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));
        RxJavaHooks.setOnNewThreadScheduler(s -> RxHelper.blockingScheduler(vertx));

        CassandraClient session = CassandraClientFactory.create(environment, vertx, scenario.createCassandraConfig("test_designs_aggregate_fetcher"));

        testCassandra = new TestCassandra(session);

        s3Client = TestS3.createS3Client(URI.create("http://" + scenario.getMinioHost() + ":" + scenario.getMinioPort()));

        TestS3.deleteContent(s3Client, TestConstants.BUCKET, object -> true);
        TestS3.deleteBucket(s3Client, TestConstants.BUCKET);
        TestS3.createBucket(s3Client, TestConstants.BUCKET);
    }

    public void after() throws IOException, InterruptedException {
        try {
            vertx.rxClose()
                    .doOnError(Throwable::printStackTrace)
                    .subscribeOn(Schedulers.io())
                    .toCompletable()
                    .await();
        } catch (Exception ignore) {
        }

        try {
            if (s3Client != null) {
                s3Client.close();
            }
        } catch (Exception e) {
        }

        scenario.after();
    }

    public void deleteDesigns() {
        testCassandra.deleteDesigns();
    }

    public void insertDesign(Design design) {
        testCassandra.insertDesign(design);

        final byte[] data = makeImage(256);

        generateKeys(design)
            .doOnNext(key -> TestS3.putObject(s3Client, TestConstants.BUCKET, key, data))
            .ignoreElements()
            .toCompletable()
            .await();
    }

    @NotNull
    private byte[] makeImage(int size) {
        try {
            return Objects.requireNonNull(getClass().getResourceAsStream("/" + size + ".png")).readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private List<Tile> generateTiles(int level) {
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
    private Observable<String> generateKeys(Design design) {
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
    private Observable<String> generateKeys(Design design, int level) {
        if (design.getLevels() > level) {
            return Observable.from(generateTiles(level))
                    .map(tile -> createBucketKey(design, tile));
        } else {
            return Observable.empty();
        }
    }

    private String createBucketKey(Design design, Tile tile) {
        return String.format("%s/%d/%04d%04d.png", design.getChecksum(), tile.getLevel(), tile.getRow(), tile.getCol());
    }
}
