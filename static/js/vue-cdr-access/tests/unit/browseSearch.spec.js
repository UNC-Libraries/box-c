import { flushPromises, mount } from '@vue/test-utils';
import { createI18n } from 'vue-i18n';
import  { createRouter, createWebHistory } from 'vue-router';
import browseSearch from '@/components/browseSearch.vue';
import displayWrapper from "@/components/displayWrapper.vue";
import translations from "@/translations";

const query = 'Test Collection';
let wrapper, router;

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
                plugins: [i18n, router]
            },
            props: {
                objectType: 'Folder',
                filterParameters: {}
            }
        });

        await router.push('/record/1234');
        wrapper.vm.$router.currentRoute.value.query.anywhere = '';
    });

    afterEach(() => router = null);

    it("updates the url when search results change", async() => {
        wrapper.find('input').setValue(query);
        let btn = wrapper.find('button');
        await btn.trigger('click');
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.anywhere).toEqual(encodeURIComponent(query));
    });

    it("clears search results", async() => {
        wrapper.find('input').setValue(query);
        let btn = wrapper.find('button');
        await btn.trigger('click');
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.anywhere).toEqual(encodeURIComponent(query));

        let clearLink = wrapper.find('a#clear-results');
        await clearLink.trigger('click');
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.anywhere).not.toBeDefined();
    });

    it("start over button clears keyword and facets", async() => {
        await router.push('/record/1234?anywhere=test&subject=topic');
        await flushPromises();

        expect(wrapper.vm.$router.currentRoute.value.query.anywhere).toEqual("test");
        expect(wrapper.vm.$router.currentRoute.value.query.subject).toEqual("topic");

        let clearLink = wrapper.find('a#clear-results');
        expect(clearLink.classes()).not.toContain('disabled');

        await clearLink.trigger('click');
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.anywhere).not.toBeDefined();
        expect(wrapper.vm.$router.currentRoute.value.query.subject).not.toBeDefined();
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
                plugins: [i18n]
            },
            props: { filterParameters: {} }
        });
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
                plugins: [i18n]
            },
            props: {
                objectType: 'Folder',
                filterParameters: {}
            }
        });

        expect(wrapper.vm.search_query).toEqual('Test Folder');
    });

    it("clear all facets button not displayed when no facets selected", async() => {
        await router.push('/record/1234?anywhere=test');
        await flushPromises();
        let clearFacetsButton = wrapper.find('.clear-all-facets');

        expect(clearFacetsButton.exists()).toBe(false);
        expect(wrapper.vm.$router.currentRoute.value.query.anywhere).toEqual(encodeURIComponent('test'));
        expect(wrapper.vm.$router.currentRoute.value.query.subject).not.toBeDefined();
    });

    it("displays and uses clear all facets button", async() => {
        await router.push('/record/1234?anywhere=test&subject=topic');
        await flushPromises();
        let clearFacetsButton = wrapper.find('.clear-all-facets');
        expect(clearFacetsButton.exists()).toBe(true);
        expect(wrapper.vm.$router.currentRoute.value.query.anywhere).toEqual(encodeURIComponent('test'));
        expect(wrapper.vm.$router.currentRoute.value.query.subject).toEqual(encodeURIComponent('topic'));

        await clearFacetsButton.trigger('click');
        await flushPromises();

        clearFacetsButton = wrapper.find('.clear-all-facets');
        expect(clearFacetsButton.exists()).toBe(false);
        expect(wrapper.vm.$router.currentRoute.value.query.anywhere).toEqual(encodeURIComponent('test'));
        expect(wrapper.vm.$router.currentRoute.value.query.subject).not.toBeDefined();
    });
});