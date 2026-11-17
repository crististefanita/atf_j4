import assert from "node:assert/strict";
import { resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const REFERENCE = Object.freeze({ width: 414, height: 896 });
const MIN_DESKTOP_SCALE = 0.68;

export function verifyViewportContract(viewport) {
  const { isPhone, viewportGeometry } = viewport;
  assert.deepEqual(viewport.REFERENCE, REFERENCE, "The common logical frame must match the phone reference");
  assert.equal(viewport.MIN_DESKTOP_SCALE, MIN_DESKTOP_SCALE, "The readable desktop floor is part of the contract");

  const mobileCases = [
    [414, 896], [360, 800], [390, 844], [430, 932], [896, 414], [800, 1280]
  ];
  for (const [width, height] of mobileCases) {
    const result = viewportGeometry(width, height, true);
    assert.deepEqual(result, { width, height, scale: 1, frameWidth: width, frameHeight: height },
      `Mobile viewport ${width}×${height} must use its CSS dimensions without a second scale`);
  }

  const desktopCases = [
    [1920, 1080], [1280, 720], [1440, 900], [535, 760], [414, 896], [400, 900],
    [320, 900], [282, 900], [280, 900], [220, 420], [400, 500], [400, 600], [400, 610], [300, 400],
    [800, 600], [1280, 420], [1920, 609], [1920, 610]
  ];
  for (const [width, height] of desktopCases) {
    const result = viewportGeometry(width, height, false);
    const fitScale = Math.min(height / REFERENCE.height, width / REFERENCE.width);
    const readableFloorFits = width >= REFERENCE.width * MIN_DESKTOP_SCALE && height >= REFERENCE.height * MIN_DESKTOP_SCALE;
    const expectedScale = fitScale >= MIN_DESKTOP_SCALE || !readableFloorFits ? fitScale : MIN_DESKTOP_SCALE;
    assert.ok(Math.abs(result.scale - expectedScale) < 1e-12,
      `Desktop ${width}×${height} scale should respect both usable axes`);
    assert.ok(Math.abs(result.frameWidth / result.frameHeight - REFERENCE.width / REFERENCE.height) < 1e-12,
      `Desktop ${width}×${height} must preserve the reference aspect ratio`);
    assert.ok(result.frameWidth <= width + 1e-9,
      `Desktop ${width}×${height} must not create horizontal overflow`);
    assert.ok(result.frameHeight <= height + 1e-9,
      `Desktop ${width}×${height} must not create vertical overflow`);
    assert.equal(result.width, REFERENCE.width);
    assert.equal(result.height, REFERENCE.height);
  }

  const widthSeries = [220, 250, 280, 320, 414, 535, 800, 1280, 1920]
    .map(width => viewportGeometry(width, 1080, false).scale);
  for (let index = 1; index < widthSeries.length; index++) {
    assert.ok(widthSeries[index] >= widthSeries[index - 1], "Wider windows cannot reduce the scale");
  }
  const heightSeries = [420, 609, 610, 720, 896, 1080, 1440]
    .map(height => viewportGeometry(1920, height, false).scale);
  for (let index = 1; index < heightSeries.length; index++) {
    assert.ok(heightSeries[index] >= heightSeries[index - 1], "Taller windows cannot reduce the scale");
  }

  assert.ok(isPhone("Mozilla/5.0 (Linux; Android 14; Mobile)"));
  assert.ok(isPhone("Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X)"));
  assert.ok(!isPhone("Mozilla/5.0 (Windows NT 10.0; Win64; x64)"));

  for (const [width, height, mobile] of [
    [0, 896, true], [-1, 896, false], [414, 0, true], [414, -1, false],
    [Number.NaN, 896, false], [414, Number.NaN, true],
    [Number.POSITIVE_INFINITY, 896, false], [414, Number.POSITIVE_INFINITY, true]
  ]) {
    assert.throws(() => viewportGeometry(width, height, mobile), RangeError,
      `Invalid viewport ${width}×${height} must be rejected`);
  }

  return {
    mobileCases: mobileCases.length,
    desktopCases: desktopCases.length,
    monotonicityChecks: widthSeries.length + heightSeries.length - 2,
    invalidCases: 8,
    totalCases: mobileCases.length + desktopCases.length + widthSeries.length + heightSeries.length + 8 + 3,
    constrainedWidth: viewportGeometry(220, 420, false),
    shortHeight: viewportGeometry(400, 500, false),
    readableFloor: viewportGeometry(400, 610, false)
  };
}

async function runFromCommandLine() {
  const [modulePath] = process.argv.slice(2);
  if (!modulePath) throw new Error("Expected a path to the admitted viewport.mjs module");
  const viewport = await import(pathToFileURL(resolve(modulePath)).href);
  const evidence = verifyViewportContract(viewport);
  console.log(`PASS shared viewport contract ${JSON.stringify(evidence)}`);
}

if (process.argv[1] && resolve(process.argv[1]).toLowerCase() === fileURLToPath(import.meta.url).toLowerCase()) {
  await runFromCommandLine();
}
