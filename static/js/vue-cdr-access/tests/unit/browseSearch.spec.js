import { createLocalVue, shallowMount } from '@vue/test-utils';
import VueRouter from 'vue-router';
import browseSearch from '@/components/browseSearch.vue';

const localVue = createLocalVue();
localVue.use(VueRouter);
const router = new VueRouter({
    routes: [
        {
            path: '/record/uuid1234',
            name: 'browseDisplay'
        }
    ]
});
const query = 'Test Collection';
let wrapper;

describe('browseSearch.vue', () => {
    beforeEach(() => {
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
    });

    it("updates the url when search results change", () => {
        expect(wrapper.vm.$router.currentRoute.query.anywhere).toEqual(encodeURIComponent(query));
    });
});