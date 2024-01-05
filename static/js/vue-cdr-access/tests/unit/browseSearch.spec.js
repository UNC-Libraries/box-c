import { flushPromises, mount } from '@vue/test-utils';
import { createI18n } from 'vue-i18n';
import  { createRouter, createWebHistory } from 'vue-router';
import {createTestingPinia} from '@pinia/testing';
import { useAccessStore } from '@/stores/access';
import browseSearch from '@/components/browseSearch.vue';
import displayWrapper from "@/components/displayWrapper.vue";
import translations from "@/translations";

const query = 'Test Collection';
let wrapper, router, store;

describe('browseSearch.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(async () => {
        router = createRouter({
            history: createWebHistory(process.env.BASE_URL),
            routes: [
                {
                    path: '/record/:uuid',
                    name: 'displayRecords',
                    component: displayWrapper
                }
            ]
        });
        wrapper = mount(browseSearch, {
            global: {
                plugins: [i18n, router, createTestingPinia({
                    stubActions: false
                })]
            },
            props: {
                objectType: 'Folder'
            }
        });
        store = useAccessStore();

        await router.push('/record/1234');
        wrapper.vm.$router.currentRoute.value.query.anywhere = '';
    });

    afterEach(() =>  {
        store.$reset();
        wrapper = null;
        router = null;
    });

    it("updates the url when search results change", async() => {
        wrapper.find('input').setValue(query);
        let btn = wrapper.find('button');
        await btn.trigger('click');
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.anywhere).toEqual(encodeURIComponent(query));
    });

    it("sets placeholder text from the object type", () => {
        expect(wrapper.find('input').attributes('placeholder')).toBe('Search within this folder');
    });

    it("sets default placeholder text if no object type is given", () => {
        const $route = {
            path: '/record/1234',
            name: 'displayRecords',
            query: { anywhere: encodeURIComponent('Test Folder') }
        };

        wrapper = mount(browseSearch, {
            global: {
                mocks: {
                    $route
                },
                plugins: [i18n, createTestingPinia({
                    stubActions: false
                })]
            }
        });
        store = useAccessStore();
        expect(wrapper.find('input').attributes('placeholder')).toBe('Search within this object');
    });

    it("can set the search query value from the url", () => {
        const $route = {
            path: '/record/1234',
            name: 'displayRecords',
            query: { anywhere: encodeURIComponent('Test Folder') }
        };

        wrapper = mount(browseSearch, {
            global: {
                mocks: {
                    $route
                },
                plugins: [i18n, createTestingPinia({
                    stubActions: false
                })]
            },
            props: {
                objectType: 'Folder'
            }
        });
        store = useAccessStore();
        expect(wrapper.vm.search_query).toEqual('Test Folder');
    });
});