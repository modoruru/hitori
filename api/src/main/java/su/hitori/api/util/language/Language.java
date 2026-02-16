package su.hitori.api.util.language;

import su.hitori.api.util.time.TimeUnit;

public interface Language {

    Russian RUSSIAN = new Russian();
    English ENGLISH = new English();

    String defineGrammaticalNumber(String[] variants, int count);

    String timeUnitToLiteral(TimeUnit timeUnit);
    TimeUnit timeUnitFromLiteral(String literal);

}
