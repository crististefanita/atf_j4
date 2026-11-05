package atf.tests.governance.documentation.release;

import java.io.IOException;
import java.nio.file.Path;

/** Delimitează intrarea curentă, fără a o confunda cu tabelul tuturor versiunilor. */
final class ReleaseCatalogPresentation {
    private ReleaseCatalogPresentation() {}

    static String currentSection(Path file, String catalog) throws IOException {
        int start = catalog.indexOf("## Versiunea curentă");
        int end = catalog.indexOf("## Versiuni disponibile");
        if (start < 0 || end < start) {
            throw new IOException("RELEASE-CATALOG-SECTIONS: " + file
                    + "; expected '## Versiunea curentă' before '## Versiuni disponibile'");
        }
        return catalog.substring(start, end);
    }
}
