/* ═══════════════════════════════════════════════════════════════ */
/* Alauncher Theme Creator - JavaScript                            */
/* ═══════════════════════════════════════════════════════════════ */

let currentTheme = {
    name: 'My Theme',
    author: 'Anonymous',
    description: 'A beautiful custom theme.',
    category: 'dark',
    clockStyle: 'symbol_horizontal',
    colors: {
        background: '#06060A',
        surface: '#0E0E14',
        primary: '#FFFFFF',
        textPrimary: '#FFFFFF',
        textSecondary: '#A0A0B0',
        glowColor: '#FFFFFF'
    },
    layout: {
        columns: 4,
        cardCornerRadius: 16,
        showBorders: true,
        showAppName: true
    },
    wallpaper: null,
    character: null,
    symbol: null
};

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

const presets = {
    midnight: {
        background: '#0D1117',
        surface: '#161B22',
        primary: '#58A6FF',
        textPrimary: '#FFFFFF',
        textSecondary: '#8B949E',
        glowColor: '#58A6FF'
    },
    ocean: {
        background: '#001220',
        surface: '#001830',
        primary: '#00BCD4',
        textPrimary: '#FFFFFF',
        textSecondary: '#80DEEA',
        glowColor: '#00BCD4'
    },
    forest: {
        background: '#0A1A0A',
        surface: '#0F2A0F',
        primary: '#66BB6A',
        textPrimary: '#FFFFFF',
        textSecondary: '#A5D6A7',
        glowColor: '#66BB6A'
    },
    sunset: {
        background: '#1A0A0A',
        surface: '#2A1010',
        primary: '#FF7043',
        textPrimary: '#FFFFFF',
        textSecondary: '#FFAB91',
        glowColor: '#FF7043'
    },
    lavender: {
        background: '#1A0A2A',
        surface: '#2A1A3A',
        primary: '#CE93D8',
        textPrimary: '#FFFFFF',
        textSecondary: '#E1BEE7',
        glowColor: '#CE93D8'
    },
    snow: {
        background: '#F5F5F5',
        surface: '#FFFFFF',
        primary: '#424242',
        textPrimary: '#212121',
        textSecondary: '#757575',
        glowColor: '#000000'
    }
};

// Portuguese day/month names
const DAYS_PT = ['Domingo', 'Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado'];
const MONTHS_PT = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'];

