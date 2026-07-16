class WallpaperEditor {
  constructor(viewportEl, imageEl) {
    this.viewport = viewportEl;
    this.image = imageEl;
    this.scale = 1;
    this.offsetX = 0;
    this.offsetY = 0;
    this.minScale = 0.3;
    this.maxScale = 5;
    this._isDragging = false;
    this._startX = 0;
    this._startY = 0;
    this._startOffX = 0;
    this._startOffY = 0;
    this._pinchDist = 0;
    this._pinchScale = 1;
    this._onChange = null;

    this._bindEvents();
  }

  set onChange(fn) { this._onChange = fn; }

  get frameRatio() { return 9 / 19.5; }

  getFrameRect() {
    const vp = this.viewport.getBoundingClientRect();
    const cropFrame = this.viewport.querySelector('.phone-crop-frame');
    if (cropFrame) {
      const rect = cropFrame.getBoundingClientRect();
      if (rect.width > 10) {
        return {
          x: rect.left - vp.left,
          y: rect.top - vp.top,
          w: rect.width,
          h: rect.height,
          vpW: vp.width,
          vpH: vp.height
        };
      }
    }
    // Fallback if elements aren't rendered or visible yet
    const fw = vp.width * (vp.width < 680 ? 0.60 : 0.45);
    const fh = fw / this.frameRatio;
    return {
      x: (vp.width - fw) / 2,
      y: (vp.height - fh) / 2,
      w: fw,
      h: fh,
      vpW: vp.width,
      vpH: vp.height
    };
  }

  loadImage(src) {
    return new Promise((resolve) => {
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = () => {
        this.naturalWidth = img.naturalWidth;
        this.naturalHeight = img.naturalHeight;
        this.image.src = src;
        this.image.onload = () => {
          this._calcMinScale();
          this.reset();
          this._notify();
          resolve();
        };
      };
      img.onerror = () => resolve();
      img.src = src;
    });
  }

  _calcMinScale() {
    const f = this.getFrameRect();
    const imgW = this.naturalWidth || f.vpW;
    const imgH = this.naturalHeight || f.vpH;
    const coverScale = Math.max(f.vpW / imgW, f.vpH / imgH);
    const dspW = imgW * coverScale;
    const dspH = imgH * coverScale;
    const sx = f.w / dspW;
    const sy = f.h / dspH;
    this.minScale = Math.max(sx, sy, 0.2);
  }

  reset() {
    this.scale = 1;
    this.offsetX = 0;
    this.offsetY = 0;
    this._clampOffset();
    this._applyTransform();
    this._notify();
  }

  center() {
    this.offsetX = 0;
    this.offsetY = 0;
    this._clampOffset();
    this._applyTransform();
    this._notify();
  }

  fit() {
    const f = this.getFrameRect();
    const iw = this.image.naturalWidth || f.vpW;
    const ih = this.image.naturalHeight || f.vpH;
    const sx = f.w / iw;
    const sy = f.h / ih;
    this.scale = Math.min(sx, sy);
    this.offsetX = 0;
    this.offsetY = 0;
    this._clampOffset();
    this._applyTransform();
    this._notify();
  }

  fill() {
    const f = this.getFrameRect();
    const iw = this.image.naturalWidth || f.vpW;
    const ih = this.image.naturalHeight || f.vpH;
    const sx = f.w / iw;
    const sy = f.h / ih;
    this.scale = Math.max(sx, sy);
    this.offsetX = 0;
    this.offsetY = 0;
    this._clampOffset();
    this._applyTransform();
    this._notify();
  }

  zoom(delta, cx, cy) {
    const vp = this.viewport.getBoundingClientRect();
    const mx = cx - vp.left;
    const my = cy - vp.top;
    const cw = vp.width;
    const ch = vp.height;

    const px = (mx - cw / 2 - this.offsetX) / this.scale;
    const py = (my - ch / 2 - this.offsetY) / this.scale;

    let newScale = this.scale * (1 - delta * 0.1);
    newScale = Math.max(this.minScale, Math.min(this.maxScale, newScale));

    this.offsetX = mx - cw / 2 - px * newScale;
    this.offsetY = my - ch / 2 - py * newScale;
    this.scale = newScale;
    this._clampOffset();
    this._applyTransform();
    this._notify();
  }

  getCropParams() {
    const vp = this.viewport.getBoundingClientRect();
    return {
      scale: this.scale,
      offsetX: this.offsetX,
      offsetY: this.offsetY
    };
  }

  getImageCropRect() {
    const f = this.getFrameRect();
    const imgW = this.naturalWidth || f.vpW;
    const imgH = this.naturalHeight || f.vpH;
    const coverScale = Math.max(f.vpW / imgW, f.vpH / imgH);

    let left = Math.round((f.x - f.vpW / 2 - this.offsetX) / (coverScale * this.scale) + imgW / 2);
    let top = Math.round((f.y - f.vpH / 2 - this.offsetY) / (coverScale * this.scale) + imgH / 2);
    let width = Math.round(f.w / (coverScale * this.scale));
    let height = Math.round(f.h / (coverScale * this.scale));

    // Garante que o aspect ratio de 9/19.5 (f.w / f.h) seja preservado ao limitar dimensões
    const targetRatio = f.w / f.h;
    if (height > imgH) {
      height = imgH;
      width = Math.round(height * targetRatio);
    }
    if (width > imgW) {
      width = imgW;
      height = Math.round(width / targetRatio);
    }

    left = Math.max(0, Math.min(left, imgW - width));
    top = Math.max(0, Math.min(top, imgH - height));

    return { left, top, width, height };
  }

  _applyTransform() {
    this.image.style.transform = `translate(${this.offsetX}px, ${this.offsetY}px) scale(${this.scale})`;
  }

  _notify() {
    if (this._onChange) this._onChange(this.getCropParams());
  }

  _bindEvents() {
    this.viewport.addEventListener('mousedown', this._onMouseDown.bind(this));
    document.addEventListener('mousemove', this._onMouseMove.bind(this));
    document.addEventListener('mouseup', this._onMouseUp.bind(this));
    this.viewport.addEventListener('wheel', this._onWheel.bind(this), { passive: false });
    this.viewport.addEventListener('touchstart', this._onTouchStart.bind(this), { passive: false });
    this.viewport.addEventListener('touchmove', this._onTouchMove.bind(this), { passive: false });
    this.viewport.addEventListener('touchend', this._onTouchEnd.bind(this));
  }

  _onMouseDown(e) {
    this._isDragging = true;
    this._startX = e.clientX;
    this._startY = e.clientY;
    this._startOffX = this.offsetX;
    this._startOffY = this.offsetY;
  }

  _clampOffset() {
    const f = this.getFrameRect();
    const imgW = this.naturalWidth || f.vpW;
    const imgH = this.naturalHeight || f.vpH;
    const coverScale = Math.max(f.vpW / imgW, f.vpH / imgH);
    const dispW = imgW * coverScale * this.scale;
    const dispH = imgH * coverScale * this.scale;
    const maxOffX = Math.max(0, (dispW - f.w) / 2);
    const maxOffY = Math.max(0, (dispH - f.h) / 2);
    this.offsetX = Math.max(-maxOffX, Math.min(maxOffX, this.offsetX));
    this.offsetY = Math.max(-maxOffY, Math.min(maxOffY, this.offsetY));
  }

  _onMouseMove(e) {
    if (!this._isDragging) return;
    const dx = e.clientX - this._startX;
    const dy = e.clientY - this._startY;
    this.offsetX = this._startOffX + dx;
    this.offsetY = this._startOffY + dy;
    this._clampOffset();
    this._applyTransform();
    this._notify();
  }

  _onMouseUp() {
    this._isDragging = false;
  }

  _onWheel(e) {
    e.preventDefault();
    this.zoom(e.deltaY > 0 ? 1 : -1, e.clientX, e.clientY);
  }

  _onTouchStart(e) {
    if (e.touches.length === 1) {
      this._isDragging = true;
      this._startX = e.touches[0].clientX;
      this._startY = e.touches[0].clientY;
      this._startOffX = this.offsetX;
      this._startOffY = this.offsetY;
    } else if (e.touches.length === 2) {
      this._isDragging = false;
      const t = e.touches;
      this._pinchDist = Math.hypot(t[0].clientX - t[1].clientX, t[0].clientY - t[1].clientY);
      this._pinchScale = this.scale;
      this._pinchCenter = {
        x: (t[0].clientX + t[1].clientX) / 2,
        y: (t[0].clientY + t[1].clientY) / 2
      };
    }
  }

  _onTouchMove(e) {
    e.preventDefault();
    if (e.touches.length === 1 && this._isDragging) {
      const dx = e.touches[0].clientX - this._startX;
      const dy = e.touches[0].clientY - this._startY;
      this.offsetX = this._startOffX + dx;
      this.offsetY = this._startOffY + dy;
      this._clampOffset();
      this._applyTransform();
      this._notify();
    } else if (e.touches.length === 2) {
      const t = e.touches;
      const dist = Math.hypot(t[0].clientX - t[1].clientX, t[0].clientY - t[1].clientY);
      const newScale = this._pinchScale * (dist / this._pinchDist);
      const cx = this._pinchCenter.x;
      const cy = this._pinchCenter.y;
      const vp = this.viewport.getBoundingClientRect();
      const mx = cx - vp.left;
      const my = cy - vp.top;
      const cw = vp.width;
      const ch = vp.height;
      const px = (mx - cw / 2 - this.offsetX) / this.scale;
      const py = (my - ch / 2 - this.offsetY) / this.scale;
      let clamped = Math.max(this.minScale, Math.min(this.maxScale, newScale));
      this.offsetX = mx - cw / 2 - px * clamped;
      this.offsetY = my - ch / 2 - py * clamped;
      this.scale = clamped;
      this._clampOffset();
      this._applyTransform();
      this._notify();
    }
  }

  _onTouchEnd() {
    this._isDragging = false;
  }
}
