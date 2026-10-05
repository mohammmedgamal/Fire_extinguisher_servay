/* Fire Extinguisher Survey – offline web app (PWA).
 * Data lives in this browser's localStorage; use Backup/Restore to move it between devices. */
'use strict';

// ---------------------------------------------------------------------------
// Reference data (kept identical to the Android and iOS apps)
// ---------------------------------------------------------------------------

const ISSUES = [
  ['CORRODED', 'Corroded / rusted'],
  ['DAMAGED', 'Physically damaged / dented'],
  ['NEEDS_CASING', 'Needs casing / cabinet'],
  ['LOW_PRESSURE', 'Low pressure (gauge not in green)'],
  ['PIN_SEAL', 'Safety pin or tamper seal missing/broken'],
  ['HOSE_NOZZLE', 'Hose or nozzle damaged / blocked'],
  ['LABEL', 'Label / instructions missing or unreadable'],
  ['OBSTRUCTED', 'Access obstructed / not visible'],
  ['NEEDS_REFILL', 'Used or needs refill / recharge'],
  ['SERVICE_DUE', 'Service / hydrostatic test overdue'],
  ['MOUNTING', 'Bracket / mounting damaged'],
  ['MISSING', 'Extinguisher missing from location'],
  ['OTHER', 'Other (describe in notes)'],
];
const ISSUE_LABEL = Object.fromEntries(ISSUES);

const TYPES = [
  'Dry chemical powder (ABC)',
  'CO2',
  'Foam (AFFF)',
  'Water',
  'Wet chemical',
  'Clean agent (FM-200 / Halotron)',
  'Other',
];

/** Extinguishers are expected to be checked at least this often. */
const INSPECTION_INTERVAL_DAYS = 30;
const DAY_MS = 24 * 60 * 60 * 1000;

// ---------------------------------------------------------------------------
// Storage
// ---------------------------------------------------------------------------

const STORE_KEY = 'fire-extinguisher-survey.v1';

/** @type {{extinguishers: Object<string, any>, inspections: any[], inspector: string}} */
let db = load();

function emptyDb() {
  return { extinguishers: {}, inspections: [], inspector: '' };
}

function load() {
  try {
    const raw = localStorage.getItem(STORE_KEY);
    if (!raw) return emptyDb();
    return validateDb(JSON.parse(raw));
  } catch (e) {
    console.error('Could not load data', e);
    return emptyDb();
  }
}

function validateDb(data) {
  if (!data || typeof data !== 'object' || typeof data.extinguishers !== 'object' || !Array.isArray(data.inspections)) {
    throw new Error('Not a valid survey backup file');
  }
  return { extinguishers: data.extinguishers, inspections: data.inspections, inspector: String(data.inspector || '') };
}

function save() {
  try {
    localStorage.setItem(STORE_KEY, JSON.stringify(db));
    return true;
  } catch (e) {
    console.error('Could not save data', e);
    toast('Could not save – browser storage is full or blocked');
    return false;
  }
}

function getExt(code) {
  return Object.prototype.hasOwnProperty.call(db.extinguishers, code) ? db.extinguishers[code] : null;
}

/** Inspections for one extinguisher, newest first. */
function inspectionsFor(code) {
  return db.inspections.filter((i) => i.code === code).sort((a, b) => b.ts - a.ts);
}

