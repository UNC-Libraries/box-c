import Vue from 'vue'
import Router from 'vue-router'
import browseDisplay from "./components/browseDisplay";

Vue.use(Router);

export default new Router({
  mode: 'history',
  base: process.env.BASE_URL,
  routes: [
    {
      path: '/record/:uuid/',
      name: 'browseDisplay',
      component: browseDisplay
    }
  ]
});