import { createApp, h } from 'vue'
import App from './App.vue'
import router from './router'

if (document.getElementById('app') !== null && window.dcr_browse_records === undefined) {
  window.dcr_browse_records = createApp({
    render() {
      return h(App);
    }
  }).use(router).mount('#app');
}