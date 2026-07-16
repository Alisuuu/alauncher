/* ═══════════════════════════════════════════════════════════════ */
/* Theme Presets - JavaScript                                       */
/* ═══════════════════════════════════════════════════════════════ */

const TMDB_API_KEY = '5e5da432e96174227b25086fe8637985';
const TMDB_BASE_URL = 'https://api.themoviedb.org/3';
const TMDB_IMG_URL = 'https://image.tmdb.org/t/p';

const DAYS_PT = ['Domingo', 'Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado'];
const MONTHS_PT = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'];

const weatherIcons = {
    sunny: `<svg viewBox="0 0 64 64" fill="none">
        <circle cx="32" cy="32" r="14" fill="#FFD93D" opacity="0.9"/>
        <g stroke="#FFD93D" stroke-width="2.5" stroke-linecap="round">
            <line x1="32" y1="4" x2="32" y2="12"/><line x1="32" y1="52" x2="32" y2="60"/>
            <line x1="4" y1="32" x2="12" y2="32"/><line x1="52" y1="32" x2="60" y2="32"/>
            <line x1="12.2" y1="12.2" x2="17.9" y2="17.9"/><line x1="46.1" y1="46.1" x2="51.8" y2="51.8"/>
            <line x1="12.2" y1="51.8" x2="17.9" y2="46.1"/><line x1="46.1" y1="17.9" x2="51.8" y2="12.2"/>
        </g>
    </svg>`,
    night: `<svg viewBox="0 0 64 64" fill="none">
        <path d="M38 12C30 12 24 18 24 26C24 34 30 40 38 40C42 40 46 38 48 35C44 37 39 36 36 32C33 28 33 22 36 18C38 15 40 13 38 12Z" fill="#F4D03F" opacity="0.9"/>
        <g fill="#F4D03F" opacity="0.4">
            <circle cx="16" cy="16" r="1.5"/><circle cx="52" cy="20" r="1"/><circle cx="48" cy="10" r="1.5"/>
            <circle cx="10" cy="30" r="1"/><circle cx="54" cy="34" r="1.5"/><circle cx="20" cy="48" r="1"/>
        </g>
    </svg>`,
    cloudy: `<svg viewBox="0 0 64 64" fill="none">
        <circle cx="24" cy="24" r="10" fill="#FFD93D" opacity="0.7"/>
        <path d="M20 44C14 44 10 40 10 35C10 30 14 26 20 26C20 20 26 16 32 16C38 16 44 20 44 26C50 26 54 30 54 35C54 40 50 44 44 44H20Z" fill="white" opacity="0.85"/>
    </svg>`
};

let currentThemeData = null;

function init() {
    loadCategory('trending');
    setupCategories();
    setupSearch();
    setupModalActions();
}

function setupCategories() {
    document.querySelectorAll('.category-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.category-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            loadCategory(btn.dataset.category);
        });
    });
}

function setupSearch() {
    const searchInput = document.getElementById('tmdb-search');
    let debounceTimer;
    
    searchInput.addEventListener('input', (e) => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            const query = e.target.value.trim();
            if (query.length >= 2) {
                searchTMDB(query);
            } else if (query.length === 0) {
                const activeCategory = document.querySelector('.category-btn.active');
                loadCategory(activeCategory.dataset.category);
            }
        }, 500);
    });
}

async function loadCategory(category) {
    const grid = document.getElementById('tmdb-grid');
    const loading = document.getElementById('tmdb-loading');
    
    grid.innerHTML = '';
    loading.style.display = 'flex';
    
    try {
        let items = [];
        
        switch (category) {
            case 'trending':
                const trending = await fetchTMDB('/trending/all/week');
                items = trending.results.filter(i => i.poster_path).slice(0, 40);
                break;
            case 'movies':
                const movies = await fetchTMDB('/movie/popular');
                items = movies.results.filter(i => i.poster_path).slice(0, 40);
                break;
            case 'tv':
                const tv = await fetchTMDB('/tv/popular');
                items = tv.results.filter(i => i.poster_path).slice(0, 40);
                break;
            case 'anime':
                const anime = await fetchTMDB('/discover/tv', '&with_genres=16&with_origin_country=JP');
                items = anime.results.filter(i => i.poster_path).slice(0, 40);
                break;
        }
        
        console.log('Items loaded:', items.length);
        renderItems(items);
    } catch (error) {
        console.error('Load category error:', error);
        grid.innerHTML = `<div class="empty-state">
            <p>Failed to load content: ${error.message}</p>
            <p style="font-size:12px;color:var(--text-tertiary);margin-top:8px;">Check console for details</p>
        </div>`;
    }
    
    loading.style.display = 'none';
}

