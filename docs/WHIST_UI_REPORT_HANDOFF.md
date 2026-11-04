# Mecanismul handoff-ului UI

Handoff-ul publică rezultatele într-un depozit extern și menține separat încercarea, republicarea și acceptanța unui scop. Identitatea execuției originale se păstrează la orice publicare ulterioară.

## Fluxul publicării

```mermaid
flowchart LR
    TRACE[Trace original] -->|leagă de descriptor| DESC[Descriptor explicit]
    DESC -->|validează| CHECK[Integritate și scop]
    CHECK -->|arhivează imuabil| ARCH[Înregistrare externă]
    ARCH -->|actualizează atomic| POINTER[Pointer pe scop]
```

<details>
<summary>Legendă: publicare și acceptare</summary>

O înregistrare poate descrie succes, eșec sau refuzul execuției. Acceptarea cere controalele stabilite de scop; simpla existență a unui HTML sau trecerea JUnit nu actualizează implicit acceptanța.

</details>

## Contractul descriptorului

| Grup | Responsabilitate |
|---|---|
| Identitate | Run ID, sursa republicării, pachet, scenariu și scop declarat/efectiv. |
| Execuție | Verdict nativ, început/sfârșit capturat, bază temporală și limite. |
| Aplicație și surse | Identitatea binarului instalat, a surselor și a bibliotecilor folosite. |
| Conținut | HTML, manifest, checkpoint-uri și asocieri între acțiuni, observații și comparații. |
| Revizie | Verificările cerute de scop, legate de hash-ul conținutului inspectat. |

Datele lipsă rămân marcate explicit. Un timp estimat, o identitate citită ulterior sau o distribuție întreruptă nu devin capturi originale complete.

## Spații de nume externe

| Contract | Semnificație |
|---|---|
| runs și metadata | Înregistrări imuabile pentru fiecare execuție sau republicare. |
| latest-attempt pe scop | Cea mai recentă încercare înregistrată, inclusiv eșecul. |
| latest-accepted pe scop | Cea mai recentă acceptanță care îndeplinește criteriile scopului. |
| public-trace | Observațiile permise pentru replay, distincte de datele ex-post ale oracolului. |
| indexes | Indici generați de conveniență, fără autoritate de a declara acceptanța. |

## Invariante operaționale

- Identificatorul unei înregistrări este imuabil; reimportul identic este idempotent.
- Pointerele sunt ordonate după timpul execuției, cu un singur lock și înlocuire atomică.
- O republicare veche nu devine execuție nouă prin data copierii.
- Scope-ul și pachetul delimitează rezultatele care pot fi comparate.
- Datele rezervate oracolului nu ajung în API-ul jucătorului.
- Rădăcina externă este transmisă explicit, independent de directorul de staging.
- Indexarea automată refuză destinațiile din checkout și nu rescrie README-uri editoriale.

## Implementări și limite

[whist_ui_handoff.py](../scripts/whist_ui_handoff.py) implementează republicarea offline, cu descriptor și rădăcină externă. Tipul său de înregistrare nu trebuie folosit pentru a pretinde o nouă execuție Android.

[WhistExtentFeedback](../atf-test-android/src/main/java/atf/android/whist/evidence/WhistExtentFeedback.java) publică indexul unei rulări auditate în spațiul extern indexes. Rădăcina de handoff și directorul indexului sunt configurabile; directorul sursă nu este o destinație validă.

[Arhitectura publicării](reporting/README.md) · [Contractul Android](ATF_Android_Report_Contract.md) · [Criterii pentru handoff](feedback/UI_REPORT_HANDOFF_Feedback.md)
