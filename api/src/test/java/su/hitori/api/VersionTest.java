package su.hitori.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.List;

public final class VersionTest {

    @Test
    public void testBasicCases() {
        final List<String> cases = List.of(
                "0.1.0",
                "12.3.7",
                "1.0.0",
                "156.17.53"
        );

        for (String aCase : cases) {
            Assertions.assertEquals(aCase, new Version(aCase).toString());
        }
    }

    @Test
    public void testIllegalCases() {
        final List<String> cases = List.of(
                "v0.1.0",
                "843718941",
                "24-2.2467146",
                "24w07a"
        );

        for (String aCase : cases) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> new Version(aCase));
        }
    }

    @Test
    public void testAdvancedCases() {
        final List<String> cases = List.of(
                "1.0.0-alpha",
                "26.2.0-snapshot6",
                "7.0.3-pre+build3",
                "1.0.3+08-15-2026"
        );

        for (String aCase : cases) {
            try {
                Assertions.assertEquals(aCase, new Version(aCase).toString());
            }
            catch (IllegalArgumentException illegalArgumentException) {
                throw new AssertionFailedError(String.format(
                        "\"%s\" on \"%s\" case",
                        illegalArgumentException.getMessage(),
                        aCase
                ));
            }
        }
    }

}
