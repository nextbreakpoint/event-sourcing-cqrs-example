package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Tiles;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.test.TestContext;
import com.nextbreakpoint.blueprint.designs.TestActions.Source;
import com.nextbreakpoint.blueprint.designs.model.Design;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_DELETE_COMPLETED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_UPDATE_COMPLETED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static java.time.temporal.ChronoUnit.SECONDS;
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

        public Givens theDesignDocumentUpdateRequestedMessage(OutputMessage message) {
            final DesignDocumentUpdateRequested designDocumentUpdateRequested = Json.decodeValue(message.getValue().getData(), DesignDocumentUpdateRequested.class);
            context.putObject("designId", designDocumentUpdateRequested.getDesignId());
            context.putObject(designDocumentUpdateRequested.getDesignId() + ".userId", designDocumentUpdateRequested.getUserId());
            context.putObject(designDocumentUpdateRequested.getDesignId() + ".commandId", designDocumentUpdateRequested.getCommandId());
            context.putObject(designDocumentUpdateRequested.getDesignId() + ".data", designDocumentUpdateRequested.getData());
            context.putObject(designDocumentUpdateRequested.getDesignId() + ".status", designDocumentUpdateRequested.getStatus());
            context.putObject(designDocumentUpdateRequested.getDesignId() + ".checksum", Checksum.of(designDocumentUpdateRequested.getData()));
            context.putObject(designDocumentUpdateRequested.getDesignId() + ".revision", designDocumentUpdateRequested.getRevision());
            context.putObject(designDocumentUpdateRequested.getDesignId() + ".levels", designDocumentUpdateRequested.getLevels());
            context.putObject(designDocumentUpdateRequested.getDesignId() + ".tiles", designDocumentUpdateRequested.getTiles());
            context.putObject("message", message);
            return this;
        }

        public Givens theDesignDocumentDeleteRequestedMessage(OutputMessage message) {
            final DesignDocumentDeleteRequested designDocumentDeleteRequested = Json.decodeValue(message.getValue().getData(), DesignDocumentDeleteRequested.class);
            context.putObject("designId", designDocumentDeleteRequested.getDesignId());
            context.putObject(designDocumentDeleteRequested.getDesignId() + ".commandId", designDocumentDeleteRequested.getCommandId());
            context.putObject(designDocumentDeleteRequested.getDesignId() + ".revision", designDocumentDeleteRequested.getRevision());
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

        public Whens publishTheMessage() {
            publishTheMessage(Source.EVENTS, Function.identity());
            return this;
        }

        public Whens publishTheMessage(Source source, Function<String, String> topicMapper) {
            final OutputMessage message = (OutputMessage) context.getObject("message");

            actions.clearMessages(source);
            actions.emitMessage(source, message, topicMapper);

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

        public Thens aDocumentUpdateCompletedMessageShouldBePublished() {
            var designId = (UUID) context.getObject("designId");

            defaultAwait()
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = actions.findMessages(Source.EVENTS, MESSAGE_SOURCE, DESIGN_DOCUMENT_UPDATE_COMPLETED, key -> key.equals(designId.toString()), msg -> true);
                        assertThat(messages).hasSize(1);
                        context.putObject("message", messages.get(0));
                        TestAssertions.assertExpectedDesignDocumentUpdateCompletedMessage(messages.get(0), designId);
                    });

            return this;
        }

        public Thens aDocumentDeleteCompletedMessageShouldBePublished() {
            var designId = (UUID) context.getObject("designId");

            defaultAwait()
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = actions.findMessages(Source.EVENTS, MESSAGE_SOURCE, DESIGN_DOCUMENT_DELETE_COMPLETED, key -> key.equals(designId.toString()), msg -> true);
                        assertThat(messages).hasSize(1);
                        context.putObject("message", messages.get(0));
                        TestAssertions.assertExpectedDesignDocumentDeleteCompletedMessage(messages.get(0), designId);
                    });

            return this;
        }

        public Thens theDocumentUpdateCompletedEventShouldHaveExpectedValues() {
            var designId = (UUID) context.getObject("designId");
            var message = (InputMessage) context.getObject("message");

            defaultAwait()
                    .untilAsserted(() -> {
                        DesignDocumentUpdateCompleted designDocumentUpdateCompleted = Json.decodeValue(message.getValue().getData(), DesignDocumentUpdateCompleted.class);
                        TestAssertions.assertExpectedDesignDocumentUpdateCompletedEvent(designDocumentUpdateCompleted, designId);
                    });

            return this;
        }

        public Thens theDocumentDeleteCompletedEventShouldHaveExpectedValues() {
            var designId = (UUID) context.getObject("designId");
            var message = (InputMessage) context.getObject("message");

            defaultAwait()
                    .untilAsserted(() -> {
                        DesignDocumentDeleteCompleted designDocumentDeleteCompleted = Json.decodeValue(message.getValue().getData(), DesignDocumentDeleteCompleted.class);
                        TestAssertions.assertExpectedDesignDocumentDeleteCompletedEvent(designDocumentDeleteCompleted, designId);
                    });

            return this;
        }

        public Thens theDesignDocumentShouldBeUpdated() {
            var designId = (UUID) context.getObject("designId");
            var userId = (UUID) context.getObject(designId + ".userId");
            var commandId = (UUID) context.getObject(designId + ".commandId");
            var data = (String) context.getObject(designId + ".data");
            var checksum = (String) context.getObject(designId + ".checksum");
            var revision = (String) context.getObject(designId + ".revision");
            var status = (String) context.getObject(designId + ".status");
            var levels = (Integer) context.getObject(designId + ".levels");
            var tiles = (List<Tiles>) context.getObject(designId + ".tiles");

            defaultAwait()
                    .atMost(Duration.of(20, SECONDS))
                    .untilAsserted(() -> {
                        final List<Design> designs = actions.findDesigns(designId);
                        assertThat(designs).hasSize(1);
                        TestAssertions.assertExpectedDesign(designs.get(0), designId, commandId, userId, data, checksum, revision, status, tiles, levels);
                    });

            return this;
        }

        public Thens theDraftDesignDocumentShouldBeUpdated() {
            var designId = (UUID) context.getObject("designId");
            var userId = (UUID) context.getObject(designId + ".userId");
            var commandId = (UUID) context.getObject(designId + ".commandId");
            var data = (String) context.getObject(designId + ".data");
            var checksum = (String) context.getObject(designId + ".checksum");
            var revision = (String) context.getObject(designId + ".revision");
            var status = (String) context.getObject(designId + ".status");
            var levels = (Integer) context.getObject(designId + ".levels");
            var tiles = (List<Tiles>) context.getObject(designId + ".tiles");

            defaultAwait()
                    .atMost(Duration.of(20, SECONDS))
                    .untilAsserted(() -> {
                        final List<Design> designs = actions.findDraftDesigns(designId);
                        assertThat(designs).hasSize(1);
                        TestAssertions.assertExpectedDesign(designs.get(0), designId, commandId, userId, data, checksum, revision, status, tiles, levels);
                    });

            return this;
        }

        public Thens theDesignDocumentShouldNotExist() {
            var designId = (UUID) context.getObject("designId");

            defaultAwait()
                    .atMost(Duration.of(20, SECONDS))
                    .untilAsserted(() -> {
                        final List<Design> designs = actions.findDesigns(designId);
                        assertThat(designs).isEmpty();
                    });

            return this;
        }

        public Thens theDraftDesignDocumentShouldNotExist() {
            var designId = (UUID) context.getObject("designId");

            defaultAwait()
                    .atMost(Duration.of(20, SECONDS))
                    .untilAsserted(() -> {
                        final List<Design> designs = actions.findDraftDesigns(designId);
                        assertThat(designs).isEmpty();
                    });

            return this;
        }
    }
}
