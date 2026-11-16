package ro.cardtable.ui.animation;

import java.util.ArrayList;
import java.util.List;

/** Pure timing and pixel translation; all faces converge on the measured winning seat. */
public final class TrickCollectionMotion {
    public static final long DISPLAY_MILLIS = 1_500;
    public static final long COLLECT_MILLIS = 500;

    public record Point(float x, float y) {
        public Point {
            if (!Float.isFinite(x) || !Float.isFinite(y)) throw new IllegalArgumentException("Invalid point");
        }
    }
    public record Frame(float translationX, float translationY, float scale, float alpha) { }
    public record Sample(float offset, float travel, float scale, float alpha) { }
    public record Timing(long delayMillis, long durationMillis, float startFraction) { }

    private TrickCollectionMotion() { }

    public static Timing timing(long remainingMillis, long layoutDelayMillis) {
        if (remainingMillis < 0 || remainingMillis > DISPLAY_MILLIS || layoutDelayMillis < 0)
            throw new IllegalArgumentException("Invalid collection interval");
        long remaining = Math.max(0, remainingMillis - layoutDelayMillis);
        long duration = Math.min(COLLECT_MILLIS, remaining);
        return new Timing(Math.max(0, remaining - COLLECT_MILLIS), duration,
                (COLLECT_MILLIS - duration) / (float) COLLECT_MILLIS);
    }

    /** Frame progress follows elapsed uptime, including time spent waiting for a busy Android frame. */
    public static float progress(Timing timing, long elapsedMillis) {
        if (elapsedMillis >= timing.durationMillis()) return 1f;
        float elapsed = Math.max(0, elapsedMillis) / (float) timing.durationMillis();
        return timing.startFraction() + (1f - timing.startFraction()) * elapsed;
    }

    public static Frame frame(Point source, Point target, float fraction) {
        var sample = sample(fraction);
        if (sample.offset() == 0f) return new Frame(0, 0, 1, 1);
        return new Frame((target.x() - source.x()) * sample.travel(),
                (target.y() - source.y()) * sample.travel(), sample.scale(), sample.alpha());
    }

    public static List<Sample> keyframes() {
        List<Sample> samples = new ArrayList<>(11);
        for (int step = 0; step <= 10; step++) samples.add(sample(step / 10f));
        return List.copyOf(samples);
    }

    private static Sample sample(float fraction) {
        if (!Float.isFinite(fraction)) throw new IllegalArgumentException("Invalid animation fraction");
        float progress = Math.max(0, Math.min(1, fraction));
        if (progress == 0) return new Sample(0, 0, 1, 1);
        float ease = 1 - (1 - progress) * (1 - progress) * (1 - progress);
        float alpha = progress < .8f ? 1 : Math.max(0, (1 - progress) / .2f);
        return new Sample(progress, ease, 1 - .82f * ease, alpha);
    }
}
