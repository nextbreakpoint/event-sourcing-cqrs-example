
package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.test.TestContext;
import com.nextbreakpoint.blueprint.designs.TestActions.Source;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Durations.ONE_SECOND;

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
        return Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(ONE_SECOND);
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

        public Givens theDesignDocumentUpdateCompletedMessage(OutputMessage<DesignDocumentUpdateCompleted> message) {
            final DesignDocumentUpdateCompleted designDocumentUpdateCompleted = message.getValue().getData();
            context.putObject("designId", designDocumentUpdateCompleted.getDesignId());
            context.putObject(designDocumentUpdateCompleted.getDesignId() + ".commandId", designDocumentUpdateCompleted.getCommandId());
            context.putObject(designDocumentUpdateCompleted.getDesignId() + ".revision", designDocumentUpdateCompleted.getRevision());
            context.putObject("message", message);
            return this;
        }

        public Givens theDesignDocumentDeleteCompletedMessage(OutputMessage<DesignDocumentDeleteCompleted> message) {
            final DesignDocumentDeleteCompleted designDocumentDeleteCompleted = message.getValue().getData();
            context.putObject("designId", designDocumentDeleteCompleted.getDesignId());
            context.putObject(designDocumentDeleteCompleted.getDesignId() + ".commandId", designDocumentDeleteCompleted.getCommandId());
            context.putObject(designDocumentDeleteCompleted.getDesignId() + ".revision", designDocumentDeleteCompleted.getRevision());
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

        public Whens appendMessage() {
            var message = (OutputMessage<Object>) context.getObject("message");
            var messages = (List<OutputMessage<Object>>) context.getObject("outputMessages", new ArrayList<>());
            messages.add(message);
            context.putObject("outputMessages", messages);
            return this;
        }

        public Whens discardMessages() {
            var messages = (List<OutputMessage<Object>>) context.getObject("outputMessages", new ArrayList<>());
            messages.clear();
            context.putObject("outputMessages", messages);
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

        public void shouldNotifyWatchersOfAllResources() {
            try {
                List<TestCases.SSENotification> notifications = Collections.synchronizedList(new ArrayList<>());

                var messages = (List<OutputMessage<Object>>) context.getObject("outputMessages");

                actions.subscribe(notifications, () -> messages.forEach(message -> actions.emitMessage(Source.EVENTS, message, Function.identity())));

                defaultAwait()
                        .untilAsserted(() -> {
                            assertThat(notifications).isNotEmpty();
                            List<TestCases.SSENotification> events = new ArrayList<>(notifications);
//                          events.forEach(System.out::println);
                            assertThat(notifications).hasSize(4);
                            assertThat(events.get(0).type).isEqualTo("CONNECT");
                            assertThat(events.get(1).type).isEqualTo("OPEN");
                            assertThat(events.get(2).type).isEqualTo("UPDATE");
                            assertThat(events.get(3).type).isEqualTo("UPDATE");
                            String openData = events.get(1).body.split("\n")[1];
                            Map<String, Object> openObject = Json.decodeValue(openData, HashMap.class);
                            assertThat(openObject.get("revision")).isNotNull();
                            assertThat(openObject.get("session")).isNotNull();
                            String updateData = events.get(2).body.split("\n")[1];
                            Map<String, Object> updateObject = Json.decodeValue(updateData, HashMap.class);
                            assertThat(updateObject.get("revision")).isNotNull();
                            assertThat(updateObject.get("session")).isNotNull();
                            assertThat(updateObject.get("session")).isEqualTo(openObject.get("session"));
                            assertThat(updateObject.get("uuid")).isNotNull();
                            assertThat(updateObject.get("uuid")).isEqualTo("*");
                        });
            } finally {
                actions.unsubscribe();
            }
        }

        public void shouldNotifyWatchersOfAResource(UUID designId) {
            try {
                List<TestCases.SSENotification> notifications = Collections.synchronizedList(new ArrayList<>());

                var messages = (List<OutputMessage<Object>>) context.getObject("outputMessages");

                actions.subscribe(notifications, () -> messages.forEach(message -> actions.emitMessage(Source.EVENTS, message, Function.identity())), designId);

                defaultAwait()
                        .untilAsserted(() -> {
                            assertThat(notifications).isNotEmpty();
                            List<TestCases.SSENotification> events = new ArrayList<>(notifications);
//                          events.forEach(System.out::println);
                            assertThat(notifications).hasSize(3);
                            assertThat(events.get(0).type).isEqualTo("CONNECT");
                            assertThat(events.get(1).type).isEqualTo("OPEN");
                            assertThat(events.get(2).type).isEqualTo("UPDATE");
                            String openData = events.get(1).body.split("\n")[1];
                            Map<String, Object> openObject = Json.decodeValue(openData, HashMap.class);
                            assertThat(openObject.get("revision")).isNotNull();
                            assertThat(openObject.get("session")).isNotNull();
                            String updateData = events.get(2).body.split("\n")[1];
                            Map<String, Object> updateObject = Json.decodeValue(updateData, HashMap.class);
                            assertThat(updateObject.get("revision")).isNotNull();
                            assertThat(updateObject.get("session")).isNotNull();
                            assertThat(updateObject.get("session")).isEqualTo(openObject.get("session"));
                            assertThat(updateObject.get("uuid")).isNotNull();
                            assertThat(updateObject.get("uuid")).isEqualTo(designId.toString());
                        });
            } finally {
                actions.unsubscribe();
            }
        }
    }
}