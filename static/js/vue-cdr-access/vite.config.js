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
                entryFileNames: `assets/vue-access-[name].js`,
                chunkFileNames: `assets/vue-access-[name].js`,
                assetFileNames: `assets/[name].[ext]`
            }
        }
    }
});