async function searchTMDB(query) {
    const grid = document.getElementById('tmdb-grid');
    const loading = document.getElementById('tmdb-loading');
    
    grid.innerHTML = '';
    loading.style.display = 'flex';
    
    try {
        const results = await fetchTMDB('/search/multi', `&query=${encodeURIComponent(query)}`);
        const items = results.results.filter(i => i.poster_path && (i.media_type === 'movie' || i.media_type === 'tv')).slice(0, 40);
        renderItems(items);
    } catch (error) {
        grid.innerHTML = '<div class="empty-state"><p>Search failed. Please try again.</p></div>';
    }
    
    loading.style.display = 'none';
}

async function fetchTMDB(endpoint, params = '') {
    const url = `${TMDB_BASE_URL}${endpoint}?api_key=${TMDB_API_KEY}${params}`;
    console.log('Fetching:', url);
    
    try {
        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        console.log('TMDB Response:', data);
        return data;
    } catch (error) {
        console.error('TMDB Fetch Error:', error);
        throw error;
    }
}

function renderItems(items) {
    const grid = document.getElementById('tmdb-grid');
    
    if (!grid) return;
    
    if (items.length > 0) {
        setWallpaperBackground(items[0]);
    }
    
    grid.innerHTML = items.map(item => {
        const title = item.title || item.name;
        const posterUrl = `${TMDB_IMG_URL}/w185${item.poster_path}`;
        const backdropUrl = `${TMDB_IMG_URL}/w500${item.backdrop_path}`;
        const mediaType = item.media_type || (item.first_air_date ? 'tv' : 'movie');
        const colors = extractColorsFromTitle(title, item.vote_average || 5);
        const typeLabel = mediaType === 'movie' ? 'Movie' : mediaType === 'tv' ? 'Series' : 'Anime';
        
        return `
            <div class="tmdb-card" data-id="${item.id}" data-type="${mediaType}">
                <div class="card-header">
                    <div class="card-title">${title}</div>
                    <span class="card-media-type">${typeLabel}</span>
                </div>
                <div class="card-previews">
                    <div class="card-preview-item">
                        <img src="${backdropUrl}" alt="" loading="lazy" onerror="this.parentElement.style.background='${colors.background}'">
                    </div>
                    <div class="card-preview-item">
                        <img src="${posterUrl}" alt="" loading="lazy" onerror="this.parentElement.style.background='${colors.surface}'">
                    </div>
                    <div class="card-colors">
                        <div class="card-color-dot" style="background:${colors.primary}"></div>
                        <div class="card-color-dot" style="background:${colors.background}"></div>
                        <div class="card-color-dot" style="background:${colors.surface}"></div>
                        <div class="card-color-dot" style="background:${colors.textPrimary}"></div>
                    </div>
                </div>
            </div>
        `;
    }).join('');
    
    grid.querySelectorAll('.tmdb-card').forEach(card => {
        card.addEventListener('click', () => openThemePreview(card.dataset.id, card.dataset.type));
    });
}

function setWallpaperBackground(item) {
    const img = document.getElementById('wallpaper-bg');
    if (!img || !item) return;
    const backdropUrl = `${TMDB_IMG_URL}/original${item.backdrop_path}`;
    if (item.backdrop_path) {
        img.src = backdropUrl;
    }
}

