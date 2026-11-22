'use strict';

const shellScript = '/data/adb/modules/camera68_aurora_allinone/webroot/diagnostics.sh';
const operations = new Set(['status', 'save-camera', 'export-diagnostics']);
const previewOperations = new Set(['status', 'start', 'stop', 'export', 'notification-settings']);
const scopeNames = {
  compat: '相机',
  'live-motion': '实况运镜',
  editor: '相册编辑',
  'photo-editor': '相册图片编辑进程',
};
const mountNames = {
  palette: '调色盘预览插件',
  auth: 'AUTH 拍照滤镜库',
  'natural-graph': '原色预览管线',
  'natural-photo': '原色拍照后处理',
  night: 'ArcSoft 夜景检测',
  m3: 'M3 滤镜插件与配置',
  'com.qti.tuned.aurora_semco_LYT900_wide.bin': '原色主摄调校',
  'com.qti.tuned.aurora_semco_imx858_ultra.bin': '原色超广角调校',
  'com.qti.tuned.aurora_ofilm_imx858_tele3x.bin': '原色 3.2× 调校',
  'com.qti.tuned.aurora_semco_imx858_tele5x.bin': '原色 5× 调校',
};
const labels = {
  registered: ['已注册', 'pending'],
  ran: ['已触发', 'success'],
  ready: ['已就绪', 'success'],
  mounted: ['已挂载', 'success'],
  removed: ['已卸载', 'muted'],
  skipped: ['不适用', 'muted'],
  pending: ['待触发', 'pending'],
  waiting: ['待启动', 'pending'],
  off: ['已关闭', 'muted'],
  resolving: ['解析中', 'pending'],
  resolved: ['待注册', 'pending'],
  installing: ['注册中', 'pending'],
  'register-error': ['注册失败', 'error'],
  'resolve-error': ['解析失败', 'error'],
  'install-error': ['安装失败', 'error'],
  'callback-error': ['执行异常', 'error'],
  'mount-error': ['挂载不符', 'error'],
};
const scopeHints = {
  current: '',
  waiting: '等待本次进程记录',
  stale: '本次暂无记录 · 下方为上次进程',
  previous: '上次进程记录',
  stopped: '进程未启动',
  missing: '所需 APK 未安装',
  old: '进程仍在使用旧版 Hook',
};
const byId = id => document.getElementById(id);
const errorState = state => state.endsWith('-error');
let filter = 'all';
let snapshot;
let exporting = false;
let requestId = 0;

function exec(operation, preview = false) {
  if (!(preview ? previewOperations : operations).has(operation)) throw new Error('未知操作');
  if (!window.ksu || typeof window.ksu.exec !== 'function') {
    throw new Error('请从 KernelSU 或兼容的模块 WebUI 查看器打开。');
  }
  return new Promise((resolve, reject) => {
    const callback = 'auroraResult' + ++requestId;
    const timer = setTimeout(() => {
      delete window[callback];
      reject(new Error('读取超时，请重试。'));
    }, 20000);
    window[callback] = (code, stdout, stderr) => {
      clearTimeout(timer);
      delete window[callback];
      if (code === 0) resolve(stdout.trimEnd());
      else reject(new Error(stderr.trim() || '操作失败（' + code + '）'));
    };
    try {
      const script = preview ? shellScript.replace('diagnostics.sh', 'preview-diagnostics.sh') : shellScript;
      window.ksu.exec('/system/bin/sh ' + script + ' ' + operation, '{}', callback);
    } catch (error) {
      clearTimeout(timer);
      delete window[callback];
      reject(error);
    }
  });
}

