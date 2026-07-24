import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';
import prefixSelector from 'postcss-prefix-selector';

export default defineConfig({
    plugins: [vue()],
    css: {
        postcss: {
            plugins: [
                prefixSelector({
                    prefix: '.vue-dcr-admin-wrapper',
                    transform(prefix, selector, prefixedSelector, filePath) {
                        // Only scope Bulma no-dark-mode so existing project CSS behavior stays intact.
                        if (!filePath || !filePath.includes('bulma-no-dark-mode.min.css')) {
                            return selector;
                        }

                        // :root custom properties should stay at root scope to be inherited by all children
                        if (selector === ':root') {
                            return selector;
                        }

                        return prefixedSelector;
                    }
                })
            ]
        }
    },
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
        // Keep a single distributable JS file because the deployment copy step
        // only ships vue-admin-index.js into /static/assets/admin.
        codeSplitting: false,
        rollupOptions: {
            output: {
                // Some bundled UMD dependencies probe `define.amd`; shadowing `define`
                // keeps them from registering anonymous AMD modules into RequireJS.
                banner: 'var define = undefined;',
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