export const REFERENCE = Object.freeze({ width: 414, height: 896 });
export const MIN_DESKTOP_SCALE = 0.68;

export function isPhone(userAgent) {
  return /Android|iPhone|iPod/.test(userAgent);
}

export function fitTextWidth(size, measured, available) {
  return size * Math.min(1, Math.max(1, available) / Math.max(1, measured));
}

export function viewportGeometry(width, height, mobile) {
  if (![width, height].every(value => Number.isFinite(value) && value > 0)) {
    throw new RangeError("The usable viewport must have positive finite dimensions");
  }
  if (mobile) return { width, height, scale: 1, frameWidth: width, frameHeight: height };

  // Keep the readable desktop floor when width permits. When it does not, fit both axes.
  const fitScale = Math.min(height / REFERENCE.height, width / REFERENCE.width);
  const readableFloorFits = width >= REFERENCE.width * MIN_DESKTOP_SCALE && height >= REFERENCE.height * MIN_DESKTOP_SCALE;
  const scale = fitScale >= MIN_DESKTOP_SCALE || !readableFloorFits ? fitScale : MIN_DESKTOP_SCALE;
  return { ...REFERENCE, scale, frameWidth: REFERENCE.width * scale, frameHeight: REFERENCE.height * scale };
}
