const { getDefaultConfig } = require('expo/metro-config');

const config = getDefaultConfig(__dirname);

config.server = {
  ...config.server,
  enhanceMiddleware: (middleware) => {
    return (req, res, next) => {
      if (req.url?.startsWith('/mcp-proxy')) {
        const targetUrl = req.url.replace('/mcp-proxy', '');
        const target = `http://185.190.140.93:8022${targetUrl}`;

        const headers = { ...req.headers };
        delete headers.host;
        headers['host'] = '185.190.140.93:8022';

        const proxyReq = require('http').request(
          target,
          {
            method: req.method,
            headers,
          },
          (proxyRes) => {
            res.writeHead(proxyRes.statusCode ?? 500, proxyRes.headers);
            proxyRes.pipe(res);
          }
        );

        proxyReq.on('error', (err) => {
          res.writeHead(502);
          res.end(JSON.stringify({ error: err.message }));
        });

        req.pipe(proxyReq);
        return;
      }
      middleware(req, res, next);
    };
  },
};

module.exports = config;
