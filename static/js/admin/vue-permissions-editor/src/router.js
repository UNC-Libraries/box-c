import Vue from 'vue'
import Router from 'vue-router'
import modalEditor from "./components/modalEditor";

Vue.use(Router)

export default new Router({
  mode: 'history',
  base: process.env.BASE_URL,
  routes: [
    {
      path: '/admin/list/:uuid?',
      name: 'modalEditor',
      component:  modalEditor
    },
    {
      path: '/admin/search/:uuid?',
      name: 'modalEditorSearch',
      component:  modalEditor
    }
  ]
})
