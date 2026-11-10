/// <reference types="vitest" />
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '..', '');
  const appBaseUrl = process.env.APP_BASE_URL || env.APP_BASE_URL || 'http://localhost:5173';
  const proxyTarget = process.env.VITE_API_PROXY_TARGET || env.VITE_API_PROXY_TARGET || 'http://localhost:8080';
  const extraHosts = (process.env.APP_ALLOWED_HOSTS || env.APP_ALLOWED_HOSTS || '')
    .split(',').map((host) => host.trim()).filter(Boolean);
  const appHost = new URL(appBaseUrl).hostname;
  const allowedHosts = Array.from(new Set(['localhost', '127.0.0.1', appHost, '.trycloudflare.com', ...extraHosts]));

  return {
    plugins: [react()],
    server: {
      port: 5173,
      allowedHosts,
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
        },
      },
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            'vendor-react': ['react', 'react-dom', 'react-router-dom'],
            'vendor-query': ['@tanstack/react-query'],
            'vendor-icons': ['lucide-react'],
          },
        },
      },
      chunkSizeWarningLimit: 600,
    },
    test: {
      globals: true,
      environment: 'jsdom',
      setupFiles: './src/test/setup.ts',
    },
  };
});
