package su.hitori.api.util.time;

public enum TimeUnit {

    MILLISECOND(1L),
    SECOND     (1000L),
    MINUTE     (60 * 1000L),
    HOUR       (60 * 60 * 1000L),
    DAY        (24 * 60 * 60 * 1000L),
    MONTH      (30 * 24 * 60 * 60 * 1000L);

    private final long millis;

    TimeUnit(long millis) {
        this.millis = millis;
    }

    public long toMillis() {
        return millis;
    }

}
