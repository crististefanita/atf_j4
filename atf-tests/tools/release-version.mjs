/** Numeric release identity; a missing historical patch component means zero. */
export const releaseVersionPattern = /^(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)(?:\.(?:0|[1-9]\d*))?(?:-[0-9A-Za-z.-]+)?$/;
export const publishedVersionPattern = /^(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)(?:\.(?:0|[1-9]\d*))?$/;

export function releaseVersionAtLeast(version, minimum) {
  if (!releaseVersionPattern.test(version ?? '') || !releaseVersionPattern.test(minimum ?? '')) return false;
  const parts = value => value.split('-')[0].split('.').map(BigInt);
  const current = parts(version), threshold = parts(minimum);
  for (let index = 0; index < 3; index++) {
    const left = current[index] ?? 0n, right = threshold[index] ?? 0n;
    if (left !== right) return left > right;
  }
  return true;
}
