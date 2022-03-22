import { createRouter, createWebHistory } from 'vue-router';
import modalEditor from "./components/modalEditor";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
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
});

export default router;
