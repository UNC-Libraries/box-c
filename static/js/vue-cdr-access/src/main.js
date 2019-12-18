import Vue from 'vue'
import App from './App.vue'
import router from './router'

Vue.config.productionTip = false

if (document.getElementById('app') !== null && window.dcr_browse_records === undefined) {
  window.dcr_browse_records = new Vue({
    router,
    render: h => h(App)
  }).$mount('#app')
}