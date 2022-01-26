import { shallowMount, flushPromises } from '@vue/test-utils';
import  { createRouter, createWebHistory } from 'vue-router';
import browseSearch from '@/components/browseSearch.vue';
import displayWrapper from "@/components/displayWrapper";

const query = 'Test Collection';
let wrapper, router;

describe('browseSearch.vue', () => {
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
        wrapper = shallowMount(browseSearch, {
            global: {
                plugins: [router]
            },
            props: {
                objectType: 'Folder'
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

        let clearLink = wrapper.find('a.clear-results');
        await clearLink.trigger('click');
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.anywhere).toEqual(encodeURIComponent(''));
    });

    it("sets placeholder text from the object type", () => {
        expect(wrapper.find('input').attributes('placeholder')).toBe('Search within this folder');
    });

    it("sets default placeholder text if no object type is given", () => {
        const $route = {
            path: '/record/73bc003c-9603-4cd9-8a65-93a22520ef6a',
            name: 'displayRecords',
            query: { anywhere: encodeURIComponent('Test Folder') }
        };

        wrapper = shallowMount(browseSearch, {
            global: {
                mocks: {
                    $route
                }
            }
        });
        expect(wrapper.find('input').attributes('placeholder')).toBe('Search within this object');
    });

    it("can set the search query value from the url", () => {
        const $route = {
            path: '/record/1234',
            name: 'displayRecords',
            query: { anywhere: encodeURIComponent('Test Folder') }
        };

        wrapper = shallowMount(browseSearch, {
            global: {
                mocks: {
                    $route
                }
            },
            props: {
                objectType: 'Folder'
            }
        });

        expect(wrapper.vm.search_query).toEqual('Test Folder');
    });
});