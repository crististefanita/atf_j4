package ro.whistlab.core.rules;

import java.util.*;

/** Canonical help for the rules implemented here. Android and documentation use this same catalog. */
public final class HowToPlay {
    public enum Language { RO, EN }
    public record Section(String id, String title, String explanation) { }
    private HowToPlay() { }
    private static Section section(Language language,String id,String roTitle,String enTitle,String ro,String en) {
        return new Section(id,language==Language.RO?roTitle:enTitle,language==Language.RO?ro:en);
    }
    public static String title(Language language) { return language==Language.RO?"AJUTOR — CUM SE JOACĂ":"HELP — HOW TO PLAY"; }
    public static List<Section> sections(Language language) {
        Objects.requireNonNull(language,"language");
        return List.of(
            section(language,"goal","Scopul jocului","Objective",
                "În Whist românesc trebuie să câștigi exact câte levate ai licitat. O mână este o distribuire completă; o levată cuprinde câte o carte de la fiecare jucător. Câștigă scorul total cel mai mare; egalitățile se păstrează.",
                "In Romanian Whist, win exactly the number of tricks you bid. A hand is one complete deal; a trick contains one card from each player. The highest total score wins; ties remain ties."),
            section(language,"deck","Jucători și cărți","Players and cards",
                "Joacă 3–6 persoane. Se păstrează cele mai mari 8 × numărul jucătorilor cărți: la 3 jucători de la 9, la 4 de la 7, la 5 de la 5, la 6 de la 3. Asul este cel mai mare, apoi K, Q, J, 10 și valorile descrescătoare. Fiecare jucător joacă pentru sine.",
                "There are 3–6 players. Keep the highest 8 × player count cards: ranks 9 and above for 3 players, 7 for 4, 5 for 5, 3 for 6. Ace is highest, then K, Q, J, 10 and descending numbers. Every player plays individually."),
            section(language,"schedule","Ordinea mâinilor și atuul","Hand schedule and trump",
                "Cu N jucători: N mâini de o carte, câte una de 2, 3, 4, 5, 6, 7 cărți, N mâini de 8, apoi 7, 6, 5, 4, 3, 2 și N mâini de o carte. Împărțitorul se rotește. O carte neîmpărțită întoarsă stabilește atuul; mâinile de 8 sunt fără atu. La 4 jucători sunt 24 de mâini și 94 de levate.",
                "With N players: N one-card hands, one each of 2, 3, 4, 5, 6, 7 cards, N eight-card hands, then 7, 6, 5, 4, 3, 2 and N one-card hands. The dealer rotates. An undealt face-up card determines trump; eight-card hands have no trump. Four players play 24 hands and 94 tricks."),
            section(language,"bidding","Licitarea","Bidding",
                "Licitezi între 0 și numărul de cărți. Începe jucătorul din stânga împărțitorului; ordinea continuă circular. Numai ultima licitație este interzisă dacă ar face suma tuturor licitațiilor egală cu numărul de cărți. Exemplu: la 4 cărți, după 0, 1, 1, ultimul nu poate spune 2. O sumă intermediară egală cu 4 este permisă.",
                "Bid from 0 to the hand size. The player left of the dealer bids first, proceeding clockwise. Only the final bid is forbidden when it would make the total bids equal the hand size. Example: at 4 cards, after 0, 1, 1 the last player cannot bid 2. An intermediate total of 4 is allowed."),
            section(language,"following","Obligația de culoare","Follow suit",
                "Primul jucător poate pune orice carte. Dacă ai culoarea cerută, trebuie să o joci, chiar dacă altcineva a tăiat deja cu atu. Nu ești obligat să depășești o carte mai mare din culoare. Exemplu: la cupă cerută, cu 7 de cupă și as de pică atu, trebuie să pui 7 de cupă.",
                "The leader may play any card. If you hold the led suit, you must follow it, even when another player has already trumped. You do not have to beat a higher card of that suit. Example: hearts led, holding 7 of hearts and ace of spades trump, you must play 7 of hearts."),
            section(language,"trumping","Atu obligatoriu când nu ai culoarea","Mandatory trump when void",
                "Dacă nu ai culoarea cerută și ai atu, trebuie să pui atu. Poți pune orice atu; nu este obligatoriu să supratai un atu deja jucat. Dacă nu ai nici culoarea cerută, nici atu, poți arunca orice carte. Fără atu, când nu ai culoarea cerută poți arunca orice carte.",
                "If you are void in the led suit and hold trump, you must play trump. Any trump is allowed; overtrumping is not compulsory. With neither the led suit nor trump you may discard any card. At no trump, being void lets you discard any card."),
            section(language,"winner","Cine ia levata","Trick winner",
                "Cel mai mare atu câștigă. Dacă nu s-a jucat atu, câștigă cea mai mare carte din culoarea cerută. Un as din altă culoare nu bate culoarea cerută. Câștigătorul începe levata următoare. Fiecare carte se joacă o singură dată.",
                "The highest trump wins. If no trump was played, the highest card of the led suit wins. An off-suit ace cannot beat the led suit. The winner leads the next trick. Each card is played exactly once."),
            section(language,"scoring","Scorul mâinii","Hand scoring",
                "Contract exact: 5 + levate câștigate. Contract ratat: minus diferența absolută dintre licitat și câștigat, indiferent dacă ai luat prea multe sau prea puține. 0 licitat / 0 luat = +5; 2 / 2 = +7; 2 / 4 = −2; 4 / 2 = −2. Scorul poate fi negativ. Mâna se punctează o singură dată.",
                "Exact contract: 5 + tricks won. Missed contract: minus the absolute difference between bid and tricks won, whether over or under. Bid 0 / win 0 = +5; 2 / 2 = +7; 2 / 4 = −2; 4 / 2 = −2. Totals may be negative. Score each hand exactly once."),
            section(language,"series","Serii opționale de cinci mâini","Optional five-hand series",
                "În meniu alegi înaintea jocului bonusul +5 sau +10 (implicit +10), ori dezactivezi seriile. Numai mâinile de 2–8 cărți avansează seria. Al cincilea contract exact adaugă bonusul ales; a cincea ratare scade 5. Seria se resetează după acordare. O mână de o carte nu avansează o serie de același tip, dar un rezultat opus o întrerupe. Bonusul de serie este separat de cele 5 puncte ale fiecărui contract exact.",
                "Before starting, choose a +5 or +10 series bonus in the menu (default +10), or disable series. Only 2–8-card hands advance a series. The fifth exact contract adds the selected bonus; the fifth miss subtracts 5. Reset after awarding. A one-card hand does not advance a matching series, but an opposite outcome breaks it. This bonus is separate from the 5 points awarded for each exact contract."),
            section(language,"variants","Nu confunda variantele","Keep variants distinct",
                "Acest ghid descrie Whist românesc standard din Whist Lab. La Oh Hell poți arunca sau tăia când nu ai culoarea, iar scorul este levate +10 pentru contract exact. Long Romanian inversează programul românesc. Descending rotește pică, cupă, treflă, caro, fără atu. Regulile casei pot diferi; aici testele fixează explicit profilul și bonusul.",
                "This guide describes standard Romanian Whist in Whist Lab. In Oh Hell you may discard or trump when void, and score tricks plus 10 for an exact contract. Long Romanian reverses the Romanian schedule. Descending cycles spades, hearts, clubs, diamonds, no trump. House rules may differ; tests here explicitly fix the profile and bonus.")
        );
    }
    public static String markdown(Language language) {
        var text=new StringBuilder("# ").append(title(language)).append("\n\n");
        for (var section : sections(language)) {
            text.append("## ").append(section.title()).append("\n\n");
            for (String sentence : section.explanation().split("(?<=[.!?])\\s+")) {
                text.append("- ").append(sentence).append('\n');
            }
            text.append('\n');
        }
        return text.toString();
    }
}
