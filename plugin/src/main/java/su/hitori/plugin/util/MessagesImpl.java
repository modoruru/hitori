package su.hitori.plugin.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import su.hitori.api.util.Messages;
import su.hitori.api.util.Text;
import su.hitori.plugin.HitoriConfiguration;

public final class MessagesImpl implements Messages {

    private final HitoriConfiguration configuration;

    public MessagesImpl(HitoriConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Component create(Type type, Component component) {
        if(type == Type.DEFAULT) return component;

        var cfg = configuration.messagesUtilPrefixes;
        return Component.empty()
                .colorIfAbsent(TextColor.color(0xFFFFFF))
                .append(Text.create(switch (type) {
                    case INFO -> cfg.info;
                    case ERROR -> cfg.error;
                    case WARNING -> cfg.warning;
                    default -> throw new IllegalStateException("Unexpected value: " + type);
                }))
                .append(component);
    }

}
