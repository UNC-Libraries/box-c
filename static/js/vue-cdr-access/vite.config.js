import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

const path = require('path');

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [vue()],
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
                assetFileNames: `assets/access/[name].[ext]`,
                inlineDynamicImports: true
            }
        }
    },
    base: '/static/'
});