function generateWallpaper(colors, width, height) {
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    
    const grad = ctx.createLinearGradient(0, 0, width, height);
    grad.addColorStop(0, colors.background);
    grad.addColorStop(0.5, colors.surface);
    grad.addColorStop(1, colors.background);
    
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, width, height);
    
    for (let i = 0; i < 5; i++) {
        const cx = Math.random() * width;
        const cy = Math.random() * height;
        const r = 50 + Math.random() * 150;
        const g = ctx.createRadialGradient(cx, cy, 0, cx, cy, r);
        g.addColorStop(0, colors.primary + '15');
        g.addColorStop(1, 'transparent');
        ctx.fillStyle = g;
        ctx.fillRect(0, 0, width, height);
    }
    
    return canvas.toDataURL('image/jpeg', 0.85);
}

function generateCharacterImage(colors, title, size) {
    const canvas = document.createElement('canvas');
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d');
    
    ctx.fillStyle = colors.surface;
    ctx.beginPath();
    ctx.arc(size/2, size/2, size/2, 0, Math.PI * 2);
    ctx.fill();
    
    ctx.strokeStyle = colors.primary + '40';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.arc(size/2, size/2, size/2 - 2, 0, Math.PI * 2);
    ctx.stroke();
    
    const initials = title.split(' ').slice(0, 2).map(w => w[0]).join('').toUpperCase();
    ctx.fillStyle = colors.primary;
    ctx.font = `bold ${size * 0.35}px Inter, sans-serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(initials, size/2, size/2);
    
    return canvas.toDataURL('image/png');
}

function generateSymbol(colors, size) {
    const canvas = document.createElement('canvas');
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d');
    
    ctx.fillStyle = colors.primary;
    ctx.beginPath();
    ctx.arc(size/2, size/2, size * 0.35, 0, Math.PI * 2);
    ctx.fill();
    
    ctx.fillStyle = colors.background;
    ctx.font = `bold ${size * 0.4}px Inter, sans-serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('A', size/2, size/2);
    
    return canvas.toDataURL('image/png');
}

async function urlToDataUrl(url) {
    if (!url) return null;
    try {
        const r = await fetch(url);
        if (!r.ok) throw new Error('HTTP ' + r.status);
        const blob = await r.blob();
        return new Promise(resolve => {
            const reader = new FileReader();
            reader.onload = e => resolve(e.target.result);
            reader.readAsDataURL(blob);
        });
    } catch (e) {
        return null;
    }
}

async function loadThemeImages(details, colors, type) {
    const title = details.title || details.name || 'Theme';
    const posterPaths = details.poster_path ? [`https://image.tmdb.org/t/p/w342${details.poster_path}`] : [];
    const backdropPaths = details.backdrop_path ? [`https://image.tmdb.org/t/p/original${details.backdrop_path}`] : [];
    const logoPaths = [];
    
    try {
        const imagesData = await fetchTMDB(`/${type}/${details.id}/images`, '&include_image_language=en,null');
        
        if (imagesData.logos) {
            imagesData.logos.slice(0, 20).forEach(l => logoPaths.push(`https://image.tmdb.org/t/p/w500${l.file_path}`));
        }
        
        if (imagesData.backdrops) {
            imagesData.backdrops
                .filter(b => b.file_path !== details.backdrop_path)
                .slice(0, 20)
                .forEach(b => backdropPaths.push(`https://image.tmdb.org/t/p/original${b.file_path}`));
        }
        
        if (imagesData.posters) {
            imagesData.posters
                .filter(p => p.file_path !== details.poster_path)
                .slice(0, 20)
                .forEach(p => posterPaths.push(`https://image.tmdb.org/t/p/w342${p.file_path}`));
        }
    } catch (e) {
        console.log('No images available');
    }
    
    const wallpaper = backdropPaths[0] ? await urlToDataUrl(backdropPaths[0]) : generateWallpaper(colors, 1440, 2560);
    const character = posterPaths[0] ? await urlToDataUrl(posterPaths[0]) : generateCharacterImage(colors, title, 512);
    const symbol = logoPaths[0] ? await urlToDataUrl(logoPaths[0]) : posterPaths[0] ? await urlToDataUrl(posterPaths[0]) : generateSymbol(colors, 256);
    
    return {
        wallpaper,
        character,
        symbol,
        backdropUrls: backdropPaths,
        posterUrls: posterPaths,
        logoUrls: logoPaths
    };
}

