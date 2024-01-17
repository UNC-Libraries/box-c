import { createApp, h } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import { usePermissionsStore } from './stores/permissions';
import './assets/common-styles.css'

const pinia = createPinia();
window.perms_editor = createApp({
  render() {
    return h(App);
  }
}).use(pinia).use(router).mount('#permissions-app');

/**
 * Need to define our Pinia store instance as a global, due to the way the build scopes variables,
 * so it can be used by non-Vue admin app code
 * Action menu needs to be able to set data values on Vue instance
 * Used by resultObjectActionMenu.js
 */
window.perms_editor_store = usePermissionsStore();