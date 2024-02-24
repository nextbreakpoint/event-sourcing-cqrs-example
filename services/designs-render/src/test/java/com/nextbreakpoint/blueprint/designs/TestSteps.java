package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.test.TestContext;
import com.nextbreakpoint.blueprint.designs.TestActions.Source;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;

import java.util.List;
import java.util.Objects;
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

        public Givens theTileRenderRequestedMessage(OutputMessage<TileRenderRequested> message) {
            final TileRenderRequested tileRenderRequested = message.getValue().getData();
            context.putObject("designId", tileRenderRequested.getDesignId());
            context.putObject(tileRenderRequested.getDesignId() + ".commandId", tileRenderRequested.getCommandId());
            context.putObject(tileRenderRequested.getDesignId() + ".data", tileRenderRequested.getData());
            context.putObject(tileRenderRequested.getDesignId() + ".checksum", tileRenderRequested.getChecksum());
            context.putObject(tileRenderRequested.getDesignId() + ".revision", tileRenderRequested.getRevision());
            context.putObject(tileRenderRequested.getDesignId() + ".level", tileRenderRequested.getLevel());
            context.putObject(tileRenderRequested.getDesignId() + ".row", tileRenderRequested.getRow());
            context.putObject(tileRenderRequested.getDesignId() + ".col", tileRenderRequested.getCol());
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
            final OutputMessage<Object> message = (OutputMessage<Object>) context.getObject("message");
            actions.emitMessage(source, message, topicMapper);
            context.putObject("outputMessage", message);
            return this;
        }

        public Whens discardReceivedMessages(Source source) {
            actions.clearMessages(source);
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

        private Thens manyMessagesShouldBePublished(Source source, String messageType, Predicate<String> keyPredicate, Predicate<InputMessage<Object>> messagePredicate, int count) {
            defaultAwait()
                    .untilAsserted(() -> {
                        final List<InputMessage<Object>> messages = actions.findMessages(source, MESSAGE_SOURCE, messageType, keyPredicate, messagePredicate);
                        assertThat(messages).hasSize(count);
                        context.putObject("messages", messages);
                    });

            return this;
        }

        public Thens theTileRenderCompletedMessageShouldHaveExpectedValues() {
            var lastMessage = (OutputMessage<Object>) context.getObject("outputMessage");
            var messages = (List<InputMessage<Object>>) context.getObject("messages");

            defaultAwait()
                    .untilAsserted(() -> {
                        final TileRenderRequested tileRenderRequested = TestUtils.extractTileRenderRequestedEvent(lastMessage);
                        TestAssertions.assertExpectedTileRenderCompletedMessage(messages.get(0), tileRenderRequested);
                    });

            return this;
        }

        public Thens theTileRenderCompletedEventShouldHaveExpectedValues() {
            var lastMessage = (OutputMessage<Object>) context.getObject("outputMessage");
            var messages = (List<InputMessage<Object>>) context.getObject("messages");

            defaultAwait()
                    .untilAsserted(() -> {
                        final TileRenderRequested tileRenderRequested = TestUtils.extractTileRenderRequestedEvent(lastMessage);
                        TileRenderCompleted tileRenderCompleted = (TileRenderCompleted) messages.get(0).getValue().getData();
                        TestAssertions.assertExpectedTileRenderCompletedEvent(tileRenderCompleted, tileRenderRequested);
                    });

            return this;
        }

        public Thens theImageShouldHasBeenCreated() {
            var lastMessage = (OutputMessage<Object>) context.getObject("outputMessage");
            var tileRenderRequested = TestUtils.extractTileRenderRequestedEvent(lastMessage);
            var bytes = actions.getImage(TestUtils.createTileKey(tileRenderRequested));
            assertThat(bytes).isNotEmpty();
            return this;
        }
    }
}
