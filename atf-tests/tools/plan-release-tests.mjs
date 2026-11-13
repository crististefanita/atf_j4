import fs from 'node:fs/promises';
import path from 'node:path';
import {loadReleaseTestScope, strictJson} from './evaluate-release-rag.mjs';

const [projectDir, sourceDir, inputFile, version, outputFile, ...extra] = process.argv.slice(2);
try {
  if (!projectDir || !sourceDir || !inputFile || !version || !outputFile || extra.length) {
    throw Error('Use: node plan-release-tests.mjs <project> <pinned-source> <source-inputs.json> <version> <external-output.json>');
  }
  const [project, source, input] = await Promise.all([projectDir, sourceDir, inputFile].map(file => fs.realpath(file)));
  const output = path.join(await fs.realpath(path.dirname(path.resolve(outputFile))), path.basename(outputFile));
  if ([project, source].some(root => {
    const relative = path.relative(root, output);
    return relative === '' || (!path.isAbsolute(relative) && relative !== '..' && !relative.startsWith('..' + path.sep));
  })) throw Error('OUTPUT_MUST_BE_OUTSIDE_INPUTS');
  const plan = await loadReleaseTestScope(project, source, version, (await strictJson(input)).value);
  await fs.writeFile(output, JSON.stringify(plan, null, 2) + '\n', {flag: 'wx'});
  console.log(JSON.stringify({output, kind: plan.kind, run: plan.runComponentGroups, e2e: plan.requiredE2eGroups}));
} catch (error) {
  console.error(error.message);
  process.exitCode = 1;
}
