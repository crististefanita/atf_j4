import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {deriveReleaseTestScope} from './release-test-scope.mjs';
import {visualContractPaths} from './visual-change-contracts.mjs';

export const header = visualContractPaths[0], cards = visualContractPaths[1];
const groups = ['atf-tests', 'whist-lab-app-display', 'whist-lab-app-atf-card-table-ui'];
export const headerText = 'public final class GameHeaderTypography {\n'
  + '    public static final float TITLE_SP = 14f;\n'
  + '    public static final float PROGRESS_SP = 11f;\n'
  + '    public static final float BID_SUMMARY_SP = 11.7f;\n}\n';
const cardText = 'public record CardFaceMetrics(int heightDp) {\n'
  + '    public static final int INDEX_SIZE_SP = 13;\n'
  + '    public static final float RANK_SCALE = 1.5f;\n'
  + '    public static final int INVERTED_SIZE_DP = 9;\n'
  + '    public static final int HAND_HEIGHT_DP = 88;\n}\n';
export const hash = value => createHash('sha256').update(value).digest('hex');

export function probe(name) {
  const file = name === 'cards' ? cards : header;
  const before = file === cards ? cardText : headerText;
  let after = file === cards ? before.replace('1.5f', '1.8f') : before.replace('14f', '16f');
  if (name === 'method') after = after.replace('}\n', ' public int action() { return 1; }\n}\n');
  if (name === 'expression') after = after.replace('16f', '14f + 2f');
  if (name === 'zero') after = after.replace('16f', '0f');
  if (name === 'renamed') after = after.replace('TITLE_SP', 'OTHER_SP');
  if (name === 'format-only') after = before.replaceAll('\n', '\r\n');
  if (name === 'duplicate') after += '    public static final float TITLE_SP = 16f;\n';
  const previous = new Map([[file, hash(before)]]), candidate = new Map([[file, hash(after)]]);
  const sources = new Map([[file, {before: Buffer.from(before), after: Buffer.from(after)}]]);
  const other = {
    bidding: 'whist-lab-app/app/src/main/java/ro/whistlab/app/bidding/BidPolicy.java',
    persistence: 'whist-lab-app/app/src/main/java/ro/whistlab/app/persistence/Save.java',
    scoring: 'whist-lab-app/app/src/main/java/ro/whistlab/app/score/Score.java',
    profile: 'whist-lab-app/app/src/main/java/ro/whistlab/app/profile/Profile.java',
    achievements: 'whist-lab-app/app/src/main/java/ro/whistlab/app/achievements/Progress.java',
    binary: 'lib/card-table-ui-next.aar', unknown: 'config/unknown-runtime.json'
  }[name];
  if (other) { previous.set(other, 'old'); candidate.set(other, 'new'); }
  if (name === 'missing') sources.clear();
  if (name === 'wrong-hash') sources.get(file).before = Buffer.from('unbound source');
  if (name === 'deleted') candidate.delete(file);
  if (name === 'added') previous.delete(file);
  const selectedGroups = name === 'missing-gate' ? ['atf-tests'] : groups;
  return {input: {path: file, before, after, componentGroups: selectedGroups, other: other ?? null},
    selection: deriveReleaseTestScope(previous, candidate, selectedGroups, false, sources)};
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  process.stdout.write(JSON.stringify(probe(process.argv[2])) + '\n');
}
