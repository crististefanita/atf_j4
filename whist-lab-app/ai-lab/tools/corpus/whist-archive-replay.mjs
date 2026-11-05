import fs from 'node:fs/promises';
import crypto from 'node:crypto';
import {validateGame} from './whist-corpus.mjs';

/** Explicit archive verification, separate from the default generated component fixtures. */
export async function replayArchive(file,expectedSha256) {
    if(typeof file!=='string'||!file.trim()||typeof expectedSha256!=='string'||!/^[a-f\d]{64}$/i.test(expectedSha256))
        throw Error('Archive replay requires an explicit file and SHA-256');
    const bytes=await fs.readFile(file);
    const sourceSha256=crypto.createHash('sha256').update(bytes).digest('hex');
    if(sourceSha256!==expectedSha256.toLowerCase())throw Error('Archive fixture SHA-256 mismatch');
    const game=JSON.parse(bytes.toString('utf8'));
    return {scope:'ARCHIVE_REPLAY',sourceSha256,game,audit:validateGame(game)};
}
