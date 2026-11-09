import crypto from 'node:crypto';
import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const VERSION = /^(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)(?:-[0-9A-Za-z.-]+)?$/;
const HASH = /^[0-9a-f]{64}$/;
const COMMIT = /^[0-9a-f]{40}$/;
const STATUSES = new Set(['PASS', 'FAIL', 'BLOCKED', 'NOT_RUN', 'NOT_APPLICABLE', 'NOT_IMPLEMENTED']);
const REQUIRED_ANDROID_COMPONENT_GROUPS = [
  'atf-validation', 'atf-test-android', 'atf-tests',
  'whist-lab-app-bidding', 'whist-lab-app-commands',
  'whist-lab-app-display', 'whist-lab-app-history'
];

// A small strict JSON reader keeps duplicate keys and trailing data from changing a receipt silently.
class StrictJsonReader {
  constructor(text) { this.text = text; this.at = 0; this.line = 1; this.lines = new Map(); }
  parse() {
    const value = this.value(''); this.space();
    if (this.at !== this.text.length) throw Error('JSON_TRAILING_DATA');
    return {value, lines: this.lines};
  }
  space() { while (this.at < this.text.length && /\s/.test(this.text[this.at])) this.advance(); }
  advance() { if (this.text[this.at++] === '\n') this.line++; }
  string() {
    const start = this.at, startLine = this.line;
    if (this.text[this.at] !== '"') throw Error('JSON_STRING');
    this.advance();
    while (this.at < this.text.length) {
      const char = this.text[this.at];
      if (char === '"') { this.advance(); return {value: JSON.parse(this.text.slice(start, this.at)), line: startLine}; }
      if (char === '\\') { this.advance(); if (this.at >= this.text.length) break; this.advance(); continue; }
      if (char < ' ') throw Error('JSON_CONTROL_CHARACTER');
      this.advance();
    }
    throw Error('JSON_UNTERMINATED_STRING');
  }
  value(pointer) {
    this.space(); const char = this.text[this.at];
    if (char === '{') {
      this.advance(); this.space(); const object = Object.create(null); const seen = new Set();
      if (this.text[this.at] === '}') { this.advance(); return object; }
      while (true) {
        this.space(); const key = this.string();
        if (seen.has(key.value)) throw Error('JSON_DUPLICATE_KEY:' + key.value);
        seen.add(key.value); this.space();
        if (this.text[this.at++] !== ':') throw Error('JSON_COLON');
        const child = pointer + '/' + key.value.replaceAll('~', '~0').replaceAll('/', '~1');
        this.lines.set(child, key.line); object[key.value] = this.value(child); this.space();
        const next = this.text[this.at];
        if (next === '}') { this.advance(); return object; }
        if (next !== ',') throw Error('JSON_OBJECT_SEPARATOR');
        this.advance();
      }
    }
    if (char === '[') {
      this.advance(); this.space(); const array = [];
      if (this.text[this.at] === ']') { this.advance(); return array; }
      while (true) {
        array.push(this.value(pointer + '/' + array.length)); this.space();
        const next = this.text[this.at];
        if (next === ']') { this.advance(); return array; }
        if (next !== ',') throw Error('JSON_ARRAY_SEPARATOR');
        this.advance();
      }
    }
    if (char === '"') return this.string().value;
    const rest = this.text.slice(this.at);
    for (const [token, value] of [['true', true], ['false', false], ['null', null]]) {
      if (rest.startsWith(token)) { for (let i = 0; i < token.length; i++) this.advance(); return value; }
    }
    const number = rest.match(/^-?(?:0|[1-9][0-9]*)(?:\.[0-9]+)?(?:[eE][+-]?[0-9]+)?/);
    if (number) { for (let i = 0; i < number[0].length; i++) this.advance(); return Number(number[0]); }
    throw Error('JSON_VALUE');
  }
}

const digest = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const inlinePngCount = html => [...html.matchAll(/data:image\/png;base64,/g)].length;
const posix = value => value.split(path.sep).join('/');
function hasCredentialField(value) {
  if (!value || typeof value !== 'object') return false;
  return Object.entries(value).some(([key, child]) =>
    /authorization|token|cookie|password|secret|credential/i.test(key) || hasCredentialField(child));
}
function safeRelative(value) {
  if (typeof value !== 'string' || value.length === 0 || value.includes('\\') || path.posix.isAbsolute(value)
      || value.split('/').some(part => part === '' || part === '.' || part === '..')) throw Error('UNSAFE_RELATIVE_PATH:' + value);
  return value;
}
function inside(root, relative) {
  const safe = safeRelative(relative), result = path.resolve(root, ...safe.split('/'));
  if (!result.startsWith(root + path.sep)) throw Error('PATH_ESCAPE:' + relative);
  return result;
}
function isWithin(parent, child) { return child === parent || child.startsWith(parent + path.sep); }
function lineCount(text) { return text.split('\n').length; }

