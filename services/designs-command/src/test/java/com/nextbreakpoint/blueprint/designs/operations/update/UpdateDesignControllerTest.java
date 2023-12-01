package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.commands.avro.DesignUpdateCommand;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.ResultStatus;
import com.nextbreakpoint.blueprint.designs.TestFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rx.Single;

import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class UpdateDesignControllerTest {
    private final MessageEmitter<DesignUpdateCommand> emitter = mock();

    private final UpdateDesignController controller = new UpdateDesignController(MESSAGE_SOURCE, emitter);

    @Test
    void shouldPublishCommand() {
        final var request = aRequest(USER_ID_1, DESIGN_ID_1, COMMAND_ID_1, DATA_1, true);

        final var expectedResponse = aResponse(DESIGN_ID_1, ResultStatus.SUCCESS, null);

        when(emitter.send(any())).thenReturn(Single.just(null));

        final var actualResponse = controller.onNext(request)
                .doOnError(Assertions::fail).toBlocking().value();

        assertThat(actualResponse).isEqualTo(expectedResponse);

        final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignUpdateCommand(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, true));

        verify(emitter).send(hasExpectedValues(expectedOutputMessage));
        verifyNoMoreInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenEmitterFails() {
        final RuntimeException exception = new RuntimeException("Some error");
        final MessageEmitter<DesignUpdateCommand> mockedEmitter = mock();
        when(mockedEmitter.send(any(OutputMessage.class))).thenReturn(Single.error(exception));

        final var request = aRequest(USER_ID_1, DESIGN_ID_1, COMMAND_ID_1, DATA_1, true);

        final var expectedResponse = aResponse(DESIGN_ID_1, ResultStatus.FAILURE, "Some error");

        final var controller = new UpdateDesignController(MESSAGE_SOURCE, mockedEmitter);

        final var actualResponse = controller.onNext(request)
                .doOnError(Assertions::fail).toBlocking().value();

        assertThat(actualResponse).isEqualTo(expectedResponse);

        final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignUpdateCommand(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, true));

        verify(mockedEmitter).send(hasExpectedValues(expectedOutputMessage));
        verifyNoMoreInteractions(mockedEmitter);
    }

    @NotNull
    private static UUID aMessageId() {
        return UUID.randomUUID();
    }

    @Nullable
    private static UpdateDesignRequest aRequest(UUID userId, UUID designId, UUID commandId, String data, boolean published) {
        return UpdateDesignRequest.builder()
                .withOwner(userId)
                .withUuid(designId)
                .withChange(commandId)
                .withJson(data)
                .withPublished(published)
                .build();
    }

    @Nullable
    private static UpdateDesignResponse aResponse(UUID designId, ResultStatus status, String error) {
        return UpdateDesignResponse.builder()
                .withUuid(designId)
                .withStatus(status)
                .withError(error)
                .build();
    }

    @NotNull
    private static DesignUpdateCommand aDesignUpdateCommand(UUID designId, UUID commandId, UUID userId, String data, boolean published) {
        return DesignUpdateCommand.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setUserId(userId)
                .setData(data)
                .setPublished(published)
                .build();
    }

    @Nullable
    private static OutputMessage<DesignUpdateCommand> hasExpectedValues(OutputMessage<DesignUpdateCommand> expectedOutputMessage) {
        return assertArg(message -> {
            assertThat(message.getKey()).isEqualTo(expectedOutputMessage.getKey());
            assertThat(message.getValue().getUuid()).isNotNull();
            assertThat(message.getValue().getType()).isEqualTo(expectedOutputMessage.getValue().getType());
            assertThat(message.getValue().getData()).isEqualTo(expectedOutputMessage.getValue().getData());
            assertThat(message.getValue().getSource()).isEqualTo(expectedOutputMessage.getValue().getSource());
        });
    }
}