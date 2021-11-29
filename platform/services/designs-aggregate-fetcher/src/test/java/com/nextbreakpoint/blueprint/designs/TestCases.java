package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.provider.junit5.HttpsTestTarget;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.vertx.CassandraClientFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.cassandra.CassandraClient;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import org.jetbrains.annotations.NotNull;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class TestCases {
    private final TestScenario scenario = new TestScenario();

    private final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private final Environment environment = Environment.getDefaultEnvironment();

    private TestCassandra testCassandra;

    private S3Client s3Client;

    public TestCases() {}

    public void before() {
        scenario.before();

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));
        RxJavaHooks.setOnNewThreadScheduler(s -> RxHelper.blockingScheduler(vertx));

        CassandraClient session = CassandraClientFactory.create(environment, vertx, createCassandraConfig());

        testCassandra = new TestCassandra(session);

        s3Client = TestS3.createS3Client(URI.create("http://" + scenario.getMinioHost() + ":" + scenario.getMinioPort()));

        TestS3.deleteContent(s3Client, TestConstants.BUCKET, object -> true);
        TestS3.deleteBucket(s3Client, TestConstants.BUCKET);
        TestS3.createBucket(s3Client, TestConstants.BUCKET);
    }

    public void after() {
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
        } catch (Exception ignore) {
        }

        scenario.after();
    }

    @NotNull
    public String getVersion() {
        return scenario.getVersion();
    }

    @NotNull
    public URL makeBaseURL(String path) throws MalformedURLException {
        final String normPath = path.startsWith("/") ? path.substring(1) : path;
        return new URL("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/" + normPath);
    }

    @NotNull
    public String makeAuthorization(String user, String role) {
        return scenario.makeAuthorization(user, role);
    }

    @NotNull
    public String getOriginUrl() {
        return "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort();
    }

    @NotNull
    public HttpsTestTarget getHttpsTestTarget() {
        return new HttpsTestTarget(scenario.getServiceHost(), scenario.getServicePort(), "/", true);
    }

    @NotNull
    public JsonObject createCassandraConfig() {
        final JsonObject config = new JsonObject();
        config.put("cassandra_contactPoints", scenario.getCassandraHost());
        config.put("cassandra_port", scenario.getCassandraPort());
        config.put("cassandra_cluster", "datacenter1");
        config.put("cassandra_keyspace", TestConstants.DATABASE_KEYSPACE);
        config.put("cassandra_username", "admin");
        config.put("cassandra_password", "password");
        return config;
    }

    public void deleteDesigns() {
        testCassandra.deleteDesigns();
    }

    public void insertDesign(Design design) {
        testCassandra.insertDesign(design);

        final byte[] data = TestUtils.makeImage(256);

        TestUtils.generateKeys(design)
                .doOnNext(key -> TestS3.putObject(s3Client, TestConstants.BUCKET, key, data))
                .ignoreElements()
                .toCompletable()
                .await();
    }
}
