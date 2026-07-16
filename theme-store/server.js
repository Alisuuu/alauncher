try { require('dotenv').config(); } catch (e) {}
const express = require('express');
const cors = require('cors');
const axios = require('axios');
const sharp = require('sharp');
const archiver = require('archiver');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 3000;
const TMDB_KEY = process.env.TMDB_API_KEY || '5e5da432e96174227b25086fe8637985';
const TMDB_BASE = 'https://api.themoviedb.org/3';

app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.static(path.join(__dirname, 'public')));


const tmdb = axios.create({
  baseURL: TMDB_BASE,
  params: { api_key: TMDB_KEY, language: 'pt-BR' }
});

app.get('/api/search', async (req, res) => {
  try {
    const { q } = req.query;
    if (!q) return res.status(400).json({ error: 'Parâmetro "q" obrigatório' });

    const [movies, tv, multi] = await Promise.allSettled([
      tmdb.get('/search/movie', { params: { query: q } }).then(r => r.data.results.slice(0, 6)),
      tmdb.get('/search/tv', { params: { query: q } }).then(r => r.data.results.slice(0, 6)),
      tmdb.get('/search/multi', { params: { query: q } }).then(r => r.data.results.slice(0, 6))
    ]);

    const results = [];
    const seen = new Set();

    const push = (items, mediaType) => {
      for (const item of items) {
        if (item.media_type === 'person') continue;
        const id = `${mediaType}_${item.id}`;
        if (seen.has(id)) continue;
        seen.add(id);
        results.push({
          id: item.id,
          mediaType: item.media_type || mediaType,
          title: item.title || item.name,
          year: (item.release_date || item.first_air_date || '').slice(0, 4),
          poster: item.poster_path,
          backdrop: item.backdrop_path,
          overview: item.overview,
          voteAverage: item.vote_average
        });
      }
    };

    if (multi.status === 'fulfilled') push(multi.value, '');
    if (movies.status === 'fulfilled') push(movies.value, 'movie');
    if (tv.status === 'fulfilled') push(tv.value, 'tv');

    res.json(results.slice(0, 18));
  } catch (err) {
    console.error('Search error:', err.message);
    res.status(500).json({ error: 'Erro ao pesquisar' });
  }
});

app.get('/api/discover', async (req, res) => {
  try {
    const { type } = req.query;
    const endpoint = type === 'tv' ? '/trending/tv/week' : '/trending/movie/week';
    const { data } = await tmdb.get(endpoint, { params: { language: 'pt-BR' } });
    const results = (data.results || []).slice(0, 12).map(item => ({
      id: item.id,
      mediaType: type === 'tv' ? 'tv' : 'movie',
      title: item.title || item.name,
      year: (item.release_date || item.first_air_date || '').slice(0, 4),
      poster: item.poster_path,
      backdrop: item.backdrop_path,
      overview: item.overview,
      voteAverage: item.vote_average
    }));
    res.json(results);
  } catch (err) {
    console.error('Discover error:', err.message);
    res.status(500).json({ error: 'Erro ao carregar' });
  }
});

app.get('/api/details', async (req, res) => {
  try {
    const { id, type } = req.query;
    if (!id || !type) return res.status(400).json({ error: 'id e type obrigatórios' });

    const detail = type === 'tv'
      ? (await tmdb.get(`/tv/${id}`)).data
      : (await tmdb.get(`/movie/${id}`)).data;

    const imagesData = type === 'tv'
      ? (await tmdb.get(`/tv/${id}/images`, { params: { include_image_language: 'en,null' } })).data
      : (await tmdb.get(`/movie/${id}/images`, { params: { include_image_language: 'en,null' } })).data;

    const mapImg = path => path ? `https://image.tmdb.org/t/p/original${path}` : null;

    const backdrops = (imagesData.backdrops || []).map(b => mapImg(b.file_path)).filter(Boolean);
    const posters = (imagesData.posters || []).map(p => mapImg(p.file_path)).filter(Boolean);
    const logosList = (imagesData.logos || [])
      .filter(l => l.iso_639_1 === 'en' || !l.iso_639_1)
      .map(l => mapImg(l.file_path)).filter(Boolean);

    const title = detail.title || detail.name;

    res.json({
      id: detail.id,
      mediaType: type,
      title,
      overview: detail.overview,
      backdrop: mapImg(detail.backdrop_path),
      poster: mapImg(detail.poster_path),
      backdrops,
      posters,
      logos: logosList,
      logo: logosList[0] || null,
      year: (detail.release_date || detail.first_air_date || '').slice(0, 4),
      voteAverage: detail.vote_average
    });
  } catch (err) {
    console.error('Details error:', err.message);
    res.status(500).json({ error: 'Erro ao obter detalhes' });
  }
});

