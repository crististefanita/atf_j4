package ro.whistlab.core.ai.cost;

import static org.junit.Assert.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Orchestrates generated contexts; profile, decision adaptation and domain bounds have separate owners. */
@RunWith(Parameterized.class)
public class AiCostQualificationTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    @Parameterized.Parameters(name = "handSize={0}")
    public static Collection<Object[]> sizes() {
        return IntStream.rangeClosed(2, 8).mapToObj(size -> new Object[]{size}).toList();
    }

    private final int size;
    public AiCostQualificationTest(int size) { this.size = size; }

    @Test(timeout = 120000)
    public void generatedContextsStayLegalDeterministicAndWithinTheDeclaredJvmProfile() throws Exception {
        var profile = CostProfile.read(Path.of(required("whist.aiCost.profile")));
        Path output = Path.of(required("whist.aiCost.output"));
        ThreadMXBean cpu = ManagementFactory.getThreadMXBean();
        assertTrue("Thread CPU clock required", cpu.isCurrentThreadCpuTimeSupported());
        if (!cpu.isThreadCpuTimeEnabled()) cpu.setThreadCpuTimeEnabled(true);
        var rows = new ArrayList<Map<String, Object>>();
        var report = report(profile, rows);
        Throwable failure = null;
        try {
            for (int seed : profile.seeds()) for (var stage : CostPositions.Stage.values()) {
                if (size == 2 && stage == CostPositions.Stage.CARD_LATE) continue;
                var scenario = CostPositions.create(size, seed, stage);
                for (var player : CostPlayer.values()) runScenario(profile, scenario, player, seed, cpu, rows);
            }
            report.put("status", "PASS");
        } catch (RuntimeException | Error problem) {
            failure = problem;
            throw problem;
        } finally {
            if (report.get("status").equals("RUNNING")) report.put("status", "FAIL");
            try { writeReport(output, report); }
            catch (IOException | RuntimeException publication) {
                if (failure == null) throw publication;
                failure.addSuppressed(publication);
            }
        }
    }

    private static void runScenario(CostProfile profile, CostPositions.Scenario scenario, CostPlayer player,
                                    int seed, ThreadMXBean cpu, List<Map<String, Object>> rows) {
        int expected = 0;
        for (int i = 0; i < profile.warmups; i++) expected = player.decide(scenario);
        for (int repeat = 0; repeat < profile.repetitions; repeat++) {
            var row = new LinkedHashMap<String, Object>();
            rows.add(row);
            row.put("stage", scenario.stage()); row.put("policy", player.name());
            row.put("fixtureSeed", seed); row.put("repeat", repeat);
            row.put("publicObservation", scenario.observation()); row.put("status", "RUNNING");
            long beginCpu = cpu.getCurrentThreadCpuTime(), begin = System.nanoTime();
            try {
                var measured = player.measure(scenario, profile.workLimit, row);
                long wall = System.nanoTime() - begin, threadCpu = cpu.getCurrentThreadCpuTime() - beginCpu;
                row.put("wallNanos", wall); row.put("threadCpuNanos", threadCpu); row.put("decision", measured.decision());
                assertEquals(expected, measured.decision());
                assertEquals("NONE", measured.work().stopReason());
                assertTrue(measured.work().units() > 0 && measured.work().units() <= profile.workLimit);
                assertTrue("CPU clock must provide a nonnegative duration", threadCpu >= 0);
                assertTrue("CPU cost exceeds declared profile: " + row, threadCpu <= profile.cpuLimit);
                assertTrue("Wall safeguard exceeded", wall <= profile.wallLimit);
                CostChecks.bounds(profile, scenario, player, measured.work());
                CostChecks.legal(scenario, measured.decision());
                row.put("status", "PASS");
            } catch (RuntimeException | Error problem) {
                row.put("status", "FAIL"); row.put("failure", problem.toString());
                throw problem;
            } finally {
                row.putIfAbsent("wallNanos", System.nanoTime() - begin);
                row.putIfAbsent("threadCpuNanos", cpu.getCurrentThreadCpuTime() - beginCpu);
            }
        }
    }

    private Map<String, Object> report(CostProfile profile, List<Map<String, Object>> rows) {
        var report = new LinkedHashMap<String, Object>();
        report.put("scope", profile.scope()); report.put("profile", profile.document());
        report.put("handSize", size); report.put("profileSha256", profile.sha256());
        report.put("jvmArguments", ManagementFactory.getRuntimeMXBean().getInputArguments());
        report.put("environment", Map.of("java", System.getProperty("java.runtime.version"),
                "vm", System.getProperty("java.vm.name"), "os", System.getProperty("os.name"),
                "osVersion", System.getProperty("os.version"), "arch", System.getProperty("os.arch"),
                "processors", Runtime.getRuntime().availableProcessors(), "maximumHeapBytes", Runtime.getRuntime().maxMemory()));
        report.put("rows", rows); report.put("status", "RUNNING");
        return report;
    }

    private void writeReport(Path output, Map<String, Object> report) throws IOException {
        Files.createDirectories(output);
        try (var file = Files.newOutputStream(output.resolve("cost-hand-" + size + ".json"), StandardOpenOption.CREATE_NEW)) {
            JSON.writerWithDefaultPrettyPrinter().writeValue(file, report);
        }
    }

    private static String required(String key) {
        String value = System.getProperty(key, "");
        if (value.isBlank()) throw new IllegalArgumentException("Explicit " + key + " required");
        return value;
    }
}
