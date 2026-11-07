package ro.whistlab.core.rules;

import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.session.MatchSummary;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.*;
import org.junit.Test;

/** Semantic component coverage of the original menu text, distinct from the classic Romanian profile. */
public class OriginalInstructionsContractTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void oi01StandardDeckHasFiftyTwoUniqueCardsForEverySupportedTable(){for(int n=3;n<=6;n++){
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "n", n);
assertEquals("new HashSet<>(GameRules.deck(GameMode.OH_HELL, n)).size() must equal 52", 52,new HashSet<>(GameRules.deck(GameMode.OH_HELL,n)).size());assertEquals("the declared component operation must equal n", n,GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(n,GameMode.OH_HELL,AiDifficulty.NORMAL,1))).snapshot().players().size());}}
    @Test public void oi02BidsCoverTheExactWholeDomainIncludingHighBids(){for(int size=1;size<=17;size++){var domain=GameRules.allowedBids(size,0,false);
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "domain", domain, "size", size);
assertEquals("domain.size() must equal size + 1", size+1,domain.size());assertEquals("domain.get(0) must equal Integer.valueOf(0)", Integer.valueOf(0),domain.get(0));assertEquals("domain.get(size) must equal Integer.valueOf(size)", Integer.valueOf(size),domain.get(size));}}
    @Test public void oi03OnlyTheLastBidHasTheExactSumExclusion(){for(int size=1;size<=17;size++)for(int sum=0;sum<=size*5;sum++)for(int bid=0;bid<=size;bid++){
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "bid", bid, "size", size, "sum", sum);
assertTrue("GameRules.allowedBids(size, sum, false).contains(bid) must hold", GameRules.allowedBids(size,sum,false).contains(bid));assertEquals("GameRules.allowedBids(size, sum, true).contains(bid) must equal sum + bid != size", sum+bid!=size,GameRules.allowedBids(size,sum,true).contains(bid));}}
    @Test public void oi04ExactIsNotAMinimumContract(){
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 3, "declared constant 2", 1, "declared constant 3", 13);
assertEquals("GameRules.scoreDelta(3, 1, GameMode.OH_HELL) must equal 3", 3,GameRules.scoreDelta(3,1,GameMode.OH_HELL));assertEquals("GameRules.scoreDelta(3, 3, GameMode.OH_HELL) must equal 13", 13,GameRules.scoreDelta(3,3,GameMode.OH_HELL));assertEquals("GameRules.scoreDelta(1, 3, GameMode.OH_HELL) must equal 1", 1,GameRules.scoreDelta(1,3,GameMode.OH_HELL));}
    @Test public void oi05ZeroContractGetsTenBonusWhenExact(){
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 10, "declared constant 2", 0, "declared constant 3", 1);
assertEquals("GameRules.scoreDelta(0, 0, GameMode.OH_HELL) must equal 10", 10,GameRules.scoreDelta(0,0,GameMode.OH_HELL));assertEquals("GameRules.scoreDelta(0, 1, GameMode.OH_HELL) must equal 0", 0,GameRules.scoreDelta(0,1,GameMode.OH_HELL));assertEquals("GameRules.scoreDelta(1, 0, GameMode.OH_HELL) must equal 1", 1,GameRules.scoreDelta(1,0,GameMode.OH_HELL));}
    @Test public void oi06EveryOhHellScoreCombinationUsesTricksPlusExactTen(){for(int bid=0;bid<=17;bid++)for(int won=0;won<=17;won++){
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "bid", bid, "won", won);
assertEquals("GameRules.scoreDelta(won, bid, GameMode.OH_HELL) must equal won + (won == bid ? 10 : 0)", won+(won==bid?10:0),GameRules.scoreDelta(won,bid,GameMode.OH_HELL)); }}
    @Test public void oi07BiddingAndFirstLeaderStartAtDealersLeft(){for(int n=3;n<=6;n++)for(int dealer=0;dealer<n;dealer++){var hands=new ArrayList<List<Card>>();for(int seat=0;seat<n;seat++)hands.add(List.of(c(0,2+seat),c(1,2+seat)));var g=GameSession.fromDeal(new GameConfig(n,GameMode.OH_HELL,AiDifficulty.NORMAL,1),hands,Trump.SPADES,dealer);
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "dealer", dealer, "g", g, "hands", hands, "n", n);
assertEquals("g.snapshot().biddingSeat() must equal (dealer + 1) % n", (dealer+1)%n,g.snapshot().biddingSeat());for(int i=0;i<n;i++)g.submitBid(g.snapshot().biddingSeat(),0);assertEquals("g.snapshot().currentSeat() must equal (dealer + 1) % n", (dealer+1)%n,g.snapshot().currentSeat());}}
    @Test public void oi08LeaderMayChooseAnyHeldCard(){var h=List.of(c(0,14),c(1,2),c(2,7));
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "h", h);
assertEquals("GameRules.legalCards(h, null, Trump.SPADES, GameMode.OH_HELL) must equal h", h,GameRules.legalCards(h,null,Trump.SPADES,GameMode.OH_HELL));}
    @Test public void oi09FollowingSuitHasPriorityAfterAnotherPlayerTrumps(){var hand=List.of(c(1,7),c(0,14));
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "hand", hand);
assertEquals("GameRules.legalCards(hand, Suit.HEARTS, Trump.SPADES, GameMode.OH_HELL) must equal List.of(c(1, 7))", List.of(c(1,7)),GameRules.legalCards(hand,Suit.HEARTS,Trump.SPADES,GameMode.OH_HELL));}
    @Test public void oi10OhHellAllowsDiscardEvenWithTrumpAvailable(){var hand=List.of(c(0,7),c(2,14));
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "hand", hand);
assertEquals("GameRules.legalCards(hand, Suit.HEARTS, Trump.SPADES, GameMode.OH_HELL) must equal hand", hand,GameRules.legalCards(hand,Suit.HEARTS,Trump.SPADES,GameMode.OH_HELL));assertEquals("GameRules.legalCards(hand, Suit.HEARTS, Trump.SPADES, GameMode.ROMANIAN_WHIST) must equal List.of(c(0, 7))", List.of(c(0,7)),GameRules.legalCards(hand,Suit.HEARTS,Trump.SPADES,GameMode.ROMANIAN_WHIST));}
    @Test public void oi11HighestLedSuitWinsAndOffSuitAceCannotWin(){
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 2, "declared constant 2", 0, "declared constant 3", 1, "declared constant 4", 7, "declared constant 5", 14, "declared constant 6", 13, "declared constant 7", 3);
assertEquals("GameRules.winningSeat(List.of(p(0, 1, 7), p(1, 2, 14), p(2, 1, 13), p(3, 3, 14)), Trump.NO_TRUMP) must equal 2", 2,GameRules.winningSeat(List.of(p(0,1,7),p(1,2,14),p(2,1,13),p(3,3,14)),Trump.NO_TRUMP));}
    @Test public void oi12AnyTrumpBeatsTheLedSuitAndHighestTrumpWins(){
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 3, "declared constant 2", 0, "declared constant 3", 1, "declared constant 4", 14, "declared constant 5", 7, "declared constant 6", 2, "declared constant 7", 8);
assertEquals("GameRules.winningSeat(List.of(p(0, 1, 14), p(1, 0, 7), p(2, 2, 14), p(3, 0, 8)), Trump.SPADES) must equal 3", 3,GameRules.winningSeat(List.of(p(0,1,14),p(1,0,7),p(2,2,14),p(3,0,8)),Trump.SPADES));}
    @Test public void oi13NoTrumpDoesNotTreatSpadesAsAnImplicitTrump(){
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 0, "declared constant 2", 1, "declared constant 3", 7, "declared constant 4", 14, "declared constant 5", 2, "declared constant 6", 3);
assertEquals("GameRules.winningSeat(List.of(p(0, 1, 7), p(1, 0, 14), p(2, 2, 14), p(3, 3, 14)), Trump.NO_TRUMP) must equal 0", 0,GameRules.winningSeat(List.of(p(0,1,7),p(1,0,14),p(2,2,14),p(3,3,14)),Trump.NO_TRUMP));}
    @Test public void oi14DocumentedFiveSuitCycleAppliesToTheDescendingProfile(){for(int h=0;h<30;h++){
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "h", h);
assertEquals("Trump.forHandIndex(h) must equal Trump.values()[h % 5]", Trump.values()[h%5],Trump.forHandIndex(h)); }}
    @Test public void oi15TrickWinnerLeadsTheNextTrick(){var g=GameSession.fromDeal(new GameConfig(4,GameMode.OH_HELL,AiDifficulty.NORMAL,1),List.of(List.of(c(1,7),c(2,7)),List.of(c(1,14),c(2,8)),List.of(c(1,8),c(2,9)),List.of(c(1,9),c(2,10))),Trump.SPADES,3);for(int s=0;s<4;s++)g.submitBid(s,0);g.playCard(0,c(1,7));g.playCard(1,c(1,14));g.playCard(2,c(1,8));g.playCard(3,c(1,9));
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "g", g);
assertEquals("g.snapshot().lastTrickWinnerSeat() must equal 1", 1,g.snapshot().lastTrickWinnerSeat());assertEquals("g.snapshot().currentSeat() must equal 1", 1,g.snapshot().currentSeat());}
    @Test public void oi16AHandScoresOnlyAfterEveryLastCard(){var g=one();for(int s=0;s<4;s++)g.submitBid(s,0);for(int s=0;s<3;s++){g.playCard(s,c(1,14-s));
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "g", g, "s", s);
assertTrue("g.snapshot().players().stream().allMatch((p)->p.totalScore() == 0) must hold", g.snapshot().players().stream().allMatch(p->p.totalScore()==0));}g.playCard(3,c(1,11));assertEquals("g.snapshot().phase() must equal GamePhase.SCORE", GamePhase.SCORE,g.snapshot().phase());assertEquals("g.snapshot().players().stream().map(GameSession.PlayerSnapshot::totalScore).toList() must equal List.of(1, 10, 10, 10)", List.of(1,10,10,10),g.snapshot().players().stream().map(GameSession.PlayerSnapshot::totalScore).toList());}
    @Test public void oi17NextHandPreservesCumulativeScoresAndResetsBidsAndWonCounters(){var g=GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(4,GameMode.OH_HELL,AiDifficulty.NORMAL,35)));while(g.snapshot().phase()==GamePhase.BIDDING)g.submitBid(g.snapshot().biddingSeat(),0);while(g.snapshot().phase()==GamePhase.PLAYING){var s=g.snapshot();g.playCard(s.currentSeat(),AiPolicy.chooseCard(s,s.currentSeat(),AiDifficulty.NORMAL));}var before=g.snapshot().players().stream().map(GameSession.PlayerSnapshot::totalScore).toList();g.startNextHand();
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "g", g);
assertEquals("g.snapshot().players().stream().map(GameSession.PlayerSnapshot::totalScore).toList() must equal before", before,g.snapshot().players().stream().map(GameSession.PlayerSnapshot::totalScore).toList());assertTrue("g.snapshot().players().stream().allMatch((p)->p.bid() == null && p.tricksWon() == 0) must hold", g.snapshot().players().stream().allMatch(p->p.bid()==null&&p.tricksWon()==0));}
    @Test public void oi18PlayCannotStartBeforeAllBidsAndScoringCannotBeAppliedTwice(){var g=one();
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "g", g);
assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class,()->g.playCard(0,c(1,14)));for(int s=0;s<4;s++)g.submitBid(s,0);for(int s=0;s<4;s++)g.playCard(s,c(1,14-s));var before=g.snapshot();assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class,()->g.playCard(0,c(1,14)));assertEquals("g.snapshot() must equal before", before,g.snapshot());}
    @Test public void oi19CurrentAndLastTrickRetainTheActualPlayOrderAndCannotBeMutated(){var g=one();for(int s=0;s<4;s++)g.submitBid(s,0);for(int s=0;s<3;s++)g.playCard(s,c(1,14-s));var during=g.snapshot();
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "during", during, "g", g);
assertEquals("during.currentTrick().stream().map(PlayedCard::seat).toList() must equal List.of(0, 1, 2)", List.of(0,1,2),during.currentTrick().stream().map(PlayedCard::seat).toList());assertThrows("The operation rejects its input with UnsupportedOperationException.class", UnsupportedOperationException.class,()->during.currentTrick().clear());g.playCard(3,c(1,11));assertEquals("g.snapshot().lastTrick().stream().map(PlayedCard::seat).toList() must equal List.of(0, 1, 2, 3)", List.of(0,1,2,3),g.snapshot().lastTrick().stream().map(PlayedCard::seat).toList());assertEquals("g.snapshot().history() must equal g.snapshot().lastTrick()", g.snapshot().lastTrick(),g.snapshot().history());assertEquals("g.snapshot().lastTrickWinnerSeat() must equal 0", 0,g.snapshot().lastTrickWinnerSeat());assertThrows("The operation rejects its input with UnsupportedOperationException.class", UnsupportedOperationException.class,()->g.snapshot().history().clear());}
    @Test public void oi20BidBalanceDistinguishesUnderEqualAndOverWithoutBanningAnEarlyEqualSum(){var g=GameSession.fromDeal(new GameConfig(4,GameMode.OH_HELL,AiDifficulty.NORMAL,1),List.of(List.of(c(0,2),c(1,2)),List.of(c(0,3),c(1,3)),List.of(c(0,4),c(1,4)),List.of(c(0,5),c(1,5))),Trump.SPADES,3);
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "g", g);
assertEquals("MatchSummary.bidBalance(g.snapshot()) must equal \"-2\"", "-2",MatchSummary.bidBalance(g.snapshot()));g.submitBid(0,2);assertEquals("MatchSummary.bidBalance(g.snapshot()) must equal \"0\"", "0",MatchSummary.bidBalance(g.snapshot()));g.submitBid(1,2);assertEquals("MatchSummary.bidBalance(g.snapshot()) must equal \"+2\"", "+2",MatchSummary.bidBalance(g.snapshot()));g.submitBid(2,0);g.submitBid(3,0);assertEquals("g.snapshot().phase() must equal GamePhase.PLAYING", GamePhase.PLAYING,g.snapshot().phase());}
    private static GameSession one(){return GameSession.fromDeal(new GameConfig(4,GameMode.OH_HELL,AiDifficulty.NORMAL,1),List.of(List.of(c(1,14)),List.of(c(1,13)),List.of(c(1,12)),List.of(c(1,11))),Trump.SPADES,3);}
    private static Card c(int s,int r){return new Card(Suit.values()[s],r);}private static PlayedCard p(int seat,int s,int r){return new PlayedCard(seat,c(s,r));}
}
