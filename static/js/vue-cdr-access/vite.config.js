import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url)),
        },
        conditions: ['node', 'node-addons'],
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
    base: '/static/',
    test: {
        globals: true,
        environment: 'jsdom',
        environmentOptions: {
            jsdom: {
                url: 'https://localhost/record/73bc003c-9603-4cd9-8a65-93a22520ef6a',
            },
        },
        setupFiles: ['vitest-localstorage-mock', './vitest.setup.js'],
        coverage: {
            enabled: true,
            provider: 'v8',
            include: ['src/components/**', 'src/mixins/**'],
            reporter: [
                ['lcov', { projectRoot: '../../../' }],
                'json',
                'text'
            ],
        },
        server: {
            deps: {
                inline: [/pdf-js-dist/, /@vue-pdf-viewer\/viewer/],
            },
        },
    },
});