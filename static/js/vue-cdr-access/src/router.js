import { createWebHistory, createRouter } from 'vue-router'
import displayWrapper from "@/components/displayWrapper.vue";
import searchWrapper from "@/components/searchWrapper.vue";
import collectionBrowseWrapper from "@/components/collectionBrowseWrapper.vue";

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
      component: collectionBrowseWrapper
    }
  ]
});