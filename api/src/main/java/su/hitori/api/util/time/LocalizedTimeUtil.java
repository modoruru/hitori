package su.hitori.api.util.time;

import su.hitori.api.util.language.Language;

public final class LocalizedTimeUtil {

    private LocalizedTimeUtil() {
    }

    public static String format(long lengthMillis, boolean whiteSpaceBeforeLiteral) {
        return format(lengthMillis, whiteSpaceBeforeLiteral, Language.ENGLISH);
    }

    public static String format(long lengthMillis, boolean whiteSpaceBeforeLiteral, Language language) {
        StringBuilder builder = new StringBuilder();
        long remaining = lengthMillis;

        TimeUnit[] timeUnits = TimeUnit.values();
        for (int i = timeUnits.length - 1; i >= 1; i--) {
            TimeUnit unit = timeUnits[i];
            long millisLength = unit.toMillis();

            if(remaining < millisLength) continue;
            builder.append(remaining / millisLength);
            if(whiteSpaceBeforeLiteral)
                builder.append(' ');
            builder.append(language.timeUnitToLiteral(unit)).append(' ');
            remaining %= millisLength;
        }

        int length = builder.length();
        boolean empty = length == 0;
        if (remaining > 0 && empty) builder.append(remaining).append(language.timeUnitToLiteral(TimeUnit.MILLISECOND));
        else if (!empty && builder.charAt(length - 1) == ' ') builder.deleteCharAt(length - 1);

        return builder.toString();
    }

    public static long parse(String raw) {
        return parse(raw, Language.ENGLISH);
    }

    public static long parse(String raw, Language language) {
        String[] parts = raw.split("\\s+");

        long length = 0L;
        for (String part : parts) {
            int nonDigitIndex = 0;
            var chars = part.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                if(!Character.isDigit(chars[i])) {
                    nonDigitIndex = i;
                    break;
                }
            }

            int value = Integer.parseInt(part.substring(0, nonDigitIndex));
            String literal = part.substring(nonDigitIndex);

            TimeUnit timeUnit = language.timeUnitFromLiteral(literal);
            if(timeUnit == null)
                throw new IllegalArgumentException("Unrecognized time unit: " + literal + '.');

            length += value * timeUnit.toMillis();
        }

        return length;
    }

}
