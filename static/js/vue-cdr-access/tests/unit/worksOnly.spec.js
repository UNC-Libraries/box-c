import { createLocalVue, shallowMount } from '@vue/test-utils';
import VueRouter from 'vue-router';
import worksOnly from '@/components/worksOnly.vue';

const localVue = createLocalVue();
localVue.use(VueRouter);

const router = new VueRouter({
    routes: [
        {
            path: '/record/98bc503c-9603-4cd9-8a65-93a22520ef68',
            name: 'displayRecords'
        }
    ]
});
let wrapper, record, record_input;

describe('worksOnly.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(worksOnly, {
            localVue,
            router,
            propsData: {
                adminUnit: false
            }
        });

        wrapper.setData({
            works_only: false
        });
        record_input = wrapper.find('input');
    });

    it("does not display for admin unit records", () => {
        wrapper.setProps({
            adminUnit: true
        });
        expect(wrapper.find('#browse-display-type').exists()).toBe(false);
    });

    it("does displays for non admin units", () => {
        expect(wrapper.find('#browse-display-type').exists()).toBe(true);
    });

    it("updates route to only show works if button is checked for a gallery view", () => {
        wrapper.vm.$router.currentRoute.query.browse_type='gallery-display';
        wrapper.setData({ works_only: false });
        record_input.trigger('click');

        expect(wrapper.vm.$router.currentRoute.query.types).toEqual('Work');
    });

    it("updates route to only show works and folders if button is unchecked for a gallery view", () => {
        wrapper.vm.$router.currentRoute.query.browse_type='gallery-display';
        wrapper.setData({ works_only: true });
        record_input.trigger('click');

        expect(wrapper.vm.$router.currentRoute.query.types).toEqual('Work,Folder');
    });

    it("updates route to only show works if button is checked for a list view", () => {
        wrapper.vm.$router.currentRoute.query.browse_type='list-display';
        wrapper.setData({ works_only: false });
        record_input.trigger('click');

        expect(wrapper.vm.$router.currentRoute.query.types).toEqual('Work');
    });

    it("updates route to show works and folders if button is unchecked for a list view", () => {
        wrapper.vm.$router.currentRoute.query.browse_type='list-display';
        wrapper.setData({ works_only: true });
        record_input.trigger('click');

        expect(wrapper.vm.$router.currentRoute.query.types).toEqual('Work,Folder');
    });

    afterEach(() => {
        // Make sure box is unchecked
        wrapper.setData({ works_only: false });
    });
});