// Weather SVG icons
const weatherIcons = {
    sunny: `<svg viewBox="0 0 64 64" fill="none">
        <circle cx="32" cy="32" r="14" fill="#FFD93D" opacity="0.9"/>
        <g stroke="#FFD93D" stroke-width="2.5" stroke-linecap="round">
            <line x1="32" y1="4" x2="32" y2="12"/>
            <line x1="32" y1="52" x2="32" y2="60"/>
            <line x1="4" y1="32" x2="12" y2="32"/>
            <line x1="52" y1="32" x2="60" y2="32"/>
            <line x1="12.2" y1="12.2" x2="17.9" y2="17.9"/>
            <line x1="46.1" y1="46.1" x2="51.8" y2="51.8"/>
            <line x1="12.2" y1="51.8" x2="17.9" y2="46.1"/>
            <line x1="46.1" y1="17.9" x2="51.8" y2="12.2"/>
        </g>
    </svg>`,
    cloudy: `<svg viewBox="0 0 64 64" fill="none">
        <circle cx="24" cy="24" r="10" fill="#FFD93D" opacity="0.7"/>
        <g stroke="#FFD93D" stroke-width="2" stroke-linecap="round" opacity="0.5">
            <line x1="24" y1="8" x2="24" y2="12"/>
            <line x1="10" y1="24" x2="14" y2="24"/>
            <line x1="12" y1="12" x2="15" y2="15"/>
            <line x1="36" y1="12" x2="33" y2="15"/>
        </g>
        <path d="M20 44C14 44 10 40 10 35C10 30 14 26 20 26C20 20 26 16 32 16C38 16 44 20 44 26C50 26 54 30 54 35C54 40 50 44 44 44H20Z" fill="white" opacity="0.85"/>
        <path d="M20 44C14 44 10 40 10 35C10 30 14 26 20 26C20 20 26 16 32 16C38 16 44 20 44 26C50 26 54 30 54 35C54 40 50 44 44 44H20Z" stroke="white" stroke-width="1" opacity="0.3"/>
    </svg>`,
    rainy: `<svg viewBox="0 0 64 64" fill="none">
        <path d="M16 36C10 36 6 32 6 27C6 22 10 18 16 18C16 12 22 8 28 8C34 8 40 12 40 18C46 18 50 22 50 27C50 32 46 36 40 36H16Z" fill="white" opacity="0.8"/>
        <g stroke="#5DADE2" stroke-width="2" stroke-linecap="round">
            <line x1="18" y1="42" x2="16" y2="50"/>
            <line x1="26" y1="42" x2="24" y2="52"/>
            <line x1="34" y1="42" x2="32" y2="50"/>
            <line x1="22" y1="48" x2="20" y2="56"/>
            <line x1="30" y1="48" x2="28" y2="56"/>
        </g>
    </svg>`,
    stormy: `<svg viewBox="0 0 64 64" fill="none">
        <path d="M16 32C10 32 6 28 6 23C6 18 10 14 16 14C16 8 22 4 28 4C34 4 40 8 40 14C46 14 50 18 50 23C50 28 46 32 40 32H16Z" fill="#8B949E" opacity="0.8"/>
        <polygon points="30,34 24,48 30,48 26,58 38,44 32,44 36,34" fill="#FFD93D"/>
        <g stroke="#5DADE2" stroke-width="1.5" stroke-linecap="round" opacity="0.6">
            <line x1="14" y1="38" x2="12" y2="44"/>
            <line x1="42" y1="38" x2="44" y2="44"/>
            <line x1="18" y1="44" x2="16" y2="50"/>
        </g>
    </svg>`,
    snowy: `<svg viewBox="0 0 64 64" fill="none">
        <path d="M16 34C10 34 6 30 6 25C6 20 10 16 16 16C16 10 22 6 28 6C34 6 40 10 40 16C46 16 50 20 50 25C50 30 46 34 40 34H16Z" fill="white" opacity="0.85"/>
        <g fill="#AED6F1">
            <circle cx="18" cy="42" r="2.5"/>
            <circle cx="28" cy="44" r="2.5"/>
            <circle cx="38" cy="42" r="2.5"/>
            <circle cx="23" cy="50" r="2"/>
            <circle cx="33" cy="52" r="2"/>
            <circle cx="14" cy="50" r="1.5"/>
            <circle cx="42" cy="50" r="1.5"/>
        </g>
    </svg>`,
    night: `<svg viewBox="0 0 64 64" fill="none">
        <path d="M38 12C30 12 24 18 24 26C24 34 30 40 38 40C42 40 46 38 48 35C44 37 39 36 36 32C33 28 33 22 36 18C38 15 40 13 38 12Z" fill="#F4D03F" opacity="0.9"/>
        <g fill="#F4D03F" opacity="0.4">
            <circle cx="16" cy="16" r="1.5"/>
            <circle cx="52" cy="20" r="1"/>
            <circle cx="48" cy="10" r="1.5"/>
            <circle cx="10" cy="30" r="1"/>
            <circle cx="54" cy="34" r="1.5"/>
            <circle cx="20" cy="48" r="1"/>
            <circle cx="46" cy="48" r="1"/>
        </g>
    </svg>`
};

function init() {
    setupColorInputs();
    setupRangeInputs();
    setupFileUploads();
    setupPresets();
    setupActions();
    loadTMDBPreset();
    updatePreview();
    setInterval(updateClock, 1000);
}

function updateClock() {
    const clockEl = document.getElementById('preview-clock');
    const dateEl = document.getElementById('preview-date');
    if (!clockEl || !dateEl) return;
    
    const now = new Date();
    const h = now.getHours().toString().padStart(2, '0');
    const m = now.getMinutes().toString().padStart(2, '0');
    const dayName = DAYS_PT[now.getDay()];
    const day = now.getDate();
    const month = MONTHS_PT[now.getMonth()];
    
    clockEl.textContent = `${h}:${m}`;
    dateEl.textContent = `${dayName}, ${day} de ${month}`;
}

