import { shallowMount, flushPromises } from '@vue/test-utils';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { createRouter, createWebHistory } from 'vue-router';
import {createTestingPinia} from '@pinia/testing';
import { useAccessStore } from '@/stores/access';
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

    it("calculates the pages to display with fewer than page limit number of pages", async () => {
        await wrapper.setProps({ numberOfRecords: 70 });
        await changeToPage(1);
        expect(wrapper.vm.currentPageList).toEqual([2, 3]);
        await changeToPage(2);
        expect(wrapper.vm.currentPageList).toEqual([2, 3]);
        await changeToPage(3);
        expect(wrapper.vm.currentPageList).toEqual([2, 3]);
        await changeToPage(4);
        expect(wrapper.vm.currentPageList).toEqual([2, 3]);
    });

    it("calculates the correct pages to display with large number of pages", async () => {
        await wrapper.setProps({ numberOfRecords: 1000 });
        await changeToPage(1);
        expect(wrapper.vm.currentPageList).toEqual([2, 3, 4, 5, 6]);
        await changeToPage(2);
        expect(wrapper.vm.currentPageList).toEqual([2, 3, 4, 5, 6]);
        await changeToPage(3);
        expect(wrapper.vm.currentPageList).toEqual([2, 3, 4, 5, 6]);
        await changeToPage(4);
        expect(wrapper.vm.currentPageList).toEqual([2, 3, 4, 5, 6]);
        await changeToPage(5);
        expect(wrapper.vm.currentPageList).toEqual([3, 4, 5, 6, 7]);
        await changeToPage(25);
        expect(wrapper.vm.currentPageList).toEqual([23, 24, 25, 26, 27]);
        await changeToPage(45);
        expect(wrapper.vm.currentPageList).toEqual([43, 44, 45, 46, 47]);
        await changeToPage(46);
        expect(wrapper.vm.currentPageList).toEqual([44, 45, 46, 47, 48]);
        await changeToPage(47);
        expect(wrapper.vm.currentPageList).toEqual([45, 46, 47, 48, 49]);
        await changeToPage(48);
        expect(wrapper.vm.currentPageList).toEqual([45, 46, 47, 48, 49]);
        await changeToPage(49);
        expect(wrapper.vm.currentPageList).toEqual([45, 46, 47, 48, 49]);
        await changeToPage(50);
        expect(wrapper.vm.currentPageList).toEqual([45, 46, 47, 48, 49]);
    });

    it("calculates the correct pages to display with page limit number of pages plus first and last page", async () => {
        await wrapper.setProps({ numberOfRecords: 140 });
        await changeToPage(1);
        expect(wrapper.vm.currentPageList).toEqual([2, 3, 4, 5, 6]);
        await changeToPage(2);
        expect(wrapper.vm.currentPageList).toEqual([2, 3, 4, 5, 6]);
        await changeToPage(3);
        expect(wrapper.vm.currentPageList).toEqual([2, 3, 4, 5, 6]);
        await changeToPage(4);
        expect(wrapper.vm.currentPageList).toEqual([2, 3, 4, 5, 6]);
        await changeToPage(5);
        expect(wrapper.vm.currentPageList).toEqual([2, 3, 4, 5, 6]);
        await changeToPage(7);
        expect(wrapper.vm.currentPageList).toEqual([2, 3, 4, 5, 6]);
    });
    
    async function changeToPage(page) {
        await router.push('/record/1234?start=' + (page - 1) * 20);
    }

    it("displays a list of pages if the user is on the first page and there are <= pages than the page limit", async () => {
        await wrapper.setProps({ numberOfRecords: 24 });
        expect(wrapper.findAll('.pagination-link').length).toEqual(2);
    });

    it("displays a list of pages if the user is on the first page and there are more pages than the page limit", () => {
        expect(wrapper.findAll('.pagination-link').length).toEqual(7);
    });

    it("updates the page when a page is selected", async () => {
        await wrapper.findAll('.pagination-link')[3].trigger('click');
        await flushPromises();
        expect(wrapper.vm.currentPage).toEqual(4);
        expect(wrapper.vm.currentPageList).toEqual([2, 3, 4, 5, 6]);
    });

    it("updates the start record when a 'display' page is selected",  async () => {
        await wrapper.findAll('.pagination-link')[1].trigger('click');
        await flushPromises();
        expect(parseInt(wrapper.vm.$router.currentRoute.value.query.start)).toEqual(20);
    });

    it("updates the start record when a 'search' page is selected", async () => {
        await wrapper.setProps({
            browseType: 'search'
        });

        await wrapper.findAll('.pagination-link')[1].trigger('click');
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.start).toEqual('20');
    });

    it("displays a link to jump to the first page if the user in on a page beyond the pageLimit", async () => {
        await wrapper.findAll('.pagination-link')[5].trigger('click');
        await flushPromises();
        await wrapper.findAll('.pagination-link')[1].trigger('click');
        await flushPromises();

        expect(wrapper.find('#first-page-link').isVisible()).toBe(true);
    });

    it("displays a link to jump to the last page if the user is on a page that is before the pageLimit and" +
        "there are more pages than the pageLimit",  () => {
        expect(wrapper.find('#last-page-link').isVisible()).toBe(true);
    });

    it("displays links to all pages if on the page before the page limit", async () => {
        await wrapper.setProps({
            numberOfRecords: 100
        });

        await wrapper.findAll('.pagination-link')[4].trigger('click');
        await flushPromises();
        let page_links = wrapper.findAll('.pagination-link');
        expect(page_links.map((link) => link.text())).toEqual(['1', '2', '3', '4', '5']);
    });

    it("displays all links if on the second page and there are fewer pages than the page limit", async () => {
        await wrapper.setProps({
            numberOfRecords: 100
        });

        await wrapper.findAll('.pagination-link')[1].trigger('click');
        await flushPromises();
        let page_links = wrapper.findAll('.pagination-link');
        expect(page_links.map((link) => link.text())).toEqual(['1', '2', '3', '4', '5']);
    });

    it("displays a back link", async () => {
        await wrapper.findAll('.pagination-link')[2].trigger('click');
        await flushPromises();
        let prev_btn = wrapper.find('.pagination-previous');
        expect(prev_btn.classes('is-disabled')).toBe(false);

        await wrapper.findAll('.pagination-link')[0].trigger('click');
        await flushPromises();
        prev_btn = wrapper.find('.pagination-previous');
        expect(prev_btn.classes('is-disabled')).toBe(true);
    });

    it("displays a next link", async () => {
        await wrapper.setProps({
            numberOfRecords: 100
        });

        expect(wrapper.find('.pagination-next').classes('is-disabled')).toBe(false);

        await wrapper.findAll('.pagination-link')[4].trigger('click');
        await flushPromises();
        expect(wrapper.find('.pagination-next').classes('is-disabled')).toBe(true);
    });

    it("maintains the base path when changing search pages", async () => {
        // Change pages
        await changeToPage(2);
        expect(wrapper.vm.$route.path).toEqual('/record/1234');
    });

    it("if user is on the first page, then correct page is selected and previous button is disabled", async () => {
        await changeToPage(1);
        expect(wrapper.vm.currentPageList).toEqual([2, 3, 4, 5, 6]);
        expect(wrapper.find('.pagination-previous').classes('is-disabled')).toBe(true);
        expect(wrapper.find('.pagination-next').classes('is-disabled')).toBe(false);
        expect(wrapper.find('.pagination-link.is-current').text()).toEqual('1');
        expect(wrapper.find('.pagination-ellipsis-start').exists()).toBe(false);
        expect(wrapper.find('.pagination-ellipsis-end').exists()).toBe(true);
    });

    it("if user is on the second page, then correct page is selected and previous button is enabled", async () => {
        await changeToPage(2);
        expect(wrapper.vm.currentPageList).toEqual([2, 3, 4, 5, 6]);
        expect(wrapper.find('.pagination-previous').classes('is-disabled')).toBe(false);
        expect(wrapper.find('.pagination-next').classes('is-disabled')).toBe(false);
        expect(wrapper.find('.pagination-link.is-current').text()).toEqual('2');
        expect(wrapper.find('.pagination-ellipsis-start').exists()).toBe(false);
        expect(wrapper.find('.pagination-ellipsis-end').exists()).toBe(true);
    });

    it("if user is on the middle page, then correct page is selected and all ellipses are shown", async () => {
        await changeToPage(5);
        expect(wrapper.vm.currentPageList).toEqual([3, 4, 5, 6, 7]);
        expect(wrapper.find('.pagination-previous').classes('is-disabled')).toBe(false);
        expect(wrapper.find('.pagination-next').classes('is-disabled')).toBe(false);
        expect(wrapper.find('.pagination-link.is-current').text()).toEqual('5');
        expect(wrapper.find('.pagination-ellipsis-start').exists()).toBe(true);
        expect(wrapper.find('.pagination-ellipsis-end').exists()).toBe(true);
    });

    it("if user is on the last page, then correct page is selected and previous button is enabled", async () => {
        await changeToPage(10);
        expect(wrapper.vm.currentPageList).toEqual([5, 6, 7, 8, 9]);
        expect(wrapper.find('.pagination-previous').classes('is-disabled')).toBe(false);
        expect(wrapper.find('.pagination-next').classes('is-disabled')).toBe(true);
        expect(wrapper.find('.pagination-link.is-current').text()).toEqual('10');
        expect(wrapper.find('.pagination-ellipsis-start').exists()).toBe(true);
        expect(wrapper.find('.pagination-ellipsis-end').exists()).toBe(false);
    });
});