'use strict';

const Auth = {
  _token:    null,
  _username: null,
  _role:     null,

  init() {
    this._token    = sessionStorage.getItem('cityorbit_token');
    this._username = sessionStorage.getItem('cityorbit_user');
    this._role     = sessionStorage.getItem('cityorbit_role');
    this._updateUI();
  },

  isAuthenticated() { return !!this._token; },
  getToken()        { return this._token; },

  _authHeaders() {
    const h = { 'Content-Type': 'application/json' };
    if (this._token) h['Authorization'] = `Bearer ${this._token}`;
    return h;
  },

  async login() {
    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value;
    const errEl    = document.getElementById('loginError');
    const btn      = document.getElementById('loginBtn');

    if (!username || !password) { errEl.textContent = 'Preencha usuário e senha.'; errEl.style.display='block'; return; }
    errEl.style.display = 'none';
    btn.disabled = true; btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Entrando…';

    try {
      const r = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      const data = await r.json();
      if (!r.ok) throw new Error(data.message || 'Credenciais inválidas');

      this._token    = data.token;
      this._username = data.username;
      this._role     = data.role;
      sessionStorage.setItem('cityorbit_token',    this._token);
      sessionStorage.setItem('cityorbit_user',     this._username);
      sessionStorage.setItem('cityorbit_role',     this._role);

      this._updateUI();
      this.closeModal();
      Toast.show(`Bem-vindo, ${this._username}!`, 'success');
    } catch(e) {
      errEl.textContent = e.message; errEl.style.display = 'block';
    } finally {
      btn.disabled = false; btn.innerHTML = '<i class="fas fa-sign-in-alt"></i> Entrar';
    }
  },

  logout() {
    this._token = this._username = this._role = null;
    sessionStorage.removeItem('cityorbit_token');
    sessionStorage.removeItem('cityorbit_user');
    sessionStorage.removeItem('cityorbit_role');
    this._updateUI();
    Toast.show('Sessão encerrada.', 'info');
  },

  toggleModal() {
    if (this.isAuthenticated()) {
      if (confirm(`Sair da conta ${this._username}?`)) this.logout();
    } else {
      this.openModal();
    }
  },

  openModal() {
    document.getElementById('loginUsername').value = '';
    document.getElementById('loginPassword').value = '';
    document.getElementById('loginError').style.display = 'none';
    document.getElementById('loginModal').classList.add('open');
    setTimeout(() => document.getElementById('loginUsername').focus(), 100);
  },

  closeModal() { document.getElementById('loginModal').classList.remove('open'); },

  requireAuth(action) {
    if (this.isAuthenticated()) { action(); return; }
    Toast.show('Esta operação requer autenticação.', 'info');
    this.openModal();
  },

  _updateUI() {
    const el = document.getElementById('authStatus');
    if (!el) return;
    if (this.isAuthenticated()) {
      el.className = 'auth-status logged-in';
      el.innerHTML = `<i class="fas fa-lock"></i> ${this._username} (${this._role})`;
      el.title = 'Clique para sair';
    } else {
      el.className = 'auth-status logged-out';
      el.innerHTML = '<i class="fas fa-lock-open"></i> Entrar';
      el.title = 'Clique para autenticar e habilitar escrita';
    }
  }
};

const API = {
  async get(path) {
    const r = await fetch(path);
    if (!r.ok) throw new Error(await r.text());
    return r.json();
  },
  async post(path, body) {
    const r = await fetch(path, {
      method: 'POST',
      headers: Auth._authHeaders(),
      body: JSON.stringify(body)
    });
    if (r.status === 401) { Auth.openModal(); throw new Error('Autenticação necessária'); }
    if (!r.ok) { const t = await r.text(); throw new Error(t); }
    return r.status === 204 ? null : r.json();
  },
  async put(path, body) {
    const r = await fetch(path, {
      method: 'PUT',
      headers: Auth._authHeaders(),
      body: JSON.stringify(body)
    });
    if (r.status === 401) { Auth.openModal(); throw new Error('Autenticação necessária'); }
    if (!r.ok) { const t = await r.text(); throw new Error(t); }
    return r.json();
  },
  async delete(path) {
    const r = await fetch(path, {
      method: 'DELETE',
      headers: Auth._authHeaders()
    });
    if (r.status === 401) { Auth.openModal(); throw new Error('Autenticação necessária'); }
    if (!r.ok) throw new Error(await r.text());
  },
  async soap(xml) {
    const r = await fetch('/ws', {
      method: 'POST',
      headers: { 'Content-Type': 'text/xml; charset=utf-8', ...(Auth._token ? { 'Authorization': `Bearer ${Auth._token}` } : {}) },
      body: xml
    });
    return r.text();
  }
};

function buildCorridor(lat1, lng1, lat2, lng2, widthRatio) {
  const dLat = lat2 - lat1;
  const dLng = lng2 - lng1;
  const len  = Math.sqrt(dLat * dLat + dLng * dLng) || 0.001;
  const w    = Math.max(0.0008, len * (widthRatio ?? 0.12));
  const pLat = (-dLng / len) * w;
  const pLng = ( dLat / len) * w;
  return [
    [lat1 + pLat, lng1 + pLng],
    [lat2 + pLat, lng2 + pLng],
    [lat2 - pLat, lng2 - pLng],
    [lat1 - pLat, lng1 - pLng]
  ];
}

function calcIntensity(simType, params) {
  if (!params || typeof params !== 'object') return 0.3;
  const n = (v, min, max) => Math.min(1, Math.max(0, (parseFloat(v || 0) - min) / (max - min)));
  switch (simType) {
    case 'FLOOD': {
      const rainScore = n(params.rain, 10, 300);
      const durScore  = n(params.dur,  1,  24);
      return Math.sqrt(rainScore * durScore) * 0.7 + Math.min(rainScore, durScore) * 0.3;
    }
    case 'TRAFFIC': {
      const roadWeights = {
        'Via expressa / Corredor BRT': 1.0,
        'Avenida principal / Radial':  0.75,
        'Via coletora / Comercial':    0.50,
        'Via local / Rua comum':       0.25
      };
      const roadScore = roadWeights[params.road_type] ?? 0.5;
      const peakStr   = String(params.peak || '');
      const peakMult  = (peakStr.includes('06:00') || peakStr.includes('17:00')) ? 1.0
                      : peakStr.includes('11:00') ? 0.7 : 0.4;
      return Math.min(1, roadScore * peakMult + roadScore * 0.15);
    }
    case 'CONSTRUCTION': {
      const typeWeights = {
        'Metrô / Linha de trem':    1.0,
        'Viaduto / Ponte':          0.85,
        'Saneamento e Tubulação':   0.70,
        'Pavimentação':             0.50,
        'Edifício comercial':       0.65,
        'Edifício residencial':     0.50,
        'Parque / Área verde':      0.30
      };
      const typeW  = typeWeights[params.type] ?? 0.5;
      const mScore = n(params.months, 1, 120);
      return typeW * 0.6 + mScore * 0.4;
    }
    case 'HEAT_ISLAND': {
      const aft = parseFloat(params.afternoon) || 30;
      return Math.min(1, Math.max(0, (aft - 18) / 24));
    }
    default: return 0.3;
  }
}

function lerpHex(a, b, t) {
  const ai = parseInt(a.slice(1), 16), bi = parseInt(b.slice(1), 16);
  const ar = (ai >> 16) & 0xff, ag = (ai >> 8) & 0xff, ab = ai & 0xff;
  const br = (bi >> 16) & 0xff, bg = (bi >> 8) & 0xff, bb = bi & 0xff;
  const r = Math.round(ar + (br - ar) * t).toString(16).padStart(2, '0');
  const g = Math.round(ag + (bg - ag) * t).toString(16).padStart(2, '0');
  const bx= Math.round(ab + (bb - ab) * t).toString(16).padStart(2, '0');
  return `#${r}${g}${bx}`;
}
function intensityColor(score) {
  const s = Math.min(1, Math.max(0, score));
  if (s < 0.5) return lerpHex('#22c55e', '#eab308', s * 2);
  return       lerpHex('#eab308', '#ef4444', (s - 0.5) * 2);
}
function intensityLabel(score) {
  if (score < 0.33) return 'Baixo';
  if (score < 0.66) return 'Moderado';
  return 'Alto';
}

