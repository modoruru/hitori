package su.hitori.api.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.CommandAPIArgumentType;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.executors.CommandArguments;

import java.net.URI;
import java.net.URL;

public final class URLArgument extends Argument<URL> {

    private final GreedyStringArgument base;

    public URLArgument(String nodeName) {
        super(nodeName, StringArgumentType.string());
        this.base = new GreedyStringArgument(nodeName);
    }

    @Override
    public Class<URL> getPrimitiveType() {
        return null;
    }

    @Override
    public CommandAPIArgumentType getArgumentType() {
        return CommandAPIArgumentType.CUSTOM;
    }

    @Override
    public <Source> URL parseArgument(CommandContext<Source> cmdCtx, String key, CommandArguments previousArgs) throws CommandSyntaxException {
        String text = base.parseArgument(cmdCtx, key, previousArgs);
        try {
            return URI.create(text).toURL();
        }
        catch (Throwable ex) {
            throw new SimpleCommandExceptionType(() -> "Malformed URL!").create();
        }
    }
}
