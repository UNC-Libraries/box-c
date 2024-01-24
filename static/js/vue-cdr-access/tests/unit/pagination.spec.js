import { shallowMount, flushPromises } from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import {createTestingPinia} from '@pinia/testing';
import { useAccessStore } from '@/stores/access';
import '@testing-library/jest-dom';
import pagination from '@/components/pagination.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import searchWrapper from '@/components/searchWrapper.vue';
import {createI18n} from "vue-i18n";
import translations from "@/translations";

let router, wrapper, store;

describe('pagination.vue', () => {
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
                },
                {
                    path: '/search/:uuid?',
                    name: 'searchRecords',
                    component: searchWrapper
                }
            ]
        });
        wrapper = shallowMount(pagination, {
            global: {
                plugins: [router, i18n, createTestingPinia({
                    stubActions: false
                })]
            },
            props: {
                browseType: 'display',
                numberOfRecords: 199
            },
            data() {
                return {
                    pageLimit: 5,
                    pageOffset: 2,
                    startRecord: 1,
                    totalPageCount: 1
                }
            }
        });
        store = useAccessStore();

        await router.push('/record/1234');
    });

    afterEach(() => {
        store.$reset();
        wrapper = null;
        router = null;
    });

    it("calculates the total number of pages", () => {
        expect(wrapper.vm.totalPageCount).toEqual(10);
    });

    it("calculates the pages to display", () => {
        expect(wrapper.vm.currentPageList).toEqual([1, 2, 3, 4, 5]);
    });

    it("displays a list of pages if the user is on the first page and there are <= pages than the page limit", async () => {
        await wrapper.setProps({ numberOfRecords: 24 });
        expect(wrapper.findAll('.page-number').length).toEqual(2);
    });

    it("displays a list of pages if the user is on the first page and there are more pages than the page limit", () => {
        expect(wrapper.findAll('.page-number').length).toEqual(6);
    });

    it("updates the page when a page is selected", async () => {
        await wrapper.findAll('.page-number')[3].trigger('click');
        await flushPromises();
        expect(wrapper.vm.currentPage).toEqual(4);
        expect(wrapper.vm.currentPageList).toEqual([2, 3, 4, 5, 6]);
    });

    it("updates the start record when a 'display' page is selected",  async () => {
        await wrapper.findAll('.page-number')[1].trigger('click');
        await flushPromises();
        expect(parseInt(wrapper.vm.$router.currentRoute.value.query.start)).toEqual(20);
    });

    it("updates the start record when a 'search' page is selected", async () => {
        await wrapper.setProps({
            browseType: 'search'
        });

        await wrapper.findAll('.page-number')[1].trigger('click');
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.start).toEqual('20');
    });

    it("displays a link to jump to the first page if the user in on a page beyond the pageLimit", async () => {
        await wrapper.findAll('.page-number')[5].trigger('click');
        await flushPromises();
        await wrapper.findAll('.page-number')[1].trigger('click');
        await flushPromises();

        expect(wrapper.find('#first-page-link').isVisible()).toBe(true);
    });

    it("displays a link to jump to the last page if the user in on a page that is before the pageLimit and" +
        "there are more pages than the pageLimit",  () => {
        expect(wrapper.find('#last-page-link').isVisible()).toBe(true);
    });

    it("does not display a link to jump to the first page if the user in on a page before the pageLimit and" +
        "there are less than or eqaul number of pages than the pageLimit", async () => {
        await wrapper.setProps({
            numberOfRecords: 100
        });

        await wrapper.findAll('.page-number')[4].trigger('click');
        await flushPromises();
        expect(wrapper.find('#first-page-link').exists()).toBe(false);
    });

    it("does not display a link to jump to the last page if the user in on a page before the pageLimit and" +
        "there are less than or eqaul number of pages than the pageLimit", async () => {
        await wrapper.setProps({
            numberOfRecords: 100
        });

        await wrapper.findAll('.page-number')[1].trigger('click');
        await flushPromises();
        expect(wrapper.find('#last-page-link').exists()).toBe(false);
    });

    it("displays a back link", async () => {
        await wrapper.findAll('.page-number')[2].trigger('click');
        await flushPromises();
        let start_btn = wrapper.find('.start');
        expect(start_btn.classes('back-next')).toBe(true);
        expect(start_btn.classes('no-link')).toBe(false);

        await wrapper.findAll('.page-number')[0].trigger('click');
        await flushPromises();
        start_btn = wrapper.find('.start');
        expect(start_btn.classes('back-next')).toBe(false);
        expect(start_btn.classes('no-link')).toBe(true);
    });

    it("displays a next link", async () => {
        await wrapper.setProps({
            numberOfRecords: 100
        });

        expect(wrapper.find('.end').classes('back-next')).toBe(true);

        await wrapper.findAll('.page-number')[4].trigger('click');
        await flushPromises();
        expect(wrapper.find('.end').classes('back-next')).toBe(false);
        expect(wrapper.find('.end').classes('no-link')).toBe(true);
    });

    it("maintains the base path when changing search pages", async () => {
        // Change pages
        wrapper.vm.pageUrl(2);
        await flushPromises();
        expect(wrapper.vm.$route.path).toEqual('/record/1234');
    });
});