const ColorUtils = {
  async extractPalette(imageUrl) {
    return new Promise((resolve) => {
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = () => {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        const w = 100;
        const h = Math.round(img.naturalHeight * (w / img.naturalWidth));
        canvas.width = w;
        canvas.height = h;
        ctx.drawImage(img, 0, 0, w, h);
        const data = ctx.getImageData(0, 0, w, h).data;
        const pixels = [];
        for (let i = 0; i < data.length; i += 4) {
          pixels.push([data[i], data[i + 1], data[i + 2]]);
        }
        const palette = this._quantize(pixels, 6);
        const sorted = palette.sort((a, b) => this._luminance(b) - this._luminance(a));
        const presets = this._generatePresetsFromSorted(sorted);
        resolve(presets);
      };
      img.onerror = () => {
        const def = this._defaultPalette();
        resolve({
          vibrant: def,
          amoled: { ...def, background: '#000000', surface: '#0f0f16' },
          pastel: { ...def, primary: '#a0b0d0', glowColor: '#a0b0d0' }
        });
      };
      // Adiciona um cache-buster timestamp para evitar que o cache do navegador sem CORS bloqueie a extração no Canvas (Tainted Canvas error)
      const separator = imageUrl.includes('?') ? '&' : '?';
      img.src = imageUrl + separator + 't=' + Date.now();
    });
  },

  _generatePresetsFromSorted(sorted) {
    if (sorted.length < 4) {
      const def = this._defaultPalette();
      return {
        vibrant: def,
        amoled: { ...def, background: '#000000', surface: '#0f0f16' },
        pastel: { ...def, primary: '#a0b0d0', glowColor: '#a0b0d0' }
      };
    }
    const dark = sorted.filter(c => this._luminance(c) < 80);
    const mid = sorted.filter(c => this._luminance(c) >= 80 && this._luminance(c) < 180);
    const light = sorted.filter(c => this._luminance(c) >= 180);

    const pDark = dark.length > 0 ? dark[0] : [10, 10, 20];
    const pLight = light.length > 0 ? light[light.length - 1] : [200, 200, 220];
    const pAccent = mid.length > 0 ? mid[0] : (light.length > 0 ? light[Math.floor(light.length / 2)] : [100, 100, 180]);

    const pAccentBright = sorted[0];
    const pAccentMuted = mid.length > 1 ? mid[1] : (light.length > 1 ? light[1] : [120, 120, 150]);

    const vibrant = {
      background: this._rgbToHex(this._darken(pDark, 0.6)),
      surface: this._rgbToHex(this._darken(pDark, 0.3)),
      primary: this._rgbToHex(pAccent),
      textPrimary: this._rgbToHex(pLight),
      textSecondary: this._rgbToHex(this._mix(pLight, [120, 120, 140], 0.5)),
      glowColor: this._rgbToHex(pAccent)
    };

    const amoled = {
      background: '#000000',
      surface: '#0f0f16',
      primary: this._rgbToHex(pAccentBright),
      textPrimary: '#ffffff',
      textSecondary: '#94a3b8',
      glowColor: this._rgbToHex(pAccentBright)
    };

    const pastel = {
      background: this._rgbToHex(this._darken(pDark, 0.45)),
      surface: this._rgbToHex(this._darken(pDark, 0.25)),
      primary: this._rgbToHex(pAccentMuted),
      textPrimary: '#f8fafc',
      textSecondary: '#94a3b8',
      glowColor: this._rgbToHex(pAccentMuted)
    };

    return { vibrant, amoled, pastel };
  },

  _luminance([r, g, b]) {
    return 0.299 * r + 0.587 * g + 0.114 * b;
  },

  _colorDist(a, b) {
    const dr = a[0] - b[0];
    const dg = a[1] - b[1];
    const db = a[2] - b[2];
    return dr * dr + dg * dg + db * db;
  },

  _quantize(pixels, k) {
    if (pixels.length === 0) return [[20, 20, 30]];
    const step = Math.max(1, Math.floor(pixels.length / 10000));
    const sampled = [];
    for (let i = 0; i < pixels.length; i += step) {
      sampled.push(pixels[i]);
    }
    let centroids = [];
    for (let i = 0; i < k; i++) {
      centroids.push(sampled[Math.floor(Math.random() * sampled.length)]);
    }
    for (let iter = 0; iter < 20; iter++) {
      const clusters = Array.from({ length: k }, () => []);
      for (const p of sampled) {
        let minDist = Infinity;
        let best = 0;
        for (let i = 0; i < k; i++) {
          const d = this._colorDist(p, centroids[i]);
          if (d < minDist) { minDist = d; best = i; }
        }
        clusters[best].push(p);
      }
      let moved = false;
      for (let i = 0; i < k; i++) {
        if (clusters[i].length === 0) continue;
        const avg = clusters[i].reduce((a, c) => [a[0] + c[0], a[1] + c[1], a[2] + c[2]], [0, 0, 0]);
        const n = clusters[i].length;
        const nc = [Math.round(avg[0] / n), Math.round(avg[1] / n), Math.round(avg[2] / n)];
        if (this._colorDist(nc, centroids[i]) > 1) moved = true;
        centroids[i] = nc;
      }
      if (!moved) break;
    }
    return centroids;
  },

  _rgbToHex([r, g, b]) {
    return '#' + [r, g, b].map(c => Math.max(0, Math.min(255, Math.round(c))).toString(16).padStart(2, '0')).join('');
  },

  _assignPalette(sorted) {
    if (sorted.length < 4) return this._defaultPalette();
    const dark = sorted.filter(c => this._luminance(c) < 80);
    const mid = sorted.filter(c => this._luminance(c) >= 80 && this._luminance(c) < 180);
    const light = sorted.filter(c => this._luminance(c) >= 180);

    const pDark = dark.length > 0 ? dark[0] : [10, 10, 20];
    const pLight = light.length > 0 ? light[light.length - 1] : [200, 200, 220];
    const pAccent = mid.length > 0 ? mid[0] : (light.length > 0 ? light[Math.floor(light.length / 2)] : [100, 100, 180]);

    const background = this._rgbToHex(this._darken(pDark, 0.6));
    const surface = this._rgbToHex(this._darken(pDark, 0.3));
    const primary = this._rgbToHex(pAccent);
    const textPrimary = this._rgbToHex(pLight);
    const textSecondary = this._rgbToHex(this._mix(pLight, [120, 120, 140], 0.5));
    const glowColor = this._rgbToHex(pAccent);

    return { background, surface, primary, textPrimary, textSecondary, glowColor };
  },

  _darken([r, g, b], f) {
    return [Math.round(r * f), Math.round(g * f), Math.round(b * f)];
  },

  _mix([r1, g1, b1], [r2, g2, b2], t) {
    return [
      Math.round(r1 * (1 - t) + r2 * t),
      Math.round(g1 * (1 - t) + g2 * t),
      Math.round(b1 * (1 - t) + b2 * t)
    ];
  },

  _defaultPalette() {
    return {
      background: '#06060A',
      surface: '#0E0E14',
      primary: '#FFFFFF',
      textPrimary: '#FFFFFF',
      textSecondary: '#A0A0B0',
      glowColor: '#FFFFFF'
    };
  }
};
