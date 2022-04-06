package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import org.jetbrains.annotations.NotNull;
import rx.Single;
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.UUID;

public class TestCassandra {
    private CqlSession session;

    public TestCassandra(CqlSession session) {
        this.session = session;
    }

    @NotNull
    public List<Row> fetchMessages(UUID designId) {
        return Single.fromCallable(() -> session.execute(session.prepare("SELECT * FROM MESSAGE WHERE MESSAGE_KEY = ?").bind(designId.toString()).setConsistencyLevel(ConsistencyLevel.QUORUM)).all())
                .subscribeOn(Schedulers.immediate())
                .toBlocking()
                .value();
    }

    public void deleteMessages() {
        Single.fromCallable(() -> session.execute(session.prepare("TRUNCATE TABLE MESSAGE").bind().setConsistencyLevel(ConsistencyLevel.QUORUM)))
                .subscribeOn(Schedulers.immediate())
                .toBlocking()
                .value();
    }
}
