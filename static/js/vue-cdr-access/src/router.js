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

  // Can't use the fetchWrapper mixin here, as the router is not a component and doesn't have access to mixins.
  try {
    const response = await fetch('/api/userInformation', { method: 'HEAD' });

    store.setUsername(response.headers.get('username'));
    store.setIsLoggedIn();
    store.setViewAdmin(response.headers.get('can-view-admin'));
    store.setUncIP(response.headers.get('unc-ip-address') === 'true');
    store.setValidToken(response.headers.get('valid-turnstile-token') === 'true');

    // Issue a challenge if a user needs a challenge.
    if (window.turnstileEnabled === 'True' && to.name === 'searchRecords' &&
      !store.isLoggedIn && !store.uncIP && !store.validToken) {
      return { path: '/turnstile', replace: true };
    }
  } catch (error) {
    console.log(error);
  }

  return true;
});

export default router