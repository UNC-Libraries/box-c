import { createRouter, createWebHistory } from 'vue-router';
import modalEditor from "@/components/permissions-editor/modalEditor.vue";
import preIngest from "@/components/chompb/preIngest.vue";
import velocicroptorReport from "@/components/chompb/velocicroptorReport.vue";
import formsContainer from "@/components/forms-app/formsContainer.vue";

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
    },
    {
      path: '/admin/chompb',
      name: 'preIngest',
      component: preIngest
    },
    {
      path: '/admin/chompb/project/:project/processing_results/velocicroptor',
      name: 'velocicroptorReport',
      component: velocicroptorReport
    },
    {
      path: '/admin/forms',
      name: 'formsContainer',
      component: formsContainer
    }
  ]
});

export default router;
