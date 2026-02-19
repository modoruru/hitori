package su.hitori.plugin;

import su.hitori.api.config.Configuration;

import java.nio.file.Path;

public final class HitoriConfiguration extends Configuration {

    public HitoriConfiguration(Path path) {
        super(path);
    }

    public MessagesUtilPrefixes messagesUtilPrefixes = new MessagesUtilPrefixes();

    public static final class MessagesUtilPrefixes {
        public String info = "<color:#69A2FF>info » </color>";
        public String error = "<color:#FF5555>error » </color>";
        public String warning = "<color:#FFD45E>warning » </color>";
    }

}