async function strictJson(file) {
  if ((await fs.lstat(file)).isSymbolicLink()) throw Error('JSON_SYMLINK:' + file);
  const bytes = await fs.readFile(file); const text = bytes.toString('utf8');
  const parsed = new StrictJsonReader(text).parse();
  return {value: parsed.value, lines: parsed.lines, bytes, sha256: digest(bytes), text};
}
async function regularFile(root, relative) {
  const file = inside(root, relative), segments = safeRelative(relative).split('/');
  let current = root;
  for (let index = 0; index < segments.length; index++) {
    current = path.join(current, segments[index]);
    const entry = await fs.lstat(current);
    if (entry.isSymbolicLink() || (index < segments.length - 1 && !entry.isDirectory())
        || (index === segments.length - 1 && !entry.isFile())) throw Error('INVALID_FILE:' + relative);
  }
  const real = await fs.realpath(file);
  if (!isWithin(root, real)) throw Error('INVALID_FILE:' + relative);
  return file;
}
async function inventory(root) {
  const found = [];
  async function visit(directory) {
    for (const entry of await fs.readdir(directory, {withFileTypes: true})) {
      const file = path.join(directory, entry.name);
      if (entry.isSymbolicLink()) throw Error('SYMLINK:' + posix(path.relative(root, file)));
      if (entry.isDirectory()) await visit(file);
      else if (entry.isFile()) found.push(posix(path.relative(root, file)));
      else throw Error('NON_REGULAR_ENTRY:' + posix(path.relative(root, file)));
    }
  }
  await visit(root); return found.sort();
}
function addCheck(checks, code, pass, message, evidence = [], details = []) {
  const boundedDetails = details.length > 30
      ? [...details.slice(0, 30), `... încă ${details.length - 30} detalii omise; consultă manifestele citate.`]
      : details;
  checks.push({code, status: pass ? 'PASS' : 'BLOCKED', message, evidence, ...(boundedDetails.length ? {details: boundedDetails} : {})});
}
function summarizeListDiff(expected, actual, expectedLabel, actualLabel, limit = 30) {
  const expectedSet = new Set(expected), actualSet = new Set(actual);
  const differences = [
    ...expected.filter(value => !actualSet.has(value)).map(value => `${expectedLabel}-only: ${value}`),
    ...actual.filter(value => !expectedSet.has(value)).map(value => `${actualLabel}-only: ${value}`)
  ];
  return differences.length > limit
      ? [...differences.slice(0, limit), `... încă ${differences.length - limit} diferențe omise`]
      : differences;
}
function summarizeMapDiff(expected, actual, limit = 30) {
  const differences = [
    ...[...expected.keys()].filter(name => !actual.has(name)).map(name => `APK-source-only: ${name}`),
    ...[...actual.keys()].filter(name => !expected.has(name)).map(name => `component-source-only: ${name}`),
    ...[...expected.keys()].filter(name => actual.has(name) && actual.get(name) !== expected.get(name))
      .map(name => `SHA-256 mismatch: ${name} (APK=${expected.get(name)}, component=${actual.get(name)})`)
  ];
  return differences.length > limit
      ? [...differences.slice(0, limit), `... încă ${differences.length - limit} diferențe omise`]
      : differences;
}
function citation(file, sha256, jsonPointer = null, line = null) {
  return {path: file, sha256, ...(jsonPointer === null ? {} : {jsonPointer}), ...(line === null ? {} : {line})};
}

