import {crc32, inflateSync} from 'node:zlib';

const pngSignature = Buffer.from('89504e470d0a1a0a', 'hex');
const prefix = 'data:image/png;base64,';
const channels = new Map([[0, 1], [2, 3], [3, 1], [4, 2], [6, 4]]);
const depths = new Map([[0, [1, 2, 4, 8, 16]], [2, [8, 16]], [3, [1, 2, 4, 8]], [4, [8, 16]], [6, [8, 16]]]);

export function verifyPng(bytes) {
  const invalid = () => { throw Error('INVALID_PNG'); };
  if (!bytes.subarray(0, 8).equals(pngSignature)) invalid();
  let header, palette = false, ended = false;
  const data = [];
  for (let offset = 8; offset + 12 <= bytes.length;) {
    const length = bytes.readUInt32BE(offset);
    const end = offset + length + 12;
    if (end > bytes.length || ended) invalid();
    const name = bytes.toString('ascii', offset + 4, offset + 8);
    const chunk = bytes.subarray(offset + 8, end - 4);
    if (crc32(bytes.subarray(offset + 4, end - 4)) !== bytes.readUInt32BE(end - 4)) invalid();
    if (!header && name !== 'IHDR') invalid();
    switch (name) {
      case 'IHDR':
        if (header || length !== 13) invalid();
        header = chunk;
        break;
      case 'PLTE':
        if (palette || data.length || length < 3 || length > 768 || length % 3 !== 0) invalid();
        palette = true;
        break;
      case 'IDAT': data.push(chunk); break;
      case 'IEND':
        if (length || end !== bytes.length) invalid();
        ended = true;
        break;
    }
    offset = end;
  }
  if (!ended || !data.length) invalid();
  const width = header.readUInt32BE(0), height = header.readUInt32BE(4);
  const [depth, color, compression, filter, interlace] = header.subarray(8);
  if (!width || !height || !depths.get(color)?.includes(depth) || (color === 3 && !palette)
      || compression || filter || interlace > 1) invalid();
  const passes = interlace ? [[0, 0, 8, 8], [4, 0, 8, 8], [0, 4, 4, 8], [2, 0, 4, 4],
    [0, 2, 2, 4], [1, 0, 2, 2], [0, 1, 1, 2]] : [[0, 0, 1, 1]];
  const scanlines = passes.map(([x, y, dx, dy]) => {
    const columns = Math.ceil((width - x) / dx), rows = Math.ceil((height - y) / dy);
    return {stride: 1 + Math.ceil(columns * channels.get(color) * depth / 8), rows: columns > 0 ? Math.max(rows, 0) : 0};
  });
  const expected = scanlines.reduce((total, {rows, stride}) => total + rows * stride, 0);
  // Bound decompression before reading image data from an untrusted report.
  if (expected > 64 * 1024 * 1024) invalid();
  const compressed = Buffer.concat(data);
  const decoded = inflateSync(compressed, {maxOutputLength: expected, info: true});
  if (decoded.buffer.length !== expected || decoded.engine.bytesWritten !== compressed.length) invalid();
  let offset = 0;
  for (const {rows, stride} of scanlines)
    for (let row = 0; row < rows; row++, offset += stride)
      if (decoded.buffer[offset] > 4) invalid();
}

export function inspectE2eImages(html, {requireCheckpoints = false} = {}) {
  if (!/<html\b/i.test(html.slice(0, 8192)) || !/<\/html>\s*$/i.test(html)) throw Error('INCOMPLETE_EXTENT_HTML');
  const valid = new Set(), checkpoints = new Set();
  const total = html.split(prefix).length - 1;
  let count = 0, ownedCount = 0;
  for (const match of html.matchAll(/data:image\/png;base64,([A-Za-z0-9+/]+={0,2})(?=["'\s)])/g)) {
    const encoded = match[1];
    if (!valid.has(encoded)) {
      const bytes = Buffer.from(encoded, 'base64');
      if (bytes.toString('base64') !== encoded) throw Error('INVALID_PNG_BASE64');
      verifyPng(bytes);
      valid.add(encoded);
    }
    count++;
  }
  for (const match of html.matchAll(/<a\b[^>]*\bdata-atf-screenshot="base64"[^>]*>/gi)) {
    const target = /\bhref="data:image\/png;base64,([^"]+)"/i.exec(match[0])?.[1];
    if (!valid.has(target)) throw Error('INVALID_CHECKPOINT_SCREENSHOT');
    checkpoints.add(target); ownedCount++;
  }
  if (!count || count !== total || (requireCheckpoints && !checkpoints.size)) throw Error('MISSING_OR_INVALID_INLINE_PNG');
  return {count, checkpoints, ownedCount};
}
