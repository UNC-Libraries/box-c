import { createApp, h } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import './assets/common-styles.css'

window.perms_editor = createApp({
  render() {
    return h(App);
  }
}).use(router).use(store).mount('#permissions-app');

/**
 * Need to define our Vuex store instance as a global, due to the way Webpack scopes variables,
 * so it can be used by non-Vue admin app code
 * Action menu needs to be able to set data values on Vue instance
 * Used by resultObjectActionMenu.js
 */
window.perms_editor_store = store;