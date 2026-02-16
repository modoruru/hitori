package su.hitori.api.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.CommandAPIArgumentType;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.executors.CommandArguments;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public final class DateArgument extends Argument<ZonedDateTime> {

    private final TextArgument base;
    private final DateTimeFormatter formatter;
    private final ZoneId timeZone;
    private final boolean shouldBeInFuture;

    public DateArgument(String nodeName, DateTimeFormatter formatter, ZoneId timeZone, boolean shouldBeInFuture) {
        super(nodeName, StringArgumentType.string());
        this.base = new TextArgument(nodeName);
        this.formatter = formatter;
        this.timeZone = timeZone;
        this.shouldBeInFuture = shouldBeInFuture;
    }

    @Override
    public Class<ZonedDateTime> getPrimitiveType() {
        return null;
    }

    @Override
    public CommandAPIArgumentType getArgumentType() {
        return CommandAPIArgumentType.CUSTOM;
    }

    @Override
    public <Source> ZonedDateTime parseArgument(CommandContext<Source> cmdCtx, String key, CommandArguments previousArgs) throws CommandSyntaxException {
        String text = base.parseArgument(cmdCtx, key, previousArgs);
        try {
            ZonedDateTime zonedDateTime = LocalDateTime.parse(text, formatter).atZone(timeZone);
            if(shouldBeInFuture && zonedDateTime.toInstant().isBefore(Instant.now()))
                throw new SimpleCommandExceptionType(() -> "Date should be in future!").create();

            return zonedDateTime;
        }
        catch (Exception e) {
            if(e instanceof CommandSyntaxException) throw e;

            String string = "Error parsing date, format is: %s".formatted(formatter.toString());
            throw new SimpleCommandExceptionType(() -> string).create();
        }
    }

}