export async function evaluateRelease({projectRoot, candidateDir, sourceRoot, planFile, outputFile}) {
  const project = await fs.realpath(projectRoot), candidate = await fs.realpath(candidateDir), source = await fs.realpath(sourceRoot);
  const out = path.resolve(outputFile), outParent = await fs.realpath(path.dirname(out));
  const output = path.join(outParent, path.basename(out));
  if ([project, candidate, source].some(root => isWithin(root, output))) throw Error('OUTPUT_MUST_BE_OUTSIDE_INPUTS');
  if ((await fs.lstat(output).catch(error => error.code === 'ENOENT' ? null : Promise.reject(error)))?.isSymbolicLink()) throw Error('OUTPUT_SYMLINK');

  const checks = [];
  const policyPath = path.join(project, 'config/release-platform-rag-policy.json');
  const [manifestDoc, policyDoc, planDoc] = await Promise.all([
    strictJson(await regularFile(candidate, 'release-validation.json')),
    strictJson(await regularFile(project, 'config/release-platform-rag-policy.json')),
    strictJson(planFile)
  ]);
  const manifest = manifestDoc.value, policy = policyDoc.value, plan = planDoc.value;
  const evidence = (pointer, doc = manifestDoc, relative = 'release-validation.json') =>
    [citation(relative, doc.sha256, pointer, doc.lines.get(pointer) ?? 1)];

  const policyContract = policy.schemaVersion === 1
      && policy.ragEvaluator?.entrypoint === 'atf-tests/tools/evaluate-release-rag.mjs'
      && policy.ragEvaluator?.onlyHumanReviewVerdict === 'READY_FOR_HUMAN_REVIEW'
      && policy.ragEvaluator?.doesNotPerformPromotionOrPublication === true
      && policy.ragEvaluator?.outputMustBeOutsideInputs === true
      && policy.ragEvaluator?.publicReadmeSurface?.path === 'README.md'
      && policy.ragEvaluator?.publicReadmeSurface?.forbidRawSha256Digest === true
      && Array.isArray(policy.ragEvaluator?.requiredComponentGroupsByPlatform?.android)
      && REQUIRED_ANDROID_COMPONENT_GROUPS.every(name => policy.ragEvaluator.requiredComponentGroupsByPlatform.android.includes(name))
      && Array.isArray(policy.ragEvaluator?.platformGates?.androidBeforeHumanReview)
      && policy.ragEvaluator.platformGates.androidBeforeHumanReview.includes('signing')
      && Array.isArray(policy.ragEvaluator?.platformGates?.androidAfterPublication)
      && policy.ragEvaluator.platformGates.androidAfterPublication.includes('distributionDownload')
      && policy.ragEvaluator?.componentSourceBinding?.sourceSetMustEqualReleaseInputManifest === true
      && policy.ragEvaluator?.componentSourceBinding?.eachExtentMustMatchExecutionReceipt === true
      && policy.ragEvaluator?.componentSourceBinding?.allReceiptsMustBindSameSourceFingerprint === true
      && policy.ragEvaluator?.componentSourceBinding?.provenanceReceipt === 'reports/component/source-provenance.json'
      && policy.ragEvaluator?.e2e?.notRunNeverMeansPass === true
      && policy.ragEvaluator?.e2e?.executionReceiptPattern === 'reports/e2e/{suite}/execution.json'
      && policy.ragEvaluator?.e2e?.receiptMustMatchManifestReleaseAndSource === true
      && policy.ragEvaluator?.e2e?.receiptMustMatchExtentHashAndOutcomes === true
      && policy.ragEvaluator?.e2e?.inlinePngCountMustMatchExtent === true
      && policy.ragEvaluator?.e2e?.reportPathsUniqueAcrossScopes === true
      && policy.reportingContract?.owner === 'atf-reporting'
      && policy.reportingContract?.renderer === 'Extent'
      && policy.reportingContract?.oneCanonicalHtmlPerExecutedSuiteSelection === true
      && policy.reportingContract?.componentReportPathPattern === 'reports/component/{group}/component-report.html'
      && policy.reportingContract?.e2eReportPathPattern === 'reports/e2e/{suite}/report.html'
      && policy.reportingContract?.eachSuiteOwnsUniqueReportPath === true
      && policy.reportingContract?.everyExecutedSuiteRequiresInlinePngEvidence === true
      && policy.reportingContract?.componentAndE2eScopesRemainDistinct === true
      && policy.reportingContract?.exactJUnitIdentityAndOutcomeParity === true
      && policy.reportingContract?.junitXmlRole === 'temporary-independent-oracle'
      && policy.reportingContract?.duplicateJUnitXmlInFinalRelease === false
      && policy.reportingContract?.capturedImagesMustBeInlineBase64 === true
      && policy.reportingContract?.reportBindsQualifiedSourceSnapshot === true
      && policy.rules?.ragMayExecuteCommands === false && policy.rules?.ragMayBuildOrSign === false
      && policy.rules?.ragMayPublish === false && policy.candidateLifecycle?.ragMayAdvanceLifecycle === false;
  addCheck(checks, 'RAG_POLICY', policyContract, policyContract
      ? 'Politica activă limitează evaluatorul la citire și la revizia umană.' : 'Politica RAG lipsește sau permite o acțiune în afara contractului read-only.',
      [citation('config/release-platform-rag-policy.json', policyDoc.sha256, '/ragEvaluator', policyDoc.lines.get('/ragEvaluator') ?? 1)]);

  const version = plan.releaseVersion;
  const validPlan = plan.schemaVersion === 1 && VERSION.test(version ?? '')
      && ['LOCAL', 'TEST', 'PRODUCTION'].includes(plan.channel)
      && ['maintenanceOrRefactor', 'behaviorChange', 'structuralOrReleaseChange'].includes(plan.changeRisk)
      && Array.isArray(plan.declaredPlatforms) && plan.declaredPlatforms.length === 1 && plan.declaredPlatforms[0] === 'android'
      && Array.isArray(plan.componentGroups) && plan.componentGroups.length > 0
      && plan.componentGroups.every(name => typeof name === 'string' && name.trim().length > 0)
      && new Set(plan.componentGroups).size === plan.componentGroups.length
      && typeof plan.userFlowChanged === 'boolean'
      && plan.e2e && ['PASS', 'NOT_RUN', 'NOT_APPLICABLE'].includes(plan.e2e.status)
      && (plan.e2e.status === 'PASS'
        ? Array.isArray(plan.e2e.groups) && plan.e2e.groups.length > 0 && plan.e2e.groups.every(name => typeof name === 'string')
        : typeof plan.e2e.rationale === 'string' && plan.e2e.rationale.trim().length >= 12);
  addCheck(checks, 'PLAN_SCHEMA', validPlan, validPlan ? 'Planul fixează versiunea, platforma, riscul, componentele și decizia E2E.' : 'Plan absent, incomplet sau cu enum invalid.',
      [citation(path.relative(project, planFile), planDoc.sha256, '/schemaVersion', planDoc.lines.get('/schemaVersion') ?? 1)]);

  const versionMatches = validPlan && manifest.release?.versionName === version
      && path.basename(candidate) === version;
  addCheck(checks, 'RELEASE_IDENTITY', versionMatches, versionMatches ? 'Planul, manifestul și directorul indică aceeași versiune.' : 'Versiunea candidatului nu coincide în plan, manifest și numele dosarului.', evidence('/release/versionName'));

  const manifestPlatforms = manifest.release?.platforms ?? (manifest.release?.artifactKind === 'apk'
      || (manifest.release?.apkName && policy.rules?.legacyApkManifestPlatforms?.includes('android')) ? ['android'] : []);
  const selectedPlatforms = Array.isArray(plan.declaredPlatforms) ? plan.declaredPlatforms : [];
  const declaredPlatforms = Array.isArray(manifestPlatforms) ? manifestPlatforms : [];
  const platformMatches = validPlan && JSON.stringify([...declaredPlatforms].sort()) === JSON.stringify([...selectedPlatforms].sort());
  addCheck(checks, 'PLATFORM_SCOPE', platformMatches, platformMatches ? 'Platforma declarată este Android și corespunde manifestului.' : 'Planul și manifestul declară platforme diferite.', evidence('/release'));

  const qualificationPass = manifest.qualification?.state === 'PASS';
  addCheck(checks, 'QUALIFICATION_STATE', qualificationPass,
      qualificationPass ? 'Manifestul declară starea PASS pentru calificarea înregistrată.' : 'Calificarea manifestului nu este PASS.', evidence('/qualification/state'));

  const payloadEntries = manifest.integrity?.files;
  let payloadOkay = Array.isArray(payloadEntries) && payloadEntries.length > 0, payloadProblem = null;
  const declared = new Map();
  if (payloadOkay) {
    try {
      for (const item of payloadEntries) {
        const relative = safeRelative(item.path);
        if (relative === 'release-validation.json' || declared.has(relative) || !HASH.test(item.sha256 ?? '')
            || !Number.isSafeInteger(item.bytes) || item.bytes < 0) throw Error('INVALID_RECEIPT_ENTRY:' + relative);
        declared.set(relative, item);
        const file = await regularFile(candidate, relative), bytes = await fs.readFile(file);
        if (bytes.length !== item.bytes || digest(bytes) !== item.sha256) throw Error('PAYLOAD_MISMATCH:' + relative);
      }
      const actual = await inventory(candidate); actual.splice(actual.indexOf('release-validation.json'), 1);
      const expected = [...declared.keys()].sort();
      payloadOkay = actual.length === declared.size && actual.every((name, index) => name === expected[index]);
      if (!payloadOkay) payloadProblem = summarizeListDiff(expected, actual, 'expected', 'actual');
    } catch (error) { payloadOkay = false; payloadProblem = [error.message]; }
  }
  if (!Array.isArray(payloadEntries) || payloadEntries.length === 0) payloadProblem = ['integrity.files lipsește sau este gol.'];
  addCheck(checks, 'PAYLOAD_INTEGRITY', payloadOkay, payloadOkay ? 'Inventarul candidatului și SHA-256 corespund receipt-ului.' : 'Receipt-ul nu descrie exact payload-ul existent.', evidence('/integrity'), payloadProblem ?? []);

  let sourceOkay = false, inputDoc, inputRel = manifest.source?.inputManifest;
  const sourceProblems = [];
  try {
    const inputFile = await regularFile(candidate, inputRel); inputDoc = await strictJson(inputFile);
    const rows = inputDoc.value.files;
    if (!Array.isArray(rows) || rows.length === 0) sourceProblems.push('Manifestul de intrări nu conține un tablou files nevid.');
    const sortedRows = [...rows].sort((left, right) => left.path < right.path ? -1 : left.path > right.path ? 1 : 0);
    const ordinalOrder = rows.every((row, index) => row.path === sortedRows[index].path);
    if (!ordinalOrder) sourceProblems.push(`Manifestul de intrări nu este în ordine ordinală; prima abatere este la indexul ${rows.findIndex((row, index) => row.path !== sortedRows[index]?.path)}.`);
    const fingerprint = digest(Buffer.from(sortedRows.map(row => `${safeRelative(row.path)}\t${row.sha256}\n`).join(''), 'utf8'));
    const sourceCommit = manifest.source?.sourceCommit;
    if (manifest.source?.fingerprintFormat !== 'SHA-256 of UTF-8 ordinal-sorted path TAB sha256 LF records') sourceProblems.push('Formatul amprentei din release-validation.json nu este cel cerut.');
    if (!COMMIT.test(sourceCommit ?? '') || inputDoc.value.sourceCommit !== sourceCommit) sourceProblems.push('Commitul din manifestul de surse lipsește, este invalid sau diferă între receipts.');
    if (!HASH.test(manifest.source?.workingTreeFingerprint ?? '') || fingerprint !== inputDoc.value.fingerprint
        || fingerprint !== manifest.source.workingTreeFingerprint) sourceProblems.push('Amprenta calculată nu coincide cu manifestul de intrări și cu amprenta release-ului.');
    const seen = new Set();
    for (const row of Array.isArray(rows) ? rows : []) {
      let relative;
      try { relative = safeRelative(row.path); }
      catch (error) { sourceProblems.push(error.message); continue; }
      if (seen.has(relative)) { sourceProblems.push(`Intrare duplicată în manifestul sursă: ${relative}`); continue; }
      seen.add(relative);
      if (!HASH.test(row.sha256 ?? '')) { sourceProblems.push(`SHA-256 invalid în manifestul sursă: ${relative}`); continue; }
      try {
        const file = await regularFile(source, relative), actualHash = digest(await fs.readFile(file));
        if (actualHash !== row.sha256) sourceProblems.push(`Hash diferit pentru ${relative}: manifest=${row.sha256}, sursă=${actualHash}`);
      } catch (error) { sourceProblems.push(`Sursă lipsă sau inaccesibilă pentru ${relative}: ${error.message}`); }
    }
    sourceOkay = sourceProblems.length === 0;
  } catch (error) { sourceOkay = false; sourceProblems.push(`Manifestul sursă nu poate fi citit: ${error.message}`); }
  addCheck(checks, 'SOURCE_SNAPSHOT', sourceOkay,
      sourceOkay ? 'Commitul, manifestul de intrări, amprenta și fișierele sursă corespund.' : 'Snapshot-ul sursă nu poate fi reprodus din manifestul fixat.',
      [...evidence('/source'), ...(inputDoc ? [citation(posix(path.relative(candidate, path.join(candidate, inputRel))), inputDoc.sha256, '/fingerprint', inputDoc.lines.get('/fingerprint') ?? 1)] : [])], sourceProblems);

  const readmePolicy = policy.ragEvaluator?.publicReadmeSurface;
  let readmeOkay = false;
  const readmeProblems = [];
  const readmeEvidence = [];
  try {
    const relative = safeRelative(readmePolicy?.path);
    if (!Array.isArray(inputDoc?.value?.files) || !inputDoc.value.files.some(row => row.path === relative)) {
      readmeProblems.push('README-ul principal lipsește din manifestul sursei fixate.');
    }
    const file = await regularFile(source, relative);
    const bytes = await fs.readFile(file);
    const text = bytes.toString('utf8');
    const match = /\b[0-9a-f]{64}\b/i.exec(text);
    if (match) readmeProblems.push(`README-ul principal expune un digest brut SHA-256 la linia ${lineCount(text.slice(0, match.index))}.`);
    readmeOkay = readmeProblems.length === 0;
    readmeEvidence.push(citation(relative, digest(bytes), null, 1));
  } catch (error) {
    readmeProblems.push(`README-ul principal nu poate fi verificat: ${error.message}`);
  }
  addCheck(checks, 'MAIN_README_SURFACE', readmeOkay,
      readmeOkay ? 'README-ul principal este în snapshot și nu expune un digest APK brut.' : 'README-ul principal lipsește din snapshot sau publică un digest APK brut.',
      readmeEvidence, readmeProblems);

  const release = manifest.release ?? {}, apkRelative = release.apkRelativePath ?? release.apkName;
  let apkOkay = false;
  try {
    const apk = await regularFile(candidate, apkRelative), bytes = await fs.readFile(apk);
    const apkHash = digest(bytes), sidecarPath = `${apkRelative}.sha256`, sidecar = await fs.readFile(await regularFile(candidate, sidecarPath), 'utf8');
    apkOkay = bytes.length === release.apkBytes && apkHash === release.apkSha256 && sidecar.trim() === `${apkHash}  ${path.posix.basename(apkRelative)}`;
  } catch { apkOkay = false; }
  addCheck(checks, 'APK_IDENTITY', apkOkay, apkOkay ? 'APK-ul, dimensiunea și sidecar-ul SHA-256 coincid.' : 'APK-ul sau sidecar-ul SHA-256 lipsește ori diferă.', evidence('/release/apkSha256'));

  const certValid = HASH.test(release.certificateSha256 ?? '') && typeof release.signingCertificate === 'string'
      && release.signingCertificate.trim().length > 0;
  const channelOkay = certValid && (plan.channel !== 'PRODUCTION' || release.productionManagedKey === true);
  addCheck(checks, 'SIGNING_CHANNEL', channelOkay, channelOkay ? 'Certificatul este identificat și compatibil cu profilul canalului.' : 'Canalul cere un certificat identificat și, pentru producție, o cheie gestionată.', evidence('/release'));

  const platform = manifest.qualification?.platforms?.android;
  const published = /^PUBLISHED/.test(manifest.publication?.state ?? '');
  const requiredPlatformGates = [...(policy.ragEvaluator?.platformGates?.androidBeforeHumanReview ?? []),
    ...(published ? policy.ragEvaluator?.platformGates?.androidAfterPublication ?? [] : [])];
  let platformGatesOkay = Boolean(platform?.implemented === true && platform.artifactKind === 'apk' && requiredPlatformGates.length > 0);
  const platformGateEvidence = [], platformGateProblems = [];
  if (platform?.implemented !== true) platformGateProblems.push('qualification.platforms.android.implemented nu este true.');
  if (platform?.artifactKind !== 'apk') platformGateProblems.push('qualification.platforms.android.artifactKind nu este apk.');
  if (requiredPlatformGates.length === 0) platformGateProblems.push('Politica nu a furnizat porți Android pentru etapa evaluată.');
  for (const name of requiredPlatformGates) {
    const gate = platform?.gates?.[name]; let okay = gate?.status === 'PASS' && typeof gate.receipt === 'string';
    let receiptDoc;
    if (okay) {
      try {
        const receiptFile = await regularFile(candidate, gate.receipt), bytes = await fs.readFile(receiptFile);
        const payload = declared.get(gate.receipt);
        okay = Boolean(payload && payload.sha256 === digest(bytes));
        if (name === 'androidComponents') {
          receiptDoc = await strictJson(receiptFile);
          okay = okay && receiptDoc.value.kind === 'live-source-components' && receiptDoc.value.status === 'PASS'
              && receiptDoc.value.sourceStable === true;
        } else if (name === 'apkBuild') {
          const log = bytes.toString('utf8'); okay = okay && /BUILD SUCCESSFUL/.test(log) && !/BUILD FAILED/.test(log);
        } else if (name === 'apkIdentity' || name === 'signing') {
          receiptDoc = await strictJson(receiptFile);
          if (name === 'apkIdentity') {
            const identity = receiptDoc.value;
            okay = okay && (identity.result === 'PASS' || identity.status === 'PASS')
                && (identity.applicationId ?? identity.package) === release.applicationId
                && identity.versionName === release.versionName
                && identity.versionCode === release.versionCode
                && (identity.apkSha256 ?? identity.candidateApkSha256) === release.apkSha256
                && (identity.apkBytes ?? identity.installedApkBytes) === release.apkBytes;
          } else {
            const signing = receiptDoc.value;
            okay = okay && signing.result === 'PASS' && signing.certificateSha256 === release.certificateSha256
                && signing.upgradeCompatibility === 'PASS';
          }
        } else if (name === 'distributionDownload') {
          receiptDoc = await strictJson(receiptFile);
          const publication = manifest.publication ?? {};
          if (publication.accessMode === 'AUTHENTICATED' && publication.state === 'PUBLISHED') {
            const download = receiptDoc.value.authenticatedDownload ?? {};
            const repository = publication.repository ?? '';
            const expectedUrl = `https://github.com/${repository}/releases/download/v${release.versionName}/${release.apkName}`;
            okay = okay && /^[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+$/.test(repository)
                && download.url === expectedUrl && download.authenticated === true
                && download.authMode === 'GITHUB_ACCOUNT' && download.httpStatus === 200
                && download.bytes === release.apkBytes && download.sha256 === release.apkSha256
                && download.zipCrc === 'PASS' && download.channel === 'GitHub API authenticated'
                && !hasCredentialField(download);
          } else if (publication.accessMode === 'ANONYMOUS' && publication.state === 'PUBLISHED_SITE') {
            const download = receiptDoc.value.publicDownload ?? {};
            okay = okay && typeof download.url === 'string' && download.url.startsWith('https://')
                && download.httpStatus === 200 && download.pageStatus === 200 && download.checksumStatus === 200
                && download.bytes === release.apkBytes && download.sha256 === release.apkSha256
                && /anonymous HTTPS GET without cookies or authorization/i.test(download.channel ?? '');
          } else {
            okay = false;
          }
        }
      } catch { okay = false; }
    }
    if (!okay) {
      platformGatesOkay = false;
      platformGateProblems.push(`${name}: receipt absent, neinclus în payload sau invalid pentru regula porții.`);
    }
    const pointer = `/qualification/platforms/android/gates/${name}`;
    platformGateEvidence.push(...evidence(pointer));
    if (gate?.receipt && declared.has(gate.receipt)) platformGateEvidence.push(citation(gate.receipt, declared.get(gate.receipt).sha256,
        receiptDoc ? '/' : null, receiptDoc ? 1 : null));
  }
  addCheck(checks, 'PLATFORM_GATES', platformGatesOkay,
      platformGatesOkay ? `Porțile Android necesare etapei ${published ? 'publicate' : 'pre-publicare'} au receipt-uri valide și hash-uri legate de payload.`
        : 'Lipsește o poartă Android obligatorie ori receipt-ul ei nu dovedește statusul, identitatea, semnarea, compatibilitatea sau accesul de distribuție declarat.',
      platformGateEvidence, platformGateProblems);

  const groups = manifest.qualification?.auditedGroups ?? {}, groupResults = [];
  const selectedGroups = validPlan ? plan.componentGroups : [];
  const requiredComponentGroups = policy.ragEvaluator?.requiredComponentGroupsByPlatform?.android ?? REQUIRED_ANDROID_COMPONENT_GROUPS;
  let componentsOkay = requiredComponentGroups.every(name => selectedGroups.includes(name));
  const reporting = policy.reportingContract ?? {};
  const selectedReportPaths = new Set();
  const componentProblems = requiredComponentGroups.filter(name => !selectedGroups.includes(name))
      .map(name => `Grup obligatoriu neselectat: ${name}`);
  for (const name of selectedGroups) {
    const group = groups[name];
    const expectedReport = reporting.componentReportPathPattern?.replace('{group}', name);
    const pathUnique = typeof group?.report === 'string' && !selectedReportPaths.has(group.report);
    if (typeof group?.report === 'string') selectedReportPaths.add(group.report);
    let okay = Boolean(group && group.report === expectedReport && pathUnique && Number.isSafeInteger(group.tests) && group.tests > 0
        && group.failures === 0 && group.errors === 0 && group.skipped === 0
        && typeof group.report === 'string' && typeof group.protocol === 'string' && group.protocol.includes('COMPONENT'));
    if (okay) {
      try {
        const reportBytes = await fs.readFile(await regularFile(candidate, group.report));
        const payload = declared.get(group.report);
        const html = reportBytes.toString('utf8');
        okay = Boolean(payload && digest(reportBytes) === payload.sha256
            && /<html\b/i.test(html.slice(0, 8192)) && inlinePngCount(html) > 0);
      } catch { okay = false; }
    }
    if (!okay) {
      componentsOkay = false;
      componentProblems.push(`${name}: grup, cale Extent canonică/unică sau rezultate invalide (așteptat=${expectedReport ?? 'absent'}, raport=${group?.report ?? 'absent'}, teste=${group?.tests ?? 'absent'}, failures=${group?.failures ?? 'absent'}, errors=${group?.errors ?? 'absent'}, skipped=${group?.skipped ?? 'absent'}).`);
    }
    groupResults.push({name, status: okay ? 'PASS' : 'BLOCKED', invocations: group?.tests ?? null,
      evidence: [citation('release-validation.json', manifestDoc.sha256,
          `/qualification/auditedGroups/${name}`, manifestDoc.lines.get(`/qualification/auditedGroups/${name}`) ?? 1),
        ...(group?.report && declared.has(group.report) ? [citation(group.report, declared.get(group.report).sha256)] : [])]});
  }
  addCheck(checks, 'COMPONENT_EXTENT', componentsOkay,
      componentsOkay ? 'Grupurile obligatorii ale aplicației și guvernanței, plus selecțiile suplimentare, au Extent canonic și zero rezultate nepromovate.'
        : 'Lipsește un grup obligatoriu de componentă; sau un grup este duplicat, gol, nepromovat ori fără Extent în payload.',
      groupResults.flatMap(item => item.evidence), componentProblems);

  const binding = policy.ragEvaluator?.componentSourceBinding ?? {};
  let componentSourceBound = false, componentInputsDoc, componentRunDoc, componentProvenanceDoc;
  const bindingProblems = [];
  try {
    const inputRelative = binding.inputManifest, runRelative = binding.runReceipt;
    componentInputsDoc = await strictJson(await regularFile(candidate, inputRelative));
    componentRunDoc = await strictJson(await regularFile(candidate, runRelative));
    componentProvenanceDoc = await strictJson(await regularFile(candidate, binding.provenanceReceipt));
    const releaseRows = inputDoc?.value?.files ?? [], componentRows = componentInputsDoc.value;
    const toMap = rows => {
      if (!Array.isArray(rows)) throw Error('INPUT_LIST_REQUIRED');
      const result = new Map();
      for (const row of rows) {
        const name = safeRelative(row.path);
        if (!HASH.test(row.sha256 ?? '') || result.has(name)) throw Error('INPUT_ROW_INVALID:' + name);
        result.set(name, row.sha256);
      }
      return result;
    };
    const releaseSet = toMap(releaseRows), componentSet = toMap(componentRows);
    const sameSourceSet = releaseSet.size > 0 && releaseSet.size === componentSet.size
        && [...releaseSet].every(([name, hash]) => componentSet.get(name) === hash);
    if (!sameSourceSet) bindingProblems.push(...summarizeMapDiff(releaseSet, componentSet));
    const sourceFingerprint = componentRunDoc.value.sourceFingerprint;
    const rootRunOkay = componentRunDoc.value.kind === 'live-source-components'
        && componentRunDoc.value.status === 'PASS' && componentRunDoc.value.sourceStable === true
        && sourceFingerprint === componentInputsDoc.sha256 && componentRunDoc.value.inputCount === componentSet.size
        && componentRunDoc.value.inputManifest === path.posix.basename(inputRelative);
    if (!rootRunOkay) bindingProblems.push('Receipt-ul rădăcină nu este PASS/stabil sau nu se leagă de hash-ul manifestului și numărul de intrări.');
    const provenanceOkay = componentProvenanceDoc.value.kind === 'live-source-components'
        && componentProvenanceDoc.value.sourceFingerprint === sourceFingerprint
        && componentProvenanceDoc.value.inputCount === componentSet.size
        && componentProvenanceDoc.value.inputManifest === path.posix.basename(inputRelative)
        && componentRunDoc.value.provenanceReceiptSha256 === componentProvenanceDoc.sha256;
    if (!provenanceOkay) bindingProblems.push('Receipt-ul de proveniență nu se leagă de manifestul componentei, cardinalitate și rularea agregată.');
    let selectedReceiptsOkay = true;
    for (const name of selectedGroups) {
      const group = groups[name], relative = binding.executionReceiptPattern.replace('{group}', name);
      const executionDoc = await strictJson(await regularFile(candidate, relative)), execution = executionDoc.value;
      const reportFile = await regularFile(candidate, group.report), reportBytes = await fs.readFile(reportFile);
      const receipt = componentRunDoc.value.results?.find(row => row.module === name);
      const rootReceipt = receipt?.receipt;
      const countsPass = execution.outcomes?.PASS === group.tests
          && (execution.outcomes?.FAIL ?? 0) === 0 && (execution.outcomes?.ERROR ?? 0) === 0 && (execution.outcomes?.SKIP ?? 0) === 0;
      const receiptPass = execution.kind === 'live-source-components' && execution.apkQualification === false
          && execution.tests === group.tests && countsPass
          && Array.isArray(execution.coverageErrors) && execution.coverageErrors.length === 0
          && Array.isArray(execution.containerFailures) && execution.containerFailures.length === 0
          && execution.provenanceSha256 === componentProvenanceDoc.sha256
          && execution.reportSha256 === digest(reportBytes)
          && receipt?.exitCode === 0 && rootReceipt?.reportSha256 === execution.reportSha256
          && rootReceipt?.provenanceSha256 === componentProvenanceDoc.sha256;
      if (!receiptPass) {
        selectedReceiptsOkay = false;
        bindingProblems.push(`${name}: receipt-ul execuției, numărătoarea, provenance-ul sau hash-ul Extent nu corespunde rulării rădăcină.`);
      }
    }
    componentSourceBound = sameSourceSet && rootRunOkay && provenanceOkay && selectedReceiptsOkay;
  } catch (error) { componentSourceBound = false; bindingProblems.push(`Nu s-a putut verifica legarea surselor componentelor: ${error.message}`); }
  addCheck(checks, 'COMPONENT_SOURCE_BINDING', componentSourceBound,
      componentSourceBound ? 'Listele sursă pentru APK și componente sunt identice; fiecare Extent este legat de receipt-ul execuției și de aceeași amprentă.'
        : 'Receiptele componentei lipsesc, sunt neconcordante cu APK-ul sau nu sunt legate de aceeași amprentă de sursă.',
      [...evidence('/source'),
        ...(componentInputsDoc ? [citation(binding.inputManifest ?? 'reports/component/source-inputs.json', componentInputsDoc.sha256, '', 1)] : []),
        ...(componentRunDoc ? [citation(binding.runReceipt ?? 'reports/component/source-component-run.json', componentRunDoc.sha256, '/sourceFingerprint', componentRunDoc.lines.get('/sourceFingerprint') ?? 1)] : []),
        ...(componentProvenanceDoc ? [citation(binding.provenanceReceipt ?? 'reports/component/source-provenance.json', componentProvenanceDoc.sha256, '/sourceFingerprint', componentProvenanceDoc.lines.get('/sourceFingerprint') ?? 1)] : [])], bindingProblems);

  const canonical = manifest.evidenceRetention?.canonicalReport;
  const canonicalHash = declared.get(canonical)?.sha256;
  const canonicalOkay = typeof canonical === 'string' && selectedGroups.some(name => groups[name]?.report === canonical)
      && HASH.test(manifest.evidenceRetention?.canonicalReportSha256 ?? '')
      && canonicalHash === manifest.evidenceRetention.canonicalReportSha256;
  addCheck(checks, 'CANONICAL_EXTENT', canonicalOkay,
      canonicalOkay ? 'Raportul canonic este unul dintre grupurile Extent selectate și hash-ul său corespunde.'
        : 'Raportul canonic lipsește, nu este selectat sau hash-ul declarat diferă.',
      [...evidence('/evidenceRetention'), ...(canonicalOkay ? [citation(canonical, canonicalHash)] : [])]);

  const e2e = plan.e2e ?? {};
  const e2eBinding = policy.ragEvaluator?.e2e ?? {};
  let e2eReceiptsOkay = e2e.status !== 'PASS' || Array.isArray(e2e.groups);
  const e2eProblems = [], e2eEvidence = [];
  const e2eReportPaths = new Set();
  if (e2eReceiptsOkay && e2e.status === 'PASS') {
    for (const name of e2e.groups) {
      const group = manifest.qualification?.e2eGroups?.[name];
      if (!group || !Number.isSafeInteger(group.tests) || group.tests < 1 || group.failures !== 0
          || group.errors !== 0 || group.skipped !== 0 || !String(group.protocol ?? '').includes('E2E')) {
        e2eReceiptsOkay = false; e2eProblems.push(`${name}: grupul E2E lipsește sau numărătoarea/protocolul nu este promovat.`); continue;
      }
      const reportPath = group.report, receiptPath = group.executionReceipt;
      const reportDirectory = typeof reportPath === 'string' ? path.posix.dirname(reportPath) : '';
      const suiteDirectory = path.posix.basename(reportDirectory);
      const expectedReport = reporting.e2eReportPathPattern?.replace('{suite}', suiteDirectory);
      const expectedReceipt = e2eBinding.executionReceiptPattern?.replace('{suite}', suiteDirectory);
      if (reportPath !== expectedReport || !reportPath || selectedReportPaths.has(reportPath) || e2eReportPaths.has(reportPath)
          || receiptPath !== expectedReceipt || !receiptPath || receiptPath === reportPath) {
        e2eReceiptsOkay = false;
        e2eProblems.push(`${name}: raport/receipt lipsă, necanonic sau cu o cale reutilizată (așteptat raport=${expectedReport}, receipt=${expectedReceipt}; observat raport=${reportPath ?? 'absent'}, receipt=${receiptPath ?? 'absent'}).`);
        continue;
      }
      e2eReportPaths.add(reportPath);
      try {
        const reportFile = await regularFile(candidate, reportPath), reportBytes = await fs.readFile(reportFile);
        const html = reportBytes.toString('utf8'), reportHash = digest(reportBytes), imageCount = inlinePngCount(html);
        const reportItem = declared.get(reportPath), receiptItem = declared.get(receiptPath);
        const receiptDoc = await strictJson(await regularFile(candidate, receiptPath)), receipt = receiptDoc.value;
        const countsMatch = receipt.tests === group.tests && receipt.failures === group.failures
            && receipt.errors === group.errors && receipt.skipped === group.skipped;
        const bindingMatches = receipt.status === 'PASS' && receipt.protocol === group.protocol && countsMatch && imageCount > 0
            && receipt.releaseVersion === manifest.release?.versionName
            && receipt.installedApkSha256 === manifest.release?.apkSha256
            && receipt.sourceCommit === manifest.source?.sourceCommit
            && receipt.sourceFingerprint === manifest.source?.workingTreeFingerprint
            && receipt.reportSha256 === reportHash && receipt.inlinePngCount === imageCount;
        const reportMatches = Boolean(reportItem && digest(reportBytes) === reportItem.sha256 && /<html\b/i.test(html.slice(0, 8192)));
        const receiptMatches = Boolean(receiptItem && receiptItem.sha256 === receiptDoc.sha256);
        if (!reportMatches || !receiptMatches || !bindingMatches) {
          e2eReceiptsOkay = false;
          e2eProblems.push(`${name}: Extent-ul, receipt-ul, APK-ul, sursa, numărătoarea sau capturile inline nu se leagă între ele.`);
        }
        e2eEvidence.push(citation('release-validation.json', manifestDoc.sha256,
            `/qualification/e2eGroups/${name}`, manifestDoc.lines.get(`/qualification/e2eGroups/${name}`) ?? 1));
        if (receiptItem) e2eEvidence.push(citation(receiptPath, receiptItem.sha256, '/reportSha256', receiptDoc.lines.get('/reportSha256') ?? 1));
        if (reportItem) e2eEvidence.push(citation(reportPath, reportItem.sha256));
      } catch (error) {
        e2eReceiptsOkay = false;
        e2eProblems.push(`${name}: receipt sau raport E2E ilizibil/neinclus în payload (${error.message}).`);
      }
    }
  }
  const e2eOkay = e2eReceiptsOkay && (!plan.userFlowChanged
      ? (e2e.status === 'PASS' || (['NOT_RUN', 'NOT_APPLICABLE'].includes(e2e.status) && e2e.rationale?.trim().length >= 12))
      : e2e.status === 'PASS');
  addCheck(checks, 'E2E_SCOPE', e2eOkay,
      e2eOkay ? `E2E: ${e2e.status}; decizia este explicită, cu raport Extent și receipt legați de APK, sursă, rezultate și capturi.`
        : 'Flux de utilizator schimbat fără E2E trecut, raport E2E lipsă/nepotrivit sau omitere fără justificare.',
      [citation(path.relative(project, planFile), planDoc.sha256, '/e2e', planDoc.lines.get('/e2e') ?? 1), ...e2eEvidence], e2eProblems);

  const allowedVerdicts = policy.rules?.allowedAggregateStatuses;
  const allPass = checks.every(check => check.status === 'PASS') && policyContract
      && Array.isArray(allowedVerdicts) && allowedVerdicts.includes('READY_FOR_HUMAN_REVIEW');
  const report = {
    schemaVersion: 1, kind: 'ATF_RELEASE_RAG_VERDICT', evaluatedAtUtc: new Date().toISOString(),
    verdict: allPass ? 'READY_FOR_HUMAN_REVIEW' : 'BLOCKED', releaseVersion: version ?? null,
    channel: plan.channel ?? null, sourceCommit: manifest.source?.sourceCommit ?? null,
    workingTreeFingerprint: manifest.source?.workingTreeFingerprint ?? null,
    candidateManifestSha256: manifestDoc.sha256, planSha256: planDoc.sha256,
    checks, componentGroups: groupResults,
    e2e: {status: e2e.status ?? 'NOT_RUN', userFlowChanged: plan.userFlowChanged ?? null,
      rationale: e2e.rationale ?? null, note: 'NOT_RUN/NOT_APPLICABLE nu devine PASS; această ieșire nu califică fluxurile E2E ale aplicației.'},
    lifecycleEffect: 'NONE', writesOnly: posix(path.relative(project, output)),
    limitations: ['Verificarea sursei acoperă exact lista din input manifest; nu execută Git și nu certifică starea indexului Git.',
      'Verdictul este o recomandare auditabilă pentru revizie umană; nu promovează, semnează sau publică.']
  };
  await fs.mkdir(path.dirname(output), {recursive: true});
  const temporary = output + '.' + crypto.randomUUID() + '.tmp';
  try { await fs.writeFile(temporary, JSON.stringify(report, null, 2) + '\n', {flag: 'wx'}); await fs.rename(temporary, output); }
  finally { await fs.unlink(temporary).catch(error => { if (error.code !== 'ENOENT') throw error; }); }
  return report;
}

function parseArgs(args) {
  const values = new Map();
  for (let index = 0; index < args.length; index += 2) {
    const key = args[index], value = args[index + 1];
    if (!['--project-root', '--candidate-dir', '--source-root', '--plan-file', '--output'].includes(key)
        || !value || values.has(key)) throw Error('Use --project-root <dir> --candidate-dir <dir> --source-root <dir> --plan-file <json> --output <external-json>');
    values.set(key, value);
  }
  for (const key of ['--project-root', '--candidate-dir', '--source-root', '--plan-file', '--output'])
    if (!values.has(key)) throw Error('Missing ' + key);
  const names = new Map([['--project-root', 'projectRoot'], ['--candidate-dir', 'candidateDir'],
    ['--source-root', 'sourceRoot'], ['--plan-file', 'planFile'], ['--output', 'outputFile']]);
  return Object.fromEntries([...values].map(([key, value]) => [names.get(key), path.resolve(value)]));
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const options = parseArgs(process.argv.slice(2));
    const report = await evaluateRelease(options);
    console.log(JSON.stringify({verdict: report.verdict, releaseVersion: report.releaseVersion, output: options.outputFile}));
    if (report.verdict !== 'READY_FOR_HUMAN_REVIEW') process.exitCode = 2;
  } catch (error) { console.error(error.message); process.exitCode = 1; }
}
