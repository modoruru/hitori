package su.hitori.plugin.command;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.key.Key;
import su.hitori.api.util.UnsafeUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public abstract class CommandRegistryModifier {

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

    public abstract void applyModificationsInBatch(List<LiteralCommandNode<CommandSourceStack>> toRegister, Set<String> toUnregister, boolean performReload);

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

    public static <S> void removeBrigadierCommands(RootCommandNode<S> root, String commandName, boolean unregisterNamespaces) {
        iterateOverMaps(root, map -> map.remove(commandName));

        if(!unregisterNamespaces) return;
        iterateOverMaps(root, map -> {
            for (String command : Set.copyOf(map.keySet())) {
                if(command.indexOf(':') == -1) continue;

                Key key = Key.key(command);
                if(key.value().equalsIgnoreCase(commandName)) map.remove(command);
            }
        });
    }

}
