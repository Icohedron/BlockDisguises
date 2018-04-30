package io.github.icohedron.blockdisguises.cmds;

import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.trait.BlockTrait;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Optional;
import java.util.UUID;

public class DisguiseCmd extends BlockDisguiseCmd {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Player player = args.<Player>getOne("player").get();
        UUID uuid = player.getUniqueId();

        if (disguiseManager.isDisguised(uuid)) {
            disguiseManager.undisguise(uuid);
        }

        BlockType blockType = args.<BlockType>getOne("blocktype").get();
        BlockState blockState = blockType.getDefaultState();

        String[] traits = new String[] {"variant", "facing", "color", "half", "type", "wet", "powered", "delay", "shape", "conditional", "axis"};
        for (String flag : traits) {

            Optional<String> flagString = args.getOne(flag);
            if (flagString.isPresent()) { // If the flag was specified for this trait (with an argument)

                Optional<BlockTrait<?>> flagTrait = blockState.getTrait(flag); // Retrieve the block trait corresponding to the flag
                if (flagTrait.isPresent()) { // If the block has this trait

                    Optional<BlockState> blockStateWithTrait = blockState.withTrait(flagTrait.get(), flagString.get()); // Try applying the trait to the block state
                    if (blockStateWithTrait.isPresent()) { // If successful
                        blockState = blockStateWithTrait.get(); // Set it as the new block trait
                    }

                }

            }
        }

        disguiseManager.disguise(player, blockState);

        if (src != player) {
            src.sendMessage(Text.of(blockDisguises.getTextPrefix(), TextColors.YELLOW, player.getName(), " is now disguised as " + blockState.getType().getName().substring(10)));
        }
        // String startClause = src == player ? "You are" : player.getName() + " is";
        // src.sendMessage(Text.of(blockDisguises.getTextPrefix(), TextColors.YELLOW, startClause + " now disguised as " + blockState.getType().getName().substring(10)));

        return CommandResult.success();
    }
}
