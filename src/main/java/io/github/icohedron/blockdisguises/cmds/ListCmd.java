package io.github.icohedron.blockdisguises.cmds;

import io.github.icohedron.blockdisguises.Disguise;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Stack;
import java.util.UUID;

public class ListCmd extends BlockDisguiseCmd {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Stack<Text> contents = new Stack<>();
        for (UUID disguised : disguiseManager.getAllDisguised()) {
            Disguise disguise = disguiseManager.getDisguise(disguised);
            contents.push(Text.of(disguise.getOwnerName().get(), TextColors.GRAY, " -> Disguised as ", TextColors.YELLOW, disguise.getBlockState().getName()));
        }
        PaginationList.builder()
                .title(Text.of(TextColors.DARK_GREEN, "Disguised players"))
                .contents(contents)
                .padding(Text.of(TextColors.DARK_GREEN, "="))
                .build().sendTo(src);
        return CommandResult.success();
    }
}
