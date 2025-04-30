import en from '@vueform/vueform/locales/en'
import vueform from '@vueform/vueform/dist/vueform'
import { defineConfig } from '@vueform/vueform'

// https://vueform.com/docs/file-uploads#global-upload-endpoint-config
export default defineConfig({
    theme: vueform,
    locales: { en },
    locale: 'en',
    endpoints: {
        uploadTempFile: {
            url: '/services/api/edit/ingest/stageFile',
            method: 'post'
        },
        removeTempFile: {
            url: '/services/api/edit/ingest/removeStagedFile',
            method: 'post',
        }
    }
});