const MapPicker = {
  _map: null, _markerA: null, _markerB: null, _markerC: null, _preview: null,
  _step: 'A', _simType: 'FLOOD',

  init(cityLat, cityLng, simType) {
    this.destroy();
    this._step = 'A'; this._simType = simType || 'FLOOD';
    this._clearCoords();
    this._updateStepUI();

    const map = L.map('simPickerMapEl', { zoomControl: true });
    this._map = map;

    L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
      { attribution: '© Esri', maxZoom: 19 }).addTo(map);
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_only_labels/{z}/{x}/{y}{r}.png',
      { attribution: '© CartoDB', maxZoom: 19, opacity: 0.9 }).addTo(map);

    map.setView([cityLat, cityLng], 14);
    map.on('click', e => this._onClick(e.latlng));

    map.on('mousemove', e => {
      const c = document.getElementById('pickerCoordB');
      if (this._step === 'B' && c)
        c.innerHTML = `<i class="fas fa-crosshairs" style="color:var(--text-3)"></i> ${e.latlng.lat.toFixed(5)}, ${e.latlng.lng.toFixed(5)}`;
    });

    setTimeout(() => map.invalidateSize(), 150);
  },

  _onClick(latlng) {
    if (this._step === 'A') {
      if (this._markerA) this._map.removeLayer(this._markerA);
      this._markerA = L.marker([latlng.lat, latlng.lng], { icon: this._icon('A', true) })
        .addTo(this._map).bindPopup('<strong>Ponto A</strong>').openPopup();
      document.getElementById('p_lat_a').value = latlng.lat.toFixed(6);
      document.getElementById('p_lng_a').value = latlng.lng.toFixed(6);
      document.getElementById('pickerCoordA').innerHTML =
        `<i class="fas fa-map-marker-alt" style="color:var(--cyan)"></i> A: ${latlng.lat.toFixed(5)}, ${latlng.lng.toFixed(5)}`;
      this._step = 'B';

    } else if (this._step === 'B') {
      if (this._markerB) this._map.removeLayer(this._markerB);
      this._markerB = L.marker([latlng.lat, latlng.lng], { icon: this._icon('B', false) })
        .addTo(this._map).bindPopup('<strong>Ponto B</strong>').openPopup();
      document.getElementById('p_lat_b').value = latlng.lat.toFixed(6);
      document.getElementById('p_lng_b').value = latlng.lng.toFixed(6);
      document.getElementById('pickerCoordB').innerHTML =
        `<i class="fas fa-flag" style="color:var(--green)"></i> B: ${latlng.lat.toFixed(5)}, ${latlng.lng.toFixed(5)}`;
      if (this._simType === 'HEAT_ISLAND') {
        this._step = 'C';
        this._drawPreview();
      } else {
        this._step = 'done';
        this._drawPreview();
        const aLL = this._markerA.getLatLng();
        this._map.fitBounds([[aLL.lat, aLL.lng],[latlng.lat, latlng.lng]], { padding:[40,40] });
      }

    } else if (this._step === 'C') {
      if (this._markerC) this._map.removeLayer(this._markerC);
      this._markerC = L.marker([latlng.lat, latlng.lng], { icon: this._icon('C', false) })
        .addTo(this._map).bindPopup('<strong>Ponto C</strong>').openPopup();
      document.getElementById('p_lat_c').value = latlng.lat.toFixed(6);
      document.getElementById('p_lng_c').value = latlng.lng.toFixed(6);
      this._step = 'done';
      this._drawPreview();
      const aLL = this._markerA.getLatLng(), bLL = this._markerB.getLatLng();
      this._map.fitBounds([[aLL.lat,aLL.lng],[bLL.lat,bLL.lng],[latlng.lat,latlng.lng]], { padding:[40,40] });

    } else {
      this.reset();
      setTimeout(() => this._onClick(latlng), 30);
      return;
    }
    this._updateStepUI();
  },

  _drawPreview() {
    if (this._preview) { this._map.removeLayer(this._preview); this._preview = null; }
    if (!this._markerA) return;
    const params  = this._currentParams();
    const score   = calcIntensity(this._simType, params);
    const color   = intensityColor(score);
    const opacity = 0.28 + score * 0.40;
    const tip     = `${intensityLabel(score)} (${(score*100).toFixed(0)}%)`;

    if (this._simType === 'HEAT_ISLAND') {
      if (!this._markerB) return;
      const a = this._markerA.getLatLng(), b = this._markerB.getLatLng();
      const c = this._markerC?.getLatLng() ?? null;
      const pts = c ? [[a.lat,a.lng],[b.lat,b.lng],[c.lat,c.lng]] : [[a.lat,a.lng],[b.lat,b.lng]];
      this._preview = L.polygon(pts, { color, fillColor: color, fillOpacity: opacity, weight: 2.5 })
        .addTo(this._map).bindTooltip(tip, { sticky: true });
    } else {
      if (!this._markerB) return;
      const a = this._markerA.getLatLng(), b = this._markerB.getLatLng();
      const widthRatio = 0.10 + score * 0.18;
      const isDash     = this._simType === 'FLOOD';
      this._preview = L.polygon(buildCorridor(a.lat, a.lng, b.lat, b.lng, widthRatio), {
        color, fillColor: color, fillOpacity: opacity,
        weight: 2.5, dashArray: isDash ? '10 6' : null
      }).addTo(this._map).bindTooltip(tip, { sticky: true });
    }
  },

  _currentParams() {
    const def = PARAM_FIELDS[this._simType];
    if (!def) return {};
    const obj = {};
    def.fields.forEach(f => {
      const el = document.getElementById(f.id);
      if (el) {
      const key = f.id.replace('p_', '');
      obj[key] = f.type === 'range' ? parseFloat(el.value) : el.value;
    }
    });
    return obj;
  },

  refreshPreview() { this._drawPreview(); },

  updateType(simType) {
    this._simType = simType;
    if (this._markerA && this._markerB) this._drawPreview();
  },

  reset() {
    ['_markerA','_markerB','_markerC','_preview'].forEach(k => {
      if (this[k]) { this._map?.removeLayer(this[k]); this[k] = null; }
    });
    this._step = 'A';
    this._clearCoords();
    this._updateStepUI();
  },

  destroy() {
    if (this._map) { this._map.remove(); this._map = null; }
    this._markerA = this._markerB = this._markerC = this._preview = null;
  },

  _clearCoords() {
    ['p_lat_a','p_lng_a','p_lat_b','p_lng_b','p_lat_c','p_lng_c'].forEach(id => {
      const el = document.getElementById(id); if (el) el.value = '';
    });
    const cA = document.getElementById('pickerCoordA');
    const cB = document.getElementById('pickerCoordB');
    if (cA) cA.innerHTML = '<i class="fas fa-map-marker-alt" style="color:var(--cyan)"></i> A: clique no mapa';
    if (cB) cB.innerHTML = '<i class="fas fa-flag" style="color:var(--text-2)"></i> B: clique no mapa';
  },

  _updateStepUI() {
    const la = document.getElementById('stepLabelA');
    const lb = document.getElementById('stepLabelB');
    if (!la) return;
    const isHeat = this._simType === 'HEAT_ISLAND';
    const bb = document.getElementById('pickerStepB')?.querySelector('.step-circle');
    if (this._step === 'A') {
      la.textContent = 'Clique no mapa para marcar o Ponto A';
      lb.textContent = isHeat ? 'Depois: Ponto B (2º vértice)' : 'Depois: Ponto B (fim da área)';
      bb?.classList.remove('done');
    } else if (this._step === 'B') {
      la.textContent = 'Ponto A marcado';
      lb.textContent = isHeat ? 'Clique para o Ponto B (2º vértice)' : 'Clique para marcar o Ponto B';
      bb?.classList.remove('done');
    } else if (this._step === 'C') {
      la.textContent = 'A e B marcados — triângulo se formando';
      lb.textContent = 'Clique para fechar a zona (Ponto C — 3º vértice)';
      bb?.classList.add('done');
    } else {
      la.textContent = 'Ponto A marcado';
      lb.textContent = isHeat ? 'Zona térmica definida com 3 pontos' : 'Corredor de impacto desenhado';
      bb?.classList.add('done');
    }
  },

  _icon(label, isCyan) {
    const bg  = isCyan ? 'var(--cyan)' : 'var(--bg-1)';
    const clr = isCyan ? '#000' : 'var(--green)';
    const brd = isCyan ? 'var(--cyan)' : 'var(--green)';
    return L.divIcon({
      className: '', iconSize: [30,30], iconAnchor: [15,15],
      html: `<div style="background:${bg};border:2px solid ${brd};width:30px;height:30px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:13px;font-weight:900;color:${clr};box-shadow:0 0 12px ${brd}66;transition:all .2s">${label}</div>`
    });
  }
};

