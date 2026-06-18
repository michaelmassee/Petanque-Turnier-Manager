export function liveUrl(path) {
  const base = new URL('.', window.location.href);
  return new URL(path.replace(/^\//, ''), base).toString();
}
