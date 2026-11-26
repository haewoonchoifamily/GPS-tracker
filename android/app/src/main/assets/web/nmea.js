// NMEA 0183 파서 (GP/GN/GL/GA/GB 접두어 지원) — 브라우저와 Node 양쪽에서 사용
(function (root) {
  function checksumOk(line) {
    const star = line.lastIndexOf('*');
    if (line[0] !== '$' || star < 0) return false;
    let cs = 0;
    for (let i = 1; i < star; i++) cs ^= line.charCodeAt(i);
    return cs === parseInt(line.slice(star + 1, star + 3), 16);
  }

  // ddmm.mmmm + 반구 → 십진 도
  function toDeg(v, hemi) {
    if (!v) return null;
    const dot = v.indexOf('.');
    const degLen = (dot < 0 ? v.length : dot) - 2;
    const d = parseInt(v.slice(0, degLen), 10) + parseFloat(v.slice(degLen)) / 60;
    return hemi === 'S' || hemi === 'W' ? -d : d;
  }

  function num(v) { return v === '' || v == null ? null : parseFloat(v); }

  function parse(line, state) {
    line = line.trim();
    if (!checksumOk(line)) return null;
    const f = line.slice(1, line.lastIndexOf('*')).split(',');
    const type = f[0].slice(2);
    state.lastSentence = f[0];

    if (type === 'RMC') {
      state.time = f[1] ? `${f[1].slice(0, 2)}:${f[1].slice(2, 4)}:${f[1].slice(4, 6)}` : null;
      state.date = f[9] ? `20${f[9].slice(4, 6)}-${f[9].slice(2, 4)}-${f[9].slice(0, 2)}` : null;
      state.valid = f[2] === 'A';
      if (state.valid) {
        state.lat = toDeg(f[3], f[4]);
        state.lon = toDeg(f[5], f[6]);
        state.speedKmh = num(f[7]) != null ? num(f[7]) * 1.852 : null;
        state.course = num(f[8]);
      }
      return 'RMC';
    }
    if (type === 'GGA') {
      state.quality = parseInt(f[6] || '0', 10);
      state.satsUsed = parseInt(f[7] || '0', 10);
      state.hdop = num(f[8]);
      state.alt = num(f[9]);
      return 'GGA';
    }
    if (type === 'GSA') {
      state.fixType = parseInt(f[2] || '1', 10); // 1=없음, 2=2D, 3=3D
      return 'GSA';
    }
    if (type === 'GSV') {
      const total = parseInt(f[1], 10), idx = parseInt(f[2], 10);
      const key = f[0].slice(0, 2);
      state._gsv = state._gsv || {};
      if (idx === 1) state._gsv[key] = [];
      const list = state._gsv[key] || (state._gsv[key] = []);
      for (let i = 4; i + 3 < f.length; i += 4) {
        if (!f[i]) continue;
        list.push({ prn: f[i], elev: num(f[i + 1]), az: num(f[i + 2]), snr: num(f[i + 3]), sys: key });
      }
      if (idx === total) {
        state.sats = Object.values(state._gsv).flat();
      }
      return 'GSV';
    }
    return type;
  }

  const api = { parse, checksumOk, toDeg };
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  else root.NMEA = api;
})(this);