function lastInspectionMap() {
  const map = {};
  for (const i of db.inspections) {
    if (!map[i.code] || map[i.code].ts < i.ts) map[i.code] = i;
  }
  return map;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

function esc(value) {
  return String(value ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
}

const pad = (n) => String(n).padStart(2, '0');
function fmtDate(ts) {
  const d = new Date(ts);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}
function fmtDateTime(ts) {
  const d = new Date(ts);
  return `${fmtDate(ts)} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
function daysAgo(ts) {
  return Math.floor((Date.now() - ts) / DAY_MS);
}
function relative(ts) {
  const d = daysAgo(ts);
  return d < 1 ? 'today' : d === 1 ? 'yesterday' : `${d} days ago`;
}
function isOverdue(ts) {
  return ts == null || daysAgo(ts) >= INSPECTION_INTERVAL_DAYS;
}

const ICONS = {
  scan: '<svg viewBox="0 0 24 24"><path d="M3 11h8V3H3v8zm2-6h4v4H5V5zm-2 16h8v-8H3v8zm2-6h4v4H5v-4zm8-12v8h8V3h-8zm6 6h-4V5h4v4zm0 10h2v2h-2zm-6-6h2v2h-2zm2 2h2v2h-2zm-2 2h2v2h-2zm2 2h2v2h-2zm2-2h2v2h-2zm0-4h2v2h-2zm2 2h2v2h-2z"/></svg>',
  person: '<svg viewBox="0 0 24 24"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>',
  add: '<svg viewBox="0 0 24 24"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>',
  menu: '<svg viewBox="0 0 24 24"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/></svg>',
  edit: '<svg viewBox="0 0 24 24"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>',
  qr: '<svg viewBox="0 0 24 24"><path d="M3 3h8v8H3V3zm2 2v4h4V5H5zm8-2h8v8h-8V3zm2 2v4h4V5h-4zM3 13h8v8H3v-8zm2 2v4h4v-4H5zm13-2h3v2h-3v-2zm-5 0h3v3h-3v-3zm3 3h2v2h-2v-2zm2 2h3v3h-3v-3zm-5 1h3v2h-3v-2z"/></svg>',
  check: '<svg viewBox="0 0 24 24"><path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm-2 15-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>',
  cross: '<svg viewBox="0 0 24 24"><path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z"/></svg>',
  torch: '<svg viewBox="0 0 24 24"><path d="M6 2h12v5l-2 3v12H8V10L6 7V2zm6 10a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3z"/></svg>',
  keyboard: '<svg viewBox="0 0 24 24"><path d="M20 5H4a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2zm-9 3h2v2h-2V8zm0 3h2v2h-2v-2zM8 8h2v2H8V8zm0 3h2v2H8v-2zm-1 2H5v-2h2v2zm0-3H5V8h2v2zm9 7H8v-2h8v2zm0-4h-2v-2h2v2zm0-3h-2V8h2v2zm3 3h-2v-2h2v2zm0-3h-2V8h2v2z"/></svg>',
};

function badge(status, ts) {
  if (!status) return '<span class="badge never">Never checked</span>';
  if (status === 'NOT_OK') return '<span class="badge notok">Not OK</span>';
  if (isOverdue(ts)) return '<span class="badge due">OK · check due</span>';
  return '<span class="badge ok">OK</span>';
}

function lv(label, value, cls = '') {
  return `<div><div class="lv-label">${esc(label)}</div><div class="lv-value ${cls}">${esc(value || '—')}</div></div>`;
}

let toastTimer;
function toast(msg) {
  const el = $('#toast');
  el.textContent = msg;
  el.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove('show'), 2600);
}

function go(hash) {
  location.hash = hash;
}

function vibrate() {
  try { navigator.vibrate?.(80); } catch { /* not supported */ }
}

// ---------------------------------------------------------------------------
// Modals
// ---------------------------------------------------------------------------

function openModal(html, onMount) {
  const root = $('#modal-root');
  root.innerHTML = `<div class="modal-backdrop"><div class="modal" role="dialog" aria-modal="true">${html}</div></div>`;
  const backdrop = root.firstElementChild;
  backdrop.addEventListener('click', (e) => { if (e.target === backdrop) closeModal(); });
  $$('[data-close]', root).forEach((b) => b.addEventListener('click', closeModal));
  onMount?.(root);
  $('input, textarea, button', root)?.focus();
}

function closeModal() {
  $('#modal-root').innerHTML = '';
}

function promptInspector(onDone) {
  openModal(`
    <h3>Operator / inspector name</h3>
    <form id="name-form">
      <input class="input" name="name" placeholder="Your name or staff ID" value="${esc(db.inspector)}" autocomplete="name" required>
      <div class="modal-actions">
        <button type="button" class="btn subtle" data-close>Cancel</button>
        <button class="btn">Save</button>
      </div>
    </form>`, (root) => {
    $('#name-form', root).addEventListener('submit', (e) => {
      e.preventDefault();
      const name = e.target.name.value.trim();
      if (!name) return;
      db.inspector = name;
      save();
      closeModal();
      onDone ? onDone() : render();
    });
  });
}

function confirmModal(title, text, confirmLabel, onConfirm) {
  openModal(`
    <h3>${esc(title)}</h3><p>${esc(text)}</p>
    <div class="modal-actions">
      <button class="btn subtle" data-close>Cancel</button>
      <button class="btn" id="confirm-btn">${esc(confirmLabel)}</button>
    </div>`, (root) => {
    $('#confirm-btn', root).addEventListener('click', () => { closeModal(); onConfirm(); });
  });
}

// ---------------------------------------------------------------------------
// QR labels
// ---------------------------------------------------------------------------

/** Renders a printable label (QR + name + code + location) to a canvas. */
function labelCanvas(ext) {
  qrcode.stringToBytes = qrcode.stringToBytesFuncs['UTF-8'];
  const qr = qrcode(0, 'H'); // high error correction: labels get dirty and scratched
  qr.addData(ext.code, 'Byte');
  qr.make();

  const modules = qr.getModuleCount();
  const qrSize = 600;
  const cell = Math.floor(qrSize / (modules + 2));
  const qrPx = cell * (modules + 2);
  const padding = 40;
  const lines = [
    [ext.name, 'bold 44px system-ui, sans-serif', 44],
    [ext.code, '36px system-ui, sans-serif', 36],
    [ext.location, '32px system-ui, sans-serif', 32],
  ].filter((l) => l[0] && l[0].trim());
  const textHeight = lines.reduce((h, l) => h + l[2] * 1.4, 0);
  const width = qrPx + padding * 2;

  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = Math.ceil(qrPx + padding * 2 + textHeight);
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = '#fff';
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.fillStyle = '#000';
  for (let r = 0; r < modules; r++) {
    for (let c = 0; c < modules; c++) {
      if (qr.isDark(r, c)) ctx.fillRect(padding + (c + 1) * cell, padding + (r + 1) * cell, cell, cell);
    }
  }
  ctx.textAlign = 'center';
  let y = padding + qrPx;
  for (const [text, font, size] of lines) {
    ctx.font = font;
    y += size * 1.4;
    ctx.fillText(ellipsize(ctx, text, width - padding * 2), width / 2, y - size * 0.3);
  }
  return canvas;
}

function ellipsize(ctx, text, maxWidth) {
  if (ctx.measureText(text).width <= maxWidth) return text;
  let end = text.length;
  while (end > 0 && ctx.measureText(text.slice(0, end) + '…').width > maxWidth) end--;
  return text.slice(0, end) + '…';
}

function showLabel(ext) {
  const url = labelCanvas(ext).toDataURL('image/png');
  const fileName = `qr_${ext.code.replace(/[^A-Za-z0-9_-]/g, '_')}.png`;
  openModal(`
    <h3>QR label</h3>
    <img class="label-preview" src="${url}" alt="QR label for ${esc(ext.code)}">
    <div class="modal-actions">
      <button class="btn subtle" data-close>Close</button>
      <a class="btn subtle" href="${url}" download="${esc(fileName)}">Download</a>
      <button class="btn" id="print-btn">Print</button>
    </div>`, (root) => {
    $('#print-btn', root).addEventListener('click', () => {
      $('#print-area').innerHTML = `<img src="${url}" alt="">`;
      window.print();
    });
  });
}

// ---------------------------------------------------------------------------
// Export / backup
// ---------------------------------------------------------------------------

function download(fileName, content, type) {
  const blob = new Blob([content], { type });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(a.href), 1000);
}

function csvField(v) {
  const s = String(v ?? '');
  return /[",\r\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
}

function exportCsv() {
  const rows = [['Date', 'Extinguisher code', 'Name', 'Location', 'Type', 'Inspector', 'Result', 'Conditions', 'Notes']];
  for (const i of [...db.inspections].sort((a, b) => b.ts - a.ts)) {
    const e = getExt(i.code) || {};
    rows.push([
      fmtDateTime(i.ts), i.code, e.name, e.location, e.type, i.inspector,
      i.status === 'OK' ? 'OK' : 'Not OK',
      (i.issues || []).map((k) => ISSUE_LABEL[k] || k).join('; '),
      i.notes,
    ]);
  }
  // BOM so Excel opens UTF-8 (e.g. Arabic names) correctly
  const csv = '﻿' + rows.map((r) => r.map(csvField).join(',')).join('\r\n') + '\r\n';
  download(`extinguisher_survey_${fmtDate(Date.now())}.csv`, csv, 'text/csv;charset=utf-8');
}

function exportBackup() {
  download(`extinguisher_survey_backup_${fmtDate(Date.now())}.json`, JSON.stringify(db, null, 2), 'application/json');
}

function importBackup() {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = '.json,application/json';
  input.addEventListener('change', async () => {
    const file = input.files?.[0];
    if (!file) return;
    try {
      const data = validateDb(JSON.parse(await file.text()));
      const count = Object.keys(data.extinguishers).length;
      confirmModal('Restore backup?',
        `This replaces all data on this device with the backup (${count} extinguishers, ${data.inspections.length} surveys).`,
        'Restore', () => {
          data.inspector = db.inspector || data.inspector;
          db = data;
          save();
          toast('Backup restored');
          go('#/');
          render();
        });
    } catch (e) {
      toast(`Could not read backup: ${e.message}`);
    }
  });
  input.click();
}

function showMenu() {
  openModal(`
    <h3>Data</h3>
    <div class="menu-list">
      <button class="btn subtle" id="m-csv">Export surveys to CSV (Excel)</button>
      <button class="btn subtle" id="m-backup">Download full backup (.json)</button>
      <button class="btn subtle" id="m-restore">Restore from backup…</button>
    </div>
    <p class="small muted">Data is stored only in this browser on this device. Use backup / restore to move it to another device or browser.</p>
    <div class="modal-actions"><button class="btn subtle" data-close>Close</button></div>`, (root) => {
    $('#m-csv', root).addEventListener('click', () => { closeModal(); exportCsv(); });
    $('#m-backup', root).addEventListener('click', () => { closeModal(); exportBackup(); });
    $('#m-restore', root).addEventListener('click', () => { closeModal(); importBackup(); });
  });
}

// ---------------------------------------------------------------------------
// Views
// ---------------------------------------------------------------------------

let cleanup = null;      // called when leaving the current view (e.g. stop the camera)
let homeState = { query: '', filter: 'ALL' };

function setChrome({ title, subtitle = '', back = false, actions = '' }) {
  $('#title').textContent = title;
  $('#subtitle').textContent = subtitle;
  $('#back-btn').hidden = !back;
  $('#appbar-actions').innerHTML = actions;
  document.title = title === 'Fire Extinguisher Survey' ? title : `${title} – Extinguisher Survey`;
}

function viewHome(view) {
  setChrome({
    title: 'Fire Extinguisher Survey',
    subtitle: db.inspector ? `Operator: ${db.inspector}` : 'Tap the person icon to set your name',
    actions: `
      <button class="icon-btn" id="a-name" aria-label="Set operator name" title="Operator name">${ICONS.person}</button>
      <button class="icon-btn" id="a-add" aria-label="Register extinguisher" title="Register extinguisher">${ICONS.add}</button>
      <button class="icon-btn" id="a-menu" aria-label="Export and backup" title="Export / backup">${ICONS.menu}</button>`,
  });

  const last = lastInspectionMap();
  const all = Object.values(db.extinguishers).sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }));
  const notOk = all.filter((e) => last[e.code]?.status === 'NOT_OK').length;
  const due = all.filter((e) => last[e.code]?.status !== 'NOT_OK' && isOverdue(last[e.code]?.ts)).length;

  view.innerHTML = `
    <div class="stats">
      <div class="stat"><b>${all.length}</b>Total</div>
      <div class="stat"><b>${notOk}</b>Not OK</div>
      <div class="stat"><b>${due}</b>Due</div>
    </div>
    <input class="search" id="search" type="search" placeholder="Search name, code or location" value="${esc(homeState.query)}">
    <div class="chips">
      ${[['ALL', 'All'], ['NOT_OK', 'Not OK'], ['DUE', 'Due / never']].map(([k, l]) =>
        `<button class="chip" data-filter="${k}" aria-pressed="${homeState.filter === k}">${l}</button>`).join('')}
    </div>
    <div id="list"></div>
    <button class="btn fab" id="scan-fab">${ICONS.scan} Scan QR code</button>`;

  const renderList = () => {
    const q = homeState.query.trim().toLowerCase();
    const items = all.filter((e) => {
      const l = last[e.code];
      const matchesQuery = !q || [e.code, e.name, e.location, e.type].some((v) => (v || '').toLowerCase().includes(q));
      const matchesFilter = homeState.filter === 'ALL'
        || (homeState.filter === 'NOT_OK' && l?.status === 'NOT_OK')
        || (homeState.filter === 'DUE' && isOverdue(l?.ts));
      return matchesQuery && matchesFilter;
    });
    const list = $('#list', view);
    if (!all.length) {
      list.innerHTML = `<div class="empty">No extinguishers registered yet.<br><br>Scan an extinguisher's QR code to register it, or tap + to add one and print its QR label.</div>`;
      return;
    }
    if (!items.length) {
      list.innerHTML = '<div class="empty">No matches</div>';
      return;
    }
    list.innerHTML = items.map((e) => {
      const l = last[e.code];
      const issues = l?.status === 'NOT_OK' && l.issues?.length
        ? `<div class="small error-text">${esc(l.issues.map((k) => ISSUE_LABEL[k] || k).join(', '))}</div>` : '';
      return `
        <a class="card list-item" href="#/e/${encodeURIComponent(e.code)}">
          <div class="row">
            <div style="min-width:0;flex:1">
              <div class="item-name">${esc(e.name)}</div>
              <div class="item-sub">${esc([e.code, e.location].filter(Boolean).join(' · '))}</div>
            </div>
            ${badge(l?.status, l?.ts)}
          </div>
          <div class="small" style="margin-top:6px">${l ? `Last check: ${fmtDate(l.ts)} (${relative(l.ts)})` : 'Last check: never'}</div>
          ${issues}
        </a>`;
    }).join('');
  };
  renderList();

  $('#search', view).addEventListener('input', (e) => { homeState.query = e.target.value; renderList(); });
  $$('.chip', view).forEach((chip) => chip.addEventListener('click', () => {
    homeState.filter = chip.dataset.filter;
    $$('.chip', view).forEach((c) => c.setAttribute('aria-pressed', c === chip));
    renderList();
  }));
  $('#scan-fab', view).addEventListener('click', () => go('#/scan'));
  $('#a-name').addEventListener('click', () => promptInspector());
  $('#a-add').addEventListener('click', () => go('#/new'));
  $('#a-menu').addEventListener('click', showMenu);
}