function parse(text) {
  const result = {version: '', time: '', scopes: [], mounts: []};
  let current;
  for (const line of text.split('\n')) {
    const [type, a, b, c, d, e, f, g] = line.split('\t');
    switch (type) {
      case 'module':
        [result.version, result.time] = [a, b];
        break;
      case 'scope':
        current = {id: a, state: b, process: c, pid: d, version: e, groups: new Map(), points: new Map()};
        result.scopes.push(current);
        break;
      case 'group':
        current.groups.set(a, {state: b});
        break;
      case 'error':
        current.groups.set(a, {state: b, detail: c});
        break;
      case 'hook': {
        if (!c || !d || !e) break;
        const point = current.points.get(a) || {group: b, target: d, owner: e};
        if (c === 'registered') point.registered = true;
        if (c === 'ran') point.ran = true;
        if (c === 'removed') point.removed = true;
        if (errorState(c)) {
          point.error = c;
          point.detail = f || g || '';
        }
        point.state = point.error || (point.removed ? 'removed' : point.ran ? 'ran' : 'registered');
        current.points.set(a, point);
        break;
      }
      case 'mount':
        result.mounts.push({id: a, state: b, target: c, source: d});
        break;
      case 'preview': {
        const failed = a === 'native-error' || (['saved', 'stopped', 'incomplete', 'interrupted'].includes(a) && c !== 'ran');
        result.mounts.push({id: 'preview-native', state: failed ? 'mount-error' : a === 'off' ? 'off' : c,
          target: a === 'off' ? '从 WebUI 开始采集后触发' : previewStates[a]?.[0]});
        break;
      }
    }
  }
  if (!result.version || !result.scopes.length) throw new Error('模块未返回诊断状态。');
  return result;
}

function element(tag, text, className) {
  const node = document.createElement(tag);
  if (text) node.textContent = text;
  if (className) node.className = className;
  return node;
}

function badge(state) {
  const [label, color] = labels[state] || labels.waiting;
  return element('span', label, 'badge ' + color);
}

function notice(id, message, kind) {
  const node = byId(id);
  node.textContent = message;
  node.hidden = !message;
  node.className = 'notice' + (kind ? ' ' + kind : '');
}

function visible(state, points = []) {
  if (filter === 'error') return errorState(state) || points.some(point => errorState(point.state));
  if (filter === 'pending') return ['registered', 'pending', 'waiting', 'resolved'].includes(state)
    || points.some(point => point.state === 'registered');
  return true;
}

function showFeature(title, help) {
  byId('feature-title').textContent = title;
  byId('feature-description').textContent = help[0];
  byId('feature-steps').textContent = help[1];
  byId('feature-dialog').showModal();
}

function row(title, state, detail, points, key, open, help) {
  const item = element('div', '', 'row');
  const disclosure = element('details');
  disclosure.dataset.key = key;
  disclosure.open = open;
  const summary = element('summary');
  summary.append(element('span', title, 'row-title'), badge(state));
  disclosure.append(summary);
  item.append(disclosure);
  if (help) {
    const button = element('button', '', 'info-button');
    button.type = 'button';
    button.setAttribute('aria-label', title + '的功能说明');
    button.setAttribute('aria-haspopup', 'dialog');
    const icon = element('span', 'i', 'info-icon');
    icon.setAttribute('aria-hidden', 'true');
    button.append(icon);
    button.addEventListener('click', () => showFeature(title, help));
    item.append(button);
  }
  const content = element('div', '', 'point-list');
  disclosure.append(content);
  if (detail) content.append(element('p', detail, 'detail' + (errorState(state) ? ' failure' : '')));
  for (const point of points) {
    const view = element('div', '', 'point');
    view.append(element('code', point.target), badge(point.state));
    if (point.detail) view.append(element('span', point.detail, 'detail failure'));
    content.append(view);
  }
  if (!detail && !points.length) {
    const message = state === 'ready' ? '本组初始化已完成。'
      : state === 'skipped' ? '本进程或当前版本不使用此功能。'
      : '等待对应功能提供运行记录。';
    content.append(element('p', message, 'detail'));
  }
  if (errorState(state)) {
    const action = element('div', '', 'error-action');
    const button = element('button', '导出诊断日志', 'secondary');
    button.type = 'button';
    button.addEventListener('click', () => save(button, 'export-diagnostics', 'export-message'));
    action.append(button);
    content.append(action);
  }
  return item;
}

