import { createRouter, createWebHistory } from 'vue-router';
import modalEditor from "@/components/permissions-editor/modalEditor.vue";
import preIngest from "@/components/chompb/preIngest.vue";
import croppingReport from "@/components/chompb/croppingReport.vue";

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
      path: '/admin/chompb/cropping_report/:project',
      name: 'croppingReport',
      component: croppingReport
    },
  ]
});

export default router;
