package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.cql.Row;
import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.TilesBitmap;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdated;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.test.TestContext;
import com.nextbreakpoint.blueprint.designs.TestActions.Source;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;

public class TestSteps {
    private final TestContext context;
    private final TestActions actions;

    public TestSteps(TestContext context, TestActions actions) {
        this.context = Objects.requireNonNull(context);
        this.actions = Objects.requireNonNull(actions);
    }

    public Givens given() {
        return new Givens(context, actions);
    }

    private static ConditionFactory defaultAwait() {
        return Awaitility.await().atMost(TEN_SECONDS).pollInterval(ONE_SECOND);
    }

    public void reset() {
        context.clear();
    }

    public static class Givens {
        private final TestContext context;
        private final TestActions actions;

        public Givens(TestContext context, TestActions actions) {
            this.context = Objects.requireNonNull(context);
            this.actions = Objects.requireNonNull(actions);
        }

        public Givens and() {
            return this;
        }

        public Whens when() {
            return new Whens(context, actions);
        }

        public Givens theDesignInsertRequestedMessage(OutputMessage message) {
            final DesignInsertRequested designInsertRequested = Json.decodeValue(message.getValue().getData(), DesignInsertRequested.class);
            context.putObject("designId", designInsertRequested.getDesignId());
            context.putObject(designInsertRequested.getDesignId() + ".userId", designInsertRequested.getUserId());
            context.putObject(designInsertRequested.getDesignId() + ".commandId", designInsertRequested.getCommandId());
            context.putObject(designInsertRequested.getDesignId() + ".data", designInsertRequested.getData());
            context.putObject(designInsertRequested.getDesignId() + ".checksum", Checksum.of(designInsertRequested.getData()));
            context.putObject(designInsertRequested.getDesignId() + ".published", false);
            context.putObject("message", message);
            return this;
        }

        public Givens theDesignUpdateRequestedMessage(OutputMessage message) {
            final DesignUpdateRequested designUpdateRequested = Json.decodeValue(message.getValue().getData(), DesignUpdateRequested.class);
            context.putObject("designId", designUpdateRequested.getDesignId());
            context.putObject(designUpdateRequested.getDesignId() + ".userId", designUpdateRequested.getUserId());
            context.putObject(designUpdateRequested.getDesignId() + ".commandId", designUpdateRequested.getCommandId());
            context.putObject(designUpdateRequested.getDesignId() + ".data", designUpdateRequested.getData());
            context.putObject(designUpdateRequested.getDesignId() + ".checksum", Checksum.of(designUpdateRequested.getData()));
            context.putObject(designUpdateRequested.getDesignId() + ".published", designUpdateRequested.getPublished());
            context.putObject("message", message);
            return this;
        }

        public Givens theDesignDeleteRequestedMessage(OutputMessage message) {
            final DesignDeleteRequested designDeleteRequested = Json.decodeValue(message.getValue().getData(), DesignDeleteRequested.class);
            context.putObject("designId", designDeleteRequested.getDesignId());
            context.putObject(designDeleteRequested.getDesignId() + ".userId", designDeleteRequested.getUserId());
            context.putObject(designDeleteRequested.getDesignId() + ".commandId", designDeleteRequested.getCommandId());
            context.putObject("message", message);
            return this;
        }

        public Givens theTileRenderCompletedMessage(OutputMessage message) {
            final TileRenderCompleted tileRenderCompleted = Json.decodeValue(message.getValue().getData(), TileRenderCompleted.class);
            context.putObject("designId", tileRenderCompleted.getDesignId());
            context.putObject(tileRenderCompleted.getDesignId() + ".commandId", tileRenderCompleted.getCommandId());
            context.putObject(tileRenderCompleted.getDesignId() + ".revision", tileRenderCompleted.getRevision());
            context.putObject("message", message);
            return this;
        }
    }

    public static class Whens {
        private final TestContext context;
        private final TestActions actions;

        public Whens(TestContext context, TestActions actions) {
            this.context = Objects.requireNonNull(context);
            this.actions = Objects.requireNonNull(actions);
        }

        public Givens given() {
            return new Givens(context, actions);
        }

        public Whens and() {
            return this;
        }

        public Thens then() {
            return new Thens(context, actions);
        }

        public Whens publishTheMessage(Source source) {
            publishTheMessage(source, Function.identity());
            return this;
        }

        public Whens publishTheMessage(Source source, Function<String, String> topicMapper) {
            final OutputMessage message = (OutputMessage) context.getObject("message");
            actions.emitMessage(source, message, topicMapper);
            return this;
        }

        public Whens discardReceivedMessages(Source source) {
            actions.clearMessages(source);
            return this;
        }

        public Whens selectDesignId(UUID designId) {
            context.putObject("designId", designId);
            return this;
        }
    }

    public static class Thens {
        private final TestContext context;
        private final TestActions actions;

        public Thens(TestContext context, TestActions actions) {
            this.context = Objects.requireNonNull(context);
            this.actions = Objects.requireNonNull(actions);
        }

        public Givens given() {
            return new Givens(context, actions);
        }

        public Whens when() {
            return new Whens(context, actions);
        }

        public Thens and() {
            return this;
        }

        public Thens aMessageShouldBePublished(Source source, String messageType) {
            manyMessagesShouldBePublished(source, messageType, key -> true, message -> true, 1);
            return this;
        }

