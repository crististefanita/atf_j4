# Guvernanța retenției și indexării externe

## Responsabilitate

Consumerul Whist Lab deține organizarea și indexarea datelor sale de execuție. Biblioteca comună de raportare deține contractul generic al publicării și randării.

## Problemă de mecanism

Indexarea trebuie să distingă o încercare terminată, o republicare, o acceptanță pe scop și materialul de explorare. Data copierii unui fișier nu este data execuției și nu justifică actualizarea acceptanței.

## Cerințe

- Păstrați ultima încercare separat de ultima acceptanță, pentru fiecare scop.
- Modelați execuția Android nouă și republicarea offline prin tipuri distincte.
- Permiteți înregistrarea unei încercări fără acceptanță vizuală implicită.
- Legați identitatea aplicației, a surselor și a bibliotecilor de execuția originală.
- Păstrați intervalele temporale capturate și absențele explicite.
- Actualizați pointerele atomic sub un singur lock.
- Protejați înregistrările referite de manifest sau pointer înaintea arhivării.
- Păstrați build-urile intermediare în staging, iar datele durabile în arhiva externă.
- Păstrați documentația proiectului pentru mecanisme și contracte.

## Criterii de închidere

1. Fiecare înregistrare durabilă are rol și statut explicit.
2. Țintele pointerelor există și trec verificarea integrității.
3. O încercare neacceptată nu modifică acceptanța.
4. Un reimport identic este idempotent, iar schimbarea aceluiași ID este refuzată.
5. Testele de componentă acoperă concurența, datele lipsă și coruperea identității.

[Contractul handoff-ului](../WHIST_UI_REPORT_HANDOFF.md) · [Responsabilități](README.md)
