package su.hitori.api.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts names from one cases to another.<br>
 * Example usage: <pre>{@code
 * String className = ExampleClass.class.getSimpleName();
 * String classNameInSnakeCase = NameFormatter.fromPascalCase(className).toSnake();
 * }</pre>
 */
public final class NameFormatter {

    private static final String[] CONVERT = new String[0];

    private final String[] parts;
    private final boolean[] acronyms;

    private NameFormatter(List<String> parts, boolean[] acronyms) {
        this.parts = parts.toArray(CONVERT);
        this.acronyms = acronyms;
    }

    /**
     * PascalCase
     */
    public String toPascal() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            builder.append(Character.toUpperCase(part.charAt(0)));

            if(acronyms[i]) builder.append(part.substring(1).toUpperCase());
            else builder.append(part.substring(1));
        }
        return builder.toString();
    }

    /**
     * camelCase
     */
    public String toCamel() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if(i == 0) builder.append(part);
            else {
                builder.append(Character.toUpperCase(part.charAt(0)));
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    /**
     * snake_case
     */
    public String toSnake() {
        return toNameWithSeparator('_');
    }

    /**
     * kebab-case
     */
    public String toKebab() {
        return toNameWithSeparator('-');
    }

    /**
     * SCREAMING_SNAKE_CASE
     */
    public String toScreamingSnake() {
        return toSnake().toUpperCase();
    }

    private String toNameWithSeparator(char separator) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            builder.append(parts[i]);
            if(i != parts.length - 1) builder.append(separator);
        }
        return builder.toString();
    }

    public static NameFormatter fromAnyCase(String anyCaseName) {
        if(anyCaseName.isEmpty()) return new NameFormatter(List.of(), new boolean[0]);

        if(anyCaseName.indexOf('_') >= 0) return NameFormatter.fromSnakeCase(anyCaseName);
        if(anyCaseName.indexOf('-') >= 0) return NameFormatter.fromKebabCase(anyCaseName);

        if(Character.isUpperCase(anyCaseName.charAt(0))) return NameFormatter.fromPascalCase(anyCaseName);

        return NameFormatter.fromCamelCase(anyCaseName);
    }

    /**
     * PascalCase
     */
    public static NameFormatter fromPascalCase(String pascalCaseName) {
        List<String> parts = new ArrayList<>();
        List<Integer> acronymIndexes = new ArrayList<>();

        char[] chars = pascalCaseName.toCharArray();
        int lastPartStart = 0;
        boolean onAcronym = false;
        for (int i = 1; i < chars.length; i++) {
            if(i == chars.length - 1d) {
                if(onAcronym) acronymIndexes.add(parts.size());
                parts.add(pascalCaseName.substring(lastPartStart, i + 1).toLowerCase());
                break;
            }

            char ch = chars[i];
            if(Character.isLowerCase(ch) && onAcronym) {
                acronymIndexes.add(parts.size());
                parts.add(pascalCaseName.substring(lastPartStart, i - 1).toLowerCase());
                lastPartStart = i - 1;
                onAcronym = false;
                continue;
            }

            if(Character.isUpperCase(ch) && !onAcronym) {
                if(Character.isUpperCase(chars[i + 1]))
                    onAcronym = true;

                if(i == 1) continue;
                parts.add(pascalCaseName.substring(lastPartStart, i).toLowerCase());
                lastPartStart = i;
            }
        }

        boolean[] acronyms = new boolean[parts.size()];
        for (Integer acronymsIndex : acronymIndexes) {
            acronyms[acronymsIndex] = true;
        }
        return new NameFormatter(parts, acronyms);
    }

    /**
     * camelCase
     */
    public static NameFormatter fromCamelCase(String camelCaseName) {
        return fromPascalCase(camelCaseName);
    }

    /**
     * snake_case
     */
    public static NameFormatter fromSnakeCase(String snakeCaseName) {
        return withSymbolSeparator('_', snakeCaseName);
    }

    /**
     * kebab-case
     */
    public static NameFormatter fromKebabCase(String kebabCaseName) {
        return withSymbolSeparator('-', kebabCaseName);
    }

    /**
     * SCREAMING_SNAKE_CASE
     */
    public static NameFormatter fromScreamingSnakeCase(String screamingSnakeCaseName) {
        return fromSnakeCase(screamingSnakeCaseName.toLowerCase());
    }

    private static NameFormatter withSymbolSeparator(char separator, String name) {
        List<String> parts = new ArrayList<>();

        char[] chars = name.toCharArray();
        int lastPartStart = 0;
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            boolean end = i == chars.length - 1;
            if((separator == ch && i != 0) || end) {
                parts.add(name.substring(lastPartStart, (end ? i + 1 : i)).toLowerCase());
                lastPartStart = i + 1;
            }
        }

        return new NameFormatter(parts, new boolean[parts.size()]);
    }

}
