import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            // Replaces moduleNameMapper: '@'
            '@': fileURLToPath(new URL('./src', import.meta.url)),
        },
        // Replaces testEnvironmentOptions.customExportConditions
        conditions: ['node', 'node-addons'],
    },
    test: {
        globals: true,
        environment: 'jsdom',
        environmentOptions: {
            jsdom: {
                url: 'https://localhost/record/73bc003c-9603-4cd9-8a65-93a22520ef6a',
            },
        },
        // Replaces setupFiles: ['jest-localstorage-mock']
        setupFiles: ['vitest-localstorage-mock', './vitest.setup.ts'],
        coverage: {
            provider: 'v8',
            enabled: true,
            include: ['src/components/**', 'src/mixins/**'],
            reporter: [
                ['lcov', { projectRoot: '../../../' }],
                'json',
                'text'
            ],
        },
        // Replaces transformIgnorePatterns: ["node_modules/pdf-js-dist"]
        server: {
            deps: {
                inline: [/pdf-js-dist/, /@vue-pdf-viewer\/viewer/],
            },
        },
    },
});
