package ro.whistlab.core.ai.cost;

import java.util.LinkedHashMap;
import java.util.Map;
import ro.whistlab.coach.IndependentPlayer;
import ro.whistlab.coach.PlannerControl;
import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.ai.simulation.SearchWork;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.testing.match.PlayerObservations;

/** Adapts the two decision APIs while keeping their measurement controls independent. */
enum CostPlayer {
    NORMAL, HARD, INDEPENDENT;

    record Work(long units, Map<String, Long> events, String stopReason) {
        Work { events = Map.copyOf(events); }

        void publish(Map<String, Object> row) {
            row.put("workUnits", units);
            row.put("events", events);
            row.put("controlStopReason", stopReason);
        }
    }

    record Measured(int decision, Work work) { }

    int decide(CostPositions.Scenario scenario) {
        if (this == INDEPENDENT) {
            var position = PlayerObservations.independent(scenario.observation());
            return scenario.bidding() ? IndependentPlayer.bid(position).value() : IndependentPlayer.card(position).value();
        }
        var level = AiDifficulty.valueOf(name());
        if (scenario.bidding()) return AiPolicy.chooseBid(scenario.snapshot(), scenario.actor(), level);
        var card = AiPolicy.chooseCard(scenario.snapshot(), scenario.actor(), level);
        return card.suit().ordinal() * 13 + card.rank() - 2;
    }

    Measured measure(CostPositions.Scenario scenario, long limit, Map<String, Object> row) {
        if (this == INDEPENDENT) {
            try (var control = PlannerControl.measure(limit)) {
                try {
                    int decision = decide(scenario);
                    var metrics = control.snapshot();
                    return new Measured(decision, work(metrics.workUnits(), metrics.events(), metrics.stopReason()));
                } finally {
                    var metrics = control.snapshot();
                    work(metrics.workUnits(), metrics.events(), metrics.stopReason()).publish(row);
                }
            }
        }
        try (var control = SearchWork.measure(limit)) {
            try {
                int decision = decide(scenario);
                var metrics = control.snapshot();
                return new Measured(decision, work(metrics.workUnits(), metrics.events(), metrics.stopReason()));
            } finally {
                var metrics = control.snapshot();
                work(metrics.workUnits(), metrics.events(), metrics.stopReason()).publish(row);
            }
        }
    }

    private static Work work(long units, Map<? extends Enum<?>, Long> events, String stopReason) {
        Map<String, Long> named = new LinkedHashMap<>();
        events.forEach((event, count) -> named.put(event.name(), count));
        return new Work(units, named, stopReason);
    }
}