async function openThemePreview(id, type) {
    const modal = document.getElementById('theme-modal');
    
    modal.classList.add('active');
    document.getElementById('modal-preview-screen').innerHTML = '<div style="display:flex;align-items:center;justify-content:center;height:100%;color:var(--text-tertiary);font-size:13px;">Carregando...</div>';
    document.getElementById('modal-title').textContent = 'Loading...';
    document.getElementById('modal-overview').textContent = '';
    document.getElementById('modal-colors').innerHTML = '';
    
    try {
        const details = await fetchTMDB(`/${type}/${id}`);
        const title = details.title || details.name;
        const posterUrl = details.poster_path ? `${TMDB_IMG_URL}/w185${details.poster_path}` : null;
        const backdropUrl = details.backdrop_path ? `${TMDB_IMG_URL}/original${details.backdrop_path}` : null;
        
        document.getElementById('modal-title').textContent = title;
        document.getElementById('modal-overview').textContent = details.overview ? details.overview.substring(0, 200) + '...' : '';
        
        const colors = extractColorsFromTitle(title, details.vote_average);
        
        const images = await loadThemeImages(details, colors, type);
        
        let extractedColors = await extractColorsFromImage(images.character);
        if (!extractedColors) extractedColors = colors;
        
        currentThemeData = {
            id: `tmdb_${id}`,
            name: title,
            type: type,
            posterUrl: posterUrl,
            backdropUrl: backdropUrl,
            overview: details.overview,
            colors: extractedColors,
            voteAverage: details.vote_average,
            wallpaperData: images.wallpaper,
            characterData: images.character,
            symbolData: images.symbol,
            backdropUrls: images.backdropUrls || [],
            posterUrls: images.posterUrls || [],
            logoUrls: images.logoUrls || [],
            selectedBackdrop: 0,
            selectedPoster: 0,
            selectedLogo: 0
        };
        
        renderColorPreview(extractedColors);
        renderModalPreview(extractedColors, images.character, images.wallpaper, images.symbol);
        
        const pageWallpaper = document.getElementById('wallpaper-bg');
        if (pageWallpaper && images.wallpaper) {
            pageWallpaper.src = images.wallpaper;
            pageWallpaper.classList.add('loaded');
        }
        
        function renderBackdropSelectors() {
            const containers = [
                { id: 'modal-backdrops', urls: currentThemeData.backdropUrls, label: 'Wallpapers', key: 'wallpaperData', selKey: 'selectedBackdrop', updateFn: (val) => {
                    renderModalPreview(extractedColors, currentThemeData.characterData, val, currentThemeData.symbolData);
                    const pw = document.getElementById('wallpaper-bg');
                    if (pw) { pw.src = val; pw.classList.add('loaded'); }
                }},
                { id: 'modal-posters', urls: currentThemeData.posterUrls, label: 'Posters', key: 'characterData', selKey: 'selectedPoster', updateFn: (val) => {
                    currentThemeData.characterData = val;
                    renderModalPreview(extractedColors, val, currentThemeData.wallpaperData, currentThemeData.symbolData);
                }},
                { id: 'modal-logos', urls: currentThemeData.logoUrls, label: 'Logos', key: 'symbolData', selKey: 'selectedLogo', updateFn: (val) => {
                    currentThemeData.symbolData = val;
                    renderModalPreview(extractedColors, currentThemeData.characterData, currentThemeData.wallpaperData, val);
                }}
            ];
            
            containers.forEach(({ id, urls, label, key, selKey, updateFn }) => {
                const container = document.getElementById(id);
                if (urls && urls.length > 1) {
                    container.innerHTML = `
                        <div class="backdrop-selector">
                            <div class="backdrop-selector-label">${label}</div>
                            <div class="backdrop-thumbs">${urls.map((url, i) => `
                                <div class="backdrop-thumb${i === currentThemeData[selKey] ? ' active' : ''}" data-idx="${i}" data-url="${url}">
                                    <img src="${url}" alt="" loading="lazy">
                                </div>
                            `).join('')}</div>
                        </div>
                    `;
                    container.style.display = 'block';
                    
                    container.querySelectorAll('.backdrop-thumb').forEach(el => {
                        el.addEventListener('click', async () => {
                            const idx = parseInt(el.dataset.idx);
                            if (idx === currentThemeData[selKey]) return;
                            currentThemeData[selKey] = idx;
                            const thumbUrl = el.dataset.url;
                            const dataUrl = await urlToDataUrl(thumbUrl);
                            if (!dataUrl) return;
                            currentThemeData[key] = dataUrl;
                            updateFn(dataUrl);
                            container.querySelectorAll('.backdrop-thumb').forEach(t => t.classList.remove('active'));
                            el.classList.add('active');
                        });
                    });
                } else {
                    container.style.display = 'none';
                }
            });
        }
        
        renderBackdropSelectors();
        
    } catch (error) {
        console.error('Error:', error);
        document.getElementById('modal-title').textContent = 'Error loading details';
        document.getElementById('modal-preview-screen').innerHTML = '';
    }
}

