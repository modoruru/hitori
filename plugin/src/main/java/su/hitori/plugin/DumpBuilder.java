package su.hitori.plugin;

import org.jspecify.annotations.Nullable;

final class DumpBuilder {

    private final StringBuilder base, styled;
    private int indentation, sectionDepth;

    DumpBuilder() {
        base = new StringBuilder();
        styled = new StringBuilder();
    }

    DumpBuilder indentation(int indentation) {
        this.indentation = indentation;
        return this;
    }

    DumpBuilder startSection(String name) {
        appendWithIndentation(name).newLine();
        ++sectionDepth;
        return this;
    }

    DumpBuilder appendParameter(String name, Object value, @Nullable String color) {
        return appendWithIndentation(name)
                .appendBaseAndStyled(": ", ": ")
                .appendBaseAndStyled(value, color == null ? value : String.format("<color:%s>%s</color>", color, value))
                .newLine();
    }


    DumpBuilder dropSection() {
        --sectionDepth;
        return this;
    }

    private DumpBuilder appendWithIndentation(Object base) {
        if(indentation > 0 && sectionDepth > 0) {
            this.base.repeat(' ', indentation * sectionDepth);
            this.styled.repeat(' ', indentation * sectionDepth);
        }
        return appendBaseAndStyled(base, base);
    }

    private DumpBuilder appendBaseAndStyled(Object base, Object styled) {
        this.base.append(base);
        this.styled.append(styled);
        return this;
    }

    DumpBuilder newLine() {
        return appendBaseAndStyled('\n', '\n');
    }

    String baseToString() {
        return base.toString();
    }

    String styledToString() {
        return styled.toString();
    }

}
