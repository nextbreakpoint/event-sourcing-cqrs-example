package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.cql.Row;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.test.TestContext;
import com.nextbreakpoint.blueprint.designs.TestActions.Source;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;

import java.net.MalformedURLException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DELETE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_INSERT_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_UPDATE_REQUESTED;
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

        public Givens theDesignId(UUID designId) {
            context.putObject("designId", designId);
            return this;
        }

        public Givens theUserId(UUID userId) {
            context.putObject("userId", userId);
            return this;
        }

        public Givens anAuthorization(String authority) {
            var userId = (UUID) context.getObject("userId");
            context.putObject("authorization", actions.makeAuthorization(userId, authority));
            return this;
        }

        public Givens theManifest(String manifest) {
            context.putObject("manifest", manifest);
            return this;
        }

        public Givens theMetadata(String metadata) {
            context.putObject("metadata", metadata);
            return this;
        }

        public Givens theScript(String script) {
            context.putObject("script", script);
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

        public Whens submitInsertDesignRequest() throws MalformedURLException {
            var authorization = (String) context.getObject("authorization");
            var manifest = (String) context.getObject("manifest");
            var metadata = (String) context.getObject("metadata");
            var script = (String) context.getObject("script");
            var designId = actions.submitInsertDesignRequest(authorization, TestUtils.createPostData(manifest, metadata, script));
            context.putObject("designId", designId);
            return this;
        }

        public Whens submitUpdateDesignRequest() throws MalformedURLException {
            var authorization = (String) context.getObject("authorization");
            var manifest = (String) context.getObject("manifest");
            var metadata = (String) context.getObject("metadata");
            var script = (String) context.getObject("script");
            var designId = (UUID) context.getObject("designId");
            actions.submitUpdateDesignRequest(authorization, TestUtils.createPostData(manifest, metadata, script), designId);
            return this;
        }

        public Whens submitDeleteDesignRequest() throws MalformedURLException {
            var authorization = (String) context.getObject("authorization");
            var designId = (UUID) context.getObject("designId");
            actions.submitDeleteDesignRequest(authorization, designId);
            return this;
        }

        public Whens discardReceivedEvents() {
            actions.clearMessages(Source.EVENTS);
            return this;
        }

        public Whens discardReceivedCommands() {
            actions.clearMessages(Source.COMMANDS);
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

        public Thens aCommandMessageShouldBePublished(String messageType) {
            var designId = (UUID) context.getObject("designId");

            defaultAwait()
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = actions.findMessages(Source.COMMANDS, MESSAGE_SOURCE, messageType, key -> key.equals(designId.toString()), msg -> true);
                        assertThat(messages).hasSize(1);
                        context.putObject("messageId", messages.get(0).getValue().getUuid());
                    });

            return this;
        }

        public Thens aDesignInsertRequestedMessageShouldBePublished() {
            var designId = (UUID) context.getObject("designId");

            defaultAwait()
                    .atMost(Duration.ofSeconds(20))
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = actions.findMessages(Source.EVENTS, MESSAGE_SOURCE, DESIGN_INSERT_REQUESTED, key -> key.equals(designId.toString()), msg -> true);
                        assertThat(messages).hasSize(1);
                        context.putObject("message", messages.get(0));
                        TestAssertions.assertExpectedDesignInsertRequestedMessage(messages.get(0), designId.toString());
                    });

            return this;
        }

        public Thens aDesignUpdateRequestedMessageShouldBePublished() {
            var designId = (UUID) context.getObject("designId");

            defaultAwait()
                    .atMost(Duration.ofSeconds(20))
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = actions.findMessages(Source.EVENTS, MESSAGE_SOURCE, DESIGN_UPDATE_REQUESTED, key -> key.equals(designId.toString()), msg -> true);
                        assertThat(messages).hasSize(1);
                        context.putObject("message", messages.get(0));
                        TestAssertions.assertExpectedDesignUpdateRequestedMessage(messages.get(0), designId.toString());
                    });

            return this;
        }

        public Thens aDesignDeleteRequestedMessageShouldBePublished() {
            var designId = (UUID) context.getObject("designId");

            defaultAwait()
                    .atMost(Duration.ofSeconds(20))
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = actions.findMessages(Source.EVENTS, MESSAGE_SOURCE, DESIGN_DELETE_REQUESTED, key -> key.equals(designId.toString()), msg -> true);
                        assertThat(messages).hasSize(1);
                        context.putObject("message", messages.get(0));
                        TestAssertions.assertExpectedDesignDeleteRequestedMessage(messages.get(0), designId.toString());
                    });

            return this;
        }

        public Thens aDesignInsertCommandMessageShouldBeSaved() {
            var designId = (UUID) context.getObject("designId");
            var messageId = (UUID) context.getObject("messageId");

            defaultAwait()
                    .untilAsserted(() -> {
                        final List<Row> rows = actions.fetchMessages(designId, messageId);
                        assertThat(rows).hasSize(1);
                        TestAssertions.assertExpectedDesignInsertCommandMessage(rows.get(0), designId.toString());
                    });

            return this;
        }

        public Thens aDesignUpdateCommandMessageShouldBeSaved() {
            var designId = (UUID) context.getObject("designId");
            var messageId = (UUID) context.getObject("messageId");

            defaultAwait()
                    .untilAsserted(() -> {
                        final List<Row> rows = actions.fetchMessages(designId, messageId);
                        assertThat(rows).hasSize(1);
                        TestAssertions.assertExpectedDesignUpdateCommandMessage(rows.get(0), designId.toString());
                    });

            return this;
        }

        public Thens aDesignDeleteCommandMessageShouldBeSaved() {
            var designId = (UUID) context.getObject("designId");
            var messageId = (UUID) context.getObject("messageId");

            defaultAwait()
                    .untilAsserted(() -> {
                        final List<Row> rows = actions.fetchMessages(designId, messageId);
                        assertThat(rows).hasSize(1);
                        TestAssertions.assertExpectedDesignDeleteCommandMessage(rows.get(0), designId.toString());
                    });

            return this;
        }

        public Thens theDesignInsertRequestedEventShouldHaveExpectedValues() {
            var message = (InputMessage) context.getObject("message");
            var designId = (UUID) context.getObject("designId");
            var userId = (UUID) context.getObject("userId");

            defaultAwait()
                    .atMost(Duration.ofSeconds(20))
                    .untilAsserted(() -> {
                        DesignInsertRequested event = Json.decodeValue(message.getValue().getData(), DesignInsertRequested.class);
                        TestAssertions.assertExpectedDesignInsertRequestedEvent(event, designId, userId);
                    });

            return this;
        }

        public Thens theDesignUpdateRequestedEventShouldHaveExpectedValues() {
            var message = (InputMessage) context.getObject("message");
            var designId = (UUID) context.getObject("designId");
            var userId = (UUID) context.getObject("userId");

            defaultAwait()
                    .atMost(Duration.ofSeconds(20))
                    .untilAsserted(() -> {
                        DesignUpdateRequested event = Json.decodeValue(message.getValue().getData(), DesignUpdateRequested.class);
                        TestAssertions.assertExpectedDesignUpdateRequestedEvent(event, designId, userId);
                    });

            return this;
        }

        public Thens theDesignDeleteRequestedEventShouldHaveExpectedValues() {
            var message = (InputMessage) context.getObject("message");
            var designId = (UUID) context.getObject("designId");
            var userId = (UUID) context.getObject("userId");

            defaultAwait()
                    .atMost(Duration.ofSeconds(20))
                    .untilAsserted(() -> {
                        DesignDeleteRequested event = Json.decodeValue(message.getValue().getData(), DesignDeleteRequested.class);
                        TestAssertions.assertExpectedDesignDeleteRequestedEvent(event, designId, userId);
                    });

            return this;
        }
    }
}
