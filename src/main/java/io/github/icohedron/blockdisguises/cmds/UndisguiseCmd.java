package io.github.icohedron.blockdisguises.cmds;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.UUID;

public class UndisguiseCmd extends BlockDisguiseCmd {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Player player = args.<Player>getOne("player").get();
        UUID uuid = player.getUniqueId();

        if (!disguiseManager.isDisguised(uuid)) {
            if (src == player) {
                src.sendMessage(Text.of(blockDisguises.getTextPrefix(), TextColors.RED, "You are not currently disguised"));
            } else {
                src.sendMessage(Text.of(blockDisguises.getTextPrefix(), TextColors.RED, "'" + player.getName() + "' is not currently disguised"));
            }
            return CommandResult.empty();
        }

        disguiseManager.undisguise(uuid);

        String start = src == player ? "You are" : player.getName() + " is";
        src.sendMessage(Text.of(blockDisguises.getTextPrefix(), TextColors.YELLOW, start + " no longer disguised as a block"));

        return CommandResult.success();
    }

}
