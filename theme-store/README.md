# Alauncher Theme Store

A web-based theme store and creator for Alauncher Android app.

## Features

### TMDB Theme Presets (`index.html`)
- Browse trending movies, series, and anime from TMDB
- Search for specific titles
- Generate theme presets based on movie/series visual identity
- Live preview of generated themes
- Download theme ZIP directly
- Send to editor for customization

### Theme Creator (`create.html`)
- Live preview of theme changes (Home and Drawer)
- Customize all colors (background, surface, primary, text, glow)
- Adjust layout settings (columns, corner radius, borders)
- Upload custom assets (wallpaper, character, symbol)
- Use preset color schemes
- Generate random colors
- Download as ZIP file with all assets
- Load TMDB presets for customization

## How to Use

### TMDB Presets
1. Open `index.html` in a web browser
2. Browse or search for movies/series/anime
3. Click on a title to preview the generated theme
4. Download directly or send to the editor for customization

### Custom Theme Creator
1. Open `create.html` in a web browser
2. Customize your theme using the editor
3. Click "Download ZIP" to get the theme file with all assets
4. Import the ZIP in Alauncher Settings > Themes

## Theme JSON Format

The exported JSON follows this structure:

```json
{
  "name": "Theme Name",
  "id": "custom_1234567890",
  "author": "Author Name",
  "description": "Theme description",
  "wallpaperPath": "wallpaper.jpg",
  "cardXmlPath": "texture.xml",
  "cardTexture": "grid",
  "characterPath": "character.png",
  "symbolPath": "symbol.png",
  "useCardTexture": true,
  "colors": {
    "background": "#06060A",
    "surface": "#0E0E14",
    "primary": "#FFFFFF",
    "textPrimary": "#FFFFFF",
    "textSecondary": "#A0A0B0",
    "glowColor": "#FFFFFF"
  },
  "shapes": {
    "cardCornerRadius": 16,
    "searchBarCornerRadius": 28,
    "showBorders": true
  },
  "layout": {
    "columns": 4,
    "showAppName": true
  }
}
```

## Installation

No installation required! Just open the HTML file in a web browser.

For local development, you can use any simple HTTP server:

```bash
# Python
python -m http.server 8000

# Node.js
npx serve .

# PHP
php -S localhost:8000
```
