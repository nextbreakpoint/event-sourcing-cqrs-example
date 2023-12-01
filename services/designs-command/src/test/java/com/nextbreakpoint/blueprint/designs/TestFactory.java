package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.commands.avro.DesignDeleteCommand;
import com.nextbreakpoint.blueprint.common.commands.avro.DesignInsertCommand;
import com.nextbreakpoint.blueprint.common.commands.avro.DesignUpdateCommand;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignUpdateRequested;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DELETE_COMMAND;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DELETE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_INSERT_COMMAND;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_INSERT_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_UPDATE_COMMAND;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_UPDATE_REQUESTED;

public class TestFactory {
    private TestFactory() {}

    @NotNull
    public static InputMessage<DesignInsertRequested> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, DesignInsertRequested designInsertRequested) {
        return TestUtils.createInputMessage(
                designInsertRequested.getDesignId().toString(), DESIGN_INSERT_REQUESTED, messageId, designInsertRequested, messageToken, messageTime
        );
    }

    @NotNull
    public static InputMessage<DesignUpdateRequested> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, DesignUpdateRequested designUpdateRequested) {
        return TestUtils.createInputMessage(
                designUpdateRequested.getDesignId().toString(), DESIGN_UPDATE_REQUESTED, messageId, designUpdateRequested, messageToken, messageTime
        );
    }

    @NotNull
    public static InputMessage<DesignDeleteRequested> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, DesignDeleteRequested designDeleteRequested) {
        return TestUtils.createInputMessage(
                designDeleteRequested.getDesignId().toString(), DESIGN_DELETE_REQUESTED, messageId, designDeleteRequested, messageToken, messageTime
        );
    }

    @NotNull
    public static InputMessage<DesignInsertCommand> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, DesignInsertCommand designInsertCommand) {
        return TestUtils.createInputMessage(
                designInsertCommand.getDesignId().toString(), DESIGN_INSERT_COMMAND, messageId, designInsertCommand, messageToken, messageTime
        );
    }

    @NotNull
    public static InputMessage<DesignUpdateCommand> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, DesignUpdateCommand designUpdateCommand) {
        return TestUtils.createInputMessage(
                designUpdateCommand.getDesignId().toString(), DESIGN_UPDATE_COMMAND, messageId, designUpdateCommand, messageToken, messageTime
        );
    }

    @NotNull
    public static InputMessage<DesignDeleteCommand> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, DesignDeleteCommand designDeleteCommand) {
        return TestUtils.createInputMessage(
                designDeleteCommand.getDesignId().toString(), DESIGN_DELETE_COMMAND, messageId, designDeleteCommand, messageToken, messageTime
        );
    }

    @NotNull
    public static OutputMessage<DesignInsertRequested> createOutputMessage(UUID messageId, DesignInsertRequested designInsertRequested) {
        return TestUtils.createOutputMessage(
                designInsertRequested.getDesignId().toString(), DESIGN_INSERT_REQUESTED, messageId, designInsertRequested
        );
    }

    @NotNull
    public static OutputMessage<DesignUpdateRequested> createOutputMessage(UUID messageId, DesignUpdateRequested designUpdateRequested) {
        return TestUtils.createOutputMessage(
                designUpdateRequested.getDesignId().toString(), DESIGN_UPDATE_REQUESTED, messageId, designUpdateRequested
        );
    }

    @NotNull
    public static OutputMessage<DesignDeleteRequested> createOutputMessage(UUID messageId, DesignDeleteRequested designDeleteRequested) {
        return TestUtils.createOutputMessage(
                designDeleteRequested.getDesignId().toString(), DESIGN_DELETE_REQUESTED, messageId, designDeleteRequested
        );
    }

    @NotNull
    public static OutputMessage<DesignInsertCommand> createOutputMessage(UUID messageId, DesignInsertCommand designInsertCommand) {
        return TestUtils.createOutputMessage(
                designInsertCommand.getDesignId().toString(), DESIGN_INSERT_COMMAND, messageId, designInsertCommand
        );
    }

    @NotNull
    public static OutputMessage<DesignUpdateCommand> createOutputMessage(UUID messageId, DesignUpdateCommand designUpdateCommand) {
        return TestUtils.createOutputMessage(
                designUpdateCommand.getDesignId().toString(), DESIGN_UPDATE_COMMAND, messageId, designUpdateCommand
        );
    }

    @NotNull
    public static OutputMessage<DesignDeleteCommand> createOutputMessage(UUID messageId, DesignDeleteCommand designDeleteCommand) {
        return TestUtils.createOutputMessage(
                designDeleteCommand.getDesignId().toString(), DESIGN_DELETE_COMMAND, messageId, designDeleteCommand
        );
    }
}
