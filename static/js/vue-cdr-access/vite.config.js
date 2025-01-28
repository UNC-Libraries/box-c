import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import UnheadVite from '@unhead/addons/vite';

const path = require('path');

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [vue(), UnheadVite()],
    resolve: {
        alias: {
            "@": path.resolve(__dirname, "./src"),
        }
    },
    build: {
        minify: false,
        rollupOptions: {
            output: {
                entryFileNames: `assets/access/vue-access-[name].js`,
                chunkFileNames: `assets/access/vue-access-[name].js`,
                assetFileNames: `assets/access/[name].[ext]`
            }
        }
    },
    base: '/static/'
});