function viewScan(view) {
  setChrome({ title: 'Scan extinguisher QR', back: true });
  view.innerHTML = `
    <div class="scanner">
      <video id="video" playsinline muted></video>
      <div class="frame"></div>
      <div class="scan-msg" id="scan-msg">Starting camera…</div>
    </div>
    <div class="scanner-tools">
      <button class="btn subtle" id="torch-btn" hidden>${ICONS.torch} Flashlight</button>
      <button class="btn subtle" id="manual-btn">${ICONS.keyboard} Enter code</button>
    </div>
    <form id="manual-form" class="card" hidden>
      <label class="field"><span>Code printed under the QR</span>
        <input class="input" name="code" autocomplete="off" autocapitalize="characters" required></label>
      <button class="btn block">Open</button>
    </form>`;

  let stream = null;
  let stopped = false;
  let torchOn = false;
  const video = $('#video', view);
  const msg = $('#scan-msg', view);
  const canvas = document.createElement('canvas');
  const ctx = canvas.getContext('2d', { willReadFrequently: true });
  let detector = null;

  const stop = () => {
    stopped = true;
    stream?.getTracks().forEach((t) => t.stop());
  };
  cleanup = stop;

  const found = (raw) => {
    const code = String(raw || '').trim();
    if (!code || stopped) return;
    stop();
    vibrate();
    location.replace(`#/e/${encodeURIComponent(code)}`);
  };

  $('#manual-btn', view).addEventListener('click', () => {
    const form = $('#manual-form', view);
    form.hidden = false;
    form.code.focus();
  });
  $('#manual-form', view).addEventListener('submit', (e) => {
    e.preventDefault();
    found(e.target.code.value);
  });

  const tick = async () => {
    if (stopped) return;
    if (video.readyState >= 2 && video.videoWidth) {
      try {
        if (detector) {
          const codes = await detector.detect(video);
          const hit = codes.find((c) => c.rawValue);
          if (hit) return found(hit.rawValue);
        } else {
          // Downscale for speed; QR labels are large in frame.
          const scale = Math.min(1, 640 / Math.max(video.videoWidth, video.videoHeight));
          canvas.width = Math.round(video.videoWidth * scale);
          canvas.height = Math.round(video.videoHeight * scale);
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
          const img = ctx.getImageData(0, 0, canvas.width, canvas.height);
          const hit = jsQR(img.data, img.width, img.height, { inversionAttempts: 'dontInvert' });
          if (hit?.data) return found(hit.data);
        }
      } catch (e) {
        console.warn('Scan error', e);
      }
    }
    requestAnimationFrame(tick);
  };

  (async () => {
    if (!navigator.mediaDevices?.getUserMedia) {
      msg.textContent = window.isSecureContext
        ? 'This browser cannot use the camera. Enter the code manually.'
        : 'Camera needs HTTPS. Open the app over https:// or enter the code manually.';
      return;
    }
    try {
      if ('BarcodeDetector' in window) {
        const formats = await BarcodeDetector.getSupportedFormats();
        if (formats.includes('qr_code')) detector = new BarcodeDetector({ formats: ['qr_code'] });
      }
    } catch { detector = null; }
    try {
      stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 } },
        audio: false,
      });
      if (stopped) { stream.getTracks().forEach((t) => t.stop()); return; }
      video.srcObject = stream;
      await video.play();
      msg.textContent = 'Point the camera at the QR label on the extinguisher';
      const track = stream.getVideoTracks()[0];
      if (track?.getCapabilities?.().torch) {
        const btn = $('#torch-btn', view);
        btn.hidden = false;
        btn.addEventListener('click', async () => {
          torchOn = !torchOn;
          try { await track.applyConstraints({ advanced: [{ torch: torchOn }] }); } catch { torchOn = !torchOn; }
        });
      }
      requestAnimationFrame(tick);
    } catch (e) {
      msg.textContent = e.name === 'NotAllowedError'
        ? 'Camera permission denied. Allow camera access in browser settings, or enter the code manually.'
        : `Could not start camera (${e.name || e.message}). Enter the code manually.`;
    }
  })();
}

