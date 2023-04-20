import { createApp, h } from 'vue'
import { createI18n } from 'vue-i18n'
import { createHead, VueHeadMixin } from "@vueuse/head"
import VueGtag from 'vue-gtag';
import App from './App.vue'
import router from './router'
import store from './store'
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

    const gaCode = import.meta.env.VITE_GA_CODE || '';

    window.dcr_browse_records = createApp({
        render() {
            return h(App);
        }
    }).mixin(VueHeadMixin)
        .use(head)
        .use(store)
        .use(router)
        .use(i18n)
        .use(VueGtag, {
            config: {
                id: gaCode
            }
        }).mount('#app');
}