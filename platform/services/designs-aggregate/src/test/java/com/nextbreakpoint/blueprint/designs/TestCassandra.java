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
    public List<Row> fetchMessage(UUID uuid) {
        return session.rxPrepare("SELECT * FROM MESSAGE WHERE MESSAGE_UUID = ?")
                .map(stmt -> stmt.bind(uuid).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .flatMap(session::rxExecuteWithFullFetch)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();
    }

    @NotNull
    public List<Row> fetchDesign(UUID designId) {
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
}