function viewDetail(view, code) {
  const ext = getExt(code);
  if (!ext) {
    setChrome({ title: 'Unknown extinguisher', back: true });
    view.innerHTML = `
      <div class="card empty">
        <h2 class="error-text">Unknown extinguisher</h2>
        <p>No extinguisher is registered with code:<br><b>"${esc(code)}"</b></p>
        <a class="btn block" href="#/new/${encodeURIComponent(code)}">Register this extinguisher</a>
      </div>`;
    return;
  }

  const past = inspectionsFor(code);
  const last = past[0];
  setChrome({
    title: ext.name,
    subtitle: ext.code,
    back: true,
    actions: `
      <button class="icon-btn" id="a-qr" aria-label="Show QR label" title="QR label">${ICONS.qr}</button>
      <button class="icon-btn" id="a-edit" aria-label="Edit" title="Edit">${ICONS.edit}</button>`,
  });

  let lastHtml;
  if (!last) {
    lastHtml = '<p>This extinguisher has never been surveyed.</p>';
  } else {
    const condition = last.status === 'OK'
      ? 'OK – no defects found'
      : (last.issues || []).map((k) => `• ${ISSUE_LABEL[k] || k}`).join('\n');
    lastHtml = `
      <div class="grid2">
        ${lv('Date', `${fmtDateTime(last.ts)}\n(${relative(last.ts)})`)}
        ${lv('Checked by', last.inspector)}
      </div>
      <div style="margin-top:10px">${lv('Condition', condition, last.status === 'OK' ? 'ok-text' : 'error-text')}</div>
      ${last.notes ? `<div style="margin-top:10px">${lv('Notes', last.notes)}</div>` : ''}
      ${isOverdue(last.ts) ? `<p class="small error-text">Monthly check is due (last check over ${INSPECTION_INTERVAL_DAYS} days ago).</p>` : ''}`;
  }

  view.innerHTML = `
    <div class="card">
      <h2>${esc(ext.name)}</h2>
      <div class="grid2">
        ${lv('Code', ext.code)}${lv('Location', ext.location)}
        ${lv('Type', ext.type)}${lv('Capacity', ext.capacity)}
      </div>
    </div>
    <div class="card ${last ? (last.status === 'OK' ? 'tint-ok' : 'tint-notok') : ''}">
      <div class="row"><h3 class="spacer">Last check</h3>${badge(last?.status, last?.ts)}</div>
      ${lastHtml}
    </div>
    <a class="btn block big" href="#/survey/${encodeURIComponent(code)}">${ICONS.check} Start survey</a>
    ${past.length > 1 ? `
      <div class="card" style="margin-top:16px">
        <h3>Inspection history</h3>
        ${past.slice(1).map((i) => `
          <div class="history-item">
            <div class="row">
              <div class="spacer"><b>${fmtDateTime(i.ts)}</b><div class="small muted">${esc(i.inspector || '—')}</div></div>
              ${badge(i.status, Date.now())}
            </div>
            ${i.issues?.length ? `<div class="small error-text">${esc(i.issues.map((k) => ISSUE_LABEL[k] || k).join(', '))}</div>` : ''}
            ${i.notes ? `<div class="small">${esc(i.notes)}</div>` : ''}
          </div>`).join('')}
      </div>` : ''}`;

  $('#a-qr').addEventListener('click', () => showLabel(ext));
  $('#a-edit').addEventListener('click', () => go(`#/edit/${encodeURIComponent(code)}`));
}

