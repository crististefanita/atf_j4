/** Editorial preflight for new notes; published historical payloads stay immutable. */
export function validateReleaseNotes(notes) {
  const text = notes.replaceAll('\r\n', '\n').trim();
  const firstSection = text.search(/^##\s/m);
  const opening = (firstSection < 0 ? text : text.slice(0, firstSection)).replace(/^# .+\n/, '').trim();
  if (!opening || opening.includes('\n\n') || opening.startsWith('- ') || Array.from(opening).length > 400) {
    throw Error('GITHUB_NOTES_SUMMARY: one short opening paragraph is required');
  }
  const changes = /^## (?:Changes[^\n]*|Schimbări[^\n]*)\n([\s\S]*?)(?=^## |(?![\s\S]))/m.exec(text)?.[1]?.trim();
  const lines = changes?.split('\n').filter(line => line.trim()) ?? [];
  if (!lines.length || lines.some(line => !line.startsWith('- ') || line.length < 4 || Array.from(line).length > 300)) {
    throw Error('GITHUB_NOTES_CHANGES: changes require separate concise bullets');
  }
  return {summaryCharacters: Array.from(opening).length, changeCount: lines.length};
}
