import { createRouter, createWebHistory } from 'vue-router';
import modal from "@/components/modal.vue";
import preIngest from "@/components/chompb/preIngest.vue";
import velocicroptorReport from "@/components/chompb/velocicroptorReport.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
     {
       path: '/admin/list/:uuid?',
       name: 'modalEditor',
       component:  modal
     },
     {
       path: '/admin/search/:uuid?',
       name: 'modalEditorSearch',
       component:  modal
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
    }
  ]
});

export default router;
