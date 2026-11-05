"""Publică offline dovezi J4 finalizate, fără ADB. Intrările sunt selectate explicit."""
import argparse
from html import escape
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import shutil
import tempfile
import xml.etree.ElementTree as ET

from whist_ui_replay import check, digest, export, replay


def read_json(path):
    return json.loads(Path(path).read_bytes())


def write_json(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def properties(path):
    fields = {}
    for line in path.read_text(encoding="iso-8859-1").splitlines():
        if not line or line.startswith(("#", "!")):
            continue
        key, value = line.split("=", 1)
        fields[key] = re.sub(r"\\u([\da-fA-F]{4})|\\(.)",
                             lambda m: chr(int(m[1], 16)) if m[1] else {"n": "\n", "t": "\t", "r": "\r"}.get(m[2], m[2]), value)
    return fields


def token(value):
    check(re.fullmatch(r"[a-z0-9][a-z0-9.-]{0,119}", value) is not None and ".." not in value, "Unsafe run or scope ID")
    return value


def native_result(path, method):
    root = ET.parse(path).getroot()
    cases = [t for t in root.findall("testcase") if t.get("name") == method]
    check(len(cases) == 1, "Native invocation missing or ambiguous")
    t = cases[0]
    verdict = next((status for tag, status in (("error", "ERROR"), ("failure", "FAIL"), ("skipped", "SKIP"))
                    if t.find(tag) is not None), "PASS")
    cp = next((p.get("value", "") for p in root.findall("properties/property") if p.get("name") == "java.class.path"), "")
    libs = {}
    for name in ("atf-core", "atf-reporting"):
        jars = [Path(j) for j in cp.split(os.pathsep) if Path(j).name.startswith(name + "-") and j.endswith(".jar")]
        jar = jars[0] if len(jars) == 1 else None
        libs[name] = {"coordinate": f"atf.api:{name}:{jar.parent.name}" if jar else None,
                      "classpathAtRun": str(jar) if jar else None, "sha256AtExecution": None,
                      "sha256AtOfflineReview": digest(jar) if jar and jar.is_file() else None}
    return {"class": t.get("classname"), "method": method, "invocations": 1, "status": verdict,
            "seconds": float(t.get("time", 0)), "originalXmlSha256": digest(path)}, libs


def build_bundle(spec, out):
    report, manifest, junit, browser, apk, sources = [Path(spec[k]) for k in ("html", "manifest", "junit", "browser", "apk", "sources")]
    flow = properties(manifest)
    check(flow["flow.appPackage"] == "ro.whistlab.app", "Separate Lab and original app namespaces")
    bound = read_json(browser)
    report_hash = digest(report)
    check(bound["htmlSha256"].lower() == report_hash, "Browser receipt belongs to other HTML")
    native, libs = native_result(junit, spec["method"])
    check(native["class"].split(".")[-1] == flow["flow.testClass"], "JUnit and manifest classes differ")
    provenance = read_json(apk)
    check(provenance["package"] == flow["flow.appPackage"] and provenance["device"] == provenance["apk"], "APK identity mismatch")
    checkpoints = [k[:-len(".status")] for k in flow if re.fullmatch(r"phase\.\d+\.step\.\d+\.status", k)]
    refs = [flow[key + ".screenshot"] for key in checkpoints if flow.get(key + ".screenshot")]
    distinct = set()
    for ref in refs:
        png = (manifest.parent / ref).resolve(strict=True)
        check(png.is_relative_to(manifest.parent.resolve()), "Screenshot outside evidence directory")
        distinct.add(digest(png))
    result = properties(Path(spec["result"])) if spec.get("result") else {}
    rules = None
    if result:
        rules = export(manifest.parent, spec["runId"], int(result.get("handsCompleted", 0)))
        write_json(out / "public-trace.json", rules)
        offline = replay(rules)
        check(result.get("difficulty") == spec["profile"]["difficulty"]
              and int(result["streakBonus"]) == spec["profile"]["seriesBonus"], "Pilot profile mismatch")
        scope_complete = len(rules["hands"]) == spec["expectedHands"]
        if rules["hands"]:
            check(all(len(h["exPost"]["bids"]) == spec["profile"]["seats"] for h in rules["hands"]), "Seat count mismatch")
    else:
        offline = {"status": "NOT_APPLICABLE"}
        scope_complete = len(checkpoints) == spec["expectedCheckpoints"]
    scores = list(map(int, re.findall(r"-?\d+", result.get("finalTotals", ""))))
    visual = spec.get("visualReview", "NOT_REVIEWED")
    status = {"native": native["status"], "journey": flow["flow.status"],
              "rulesAndScore": result.get("businessStatus", "NOT_APPLICABLE"), "offlineReplay": offline["status"],
              "publication": result.get("reportStatus", "PASS"), "structuralAudit": "PASS" if native["status"] == "PASS" else "NOT_ATTESTED",
              "browserAudit": bound["status"], "visualReview": visual, "matchVictory": "NOT_APPLICABLE",
              "leadersAfterCapturedHands": [i for i,v in enumerate(scores) if v == max(scores)]}
    accepted = (native["status"] == flow["flow.status"] == bound["status"] == status["publication"] == "PASS"
                and visual == "REVIEWED" and scope_complete
                and (not result or result.get("status") == status["rulesAndScore"] == offline["status"] == "PASS"))
    check(report.stat().st_size > 0, "Empty report")
    for source, name in [(report,"report.html"), (manifest,"comparisons.properties"), (browser,"browser-audit.json"),
                         (apk,"apk-provenance.json"), (sources,"source-hashes.json")]:
        shutil.copyfile(source, out / name)
        check(digest(out / name) == digest(source), "Archive copy mismatch")
    if result:
        shutil.copyfile(spec["result"], out / "game-result.properties")
    write_json(out / "native-junit.json", native)
    counts = {"junitInvocations": 1, "checkpoints": len(checkpoints),
              "assertions": sum(int(flow[k+".assertionCount"]) for k in checkpoints),
              "pngReferences": len(refs), "distinctPng": len(distinct)}
    if rules:
        counts.update({k: v for k,v in offline.items() if k != "status"})
    return {"schema":"j4.whist-ui-handoff/1", "owner":"J4 UI", "runId":token(spec["runId"]),
            "sourceRunId":spec["sourceRunId"], "scope":token(spec["scope"]), "requestedScope":spec["requestedScope"],
            "effectiveScope":spec["effectiveScope"], "scopeComplete":scope_complete, "profile":spec["profile"], "namespace":"whist-lab",
            "publicationKind":"OFFLINE_REPUBLICATION", "startedAt":spec.get("startedAt"), "endedAt":spec["endedAt"],
            "timingBasis":spec["timingBasis"], "newAndroidExecution":False,
            "package":provenance["package"], "version":provenance["version"], "versionCode":provenance["versionCode"],
            "scenario":flow["flow.name"], "verdict":status, "accepted":accepted, "executionFailure":flow["flow.failure"],
            "failureStage":result.get("failureStage") or ("UI" if flow["flow.status"] != "PASS" else None),
            "html":{"file":"report.html","bytes":report.stat().st_size,"sha256":report_hash},
            "build":{"sourceSnapshotSha256":digest(sources),"apkBuiltSha256":provenance["apk"],
                     "apkInstalledAtCaptureSha256":provenance["device"],"libraries":libs},
            "populations":counts,"limits":spec["limits"],
            "files":{p.name:digest(p) for p in sorted(out.iterdir())}}


def atomic_text(path, content):
    with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", dir=path.parent, delete=False) as f:
        temp = Path(f.name)
        f.write(content)
    try:
        os.replace(temp, path)
    finally:
        temp.unlink(missing_ok=True)


def unavailable(spec, out):
    status = spec["attemptStatus"]
    check(status in ("SKIP", "NOT_RUN", "ERROR", "FAIL") and spec.get("reason"), "Missing reason for an attempt without HTML")
    check(spec["package"] == "ro.whistlab.app", "Unknown application namespace")
    native = {"status":"NOT_RUN","invocations":0}
    if spec.get("junit"):
        native, _ = native_result(Path(spec["junit"]), spec["method"])
        check(native["status"] == status, "Status differs from native JUnit")
    elif status != "NOT_RUN":
        raise ValueError("Native outcome requires a JUnit receipt")
    write_json(out / "native-junit.json", native)
    return {"schema":"j4.whist-ui-handoff/1","owner":"J4 UI","namespace":"whist-lab","package":spec["package"],
            "runId":spec["runId"],"sourceRunId":spec["sourceRunId"],"scope":spec["scope"],"scenario":spec["scenario"],
            "requestedScope":spec["requestedScope"],"effectiveScope":spec["effectiveScope"],"scopeComplete":False,
            "profile":spec["profile"],"endedAt":spec["endedAt"],"startedAt":spec.get("startedAt"),
            "timingBasis":spec["timingBasis"],"failureStage":spec["failureStage"],"executionFailure":spec["reason"],
            "publicationKind":"OFFLINE_REPUBLICATION","newAndroidExecution":False,"accepted":False,"html":None,
            "verdict":{"native":native["status"],"journey":status,"publication":"NOT_RUN","browserAudit":"NOT_RUN",
                       "rulesAndScore":"NOT_RUN","visualReview":"NOT_REVIEWED","matchVictory":"NOT_APPLICABLE"},
            "populations":{"junitInvocations":native["invocations"],"checkpoints":0,"assertions":0,"pngReferences":0},
            "files":{"native-junit.json":digest(out / "native-junit.json")},"limits":spec["limits"]}


def publish_pointers(directory, record):
    directory.mkdir(parents=True, exist_ok=True)
    values = {}
    for key in ("attempt","accepted"):
        path = directory / f"latest-{key}.json"
        previous = read_json(path) if path.exists() else None
        if (key == "attempt" or record["accepted"]) and (not previous or
                (datetime.fromisoformat(record["endedAt"]), record["runId"]) >
                (datetime.fromisoformat(previous["endedAt"]), previous["runId"])):
            previous = record
        atomic_text(path, json.dumps(previous, ensure_ascii=False, indent=2) + "\n")
        link = escape(previous["htmlUri"] + "#test-view", quote=True) if previous and previous.get("htmlUri") else None
        shortcut = (f'<meta http-equiv="refresh" content="0;url={link}"><a href="{link}">Deschide raportul Extent</a>'
                    if link else '<p>Niciun HTML disponibil pentru acest verdict.</p>')
        atomic_text(directory / f"latest-{key}.html", '<!doctype html><meta charset="utf-8"><title>Whist Lab · Extent</title>' + shortcut)
        values[key] = previous
    lines = ["# Whist Lab · rapoarte Extent", "", "Raport acceptat pentru scopul declarat și ultima încercare finalizată.", ""]
    for key, title in (("accepted","Ultima acceptanță"),("attempt","Ultima încercare")):
        r = values[key]
        lines += [f"## {title}", ""]
        if r:
            link = f"[{r['scenario']}](<{r['htmlUri']}>)" if r.get("htmlUri") else r["scenario"] + " · fără HTML"
            lines += [f"{link} · {r['scope']} · {r['endedAt']}",
                      "",f"JUnit **{r['verdict']['native']}** · joc **{r['verdict']['journey']}** · publicare **{r['verdict']['publication']}**.",
                      "",f"[Metadate, populații și limite](<{r['metadataUri']}>)", ""]
        else:
            lines += ["Nu există o acceptanță în acest scop.", ""]
    atomic_text(directory / "LATEST.md", "\n".join(lines))


def publish(root, spec):
    scope, run = token(spec["scope"]), token(spec["runId"])
    end = datetime.fromisoformat(spec["endedAt"])
    check(end.tzinfo is not None, "Completion time needs a timezone")
    base = root.resolve() / "whist-lab"
    runs = base / "runs"
    runs.mkdir(parents=True, exist_ok=True)
    dest = runs / run
    with tempfile.TemporaryDirectory(dir=runs) as tmp:
        bundle = Path(tmp) / "bundle"
        bundle.mkdir()
        record = build_bundle(spec,bundle) if spec.get("html") else unavailable(spec,bundle)
        record.update(htmlUri=(dest/"report.html").as_uri() if record["html"] else None, metadataUri=(dest/"metadata.json").as_uri())
        lock = base / ".publish.lock"
        lock.touch(exist_ok=False)
        try:
            if dest.exists():
                old = read_json(dest / "metadata.json")
                check(old == record, "Immutable run ID already has different evidence or metadata")
                for name, expected in old["files"].items():
                    check(digest(dest/name) == expected, "Existing archive changed")
                record = old
            else:
                write_json(bundle / "metadata.json", record)
                bundle.rename(dest)
            publish_pointers(base / "scopes" / scope, record)
            publish_pointers(base, record)
        finally:
            lock.unlink()
    return record


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("descriptor", type=Path)
    parser.add_argument("--root", type=Path, required=True)
    args = parser.parse_args()
    print(publish(args.root, read_json(args.descriptor))["metadataUri"])