function extractColorsFromImage(dataUrl) {
    const canvas = document.createElement('canvas');
    canvas.width = 50;
    canvas.height = 75;
    const ctx = canvas.getContext('2d');
    const img = new Image();
    img.crossOrigin = 'anonymous';
    
    return new Promise(resolve => {
        img.onload = () => {
            ctx.drawImage(img, 0, 0, 50, 75);
            const data = ctx.getImageData(0, 0, 50, 75).data;
            
            let pixels = [];
            for (let i = 0; i < data.length; i += 16) {
                const r = data[i], g = data[i+1], b = data[i+2];
                const brightness = r * 0.299 + g * 0.587 + b * 0.114;
                const saturation = Math.max(r, g, b) - Math.min(r, g, b);
                pixels.push({ r, g, b, brightness, saturation });
            }
            
            pixels.sort((a, b) => a.brightness - b.brightness);
            
            const mid = Math.floor(pixels.length / 2);
            const darkPixels = pixels.slice(0, mid);
            const lightPixels = pixels.slice(mid);
            
            const avg = arr => {
                const r = Math.round(arr.reduce((s, p) => s + p.r, 0) / arr.length);
                const g = Math.round(arr.reduce((s, p) => s + p.g, 0) / arr.length);
                const b = Math.round(arr.reduce((s, p) => s + p.b, 0) / arr.length);
                return `#${r.toString(16).padStart(2,'0')}${g.toString(16).padStart(2,'0')}${b.toString(16).padStart(2,'0')}`;
            };
            
            const bgPixels = darkPixels.slice(0, Math.floor(darkPixels.length / 3));
            const surfacePixels = darkPixels.slice(Math.floor(darkPixels.length / 3));
            
            const sortedBySat = [...pixels].sort((a, b) => b.saturation - a.saturation);
            const vibrantPixels = sortedBySat.slice(0, Math.max(5, Math.floor(pixels.length * 0.1)));
            
            const background = avg(bgPixels);
            const surface = avg(surfacePixels);
            const primary = avg(vibrantPixels);
            const textPrimary = '#FFFFFF';
            const textSecondary = avg(lightPixels.slice(Math.floor(lightPixels.length * 0.5)));
            const glowColor = primary;
            
            resolve({ background, surface, primary, textPrimary, textSecondary, glowColor });
        };
        img.onerror = () => resolve(null);
        img.src = dataUrl;
    });
}

