package su.hitori.api.util.language;

import su.hitori.api.util.time.TimeUnit;

public final class English implements Language {

    English() {}

    /**
     * Returns the appropriate plural form for English with support for irregular nouns.
     * @param variants provide these forms: singular, plural,
     *                 or singular, plural, irregular exceptions as needed
     * @param count the number to determine plural form for
     * @return the appropriate string variant
     */
    public String defineGrammaticalNumber(String[] variants, int count) {
        if (variants.length == 2 || variants.length == 3)
            return count == 1 ? variants[0] : variants[1];

        throw new IllegalArgumentException("illegal count of variants: should be 2 or 3 for English");
    }

    @Override
    public String timeUnitToLiteral(TimeUnit timeUnit) {
        return switch (timeUnit) {
            case MILLISECOND -> "ms";
            case SECOND -> "s";
            case MINUTE -> "m";
            case HOUR -> "h";
            case DAY -> "d";
            case MONTH -> "mo";
        };
    }

    @Override
    public TimeUnit timeUnitFromLiteral(String literal) {
        return switch (literal.toLowerCase()) {
            case "ms" -> TimeUnit.MILLISECOND;
            case "s" -> TimeUnit.SECOND;
            case "m" -> TimeUnit.MINUTE;
            case "h" -> TimeUnit.HOUR;
            case "d" -> TimeUnit.DAY;
            case "mo" -> TimeUnit.MONTH;
            default -> null;
        };
    }
}
