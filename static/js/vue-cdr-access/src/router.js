import { createWebHistory, createRouter } from 'vue-router'
import advancedSearch from "@/components/advancedSearch.vue";
import displayWrapper from "@/components/displayWrapper.vue";
import searchWrapper from "@/components/searchWrapper.vue";
import collectionBrowseWrapper from "@/components/collectionBrowseWrapper.vue";
import frontPage from "@/components/frontPage.vue";

export default createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/advancedSearch',
      name: 'advancedSearch',
      component: advancedSearch
    },
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
    },
    {
      path: '/',
      name: 'frontPage',
      component: frontPage
    }
  ]
});