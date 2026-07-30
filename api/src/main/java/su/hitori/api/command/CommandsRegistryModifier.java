package su.hitori.api.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Tool for registering and unregistering commands.
 * <p>
 * Please note that {@link CommandsRegistryModifier} chooses on its own when to perform reload.
 */
public interface CommandsRegistryModifier {

    /**
     * Removes modifications of passed {@link CommandsModificationInfo}
     */
    void undoBatch(CommandsModificationInfo commandsModificationInfo);

    /**
     * @return CommandModification which can be lately used to undo modifications using {@link #undoBatch(CommandsModificationInfo)}. null if toRegister and toUnregister are empty.
     */
    @Nullable CommandsModificationInfo applyModificationsInBatch(List<LiteralCommandNode<CommandSourceStack>> toRegister, Set<String> toUnregister);

}
