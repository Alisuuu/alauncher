const API = {
  base() {
    if (typeof CONFIG === 'undefined' || !CONFIG.API_BASE_URL) {
      console.warn('config.js nao carregado. Configure local/config.js com API_BASE_URL');
      return '';
    }
    return CONFIG.API_BASE_URL.replace(/\/+$/, '');
  },

  async search(query) {
    const r = await fetch(`${this.base()}/api/search?q=${encodeURIComponent(query)}`);
    if (!r.ok) throw new Error('Erro na pesquisa');
    return r.json();
  },

  async discover(type) {
    const r = await fetch(`${this.base()}/api/discover?type=${type}`);
    if (!r.ok) throw new Error('Erro ao carregar');
    return r.json();
  },

  async details(id, type) {
    const r = await fetch(`${this.base()}/api/details?id=${id}&type=${type}`);
    if (!r.ok) throw new Error('Erro ao carregar detalhes');
    return r.json();
  },

  async download(payload) {
    const r = await fetch(`${this.base()}/api/download`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    if (!r.ok) {
      const err = await r.json().catch(() => ({}));
      throw new Error(err.error || 'Erro ao baixar tema');
    }
    return r.blob();
  }
};
