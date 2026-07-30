package su.hitori.plugin.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import su.hitori.api.command.CommandsModificationInfo;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CommandsModificationInfoImpl implements CommandsModificationInfo {

    private final UUID uuid;
    final List<LiteralCommandNode<CommandSourceStack>> toRegister;
    final Set<String> toUnregister;
    boolean active;

    public CommandsModificationInfoImpl(List<LiteralCommandNode<CommandSourceStack>> toRegister, Set<String> toUnregister) {
        this.uuid = UUID.randomUUID();
        this.toRegister = toRegister;
        this.toUnregister = toUnregister;
        this.active = true;
    }

    @Override
    public UUID uuid() {
        return uuid;
    }

    @Override
    public boolean active() {
        return active;
    }

}
