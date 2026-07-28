package su.hitori.api.util;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;

/**
 * Small tool for creating beauty log-messages sent (often) to the players.
 */
public interface Messages {

    Type
            DEF = Type.DEFAULT,
            INFO = Type.INFO,
            ERROR = Type.ERROR,
            WARNING = Type.WARNING;

    @SuppressWarnings("DataFlowIssue")
    @ApiStatus.Internal
    static Component createStatic(Type type, Component component) {
        return Optional.ofNullable(Bukkit.getServer().getServicesManager().getRegistration(Messages.class))
                .map(RegisteredServiceProvider::getProvider)
                .map(messages -> messages.create(type, component))
                .orElse(null);
    }

    Component create(Type type, Component component);

    enum Type {
        DEFAULT,
        INFO,
        ERROR,
        WARNING;

        public Component text(int text) {
            return text(String.valueOf(text));
        }

        public Component text(double text) {
            return text(String.valueOf(text));
        }

        public Component text(float text) {
            return text(String.valueOf(text));
        }

        public Component text(boolean text) {
            return text(String.valueOf(text));
        }

        /**
         * Uses {@link Text#create(String)} to create text component using {@link MiniMessage}
         */
        public Component create(String text) {
            return createStatic(this, Text.create(text));
        }

        public void createAndSendToSender(CommandSourceStack source, String text) {
            source.getSender().sendMessage(create(text));
        }

        public Component text(String text) {
            return createStatic(this, Component.text(text));
        }

        public Component translatable(String key, Component... args) {
            return createStatic(this, Component.translatable(key, args));
        }

    }

}