app.post('/api/download', async (req, res) => {
  try {
    const {
      title, id, backdropUrl, posterUrl, logoUrl, altBackdropUrl,
      cropLeft, cropTop, cropWidth, cropHeight,
      colors
    } = req.body;

    if (!title || !backdropUrl || !posterUrl) {
      return res.status(400).json({ error: 'Dados incompletos para gerar o tema' });
    }

    const baseDir = process.env.VERCEL ? '/tmp' : path.join(__dirname, 'downloads');
    const tmpDir = path.join(baseDir, `tmp_${Date.now()}`);
    fs.mkdirSync(tmpDir, { recursive: true });

    const safe = s => s.replace(/[<>:"/\\|?*]/g, '_');
    const themeId = `tmdb_${id}`;

    const download = async (url, dest) => {
      if (!url) return null;
      const resp = await axios({ url, responseType: 'arraybuffer', timeout: 30000 });
      fs.writeFileSync(dest, resp.data);
      return dest;
    };

    const wallSrc = path.join(tmpDir, 'wallpaper_src.jpg');
    await download(backdropUrl, wallSrc);

    const wallDest = path.join(tmpDir, 'wallpaper.jpg');
    if (cropLeft != null && cropTop != null && cropWidth > 0 && cropHeight > 0) {
      await sharp(wallSrc)
        .extract({ left: cropLeft, top: cropTop, width: cropWidth, height: cropHeight })
        .toFile(wallDest);
    } else {
      await sharp(wallSrc).toFile(wallDest);
    }

    const charDest = path.join(tmpDir, 'character.jpg');
    await download(posterUrl, charDest);

    let hasLogo = false;
    const symDest = path.join(tmpDir, 'symbol.png');
    if (logoUrl) {
      try {
        const buf = await axios({ url: logoUrl, responseType: 'arraybuffer', timeout: 15000 })
          .then(r => r.data);
        await sharp(buf).ensureAlpha().toFile(symDest);
        hasLogo = true;
      } catch {
        hasLogo = false;
      }
    }

    let hasAltBackdrop = false;
    const altWallDest = path.join(tmpDir, 'alt_wallpaper.jpg');
    if (altBackdropUrl) {
      try {
        const altSrc = path.join(tmpDir, 'alt_wallpaper_src.jpg');
        await download(altBackdropUrl, altSrc);
        await sharp(altSrc).toFile(altWallDest);
        hasAltBackdrop = true;
      } catch {
        hasAltBackdrop = false;
      }
    }

    const theme = {
      name: title,
      id: themeId,
      author: 'Theme Store',
      description: `Theme inspired by ${title}`,
      wallpaperPath: 'wallpaper.jpg',
      altBackdropPath: hasAltBackdrop ? 'alt_wallpaper.jpg' : null,
      cardXmlPath: null,
      cardTexture: null,
      characterPath: 'character.jpg',
      symbolPath: hasLogo ? 'symbol.png' : null,
      clockStyle: 'character_card',
      useCardTexture: true,
      colors: colors || {
        background: '#06060A',
        surface: '#0E0E14',
        primary: '#FFFFFF',
        textPrimary: '#FFFFFF',
        textSecondary: '#A0A0B0',
        glowColor: '#FFFFFF'
      },
      shapes: {
        cardCornerRadius: 16,
        searchBarCornerRadius: 28,
        showBorders: true
      },
      layout: {
        columns: 4,
        showAppName: true
      },
      texturePath: hasLogo ? 'symbol.png' : null
    };

    fs.writeFileSync(path.join(tmpDir, 'theme.json'), JSON.stringify(theme, null, 2));

    const zipPath = path.join(baseDir, `${safe(title)}_theme.zip`);
    const output = fs.createWriteStream(zipPath);
    const archive = archiver('zip', { zlib: { level: 9 } });

    archive.pipe(output);
    archive.file(wallDest, { name: 'wallpaper.jpg' });
    archive.file(charDest, { name: 'character.jpg' });
    if (hasLogo && fs.existsSync(symDest)) {
      archive.file(symDest, { name: 'symbol.png' });
    }
    if (hasAltBackdrop && fs.existsSync(altWallDest)) {
      archive.file(altWallDest, { name: 'alt_wallpaper.jpg' });
    }
    archive.append(JSON.stringify(theme, null, 2), { name: 'theme.json' });

    await archive.finalize();

    output.on('close', () => {
      res.download(zipPath, `${safe(title)}_theme.zip`, () => {
        fs.rmSync(tmpDir, { recursive: true, force: true });
        fs.unlinkSync(zipPath);
      });
    });

    output.on('error', () => {
      fs.rmSync(tmpDir, { recursive: true, force: true });
      res.status(500).json({ error: 'Erro ao gerar ZIP' });
    });
  } catch (err) {
    console.error('Download error:', err.message);
    res.status(500).json({ error: 'Erro ao gerar tema' });
  }
});

if (!process.env.VERCEL) {
  try { fs.mkdirSync(path.join(__dirname, 'downloads')); } catch {}
  app.listen(PORT, () => {
    console.log(`Theme Store rodando em http://localhost:${PORT}`);
  });
}

module.exports = app;
