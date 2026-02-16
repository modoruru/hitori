package su.hitori.api.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class Messages {

    public static final Type
            DEF = Type.DEFAULT,
            INFO = Type.INFO,
            ERROR = Type.ERROR,
            WARNING = Type.WARNING;

    private Messages() {}

    public static Component createFromType(Type type, Component component) {
        // such solution will fix color from type applied to other text
        return Component.empty()
                .colorIfAbsent(TextColor.color(0xFFFFFF))
                .append(type.build())
                .append(component);
    }

    public enum Type {
        DEFAULT(),
        INFO("info", 0x69A2FF),
        ERROR("error", 0xFF5555),
        WARNING("warning", 0xFFD45E);

        private final String path = name().toLowerCase();
        private final String display;
        private final int color;

        Type() {
            this.display = null;
            this.color = 0xFFFFFF;
        }

        Type(String display, int color) {
            this.display = display;
            this.color = color;
        }

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
         * Uses {@link Text#create(String)} to create text using {@link MiniMessage}
         */
        public Component create(String text) {
            return createFromType(this, Text.create(text));
        }

        public Component text(String text) {
            return createFromType(this, Component.text(text));
        }

        public Component translatable(String key, Component... args) {
            return createFromType(this, Component.translatable(key, args));
        }

        private Component build() {
            if(this == DEFAULT) return Component.empty();
            return Component.translatable("system.prefix." + this.path).fallback(display)
                    .append(Component.text(" » "))
                    .color(TextColor.color(this.color));
        }

    }

}