function setupColorInputs() {
    const colorKeys = ['background', 'surface', 'primary', 'text-primary', 'text-secondary', 'glow'];
    
    colorKeys.forEach(key => {
        const colorInput = document.getElementById(`color-${key}`);
        const textInput = document.getElementById(`color-${key}-text`);
        
        if (colorInput && textInput) {
            colorInput.addEventListener('input', (e) => {
                const value = e.target.value;
                textInput.value = value.toUpperCase();
                updateThemeColor(key, value);
            });
            
            textInput.addEventListener('input', (e) => {
                let value = e.target.value;
                if (value.match(/^#[0-9A-Fa-f]{6}$/)) {
                    colorInput.value = value;
                    updateThemeColor(key, value);
                }
            });
            
            textInput.addEventListener('blur', (e) => {
                let value = e.target.value;
                if (!value.startsWith('#')) value = '#' + value;
                if (value.match(/^#[0-9A-Fa-f]{6}$/)) {
                    colorInput.value = value;
                    textInput.value = value.toUpperCase();
                    updateThemeColor(key, value);
                }
            });
        }
    });
}

function updateThemeColor(key, value) {
    const colorKey = key.replace(/-([a-z])/g, (g) => g[1].toUpperCase());
    currentTheme.colors[colorKey] = value;
    updatePreview();
}

function setupRangeInputs() {
    const columnsInput = document.getElementById('layout-columns');
    const cornerRadiusInput = document.getElementById('layout-cornerRadius');
    
    columnsInput.addEventListener('input', (e) => {
        document.getElementById('columns-value').textContent = e.target.value;
        currentTheme.layout.columns = parseInt(e.target.value);
        updatePreview();
    });
    
    cornerRadiusInput.addEventListener('input', (e) => {
        document.getElementById('cornerRadius-value').textContent = e.target.value;
        currentTheme.layout.cardCornerRadius = parseInt(e.target.value);
        updatePreview();
    });
    
    document.getElementById('layout-showBorders').addEventListener('change', (e) => {
        currentTheme.layout.showBorders = e.target.checked;
        updatePreview();
    });
    
    document.getElementById('layout-showAppName').addEventListener('change', (e) => {
        currentTheme.layout.showAppName = e.target.checked;
        updatePreview();
    });
}

function setupFileUploads() {
    setupFileUpload('wallpaper', 'wallpaper');
    setupFileUpload('character', 'character');
    setupFileUpload('symbol', 'symbol');
}

function setupFileUpload(type, key) {
    const upload = document.getElementById(`${type}-upload`);
    const input = document.getElementById(`${type}-input`);
    const preview = document.getElementById(`${type}-preview`);
    const previewImg = document.getElementById(`${type}-preview-img`);
    const removeBtn = document.getElementById(`${type}-remove`);
    
    upload.addEventListener('click', () => input.click());
    
    input.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = (e) => {
                currentTheme[key] = e.target.result;
                previewImg.src = e.target.result;
                preview.style.display = 'block';
                upload.style.display = 'none';
                updatePreview();
            };
            reader.readAsDataURL(file);
        }
    });
    
    removeBtn.addEventListener('click', () => {
        currentTheme[key] = null;
        preview.style.display = 'none';
        upload.style.display = 'flex';
        input.value = '';
        updatePreview();
    });
}

function setupPresets() {
    document.querySelectorAll('.preset-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const preset = presets[btn.dataset.preset];
            if (preset) {
                currentTheme.colors = { ...preset };
                updateColorInputs();
                updatePreview();
                showToast('Preset applied!');
            }
        });
    });
}

function updateColorInputs() {
    const colorMap = {
        'background': 'background',
        'surface': 'surface',
        'primary': 'primary',
        'text-primary': 'textPrimary',
        'text-secondary': 'textSecondary',
        'glow': 'glowColor'
    };
    
    Object.entries(colorMap).forEach(([inputKey, themeKey]) => {
        const colorInput = document.getElementById(`color-${inputKey}`);
        const textInput = document.getElementById(`color-${inputKey}-text`);
        if (colorInput && textInput) {
            colorInput.value = currentTheme.colors[themeKey];
            textInput.value = currentTheme.colors[themeKey].toUpperCase();
        }
    });
}