function render() {
  if (!snapshot) return;
  const output = byId('results');
  const open = new Set(Array.from(output.querySelectorAll('details[open]'), item => item.dataset.key));
  output.replaceChildren();
  let registered = 0;
  let ran = 0;
  let errors = 0;
  for (const scope of snapshot.scopes) {
    const section = element('section', '', 'scope');
    const heading = element('header', '', 'scope-heading');
    const info = [scope.version && 'v' + scope.version, scopeHints[scope.state], scope.pid && 'PID ' + scope.pid];
    heading.append(element('h3', scopeNames[scope.id]), element('span', info.filter(Boolean).join(' · ')));
    section.append(heading);
    const panel = element('div', '', 'panel');
    section.append(panel);
    const names = {...(window.auroraGroups?.[scope.id === 'photo-editor' ? 'editor' : scope.id] || {})};
    for (const name of scope.groups.keys()) if (!(name in names)) names[name] = name || '模块加载';
    const groupPoints = new Map();
    for (const point of scope.points.values()) {
      if (!(point.group in names)) names[point.group] = point.owner;
      if (!groupPoints.has(point.group)) groupPoints.set(point.group, []);
      groupPoints.get(point.group).push(point);
      if (scope.state === 'current') {
        if (point.registered && !point.removed) registered++;
        if (point.ran) ran++;
      }
    }
    for (const [id, title] of Object.entries(names)) {
      const info = scope.groups.get(id);
      const points = groupPoints.get(id) || [];
      const failed = points.find(point => point.error);
      const state = info?.state && errorState(info.state) ? info.state
        : failed ? failed.error
        : points.some(point => point.state === 'ran') ? 'ran'
        : points.some(point => point.state === 'registered') ? 'registered'
        : points.length ? 'removed'
        : info?.state === 'registered' ? 'ready'
        : info?.state || (scope.state === 'current' ? 'pending' : 'waiting');
      if (scope.state === 'current') {
        errors += points.filter(point => point.error).length;
        if (errorState(state) && !failed) errors++;
      }
      if (visible(state, points)) {
        const key = scope.id + '/' + id;
        panel.append(row(title, state, info?.detail, points, key, open.has(key), window.auroraHelp[id]));
      }
    }
    if (panel.childElementCount) output.append(section);
  }
  const native = element('section', '', 'scope');
  const heading = element('header', '', 'scope-heading');
  heading.append(element('h3', '底层相机组件'), element('span', '相机服务进程可见性'));
  native.append(heading);
  const panel = element('div', '', 'panel');
  for (const mount of snapshot.mounts) {
    if (errorState(mount.state)) errors++;
    if (visible(mount.state)) {
      const key = 'mount/' + mount.id;
      const detail = [mount.target, mount.source].filter(Boolean).join('\n');
      panel.append(row(mountNames[mount.id] || mount.id, mount.state, detail, [], key, open.has(key), window.auroraHelp[mount.id]));
    }
  }
  if (panel.childElementCount) {
    native.append(panel);
    output.append(native);
  }
  if (!output.childElementCount) output.append(element('p', '没有符合筛选条件的项目。', 'empty'));
  byId('registered').textContent = registered;
  byId('ran').textContent = ran;
  byId('errors').textContent = errors;
}

async function refresh() {
  const button = byId('refresh');
  button.disabled = true;
  notice('status-message', '');
  try {
    snapshot = parse(await exec('status'));
    byId('module-version').textContent = 'Xiaomi 14 Ultra · ' + snapshot.version;
    byId('last-refresh').textContent = '刷新于 ' + snapshot.time;
    render();
  } catch (error) {
    notice('status-message', error.message, 'error');
  } finally {
    button.disabled = false;
  }
}

async function save(button, operation, messageId, preview = false) {
  if (exporting) return;
  exporting = true;
  button.disabled = true;
  notice(messageId, '正在保存…');
  if (operation === 'export-diagnostics' && button.id !== 'export') {
    byId('export-message').scrollIntoView({block: 'center'});
  }
  try {
    const path = await exec(operation, preview);
    if (!path.startsWith('/storage/emulated/0/Download/')) throw new Error('模块未返回已保存的文件路径。');
    notice(messageId, '已保存到 Download/' + path.split('/').pop(), 'success');
  } catch (error) {
    notice(messageId, '保存失败 · ' + error.message, 'error');
  } finally {
    button.disabled = false;
    exporting = false;
  }
}