function extractColorsFromTitle(title, rating) {
    const hash = title.split('').reduce((a, c) => ((a << 5) - a) + c.charCodeAt(0), 0);
    const hue = Math.abs(hash % 360);
    const isDark = rating > 5;
    const bg = isDark ? 6 : 240;
    const surf = isDark ? 14 : 220;
    const primary = `hsl(${hue}, ${60 + Math.abs((hash >> 8) % 30)}%, ${isDark ? 60 : 40}%)`;
    return {
        background: `hsl(${hue}, ${20 + Math.abs(hash % 20)}%, ${bg}%)`,
        surface: `hsl(${hue}, ${15 + Math.abs((hash >> 4) % 15)}%, ${surf}%)`,
        primary,
        textPrimary: isDark ? '#FFFFFF' : '#1A1A2E',
        textSecondary: `hsl(${hue}, 20%, ${isDark ? 70 : 40}%)`,
        glowColor: primary
    };
}

function renderColorPreview(colors) {
    const container = document.getElementById('modal-colors');
    container.innerHTML = `
        <div class="color-swatch" style="background:${colors.background}" title="Background"></div>
        <div class="color-swatch" style="background:${colors.surface}" title="Surface"></div>
        <div class="color-swatch" style="background:${colors.primary}" title="Primary"></div>
        <div class="color-swatch" style="background:${colors.textPrimary}" title="Text Primary"></div>
        <div class="color-swatch" style="background:${colors.textSecondary}" title="Text Secondary"></div>
        <div class="color-swatch" style="background:${colors.glowColor}" title="Glow"></div>
    `;
}

