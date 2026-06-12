import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';

export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src')
        },
        dedupe: ['jquery']
    },
    optimizeDeps: {
        include: [
            'jquery',
            'datatables.net-vue3',
            'datatables.net-bm',
            'datatables.net-fixedheader',
            'datatables.net-buttons-bm',
            'datatables.net-buttons/js/buttons.colVis.js',
            'datatables.net-searchpanes-bm',
            'datatables.net-select-bm',
            'datatables.mark.js'
        ]
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