const MainMap = {
  _map: null, _layers: [], _active: null,

  init() {
    if (this._map) { this._map.remove(); this._map = null; }
    this._layers = []; this._active = null;

    const map = L.map('mainMapEl', { zoomControl: true });
    this._map = map;

    const sat = L.tileLayer(
      'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
      { attribution: '© Esri — NASA', maxZoom: 19 });
    const osm = L.tileLayer(
      'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
      { attribution: '© OpenStreetMap', maxZoom: 19 });

    sat.addTo(map);
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_only_labels/{z}/{x}/{y}{r}.png',
      { maxZoom: 19, opacity: 0.85 }).addTo(map);
    L.control.layers({ 'Satélite': sat, 'Ruas': osm }).addTo(map);

    map.setView([-23.5505, -46.6333], 11);
    this.renderAll();
    setTimeout(() => map.invalidateSize(), 200);
  },

  renderAll() {
    const typeF   = document.getElementById('mapviewTypeFilter')?.value   || '';
    const statusF = document.getElementById('mapviewStatusFilter')?.value || '';

    this._layers.forEach(l => { if (this._map) this._map.removeLayer(l); });
    this._layers = [];

    let sims = state.simulations;
    if (typeF)   sims = sims.filter(s => s.simulationType === typeF);
    if (statusF) sims = sims.filter(s => s.status         === statusF);

    document.getElementById('mapviewCount').textContent = `${sims.length} simulação${sims.length !== 1 ? 'ões' : ''}`;

    const bounds = [];
    sims.forEach(s => {
      let p = {};
      try { p = JSON.parse(s.parameters || '{}'); } catch {}
      const hasCoords = p.lat_a != null && p.lat_b != null;
      this._renderSimLayer(s, p, hasCoords);
      if (hasCoords) { bounds.push([p.lat_a, p.lng_a]); bounds.push([p.lat_b, p.lng_b]); }
    });

    if (bounds.length > 1) this._map.fitBounds(bounds, { padding:[50,50] });
    this._renderList(sims);
  },

  _renderSimLayer(sim, params, hasCoords) {
    const vis    = SimMap.TYPE_VIS[sim.simulationType] || { label:'Área' };
    const city   = state.cities.find(c => c.id === sim.cityId);

    const score  = sim.riskScore != null ? sim.riskScore : calcIntensity(sim.simulationType, params);
    const color  = intensityColor(score);
    const width  = 0.003 + score * 0.009;
    const opac   = 0.22 + score * 0.45;
    const isDash = ['FLOOD','HEAT_ISLAND'].includes(sim.simulationType);

    if (hasCoords) {
      const widthRatio = 0.10 + score * 0.18;
      const pts = (sim.simulationType === 'HEAT_ISLAND' && params.lat_c != null)
        ? [[params.lat_a,params.lng_a],[params.lat_b,params.lng_b],[params.lat_c,params.lng_c]]
        : buildCorridor(params.lat_a, params.lng_a, params.lat_b, params.lng_b, widthRatio);
      const isDash = sim.simulationType === 'FLOOD';
      const poly = L.polygon(pts, {
        color, fillColor: color, fillOpacity: opac,
        weight: 2.5, dashArray: isDash ? '10 6' : null
      }).addTo(this._map);

      poly.on('click', () => this._focusSim(sim));
      poly.bindTooltip(
        `<strong>${sim.cityName}</strong><br>${vis.label}<br>${intensityLabel(score)} (${(score*100).toFixed(0)}%)`,
        { sticky: true }
      );

      const mkA = L.marker([params.lat_a, params.lng_a], { icon: this._ptIcon('A', color) }).addTo(this._map);
      const mkB = L.marker([params.lat_b, params.lng_b], { icon: this._ptIcon('B', '#fff', color) }).addTo(this._map);

      this._layers.push(poly, mkA, mkB);
    } else if (city) {

      const mk = L.marker([city.latitude, city.longitude], { icon: this._cityIcon(vis.color) }).addTo(this._map);
      mk.bindTooltip(`<strong>${sim.cityName}</strong><br>${vis.label}<br><small>Sem área definida</small>`, { sticky: true });
      mk.on('click', () => this._focusSim(sim));
      this._layers.push(mk);
    }
  },

  _focusSim(sim) {

    document.querySelectorAll('.mapview-item').forEach(el => el.classList.remove('active'));
    const item = document.getElementById(`mapItem_${sim.id}`);
    if (item) { item.classList.add('active'); item.scrollIntoView({ behavior:'smooth', block:'nearest' }); }
    this._active = sim.id;
  },

  flyToSim(simId) {
    const sim = state.simulations.find(s => s.id === simId);
    if (!sim) return;
    let p = {};
    try { p = JSON.parse(sim.parameters || '{}'); } catch {}
    if (p.lat_a != null && p.lat_b != null) {
      this._map.fitBounds([[p.lat_a, p.lng_a],[p.lat_b, p.lng_b]], { padding:[60,60], maxZoom:15 });
    } else {
      const city = state.cities.find(c => c.id === sim.cityId);
      if (city) this._map.setView([city.latitude, city.longitude], 14);
    }
    this._focusSim(sim);
  },

  fitAll() { this.renderAll(); },
  filter()  { this.renderAll(); },

  _renderList(sims) {
    const el = document.getElementById('mapviewList');
    if (!sims.length) {
      el.innerHTML = '<div class="empty-state" style="padding:24px"><i class="fas fa-map-marked-alt"></i><p>Nenhuma simulação</p></div>';
      return;
    }
    el.innerHTML = sims.map(s => {
      const vis = SimMap.TYPE_VIS[s.simulationType] || { color:'#aaa', label:'Área' };
      let p = {};
      try { p = JSON.parse(s.parameters || '{}'); } catch {}
      const hasCoords = p.lat_a != null;
      const riskPct = s.riskScore != null ? (s.riskScore*100).toFixed(0)+'%' : '—';
      return `<div class="mapview-item" id="mapItem_${s.id}" onclick="MainMap.flyToSim(${s.id})">
        <div class="mapview-item-top">
          <div class="mapview-type-dot" style="background:${vis.color}"></div>
          <span class="mapview-item-name">${s.cityName} — ${vis.label}</span>
        </div>
        <div class="mapview-item-sub">
          ${fmt.statusBadge(s.status)}
          ${s.riskScore != null ? `<span>Risco: ${riskPct}</span>` : ''}
          ${!hasCoords ? '<span class="no-coords-badge">sem área</span>' : ''}
        </div>
      </div>`;
    }).join('');
  },

  _ptIcon(label, bg, border) {
    const brd = border || bg;
    const clr = label === 'A' ? '#000' : bg;
    const bgc = label === 'A' ? bg : 'var(--bg-1)';
    return L.divIcon({ className:'', iconSize:[22,22], iconAnchor:[11,11],
      html:`<div style="background:${bgc};border:2px solid ${brd};width:22px;height:22px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:700;color:${clr};box-shadow:0 0 6px ${brd}88">${label}</div>`
    });
  },
  _cityIcon(color) {
    return L.divIcon({ className:'', iconSize:[26,26], iconAnchor:[13,13],
      html:`<div style="background:var(--bg-1);border:2px solid ${color};width:26px;height:26px;border-radius:8px;display:flex;align-items:center;justify-content:center;box-shadow:0 0 8px ${color}66"><i class='fas fa-city' style='font-size:11px;color:${color}'></i></div>`
    });
  }
};