const previewStates = {
  off: ['已关闭', ''],
  starting: ['正在启动', ''],
  recording: ['采集中', 'success'],
  marked: ['异常已标记，正在保留后续记录', 'success'],
  saved: ['采集完成，可以导出', 'success'],
  stopped: ['已手动停止，可以导出', 'success'],
  incomplete: ['采集已结束，数据不完整，可以导出', 'error'],
  empty: ['采集已结束，本次没有记录到数据', ''],
  interrupted: ['采集意外中断', 'error'],
  'notification-error': ['通知未启用，请允许「6.8Cam原包适配」发送通知后重试', 'error'],
  'compat-error': ['当前 Compat 版本不支持这个采集方式，请使用整合包内的版本', 'error'],
  'native-error': ['底层采集启动失败，请导出模块诊断日志', 'error'],
  'capture-error': ['采集中断，请导出模块诊断日志', 'error'],
};
mountNames['preview-native'] = '预览逐帧底层调校采集';
let previewPhase = 'off';
let previewTimer;
function renderPreview(text) {
  const [phase, app, native, finish, appCount, nativeCount] = text.split('\t');
  let [label, color] = previewStates[phase];
  previewPhase = phase;
  const collecting = ['starting', 'recording', 'marked'].includes(phase);
  const hasData = Number(appCount) > 0 || Number(nativeCount) > 0;
  if (phase === 'incomplete' && finish === 'manual') label = '已手动停止，部分记录可导出';
  if (phase === 'empty' && finish === 'manual') label = '已手动停止，本次没有记录到数据';
  if (phase === 'interrupted') label += hasData ? '，可导出已记录的数据' : '，请导出模块诊断日志';
  if (finish === 'manual') color = phase === 'empty' ? '' : 'success';
  const detail = collecting ? ' · 相机' + (app === 'ran' ? '已记录' : '待取景')
    + ' · 底层' + (native === 'ran' ? '已记录' : native === 'registered' ? '已挂载' : '待挂载')
    : appCount === undefined ? '' : ' · 相机 ' + appCount + ' 帧 · 底层 ' + nativeCount + ' 条';
  notice('preview-state', label + detail, color);
  byId('preview-toggle').textContent = collecting ? '停止采集' : '开始采集';
  byId('preview-toggle').disabled = phase === 'starting' || phase === 'marked';
  byId('preview-export').disabled = !hasData;
  byId('preview-permission').hidden = phase !== 'notification-error';
  clearTimeout(previewTimer);
  if (collecting && !document.hidden) previewTimer = setTimeout(() => updatePreview('status'), 2000);
}

async function updatePreview(operation) {
  clearTimeout(previewTimer);
  try {
    renderPreview(await exec(operation, true));
  } catch (error) {
    notice('preview-message', error.message, 'error');
    byId('preview-toggle').disabled = false;
  }
}

byId('preview-toggle').addEventListener('click', event => {
  notice('preview-message', '');
  event.currentTarget.disabled = true;
  updatePreview(previewPhase === 'recording' ? 'stop' : 'start');
});
byId('preview-export').addEventListener('click', event => save(event.currentTarget, 'export', 'preview-message', true));
byId('preview-permission').addEventListener('click', async () => {
  try { await exec('notification-settings', true); }
  catch (error) { notice('preview-message', error.message, 'error'); }
});
document.addEventListener('visibilitychange', () => {
  if (document.hidden) clearTimeout(previewTimer);
  else updatePreview('status');
});
byId('refresh').addEventListener('click', refresh);
byId('close-feature').addEventListener('click', () => byId('feature-dialog').close());
byId('save-camera').addEventListener('click', event => save(event.currentTarget, 'save-camera', 'camera-message'));
byId('export').addEventListener('click', event => save(event.currentTarget, 'export-diagnostics', 'export-message'));
for (const button of document.querySelectorAll('[data-filter]')) {
  button.addEventListener('click', () => {
    filter = button.dataset.filter;
    for (const item of document.querySelectorAll('[data-filter]')) {
      item.setAttribute('aria-pressed', String(item === button));
    }
    render();
  });
}
refresh();
updatePreview('status');
