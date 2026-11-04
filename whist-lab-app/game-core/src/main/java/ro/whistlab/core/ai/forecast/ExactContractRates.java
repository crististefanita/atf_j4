package ro.whistlab.core.ai.forecast;

import ro.whistlab.core.rules.GameMode;

/** Exact-contract frequency prior trained only on256 frozen-V2 development games. */
public final class ExactContractRates {
    public static final String TRAINING_DIGEST="0e7f9f609483f051a7a9b82ee2cc6aff56b56de222259eb6d968db27017c86e7";
    private static final double[] FOUR_PLAYER_TEN={0.74347720068276035,0.69679300291545188,0.64820213799805637,0.61273080660835766,0.60009718172983484,0.57191448007774537,0.58649173955296408,0.60350706283487576};
    private ExactContractRates() { }
    public static double probability(GameMode mode,int players,int bonus,int handSize){
        if(handSize<1||handSize>8||players<3||players>6)throw new IllegalArgumentException("Unsupported calibration domain");
        return mode==GameMode.ROMANIAN_WHIST&&players==4&&bonus==10?FOUR_PLAYER_TEN[handSize-1]:.6;
    }
}
