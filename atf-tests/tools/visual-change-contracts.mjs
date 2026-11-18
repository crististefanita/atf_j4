import {createHash} from 'node:crypto';
import {TextDecoder} from 'node:util';

const digest = bytes => createHash('sha256').update(bytes).digest('hex');
const decode = bytes => new TextDecoder('utf-8', {fatal: true}).decode(bytes);
const app = 'whist-lab-app/app/src/main/java/ro/whistlab/app/';
const shared = 'whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/';
const contracts = new Map([
  [app + 'theme/GameHeaderTypography.java', {
    id: 'game-header-typography', fields: {TITLE_SP: 'float', PROGRESS_SP: 'float', BID_SUMMARY_SP: 'float'},
    componentGroups: ['whist-lab-app-display'],
    e2eGroups: ['responsive-ui-sanity', 'signed-release-smoke']
  }],
  [shared + 'cards/CardFaceMetrics.java', {
    id: 'card-rank-typography', fields: {INDEX_SIZE_SP: 'int', RANK_SCALE: 'float', INVERTED_SIZE_DP: 'int'},
    componentGroups: ['whist-lab-app-display', 'whist-lab-app-atf-card-table-ui'],
    e2eGroups: ['responsive-ui-sanity', 'signed-release-auto-game', 'signed-release-smoke']
  }]
]);

export const visualContractPaths = Object.freeze([...contracts.keys()]);

function normalize(text, fields) {
  const values = {};
  for (const [name, type] of Object.entries(fields)) {
    const literal = type === 'int' ? '(?:0|[1-9][0-9]*)' : '(?:[0-9]+(?:\\.[0-9]+)?[fF])';
    const declaration = new RegExp('^([ \\t]*public static final ' + type + ' ' + name
      + '[ \\t]*=[ \\t]*)(' + literal + ')([ \\t]*;[ \\t]*)$', 'gm');
    const matches = [...text.matchAll(declaration)];
    if (matches.length !== 1 || !Number.isFinite(Number.parseFloat(matches[0][2])) || Number.parseFloat(matches[0][2]) <= 0) return null;
    values[name] = matches[0][2];
    text = text.replace(declaration, (_, prefix, value, suffix) => prefix + '<' + name + '>' + suffix);
  }
  return {text, values};
}

// Source bytes, not a caller's visual label, authorize a narrower native selection.
export function visualChangeContract(file, beforeHash, afterHash, sources, componentGroups) {
  const contract = contracts.get(file);
  if (!contract) return null;
  const fallback = reason => ({id: contract.id, status: 'FALLBACK', reason});
  if (!beforeHash || !afterHash) return fallback('ADDED_OR_DELETED_CONTRACT');
  if (!sources?.before || !sources?.after) return fallback('SOURCE_CONTENT_UNAVAILABLE');
  if (digest(sources.before) !== beforeHash || digest(sources.after) !== afterHash) return fallback('SOURCE_HASH_MISMATCH');
  if (contract.componentGroups.some(group => !componentGroups.includes(group))) return fallback('COMPONENT_GATE_MISSING');
  let before, after;
  try {
    // EOL differences cannot conceal another edit; all non-literal text must still match.
    before = normalize(decode(sources.before).replaceAll('\r\n', '\n'), contract.fields);
    after = normalize(decode(sources.after).replaceAll('\r\n', '\n'), contract.fields);
  } catch { return fallback('INVALID_UTF8'); }
  if (!before || !after || before.text !== after.text) return fallback('NON_LITERAL_OR_BEHAVIOR_CHANGE');
  const constants = Object.keys(contract.fields).filter(name => before.values[name] !== after.values[name])
    .map(name => ({name, before: before.values[name], after: after.values[name]}));
  if (!constants.length) return fallback('NO_VISUAL_LITERAL_CHANGE');
  return {id: contract.id, status: 'PROVEN', beforeSha256: beforeHash, afterSha256: afterHash, constants,
    allowedConstants: Object.keys(contract.fields),
    componentGroups: [...contract.componentGroups], e2eGroups: [...contract.e2eGroups]};
}
