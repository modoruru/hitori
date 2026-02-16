package su.hitori.api.util.language;

import su.hitori.api.util.time.TimeUnit;

import java.util.Locale;

public final class Russian implements Language {

    Russian() {}

    /**
     * @param variants provide these forms: singular, few, many (единственное число, немного, много)
     */
    @Override
    public String defineGrammaticalNumber(String[] variants, int count) {
        if(variants.length != 3) throw new IllegalArgumentException("illegal count of variants: should be 3");

        if (count % 100 >= 11 && count % 100 <= 14)
            return variants[2]; // many

        return switch (count % 10) {
            case 1 -> variants[0]; // singular
            case 2, 3, 4 -> variants[1]; // few
            default -> variants[2]; // many
        };
    }

    @Override
    public String timeUnitToLiteral(TimeUnit timeUnit) {
        return switch (timeUnit) {
            case MILLISECOND -> "мс";
            case SECOND -> "сек";
            case MINUTE -> "мин";
            case HOUR -> "ч";
            case DAY -> "д";
            case MONTH -> "мес";
        };
    }

    @Override
    public TimeUnit timeUnitFromLiteral(String literal) {
        return switch (literal.toLowerCase(Locale.of("ru", "RU"))) {
            case "мс" -> TimeUnit.MILLISECOND;
            case "сек" -> TimeUnit.SECOND;
            case "мин" -> TimeUnit.MINUTE;
            case "ч" -> TimeUnit.HOUR;
            case "д" -> TimeUnit.DAY;
            case "мес" -> TimeUnit.MONTH;
            default -> null;
        };
    }

}
