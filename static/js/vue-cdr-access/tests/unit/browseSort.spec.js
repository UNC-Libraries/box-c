import { mount, flushPromises } from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import {createTestingPinia} from '@pinia/testing';
import { useAccessStore } from '@/stores/access';
import browseSort from '@/components/browseSort.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import searchWrapper from '@/components/searchWrapper.vue';
import {createI18n} from "vue-i18n";
import translations from "@/translations";

let wrapper, wrapper_search, router, store;

describe('browseSort.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        router = createRouter({
            history: createWebHistory(process.env.BASE_URL),
            routes: [
                {
                    path: '/record/:uuid',
                    name: 'displayRecords',
                    component: displayWrapper
                },
                {
                    path: '/search/:uuid?',
                    name: 'searchRecords',
                    component: searchWrapper
                }
            ]
        });
        wrapper = mount(browseSort, {
            global: {
                plugins: [router, i18n]
            },
            props: {
                browseType: 'display'
            }
        });

        wrapper_search = mount(browseSort, {
            global: {
                plugins: [router, i18n, createTestingPinia({
                    stubActions: false
                })]
            },
            props: {
                browseType: 'search'
            }
        });

        store = useAccessStore();
    });

    it("shows the default, 'Relevance', option when mounted with no sort specified", () => {
        expect(wrapper.vm.sort_order).toEqual('default,normal');
    });

    it("updates the url when the dropdown changes for browsing", async () => {
        await router.push('/record/d77fd8c9-744b-42ab-8e20-5ad9bdf8194e/?collection_name=testCollection&sort=title,normal');
        wrapper.findAll('option')[2].setSelected();
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.sort).toEqual('title,reverse');
        expect(wrapper.vm.sort_order).toEqual('title,reverse');
    });

    it("updates the url when the dropdown changes for a search", async () => {
        await router.push('/search/d77fd8c9-744b-42ab-8e20-5ad9bdf8194e?collection_name=testCollection&sort=title,normal');
        wrapper_search.findAll('option')[2].setSelected();
        await flushPromises();
        expect(wrapper_search.vm.$router.currentRoute.value.query.sort).toEqual('title,reverse');
        expect(wrapper_search.vm.sort_order).toEqual('title,reverse');
    });

    it("maintains the base path when changing search pages", async () => {
        await router.push('/search/d77fd8c9-744b-42ab-8e20-5ad9bdf8194e?collection_name=testCollection&sort=title,normal');

        // Change sort types
        wrapper_search.findAll('option')[2].setSelected(true);
        await flushPromises();
        expect(wrapper_search.vm.sort_order).toEqual('title,reverse');
        expect(wrapper_search.vm.$route.path).toEqual('/search/d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
    });

    afterEach(() => {
        wrapper = null;
        wrapper_search = null;
        router = null;
        store.$reset();
    });
});
