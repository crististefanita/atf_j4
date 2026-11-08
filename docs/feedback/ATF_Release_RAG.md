# Guvernanța RAG pentru calificarea livrărilor

## Domeniu și limite

Guvernanța descrie cum se agregă dovezile deja produse pentru o livrare și cum se blochează contradicțiile dintre surse, rezultate, manifest și linkuri publice. Ea nu execută build-uri, teste, semnare sau publicare și nu afirmă existența unui motor automat de recuperare RAG.

Ediția curentă declară numai Android. Un APK și un IPA/Archive sunt livrabile distincte: testele motorului comun pot susține mai multe platforme, dar nu califică build-ul nativ, semnarea sau distribuția alteia. O platformă viitoare trebuie declarată separat și trece prin porțile proprii.

## Contract de guvernanță

| Prioritate | Cerință | Criteriu de închidere |
|---|---|---|
| P1 | Snapshot și corpus fixate | Sursele sunt allowlistate; raportul identifică repository-ul, commitul, arborele, starea worktree-ului și hash-ul fiecărei surse. Schimbarea unei intrări invalidează citările și receipt-urile afectate. |
| P1 | Citate verificabile | Fiecare afirmație de stare citează calea, liniile și SHA-256 din snapshot. README-ul, feedback-ul sau un rezumat nu produce `PASS`. |
| P1 | Stări fail-closed | Se folosesc `PASS`, `FAIL`, `BLOCKED`, `NOT_RUN`, `NOT_APPLICABLE` și `NOT_IMPLEMENTED`. O dovadă lipsă nu devine trecută; o funcție absentă nu este prezentată drept test neexecutat. |
| P1 | Catalog coerent | Versiunea din index, manifest, dosar și linkurile publice trebuie să coincidă. Orice contradicție blochează verdictul; README-ul nu este autoritatea pentru identitatea artefactului. |
| P1 | Porți independente pe platformă | Android cere APK, identitate, semnare și descărcare proprie. iOS cere probe Xcode, puntea nativă, identitate și semnare proprie, plus distribuție iOS verificată. Android nu poate satisface nicio poartă nativă iOS. |
| P1 | Agregare conservatoare | Verdictul de pregătire cere trecerea tuturor platformelor declarate. Rezultatul maxim este `READY_FOR_HUMAN_REVIEW`; numai procesul existent și operatorul pot publica. |
| P1 | RAG read-only | RAG-ul citește receipt-uri deja produse; nu rulează comenzi, nu construiește, nu semnează, nu modifică surse, tag-uri sau asset-uri și nu publică. Raportul și indexul se scriu în afara checkout-ului. |
| P2 | Probe negative controlate | Testele demonstrează citări expirate, receipt lipsă, digest divergent, gate blocat și substituție Android→iOS; fiecare caz trebuie să rămână blocat cu diagnostic și pas următor. |

Configurația executabilă este [politica de dovezi pe platforme](../../config/release-platform-rag-policy.json), iar verificările de componentă sunt în [ReleaseRagPlatformGovernanceTest](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseRagPlatformGovernanceTest.java). Contractul separă platformele și nu deduce o calificare din textul documentației.

Pentru o livrare iOS viitoare sunt necesare build și teste Xcode, identitatea Bundle ID/build, semnare cu identitate de distribuție și o rută verificată TestFlight/App Store. APK-ul Android rămâne un artefact Android; un IPA sau o arhivă nu poate fi redenumit ori prezentat ca APK.

## Închiderea

Păstrați motorul de recuperare și arhitectura aplicației în proprietarii lor existenți. Închideți criteriile cu teste pozitive și negative pe fixture-uri controlate, apoi conectați orice adaptor la receipt-urile produse de porțile release. Verdictul agregat nu poate depăși `READY_FOR_HUMAN_REVIEW`, iar publicarea rămâne în procesul existent.
