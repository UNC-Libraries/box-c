import { createWebHistory, createRouter } from 'vue-router'
import axios from 'axios';
import advancedSearch from "@/components/advancedSearch.vue";
import displayWrapper from "@/components/displayWrapper.vue";
import searchWrapper from "@/components/searchWrapper.vue";
import collectionBrowseWrapper from "@/components/collectionBrowseWrapper.vue";
import frontPage from "@/components/frontPage.vue";
import aboutRepository from "@/components/aboutRepository.vue";
import store from './store'

const router = createRouter({
  history: createWebHistory(),
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
    },
    {
      path: '/aboutRepository',
      name: 'aboutRepository',
      component: aboutRepository
    }
  ]
});

router.beforeEach((to, from) => {
  axios.head('/userInformation').then((response) => {
    store.commit('setUsername', response.headers['username']);
    store.commit('setIsLoggedIn');
    store.commit('setViewAdmin', response.headers['can-view-admin']);
  });
});

export default router