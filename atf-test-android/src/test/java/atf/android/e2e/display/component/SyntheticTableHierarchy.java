package atf.android.e2e.display.component;

import atf.android.e2e.display.profile.ResponsiveDisplayProfile;

/** Independent measured observations, not an implementation of the application's layout algorithm. */
final class SyntheticTableHierarchy {
    enum Fault { NONE, MISSING_HUMAN, SHORT_SEAT, SHORT_FACE, SCROLL, ZERO_TITLE, ZERO_TRUMP, OVERLAP, OBSTRUCTED_WINDOW,
        MISSING_ACTION, ACTION_BELOW_NAME, CUT_ACTION, TALL_PLAYED_FACE, TALL_HAND_FACE,
        THIN_HAND_FACE, THIN_PLAYED_FACE, WIDE_PLAYED_FACE, WIDE_HAND_BAND, ZERO_HAND_BAND }
    private static final String[] INDICES={"3♠","4♠","5♠","6♠","7♠","8♠","9♠","10♠","J♠","Q♥","K♣","A♦","2♥"};
    private final ResponsiveDisplayProfile profile;
    private final double density;
    private final int origin;

    SyntheticTableHierarchy(ResponsiveDisplayProfile profile) {
        this.profile=profile;density=profile.density()/160.0;origin=(profile.width()-px(320))/2;
    }

    String xml(int count,boolean trick,float fontScale,Fault fault) {
        var xml=new StringBuilder("<hierarchy>");
        int bottom=fault==Fault.OBSTRUCTED_WINDOW?px(500):profile.height();
        xml.append("<node class=\"android.widget.FrameLayout\" resource-id=\"android:id/content\" bounds=\"[0,0][")
                .append(profile.width()).append(',').append(bottom).append("]\">");
        xml.append(node("android.widget.TextView","Whist · Descending","",bounds(10,4,210,fault==Fault.ZERO_TITLE?0:30)));
        xml.append(node("android.widget.FrameLayout","","Trump: Spades",bounds(260,4,57,fault==Fault.ZERO_TRUMP?0:34)));
        xml.append("<node class=\"").append(fault==Fault.SCROLL?"android.widget.ScrollView":"android.widget.FrameLayout")
                .append("\" content-desc=\"Playing surface\" bounds=\"").append(bounds(10,45,300,385)).append("\">");
        int[][] seats={{133,355},{16,210},{133,50},{250,210}};
        for(int seat=0;seat<4;seat++) {
            if(seat==0&&fault==Fault.MISSING_HUMAN)continue;
            int height=seat==0&&fault==Fault.SHORT_SEAT?20:(int)Math.ceil(44*fontScale);
            xml.append(node("android.widget.LinearLayout","",(seat==0?"You":"AI"+seat)+". Bid 0. Won 0. Score 0."
                            +(seat==0&&!trick?" Current turn. PLAY NOW":""),
                    bounds(seats[seat][0],seats[seat][1],54,height)));
        }
        if (!trick && fault != Fault.MISSING_ACTION) {
            int labelHeight = (int)Math.ceil(12*fontScale);
            int y = fault == Fault.ACTION_BELOW_NAME ? 357 : 355 - 3 - labelHeight;
            xml.append(node("android.widget.TextView",fault == Fault.CUT_ACTION ? "PLAY N…" : "PLAY NOW",
                    "",bounds(133,y,54,labelHeight)));
        }
        if(trick) {
            String[] descriptions={"King of hearts","Queen of spades","2 of spades","Ace of clubs"};
            String[] labels={"K♥","Q♠","2♠","A♣"};
            int[][] places={{145,125},{90,185},{145,245},{200,185}};
            for(int seat=0;seat<4;seat++) {
                int x=places[seat][0],y=places[seat][1];
                if(seat==3&&fault==Fault.OVERLAP){x=133;y=355;}
                int height=profile.playedCardFaceHeightDp();
                int width=height==64?46:50;
                if(seat==0&&fault==Fault.THIN_PLAYED_FACE)width=44;
                if(seat==0&&fault==Fault.WIDE_PLAYED_FACE)width=88;
                xml.append("<node class=\"android.widget.LinearLayout\" content-desc=\"AI").append(seat)
                        .append(" played ").append(descriptions[seat]).append("\" bounds=\"").append(bounds(x,y,width,height+2)).append("\">")
                        .append(node("android.widget.TextView",labels[seat],"",bounds(x,y+1,width,
                                height+(seat==0&&fault==Fault.TALL_PLAYED_FACE?1:0)))).append("</node>");
            }
        }
        xml.append("</node><node class=\"android.widget.FrameLayout\" content-desc=\"Your hand, ").append(count)
                .append(" cards.\" bounds=\"").append(bounds(10,440,300,120)).append("\">");
        int cardWidth=px(63),advance=count==1?0:Math.min(px(22),(px(300)-cardWidth)/(count-1));
        int start=origin+px(10)+(px(300)-cardWidth-(count-1)*advance)/2;
        for(int card=0;card<count;card++) {
            int heightDp=card==0&&fault==Fault.TALL_HAND_FACE?98:card==0&&fault==Fault.SHORT_FACE?87:88;
            int left=start+card*advance,top=px(450),height=px(heightDp);
            int faceWidth=card==count-1&&fault==Fault.THIN_HAND_FACE?px(42):card==count-1?cardWidth:advance;
            if(card==0&&fault==Fault.WIDE_HAND_BAND)faceWidth=px(64);
            if(card==0&&fault==Fault.ZERO_HAND_BAND)faceWidth=0;
            String face="["+left+","+top+"]["+(left+faceWidth)+","+(top+height)+"]";
            String index=INDICES[card];
            String label=index.substring(0,index.length()-1)+"&#10;"+index.substring(index.length()-1);
            xml.append("<node class=\"android.widget.FrameLayout\" bounds=\"").append(face).append("\">")
                    .append(node("android.widget.TextView",label,"","["+(left+2)+","+(top+2)+"]["+(left+px(18))+","+(top+px(35))+"]"))
                    .append("</node>");
        }
        return xml.append("</node></node></hierarchy>").toString();
    }
    private int px(int dp){return (int)Math.round(dp*density);}
    private String bounds(int x,int y,int width,int height){
        int left=origin+px(x),top=px(y);
        return "["+left+","+top+"]["+(left+px(width))+","+(top+px(height))+"]";
    }
    private static String node(String type,String text,String description,String bounds){
        return "<node class=\""+type+"\" text=\""+text+"\" content-desc=\""+description+"\" bounds=\""+bounds+"\"/>";
    }
}
