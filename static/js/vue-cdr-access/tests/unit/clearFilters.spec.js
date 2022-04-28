import { flushPromises, mount } from '@vue/test-utils';
import { createI18n } from 'vue-i18n';
import  { createRouter, createWebHistory } from 'vue-router';
import store from '@/store';
import displayWrapper from "@/components/displayWrapper.vue";
import clearFilters from "@/components/clearFilters.vue";
import translations from "@/translations";

let wrapper, router;

describe('clearFilters.vue', () => {
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
        wrapper = mount(clearFilters, {
            global: {
                plugins: [i18n, store, router]
            },
            props: {
                filterParameters: {}
            }
        });

        await router.push('/record/1234');
        wrapper.vm.$router.currentRoute.value.query.anywhere = '';
    });

    afterEach(() => router = null);

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

    it("clears search results", async() => {
        await router.push(`/record/1234?anywhere=TestCollection`);
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.anywhere).toEqual('TestCollection');

        let clearLink = wrapper.find('a#clear-results');
        await clearLink.trigger('click');
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.anywhere).not.toBeDefined();
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