        public Thens aMessageShouldBePublished(Source source, String messageType, Predicate<String> keyPredicate) {
            manyMessagesShouldBePublished(source, messageType, keyPredicate, message -> true, 1);
            return this;
        }

        public Thens manyMessagesShouldBePublished(Source source, String messageType, Predicate<String> keyPredicate, int count) {
            manyMessagesShouldBePublished(source, messageType, keyPredicate, message -> true, count);
            return this;
        }

        private Thens manyMessagesShouldBePublished(Source source, String messageType, Predicate<String> keyPredicate, Predicate<InputMessage> messagePredicate, int count) {
            defaultAwait()
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = actions.findMessages(source, MESSAGE_SOURCE, messageType, keyPredicate, messagePredicate);
                        assertThat(messages).hasSize(count);
                        context.putObject("messages", messages);
                    });

            return this;
        }

        public Thens theMessageShouldBeSaved() {
            var designId = (UUID) context.getObject("designId");
            var message = (OutputMessage) context.getObject("message");

            defaultAwait()
                    .untilAsserted(() -> {
                        final List<Row> rows = actions.fetchMessages(designId, message.getValue().getUuid());
                        assertThat(rows).hasSize(1);
                        TestAssertions.assertExpectedMessage(rows.get(0), message);
                    });

            return this;
        }

        public Thens theDesignShouldBeSaved(String status, int levels) {
            theDesignShouldBeSaved(status, TilesBitmap.empty(), levels);
            return this;
        }

        public Thens theDesignShouldBeSaved(String status, TilesBitmap bitmap, int levels) {
            var designId = (UUID) context.getObject("designId");
            var data = (String) context.getObject(designId + ".data");

            defaultAwait()
                    .untilAsserted(() -> {
                        final List<Row> rows = actions.fetchDesigns(designId);
                        assertThat(rows).hasSize(1);
                        TestAssertions.assertExpectedDesign(rows.get(0), data, status, bitmap.getBitmap().array(), levels);
                    });

            return this;
        }

        public Thens theAggregateUpdatedMessageHasExpectedValues(String status, int levels) {
            var designId = (UUID) context.getObject("designId");
            var data = (String) context.getObject(designId + ".data");
            var checksum = (String) context.getObject(designId + ".checksum");
            var messages = (List<InputMessage>) context.getObject("messages");

            defaultAwait()
                    .untilAsserted(() -> {
                        TestAssertions.assertExpectedDesignAggregateUpdatedMessage(messages.get(0), designId);
                        DesignAggregateUpdated actualEvent = TestUtils.extractDesignAggregateUpdatedEvent(messages.get(0));
                        TestAssertions.assertExpectedDesignAggregateUpdatedEvent(actualEvent, designId, data, checksum, status, levels);
                    });

            return this;
        }

        public Thens theTileRenderRequestedMessagesHaveExpectedValues(int levels) {
            var designId = (UUID) context.getObject("designId");
            var data = (String) context.getObject(designId + ".data");
            var checksum = (String) context.getObject(designId + ".checksum");
            var messages = (List<InputMessage>) context.getObject("messages");

            defaultAwait()
                    .untilAsserted(() -> {
                        assertThat(messages).hasSize(TestUtils.totalTilesByLevels(levels));
                        messages.forEach(message -> TestAssertions.assertExpectedTileRenderRequestedMessage(message, designId));
                        List<TileRenderRequested> events = TestUtils.extractTileRenderRequestedEvents(messages, event -> event.getChecksum().equals(checksum));
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> TestAssertions.assertExpectedTileRenderRequestedEvent(event, designId, data, checksum));
                    });

            return this;
        }

        public Thens theDesignDocumentUpdateRequestedMessageHasExpectedValues(String status, int levels) {
            var designId = (UUID) context.getObject("designId");
            var userId = (UUID) context.getObject(designId + ".userId");
            var data = (String) context.getObject(designId + ".data");
            var checksum = (String) context.getObject(designId + ".checksum");
            var published = (Boolean) context.getObject(designId + ".published");
            var messages = (List<InputMessage>) context.getObject("messages");

            defaultAwait()
                    .untilAsserted(() -> {
                        TestAssertions.assertExpectedDesignDocumentUpdateRequestedMessage(messages.get(0), designId);
                        DesignDocumentUpdateRequested actualEvent = TestUtils.extractDesignDocumentUpdateRequestedEvent(messages.get(0));
                        TestAssertions.assertExpectedDesignDocumentUpdateRequestedEvent(actualEvent, designId, userId, data, checksum, status, levels, published);
                    });

            return this;
        }

        public Thens theDesignDocumentDeleteRequestedMessageHasExpectedValues() {
            var designId = (UUID) context.getObject("designId");
            var messages = (List<InputMessage>) context.getObject("messages");

            defaultAwait()
                    .untilAsserted(() -> {
                        TestAssertions.assertExpectedDesignDocumentDeleteRequestedMessage(messages.get(0), designId);
                        DesignDocumentDeleteRequested actualEvent = TestUtils.extractDesignDocumentDeleteRequestedEvent(messages.get(0));
                        TestAssertions.assertExpectedDesignDocumentDeleteRequestedEvent(actualEvent, designId);
                    });

            return this;
        }
    }
}