function setupActions() {
    document.getElementById('btn-random-colors').addEventListener('click', () => {
        currentTheme.colors = {
            background: randomDarkColor(),
            surface: randomDarkColor(),
            primary: randomBrightColor(),
            textPrimary: '#FFFFFF',
            textSecondary: randomBrightColor(),
            glowColor: randomBrightColor()
        };
        updateColorInputs();
        updatePreview();
        showToast('Random colors generated!');
    });
    
    document.getElementById('btn-publish').addEventListener('click', () => {
        const modal = document.getElementById('publish-modal');
        document.getElementById('publish-name').textContent = currentTheme.name;
        document.getElementById('publish-author').textContent = currentTheme.author;
        document.getElementById('publish-category').textContent = currentTheme.category;
        modal.classList.add('active');
    });
    
    document.getElementById('btn-confirm-publish').addEventListener('click', () => publishTheme());
    document.getElementById('btn-cancel-publish').addEventListener('click', () => {
        document.getElementById('publish-modal').classList.remove('active');
    });
    
    document.getElementById('btn-download').addEventListener('click', () => exportTheme());
    
    document.querySelectorAll('.modal-close').forEach(btn => {
        btn.addEventListener('click', () => btn.closest('.modal').classList.remove('active'));
    });
    
    document.querySelectorAll('.modal-overlay').forEach(overlay => {
        overlay.addEventListener('click', () => overlay.closest('.modal').classList.remove('active'));
    });
    
    document.getElementById('btn-create-another').addEventListener('click', () => {
        document.getElementById('success-modal').classList.remove('active');
        resetForm();
    });
    
    document.getElementById('theme-name').addEventListener('input', (e) => currentTheme.name = e.target.value);
    document.getElementById('theme-author').addEventListener('input', (e) => currentTheme.author = e.target.value);
    document.getElementById('theme-description').addEventListener('input', (e) => currentTheme.description = e.target.value);
    document.getElementById('theme-category').addEventListener('change', (e) => currentTheme.category = e.target.value);
    document.getElementById('clock-style').addEventListener('change', (e) => {
        currentTheme.clockStyle = e.target.value;
        updatePreview();
    });
    
    document.querySelectorAll('.preview-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            document.querySelectorAll('.preview-tab').forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            updatePreview();
        });
    });
}