const SimMap = {
  _instance: null,
  _currentSim: null,

  TYPE_VIS: {
    FLOOD:        { color: '#0080ff', label: 'Zona de Alagamento',  pulse: true  },
    TRAFFIC:      { color: '#ff6600', label: 'Via com Congestionamento', pulse: false },
    CONSTRUCTION: { color: '#ffcc00', label: 'Área de Impacto da Obra', pulse: false },
    ZONING:       { color: '#00cc66', label: 'Área de Mudança de Zona', pulse: false },
    HEAT_ISLAND:  { color: '#ff3300', label: 'Zona de Calor Extremo', pulse: true  }
  },

  destroy() {
    if (this._instance) { this._instance.remove(); this._instance = null; }
  },

  render(sim) {
    this._currentSim = sim;
    this.destroy();

    const vis = this.TYPE_VIS[sim.simulationType] || { color: '#ffffff', label: 'Área de Impacto' };
    let params = {};
    try { params = JSON.parse(sim.parameters || '{}'); } catch {}

    const city = state.cities.find(c => c.id === sim.cityId);
    const cityLat = city?.latitude  ?? -23.5505;
    const cityLng = city?.longitude ?? -46.6333;

    const latA = parseFloat(params.lat_a ?? (cityLat - 0.015));
    const lngA = parseFloat(params.lng_a ?? (cityLng - 0.01 ));
    const latB = parseFloat(params.lat_b ?? (cityLat + 0.015));
    const lngB = parseFloat(params.lng_b ?? (cityLng + 0.01 ));

    const centerLat = (latA + latB) / 2;
    const centerLng = (lngA + lngB) / 2;

    const map = L.map('simMap', { zoomControl: true, attributionControl: true });
    this._instance = map;

    const satellite = L.tileLayer(
      'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
      { attribution: 'Tiles © Esri — NASA, NGA, USGS', maxZoom: 19 }
    );

    const streets = L.tileLayer(
      'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
      { attribution: '© OpenStreetMap contributors', maxZoom: 19 }
    );

    satellite.addTo(map);
    L.control.layers({ 'Satélite': satellite, 'Mapa de Ruas': streets }).addTo(map);

    map.setView([centerLat, centerLng], 14);

    let paramsObj = {};
    try { paramsObj = JSON.parse(sim.parameters || '{}'); } catch {}
    const score   = sim.riskScore != null ? sim.riskScore : calcIntensity(sim.simulationType, paramsObj);
    const color   = intensityColor(score);
    const opacity = 0.28 + score * 0.42;
    const tip     = `<strong>${vis.label}</strong><br>${intensityLabel(score)} (${(score*100).toFixed(0)}%)`;

    let polyLayer;
    if (sim.simulationType === 'HEAT_ISLAND' && paramsObj.lat_c != null) {

      polyLayer = L.polygon([[latA,lngA],[latB,lngB],[paramsObj.lat_c,paramsObj.lng_c]], {
        color, fillColor: color, fillOpacity: opacity, weight: 2.5
      }).addTo(map).bindPopup(tip);
      L.marker([paramsObj.lat_c, paramsObj.lng_c], { icon: iconA }).addTo(map)
        .bindPopup(`<strong>Ponto C</strong>`);
    } else {
      const widthRatio = 0.10 + score * 0.18;
      const isDash     = sim.simulationType === 'FLOOD';
      polyLayer = L.polygon(buildCorridor(latA, lngA, latB, lngB, widthRatio), {
        color, weight: 2.5, fillColor: color, fillOpacity: opacity,
        dashArray: isDash ? '10 6' : null
      }).addTo(map).bindPopup(tip);
    }

    const iconA = L.divIcon({
      className: '', html: `<div style="background:${color};border:2px solid #fff;width:16px;height:16px;border-radius:50%;box-shadow:0 0 10px ${color}99;display:flex;align-items:center;justify-content:center;font-size:9px;font-weight:900;color:#000">A</div>`,
      iconSize: [16,16], iconAnchor: [8,8]
    });
    L.marker([latA, lngA], { icon: iconA }).addTo(map)
      .bindPopup(`<strong>Ponto A</strong><br>${latA.toFixed(5)}, ${lngA.toFixed(5)}`);

    const iconB = L.divIcon({
      className: '', html: `<div style="background:#fff;border:2px solid ${color};width:16px;height:16px;border-radius:50%;box-shadow:0 0 10px ${color}99;display:flex;align-items:center;justify-content:center;font-size:9px;font-weight:900;color:${color}">B</div>`,
      iconSize: [16,16], iconAnchor: [8,8]
    });
    L.marker([latB, lngB], { icon: iconB }).addTo(map)
      .bindPopup(`<strong>Ponto B</strong><br>${latB.toFixed(5)}, ${lngB.toFixed(5)}`);

    if (city) {
      const iconCity = L.divIcon({
        className: '', html: `<div style="background:#0d1525;border:2px solid #00d4ff;color:#00d4ff;width:28px;height:28px;border-radius:6px;display:flex;align-items:center;justify-content:center;box-shadow:0 0 10px #00d4ff44"><i class='fas fa-city' style='font-size:12px'></i></div>`,
        iconSize: [28,28], iconAnchor: [14,14]
      });
      L.marker([city.latitude, city.longitude], { icon: iconCity }).addTo(map)
        .bindPopup(`<strong>${city.name} / ${city.state}</strong><br>Centro monitorado`);
    }

    L.polyline([[latA, lngA], [latB, lngB]], {
      color: vis.color, weight: 1.5, opacity: 0.6, dashArray: '4 4'
    }).addTo(map);

    map.fitBounds([[latA, lngA], [latB, lngB]], { padding: [60, 60] });

    this._renderLegend(sim, vis, paramsObj, score, color);

    setTimeout(() => map.invalidateSize(), 120);
  },

  _renderLegend(sim, vis, params, score, color) {
    document.getElementById('simMapLegend').innerHTML = `
      <div class="legend-item">
        <div class="legend-dot" style="background:${color}"></div>
        <span><strong>${vis.label}</strong></span>
      </div>
      <div class="legend-item">
        <span>Intensidade: <strong style="color:${color}">${intensityLabel(score)} (${(score*100).toFixed(0)}%)</strong></span>
      </div>
      <div class="legend-scale">
        <span style="color:#22c55e">🟢 Baixo</span>
        <div class="scale-gradient"></div>
        <span style="color:#ef4444">🔴 Alto</span>
      </div>
      <div class="legend-item">
        <i class="fas fa-city" style="color:var(--cyan)"></i>
        <span>${sim.cityName} / ${sim.cityState}</span>
      </div>
      <div class="legend-item" style="margin-left:auto">
        <i class="fas fa-satellite" style="color:var(--text-2)"></i>
        <span style="color:var(--text-2)">ESRI World Imagery</span>
      </div>
    `;
  }
};

