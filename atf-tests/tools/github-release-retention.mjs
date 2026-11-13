const versionPattern = /^(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)$/;
const sha256Pattern = /^[a-f0-9]{64}$/;
const commitPattern = /^[a-f0-9]{40}$/;
const repositoryPattern = /^[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+$/;

export const githubRetentionPolicy = {
  collector: 'atf-tests/tools/collect-github-release-retention.mjs',
  catalog: 'release/index.json',
  retainedReleaseCount: 2,
  requiredAfterGithubPublication: true
};

export function catalogVersions(catalog) {
  const versions = catalog?.versions;
  if (catalog?.schemaVersion !== 2 || !Array.isArray(versions)
      || versions.length !== githubRetentionPolicy.retainedReleaseCount
      || catalog.currentVersion !== versions[0]
      || !versions.every(version => typeof version === 'string' && versionPattern.test(version))) {
    throw Error('RETENTION_CATALOG: expected the current version and one previous version');
  }
  const current = versions[0].split('.').map(Number), previous = versions[1].split('.').map(Number);
  const difference = current.findIndex((value, index) => value !== previous[index]);
  if (difference < 0 || current[difference] <= previous[difference]) throw Error('RETENTION_CATALOG_ORDER');
  return versions;
}

export function expectedEdition(version, manifest) {
  const {release, source, publication, integrity} = manifest;
  const apk = 'WhistLab-' + version + '.apk';
  const tag = 'v' + version;
  const repository = publication?.repository;
  const url = name => 'https://github.com/' + repository + '/releases/download/' + tag + '/' + name;
  if (release?.versionName !== version || release.apkName !== apk || release.apkRelativePath !== apk
      || !commitPattern.test(source?.sourceCommit ?? '') || !repositoryPattern.test(repository ?? '')
      || publication.state !== 'PUBLISHED' || publication.targetUrl !== url(apk)) {
    throw Error('RETENTION_MANIFEST: ' + version);
  }

  const assets = [apk, apk + '.sha256'].map(name => {
    const rows = Array.isArray(integrity?.files) ? integrity.files.filter(row => row?.path === name) : [];
    const row = rows[0];
    if (rows.length !== 1 || !Number.isSafeInteger(row.bytes) || row.bytes <= 0 || !sha256Pattern.test(row.sha256 ?? '')) {
      throw Error('RETENTION_MANIFEST_ASSET: ' + name);
    }
    return {name, size: row.bytes, sha256: row.sha256, url: url(name)};
  });
  if (assets[0].size !== release.apkBytes || assets[0].sha256 !== release.apkSha256) throw Error('RETENTION_MANIFEST_APK: ' + version);
  return {repository, tag, commit: source.sourceCommit, assets};
}

function indexRows(rows, key, expectedKeys, label, problems) {
  const map = new Map();
  if (!Array.isArray(rows)) {
    problems.push(label + ': inventory missing');
    return map;
  }
  for (const row of rows) {
    const value = row?.[key];
    if (typeof value !== 'string' || map.has(value)) problems.push(label + ': invalid or duplicate ' + key + ' ' + String(value));
    else map.set(value, row);
  }
  for (const value of map.keys()) if (!expectedKeys.includes(value)) problems.push(label + ': unexpected ' + value);
  for (const value of expectedKeys) if (!map.has(value)) problems.push(label + ': missing ' + value);
  return map;
}

export function githubRetentionProblems(expected, observation) {
  const problems = [];
  if (observation?.schemaVersion !== 1 || observation.kind !== 'github-release-retention'
      || observation.repository !== expected.repository || observation.catalogSha256 !== expected.catalogSha256
      || !expected.evidence.every(file => observation.mainFileSha256?.[file.path] === file.sha256)
      || observation.releaseListAccess !== 'AUTHENTICATED_WRITE' || !commitPattern.test(observation.mainCommit ?? '')) {
    problems.push('RETENTION_OBSERVATION: repository, catalog, main or authenticated inventory differs');
  }
  const tags = expected.editions.map(edition => edition.tag);
  const releases = indexRows(observation?.releases, 'tag', tags, 'RELEASES', problems);
  const refs = indexRows(observation?.tags, 'tag', tags, 'TAGS', problems);
  const releaseIds = new Set(), assetIds = new Set();
  for (const edition of expected.editions) {
    const ref = refs.get(edition.tag);
    if (ref && ref.commit !== edition.commit) problems.push('TAG_COMMIT: ' + edition.tag);
    const release = releases.get(edition.tag);
    if (!release) continue;
    if (release.draft !== false || !release.publishedAt || !Number.isSafeInteger(release.id)
        || release.id < 1 || releaseIds.has(release.id)) problems.push('RELEASE_PUBLICATION: ' + edition.tag);
    releaseIds.add(release.id);

    const assets = indexRows(release.assets, 'name', edition.assets.map(asset => asset.name), 'ASSETS ' + edition.tag, problems);
    for (const asset of edition.assets) {
      const actual = assets.get(asset.name);
      if (!actual) continue;
      if (actual.state !== 'uploaded' || actual.url !== asset.url || actual.size !== asset.size
          || actual.sha256 !== asset.sha256 || !Number.isSafeInteger(actual.id) || actual.id < 1 || assetIds.has(actual.id)) {
        problems.push('ASSET_METADATA: ' + asset.name);
      }
      assetIds.add(actual.id);
      if (actual.download?.httpStatus !== 200 || actual.download.bytes !== asset.size
          || actual.download.sha256 !== asset.sha256) problems.push('ASSET_DOWNLOAD: ' + asset.name);
    }
  }
  return problems;
}
