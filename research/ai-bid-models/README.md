# Experimente izolate pentru modelarea licitațiilor

## Pe scurt

Acest director păstrează implementări alternative și mecanisme de calificare pentru cercetarea AI. Sursele sale sunt excluse din aplicație și din selecția implicită a testelor.

## Aplicarea unei alternative

```mermaid
flowchart LR
CAT[Catalog candidat] -->|declară fișiere| GUARD[Verificare hash-uri]
BASE[Sursă de bază] -->|trece verificarea| GUARD
GUARD -->|permite suprapunere de surse| TEMP[Copie temporară]
TEMP -->|rulează| GATES[Componente și protocol]
GATES -->|informează| REVIEW[Decizie de promovare]
```

<details>
<summary>Legendă și limite</summary>

Suprapunerea de surse se aplică exclusiv în copia temporară și numai după verificarea bazei și a fișierelor declarate. Rezultatul evaluării nu activează automat alternativa în aplicație.

</details>

[Catalogul candidaților](../../config/whist-research-candidates.json) definește identitatea, fișierele, rolurile și colecțiile permise. [Lansatorul](../../scripts/run-whist-collection.ps1) refuză o bază modificată în loc să actualizeze tacit hash-urile.

## Principii

- Modificați separat modelul licitației, modelul mâinilor ascunse și politica de continuare pentru a putea interpreta cauza progresului.
- Predicția comportamentului observat nu demonstrează o decizie optimă sau o probabilitate calibrată a cărților ascunse.
- Înghețați alternativa și criteriile înaintea lotului rezervat; aplicați aceeași distribuție și același loc în comparațiile pereche.
- Păstrați jocurile, calibrările, rezultatele și registrele de lot în arhiva externă.
- Păstrați în sursă mecanismul și regresiile minime justificate, cu limitele lor explicite.

[Arhitectura învățării](../../docs/AI_LEARNING_ARCHITECTURE.md) și [protocolul](../../docs/AI_TRAINING_PROTOCOL.md) definesc procesul comun.