const PARAM_FIELDS = {
  FLOOD: {
    icon: 'fa-water', title: 'Parâmetros da Enchente',
    fields: [
      { id: 'p_rain', label: 'Chuva acumulada esperada', type: 'range', min: 10, max: 300, step: 5, default: 60, unit: 'mm',
        hint: 'Volume total previsto. Até 50mm = chuva moderada. 100–150mm = risco de alagamento. Acima de 200mm = chuva extrema.' },
      { id: 'p_dur',  label: 'Duração do evento de chuva', type: 'range', min: 1, max: 24, step: 1, default: 6, unit: 'horas',
        hint: 'Tempo contínuo de precipitação. Eventos reais raramente ultrapassam 12–18h. Acima de 6h com muita chuva = alto risco.' },
    ]
  },
  TRAFFIC: {
    icon: 'fa-car', title: 'Parâmetros do Tráfego',
    fields: [
      { id: 'p_peak',      label: 'Horário de pico', type: 'select',
        options: ['06:00–09:00 (manhã)','11:00–13:00 (almoço)','17:00–20:00 (tarde)','21:00–00:00 (noite)'],
        hint: 'Picos da manhã e tarde concentram a maioria dos veículos.' },
      { id: 'p_road_type', label: 'Tipo de via', type: 'select',
        options: ['Via expressa / Corredor BRT','Avenida principal / Radial','Via coletora / Comercial','Via local / Rua comum'],
        hint: 'Avenidas expressas e radiais têm muito mais fluxo que ruas locais. Paulista (SP), por exemplo, é Avenida principal.' },
    ]
  },
  CONSTRUCTION: {
    icon: 'fa-hard-hat', title: 'Parâmetros da Obra',
    fields: [
      { id: 'p_type',   label: 'Tipo de obra', type: 'select',
        options: ['Metrô / Linha de trem','Viaduto / Ponte','Saneamento e Tubulação','Pavimentação','Edifício comercial','Edifício residencial','Parque / Área verde'],
        hint: 'A complexidade varia muito: uma linha de metrô leva anos; um parque pode levar meses.' },
      { id: 'p_months', label: 'Prazo do projeto', type: 'range', min: 1, max: 120, step: 1, default: 12, unit: 'meses',
        hint: 'Duração total planejada. Metrô/ponte: 24–120 meses. Edifício comercial: 12–36 meses. Parque: 3–12 meses.' },
    ]
  },
  HEAT_ISLAND: {
    icon: 'fa-temperature-high', title: 'Previsão de Temperatura (Ilha de Calor)',
    hint3: 'Marque 3 pontos no mapa para definir a zona térmica da região.',
    fields: [
      { id: 'p_morning',   label: 'Temperatura de manhã',  type: 'range', min: 10, max: 40, step: 1, default: 22, unit: '°C', hint: 'Temperatura média esperada entre 6h e 10h. Influencia o desconforto diurno.' },
      { id: 'p_afternoon', label: 'Temperatura da tarde',  type: 'range', min: 18, max: 48, step: 1, default: 34, unit: '°C', hint: 'Pico de calor entre 12h e 17h. Acima de 35°C indica ilha de calor severa.' },
      { id: 'p_night',     label: 'Temperatura à noite',   type: 'range', min: 8,  max: 35, step: 1, default: 26, unit: '°C', hint: 'Temperatura mínima noturna. Noites acima de 24°C indicam ilha de calor persistente.' },
    ]
  }
};

function renderParamFields(type, containerId) {
  const container = document.getElementById(containerId);
  const def = PARAM_FIELDS[type];
  if (!def || !container) return;
  container.style.display = 'flex';

  const picker = document.getElementById('simPickerSection');
  if (picker) {
    picker.style.display = 'flex';
    const cityId = document.getElementById('simCityId')?.value;
    const city   = state.cities.find(c => String(c.id) === String(cityId));
    const lat    = city?.latitude  ?? -23.5505;
    const lng    = city?.longitude ?? -46.6333;
    MapPicker.init(lat, lng, type);
  }

  const fieldsHtml = def.fields.map(f => {
    if (f.type === 'select') {
      return `<div class="form-group">
        <label>${f.label}</label>
        <select id="${f.id}">
          ${f.options.map(o => `<option value="${o}">${o}</option>`).join('')}
        </select>
        <span class="field-hint">${f.hint}</span>
      </div>`;
    }

    return `<div class="form-group">
      <label>${f.label}</label>
      <div class="slider-row">
        <input type="range" id="${f.id}" min="${f.min}" max="${f.max}" step="${f.step}" value="${f.default}"
          oninput="document.getElementById('${f.id}_val').textContent=this.value+' ${f.unit}'; MapPicker.refreshPreview()" />
        <span class="slider-val" id="${f.id}_val">${f.default} ${f.unit}</span>
      </div>
      <span class="field-hint">${f.hint}</span>
    </div>`;
  }).join('');

  container.innerHTML = `
    <div class="params-title"><i class="fas ${def.icon}"></i> ${def.title}</div>
    <div class="form-row">${fieldsHtml}</div>
  `;
}

function collectParams(type, containerId) {
  const def = PARAM_FIELDS[type];
  if (!def) return null;
  const obj = {};
  def.fields.forEach(f => {
    const el = document.getElementById(f.id);
    if (el) obj[f.id.replace('p_', '')] = f.type === 'range' ? parseFloat(el.value) : el.value;
  });

  ['a','b','c'].forEach(pt => {
    const la = document.getElementById(`p_lat_${pt}`);
    const lo = document.getElementById(`p_lng_${pt}`);
    if (la?.value) obj[`lat_${pt}`] = parseFloat(la.value);
    if (lo?.value) obj[`lng_${pt}`] = parseFloat(lo.value);
  });
  return JSON.stringify(obj);
}

const SIM_TYPE_LABEL = {
  FLOOD:        { label: 'Enchente',     icon: 'fa-water',           color: 'blue'   },
  TRAFFIC:      { label: 'Tráfego',      icon: 'fa-car',             color: 'orange' },
  CONSTRUCTION: { label: 'Obra',         icon: 'fa-hard-hat',        color: 'orange' },
  ZONING:       { label: 'Zoneamento',   icon: 'fa-map',             color: 'purple' },
  HEAT_ISLAND:  { label: 'Ilha de Calor',icon: 'fa-temperature-high',color: 'red'    }
};
const STATUS_BADGE = {
  PENDING:    { cls: 'badge-gray',   icon: 'fa-clock',        label: 'Pendente'    },
  PROCESSING: { cls: 'badge-blue',   icon: 'fa-spinner fa-spin', label: 'Processando' },
  COMPLETED:  { cls: 'badge-green',  icon: 'fa-check-circle', label: 'Completo'    },
  FAILED:     { cls: 'badge-red',    icon: 'fa-times-circle', label: 'Falhou'      }
};

const state = { cities: [], simulations: [] };

const Toast = {
  show(msg, type = 'info') {
    const icons = { success: 'fa-check-circle', error: 'fa-times-circle', info: 'fa-info-circle' };
    const el = document.createElement('div');
    el.className = `toast toast-${type}`;
    el.innerHTML = `<i class="fas ${icons[type]}"></i><span>${msg}</span>`;
    document.getElementById('toastContainer').appendChild(el);
    setTimeout(() => { el.style.opacity = '0'; el.style.transform = 'translateX(40px)'; el.style.transition = '.3s'; setTimeout(() => el.remove(), 300); }, 3500);
  }
};

const fmt = {
  num(n)  { return n != null ? Number(n).toLocaleString('pt-BR') : '—'; },
  pct(n)  { return n != null ? (n * 100).toFixed(0) + '%' : '—'; },
  date(d) { return d ? new Date(d).toLocaleString('pt-BR', { day:'2-digit', month:'2-digit', year:'numeric', hour:'2-digit', minute:'2-digit' }) : '—'; },
  riskClass(n) {
    if (n == null) return '';
    if (n > 0.7) return 'risk-high';
    if (n > 0.4) return 'risk-medium';
    return 'risk-low';
  },
  riskMeter(n) {
    if (n == null) return '<span style="color:var(--text-2)">—</span>';
    const cls = fmt.riskClass(n);
    const pct = Math.round(n * 100);
    return `<div class="risk-meter ${cls}"><div class="risk-bar-outer"><div class="risk-bar-inner" style="width:${pct}%"></div></div><span class="risk-val">${pct}%</span></div>`;
  },
  typeBadge(type) {
    const t = SIM_TYPE_LABEL[type] || { label: type, icon: 'fa-flask', color: 'gray' };
    return `<span class="badge badge-${t.color}"><i class="fas ${t.icon}"></i> ${t.label}</span>`;
  },
  statusBadge(status) {
    const s = STATUS_BADGE[status] || { cls: 'badge-gray', icon: 'fa-circle', label: status };
    return `<span class="badge ${s.cls}"><i class="fas ${s.icon}"></i> ${s.label}</span>`;
  }
};

const App = {
  showSection(name) {
    document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    document.getElementById('section-' + name).classList.add('active');
    document.querySelector(`[data-section="${name}"]`).classList.add('active');
    const titles = { dashboard: 'Dashboard', cities: 'Cidades', simulations: 'Simulações', mapview: 'Mapa de Simulações' };
    document.getElementById('topbarTitle').textContent = titles[name] || name;
    if (name === 'dashboard')   Dashboard.load();
    if (name === 'cities')      Cities.load();
    if (name === 'simulations') Simulations.load();
    if (name === 'mapview') {
      Simulations.load().then(() => { Cities.load().then(() => MainMap.init()); });
    }
  },
  async refreshAll() {
    await Promise.all([Cities.load(), Simulations.load()]);
    Dashboard.load();
    Toast.show('Dados atualizados', 'success');
  }
};

