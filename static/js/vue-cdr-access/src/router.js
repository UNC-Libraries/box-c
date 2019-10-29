import Vue from 'vue'
import Router from 'vue-router'
import displayWrapper from "./components/displayWrapper";

Vue.use(Router);

export default new Router({
  mode: 'history',
  base: process.env.BASE_URL,
  routes: [
    {
      path: '/record/:uuid/',
      name: 'displayRecords',
      component: displayWrapper
    }
  ]
});