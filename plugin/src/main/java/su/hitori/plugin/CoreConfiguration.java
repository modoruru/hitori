package su.hitori.plugin;

import su.hitori.api.configuration.Field;
import su.hitori.api.configuration.SectionScheme;

public final class CoreConfiguration extends SectionScheme {

    public final MessagesUtilPrefixes messagesUtilPrefixes = new MessagesUtilPrefixes();

    public static class MessagesUtilPrefixes extends SectionScheme {
        public Field<String> info = Field.create("<color:#69A2FF>info » </color>");
        public Field<String> error = Field.create("<color:#FF5555>error » </color>");
        public Field<String> warning = Field.create("<color:#FFD45E>warning » </color>");
    }

}
