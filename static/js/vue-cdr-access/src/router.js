import { createWebHistory, createRouter } from 'vue-router'
import displayWrapper from "@/components/displayWrapper.vue";
import searchWrapper from "@/components/searchWrapper.vue";
import collectionBrowse from "@/components/collectionBrowse.vue";

export default createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/record/:uuid/',
      name: 'displayRecords',
      component: displayWrapper
    },
    {
      path: '/search/:uuid?/',
      name: 'searchRecords',
      component: searchWrapper
    },
    {
      path: '/collections/',
      name: 'collectionBrowse',
      component: collectionBrowse
    }
  ]
});