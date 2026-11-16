export const REFERENCE = Object.freeze({ width: 414, height: 896 });

export function isPhone(userAgent) {
  return /Android|iPhone|iPod/.test(userAgent);
}

export function viewportGeometry(width, height, mobile) {
  if (![width, height].every(value => Number.isFinite(value) && value > 0)) {
    throw new RangeError("The usable viewport must have positive finite dimensions");
  }
  if (mobile) return { width, height, scale: 1, frameWidth: width, frameHeight: height };

  // Height sets the scale whenever width permits. Scroll instead of shrinking touch/text further.
  const scale = Math.max(0.68, Math.min(height / REFERENCE.height, width / REFERENCE.width));
  return { ...REFERENCE, scale, frameWidth: REFERENCE.width * scale, frameHeight: REFERENCE.height * scale };
}
