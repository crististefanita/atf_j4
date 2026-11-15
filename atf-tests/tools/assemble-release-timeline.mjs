import fs from 'node:fs/promises';
import path from 'node:path';
import {digest, strictJson} from './evaluate-release-rag.mjs';
import {mergeTiming, timingFromDocument, within, writeTimingReceipt} from './release-timing.mjs';

try {
  const [project, output, ...files] = process.argv.slice(2);
  if (!project || !output || !files.length) throw Error('Use <project> <new-external-output.json> <timing-or-receipt.json>...');
  const destination = path.join(await fs.realpath(path.dirname(path.resolve(output))), path.basename(output));
  if (within(await fs.realpath(project), destination)) throw Error('OUTPUT_MUST_BE_OUTSIDE_INPUTS');
  const inputs = await Promise.all(files.map(file => strictJson(path.resolve(file))));
  const fingerprints = [...new Set(inputs.map(document => document.sha256))];
  if (fingerprints.length !== inputs.length) throw Error('DUPLICATE_RELEASE_TIMING_RECEIPT');
  const report = mergeTiming(inputs.map(({value}) => timingFromDocument(value)));
  const receiptHashes = inputs.map((document, index) => ({name: path.basename(files[index]), sha256: document.sha256}));
  await writeTimingReceipt(destination, {...report, receiptHashes});
  console.log(JSON.stringify({stages: report.stages.length, elapsedWallMs: report.elapsedWallMs,
    elapsedCoveredMs: report.elapsedCoveredMs, output: destination}));
} catch (error) { console.error(error.message); process.exitCode = 1; }
