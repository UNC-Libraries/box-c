import { createLocalVue, shallowMount } from '@vue/test-utils';
import '@testing-library/jest-dom'
import VueRouter from 'vue-router';
import pagination from '@/components/pagination.vue';

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

describe('pagination.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(pagination, {
            localVue,
            router,
            propsData: {
                browseType: 'display',
                numberOfRecords: 199
            }
        });

        wrapper.setData({
            pageLimit: 5,
            pageOffset: 2,
            startRecord: 1,
            totalPageCount: 1
        });
    });

    it("calculates the total number of pages", () => {
        expect(wrapper.vm.totalPageCount).toEqual(10);
    });

    it("calculates the pages to display", () => {
        expect(wrapper.vm.currentPageList).toEqual([1, 2, 3, 4, 5]);
    });

    it("displays a list of pages if the user is on the first page and there are <= pages than the page limit", async () => {
        wrapper.setProps({ numberOfRecords: 24 });
        await wrapper.vm.$nextTick();
        expect(wrapper.findAll('.page-number').length).toEqual(2);
    });

    it("displays a list of pages if the user is on the first page and there are more pages than the page limit", () => {
        expect(wrapper.findAll('.page-number').length).toEqual(6);
    });

    it("updates the page when a page is selected", () => {
        wrapper.findAll('.page-number').at(3).trigger('click');
        expect(wrapper.vm.currentPage).toEqual(4);
        expect(wrapper.vm.currentPageList).toEqual([2, 3, 4, 5, 6]);
    });

    it("updates the start record when a 'display' page is selected", () => {
        wrapper.findAll('.page-number').at(1).trigger('click');
        expect(wrapper.vm.$router.currentRoute.query.start).toEqual(20);
    });

    it("updates the start record when a 'search' page is selected", async () => {
        wrapper.setProps({
            browseType: 'search'
        });
        await wrapper.vm.$nextTick();
        wrapper.findAll('.page-number').at(1).trigger('click');
        expect(wrapper.vm.$router.currentRoute.query['a.setStartRow']).toEqual('20');
    });

    it("displays a link to jump to the first page if the user in on a page beyond the pageLimit", async () => {
        wrapper.findAll('.page-number').at(5).trigger('click');

        await wrapper.vm.$nextTick();
        wrapper.findAll('.page-number').at(1).trigger('click');

        expect(wrapper.find('#first-page-link').element).toBeVisible();
    });

    it("displays a link to jump to the last page if the user in on a page that is before the pageLimit and" +
        "there are more pages than the pageLimit", () => {
        expect(wrapper.find('#last-page-link').element).toBeVisible();
    });

    it("does not display a link to jump to the first page if the user in on a page before the pageLimit and" +
        "there are less than or eqaul number of pages than the pageLimit", async () => {
        wrapper.setProps({
            numberOfRecords: 100
        });
        await wrapper.vm.$nextTick();
        wrapper.findAll('.page-number').at(4).trigger('click');
        expect(wrapper.find('#first-page-link').exists()).toBe(false);
    });

    it("does not display a link to jump to the last page if the user in on a page before the pageLimit and" +
        "there are less than or eqaul number of pages than the pageLimit", async () => {
        wrapper.setProps({
            numberOfRecords: 100
        });
        await wrapper.vm.$nextTick();
        wrapper.findAll('.page-number').at(1).trigger('click');
        expect(wrapper.find('#last-page-link').exists()).toBe(false);
    });

    it("displays a back link", async () => {
        expect(wrapper.find('.start').classes('back-next')).toBe(true);

        wrapper.findAll('.page-number').at(0).trigger('click');

        await wrapper.vm.$nextTick();
        expect(wrapper.find('.start').classes('back-next')).toBe(false);
        expect(wrapper.find('.start').classes('no-link')).toBe(true);
    });

    it("displays a next link", async () => {
        wrapper.setProps({
            numberOfRecords: 100
        });

        await wrapper.vm.$nextTick();
        expect(wrapper.find('.end').classes('back-next')).toBe(true);

        wrapper.findAll('.page-number').at(4).trigger('click');

        await wrapper.vm.$nextTick();
        expect(wrapper.find('.end').classes('back-next')).toBe(false);
        expect(wrapper.find('.end').classes('no-link')).toBe(true);
    });

    it("maintains the base path when changing search pages", async () => {
        wrapper.setProps({
            browseType: 'search'
        });

        await wrapper.vm.$router.push('/search/d77fd8c9-744b-42ab-8e20-5ad9bdf8194e?collection_name=testCollection&page=1');

        // Change pages
        wrapper.vm.pageUrl(2);
        expect(wrapper.vm.$route.path).toEqual('/search/d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
    });
});