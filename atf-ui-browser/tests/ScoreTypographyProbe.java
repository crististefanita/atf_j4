import ro.cardtable.ui.score.ScoreTypography;

/** Reads the actual portable contract from source or the delivered runtime JAR. */
public final class ScoreTypographyProbe {
    public static void main(String[] args) {
        System.out.print("{");
        String separator = "";
        for (var role : ScoreTypography.values()) {
            System.out.print(separator + "\"" + role.cssName() + "\":\"" + role.rem() + "\"");
            separator = ",";
        }
        System.out.println("}");
    }
}
