import { createWebHistory, createRouter } from 'vue-router';
import aboutRepository from "@/components/aboutRepository.vue";
import advancedSearch from "@/components/advancedSearch.vue";
import displayWrapper from "@/components/displayWrapper.vue";
import notFound from "@/components/error_pages/notFound.vue";
import searchWrapper from "@/components/searchWrapper.vue";
import collectionBrowseWrapper from "@/components/collectionBrowseWrapper.vue";
import frontPage from "@/components/frontPage.vue";
import cfTurnstile from "@/components/cfTurnstile.vue";
import { useAccessStore } from './stores/access';
import { shouldRedirectToTurnstile } from './routerGuard';

const UUID_REGEX = '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/advancedSearch',
      name: 'advancedSearch',
      component: advancedSearch
    },
    {
      path: `/record/:id(${UUID_REGEX})/`,
      name: 'displayRecords',
      component: displayWrapper
    },
    { // Old style DCR full record urls
      path: `/record/:id(uuid:${UUID_REGEX})/`,
      redirect: to => {
        return { path: to.path.replace('uuid:', '') }
      }
    },
    { // Old style DCR list urls
      path: `/list/:pathMatch(.*)*`,
      redirect: to => {
        return { path: to.path.replace('/list/', '/record/') }
      }
    },
    {
      path: `/search/:id(${UUID_REGEX})?`,
      name: 'searchRecords',
      component: searchWrapper,
    },
    {
      path: '/collections',
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
    },
    {
      path: '/turnstile',
      name: 'turnstile',
      component: cfTurnstile
    },
    {
      // https://router.vuejs.org/guide/migration/#removed-star-or-catch-all-routes
      path: '/:pathMatch(.*)*',
      name: 'notFound',
      component: notFound
    }
  ]
});

router.beforeEach(async (to) => {
  const store = useAccessStore();

  if (await shouldRedirectToTurnstile(to, store)) {
    return { path: '/turnstile', replace: true };
  }

  return true;
});

export default router