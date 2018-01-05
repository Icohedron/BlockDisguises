package io.github.icohedron.blockdisguises.cmds;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class UndisguiseAllCmd extends BlockDisguiseCmd {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        disguiseManager.undisguiseAll();
        src.sendMessage(Text.of(blockDisguises.getTextPrefix(), TextColors.YELLOW, "Undisguised all players"));
        return CommandResult.success();
    }

}
