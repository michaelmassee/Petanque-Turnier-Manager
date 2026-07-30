'use strict';

const express = require('express');
const qrcode = require('qrcode');
const pino = require('pino');
const { default: makeWASocket, useMultiFileAuthState, DisconnectReason } = require('@whiskeysockets/baileys');

const port = Number(process.env.PTM_WA_PORT || 9215);
const sessionDir = process.env.PTM_WA_SESSION_DIR || '.ptm-wa-session';

let lastQr = '';
let status = 'starting';
let sock = null;

async function starteVerbindung() {
  const { state, saveCreds } = await useMultiFileAuthState(sessionDir);
  sock = makeWASocket({
    auth: state,
    logger: pino({ level: 'silent' })
  });

  sock.ev.on('creds.update', saveCreds);

  sock.ev.on('connection.update', (update) => {
    if (update.qr) {
      lastQr = update.qr;
      status = 'qr';
    }
    if (update.connection === 'open') {
      lastQr = '';
      status = 'ready';
    }
    if (update.connection === 'close') {
      const code = update.lastDisconnect && update.lastDisconnect.error && update.lastDisconnect.error.output
        ? update.lastDisconnect.error.output.statusCode
        : 0;
      if (code === DisconnectReason.loggedOut) {
        status = 'disconnected:logged_out';
      } else {
        status = `disconnected:${code}`;
        starteVerbindung().catch((error) => {
          status = `error:${error.message}`;
        });
      }
    }
  });
}

starteVerbindung().catch((error) => {
  status = `error:${error.message}`;
});

const app = express();
app.use(express.json({ limit: '25mb' }));

app.get('/status', (_req, res) => {
  res.json({ status, qr: lastQr });
});

app.get('/qr', async (_req, res) => {
  res.type('html').send(await qrSeite());
});

app.get('/groups', async (_req, res) => {
  if (status !== 'ready') {
    res.status(409).json({ error: 'WhatsApp is not ready', status });
    return;
  }
  const gruppen = await sock.groupFetchAllParticipating();
  res.json({
    chats: Object.entries(gruppen).map(([id, meta]) => ({ id, name: meta.subject || id, type: 'Gruppe' }))
  });
});

app.post('/send-image', async (req, res) => {
  if (status !== 'ready') {
    res.status(409).json({ error: 'WhatsApp is not ready', status });
    return;
  }
  const { chatId, caption, imageBase64 } = req.body || {};
  if (!chatId || !imageBase64) {
    res.status(400).json({ error: 'chatId and imageBase64 are required' });
    return;
  }
  await sock.sendMessage(chatId, { image: Buffer.from(imageBase64, 'base64'), caption: caption || '' });
  res.json({ ok: true });
});

app.listen(port, '127.0.0.1', () => {
  console.log(`PTM WhatsApp bridge listening on http://127.0.0.1:${port}`);
});

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;');
}

async function qrSeite() {
  let refreshSekunden = null;
  let inhalt;

  if (status === 'ready') {
    inhalt = `
      <div class="badge">&#10003;</div>
      <h1>WhatsApp verbunden</h1>
      <p class="sub">PétTurnMngr kann jetzt Bilder in die konfigurierten Chats posten. Diese Seite kann geschlossen werden.</p>`;
  } else if (status === 'disconnected:logged_out') {
    refreshSekunden = null;
    inhalt = `
      <h1 class="fehler">Abgemeldet</h1>
      <p class="sub">Das Gerät wurde in WhatsApp getrennt. Bitte in den PétTurnMngr-Optionen (Extras &gt; Optionen &gt;
      PétTurnMngr &gt; WhatsApp-Chats) erneut auf „Login / QR-Code“ klicken, um einen neuen Code zu erhalten.</p>`;
  } else if (typeof status === 'string' && status.startsWith('error:')) {
    refreshSekunden = 5;
    inhalt = `
      <h1 class="fehler">Fehler</h1>
      <p class="sub">${escapeHtml(status)}</p>`;
  } else if (lastQr) {
    refreshSekunden = 15;
    const dataUrl = await qrcode.toDataURL(lastQr);
    inhalt = `
      <h1>WhatsApp-Anmeldung</h1>
      <p class="sub">Scanne diesen QR-Code mit deinem Handy, um PétTurnMngr mit WhatsApp zu verbinden.</p>
      <ol>
        <li>WhatsApp auf dem Handy öffnen</li>
        <li>Einstellungen &rarr; <strong>Verknüpfte Geräte</strong> öffnen
          (Android: die drei Punkte oben rechts &rarr; <strong>Verknüpfte Geräte</strong>)</li>
        <li><strong>Gerät verknüpfen</strong> antippen</li>
        <li>Diesen QR-Code mit der Handykamera im WhatsApp-Scanner erfassen</li>
      </ol>
      <img class="qr" alt="WhatsApp QR-Code" src="${dataUrl}">
      <p class="status">Der Code wird alle 15 Sekunden automatisch aktualisiert.</p>`;
  } else {
    refreshSekunden = 3;
    inhalt = `
      <h1>WhatsApp-Bridge wird vorbereitet&hellip;</h1>
      <p class="sub">Der QR-Code erscheint hier automatisch, sobald er bereit ist.</p>`;
  }

  const refreshTag = refreshSekunden ? `<meta http-equiv="refresh" content="${refreshSekunden}">` : '';

  return `<!doctype html>
<html lang="de">
<head>
<meta charset="utf-8">
${refreshTag}
<title>PétTurnMngr &ndash; WhatsApp-Anmeldung</title>
<style>
  body { font-family: -apple-system, "Segoe UI", Roboto, sans-serif; background:#F3F4F6; color:#1F2937;
         margin:0; padding:32px 16px; display:flex; justify-content:center; }
  .karte { background:#fff; border-radius:12px; box-shadow:0 1px 4px rgba(0,0,0,.12); max-width:420px;
           width:100%; padding:28px 32px; text-align:center; box-sizing:border-box; }
  h1 { font-size:20px; margin:0 0 8px; }
  .sub { color:#6B7280; font-size:13px; margin:0 0 18px; line-height:1.5; }
  ol { text-align:left; font-size:14px; line-height:1.7; padding-left:22px; margin:0 0 20px; }
  img.qr { width:260px; height:260px; border:1px solid #E5E7EB; border-radius:8px; padding:8px; }
  .status { margin-top:16px; font-size:12px; color:#9CA3AF; }
  .fehler { color:#DC2626; }
  .badge { display:inline-flex; align-items:center; justify-content:center; width:48px; height:48px;
           border-radius:50%; background:#16A34A; color:#fff; font-size:26px; margin-bottom:12px; }
</style>
</head>
<body>
  <div class="karte">${inhalt}</div>
</body>
</html>`;
}
