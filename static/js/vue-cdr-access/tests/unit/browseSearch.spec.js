import { createLocalVue, shallowMount } from '@vue/test-utils';
import VueRouter from 'vue-router';
import browseSearch from '@/components/browseSearch.vue';

const query = 'Test Collection';
let wrapper;

describe('browseSearch.vue', () => {
    it("can set the search query value from the url", () => {
        const localVue = createLocalVue();
        const $route = {
            path: '/record/1234',
            name: 'displayRecords',
            query: { anywhere: encodeURIComponent('Test Folder') }
        };

        wrapper = shallowMount(browseSearch, {
            localVue,
            mocks: {
                $route
            },
            propsData: {
                recordId: '1234'
            }
        });

        expect(wrapper.vm.search_query).toEqual('Test Folder');
    });

    it("updates the url when search results change", () => {
        const localVue = createLocalVue();
        localVue.use(VueRouter);
        const router = new VueRouter({
            routes: [
                {
                    path: '/record/uuid1234',
                    name: 'displayRecords'
                }
            ]
        });

        wrapper = shallowMount(browseSearch, {
            localVue,
            router,
            propsData: {
                recordId: '1234'
            }
        });

        wrapper.find('input').setValue(query);
        let btn = wrapper.find('button');
        btn.trigger('click');

        expect(wrapper.vm.$router.currentRoute.query.anywhere).toEqual(encodeURIComponent(query));
    });
});