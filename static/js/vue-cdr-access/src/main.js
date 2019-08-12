import Vue from 'vue'
import App from './App.vue'
import router from './router'

Vue.config.productionTip = false

if (document.getElementById('app') !== null) {
  new Vue({
    router,
    render: h => h(App)
  }).$mount('#app')
}