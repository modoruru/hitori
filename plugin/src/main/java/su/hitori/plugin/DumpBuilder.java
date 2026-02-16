package su.hitori.plugin;

final class DumpBuilder {

    private final StringBuilder base, styled;

    DumpBuilder() {
        base = new StringBuilder();
        styled = new StringBuilder();
    }

    DumpBuilder append(Object str) {
        base.append(str);
        styled.append(str);
        return this;
    }

    DumpBuilder appendAqua(Object str) {
        return appendColored(str, "aqua");
    }

    DumpBuilder appendYellow(Object str) {
        return appendColored(str, "yellow");
    }

    private DumpBuilder appendColored(Object str, String color) {
        base.append(str);
        styled.append("<color:").append(color).append('>').append(str).append("</color>");
        return this;
    }

    // break line
    DumpBuilder newLine() {
        return append('\n');
    }

    public String baseToString() {
        return base.toString();
    }

    public String styledToString() {
        return styled.toString();
    }

}