function updatePreview() {
    const screen = document.getElementById('preview-screen');
    const view = document.querySelector('.preview-tab.active').dataset.view;
    const { colors, layout } = currentTheme;
    
    const r = Math.min(layout.cardCornerRadius, 12) + 'px';
    const border = layout.showBorders ? `1px solid ${colors.primary}20` : 'none';
    const wallpaperBg = currentTheme.wallpaper 
        ? `url(${currentTheme.wallpaper}) center/cover no-repeat`
        : `linear-gradient(135deg, ${colors.background}, ${colors.surface})`;
    
    const now = new Date();
    const h = now.getHours().toString().padStart(2, '0');
    const m = now.getMinutes().toString().padStart(2, '0');
    const dayName = DAYS_PT[now.getDay()];
    const day = now.getDate();
    const month = MONTHS_PT[now.getMonth()];
    
    // Weather icon based on time
    const hour = now.getHours();
    let weatherIcon = weatherIcons.sunny;
    let temp = '24°';
    let condition = 'Ensolarado';
    
    if (hour >= 18 || hour < 6) {
        weatherIcon = weatherIcons.night;
        temp = '18°';
        condition = 'Noite clara';
    } else if (hour >= 14) {
        weatherIcon = weatherIcons.cloudy;
        temp = '22°';
        condition = 'Parcialmente nublado';
    }
    
    if (view === 'home') {
        const symbolHtml = currentTheme.symbol 
            ? `<img src="${currentTheme.symbol}" style="max-width:60px;max-height:20px;object-fit:contain;">`
            : `<div style="width:28px;height:28px;border-radius:8px;background:${colors.primary};display:flex;align-items:center;justify-content:center;font-weight:800;font-size:14px;color:${colors.background};">A</div>`;
        
        const mascotRowHtml = `
            <div style="display:flex;align-items:center;gap:10px;margin-bottom:8px;">
                ${symbolHtml}
                <div style="width:1px;height:20px;background:${colors.primary}20;"></div>
                <div style="display:flex;align-items:center;gap:6px;">
                    <div style="width:18px;height:18px;">${weatherIcon}</div>
                    <div>
                        <div style="font-size:13px;font-weight:700;color:${colors.textPrimary};line-height:1.2;">${temp}</div>
                        <div style="font-size:8px;color:${colors.textSecondary}60;line-height:1.2;">${condition}</div>
                    </div>
                </div>
            </div>
        `;
        
        const appPalette = [colors.surface, colors.background, colors.surface, colors.background, colors.surface, colors.background, colors.surface, colors.background];
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
                <div style="width:36px;height:36px;border-radius:10px;background:${appPalette[i]};border:1px solid ${colors.primary}15;display:flex;align-items:center;justify-content:center;">
                    <div style="width:16px;height:16px;">${appIcons[i]}</div>
                </div>
                <span style="font-size:6px;color:${colors.textSecondary}99;">${appNames[i]}</span>
            </div>
        `).join('');
        
        const dockIcons = [
            `<svg viewBox="0 0 24 24" fill="white"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/></svg>`,
            `<svg viewBox="0 0 24 24" fill="white"><path d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z"/></svg>`,
            `<svg viewBox="0 0 24 24" fill="white"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/></svg>`,
            `<svg viewBox="0 0 24 24" fill="white"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>`
        ];
        
        const wallpaperBg = currentTheme.wallpaper 
            ? `url(${currentTheme.wallpaper}) center/cover no-repeat`
            : `linear-gradient(135deg, ${colors.background}, ${colors.surface})`;
        
        screen.innerHTML = `
            <div style="position:absolute;inset:0;background:${wallpaperBg};"></div>
            <div style="position:absolute;inset:0;background:linear-gradient(180deg, rgba(0,0,0,0.3) 0%, rgba(0,0,0,0.5) 100%);"></div>
            
            <div style="position:relative;display:flex;flex-direction:column;height:100%;padding:0;">
                <div style="display:flex;flex-direction:column;align-items:center;padding:14px 14px 0;">
                    <div style="background:${colors.surface};border:1px solid ${colors.primary}15;border-radius:16px;padding:14px 20px;display:flex;flex-direction:column;align-items:center;">
                        ${mascotRowHtml}
                        <div id="preview-clock" style="font-size:34px;font-weight:800;color:${colors.textPrimary};letter-spacing:-1px;line-height:1;">${h}:${m}</div>
                        <div id="preview-date" style="font-size:8px;color:${colors.textSecondary}99;text-transform:uppercase;letter-spacing:1px;margin-top:3px;">${dayName}, ${day} de ${month}</div>
                    </div>
                </div>
                
                <div style="flex:1;display:grid;grid-template-columns:repeat(4,1fr);align-content:start;gap:6px;padding:12px 10px;">
                    ${appIconsHtml}
                </div>
                
                <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:4px;padding:14px 14px 18px;">
                    ${dockIcons.map(icon => `
                        <div style="display:flex;flex-direction:column;align-items:center;">
                            <div style="width:40px;height:40px;border-radius:12px;background:${colors.surface};border:1px solid ${colors.primary}20;display:flex;align-items:center;justify-content:center;">
                                <div style="width:18px;height:18px;opacity:0.8;">${icon}</div>
                            </div>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    } else {
        const drawerAppColors = [
            colors.surface, colors.background, colors.surface, colors.background,
            colors.surface, colors.background, colors.surface, colors.background,
            colors.surface, colors.background, colors.surface, colors.background,
            colors.surface, colors.background, colors.surface, colors.background
        ];
        
        const drawerIcons = [
            `<svg viewBox="0 0 24 24" fill="white"><path d="M4 8h4V4H4v4zm6 12h4v-4h-4v4zm-6 0h4v-4H4v4zm0-6h4v-4H4v4zm6 0h4v-4h-4v4zm6-10v4h4V4h-4zm-6 4h4V4h-4v4zm6 6h4v-4h-4v4zm0 6h4v-4h-4v4z"/></svg>`,
            `<svg viewBox="0 0 24 24" fill="white"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z"/></svg>`,
            `<svg viewBox="0 0 24 24" fill="white"><path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>`,
            `<svg viewBox="0 0 24 24" fill="white"><path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/></svg>`,
            `<svg viewBox="0 0 24 24" fill="white"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>`,
            `<svg viewBox="0 0 24 24" fill="white"><path d="M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z"/></svg>`,
            `<svg viewBox="0 0 24 24" fill="white"><path d="M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5 2.1h2.21c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91 3.46 4.7 4.13 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1h-2.2c.12 2.19 1.76 3.42 3.68 3.83V21h3v-2.15c1.95-.37 3.5-1.5 3.5-3.55 0-2.84-2.43-3.81-4.7-4.4z"/></svg>`,
            `<svg viewBox="0 0 24 24" fill="white"><path d="M7 18c-1.1 0-1.99.9-1.99 2S5.9 22 7 22s2-.9 2-2-.9-2-2-2zM1 2v2h2l3.6 7.59-1.35 2.45c-.16.28-.25.61-.25.96 0 1.1.9 2 2 2h12v-2H7.42c-.14 0-.25-.11-.25-.25l.03-.12.9-1.63h7.45c.75 0 1.41-.41 1.75-1.03l3.58-6.49c.08-.14.12-.31.12-.48 0-.55-.45-1-1-1H5.21l-.94-2H1zm16 16c-1.1 0-1.99.9-1.99 2s.89 2 1.99 2 2-.9 2-2-.9-2-2-2z"/></svg>`,
            `<svg viewBox="0 0 24 24" fill="white"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>`
        ];
        
        const drawerNames = ['Relógio', 'Chrome', 'Gmail', 'WhatsApp', 'YouTube', 'Drive', 'Maps', 'Play', 'Fotos'];
        const iconSize = 36;
        
        screen.innerHTML = `
            <div style="position:absolute;inset:0;background:${colors.surface}E6;"></div>
            
            <div style="position:relative;display:flex;flex-direction:column;height:100%;padding:0;">
                <div style="display:flex;justify-content:center;padding:8px 0 4px;">
                    <div style="width:40px;height:3px;background:${colors.textSecondary}40;border-radius:2px;"></div>
                </div>
                
                <div style="display:flex;align-items:center;padding:7px 10px;margin:0 10px 8px;background:${colors.background};border-radius:16px;border:1px solid ${colors.primary}15;">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="${colors.textSecondary}" stroke-width="2" style="margin-right:7px;flex-shrink:0;">
                        <circle cx="11" cy="11" r="8"/>
                        <path d="m21 21-4.35-4.35"/>
                    </svg>
                    <div style="font-size:10px;color:${colors.textSecondary};">Biblioteca de Apps...</div>
                </div>
                
                <div style="flex:1;display:grid;grid-template-columns:repeat(3,1fr);gap:10px;padding:0 10px;overflow:hidden;align-content:start;">
                    ${Array(9).fill(0).map((_, i) => `
                        <div style="display:flex;flex-direction:column;align-items:center;gap:3px;">
                            <div style="width:${iconSize}px;height:${iconSize}px;border-radius:${r};background:${drawerAppColors[i]};border:1px solid ${colors.primary}15;display:flex;align-items:center;justify-content:center;">
                                <div style="width:18px;height:18px;">${drawerIcons[i]}</div>
                            </div>
                            ${layout.showAppName ? `<span style="font-size:6px;color:${colors.textSecondary};max-width:${iconSize}px;text-align:center;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${drawerNames[i]}</span>` : ''}
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    }
    
    screen.style.background = wallpaperBg;
    screen.style.color = colors.textPrimary;
    screen.style.position = 'relative';
}

