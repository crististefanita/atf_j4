import {readFileSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

export const nonBuildDocumentation = file => file === 'README.md'
    || (/^(?:docs\/|lib\/README\.)/.test(file) && file.endsWith('.md'))
    || (file.endsWith('.md') && file.includes('/src/')
        && !/\/src\/main\/(?:assets|res)\//.test(file))
    || (!file.includes('/src/') && file.endsWith('/README.md'));

export function changedInputs(candidate, previous) {
  const paths = new Set([...candidate.keys(), ...previous.keys()]);
  return [...paths].filter(file => candidate.get(file) !== previous.get(file)).sort();
}

const standaloneGovernanceLaunchers = new Set([
  'scripts/AtfStandaloneJava.ps1',
  'scripts/AtfTestChangeBatch.ps1',
  'scripts/run-atf-governance.ps1',
  'scripts/run-documentation-governance.ps1',
  'scripts/run-jvm-inventory.ps1'
]);
export const isStandaloneGovernanceLauncher = file => standaloneGovernanceLaunchers.has(file);

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const collectionOwners = readFileSync(path.join(projectRoot, 'config/gradle-app-collections.properties'), 'utf8')
  .split(/\r?\n/).map(line => line.trim()).filter(line => line && !line.startsWith('#'))
  .map(line => line.split('=', 2));
if (collectionOwners.some(([owner, group]) => !owner || !group)
    || new Set(collectionOwners.map(([owner]) => owner)).size !== collectionOwners.length) {
  throw Error('INVALID_GRADLE_APP_COLLECTION_OWNERSHIP');
}
const gradleAppPackages = new Map(collectionOwners.map(([owner, group]) => [owner, `whist-lab-app-${group}`]));

const appGroups = [...new Set(gradleAppPackages.values())];
const gradleGroups = [...appGroups, 'whist-lab-app-atf-card-table-ui', 'whist-lab-app-rules'];
const mavenGroups = ['atf-validation', 'atf-test-android', 'atf-tests'];
const allGroups = [...mavenGroups, ...gradleGroups];
const frameworkOnly = file => nonBuildDocumentation(file)
    || file.startsWith('atf-tests/') || file.startsWith('docs/') || file.startsWith('release/')
    || file.startsWith('atf-ui-browser/') || file === '.github/workflows/whist-components.yml'
    || file.startsWith('config/documentation-editions.') || file.startsWith('config/release-platform-rag-policy.')
    || file.startsWith('config/atf-governance-policy.') || file.startsWith('config/atf-test-suites.')
    || file.startsWith('config/test-collections.') || file.startsWith('scripts/')
    || file.startsWith('.atf-signing/');

function appPackageOwner(file, sourceSet) {
  const prefix = `whist-lab-app/app/src/${sourceSet}/java/ro/whistlab/app/`;
  if (!file.startsWith(prefix)) return null;
  const packageRoot = file.slice(prefix.length).split('/')[0];
  return gradleAppPackages.get(packageRoot) ?? null;
}

function affectedGroups(file) {
  if (/^docs\/game\/ATF_Rules(?:Ro|En)\.md$/.test(file)) return ['whist-lab-app-rules', 'atf-tests'];
  if (file.startsWith('.github/')) return file === '.github/workflows/whist-components.yml' ? ['atf-tests'] : allGroups;
  if (frameworkOnly(file)) return ['atf-tests'];

  if (file.startsWith('atf-validation/')) return ['atf-validation', 'atf-tests'];
  if (file.startsWith('atf-test-android/')) return ['atf-test-android', 'atf-tests'];
  if (file.startsWith('atf-tests/')) return ['atf-tests'];

  if (file === 'pom.xml' || file.endsWith('/pom.xml') || file.startsWith('.mvn/')
      || file.startsWith('lib/')) return allGroups;

  if (file === 'config/gradle-app-collections.properties') return [...appGroups, 'atf-tests'];
  if (file === 'config/gradle-audit-collections.properties' || /^config\/gradle-[^/]+\.properties$/.test(file)
      || file.startsWith('whist-lab-app/gradle/') || file.startsWith('whist-lab-app/gradle/wrapper/')
      || ['whist-lab-app/build.gradle', 'whist-lab-app/settings.gradle', 'whist-lab-app/gradle.properties'].includes(file)
      || file.startsWith('whist-lab-app/test-audit/')) return [...gradleGroups, 'atf-tests'];

  if (file.startsWith('whist-lab-app/app/src/main/java/ro/whistlab/app/')) {
    const owner = appPackageOwner(file, 'main');
    return owner ? [owner, 'atf-tests'] : allGroups;
  }
  if (file.startsWith('whist-lab-app/app/src/test/java/ro/whistlab/app/')) {
    const owner = appPackageOwner(file, 'test');
    return owner ? [owner, 'atf-tests'] : allGroups;
  }
  if (file.startsWith('whist-lab-app/app/src/rasterTest/')) return ['whist-lab-app-history', 'atf-tests'];
  if (file.startsWith('whist-lab-app/app/')) return [...appGroups, 'atf-tests'];

  // Core, names and the shared presentation library are dependencies of the app collections.
  if (file.startsWith('whist-lab-app/game-core/src/main/java/ro/whistlab/core/rules/HowToPlay.java'))
    return ['whist-lab-app-rules', 'atf-tests'];
  if (file.startsWith('whist-lab-app/game-core/') || file.startsWith('whist-lab-app/card-table-names/'))
    return [...appGroups, 'whist-lab-app-rules', 'atf-tests'];
  if (file.startsWith('whist-lab-app/atf-card-table-ui/'))
    return [...appGroups, 'whist-lab-app-atf-card-table-ui', 'atf-tests'];
  if (file.startsWith('whist-lab-app/ai-lab/')) return ['whist-lab-app-rules', 'atf-tests'];
  if (file.startsWith('whist-lab-app/algorithm-build/') || file.startsWith('whist-lab-app/ios-bridge/'))
    return ['atf-tests'];
  if (file.startsWith('whist-lab-app/')) return [...gradleGroups, 'atf-tests'];

  if (/^config\/whist-[^/]+\.(?:json|ya?ml)$/.test(file)) return [...gradleGroups, 'atf-tests'];
  if (file.startsWith('config/')) return ['atf-tests'];

  // New or unclassified paths fail closed: establish ownership before reusing old evidence.
  return allGroups;
}

export function componentEvidenceScope(group, releaseInputs, executionInputs, {androidVersionMetadataOnly = false} = {}) {
  const changed = changedInputs(releaseInputs, executionInputs);
  if (changed.length && !allGroups.includes(group)) {
    return {decision: 'RUN', reason: 'UNKNOWN_SUITE_FAIL_CLOSED', changed, affected: changed, affectedGroups: allGroups};
  }
  const classified = changed.map(file => ({file, groups: affectedGroups(file)}));
  const metadataOnlyBuildFile = androidVersionMetadataOnly
      && classified.every(row => row.file === 'whist-lab-app/app/build.gradle' || frameworkOnly(row.file));
  const affected = classified.filter(row => row.groups.includes(group)
      && !(metadataOnlyBuildFile && row.file === 'whist-lab-app/app/build.gradle')).map(row => row.file);
  const unclassified = classified.filter(row => row.groups.length === allGroups.length);
  const decision = affected.length ? 'RUN' : changed.length ? 'REUSE' : 'MATCH';
  const reason = affected.length
      ? unclassified.some(row => affected.includes(row.file)) ? 'UNKNOWN_INPUT_FAIL_CLOSED' : 'OWNED_OR_SHARED_INPUT_CHANGED'
      : changed.length ? 'OWNED_INPUTS_UNCHANGED' : 'IDENTICAL_INPUTS';
  return {decision, reason, changed, affected,
    affectedGroups: [...new Set(classified.flatMap(row => row.groups))].sort()};
}