function renderModalPreview(colors, characterDataUrl, wallpaperDataUrl, symbolDataUrl) {
    const screen = document.getElementById('modal-preview-screen');
    const { background, surface, primary, textPrimary, textSecondary } = colors;
    
    const now = new Date();
    const h = now.getHours().toString().padStart(2, '0');
    const m = now.getMinutes().toString().padStart(2, '0');
    const dayName = DAYS_PT[now.getDay()];
    const day = now.getDate();
    const month = MONTHS_PT[now.getMonth()];
    
    const hour = now.getHours();
    let weatherIcon = weatherIcons.sunny;
    let temp = '24°';
    if (hour >= 18 || hour < 6) {
        weatherIcon = weatherIcons.night;
        temp = '18°';
    } else if (hour >= 14) {
        weatherIcon = weatherIcons.cloudy;
        temp = '22°';
    }
    
    const symbolHtml = symbolDataUrl
        ? `<img src="${symbolDataUrl}" style="max-width:60px;max-height:20px;object-fit:contain;">`
        : `<div style="width:28px;height:28px;border-radius:8px;background:${primary};display:flex;align-items:center;justify-content:center;font-weight:800;font-size:14px;color:${background};">A</div>`;
    
    const mascotRowHtml = `
        <div style="display:flex;align-items:center;gap:10px;margin-bottom:8px;">
            ${symbolHtml}
            <div style="width:1px;height:20px;background:${primary}20;"></div>
            <div style="display:flex;align-items:center;gap:6px;">
                <div style="width:18px;height:18px;">${weatherIcon}</div>
                <div>
                    <div style="font-size:13px;font-weight:700;color:${textPrimary};line-height:1.2;">${temp}</div>
                    <div style="font-size:8px;color:${textSecondary}60;line-height:1.2;">Ensolarado</div>
                </div>
            </div>
        </div>
    `;
    
    const appPalette = [surface, background, surface, background, surface, background, surface, background];
    const appIcons = [
        `<svg viewBox="0 0 24 24" fill="white"><path d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z"/></svg>`,
        `<svg viewBox="0 0 24 24" fill="white"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/></svg>`,
        `<svg viewBox="0 0 24 24" fill="white"><path d="M12 15.2a3.2 3.2 0 100-6.4 3.2 3.2 0 000 6.4z"/><path d="M9 2L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z"/></svg>`,
        `<svg viewBox="0 0 24 24" fill="white"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>`,
        `<svg viewBox="0 0 24 24" fill="white"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>`,
        `<svg viewBox="0 0 24 24" fill="white"><path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>`,
        `<svg viewBox="0 0 24 24" fill="white"><path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/></svg>`,
        `<svg viewBox="0 0 24 24" fill="white"><path d="M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z"/></svg>`
    ];
    const appNames = ['Tel', 'SMS', 'Câmera', 'Music', 'Chrome', 'Gmail', 'YouTube', 'Drive'];
    
    const appIconsHtml = Array(8).fill(0).map((_, i) => `
        <div style="display:flex;flex-direction:column;align-items:center;gap:3px;">
            <div style="width:36px;height:36px;border-radius:10px;background:${appPalette[i]};border:1px solid ${primary}15;display:flex;align-items:center;justify-content:center;">
                <div style="width:16px;height:16px;">${appIcons[i]}</div>
            </div>
            <span style="font-size:6px;color:${textSecondary}99;">${appNames[i]}</span>
        </div>
    `).join('');
    
    const dockIcons = [
        `<svg viewBox="0 0 24 24" fill="white"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/></svg>`,
        `<svg viewBox="0 0 24 24" fill="white"><path d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z"/></svg>`,
        `<svg viewBox="0 0 24 24" fill="white"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/></svg>`,
        `<svg viewBox="0 0 24 24" fill="white"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>`
    ];
    
    const wallpaperBg = wallpaperDataUrl 
        ? `url(${wallpaperDataUrl}) center/cover no-repeat`
        : `linear-gradient(135deg, ${background}, ${surface})`;
    
    screen.innerHTML = `
        <div style="position:absolute;inset:0;background:${wallpaperBg};"></div>
        <div style="position:absolute;inset:0;background:linear-gradient(180deg, rgba(0,0,0,0.3) 0%, rgba(0,0,0,0.5) 100%);"></div>
        
        <div style="position:relative;display:flex;flex-direction:column;height:100%;padding:0;">
            <div style="display:flex;flex-direction:column;align-items:center;padding:14px 14px 0;">
                <div style="background:${surface};border:1px solid ${primary}15;border-radius:16px;padding:14px 20px;display:flex;flex-direction:column;align-items:center;">
                    ${mascotRowHtml}
                    <div id="preview-clock" style="font-size:34px;font-weight:800;color:${textPrimary};letter-spacing:-1px;line-height:1;">${h}:${m}</div>
                    <div id="preview-date" style="font-size:8px;color:${textSecondary}99;text-transform:uppercase;letter-spacing:1px;margin-top:3px;">${dayName}, ${day} de ${month}</div>
                </div>
            </div>
            
            <div style="flex:1;display:grid;grid-template-columns:repeat(4,1fr);align-content:start;gap:6px;padding:12px 10px;">
                ${appIconsHtml}
            </div>
            
            <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:4px;padding:14px 14px 18px;">
                ${dockIcons.map(icon => `
                    <div style="display:flex;flex-direction:column;align-items:center;">
                            <div style="width:40px;height:40px;border-radius:12px;background:${surface};border:1px solid ${primary}20;display:flex;align-items:center;justify-content:center;">
                            <div style="width:18px;height:18px;opacity:0.8;">${icon}</div>
                        </div>
                    </div>
                `).join('')}
            </div>
        </div>
    `;
}

function setupModalActions() {
    document.getElementById('btn-download-theme').addEventListener('click', () => {
        if (currentThemeData) downloadTheme(currentThemeData);
    });
    
    document.getElementById('btn-customize-theme').addEventListener('click', () => {
        if (currentThemeData) {
            const preset = {
                name: currentThemeData.name,
                type: currentThemeData.type,
                colors: currentThemeData.colors,
                wallpaperData: currentThemeData.wallpaperData,
                characterData: currentThemeData.characterData,
                symbolData: currentThemeData.symbolData,
                backdropUrls: currentThemeData.backdropUrls || [],
                posterUrls: currentThemeData.posterUrls || [],
                logoUrls: currentThemeData.logoUrls || []
            };
            try {
                localStorage.setItem('tmdb_preset', JSON.stringify(preset));
            } catch (e) {
                const minimal = { ...preset, backdropUrls: [], posterUrls: [], logoUrls: [] };
                localStorage.setItem('tmdb_preset', JSON.stringify(minimal));
            }
            window.location.href = 'create.html?preset=tmdb';
        }
    });
    
    document.querySelectorAll('.modal-close').forEach(btn => {
        btn.addEventListener('click', () => btn.closest('.modal').classList.remove('active'));
    });
    
    document.querySelectorAll('.modal-overlay').forEach(overlay => {
        overlay.addEventListener('click', () => overlay.closest('.modal').classList.remove('active'));
    });
}

