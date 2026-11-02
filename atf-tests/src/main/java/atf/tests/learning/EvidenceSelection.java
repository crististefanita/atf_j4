package atf.tests.learning;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Selects whole measured invocations, retaining all non-PASS and every quality gate. */
public record EvidenceSelection(List<LearningRunReport.Input> inputs,Map<String,Object> scope,EvidencePolicy policy) {
    public EvidenceSelection { inputs=List.copyOf(inputs);scope=Collections.unmodifiableMap(new LinkedHashMap<>(scope));Objects.requireNonNull(policy); }
    public static EvidenceSelection choose(List<LearningRunReport.Input> population,EvidencePolicy policy){
        Objects.requireNonNull(policy);if(population.isEmpty())throw new IllegalArgumentException("Empty population");
        long comparisons=0,context=0;var ids=new HashSet<String>();
        for(var input:population){var r=input.receipt();LearningRunReport.validate(r);if(!ids.add(r.path("invocationId").asText()))throw new IllegalArgumentException("Repeated population invocation");for(var cp:r.path("checkpoints")
            ){comparisons=Math.addExact(comparisons,cp.path("comparisons").size());context=Math.addExact(context,cp.path("context").toString().length());}}
        boolean small=population.size()<=policy.maxDetailedInvocations()&&comparisons<=policy.maxDetailedComparisons()&&context<=policy.maxDetailedContextCharacters();
        boolean all=policy.detailLevel()==EvidencePolicy.DetailLevel.FULL||policy.detailLevel()==EvidencePolicy.DetailLevel.AUTO&&small;
        var included=new ArrayList<LearningRunReport.Input>();var counts=new HashMap<String,Integer>();var omitted=new ArrayList<String>();
        var selectedPassIds=new HashSet<String>();if(!all){var familyCounts=new HashMap<String,Integer>();for(var input:population.stream().sorted(Comparator.comparing(i->i.receipt().path("invocationId").asText())).toList()){var
            r=input.receipt();if(!policy.alwaysIncludeCollections().contains(r.path("parameters").path("collection").asText())&&r.path("status").asText().equals("PASS")&&!r.path("executionKind").asText().equals("NODE_COMPONENT_GATE")){String key=family(r);if(familyCounts.getOrDefault(key,0)<policy.passingExamplesPerFamily())
            {selectedPassIds.add(r.path("invocationId").asText());familyCounts.merge(key,1,Integer::sum);}}}}
        for(var input:population){
            var r=input.receipt();String id=r.path("invocationId").asText(),family=family(r);boolean mandatory=policy.alwaysIncludeCollections().contains(r.path("parameters").path("collection").asText())||!r.path("status").asText().equals("PASS")||r.path("executionKind").asText().equals("NODE_COMPONENT_GATE");
            if(all||mandatory||selectedPassIds.contains(id)){included.add(input);if(!mandatory)counts.merge(family,1,Integer::sum);}else omitted.add(id);
        }
        var scope=new LinkedHashMap<String,Object>();scope.put("requestedDetail",policy.detailLevel().name());scope.put("effectiveScope",omitted.isEmpty()
            ?"All measured invocations in the supplied collection":"Selected whole examples plus ALL non-PASS invocations, configured complete collections and the full-population quality gate; omitted games are NOT individually audited by this HTML");scope.put(
            "originalInvocations",population.size());scope.put("retainedInvocations",included.size());scope.put("omittedInvocations",omitted.size());scope.put("originalComparisons",comparisons);scope.put("originalContextCharacters",
            context);scope.put("selectionReason",
            all?"FULL requested or AUTO within all configured inclusive thresholds":"Representative selection at invocation boundaries; deterministic first IDs per family, not a best-result filter");scope.put("includedIds",
            included.stream().map(i->i.receipt().path("invocationId").asText()).toList());scope.put("omittedIds",List.copyOf(omitted));scope.put("policy",Map.of("detailLevel",policy.detailLevel().name(),"maxDetailedInvocations",
            policy.maxDetailedInvocations(),"maxDetailedComparisons",policy.maxDetailedComparisons(),"maxDetailedContextCharacters",policy.maxDetailedContextCharacters(),"passingExamplesPerFamily",policy.passingExamplesPerFamily(),
            "optionalContextBudgetCharacters",policy.optionalContextBudgetCharacters(),"rowsPerGroup",policy.rowsPerGroup(),"fieldsPerGroup",policy.fieldsPerGroup(),"optionalMaxItems",policy.optionalMaxItems(),"alwaysIncludeCollections",policy.alwaysIncludeCollections()));
        var uniqueDiagnostics=new LinkedHashSet<String>();for(var input:included){var hashes=input.receipt().path("sourceHashes");if(!hashes.isMissingNode())uniqueDiagnostics.add(hashes.toString());}long originalOptional=0,
            retainedOptional=0;var retainedDiagnostics=new ArrayList<String>();for(var diagnostic:uniqueDiagnostics){originalOptional=Math.addExact(originalOptional,diagnostic.length());if(policy.detailLevel()
            !=EvidencePolicy.DetailLevel.SUMMARY&&retainedDiagnostics.size()<policy.optionalMaxItems()&&diagnostic.length()<=policy.optionalContextBudgetCharacters()-retainedOptional){retainedDiagnostics.add(diagnostic);
            retainedOptional+=diagnostic.length();}}
        scope.put("optionalDiagnosticsOriginalRecords",uniqueDiagnostics.size());scope.put("optionalDiagnosticsRetainedRecords",retainedDiagnostics.size());scope.put("optionalDiagnosticsOmittedRecords",uniqueDiagnostics.size()
            -retainedDiagnostics.size());scope.put("optionalDiagnosticsOriginalCharacters",originalOptional);scope.put("optionalDiagnosticsRetainedCharacters",retainedOptional);scope.put("optionalDiagnosticsOmittedCharacters",
            originalOptional-retainedOptional);scope.put("optionalDiagnosticsReason",
            "SUMMARY omits optional policy hashes; otherwise repeated hashes are deduplicated and whole records use the shared cumulative budget. No assertion inputs or verdicts are optional.");scope.put(
            "optionalPolicyFingerprints",List.copyOf(retainedDiagnostics));
        long suppliedNanos=0,selectedNanos=0;for(var input:population)suppliedNanos=Math.addExact(suppliedNanos,input.receipt().path("durationNanos").asLong());for(var input:included)selectedNanos=Math.addExact(selectedNanos,input.receipt().path("durationNanos").asLong());
        scope.put("executionTiming",Map.of("suppliedCapturedIntervalsSumNanos",suppliedNanos,"retainedIntervalsSumNanos",selectedNanos,"definition","Sum of captured invocation intervals; concurrent intervals may overlap. Not full-run wall time; publication and other native test suites are excluded."));
        scope.put("presentationOrder","Supplied manifest order, preserving checkpoints within each invocation. Actual timestamps retained; concurrent workers are not one sequential UI journey.");
        return new EvidenceSelection(included,scope,policy);
    }
    private static String family(com.fasterxml.jackson.databind.JsonNode receipt){var p=receipt.path("parameters");return p.has("role")?p.path("role").asText()+" / "+p.path("opponent").asText():receipt.path("className").asText()
        +" / "+p.path("language").asText();}
}
