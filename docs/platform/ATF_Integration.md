# Integrarea bibliotecilor comune

Consumerul folosește API-urile livrate prin dependențele declarate, cu o identitate binară verificabilă. Codul aplicației și adaptoarele sale păstrează responsabilitatea semanticii de domeniu.

## Limita de integrare

```mermaid
flowchart LR
    POM[Dependențe declarate] -->|selectează| BINARY[JAR și metadate]
    BINARY -->|expune| API[API public]
    ADAPTER[Adaptor consumer] -->|apelează| API
    TEST[Contract de integrare] -->|verifică| ADAPTER
```

<details>
<summary>Legendă: contractul consumat</summary>

Coordonatele selectează livrarea, iar identitatea fizică a binarului se verifică separat. Consumerul validează contractul de care depinde fără a introduce o copie a implementării generice.

</details>

## Principii

[Consumul bibliotecilor comune](README.md) deține inventarul binar, identitatea fizică și limitele producător–consumator. Integrarea unui API urmează un traseu distinct:

1. Identificați contractul public folosit și dependența efectivă din POM.
2. Construiți adaptorul cu tipurile API și semantica domeniului păstrată explicit.
3. Verificați cazul nominal și refuzurile prin acceptanța independentă a consumatorului.
4. Reconciliați schimbarea API-ului cu configurația, binarul încărcat și adaptoarele afectate.
5. Direcționați schimbarea generică prin [feedback](../feedback/README.md), păstrând local acceptanța contractului folosit.

[CoreReportingConsumerContractTest](../../atf-tests/src/test/java/atf/tests/publishing/CoreReportingConsumerContractTest.java) verifică integrarea comună. Contractele de domeniu și [auditul publicării](../reporting/ATF_Audit.md) adaugă propriile oracole.

## Verificarea exemplelor din ghiduri

[LibraryReadmeExamplesIntegrationTest](../../atf-tests/src/test/java/atf/libraries/LibraryReadmeExamplesIntegrationTest.java) compilează fragmentele Java curente din cele patru ghiduri, cu JAR-urile fizice din `lib` înaintea dependențelor tranzitive. Execuția are un director temporar propriu și compară rezultate observabile; controalele negative disting compilarea invalidă de reușita exemplului.

Dintr-o copie temporară completă, cu JDK 21, modulele și depozitul Maven propriu:

```powershell
mvn -B -pl atf-tests -am '-Dtest=LibraryReadmeExamplesIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Proba verifică integrarea consumatorului și fragmentele documentate; nu modifică JAR-ul și nu înlocuiește regresia producătorului. [Responsabilul comun](../feedback/README.md) păstrează împachetarea și sincronizarea ghidurilor în livrările următoare.

## Modificări concurente

O execuție folosește o copie de build cu surse și biblioteci identificate la început. Schimbarea lor în checkout nu califică retrospectiv copia deja executată.

Configurația, binarul și sursele se verifică împreună. O versiune textuală egală nu justifică folosirea unor octeți diferiți sub aceeași identitate.

[Modulul de integrare](../../atf-tests/README.md) · [Publicarea rezultatelor](../reporting/README.md)
