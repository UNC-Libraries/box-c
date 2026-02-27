import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';
import path from 'path';

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src')
        }
    },
    build: {
        minify: false,
        rollupOptions: {
            output: {
                entryFileNames: `assets/vue-admin-[name].js`,
                chunkFileNames: `assets/vue-admin-[name].js`,
                assetFileNames: `assets/[name].[ext]`
            }
        }
    },
    test: {
        globals: true,
        setupFiles: ['./vitest.setup.js'],
        environment: 'jsdom',
        environmentOptions: {
            customExportConditions: ['node', 'node-addons']
        },
        coverage: {
            enabled: true,
            provider: 'v8',
            reporter: [['lcov', { projectRoot: '../../../../' }], 'json', 'text'],
            include: ['**/src/components/**', '**/src/mixins/**']
        }
    }
});