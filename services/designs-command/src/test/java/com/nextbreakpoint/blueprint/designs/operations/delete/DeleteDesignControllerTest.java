package com.nextbreakpoint.blueprint.designs.operations.delete;

import com.nextbreakpoint.blueprint.common.commands.avro.DesignDeleteCommand;
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

class DeleteDesignControllerTest {
    private final MessageEmitter<DesignDeleteCommand> emitter = mock();

    private final DeleteDesignController controller = new DeleteDesignController(MESSAGE_SOURCE, emitter);

    @Test
    void shouldPublishCommand() {
        final var request = aRequest(USER_ID_1, DESIGN_ID_1, COMMAND_ID_1);

        final var expectedResponse = aResponse(DESIGN_ID_1, ResultStatus.SUCCESS, null);

        when(emitter.send(any())).thenReturn(Single.just(null));

        final var actualResponse = controller.onNext(request)
                .doOnError(Assertions::fail).toBlocking().value();

        assertThat(actualResponse).isEqualTo(expectedResponse);

        final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignDeleteCommand(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1));

        verify(emitter).send(hasExpectedValues(expectedOutputMessage));
        verifyNoMoreInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenEmitterFails() {
        final var exception = new RuntimeException("Some error");
        when(emitter.send(any(OutputMessage.class))).thenReturn(Single.error(exception));

        final var request = aRequest(USER_ID_1, DESIGN_ID_1, COMMAND_ID_1);

        final var expectedResponse = aResponse(DESIGN_ID_1, ResultStatus.FAILURE, "Some error");

        final var controller = new DeleteDesignController(MESSAGE_SOURCE, emitter);

        final var actualResponse = controller.onNext(request)
                .doOnError(Assertions::fail).toBlocking().value();

        assertThat(actualResponse).isEqualTo(expectedResponse);

        final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignDeleteCommand(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1));

        verify(emitter).send(hasExpectedValues(expectedOutputMessage));
        verifyNoMoreInteractions(emitter);
    }

    @NotNull
    private static UUID aMessageId() {
        return UUID.randomUUID();
    }

    @Nullable
    private static DeleteDesignRequest aRequest(UUID userId, UUID designId, UUID commandId) {
        return DeleteDesignRequest.builder()
                .withOwner(userId)
                .withUuid(designId)
                .withChange(commandId)
                .build();
    }

    @Nullable
    private static DeleteDesignResponse aResponse(UUID designId, ResultStatus status, String error) {
        return DeleteDesignResponse.builder()
                .withUuid(designId)
                .withStatus(status)
                .withError(error)
                .build();
    }

    @NotNull
    private static DesignDeleteCommand aDesignDeleteCommand(UUID designId, UUID commandId, UUID userId) {
        return DesignDeleteCommand.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setUserId(userId)
                .build();
    }

    @Nullable
    private static OutputMessage<DesignDeleteCommand> hasExpectedValues(OutputMessage<DesignDeleteCommand> expectedOutputMessage) {
        return assertArg(message -> {
            assertThat(message.getKey()).isEqualTo(expectedOutputMessage.getKey());
            assertThat(message.getValue().getUuid()).isNotNull();
            assertThat(message.getValue().getType()).isEqualTo(expectedOutputMessage.getValue().getType());
            assertThat(message.getValue().getData()).isEqualTo(expectedOutputMessage.getValue().getData());
            assertThat(message.getValue().getSource()).isEqualTo(expectedOutputMessage.getValue().getSource());
        });
    }
}