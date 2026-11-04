package ro.whistlab.core.ai.profile;

/** Offline comparison profiles. Production play keeps CURRENT unless a profile is explicitly supplied by a test. */
public enum HardStrategyProfile {
    CURRENT(0.0),
    PRUDENT_BID_04(0.04),
    PRUDENT_BID(0.08),
    PRUDENT_BID_12(0.12),
    SCORE_FIRST(0.0),
    BURN_IF_DOOMED(0.0);

    private final double bidProbabilityGap;

    HardStrategyProfile(double bidProbabilityGap) { this.bidProbabilityGap=bidProbabilityGap; }

    public double bidProbabilityGap() { return bidProbabilityGap; }
}
