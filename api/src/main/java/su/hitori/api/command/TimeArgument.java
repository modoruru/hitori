package su.hitori.api.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.CommandAPIArgumentType;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.hitori.api.util.language.Language;
import su.hitori.api.util.time.LocalizedTimeUtil;

/**
 * Unlike date argument, it parses date in such format: "30d 20m 42s"
 */
public final class TimeArgument extends Argument<Long> {

    private final TextArgument base;
    private final Language language;

    public TimeArgument(String nodeName, Language language) {
        super(nodeName, StringArgumentType.string());
        this.base = new TextArgument(nodeName);
        this.language = language;
    }

    @Override
    public Class<Long> getPrimitiveType() {
        return null;
    }

    @Override
    public CommandAPIArgumentType getArgumentType() {
        return CommandAPIArgumentType.CUSTOM;
    }

    @Override
    public <Source> Long parseArgument(CommandContext<Source> commandContext, String s, CommandArguments commandArguments) throws CommandSyntaxException {
        return LocalizedTimeUtil.parse(base.parseArgument(commandContext, s, commandArguments), language);
    }

}