const Dashboard = {
  async load() {
    try {
      const [cities, sims] = await Promise.all([
        API.get('/api/cities'),
        API.get('/api/simulations')
      ]);
      state.cities = cities;
      state.simulations = sims;
      this.renderStats(cities, sims);
      this.renderCities(cities);
      this.renderSims(sims);
    } catch(e) { Toast.show('Erro ao carregar dashboard: ' + e.message, 'error'); }
  },
  renderStats(cities, sims) {
    const completed = sims.filter(s => s.status === 'COMPLETED');
    const withRisk  = sims.filter(s => s.riskScore != null);
    const avgRisk   = withRisk.length ? withRisk.reduce((a,s) => a + s.riskScore, 0) / withRisk.length : null;
    document.getElementById('statCities').textContent    = cities.length;
    document.getElementById('statSims').textContent      = sims.length;
    document.getElementById('statCompleted').textContent = completed.length;
    document.getElementById('statAvgRisk').textContent   = avgRisk != null ? (avgRisk * 100).toFixed(0) + '%' : '—';
  },
  renderCities(cities) {
    const el = document.getElementById('dashCitiesList');
    if (!cities.length) { el.innerHTML = '<div class="empty-state"><i class="fas fa-city"></i><p>Nenhuma cidade</p></div>'; return; }
    el.innerHTML = cities.slice(0,5).map(c => `
      <div class="dash-city-item">
        <div class="dash-icon" style="background:rgba(0,212,255,.1);color:var(--cyan)"><i class="fas fa-city"></i></div>
        <div class="dash-main">
          <div class="dash-name">${c.name}</div>
          <div class="dash-sub">${c.state} · ${fmt.num(c.population)} hab · ${c.totalSimulations} simulações</div>
        </div>
        ${c.lidarAvailable ? '<span class="badge badge-green" style="font-size:10px">LiDAR</span>' : ''}
      </div>`).join('');
  },
  renderSims(sims) {
    const el = document.getElementById('dashSimsList');
    if (!sims.length) { el.innerHTML = '<div class="empty-state"><i class="fas fa-flask"></i><p>Nenhuma simulação</p></div>'; return; }
    el.innerHTML = sims.slice().reverse().slice(0,6).map(s => `
      <div class="dash-sim-item">
        <div class="dash-icon" style="background:rgba(124,58,237,.1);color:#a78bfa"><i class="fas ${SIM_TYPE_LABEL[s.simulationType]?.icon || 'fa-flask'}"></i></div>
        <div class="dash-main">
          <div class="dash-name">${s.cityName || '—'} — ${SIM_TYPE_LABEL[s.simulationType]?.label || s.simulationType}</div>
          <div class="dash-sub">${fmt.statusBadge(s.status)} ${s.riskScore != null ? '· Risco: ' + (s.riskScore*100).toFixed(0)+'%' : ''}</div>
        </div>
      </div>`).join('');
  }
};

const Cities = {
  _all: [],
  _filtered: [],

  async load() {
    try {
      const cities = await API.get('/api/cities');
      state.cities = cities;
      this._all = cities;
      this._filtered = cities;
      this.renderTable(cities);
      this.populateStateFilter(cities);

      const opts = cities.map(c => `<option value="${c.id}">${c.name} / ${c.state}</option>`).join('');
      ['simCityId','soapCityId'].forEach(id => { const el = document.getElementById(id); if(el) el.innerHTML = '<option value="">Selecione...</option>' + opts; });
    } catch(e) { Toast.show('Erro ao carregar cidades: ' + e.message, 'error'); }
  },

  renderTable(cities) {
    const tbody = document.getElementById('citiesTableBody');
    if (!cities.length) {
      tbody.innerHTML = '<tr><td colspan="8"><div class="empty-state"><i class="fas fa-city"></i><p>Nenhuma cidade cadastrada</p></div></td></tr>';
      return;
    }
    tbody.innerHTML = cities.map(c => `
      <tr>
        <td style="color:var(--text-2)">${c.id}</td>
        <td><strong style="color:var(--text-0)">${c.name}</strong></td>
        <td><span class="badge badge-gray">${c.state}</span></td>
        <td>${c.country}</td>
        <td>${c.lidarAvailable ? '<span class="badge badge-green"><i class="fas fa-check"></i> Sim</span>' : '<span class="badge badge-gray">Não</span>'}</td>
        <td>${c.satelliteResolution || '<span style="color:var(--text-2)">—</span>'}</td>
        <td><span class="badge badge-blue"><i class="fas fa-flask"></i> ${c.totalSimulations}</span></td>
        <td>
          <div class="action-btns">
            <button class="btn-icon btn-icon-edit" onclick="Cities.openModal(${c.id})" title="Editar"><i class="fas fa-pen"></i></button>
            <button class="btn-icon btn-icon-delete" onclick="Cities.delete(${c.id},'${c.name}')" title="Deletar"><i class="fas fa-trash"></i></button>
          </div>
        </td>
      </tr>`).join('');
  },

  populateStateFilter(cities) {
    const states = [...new Set(cities.map(c => c.state))].sort();
    const sel = document.getElementById('cityStateFilter');
    sel.innerHTML = '<option value="">Todos os estados</option>' + states.map(s => `<option value="${s}">${s}</option>`).join('');
  },

  filter(q) {
    q = q.toLowerCase();
    const filtered = this._all.filter(c =>
      c.name.toLowerCase().includes(q) ||
      c.state.toLowerCase().includes(q) ||
      c.country.toLowerCase().includes(q)
    );
    this.renderTable(filtered);
  },

  filterByState(state) {
    const filtered = state ? this._all.filter(c => c.state === state) : this._all;
    this.renderTable(filtered);
  },

  async openModal(id) {
    document.getElementById('cityId').value      = '';
    document.getElementById('cityName').value    = '';
    document.getElementById('cityState').value   = '';
    document.getElementById('cityCountry').value = 'Brasil';
    document.getElementById('cityLat').value     = '';
    document.getElementById('cityLng').value     = '';
    document.getElementById('citySatRes').value  = '';
    document.getElementById('cityLidar').checked = false;

    if (id) {
      document.getElementById('cityModalTitle').innerHTML = '<i class="fas fa-pen"></i> Editar Cidade';
      try {
        const c = await API.get('/api/cities/' + id);
        document.getElementById('cityId').value      = c.id;
        document.getElementById('cityName').value    = c.name;
        document.getElementById('cityState').value   = c.state;
        document.getElementById('cityCountry').value = c.country;
        document.getElementById('cityLat').value     = c.latitude  || '';
        document.getElementById('cityLng').value     = c.longitude || '';
        document.getElementById('citySatRes').value  = c.satelliteResolution || '';
        document.getElementById('cityLidar').checked = !!c.lidarAvailable;
      } catch(e) { Toast.show('Erro ao carregar cidade', 'error'); return; }
    } else {
      document.getElementById('cityModalTitle').innerHTML = '<i class="fas fa-city"></i> Nova Cidade';
    }
    document.getElementById('cityModal').classList.add('open');
  },

  closeModal() { document.getElementById('cityModal').classList.remove('open'); },

  async save() {
    const name    = document.getElementById('cityName').value.trim();
    const state   = document.getElementById('cityState').value.trim();
    const country = document.getElementById('cityCountry').value.trim();

    if (!name || !state || !country) {
      Toast.show('Preencha os campos obrigatórios (Nome, Estado, País)', 'error'); return;
    }

    let lat = parseFloat(document.getElementById('cityLat').value);
    let lng = parseFloat(document.getElementById('cityLng').value);

    if (isNaN(lat) || isNaN(lng)) {
      Toast.show('Buscando coordenadas automaticamente…', 'info');
      try {
        const query = encodeURIComponent(`${name}, ${state}, ${country}`);
        const geo   = await fetch(`https://nominatim.openstreetmap.org/search?q=${query}&format=json&limit=1`,
                        { headers: { 'Accept-Language': 'pt-BR' } });
        const data  = await geo.json();
        if (!data.length) { Toast.show('Cidade não encontrada — verifique Nome, Estado e País', 'error'); return; }
        lat = parseFloat(data[0].lat);
        lng = parseFloat(data[0].lon);
      } catch(e) { Toast.show('Erro ao buscar coordenadas: ' + e.message, 'error'); return; }
    }

    const body = {
      name, state, country,
      latitude:            lat,
      longitude:           lng,
      satelliteResolution: document.getElementById('citySatRes').value || null,
      lidarAvailable:      document.getElementById('cityLidar').checked
    };

    try {
      const id = document.getElementById('cityId').value;
      if (id) {
        await API.put('/api/cities/' + id, body);
        Toast.show('Cidade atualizada com sucesso!', 'success');
      } else {
        await API.post('/api/cities', body);
        Toast.show('Cidade cadastrada com sucesso!', 'success');
      }
      this.closeModal();
      await this.load();
    } catch(e) {
      try { const j = JSON.parse(e.message); Toast.show(j.message || 'Erro ao salvar', 'error'); }
      catch { Toast.show('Erro ao salvar cidade', 'error'); }
    }
  },

  async delete(id, name) {
    if (!confirm(`Deletar a cidade "${name}"? Isso removerá todas as simulações associadas.`)) return;
    try {
      await API.delete('/api/cities/' + id);
      Toast.show(`"${name}" removida`, 'success');
      await this.load();
    } catch(e) { Toast.show('Erro ao deletar', 'error'); }
  }
};

