const express = require('express');
const path = require('path');
const { createProxyMiddleware } = require('http-proxy-middleware');

const app = express();
const PORT = process.env.PORT || 8080;
const API_TARGET = process.env.API_URL || 'https://tmdbserver-seven.vercel.app';

app.use('/api', createProxyMiddleware({
  target: API_TARGET,
  changeOrigin: true
}));

app.use(express.static(path.join(__dirname)));

app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'index.html'));
});

app.listen(PORT, () => {
  console.log(`Frontend rodando em http://localhost:${PORT}`);
  console.log(`API remota: ${API_TARGET}`);
});
