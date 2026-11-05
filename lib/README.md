# Biblioteci ATF

## Pe scurt

Patru biblioteci Java 21, consumate prin Maven. Ghidurile de mai jos arată rolul, API-ul de pornire și un exemplu pentru fiecare JAR; proiectul păstrează Whist Lab și adaptoarele Android.

| Bibliotecă | Ce oferă | Folosiți-o pentru |
|---|---|---|
| [Core](README.Core.md) | HTTP, configurație, JSON, redactare și lifecycle | Mecanisme comune și adaptoare neutre. |
| [Reporting](README.Reporting.md) | Extent/fallback, aserțiuni și dovezi UI/API | Rapoarte auditabile din observațiile consumerului. |
| [Console](README.Console.md) | Evenimente și diagnostic configurabil | Urmărirea execuției în Maven și IntelliJ. |
| [Mock](README.Mock.md) | Transport HTTP determinist | Componente și contracte offline, cu catalog propriu. |

## Integrare

1. Din rădăcina unei copii TMP, rulați `mvn -B -ntp -N validate`; bootstrap-ul instalează perechile locale în repository-ul Maven selectat.
2. Declarați dependența în modulul care o folosește; păstrați versiunile din proprietățile POM-ului rădăcină.
3. În IntelliJ, folosiți aceleași setări și același repository Maven, apoi reîncărcați POM-ul rădăcină.

<details>
<summary>Exemplu Maven — Mock</summary>

```xml
<dependency>
    <groupId>atf.api</groupId>
    <artifactId>atf-mock</artifactId>
    <version>${atf.mock.version}</version>
</dependency>
```

Core și dependențele externe vin din POM-ul publicat; adaptorul JUnit Console cere Kit declarat explicit. Reporting este independent de celelalte module ATF.

</details>

## Identitate și verificare

- `lib` păstrează patru perechi JAR/POM și cele cinci ghiduri de aici, direct în director, cu o singură versiune per bibliotecă.
- [POM-ul rădăcină](../pom.xml) selectează coordonatele; [proveniența](../library-provenance.json) păstrează hash-urile și originea. [Contractul tehnic](../docs/platform/README.md) detaliază integrarea.
- În TMP: `mvn -B -ntp -pl atf-tests -am -Dtest=PlatformLibraryContractTest,PlatformLibraryIntegrityTest -Dsurefire.failIfNoSpecifiedTests=false test`.

## Mentenanță și feedback

Codul, testele native și backlog-ul bibliotecilor aparțin proiectului separat `atf_j0`; destinațiile sunt în `atf_j0/lib/README.md`. Raportați acolo JAR-ul/hash-ul, reproducerea și rezultatul așteptat/observat, fără checklist-uri duplicate.

Remedierea și regresia se fac în J0/TMP; același candidat este apoi sincronizat și acceptat independent în consumerii afectați. Scenariile, configurația și oracolele domeniului rămân locale.