function viewSurvey(view, code) {
  const ext = getExt(code);
  if (!ext) return viewDetail(view, code);
  setChrome({ title: 'Survey', subtitle: ext.name, back: true });

  const last = inspectionsFor(code)[0];
  const lastText = last
    ? `Last check: ${fmtDateTime(last.ts)} – ${last.status === 'OK' ? 'OK' : 'Not OK'}` +
      (last.issues?.length ? ` (${last.issues.map((k) => ISSUE_LABEL[k] || k).join(', ')})` : '')
    : 'Last check: never';

  view.innerHTML = `
    <div class="card">
      <h2>${esc(ext.name)}</h2>
      <div class="muted">${esc([ext.code, ext.location].filter(Boolean).join(' · '))}</div>
      <div class="small" style="margin-top:6px">${esc(lastText)}</div>
    </div>
    <form id="survey-form" novalidate>
      <h3>Condition of the extinguisher</h3>
      <div class="status-choice">
        <button type="button" class="status-btn ok" data-status="OK" aria-pressed="false">${ICONS.check} OK</button>
        <button type="button" class="status-btn notok" data-status="NOT_OK" aria-pressed="false">${ICONS.cross} NOT OK</button>
      </div>
      <div id="issues-block" hidden>
        <h3>What is wrong? <span class="small muted">(select all that apply)</span></h3>
        <div class="card">
          ${ISSUES.map(([k, l]) => `<label class="issue"><input type="checkbox" name="issue" value="${k}"> ${esc(l)}</label>`).join('')}
        </div>
      </div>
      <label class="field"><span id="notes-label">Notes (optional)</span>
        <textarea class="input" name="notes"></textarea></label>
      <label class="field"><span>Inspected by</span>
        <input class="input" name="inspector" value="${esc(db.inspector)}" placeholder="Your name or staff ID" autocomplete="name"></label>
      <button class="btn block big" id="save-btn" disabled>Save survey</button>
    </form>`;

  const form = $('#survey-form', view);
  let status = null;

  const selectedIssues = () => $$('input[name="issue"]:checked', form).map((i) => i.value);
  const validate = () => {
    const issues = selectedIssues();
    const needsNotes = status === 'NOT_OK' && issues.includes('OTHER');
    const notesMissing = needsNotes && !form.notes.value.trim();
    $('#notes-label', view).textContent = needsNotes ? 'Notes (required for "Other")' : 'Notes (optional)';
    form.notes.classList.toggle('invalid', notesMissing);
    form.inspector.classList.toggle('invalid', !form.inspector.value.trim());
    const ok = !!form.inspector.value.trim() && (status === 'OK' || (status === 'NOT_OK' && issues.length > 0 && !notesMissing));
    $('#save-btn', view).disabled = !ok;
    return ok;
  };

  $$('.status-btn', form).forEach((btn) => btn.addEventListener('click', () => {
    status = btn.dataset.status;
    $$('.status-btn', form).forEach((b) => b.setAttribute('aria-pressed', b === btn));
    $('#issues-block', view).hidden = status !== 'NOT_OK';
    validate();
  }));
  form.addEventListener('input', validate);
  form.addEventListener('change', validate);

  form.addEventListener('submit', (e) => {
    e.preventDefault();
    if (!validate()) return;
    const inspector = form.inspector.value.trim();
    db.inspector = inspector;
    db.inspections.push({
      id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      code,
      ts: Date.now(),
      inspector,
      status,
      issues: status === 'OK' ? [] : ISSUES.map(([k]) => k).filter((k) => selectedIssues().includes(k)),
      notes: form.notes.value.trim(),
    });
    if (save()) {
      vibrate();
      toast('Survey saved');
      history.back();
    }
  });
}

