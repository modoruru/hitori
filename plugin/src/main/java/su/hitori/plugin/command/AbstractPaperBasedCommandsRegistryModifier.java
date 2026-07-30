package su.hitori.plugin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.key.Key;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;
import su.hitori.api.command.CommandsModificationInfo;
import su.hitori.api.command.CommandsRegistryModifier;
import su.hitori.api.util.UnsafeUtil;
import su.hitori.plugin.CorePlugin;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class AbstractPaperBasedCommandsRegistryModifier implements CommandsRegistryModifier {

    private static final List<Field> mapsFields;

    static {
        try {
            @SuppressWarnings("rawtypes")
            Class<CommandNode> clazz = CommandNode.class;

            mapsFields = new ArrayList<>();
            for (String child : List.of("children", "arguments", "literals")) {
                Field field = clazz.getDeclaredField(child);
                field.setAccessible(true);
                mapsFields.add(field);
            }
        }
        catch (Throwable throwable) {
            throw new IllegalStateException(throwable);
        }
    }

    private final Map<UUID, CommandsModificationInfoImpl> commandsModificationInfo;

    protected final CommandDispatcher<CommandSourceStack> dispatcher;
    private final Runnable reloadRunnable;

    private boolean reloadScheduled;

    public AbstractPaperBasedCommandsRegistryModifier(CorePlugin corePlugin) {
        this.commandsModificationInfo = new HashMap<>();

        this.dispatcher = new CommandDispatcher<>();
        this.reloadRunnable = () -> {
            if(!reloadScheduled) return;

            reload();
            if(reloadScheduled) reloadScheduled = false;
        };

        corePlugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, this::event);
    }

    public Runnable scheduleReload() {
        if(reloadScheduled) return reloadRunnable;
        reloadScheduled = true;
        return reloadRunnable;
    }

    private static <S> Map<String, CommandNode<S>> accessField(Field field, CommandNode<S> node) {
        try {
            Map<String, CommandNode<S>> result = UnsafeUtil.cast(field.get(node));
            assert result != null;
            return result;
        }
        catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private static <S> void iterateOverMaps(CommandNode<S> node, Consumer<Map<String, CommandNode<S>>> consumer) {
        for (Field field : mapsFields) {
            consumer.accept(accessField(field, node));
        }
    }

    private static <S> void removeBrigadierCommands(RootCommandNode<S> root, String commandName) {
        iterateOverMaps(root, map -> map.remove(commandName));

        iterateOverMaps(root, map -> {
            for (String command : Set.copyOf(map.keySet())) {
                if(command.indexOf(':') == -1) continue;

                Key key = Key.key(command);
                if(key.value().equalsIgnoreCase(commandName)) map.remove(command);
            }
        });
    }

    private void event(ReloadableRegistrarEvent<Commands> event) {
        CommandDispatcher<net.minecraft.commands.CommandSourceStack> vanillaDispatcher = MinecraftServer.getServer().getCommands().getDispatcher();
        for (CommandsModificationInfoImpl value : commandsModificationInfo.values()) {
            for (String literal : value.toUnregister) {
                removeBrigadierCommands(vanillaDispatcher.getRoot(), literal);
            }
        }

        for (CommandNode<CommandSourceStack> child : dispatcher.getRoot().getChildren()) {
            event.registrar().register((LiteralCommandNode<CommandSourceStack>) child);
        }
    }

    @Override
    public void undoBatch(CommandsModificationInfo commandsModificationInfo) {
        CommandsModificationInfoImpl commandsModificationInfoImpl = this.commandsModificationInfo.remove(commandsModificationInfo.uuid());
        if(commandsModificationInfoImpl == null || !commandsModificationInfoImpl.active()) throw new IllegalArgumentException("CommandsModificationInfo was not created here or is not active.");

        for (LiteralCommandNode<CommandSourceStack> node : commandsModificationInfoImpl.toRegister) {
            removeBrigadierCommands(dispatcher.getRoot(), node.getLiteral());
        }

        if(!reloadScheduled) reload();
    }

    @Override
    public @Nullable CommandsModificationInfo applyModificationsInBatch(List<LiteralCommandNode<CommandSourceStack>> toRegister, Set<String> toUnregister) {
        if(toRegister.isEmpty() && toUnregister.isEmpty()) return null;

        CommandsModificationInfoImpl commandsModificationInfo = new CommandsModificationInfoImpl(toRegister, toUnregister);
        this.commandsModificationInfo.put(commandsModificationInfo.uuid(), commandsModificationInfo);

        for (LiteralCommandNode<CommandSourceStack> node : toRegister) {
            dispatcher.getRoot().addChild(node);
        }

        for (String commandToUnregister : toUnregister) {
            removeBrigadierCommands(dispatcher.getRoot(), commandToUnregister);
        }

        if(!reloadScheduled) reload();

        return commandsModificationInfo;
    }

    public abstract void reload();

}
