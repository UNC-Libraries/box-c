import { createApp, h } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import {useFormsStore} from './stores/forms';
import { usePermissionsStore } from './stores/permissions';
import Vueform from '@vueform/vueform';
import vueformConfig from './../vueform.config';
import './assets/vueform.nodark.css';
import './assets/common-styles.css'

const pinia = createPinia();
window.perms_editor = createApp({
  render() {
    return h(App);
  }
}).use(Vueform, vueformConfig).use(pinia).use(router).mount('#vue-admin-app');

/**
 * Need to define our Pinia store instance as a global, due to the way the build scopes variables,
 * so it can be used by non-Vue admin app code
 * Action menu needs to be able to set data values on Vue instance
 * Used by resultObjectActionMenu.js
 */
window.perms_editor_store = usePermissionsStore();
/**
 * Used by addMenu.js
 */
window.forms_app_store = useFormsStore();