const Simulations = {
  _all: [],

  async load() {
    try {
      const sims = await API.get('/api/simulations');
      state.simulations = sims;
      this._all = sims;
      this.renderTable(sims);
    } catch(e) { Toast.show('Erro ao carregar simulações: ' + e.message, 'error'); }
  },

  renderTable(sims) {
    const tbody = document.getElementById('simsTableBody');
    if (!sims.length) {
      tbody.innerHTML = '<tr><td colspan="8"><div class="empty-state"><i class="fas fa-flask"></i><p>Nenhuma simulação</p></div></td></tr>';
      return;
    }
    tbody.innerHTML = sims.map(s => `
      <tr>
        <td style="color:var(--text-2)">${s.id}</td>
        <td>
          <strong style="color:var(--text-0)">${s.cityName || '—'}</strong>
          ${s.cityState ? `<span style="color:var(--text-2);font-size:11px"> / ${s.cityState}</span>` : ''}
        </td>
        <td>${fmt.typeBadge(s.simulationType)}</td>
        <td>${fmt.statusBadge(s.status)}</td>
        <td>${fmt.riskMeter(s.riskScore)}</td>
        <td>${s.nasaDataReference
          ? '<span class="badge badge-blue"><i class="fas fa-satellite-dish"></i> NASA</span>'
          : '<span style="color:var(--text-2)">—</span>'}</td>
        <td style="color:var(--text-2);font-size:12px">${fmt.date(s.createdAt)}</td>
        <td>
          <div class="action-btns">
            <button class="btn-icon btn-icon-view" onclick="Simulations.showDetail(${s.id})" title="Detalhes"><i class="fas fa-eye"></i></button>
            ${s.status === 'PENDING' || s.status === 'FAILED'
              ? `<button class="btn-icon btn-icon-process" onclick="Simulations.process(${s.id})" title="Processar com NASA"><i class="fas fa-satellite-dish"></i></button>` : ''}
            <button class="btn-icon btn-icon-edit" onclick="Simulations.openModal(${s.id})" title="Editar"><i class="fas fa-pen"></i></button>
            <button class="btn-icon btn-icon-delete" onclick="Simulations.delete(${s.id})" title="Deletar"><i class="fas fa-trash"></i></button>
          </div>
        </td>
      </tr>`).join('');
  },

  filter(q) {
    q = q.toLowerCase();
    this.renderTable(this._all.filter(s =>
      (s.cityName || '').toLowerCase().includes(q)
    ));
  },
  filterByType(type) {
    const status = document.getElementById('simStatusFilter').value;
    this.applyFilters(type, status);
  },
  filterByStatus(status) {
    const type = document.getElementById('simTypeFilter').value;
    this.applyFilters(type, status);
  },
  applyFilters(type, status) {
    let list = this._all;
    if (type)   list = list.filter(s => s.simulationType === type);
    if (status) list = list.filter(s => s.status === status);
    this.renderTable(list);
  },

  openModal(id) {
    document.getElementById('simId').value = '';
    document.getElementById('simCityId').value = '';
    document.getElementById('simNasa').checked = false;
    document.querySelectorAll('.type-card').forEach(c => c.classList.remove('selected'));
    document.querySelectorAll('input[name="simType"]').forEach(r => r.checked = false);
    const pf = document.getElementById('simParamsFields');
    pf.style.display = 'none'; pf.innerHTML = '';
    MapPicker.destroy();
    document.getElementById('simPickerSection').style.display = 'none';

    if (id) {
      document.getElementById('simModalTitle').innerHTML = '<i class="fas fa-pen"></i> Editar Simulação';
      const s = this._all.find(x => x.id === id);
      if (s) {
        document.getElementById('simId').value = s.id;
        document.getElementById('simCityId').value = s.cityId;
        const card = document.querySelector(`.type-card[data-type="${s.simulationType}"]`);
        if (card) {
          card.classList.add('selected');
          card.querySelector('input').checked = true;
          renderParamFields(s.simulationType, 'simParamsFields');
        }
      }
    } else {
      document.getElementById('simModalTitle').innerHTML = '<i class="fas fa-flask"></i> Nova Simulação';
    }
    document.getElementById('simModal').classList.add('open');
  },
  closeModal() {
    MapPicker.destroy();
    document.getElementById('simPickerSection').style.display = 'none';
    document.getElementById('simModal').classList.remove('open');
  },

  async save() {
    const cityId = document.getElementById('simCityId').value;
    const type   = document.querySelector('input[name="simType"]:checked')?.value;
    if (!cityId || !type) { Toast.show('Selecione cidade e tipo de simulação', 'error'); return; }

    const body = {
      cityId: parseInt(cityId),
      simulationType: type,
      parameters: collectParams(type, 'simParamsFields'),
      fetchNasaData: document.getElementById('simNasa').checked
    };
    try {
      const id = document.getElementById('simId').value;
      if (id) {
        await API.put('/api/simulations/' + id, body);
        Toast.show('Simulação atualizada!', 'success');
      } else {
        await API.post('/api/simulations', body);
        Toast.show('Simulação criada!', 'success');
      }
      this.closeModal();
      await this.load();
    } catch(e) {
      try { const j = JSON.parse(e.message); Toast.show(j.message || 'Erro ao salvar', 'error'); }
      catch { Toast.show('Erro ao salvar simulação', 'error'); }
    }
  },

  async process(id) {
    Toast.show('Processando simulação com dados da NASA…', 'info');
    try {
      const result = await API.post('/api/simulations/' + id + '/process', {});
      Toast.show(`Simulação ${id} processada! Risco: ${fmt.pct(result.riskScore)}`, 'success');
      await this.load();
    } catch(e) { Toast.show('Erro ao processar: ' + e.message, 'error'); }
  },

  _detailSim: null,

  switchTab(tab) {
    document.getElementById('tabBtnData').classList.toggle('active', tab === 'data');
    document.getElementById('tabBtnMap').classList.toggle('active', tab === 'map');
    document.getElementById('tabPanelData').style.display = tab === 'data' ? '' : 'none';
    document.getElementById('tabPanelMap').style.display  = tab === 'map'  ? '' : 'none';
    if (tab === 'map' && this._detailSim) {
      SimMap.render(this._detailSim);
    }
  },

  async showDetail(id) {
    const s = this._all.find(x => x.id === id);
    if (!s) return;
    this._detailSim = s;

    this.switchTab('data');

    let results = '—';
    try { results = s.results ? JSON.stringify(JSON.parse(s.results), null, 2) : '—'; } catch { results = s.results || '—'; }

    let paramsDisplay = '—';
    try {
      const p = JSON.parse(s.parameters || '{}');
      const skip = ['lat_a','lng_a','lat_b','lng_b'];
      const lines = Object.entries(p)
        .filter(([k]) => !skip.includes(k))
        .map(([k,v]) => `<div class="param-row"><span class="param-key">${k}</span><span class="param-val">${v}</span></div>`)
        .join('');
      const hasCords = p.lat_a != null;
      paramsDisplay = `<div class="param-list">${lines}</div>
        ${hasCords ? `<div style="margin-top:8px;font-size:12px;color:var(--cyan)"><i class="fas fa-map-marked-alt"></i> Coordenadas definidas — veja o Mapa de Impacto</div>` : ''}`;
    } catch { paramsDisplay = `<div class="json-block">${s.parameters || '—'}</div>`; }

    document.getElementById('tabPanelData').innerHTML = `
      <div class="modal-body">
        <div class="detail-grid">
          <div class="detail-item"><label>ID</label><div class="val">#${s.id}</div></div>
          <div class="detail-item"><label>Cidade</label><div class="val">${s.cityName || '—'} / ${s.cityState || '—'}</div></div>
          <div class="detail-item"><label>Tipo</label><div class="val">${fmt.typeBadge(s.simulationType)}</div></div>
          <div class="detail-item"><label>Status</label><div class="val">${fmt.statusBadge(s.status)}</div></div>
          <div class="detail-item"><label>Score de Risco</label><div class="val">${fmt.riskMeter(s.riskScore)}</div></div>
          <div class="detail-item"><label>Tipo de Simulação</label><div class="val">${s.simulationTypeDescription || '—'}</div></div>
          <div class="detail-item"><label>Criado em</label><div class="val">${fmt.date(s.createdAt)}</div></div>
          <div class="detail-item"><label>Atualizado em</label><div class="val">${fmt.date(s.updatedAt)}</div></div>
          <div class="detail-item full">
            <label>Dados NASA</label>
            <div class="val">${s.nasaDataReference
              ? `<span class="badge badge-blue"><i class="fas fa-satellite-dish"></i> ${s.nasaDataReference}</span>`
              : '<span style="color:var(--text-2)">Sem dados NASA — processe a simulação para obter</span>'}</div>
          </div>
          <div class="detail-item full"><label>Parâmetros da Simulação</label>${paramsDisplay}</div>
          <div class="detail-item full"><label>Resultado da Análise</label><div class="json-block">${results}</div></div>
          ${s.climateData ? `<div class="detail-item full"><label>Dados Climáticos NASA</label><div class="json-block">${s.climateData.substring(0,600)}${s.climateData.length>600?'…':''}</div></div>` : ''}
        </div>
        ${s.status === 'PENDING' || s.status === 'FAILED' ? `
        <div style="margin-top:16px;padding-top:16px;border-top:1px solid var(--border);display:flex;gap:10px;justify-content:flex-end">
          <button class="btn btn-primary" onclick="Simulations.process(${s.id});Simulations.closeDetailModal()">
            <i class="fas fa-satellite-dish"></i> Processar com NASA POWER API
          </button>
        </div>` : ''}
      </div>
    `;

    document.getElementById('simDetailModal').classList.add('open');
    SimMap.destroy();
  },

  closeDetailModal() {
    SimMap.destroy();
    document.getElementById('simDetailModal').classList.remove('open');
  },

  async delete(id) {
    if (!confirm('Deletar esta simulação?')) return;
    try {
      await API.delete('/api/simulations/' + id);
      Toast.show('Simulação removida', 'success');
      await this.load();
    } catch(e) { Toast.show('Erro ao deletar', 'error'); }
  }
};

