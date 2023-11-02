package com.nextbreakpoint.blueprint.designs.aggregate;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.TestUtils;
import com.nextbreakpoint.blueprint.designs.model.Design;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rx.Single;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DesignEventStoreTest {
    private static final String REVISION_NULL = "0000000000000000-0000000000000000";

    private final Store store = mock(Store.class);
    private final DesignMergeStrategy strategy = mock(DesignMergeStrategy.class);
    private final DesignEventStore eventStore = new DesignEventStore(store, strategy);

    @Test
    void shouldAppendMessage() {
        final Map<Object, Object> event = new HashMap<>();
        final String messageKey = "key";
        final String messageType = "something";
        final String messageToken = "0001";
        final LocalDateTime messageTime = LocalDateTime.now(ZoneId.of("UTC"));
        final InputMessage message = TestUtils.createInputMessage(messageKey, messageType, UUID.randomUUID(), event, messageToken, messageTime);

        when(store.appendMessage(message)).thenReturn(Single.just(null));

        eventStore.appendMessage(message)
                .subscribe((ignored) -> {}, Assertions::fail);

        verify(store).appendMessage(message);
        verifyNoMoreInteractions(store);
    }

    @Test
    void shouldReturnErrorWhenCannotAppendMessage() {
        final Map<Object, Object> event = new HashMap<>();
        final String messageKey = "key";
        final String messageType = "something";
        final String messageToken = "0001";
        final LocalDateTime messageTime = LocalDateTime.now(ZoneId.of("UTC"));
        final InputMessage message = TestUtils.createInputMessage(messageKey, messageType, UUID.randomUUID(), event, messageToken, messageTime);

        final RuntimeException exception = new RuntimeException();
        when(store.appendMessage(message)).thenReturn(Single.error(exception));

        eventStore.appendMessage(message)
                .subscribe(cause -> Assertions.fail(), err -> assertThat(err).isEqualTo(exception));

        verify(store).appendMessage(message);
        verifyNoMoreInteractions(store);
    }

    @Test
    void shouldFindDesign() {
        final Design someDesign = Design.builder()
                .withDesignId(UUID.randomUUID())
                .build();

        final Design expectedDesign = Design.builder()
                .withDesignId(someDesign.getDesignId())
                .build();

        when(store.findDesign(someDesign.getDesignId())).thenReturn(Single.just(Optional.of(someDesign)));

        eventStore.findDesign(someDesign.getDesignId())
                .subscribe(design -> assertThat(design).isPresent().get().isEqualTo(expectedDesign), Assertions::fail);

        verify(store).findDesign(someDesign.getDesignId());
        verifyNoMoreInteractions(store);
    }

    @Test
    void shouldReturnErrorWhenCannotFindDesign() {
        final Design someDesign = Design.builder()
                .withDesignId(UUID.randomUUID())
                .build();

        final RuntimeException exception = new RuntimeException();
        when(store.findDesign(someDesign.getDesignId())).thenReturn(Single.error(exception));

        eventStore.findDesign(someDesign.getDesignId())
                .subscribe(ignored -> Assertions.fail(), err -> assertThat(err).isEqualTo(exception));

        verify(store).findDesign(someDesign.getDesignId());
        verifyNoMoreInteractions(store);
    }

    @Test
    void shouldUpdateDesign() {
        final Design someDesign = Design.builder()
                .withDesignId(UUID.randomUUID())
                .build();

        final Design expectedDesign = Design.builder()
                .withDesignId(someDesign.getDesignId())
                .build();

        when(store.updateDesign(someDesign)).thenReturn(Single.just(null));

        eventStore.updateDesign(someDesign)
                .subscribe(design -> assertThat(design).isPresent().get().isEqualTo(expectedDesign), Assertions::fail);

        verify(store).updateDesign(someDesign);
        verifyNoMoreInteractions(store);
    }

    @Test
    void shouldReturnErrorWhenCannotUpdateDesign() {
        final Design someDesign = Design.builder()
                .withDesignId(UUID.randomUUID())
                .build();

        final RuntimeException exception = new RuntimeException();
        when(store.updateDesign(someDesign)).thenReturn(Single.error(exception));

        eventStore.updateDesign(someDesign)
                .subscribe(ignored -> Assertions.fail(), err -> assertThat(err).isEqualTo(exception));

        verify(store).updateDesign(someDesign);
        verifyNoMoreInteractions(store);
    }

    @Test
    void shouldProjectNewDesign() {
        final Design updatedDesign = Design.builder()
                .withDesignId(UUID.randomUUID())
                .withRevision("0003")
                .build();

        final Design expectedDesign = Design.builder()
                .withDesignId(updatedDesign.getDesignId())
                .withRevision("0003")
                .build();

        final List<InputMessage> messages = List.of();

        when(store.findDesign(updatedDesign.getDesignId())).thenReturn(Single.just(Optional.empty()));
        when(store.findMessages(updatedDesign.getDesignId(), REVISION_NULL, "0003")).thenReturn(Single.just(messages));
        when(strategy.applyEvents(null, messages)).thenReturn(Optional.of(updatedDesign));

        eventStore.projectDesign(updatedDesign.getDesignId(), "0003")
                .subscribe(design -> assertThat(design).isPresent().get().isEqualTo(expectedDesign), Assertions::fail);

        verify(store).findDesign(updatedDesign.getDesignId());
        verify(store).findMessages(updatedDesign.getDesignId(), REVISION_NULL, "0003");
        verify(strategy).applyEvents(null, messages);
        verifyNoMoreInteractions(store, strategy);
    }

    @Test
    void shouldProjectExistingDesign() {
        final Design someDesign = Design.builder()
                .withDesignId(UUID.randomUUID())
                .withRevision("0001")
                .build();

        final Design updatedDesign = Design.builder()
                .withDesignId(someDesign.getDesignId())
                .withRevision("0002")
                .build();

        final Design expectedDesign = Design.builder()
                .withDesignId(someDesign.getDesignId())
                .withRevision("0002")
                .build();

        final List<InputMessage> messages = List.of();

        when(store.findDesign(someDesign.getDesignId())).thenReturn(Single.just(Optional.of(someDesign)));
        when(store.findMessages(someDesign.getDesignId(), someDesign.getRevision(), "0002")).thenReturn(Single.just(messages));
        when(strategy.applyEvents(someDesign, messages)).thenReturn(Optional.of(updatedDesign));

        eventStore.projectDesign(someDesign.getDesignId(), "0002")
                .subscribe(design -> assertThat(design).isPresent().get().isEqualTo(expectedDesign), Assertions::fail);

        verify(store).findDesign(someDesign.getDesignId());
        verify(store).findMessages(someDesign.getDesignId(), someDesign.getRevision(), "0002");
        verify(strategy).applyEvents(someDesign, messages);
        verifyNoMoreInteractions(store, strategy);
    }

    @Test
    void shouldReturnErrorWhenCannotProjectDesignBecauseFindDesignFails() {
        final Design someDesign = Design.builder()
                .withDesignId(UUID.randomUUID())
                .withRevision("0001")
                .build();

        final RuntimeException exception = new RuntimeException();
        when(store.findDesign(someDesign.getDesignId())).thenReturn(Single.error(exception));

        eventStore.projectDesign(someDesign.getDesignId(), "0002")
                .subscribe(ignored -> Assertions.fail(), err -> assertThat(err).isEqualTo(exception));

        verify(store).findDesign(someDesign.getDesignId());
        verifyNoMoreInteractions(store);
    }

    @Test
    void shouldReturnErrorWhenCannotProjectDesignBecauseFindMessagesFails() {
        final Design someDesign = Design.builder()
                .withDesignId(UUID.randomUUID())
                .withRevision("0001")
                .build();

        final RuntimeException exception = new RuntimeException();
        when(store.findDesign(someDesign.getDesignId())).thenReturn(Single.just(Optional.of(someDesign)));
        when(store.findMessages(someDesign.getDesignId(), someDesign.getRevision(), "0002")).thenReturn(Single.error(exception));

        eventStore.projectDesign(someDesign.getDesignId(), "0002")
                .subscribe(ignored -> Assertions.fail(), err -> assertThat(err).isEqualTo(exception));

        verify(store).findDesign(someDesign.getDesignId());
        verify(store).findMessages(someDesign.getDesignId(), someDesign.getRevision(), "0002");
        verifyNoMoreInteractions(store, strategy);
    }

    @Test
    void shouldReturnErrorWhenCannotProjectDesignBecauseApplyEventsFails() {
        final Design someDesign = Design.builder()
                .withDesignId(UUID.randomUUID())
                .withRevision("0001")
                .build();

        final List<InputMessage> messages = List.of();

        final IllegalStateException exception = new IllegalStateException();
        when(store.findDesign(someDesign.getDesignId())).thenReturn(Single.just(Optional.of(someDesign)));
        when(store.findMessages(someDesign.getDesignId(), someDesign.getRevision(), "0002")).thenReturn(Single.just(messages));
        when(strategy.applyEvents(someDesign, messages)).thenThrow(exception);

        eventStore.projectDesign(someDesign.getDesignId(), "0002")
                .subscribe(ignored -> Assertions.fail(), err -> assertThat(err).isEqualTo(exception));

        verify(store).findDesign(someDesign.getDesignId());
        verify(store).findMessages(someDesign.getDesignId(), someDesign.getRevision(), "0002");
        verify(strategy).applyEvents(someDesign, messages);
        verifyNoMoreInteractions(store, strategy);
    }
}