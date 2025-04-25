import en from '@vueform/vueform/locales/en'
import vueform from '@vueform/vueform/dist/vueform'
import { defineConfig } from '@vueform/vueform'

// You might place these anywhere else in your project
import '@vueform/vueform/dist/vueform.css';

// https://vueform.com/docs/file-uploads#global-upload-endpoint-config
export default defineConfig({
    theme: vueform,
    locales: { en },
    locale: 'en',
    endpoints: {
        submit: {
            url: (form) => {
                // submit to the destination from the form
                const destinationId = form.data.destination;
                return `/services/api/edit/ingest/${destinationId}`;
            },
            method: 'post',
            data: (form) => {
                return {
                    ...form.data,
                    type: 'https://library.unc.edu/dcr/packaging/WorkFormJson1.0'
                };
            }
        },
        uploadTempFile: {
            url: '/services/api/edit/ingest/stageFile',
            method: 'post'
        },
        removeTempFile: {
            url: '/services/api/edit/ingest/removeStagedFile',
            method: 'post',
        },
    }
});