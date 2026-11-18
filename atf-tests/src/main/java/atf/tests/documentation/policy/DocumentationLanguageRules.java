package atf.tests.documentation.policy;

import java.util.Locale;
import java.util.regex.Pattern;

/** Checks visible Markdown, including table cells, link labels and diagram captions. */
public final class DocumentationLanguageRules {
    private static final Pattern ROMANIAN = Pattern.compile(
            "[ăâîșțşţ]|\\b(?:Pe scurt|și|sau|este|sunt|pentru|fără|dacă|fiecare|înainte|după|păstrează|acest|aceste|"
            + "intrare|ieșire|stadiu|implementat|propus|calificat|contractul|matricea|criteriile|bibliotecii|"
            + "verificare|provenienta|pachet|resurse|scop|limite|comportament|calificare|rezervare|rezultat|"
            + "suprapuneri|lansatorul|dosarul|flux|pasii|guvernanta|organizare|instrumentate|frontiera|numele|numar|"
            + "raportul|suita|comune|comun|rulare|politica|metoda|valoare|valori|descriere|calitate|temporar|"
            + "fisier|fisiere|documentatie|acoperire|consumator|implementare|cerinta|cerinte|dovada|dovezi|"
            + "legalitate|modelul|nivelul|tabel|titlu|limba|sesiune|simulat|simulare|antrenare|extragere|"
            + "revizie|revizia|statut|sursa|scorul|licitatia|jocul|progresul|rezultatul|exemplu|exemple|"
            + "verificari|controlul|mediul|publicare|publicarea|reguli|vizibil|vizibila|verificabilitate|cu|"
            + "scenariu|trimite|comenzi|calcule|portabile|recuperabil|profil|copiat|lumi|compatibile|propunere|"
            + "certificat|promovare|propune|atribuie|responsabil|complet|clasament|eligibil|schimbat|oracol|separat|"
            + "rezultate|pereche|componente|mici|partide|nefolosit|extern|autonom|inventar|agregat|"
            + "martor|preliminar|ordonat|prezentare|util|mesaj|controlat|canonic|semnat|identificat|"
            + "permite|citirea|alege|extrage|validat|randate|efectiv|temporizare|audituri|igiena|paleta|"
            + "dezechilibru|lumii|delimitat|redenumire|retragere|selectori|matricilor|parametrizare|punte|tablouri|"
            + "subiect|presupus|pornite|oprite|separatoare|versionat|diagonala|aprofundez|graf|principiu|entitate|"
            + "trimitere|decodare|necesitate|alternativ)\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ENGLISH_HEADING = Pattern.compile(
            "\\b(?:Overview|Usage|Responsibilities|Requirements|Getting started|Quick start|Entry point|English guide|Romanian guide|"
            + "Shared platform|Captured UI timing)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENGLISH_WORDS = Pattern.compile(
            "\\b(the|this|these|that|with|without|and|must|should|their|each|after|before|from|where|when|which|remain|remains)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FENCE = Pattern.compile("^\\s*(`{3,}|~{3,})(.*)$");
    private static final Pattern ROMANIAN_ENDING = Pattern.compile(
            "\\b(?![A-Za-z]*(?:ful|soul|haul)\\b)[\\p{L}]{3,}(?:ului|urilor|urile|ele|ul)\\b"
            + "|\\b[\\p{L}][\\p{L}\\p{N}-]*-(?:ul|ului|urile|urilor|uri)\\b");

    private DocumentationLanguageRules() { }

    public static int firstForeignLine(String source, String language) {
        if (!java.util.List.of("en", "ro").contains(language)) throw new IllegalArgumentException("Unsupported documentation language");
        String marker = null;
        String codeLanguage = null;
        boolean diagram = false;
        String[] lines = source.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            var fence = FENCE.matcher(line);
            if (fence.matches()) {
                if (marker == null) {
                    marker = fence.group(1);
                    codeLanguage = fence.group(2).strip();
                    diagram = codeLanguage.equals("mermaid");
                } else if (fence.group(1).charAt(0) == marker.charAt(0)
                        && fence.group(1).length() >= marker.length()) marker = null;
                continue;
            }
            if (marker != null && !diagram) {
                if (codeLanguage.equals("yaml") && line.matches("^\\s*(?:-\\s+)?reason:.*"))
                    line = line.replaceFirst("^\\s*(?:-\\s+)?reason:\\s*", "");
                else continue;
            }
            String visible = line.replaceAll("`+[^`]*`+", "")
                    .replaceAll("\\]\\((?:[^()]|\\([^)]*\\))*\\)", "]")
                    .replaceAll("https?://\\S+", "");
            if (marker != null && diagram) {
                visible = Pattern.compile("\\[([^]\\r\\n]*)\\]|\\|([^|]*)\\||\\{([^}]*)\\}|\\bas (.+)$|:\\s+(.+)$")
                        .matcher(visible).results().map(match -> match.group()).collect(java.util.stream.Collectors.joining(" "));
            }
            boolean foreign = language.equals("en") ? ROMANIAN.matcher(visible).find() || ROMANIAN_ENDING.matcher(visible).find()
                    : ENGLISH_HEADING.matcher(visible).find() || ENGLISH_WORDS.matcher(visible).results()
                            .map(match -> match.group().toLowerCase(Locale.ROOT)).distinct().count() >= 4;
            if (foreign) return index + 1;
        }
        return 0;
    }
}