const Soap = {
  async init() {

    if (state.cities.length === 0) await Cities.load();
    const opts = state.cities.map(c => `<option value="${c.id}">${c.name} / ${c.state}</option>`).join('');
    document.getElementById('soapCityId').innerHTML = '<option value="">Selecione...</option>' + opts;

    try {
      const wsdl = await fetch('/ws/simulation.wsdl').then(r => r.text());
      const preview = document.getElementById('wsdlPreview');
      preview.textContent = wsdl.replace(/<\?xml[^?]*\?>\s*/, '').substring(0, 2000) + '\n\n[... ver WSDL completo no link acima ...]';
    } catch {}
  },

  async getReport() {
    const id = document.getElementById('soapReportId').value;
    if (!id) { Toast.show('Informe o ID da simulação', 'error'); return; }
    const xml = `<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:tns="http://cityorbit.fiap.com.br/soap">
  <soapenv:Header/>
  <soapenv:Body>
    <tns:getSimulationReportRequest>
      <tns:simulationId>${id}</tns:simulationId>
    </tns:getSimulationReportRequest>
  </soapenv:Body>
</soapenv:Envelope>`;
    try {
      const resp = await API.soap(xml);
      const el = document.getElementById('soapReportResult');
      el.textContent = this.prettyXml(resp);
      el.classList.add('visible');
      Toast.show('SOAP respondeu com sucesso', 'success');
    } catch(e) { Toast.show('Erro SOAP: ' + e.message, 'error'); }
  },

  async process() {
    const cityId = document.getElementById('soapCityId').value;
    const type   = document.getElementById('soapSimType').value;
    const nasa   = document.getElementById('soapNasa').checked;
    if (!cityId) { Toast.show('Selecione uma cidade', 'error'); return; }
    const params = collectParams(type, 'soapParamsFields') || '';
    const xml = `<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:tns="http://cityorbit.fiap.com.br/soap">
  <soapenv:Header/>
  <soapenv:Body>
    <tns:processSimulationRequest>
      <tns:cityId>${cityId}</tns:cityId>
      <tns:simulationType>${type}</tns:simulationType>
      <tns:parameters>${params.replace(/</g,'&lt;').replace(/>/g,'&gt;')}</tns:parameters>
      <tns:fetchNasaData>${nasa}</tns:fetchNasaData>
    </tns:processSimulationRequest>
  </soapenv:Body>
</soapenv:Envelope>`;
    Toast.show('Enviando requisição SOAP…', 'info');
    try {
      const resp = await API.soap(xml);
      const el = document.getElementById('soapProcessResult');
      el.textContent = this.prettyXml(resp);
      el.classList.add('visible');
      Toast.show('Simulação SOAP processada!', 'success');

      await Simulations.load();
    } catch(e) { Toast.show('Erro SOAP: ' + e.message, 'error'); }
  },

  prettyXml(xml) {
    try {
      const parser = new DOMParser();
      const doc = parser.parseFromString(xml, 'text/xml');
      const ser = new XMLSerializer();
      let str = ser.serializeToString(doc);

      return str
        .replace(/></g, '>\n<')
        .split('\n')
        .map((l,i,a) => {
          const prev = a[i-1] || '';
          return l;
        }).join('\n');
    } catch { return xml; }
  }
};

function updateClock() {
  document.getElementById('topbarTime').textContent =
    new Date().toLocaleTimeString('pt-BR', { hour:'2-digit', minute:'2-digit', second:'2-digit' });
}

document.addEventListener('DOMContentLoaded', () => {

  document.querySelectorAll('.nav-item').forEach(a => {
    a.addEventListener('click', e => {
      e.preventDefault();
      App.showSection(a.dataset.section);
      // close mobile sidebar
      document.getElementById('sidebar').classList.remove('open');
    });
  });

  document.querySelectorAll('.modal-overlay').forEach(o => {
    if (o.id === 'loginModal') return;
    o.addEventListener('click', e => { if (e.target === o) o.classList.remove('open'); });
  });

  document.querySelectorAll('.type-card').forEach(card => {
    card.addEventListener('click', () => {
      document.querySelectorAll('.type-card').forEach(c => c.classList.remove('selected'));
      card.classList.add('selected');
      const type = card.dataset.type;
      renderParamFields(type, 'simParamsFields');
      MapPicker.updateType(type);
    });
  });

  document.getElementById('simCityId').addEventListener('change', function() {
    const city = state.cities.find(c => String(c.id) === this.value);
    if (!city) return;
    if (MapPicker._map) {
      MapPicker._map.setView([city.latitude, city.longitude], 14);
      MapPicker.reset();
    }
  });

  document.getElementById('sidebarToggle').addEventListener('click', () => {
    document.getElementById('sidebar').classList.toggle('open');
  });

  updateClock(); setInterval(updateClock, 1000);

  Auth.init();

  Dashboard.load();
});
