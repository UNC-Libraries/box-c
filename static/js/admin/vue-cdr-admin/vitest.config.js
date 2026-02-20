import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
    plugins: [vue()],
    test: {
        globals: true,
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
    },
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src')
        }
    }
})