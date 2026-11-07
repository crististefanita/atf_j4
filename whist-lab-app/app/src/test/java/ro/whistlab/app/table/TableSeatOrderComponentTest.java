package ro.whistlab.app.table;

import org.junit.Test;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GameSession;
import java.util.ArrayList;
import java.util.List;
import static atf.gradle.audit.assertions.GradleAssertions.*;

/** Engine identities remain in clockwise order in both the names and their played cards. */
public final class TableSeatOrderComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void namesAndPlayedCardsKeepTheSameClockwiseOrderForThreeToSixSeats() {
        for (int players=3;players<=6;players++) for (int width:new int[]{280,313,353,560,760}) {
            var viewport=new TableViewportPolicy(569);
            var trick=TrickCardMetrics.forTable(players,width,1f,viewport);
            int height=TableGeometry.minimumHeight(players,62,trick.centerHeightDp(),1)+120;
            var plan=TableGeometry.arrangeDisplay(width,height,players,TableLayoutMetrics.seatWidthDp(width,true),62,
                    width,trick.centerHeightDp(),1,true,TableGeometry.CenterAlignment.BALANCED);
            double previousSeat=-1,previousCard=-1;
            for(int seat=0;seat<players;seat++) {
                double nameAngle=angle(plan.seats().get(seat),width,height);
                var slot=trick.slotForSeat(seat);
                int top=slot.row()*trick.rowStrideDp();
                double cardAngle=angle(new TableGeometry.Rect(slot.leftDp(),top,
                        slot.leftDp()+trick.cardWidthDp(),top+trick.cellHeightDp()),width,trick.centerHeightDp());

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "cardAngle", cardAngle, "height", height, "nameAngle", nameAngle, "plan", plan, "players", players, "previousCard", previousCard, "previousSeat", previousSeat, "seat", seat, "slot", slot, "top", top, "trick", trick, "viewport", viewport, "width", width);
                assertTrue("Names must follow engine seat order: "+players+" / "+seat,nameAngle>previousSeat);
                assertTrue("Cards must follow the same seat order: "+players+" / "+seat,cardAngle>previousCard);
                previousSeat=nameAngle;previousCard=cardAngle;
            }
            if(players==4) for(int opponent=1;opponent<4;opponent++)
                assertTrue("plan.seats().get(opponent).bottom() <= plan.center().top() must hold", plan.seats().get(opponent).bottom()<=plan.center().top());
        }
    }

    @Test public void everyGameModeBidsAndPlaysInTheDisplayedSeatOrderFromItsActualLeader() {
        for(var mode:GameMode.values()) for(int players=3;players<=6;players++) {
            var session=GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(players,mode,AiDifficulty.NORMAL,49)));
            int firstBid=session.snapshot().biddingSeat();
            for(int turn=0;turn<players;turn++) {

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "firstBid", firstBid, "mode", mode, "players", players, "session", session, "turn", turn);
                assertEquals("session.snapshot().biddingSeat() must equal (firstBid + turn) % players", (firstBid+turn)%players,session.snapshot().biddingSeat());
                session.submitBid(session.snapshot().biddingSeat(),0);
            }
            int leader=session.snapshot().currentSeat();
            List<Integer> expected=new ArrayList<>();
            for(int turn=0;turn<players;turn++) {
                var state=session.snapshot();int actor=(leader+turn)%players;
                assertEquals("state.currentSeat() must equal actor", actor,state.currentSeat());expected.add(actor);
                var led=state.currentTrick().isEmpty()?null:state.currentTrick().getFirst().card().suit();
                var legal=GameRules.legalCards(state.players().get(actor).hand(),led,state.trump(),state.mode());
                session.playCard(actor,legal.getFirst());
            }
            assertEquals("session.snapshot().lastTrick().stream().map((play)->play.seat()).toList() must equal expected", expected,session.snapshot().lastTrick().stream().map(play->play.seat()).toList());
        }
    }

    private static double angle(TableGeometry.Rect box,int width,int height) {
        double x=width/2.0-(box.left()+box.right())/2.0;
        double y=(box.top()+box.bottom())/2.0-height/2.0;
        double value=Math.atan2(Math.abs(x)<=1?0:x,Math.abs(y)<=1?0:y);
        return value<0?value+Math.PI*2:value;
    }
}