function viewEdit(view, code, isNew) {
  const existing = isNew ? null : getExt(code);
  if (!isNew && !existing) return viewDetail(view, code);
  setChrome({ title: isNew ? 'Register extinguisher' : 'Edit extinguisher', back: true });
  const e = existing || { code, name: '', location: '', type: TYPES[0], capacity: '' };

  view.innerHTML = `
    <form id="edit-form" class="card" novalidate>
      <label class="field"><span>QR code / ID *</span>
        <input class="input" name="code" value="${esc(e.code)}" ${isNew ? '' : 'disabled'} autocomplete="off" autocapitalize="characters">
        <div class="hint">${isNew ? 'Text encoded in the QR label, e.g. FE-TURB-001' : 'The QR code cannot be changed'}</div></label>
      <label class="field"><span>Name *</span>
        <input class="input" name="name" value="${esc(e.name)}" placeholder="e.g. Turbine hall – extinguisher 3"></label>
      <label class="field"><span>Location</span>
        <input class="input" name="location" value="${esc(e.location)}" placeholder="e.g. Unit 2, Level 0, near switchgear"></label>
      <label class="field"><span>Type</span>
        <select class="input" name="type">
          ${TYPES.map((t) => `<option ${t === e.type ? 'selected' : ''}>${esc(t)}</option>`).join('')}
        </select></label>
      <label class="field"><span>Capacity</span>
        <input class="input" name="capacity" value="${esc(e.capacity)}" placeholder="e.g. 6 kg"></label>
      <p class="error-text" id="edit-error" hidden></p>
      <button class="btn block big" id="edit-save">Save</button>
      ${isNew ? '<p class="small muted">After saving, open the extinguisher and tap the QR icon to download or print its label.</p>' : ''}
    </form>
    ${isNew ? '' : '<button class="btn danger block" id="delete-btn">Delete extinguisher</button>'}`;

  const form = $('#edit-form', view);
  const validate = () => {
    const ok = !!form.code.value.trim() && !!form.name.value.trim();
    $('#edit-save', view).disabled = !ok;
    return ok;
  };
  validate();
  form.addEventListener('input', validate);

  form.addEventListener('submit', (ev) => {
    ev.preventDefault();
    if (!validate()) return;
    const newCode = isNew ? form.code.value.trim() : e.code;
    if (isNew && getExt(newCode)) {
      const err = $('#edit-error', view);
      err.textContent = `An extinguisher with code "${newCode}" already exists`;
      err.hidden = false;
      return;
    }
    db.extinguishers[newCode] = {
      code: newCode,
      name: form.name.value.trim(),
      location: form.location.value.trim(),
      type: form.type.value,
      capacity: form.capacity.value.trim(),
      createdAt: e.createdAt || Date.now(),
    };
    if (!save()) return;
    toast(isNew ? 'Extinguisher registered' : 'Saved');
    if (isNew && newCode !== code) location.replace(`#/e/${encodeURIComponent(newCode)}`);
    else history.back(); // back to the detail page, which now shows the saved extinguisher
  });

  $('#delete-btn', view)?.addEventListener('click', () => {
    confirmModal('Delete extinguisher?', 'This also deletes its whole inspection history. This cannot be undone.', 'Delete', () => {
      delete db.extinguishers[e.code];
      db.inspections = db.inspections.filter((i) => i.code !== e.code);
      save();
      toast('Deleted');
      location.replace('#/');
    });
  });
}

