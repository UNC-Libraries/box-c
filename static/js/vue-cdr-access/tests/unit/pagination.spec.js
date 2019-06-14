import { createLocalVue, shallowMount } from '@vue/test-utils';
import VueRouter from 'vue-router';
import range from 'lodash.range';
import pagination from '@/components/pagination.vue';

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
let wrapper;

describe('pagination.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(pagination, {
            localVue,
            router,
            propsData: {
                numberOfRecords: 199,
                pageBaseUrl: 'https://dcr.lib.unc.edu'
            }
        });

        wrapper.setData({
            pageLimit: 5,
            pageOffset: 2,
            perPage: 20,
            startRecord: 1,
            totalPageCount: 1
        });
    });

    it("calculates the number of pages", () => {
        expect(wrapper.vm.totalPageCount).toEqual(10);
    });

    it("displays a list of pages if the user is on the first page and there are <= pages than the page limit", () => {
        wrapper.setProps({ numberOfRecords: 24 });
        expect(wrapper.findAll('.page-number').length).toEqual(2);
    });

    it("displays a list of pages if the user is on the first page and there are more pages than the page limit", () => {
        expect(wrapper.findAll('.page-number').length).toEqual(6);
    });

    it("updates the page when a page is selected", () => {
        wrapper.findAll('.page-number').at(1).trigger('click');
        expect(wrapper.vm.$router.currentRoute.query.page).toEqual(2);
    });

    it("updates the start record when a page is selected", () => {
        wrapper.findAll('.page-number').at(1).trigger('click');
        expect(wrapper.vm.$router.currentRoute.query.start).toEqual(20);
    });
});