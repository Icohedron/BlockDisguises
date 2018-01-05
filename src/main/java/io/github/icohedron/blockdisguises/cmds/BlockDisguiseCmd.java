package io.github.icohedron.blockdisguises.cmds;

import io.github.icohedron.blockdisguises.BlockDisguises;
import io.github.icohedron.blockdisguises.DisguiseManager;
import org.spongepowered.api.command.spec.CommandExecutor;

public abstract class BlockDisguiseCmd implements CommandExecutor {

    protected BlockDisguises blockDisguises;
    protected DisguiseManager disguiseManager;

    public BlockDisguiseCmd() {
        blockDisguises = BlockDisguises.getInstance();
        disguiseManager = blockDisguises.getDisguiseManager();
    }

}
