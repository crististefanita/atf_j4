package atf.tests.distribution;

import java.math.BigInteger;

/** Numeric release comparison, retaining three-part historical identities. */
public final class ReleaseVersion {
    private static final String PATTERN = "(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)(?:\\.(?:0|[1-9][0-9]*))?(?:-[0-9A-Za-z.-]+)?";

    private ReleaseVersion() { }

    public static boolean isValid(String version) {
        return version != null && version.matches(PATTERN);
    }

    public static boolean atLeast(String version, String minimum) {
        if (!isValid(version) || !isValid(minimum)) return false;
        String[] current = version.split("-", 2)[0].split("\\.");
        String[] threshold = minimum.split("-", 2)[0].split("\\.");
        for (int index = 0; index < 3; index++) {
            var left = index < current.length ? new BigInteger(current[index]) : BigInteger.ZERO;
            var right = index < threshold.length ? new BigInteger(threshold[index]) : BigInteger.ZERO;
            int comparison = left.compareTo(right);
            if (comparison != 0) return comparison > 0;
        }
        return true;
    }
}
