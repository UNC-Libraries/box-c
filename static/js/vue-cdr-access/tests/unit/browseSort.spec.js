import { createLocalVue, shallowMount } from '@vue/test-utils';
import VueRouter from 'vue-router';
import browseSort from '@/components/browseSort.vue';

const localVue = createLocalVue();
localVue.use(VueRouter);
const router = new VueRouter({
    routes: [
        {
            path: '/record/uuid1234',
            name: 'displayRecords'
        },
        {
            path: '/search/:uuid?',
            name: 'searchRecords'
        }
    ]
});
let wrapper;
let wrapper_search;

describe('browseSort.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(browseSort, {
            localVue,
            router,
            propsData: {
                browseType: 'display'
            }
        });

        wrapper_search = shallowMount(browseSort, {
            localVue,
            router,
            propsData: {
                browseType: 'search'
            }
        });
    });

    it("updates the url when the dropdown changes for browsing", () => {
        wrapper.findAll('option').at(2).setSelected();
        expect(wrapper.vm.$router.currentRoute.query.sort).toEqual('title,reverse');
        expect(wrapper.vm.sort_order).toEqual('title,reverse');
    });

    it("updates the url when the dropdown changes for a search", () => {
        wrapper_search.findAll('option').at(2).setSelected();
        expect(wrapper_search.vm.$router.currentRoute.query.sort).toEqual('title,reverse');
        expect(wrapper_search.vm.sort_order).toEqual('title,reverse');
    });

    it("maintains the base path when changing search pages", async () => {
        wrapper_search.vm.$router.push('/search/d77fd8c9-744b-42ab-8e20-5ad9bdf8194e?collection_name=testCollection&sort=title,normal');

        // Change sort types
        wrapper_search.findAll('option').at(2).setSelected();
        expect(wrapper_search.vm.sort_order).toEqual('title,reverse');
        expect(wrapper_search.vm.$route.path).toEqual('/search/d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
    });

    afterEach(() => {
        wrapper.setData({ sort_order: '' });
        wrapper_search.setData({ sort_order: '' });
    });
});