// ---------------------------------------------------------------------------
// Router
// ---------------------------------------------------------------------------

function render() {
  cleanup?.();
  cleanup = null;
  closeModal();
  const view = $('#view');
  const parts = location.hash.replace(/^#\/?/, '').split('/');
  const arg = (i) => {
    try { return decodeURIComponent(parts.slice(i).join('/')); } catch { return parts.slice(i).join('/'); }
  };
  switch (parts[0]) {
    case 'scan': viewScan(view); break;
    case 'e': viewDetail(view, arg(1)); break;
    case 'survey': viewSurvey(view, arg(1)); break;
    case 'new': viewEdit(view, arg(1), true); break;
    case 'edit': viewEdit(view, arg(1), false); break;
    default: viewHome(view);
  }
  window.scrollTo(0, 0);
}

// Pages visited inside the app this session, so Back never leaves the app.
let depth = 0;
$('#back-btn').addEventListener('click', () => {
  if (depth > 0) history.back();
  else location.replace('#/');
});
window.addEventListener('hashchange', () => {
  depth = location.hash.replace(/^#\/?/, '') === '' ? 0 : depth + 1;
  render();
});
// Another tab changed the data
window.addEventListener('storage', (e) => { if (e.key === STORE_KEY) { db = load(); render(); } });
render();

if ('serviceWorker' in navigator && location.protocol !== 'file:') {
  navigator.serviceWorker.register('sw.js').catch((e) => console.warn('Service worker not registered', e));
}
