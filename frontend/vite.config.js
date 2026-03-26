import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  base: '/',
  build: {
    outDir: '../src/main/resources/de/petanqueturniermanager/webserver/static',
    emptyOutDir: true,
  },
  server: {
    // Dev-Server: SSE-Requests an den Java-Backend weiterleiten
    proxy: {
      '/events': 'http://localhost:8080',
      '/assets/data': 'http://localhost:8080',
    },
  },
});
