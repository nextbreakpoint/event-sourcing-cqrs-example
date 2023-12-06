package com.nextbreakpoint.blueprint.designs.operations.insert;

import com.nextbreakpoint.blueprint.common.commands.avro.DesignInsertCommand;
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

class InsertDesignControllerTest {
    private final MessageEmitter<DesignInsertCommand> emitter = mock();

    private final InsertDesignController controller = new InsertDesignController(MESSAGE_SOURCE, emitter);

    @Test
    void shouldPublishCommand() {
        final var request = aRequest(USER_ID_1, DESIGN_ID_1, COMMAND_ID_1, DATA_1);

        final var expectedResponse = aResponse(DESIGN_ID_1, ResultStatus.SUCCESS, null);

        when(emitter.send(any())).thenReturn(Single.just(null));

        final var actualResponse = controller.onNext(request)
                .doOnError(Assertions::fail).toBlocking().value();

        assertThat(actualResponse).isEqualTo(expectedResponse);

        final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignInsertCommand(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1));

        verify(emitter).send(hasExpectedValues(expectedOutputMessage));
        verifyNoMoreInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenEmitterFails() {
        final var exception = new RuntimeException("Some error");
        final MessageEmitter<DesignInsertCommand> mockedEmitter = mock();
        when(mockedEmitter.send(any(OutputMessage.class))).thenReturn(Single.error(exception));

        final var request = aRequest(USER_ID_1, DESIGN_ID_1, COMMAND_ID_1, DATA_1);

        final var expectedResponse = aResponse(DESIGN_ID_1, ResultStatus.FAILURE, "Some error");

        final var controller = new InsertDesignController(MESSAGE_SOURCE, mockedEmitter);

        final var actualResponse = controller.onNext(request)
                .doOnError(Assertions::fail).toBlocking().value();

        assertThat(actualResponse).isEqualTo(expectedResponse);

        final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignInsertCommand(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1));

        verify(mockedEmitter).send(hasExpectedValues(expectedOutputMessage));
        verifyNoMoreInteractions(mockedEmitter);
    }

    @NotNull
    private static UUID aMessageId() {
        return UUID.randomUUID();
    }

    @Nullable
    private static InsertDesignRequest aRequest(UUID userId, UUID designId, UUID commandId, String data) {
        return InsertDesignRequest.builder()
                .withOwner(userId)
                .withUuid(designId)
                .withChange(commandId)
                .withJson(data)
                .build();
    }

    @Nullable
    private static InsertDesignResponse aResponse(UUID designId, ResultStatus status, String error) {
        return InsertDesignResponse.builder()
                .withUuid(designId)
                .withStatus(status)
                .withError(error)
                .build();
    }

    @NotNull
    private static DesignInsertCommand aDesignInsertCommand(UUID designId, UUID commandId, UUID userId, String data) {
        return DesignInsertCommand.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setUserId(userId)
                .setData(data)
                .build();
    }

    @Nullable
    private static OutputMessage<DesignInsertCommand> hasExpectedValues(OutputMessage<DesignInsertCommand> expectedOutputMessage) {
        return assertArg(message -> {
            assertThat(message.getKey()).isEqualTo(expectedOutputMessage.getKey());
            assertThat(message.getValue().getUuid()).isNotNull();
            assertThat(message.getValue().getType()).isEqualTo(expectedOutputMessage.getValue().getType());
            assertThat(message.getValue().getData()).isEqualTo(expectedOutputMessage.getValue().getData());
            assertThat(message.getValue().getSource()).isEqualTo(expectedOutputMessage.getValue().getSource());
        });
    }
}