import { createApp, h } from 'vue'
import { createI18n } from 'vue-i18n'
import App from './App.vue'
import router from './router'
import translations from '@/translations';
import './assets/nouislider.css'; // Imported here, otherwise it breaks component tests, as an invalid import

if (document.getElementById('app') !== null && window.dcr_browse_records === undefined) {
  const i18n = createI18n({
    locale: 'en',
    fallbackLocale: 'en',
    messages: translations
  });

  window.dcr_browse_records = createApp({
    render() {
      return h(App);
    }
  }).use(router).use(i18n).mount('#app');
}