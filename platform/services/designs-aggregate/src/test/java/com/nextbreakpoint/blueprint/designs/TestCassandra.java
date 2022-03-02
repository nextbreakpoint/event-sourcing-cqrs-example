package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.Row;
import io.vertx.rxjava.cassandra.CassandraClient;
import org.jetbrains.annotations.NotNull;
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.UUID;

public class TestCassandra {
    private CassandraClient session;

    public TestCassandra(CassandraClient session) {
        this.session = session;
    }

    @NotNull
    public List<Row> fetchMessages(UUID designId) {
        return session.rxPrepare("SELECT * FROM MESSAGE WHERE MESSAGE_KEY = ?")
                .map(stmt -> stmt.bind(designId.toString()).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .flatMap(session::rxExecuteWithFullFetch)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();
    }

    @NotNull
    public List<Row> fetchDesigns(UUID designId) {
        return session.rxPrepare("SELECT * FROM DESIGN WHERE DESIGN_UUID = ?")
                .map(stmt -> stmt.bind(designId).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .flatMap(session::rxExecuteWithFullFetch)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();
    }

    public void deleteMessages() {
        session.rxPrepare("TRUNCATE TABLE MESSAGE")
                .map(stmt -> stmt.bind().setConsistencyLevel(ConsistencyLevel.QUORUM))
                .flatMap(session::rxExecute)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();
    }

    public void deleteDesigns() {
        session.rxPrepare("TRUNCATE TABLE DESIGN")
                .map(stmt -> stmt.bind().setConsistencyLevel(ConsistencyLevel.QUORUM))
                .flatMap(session::rxExecute)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();
    }

//    public void insertDesign(Design design) {
//        session.rxMetadata()
//                .map(metadata -> metadata.getKeyspace("designs").orElseThrow())
//                .map(keyspaceMetadata -> keyspaceMetadata.getUserDefinedType("LEVEL").orElseThrow(() -> new RuntimeException("UDT not found: LEVEL")))
//                .flatMap(levelType -> insertDesign(design, levelType))
//                .toCompletable()
//                .await();
//    }
//
//    private Single<ResultSet> insertDesign(Design design, UserDefinedType levelType) {
//        return session.rxPrepare("INSERT INTO DESIGN (DESIGN_UUID, DESIGN_EVID, DESIGN_ESID, DESIGN_DATA, DESIGN_CHECKSUM, DESIGN_STATUS, DESIGN_LEVELS, DESIGN_TILES, DESIGN_TIMESTAMP) VALUES (?,?,?,?,?,?,?,?,?)")
//                .map(stmt -> stmt.bind(createInsertParams(design, levelType)).setConsistencyLevel(ConsistencyLevel.QUORUM))
//                .flatMap(session::rxExecute);
//    }
//
//    private Object[] createInsertParams(Design design, UserDefinedType levelType) {
//        final Map<Integer, UdtValue> levelsMap = design.getTiles().stream().collect(Collectors.toMap(Tiles::getLevel, x -> convertTilesToUDT(levelType, x)));
//        return new Object[] { design.getUuid(), design.getEvid(), design.getEsid(), design.getJson(), Checksum.of(design.getJson()), design.getStatus(), design.getLevels(), levelsMap, design.getUpdated().toInstant() };
//    }
//
//    private UdtValue convertTilesToUDT(UserDefinedType levelType, Tiles tiles) {
//        return levelType.newValue()
//                .setInt("REQUESTED", tiles.getRequested())
//                .setSet("COMPLETED", tiles.getCompleted(), Integer.class)
//                .setSet("FAILED", tiles.getFailed(), Integer.class);
//    }
}
