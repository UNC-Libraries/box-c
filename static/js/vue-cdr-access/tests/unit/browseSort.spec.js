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
            path: '/search/?anywhere=',
            name: 'searchRecords'
        }
    ]
});

describe('browseSort.vue', () => {
    it("updates the url when the dropdown changes for browsing", () => {
        let wrapper = shallowMount(browseSort, {
            localVue,
            router,
            propsData: {
                browseType: 'display'
            }
        });
        wrapper.findAll('option').at(2).setSelected();
        expect(wrapper.vm.$router.currentRoute.query.sort).toEqual('title,reverse');
    });

    it("updates the url when the dropdown changes for a search", () => {
        let wrapper = shallowMount(browseSort, {
            localVue,
            router,
            propsData: {
                browseType: 'search'
            }
        });
        wrapper.findAll('option').at(2).setSelected();
        expect(wrapper.vm.$router.currentRoute.query.sort).toEqual('title,reverse');
    });
});
