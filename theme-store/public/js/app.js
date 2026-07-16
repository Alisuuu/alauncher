(function() {
  const $ = id => document.getElementById(id);
  const qs = (sel, ctx) => (ctx || document).querySelector(sel);
  const qsa = (sel, ctx) => (ctx || document).querySelectorAll(sel);

  const searchInput = $('searchInput');
  const searchBtn = $('searchBtn');
  const resultsGrid = $('resultsGrid');
  const emptyState = $('emptyState');
  const loadingSearch = $('loadingSearch');
  const searchView = $('searchView');
  const editorView = $('editorView');
  const backBtn = $('backBtn');
  const editorTitle = $('editorTitle');
  const cropViewport = $('cropViewport');
  const wallpaperImg = $('wallpaperImg');
  const previewWallpaper = $('previewWallpaper');
  const backdropSelector = $('backdropSelector');
  const altBackdropSelector = $('altBackdropSelector');
  const altBackdropPreview = $('altBackdropPreview');
  const posterSelector = $('posterSelector');
  const logoSelector = $('logoSelector');
  const posterPreview = $('posterPreview');
  const logoPreview = $('logoPreview');
  const colorList = $('colorList');
  const downloadBtn = $('downloadBtn');
  const toast = $('toast');



  const themeNameInput = $('themeNameInput');
  const themeAuthorInput = $('themeAuthorInput');
  const themeDescInput = $('themeDescInput');
  const cardRadiusInput = $('cardRadiusInput');
  const searchRadiusInput = $('searchRadiusInput');
  const columnsInput = $('columnsInput');
  const showBordersInput = $('showBordersInput');
  const showAppNameInput = $('showAppNameInput');
  
  const cardRadiusVal = $('cardRadiusVal');
  const searchRadiusVal = $('searchRadiusVal');
  const columnsVal = $('columnsVal');

  let editor = null;
  let currentItem = null;
  let currentColors = null;
  let toastTimer = null;

  function showToast(msg, duration) {
    clearTimeout(toastTimer);
    toast.textContent = msg;
    toast.classList.remove('hidden');
    toastTimer = setTimeout(() => toast.classList.add('hidden'), duration || 3000);
  }

  function showView(view) {
    qsa('.view').forEach(v => v.classList.remove('active'));
    view.classList.add('active');
    
    // Reseta a rolagem para o topo ao trocar de tela
    window.scrollTo(0, 0);
    document.documentElement.scrollTop = 0;
    document.body.scrollTop = 0;
    view.scrollTop = 0;
  }

  searchInput.addEventListener('keydown', e => {
    if (e.key === 'Enter') doSearch();
  });
  searchBtn.addEventListener('click', doSearch);

  let searchTimeout;
  let currentCategory = '';

  searchInput.addEventListener('input', () => {
    clearTimeout(searchTimeout);
    const q = searchInput.value.trim();
    if (q.length < 2 && !currentCategory) {
      resultsGrid.innerHTML = '';
      emptyState.classList.remove('hidden');
      return;
    }
    searchTimeout = setTimeout(doSearch, 400);
  });

  qsa('.cat-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      qsa('.cat-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      currentCategory = btn.dataset.cat;
      doSearch();
    });
  });

  async function doSearch() {
    const q = searchInput.value.trim();
    if (!q || q.length < 2) {
      if (!currentCategory) return;
    }
    loadingSearch.classList.remove('hidden');
    emptyState.classList.add('hidden');
    resultsGrid.innerHTML = '';
    try {
      let results;
      if (currentCategory === 'anime') {
        results = await API.search(q || 'anime');
      } else if (currentCategory && !q) {
        results = await API.discover(currentCategory);
      } else {
        results = await API.search(q);
      }
      loadingSearch.classList.add('hidden');
      if (results.length === 0) {
        resultsGrid.innerHTML = '<div class="empty-state"><p>Nenhum resultado encontrado</p></div>';
        return;
      }
      renderResults(results);
    } catch (err) {
      loadingSearch.classList.add('hidden');
      showToast('Erro ao pesquisar: ' + err.message);
    }
  }

  function renderResults(results) {
    resultsGrid.innerHTML = '';
    results.forEach((item, index) => {
      const card = document.createElement('div');
      card.className = 'result-card';
      card.style.animationDelay = (index * 40) + 'ms';
      card.innerHTML = `
        <div class="result-poster">
          ${item.poster
            ? `<img src="https://image.tmdb.org/t/p/w342${item.poster}" alt="${item.title}" loading="lazy">`
            : '<div class="no-poster">Sem imagem</div>'}
        </div>
      `;
      card.addEventListener('click', () => selectItem(item));
      resultsGrid.appendChild(card);
    });
  }

  function thumbUrl(url, size) {
    if (!url) return '';
    return url.replace('/original/', '/' + size + '/');
  }

  function renderSelector(container, images, type, onSelect) {
    container.innerHTML = '';
    if (!images || images.length === 0) {
      container.innerHTML = '<div class="img-option"><div class="empty-opt">Nenhum</div></div>';
      return;
    }
    images.forEach((url, i) => {
      const opt = document.createElement('div');
      opt.className = 'img-option' + (i === 0 ? ' active' : '');
      const img = document.createElement('img');
      img.src = thumbUrl(url, type === 'logo' ? 'w500' : 'w185');
      img.loading = 'lazy';
      img.onerror = function() {
        this.parentElement.innerHTML = '<div class="empty-opt">Erro</div>';
      };
      opt.appendChild(img);
      opt.addEventListener('click', () => {
        qsa('.img-option', container).forEach(o => o.classList.remove('active'));
        opt.classList.add('active');
        onSelect(url, i);
      });
      container.appendChild(opt);
    });
  }

  async function selectItem(item) {
    const loadStart = Date.now();
    const loadingOverlay = $('loadingOverlay');
    if (loadingOverlay) loadingOverlay.classList.remove('hidden');

    showView(editorView);
    if (floatingCreateBtn) floatingCreateBtn.classList.add('hidden');
    editorTitle.textContent = `Carregando "${item.title}"...`;
    currentItem = item;
    downloadBtn.disabled = true;
    downloadBtn.textContent = 'Carregando...';

    try {
      const details = await API.details(item.id, item.mediaType || 'movie');
      currentItem = { ...item, ...details };

      showView(editorView);
      editorTitle.textContent = details.title;

      const backdropUrl = details.backdrop || (details.backdrops && details.backdrops[0]);
      const backdropPromise = loadBackdrop(backdropUrl);
      loadPoster(details.poster || (details.posters && details.posters[0]));
      loadLogo(details.logo);

      renderSelector(backdropSelector, details.backdrops, 'backdrop', (url) => loadBackdrop(url));
      renderSelector(altBackdropSelector, details.backdrops, 'backdrop', (url) => {
        loadAltBackdrop(url);
      });
      renderSelector(posterSelector, details.posters, 'poster', (url) => {
        loadPoster(url);
        currentItem.poster = url;
      });
      renderSelector(logoSelector, details.logos, 'logo', (url) => {
        loadLogo(url);
        currentItem.logo = url;
      });

      const imgForColors = backdropUrl || details.poster || (details.posters && details.posters[0]);
      let colorsPromise;
      if (imgForColors) {
        colorsPromise = (async () => {
          const presets = await ColorUtils.extractPalette(imgForColors);
          currentColors = presets.vibrant;
        })();
      } else {
        currentColors = ColorUtils._defaultPalette();
      }

      await backdropPromise;
      if (colorsPromise) await colorsPromise;

      renderColorList(currentColors);
      updatePhoneColors();

      // Atualiza os inputs globais do tema
      if (themeNameInput) themeNameInput.value = details.title;
      if (themeAuthorInput) themeAuthorInput.value = 'Theme Store';
      if (themeDescInput) themeDescInput.value = 'Theme inspired by ' + details.title;
      
      // Reseta os inputs de layout para os padrões
      if (cardRadiusInput) {
        cardRadiusInput.value = 16;
        cardRadiusVal.textContent = '16px';
      }
      if (searchRadiusInput) {
        searchRadiusInput.value = 28;
        searchRadiusVal.textContent = '28px';
      }
      if (columnsInput) {
        columnsInput.value = 4;
        columnsVal.textContent = '4';
      }
      if (showBordersInput) showBordersInput.checked = true;
      if (showAppNameInput) showAppNameInput.checked = true;

      downloadBtn.disabled = false;
      downloadBtn.innerHTML = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg> Baixar Tema`;
    } catch (err) {
      showToast('Erro ao carregar: ' + err.message);
      downloadBtn.disabled = false;
      downloadBtn.innerHTML = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg> Baixar Tema';
    }

    // Mínimo de 600ms para o overlay ser visível
    const elapsed = Date.now() - loadStart;
    const minDelay = 600 - elapsed;
    if (minDelay > 0) await new Promise(r => setTimeout(r, minDelay));
    if (loadingOverlay) loadingOverlay.classList.add('hidden');
  }

  async function loadBackdrop(url) {
    if (!url) return;
    currentItem.backdrop = url;
    if (!editor) {
      editor = new WallpaperEditor(cropViewport, wallpaperImg);
      editor.onChange = onCropChange;
    }
    wallpaperImg.classList.remove('no-display');
    previewWallpaper.style.backgroundImage = `url(${url})`;
    previewWallpaper.style.backgroundSize = 'cover';
    previewWallpaper.style.backgroundPosition = 'center';

    await editor.loadImage(url);

    const presets = await ColorUtils.extractPalette(url);
    currentColors = presets.vibrant;
    renderColorList(currentColors);
    updatePhoneColors();
  }

  function loadPoster(url) {
    currentItem.poster = url;
    posterPreview.innerHTML = '';
    if (url) {
      const img = document.createElement('img');
      img.src = url;
      posterPreview.appendChild(img);
    } else {
      posterPreview.innerHTML = '<div class="img-preview-empty">Nenhum</div>';
    }
  }

  function loadLogo(url) {
    currentItem.logo = url;
    logoPreview.innerHTML = '';
    if (url) {
      const img = document.createElement('img');
      img.src = url;
      logoPreview.appendChild(img);
    } else {
      logoPreview.innerHTML = '<div class="img-preview-empty">Nenhum</div>';
    }
    updatePhoneColors();
  }

  function loadAltBackdrop(url) {
    currentItem.altBackdrop = url;
    altBackdropPreview.innerHTML = '';
    if (url) {
      const img = document.createElement('img');
      img.src = url;
      altBackdropPreview.appendChild(img);
    } else {
      altBackdropPreview.innerHTML = '<div class="img-preview-empty">Nenhum</div>';
    }
  }

  function onCropChange(params) {
    updatePhonePreview();
  }

  function updatePhonePreview() {
    if (!editor || !editor.naturalWidth || !currentItem || !currentItem.backdrop) return;
    const crop = editor.getImageCropRect();
    const ph = previewWallpaper.getBoundingClientRect();
    if (!ph.width) return;
    const imgW = editor.naturalWidth;
    const imgH = editor.naturalHeight;
    const scale = ph.width / crop.width;
    previewWallpaper.style.backgroundSize = `${Math.round(imgW * scale)}px ${Math.round(imgH * scale)}px`;
    previewWallpaper.style.backgroundPosition = `${-Math.round(crop.left * scale)}px ${-Math.round(crop.top * scale)}px`;
  }

  function updatePhoneColors() {
    if (!currentColors) return;
    const { background, surface, primary, textPrimary, textSecondary, glowColor } = currentColors;



    const screen = qs('.phone-screen');
    const time = qs('.phone-statusbar .time');
    const icons = qsa('.phone-icon span');
    const iconPlaceholders = qsa('.icon-placeholder');
    
    const mascotWrapper = $('phoneMascotWrapper');
    const clockContainer = $('phoneClockContainer');
    const clockTime = qs('.phone-clock-time');
    const clockDate = qs('.phone-clock-date');
    const weatherDivider = qs('.phone-weather-divider');
    const weatherIcon = qs('.phone-weather-icon');
    const weatherTemp = qs('.phone-weather-temp');
    const weatherCondition = qs('.phone-weather-condition');

    if (screen) {
      screen.style.backgroundColor = background;
    }
    if (time) {
      time.style.color = textPrimary;
    }
    icons.forEach(span => {
      span.style.color = textPrimary;
    });
    iconPlaceholders.forEach(ph => {
      ph.style.backgroundColor = surface;
      ph.style.borderColor = primary + '33';
      ph.style.boxShadow = `0 0 10px ${glowColor}25`;
      const svg = ph.querySelector('svg');
      if (svg) {
        svg.style.stroke = primary;
        svg.style.filter = `drop-shadow(0 0 2px ${glowColor})`;
      }
    });

    if (clockContainer) {
      clockContainer.style.backgroundColor = surface + '73'; // 45% opacity
      const showBorders = showBordersInput ? showBordersInput.checked : true;
      clockContainer.style.border = showBorders ? `1px solid ${primary}4d` : 'none';
      const radius = cardRadiusInput ? cardRadiusInput.value : 16;
      clockContainer.style.borderRadius = radius + 'px';
    }

    const showNames = showAppNameInput ? showAppNameInput.checked : true;
    qsa('.phone-dock .phone-icon span').forEach(span => {
      span.style.display = showNames ? 'block' : 'none';
    });

    if (clockTime) clockTime.style.color = textPrimary;
    if (clockDate) clockDate.style.color = textSecondary;
    if (weatherDivider) weatherDivider.style.backgroundColor = textSecondary + '33';
    if (weatherIcon) weatherIcon.style.color = textPrimary;
    if (weatherTemp) weatherTemp.style.color = textPrimary;
    if (weatherCondition) weatherCondition.style.color = textSecondary;

    if (mascotWrapper) {
      if (currentItem && currentItem.logo) {
        mascotWrapper.innerHTML = `<img src="${currentItem.logo}" alt="Mascot" style="width: 100%; height: 100%; object-fit: contain; filter: drop-shadow(0 0 6px ${glowColor});">`;
      } else {
        mascotWrapper.innerHTML = `<div class="phone-mascot-placeholder" style="background-color: ${primary}40;"></div>`;
      }
    }
  }



  function renderColorList(colors) {
    colorList.innerHTML = '';
    if (!colors) return;
    const labels = {
      background: 'Background',
      surface: 'Surface',
      primary: 'Primary',
      textPrimary: 'Text Primary',
      textSecondary: 'Text Secondary',
      glowColor: 'Glow Color'
    };
    for (const [key, hex] of Object.entries(colors)) {
      const item = document.createElement('div');
      item.className = 'color-item';
      const input = document.createElement('input');
      input.type = 'color';
      input.value = hex;
      input.addEventListener('input', () => {
        swatch.style.backgroundColor = input.value;
        hexLabel.textContent = input.value;
        currentColors[key] = input.value;
        updatePhoneColors();
      });
      const swatch = document.createElement('div');
      swatch.className = 'color-swatch';
      swatch.style.backgroundColor = hex;
      swatch.addEventListener('click', () => input.click());
      const label = document.createElement('span');
      label.className = 'color-label';
      label.textContent = labels[key] || key;
      const hexLabel = document.createElement('span');
      hexLabel.className = 'color-hex';
      hexLabel.textContent = hex;
      item.appendChild(input);
      item.appendChild(swatch);
      item.appendChild(label);
      item.appendChild(hexLabel);
      colorList.appendChild(item);
    }
    updatePhoneColors();
  }



  // Helper para carregar scripts externos dinamicamente (ex: JSZip)
  function loadScript(src) {
    return new Promise((resolve, reject) => {
      if (document.querySelector(`script[src="${src}"]`)) return resolve();
      const script = document.createElement('script');
      script.src = src;
      script.onload = resolve;
      script.onerror = reject;
      document.head.appendChild(script);
    });
  }

  // Desenha o recorte do wallpaper em um canvas para exportar como Blob no cliente
  function getCroppedWallpaperBlob(imageUrl, crop) {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = () => {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        const cw = crop.cropWidth || img.naturalWidth;
        const ch = crop.cropHeight || img.naturalHeight;
        canvas.width = cw;
        canvas.height = ch;
        
        ctx.drawImage(
          img,
          crop.cropLeft || 0,
          crop.cropTop || 0,
          cw,
          ch,
          0,
          0,
          cw,
          ch
        );
        canvas.toBlob(resolve, 'image/jpeg', 0.9);
      };
      img.onerror = () => {
        // Fallback: tela escura padrão caso falhe
        const canvas = document.createElement('canvas');
        canvas.width = 1080;
        canvas.height = 2340;
        const ctx = canvas.getContext('2d');
        ctx.fillStyle = '#06060A';
        ctx.fillRect(0, 0, 1080, 2340);
        canvas.toBlob(resolve, 'image/jpeg', 0.9);
      };
      const separator = imageUrl.includes('?') ? '&' : '?';
      img.src = imageUrl.startsWith('blob:') ? imageUrl : (imageUrl + separator + 't=' + Date.now());
    });
  }

  // Faz o download da imagem como Blob, contornando falhas de CORS
  async function fetchAsBlob(url, defaultColor) {
    if (!url) return null;
    try {
      if (url.startsWith('blob:')) {
        return await fetch(url).then(r => r.blob());
      }
      const separator = url.includes('?') ? '&' : '?';
      const fetchUrl = url + separator + 't=' + Date.now();
      return await fetch(fetchUrl).then(r => r.blob());
    } catch (e) {
      const canvas = document.createElement('canvas');
      canvas.width = 300;
      canvas.height = 450;
      const ctx = canvas.getContext('2d');
      ctx.fillStyle = defaultColor || '#0E0E14';
      ctx.fillRect(0, 0, 300, 450);
      return new Promise(res => canvas.toBlob(res, 'image/jpeg', 0.9));
    }
  }

  function isSvgBlob(blob) {
    if (blob.type === 'image/svg+xml') return true;
    if (blob.type === 'image/png' || blob.type === 'image/jpeg' || blob.type === 'image/webp') return false;
    return blob.size < 20480;
  }

  async function svgToPng(blob) {
    const text = await blob.text();
    const img = new Image();
    const url = URL.createObjectURL(new Blob([text], { type: 'image/svg+xml' }));
    try {
      await new Promise((resolve, reject) => {
        img.onload = resolve;
        img.onerror = reject;
        img.src = url;
      });
      const canvas = document.createElement('canvas');
      canvas.width = 400;
      canvas.height = 400;
      const ctx = canvas.getContext('2d');
      ctx.drawImage(img, 0, 0, 400, 400);
      return await new Promise(res => canvas.toBlob(res, 'image/png'));
    } finally {
      URL.revokeObjectURL(url);
    }
  }

  // Botão Criar Flutuante e Uploads Manuais
  const floatingCreateBtn = $('floatingCreateBtn');
  const uploadBackdrop = $('uploadBackdrop');
  const uploadPoster = $('uploadPoster');
  const uploadLogo = $('uploadLogo');

  if (floatingCreateBtn) {
    floatingCreateBtn.addEventListener('click', () => {
      currentItem = {
        title: 'Tema Personalizado',
        id: 'custom_' + Date.now(),
        backdrop: null,
        altBackdrop: null,
        poster: null,
        logo: null
      };
      currentColors = ColorUtils._defaultPalette();
      
      // Reseta painéis do editor
      editorTitle.textContent = currentItem.title;
      backdropSelector.innerHTML = '<span style="font-size:0.75rem; color:var(--text-secondary);">Faça upload do wallpaper acima...</span>';
      altBackdropSelector.innerHTML = '';
      altBackdropPreview.innerHTML = '<div class="img-preview-empty">Nenhum</div>';
      posterSelector.innerHTML = '';
      logoSelector.innerHTML = '';
      posterPreview.innerHTML = '<div class="img-preview-empty">Nenhum</div>';
      logoPreview.innerHTML = '<div class="img-preview-empty">Nenhum</div>';
      
      // Reseta editor do wallpaper
      if (editor) {
        editor.naturalWidth = null;
        editor.naturalHeight = null;
        wallpaperImg.src = '';
        wallpaperImg.classList.add('no-display');
      }
      
      previewWallpaper.style.backgroundImage = 'none';
      previewWallpaper.style.backgroundColor = currentColors.background;

      // Inicializa os inputs globais
      if (themeNameInput) themeNameInput.value = currentItem.title;
      if (themeAuthorInput) themeAuthorInput.value = 'Theme Store';
      if (themeDescInput) themeDescInput.value = 'Theme inspired by Custom';
      
      // Reseta os inputs de layout para os padrões
      if (cardRadiusInput) {
        cardRadiusInput.value = 16;
        cardRadiusVal.textContent = '16px';
      }
      if (searchRadiusInput) {
        searchRadiusInput.value = 28;
        searchRadiusVal.textContent = '28px';
      }
      if (columnsInput) {
        columnsInput.value = 4;
        columnsVal.textContent = '4';
      }
      if (showBordersInput) showBordersInput.checked = true;
      if (showAppNameInput) showAppNameInput.checked = true;
      
      renderColorList(currentColors);
      updatePhoneColors();
      
      showView(editorView);
      floatingCreateBtn.classList.add('hidden');
    });
  }

  if (uploadBackdrop) {
    uploadBackdrop.addEventListener('change', e => {
      const file = e.target.files[0];
      if (!file) return;
      const url = URL.createObjectURL(file);
      loadBackdrop(url);
    });
  }

  const uploadAltBackdrop = $('uploadAltBackdrop');
  if (uploadAltBackdrop) {
    uploadAltBackdrop.addEventListener('change', e => {
      const file = e.target.files[0];
      if (!file) return;
      const url = URL.createObjectURL(file);
      loadAltBackdrop(url);
    });
  }

  if (uploadPoster) {
    uploadPoster.addEventListener('change', e => {
      const file = e.target.files[0];
      if (!file) return;
      const url = URL.createObjectURL(file);
      loadPoster(url);
    });
  }

  if (uploadLogo) {
    uploadLogo.addEventListener('change', e => {
      const file = e.target.files[0];
      if (!file) return;
      const url = URL.createObjectURL(file);
      loadLogo(url);
    });
  }

  if (cardRadiusInput) {
    cardRadiusInput.addEventListener('input', () => {
      cardRadiusVal.textContent = cardRadiusInput.value + 'px';
      updatePhoneColors();
    });
  }

  if (searchRadiusInput) {
    searchRadiusInput.addEventListener('input', () => {
      searchRadiusVal.textContent = searchRadiusInput.value + 'px';
    });
  }

  if (columnsInput) {
    columnsInput.addEventListener('input', () => {
      columnsVal.textContent = columnsInput.value;
    });
  }

  if (showBordersInput) {
    showBordersInput.addEventListener('change', () => {
      updatePhoneColors();
    });
  }

  if (showAppNameInput) {
    showAppNameInput.addEventListener('change', () => {
      updatePhoneColors();
    });
  }

  backBtn.addEventListener('click', () => {
    showView(searchView);
    if (floatingCreateBtn) floatingCreateBtn.classList.remove('hidden');
  });

  downloadBtn.addEventListener('click', async () => {
    if (!currentItem || downloadBtn.disabled) return;
    downloadBtn.disabled = true;
    downloadBtn.textContent = 'Gerando...';

    let crop = {};
    if (editor && editor.naturalWidth) {
      const rect = editor.getImageCropRect();
      crop = {
        cropLeft: rect.left,
        cropTop: rect.top,
        cropWidth: rect.width,
        cropHeight: rect.height
      };
    }

    // Tenta gerar o ZIP inteiramente no cliente para aplicar todos os ajustes customizados
    try {
      if (typeof JSZip === 'undefined') {
        await loadScript('https://cdnjs.cloudflare.com/ajax/libs/jszip/3.10.1/jszip.min.js');
      }
      
      const zip = new JSZip();
      
      // 1. Wallpaper recortado
      if (currentItem.backdrop) {
        const wallBlob = await getCroppedWallpaperBlob(currentItem.backdrop, crop);
        zip.file('wallpaper.jpg', wallBlob);
      } else {
        const canvas = document.createElement('canvas');
        canvas.width = 1080;
        canvas.height = 2340;
        const ctx = canvas.getContext('2d');
        ctx.fillStyle = currentColors ? currentColors.background : '#06060A';
        ctx.fillRect(0, 0, 1080, 2340);
        const wallBlob = await new Promise(res => canvas.toBlob(res, 'image/jpeg', 0.9));
        zip.file('wallpaper.jpg', wallBlob);
      }
      
      // 2. Poster
      let posterBlob = null;
      if (currentItem.poster) {
        posterBlob = await fetchAsBlob(currentItem.poster, currentColors ? currentColors.surface : '#0E0E14');
        if (posterBlob) zip.file('character.jpg', posterBlob);
      }
      
      // 2b. Alt Backdrop (segundo wallpaper para card de músicas/configs)
      let altBackdropBlob = null;
      if (currentItem.altBackdrop) {
        altBackdropBlob = await fetchAsBlob(currentItem.altBackdrop, null);
        if (altBackdropBlob) zip.file('alt_wallpaper.jpg', altBackdropBlob);
      }
      
      // 3. Logo (convert SVG→PNG se necessário)
      let logoBlob = null;
      if (currentItem.logo) {
        logoBlob = await fetchAsBlob(currentItem.logo, null);
        if (logoBlob && isSvgBlob(logoBlob)) {
          try { logoBlob = await svgToPng(logoBlob); } catch (err) {
            console.error('Falha ao converter SVG→PNG:', err);
            showToast('Erro ao converter logo SVG para PNG');
            logoBlob = null;
          }
        }
        if (logoBlob) zip.file('symbol.png', logoBlob);
      }
      
      // 4. theme.json com os inputs configurados pelo usuário
      const themeJson = {
        name: themeNameInput ? themeNameInput.value : (currentItem.title || 'Tema Customizado'),
        id: 'tmdb_' + currentItem.id,
        author: themeAuthorInput ? themeAuthorInput.value : 'Theme Store',
        description: themeDescInput ? themeDescInput.value : `Theme inspired by ${currentItem.title}`,
        wallpaperPath: 'wallpaper.jpg',
        altBackdropPath: altBackdropBlob ? 'alt_wallpaper.jpg' : null,
        cardXmlPath: null,
        cardTexture: null,
        characterPath: posterBlob ? 'character.jpg' : null,
        symbolPath: logoBlob ? 'symbol.png' : null,
        clockStyle: 'character_card',
        useCardTexture: true,
        colors: currentColors || ColorUtils._defaultPalette(),
        shapes: {
          cardCornerRadius: cardRadiusInput ? parseInt(cardRadiusInput.value) : 16,
          searchBarCornerRadius: searchRadiusInput ? parseInt(searchRadiusInput.value) : 28,
          showBorders: showBordersInput ? showBordersInput.checked : true
        },
        layout: {
          columns: columnsInput ? parseInt(columnsInput.value) : 4,
          showAppName: showAppNameInput ? showAppNameInput.checked : true
        },
        texturePath: logoBlob ? 'symbol.png' : null
      };
      zip.file('theme.json', JSON.stringify(themeJson, null, 2));
      
      // 5. Baixar ZIP
      const blobContent = await zip.generateAsync({ type: 'blob' });
      const downloadUrl = URL.createObjectURL(blobContent);
      const a = document.createElement('a');
      a.href = downloadUrl;
      const safeName = (themeNameInput ? themeNameInput.value : (currentItem.title || 'theme')).replace(/[<>:"/\\|?*]/g, '_');
      a.download = `${safeName}_theme.zip`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(downloadUrl);
      showToast('Tema baixado com sucesso!');
      
      downloadBtn.disabled = false;
      downloadBtn.innerHTML = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg> Baixar Tema`;
      return;
    } catch (clientErr) {
      console.warn('Falha no download via client-side:', clientErr.message);
      // Se falhar no cliente (ex: bloqueio de rede rígido), tenta o fallback no servidor dedicado
      showToast('Compactando no servidor...');
    }

    // FALLBACK: Endpoint do servidor dedicado para TMDB
    const payload = {
      title: themeNameInput ? themeNameInput.value : currentItem.title,
      id: currentItem.id,
      backdropUrl: currentItem.backdrop,
      altBackdropUrl: currentItem.altBackdrop || null,
      posterUrl: currentItem.poster,
      logoUrl: currentItem.logo,
      ...crop,
      colors: currentColors || ColorUtils._defaultPalette()
    };

    try {
      const blob = await API.download(payload);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      const safeName = (themeNameInput ? themeNameInput.value : (currentItem.title || 'theme')).replace(/[<>:"/\\|?*]/g, '_');
      a.download = `${safeName}_theme.zip`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      showToast('Tema baixado com sucesso (servidor)!');
    } catch (err) {
      showToast('Erro ao baixar: ' + err.message);
    }
    
    downloadBtn.disabled = false;
    downloadBtn.innerHTML = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg> Baixar Tema`;
  });

  // Initialize to Filmes (Movie) category and load default movies on start!
  const moviesBtn = qs('.cat-btn[data-cat="movie"]');
  if (moviesBtn) {
    qsa('.cat-btn').forEach(b => b.classList.remove('active'));
    moviesBtn.classList.add('active');
    currentCategory = 'movie';
    doSearch();
  }
})();
