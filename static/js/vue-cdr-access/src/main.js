import { createApp, h } from 'vue';
import { createI18n } from 'vue-i18n';
import { createHead, VueHeadMixin } from '@unhead/vue/client';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import translations from '@/translations';
import './assets/common-styles.css';
import './assets/nouislider.css'; // Imported here, otherwise it breaks component tests, as an invalid import

if (document.getElementById('app') !== null && window.dcr_browse_records === undefined) {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    const default_title = 'Digital Collections Repository';
    const head = createHead({
        base: { href: `https://${window.location.host}/` },
        titleTemplate: (title) => {
            return !title ? default_title : `${default_title} - ${title}`
        }
    });

    const pinia = createPinia();

    // Set this here as it gives a build error otherwise
    window.matomoSiteSrcUrl = import.meta.env.VITE_MATOMO_SITE_SRC_URL || '';
    window.pdfViewerLicense = import.meta.env.VITE_VPV_LICENSE || ''

    window.dcr_browse_records = createApp({
        render() {
            return h(App);
        }
    }).mixin(VueHeadMixin)
        .use(pinia)
        .use(head)
        .use(router)
        .use(i18n).mount('#app');
}