function randomDarkColor() {
    const r = Math.floor(Math.random() * 30);
    const g = Math.floor(Math.random() * 30);
    const b = Math.floor(Math.random() * 40);
    return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;
}

function randomBrightColor() {
    const colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7', '#DDA0DD', '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E9'];
    return colors[Math.floor(Math.random() * colors.length)];
}

function publishTheme() {
    const theme = {
        ...currentTheme,
        id: 'theme_' + Date.now(),
        downloads: 0,
        createdAt: new Date().toISOString().split('T')[0]
    };
    
    const themes = JSON.parse(localStorage.getItem('alauncher_themes') || '[]');
    themes.push(theme);
    localStorage.setItem('alauncher_themes', JSON.stringify(themes));
    
    document.getElementById('publish-modal').classList.remove('active');
    document.getElementById('success-modal').classList.add('active');
}

function getImageExtension(dataUrl) {
    if (!dataUrl) return '';
    if (dataUrl.includes('image/jpeg') || dataUrl.includes('image/jpg')) return '.jpg';
    if (dataUrl.includes('image/png')) return '.png';
    if (dataUrl.includes('image/webp')) return '.webp';
    return '.jpg';
}

async function exportTheme() {
    const zip = new JSZip();
    const themeName = currentTheme.name.toLowerCase().replace(/\s+/g, '-');
    const themeId = 'custom_' + Date.now();
    
    const wallpaperExt = getImageExtension(currentTheme.wallpaper);
    const characterExt = getImageExtension(currentTheme.character);
    const symbolExt = getImageExtension(currentTheme.symbol);
    
    const themeData = {
        name: currentTheme.name,
        id: themeId,
        author: currentTheme.author,
        description: currentTheme.description,
        wallpaperPath: currentTheme.wallpaper ? 'wallpaper' + wallpaperExt : '',
        cardXmlPath: 'texture.xml',
        cardTexture: 'grid',
        characterPath: currentTheme.character ? 'character' + characterExt : '',
        symbolPath: currentTheme.symbol ? 'symbol' + symbolExt : '',
        clockStyle: currentTheme.clockStyle || 'symbol_horizontal',
        useCardTexture: true,
        colors: currentTheme.colors,
        shapes: {
            cardCornerRadius: currentTheme.layout.cardCornerRadius,
            searchBarCornerRadius: 28,
            showBorders: currentTheme.layout.showBorders
        },
        layout: {
            columns: currentTheme.layout.columns,
            showAppName: currentTheme.layout.showAppName
        }
    };
    
    zip.file('theme.json', JSON.stringify(themeData, null, 2));
    
    if (currentTheme.wallpaper) {
        zip.file('wallpaper' + wallpaperExt, currentTheme.wallpaper.split(',')[1], { base64: true });
    }
    if (currentTheme.character) {
        zip.file('character' + characterExt, currentTheme.character.split(',')[1], { base64: true });
    }
    if (currentTheme.symbol) {
        zip.file('symbol' + symbolExt, currentTheme.symbol.split(',')[1], { base64: true });
    }
    
    if (currentTheme.symbol) {
        themeData.cardXmlPath = null;
        themeData.cardTexture = null;
        themeData.texturePath = themeData.symbolPath;
        zip.file('theme.json', JSON.stringify(themeData, null, 2));
    } else {
        const bg = (currentTheme.colors.background || '#06060A').replace('#', '');
        const prim = (currentTheme.colors.primary || '#FFFFFF').replace('#', '');
        const textureXml = `<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#CC${bg}" />
    <stroke android:width="1dp" android:color="#44${prim}" />
    <corners android:radius="16dp" />
</shape>`;
        zip.file('texture.xml', textureXml);
    }
    
    const content = await zip.generateAsync({ type: 'blob' });
    const url = URL.createObjectURL(content);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${themeName}-theme.zip`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    
    showToast('Theme ZIP downloaded!');
}

function resetForm() {
    currentTheme = {
        name: 'My Theme',
        author: 'Anonymous',
        description: 'A beautiful custom theme.',
        category: 'dark',
        clockStyle: 'symbol_horizontal',
        colors: {
            background: '#06060A',
            surface: '#0E0E14',
            primary: '#FFFFFF',
            textPrimary: '#FFFFFF',
            textSecondary: '#A0A0B0',
            glowColor: '#FFFFFF'
        },
        layout: {
            columns: 4,
            cardCornerRadius: 16,
            showBorders: true,
            showAppName: true
        },
        wallpaper: null,
        character: null,
        symbol: null
    };
    
    document.getElementById('theme-name').value = currentTheme.name;
    document.getElementById('theme-author').value = currentTheme.author;
    document.getElementById('theme-description').value = currentTheme.description;
    document.getElementById('theme-category').value = currentTheme.category;
    document.getElementById('layout-columns').value = 4;
    document.getElementById('columns-value').textContent = '4';
    document.getElementById('layout-cornerRadius').value = 16;
    document.getElementById('cornerRadius-value').textContent = '16';
    document.getElementById('layout-showBorders').checked = true;
    document.getElementById('layout-showAppName').checked = true;
    document.getElementById('clock-style').value = 'symbol_horizontal';
    
    updateColorInputs();
    
    ['wallpaper', 'character', 'symbol'].forEach(type => {
        document.getElementById(`${type}-preview`).style.display = 'none';
        document.getElementById(`${type}-upload`).style.display = 'flex';
        document.getElementById(`${type}-input`).value = '';
    });
    
    updatePreview();
}

function showToast(message) {
    const toast = document.createElement('div');
    toast.style.cssText = `
        position:fixed;bottom:24px;left:50%;transform:translateX(-50%);
        background:#1A1A24;color:#fff;padding:12px 24px;border-radius:12px;
        border:1px solid rgba(255,255,255,0.1);font-size:14px;z-index:2000;
        animation:slideUp 0.3s ease;
    `;
    toast.textContent = message;
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(-50%) translateY(20px)';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

function loadTMDBPreset() {
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('preset') === 'tmdb') {
        const presetData = localStorage.getItem('tmdb_preset');
        
        if (presetData) {
            try {
                const preset = JSON.parse(presetData);
                
                currentTheme.name = preset.name || 'Alauncher Theme';
                currentTheme.author = 'Theme Store';
                currentTheme.description = `Theme inspired by ${preset.name}`;
                currentTheme.clockStyle = 'character_card';
                
                if (preset.colors) {
                    currentTheme.colors = { ...preset.colors };
                }
                
                if (preset.wallpaperData) {
                    currentTheme.wallpaper = preset.wallpaperData;
                    const preview = document.getElementById('wallpaper-preview');
                    const previewImg = document.getElementById('wallpaper-preview-img');
                    const upload = document.getElementById('wallpaper-upload');
                    previewImg.src = preset.wallpaperData;
                    preview.style.display = 'block';
                    upload.style.display = 'none';
                    const pw = document.getElementById('wallpaper-bg');
                    if (pw) { pw.src = preset.wallpaperData; pw.classList.add('loaded'); }
                }
                
                if (preset.characterData) {
                    currentTheme.character = preset.characterData;
                    const preview = document.getElementById('character-preview');
                    const previewImg = document.getElementById('character-preview-img');
                    const upload = document.getElementById('character-upload');
                    previewImg.src = preset.characterData;
                    preview.style.display = 'block';
                    upload.style.display = 'none';
                }
                
                if (preset.symbolData) {
                    currentTheme.symbol = preset.symbolData;
                    const preview = document.getElementById('symbol-preview');
                    const previewImg = document.getElementById('symbol-preview-img');
                    const upload = document.getElementById('symbol-upload');
                    previewImg.src = preset.symbolData;
                    preview.style.display = 'block';
                    upload.style.display = 'none';
                }
                
                const assetSelectors = [
                    { id: 'create-backdrops', thumbsId: 'create-backdrop-thumbs', urls: preset.backdropUrls, target: 'wallpaper', previewId: 'wallpaper-preview-img' },
                    { id: 'create-posters', thumbsId: 'create-poster-thumbs', urls: preset.posterUrls, target: 'character', previewId: 'character-preview-img' },
                    { id: 'create-logos', thumbsId: 'create-logo-thumbs', urls: preset.logoUrls, target: 'symbol', previewId: 'symbol-preview-img' }
                ];
                
                assetSelectors.forEach(({ id, thumbsId, urls, target, previewId }) => {
                    if (urls && urls.length > 1) {
                        const container = document.getElementById(id);
                        const thumbs = document.getElementById(thumbsId);
                        thumbs.innerHTML = urls.map((url, i) => `
                            <div class="backdrop-thumb${i === 0 ? ' active' : ''}" data-idx="${i}" data-url="${url}">
                                <img src="${url}" alt="" loading="lazy">
                            </div>
                        `).join('');
                        container.style.display = 'block';
                        
                        thumbs.querySelectorAll('.backdrop-thumb').forEach(el => {
                            el.addEventListener('click', async () => {
                                const idx = parseInt(el.dataset.idx);
                                const thumbUrl = el.dataset.url;
                                const dataUrl = await urlToDataUrl(thumbUrl);
                                if (!dataUrl) return;
                                currentTheme[target] = dataUrl;
                                document.getElementById(previewId).src = dataUrl;
                                updatePreview();
                                if (target === 'wallpaper') {
                                    const pw = document.getElementById('wallpaper-bg');
                                    if (pw) { pw.src = dataUrl; pw.classList.add('loaded'); }
                                }
                                thumbs.querySelectorAll('.backdrop-thumb').forEach(t => t.classList.remove('active'));
                                el.classList.add('active');
                            });
                        });
                    }
                });
                
                document.getElementById('theme-name').value = currentTheme.name;
                document.getElementById('theme-author').value = currentTheme.author;
                document.getElementById('theme-description').value = currentTheme.description;
                document.getElementById('clock-style').value = 'character_card';
                
                updateColorInputs();
                updatePreview();
                showToast('Theme loaded!');
                
                localStorage.removeItem('tmdb_preset');
            } catch (e) {
                console.error('Error loading preset:', e);
            }
        }
    }
}

document.addEventListener('DOMContentLoaded', init);
