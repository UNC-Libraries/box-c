import Vue from 'vue'
import App from './App.vue'
import router from './router'
import 'fg-select-css/src/select-css.css'
import './assets/common-styles.css'

Vue.config.productionTip = false

/**
 * Need to define our Vue instance as a global, due to the way Webpack scopes variables, so it can be used by non-Vue admin app code
 * See https://forum.vuejs.org/t/how-to-access-vue-from-chrome-console/3606
 * Action menu needs to be able to set data values on Vue instance
 * Used by resultObjectActionMenu.js
 */
window.perms_editor = new Vue({
  router,
  render: h => h(App)
}).$mount('#permissions-app')