async function downloadTheme(themeData) {
    const btn = document.getElementById('btn-download-theme');
    const originalText = btn.innerHTML;
    btn.innerHTML = 'Creating ZIP...';
    btn.disabled = true;
    
    const zip = new JSZip();
    const themeName = themeData.name.toLowerCase().replace(/[^a-z0-9]/g, '-').substring(0, 30);
    
    const themeJson = {
        name: themeData.name,
        id: themeData.id,
        author: 'Theme Store',
        description: `Theme inspired by ${themeData.name}`,
        wallpaperPath: 'wallpaper.jpg',
        cardXmlPath: 'texture.xml',
        cardTexture: 'grid',
        characterPath: 'character.png',
        symbolPath: 'symbol.png',
        clockStyle: 'character_card',
        useCardTexture: true,
        colors: themeData.colors,
        shapes: {
            cardCornerRadius: 16,
            searchBarCornerRadius: 28,
            showBorders: true
        },
        layout: {
            columns: 4,
            showAppName: true
        }
    };
    
    zip.file('theme.json', JSON.stringify(themeJson, null, 2));
    
    const primaryColor = themeData.colors.primary || '#FFFFFF';
    const bgColor = themeData.colors.background || '#06060A';
    
    function addDataUrlToZip(dataUrl, filename, ext) {
        if (!dataUrl) return;
        const parts = dataUrl.split(',');
        if (parts.length === 2) {
            zip.file(filename + ext, parts[1], { base64: true });
        }
    }
    
    function getExt(dataUrl, fallback) {
        if (!dataUrl) return fallback;
        if (dataUrl.includes('image/jpeg') || dataUrl.includes('image/jpg')) return '.jpg';
        if (dataUrl.includes('image/png')) return '.png';
        if (dataUrl.includes('image/webp')) return '.webp';
        return fallback;
    }
    
    const wallpaperExt = getExt(themeData.wallpaperData, '.jpg');
    const characterExt = getExt(themeData.characterData, '.png');
    const symbolExt = getExt(themeData.symbolData, '.png');
    themeJson.wallpaperPath = 'wallpaper' + wallpaperExt;
    themeJson.characterPath = 'character' + characterExt;
    themeJson.symbolPath = 'symbol' + symbolExt;
    
    addDataUrlToZip(themeData.wallpaperData, 'wallpaper', wallpaperExt);
    addDataUrlToZip(themeData.characterData, 'character', characterExt);
    addDataUrlToZip(themeData.symbolData, 'symbol', symbolExt);
    
    if (themeData.symbolData) {
        themeJson.cardXmlPath = null;
        themeJson.cardTexture = null;
        themeJson.texturePath = themeJson.symbolPath;
    } else {
        const bgHex = (themeData.colors.background || '#06060A').replace('#', '');
        const primHex = (themeData.colors.primary || '#FFFFFF').replace('#', '');
        const textureXml = `<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#CC${bgHex}" />
    <stroke android:width="1dp" android:color="#44${primHex}" />
    <corners android:radius="16dp" />
</shape>`;
        zip.file('texture.xml', textureXml);
    }
    zip.file('theme.json', JSON.stringify(themeJson, null, 2));
    
    const content = await zip.generateAsync({ type: 'blob' });
    const url = URL.createObjectURL(content);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${themeName}-theme.zip`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    
    btn.innerHTML = originalText;
    btn.disabled = false;
}

document.addEventListener('DOMContentLoaded', init);
