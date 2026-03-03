import {RouterLinkStub, shallowMount} from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import { createTestingPinia } from '@pinia/testing';
import { useAccessStore } from '@/stores/access';
import aboutRepository from '@/components/aboutRepository.vue';
import displayWrapper from "@/components/displayWrapper.vue";
import {createI18n} from "vue-i18n";
import translations from "@/translations";

let wrapper, router, store;

describe('aboutRepository.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        router = createRouter({
            history: createWebHistory(process.env.BASE_URL),
            routes: [
                {
                    path: '/aboutRepository',
                    name: 'aboutRepository',
                    component: aboutRepository
                },
                { // Add route to avoid test warnings
                    path: '/record/:uuid',
                    name: 'displayRecords',
                    component: displayWrapper
                }
            ]
        });
        wrapper = shallowMount(aboutRepository, {
            global: {
                plugins: [i18n, router, createTestingPinia({
                    stubActions: false
                })],
                stubs: {
                    RouterLink: RouterLinkStub
                }
            }
        });
        store = useAccessStore();
    });

    afterEach(function () {
        store.$reset();
    });

    it("loads the about repository page", () => {
        expect(wrapper.html()).toContain('about-repo');
        expect(wrapper.find('h2').text()).toBe('About this Repository');
    });

    it("renders the headerSmall component", () => {
        expect(wrapper.findComponent({ name: 'headerSmall' }).exists()).toBe(true);
    });

    it("renders the Wilson Library special collections link", () => {
        const links = wrapper.findAll('a');
        const found = links.find(l => l.attributes('href') === 'https://library.unc.edu/wilson/search-the-special-collections/');
        expect(found).toBeDefined();
    });

    it("renders the special collections scan link", () => {
        const links = wrapper.findAll('a');
        const found = links.find(l => l.attributes('href') === 'https://library.unc.edu/special-collection/');
        expect(found).toBeDefined();
    });

    it("renders the DocSouth link", () => {
        const links = wrapper.findAll('a');
        const found = links.find(l => l.attributes('href') === 'https://docsouth.unc.edu/docsouthdata/');
        expect(found).toBeDefined();
    });

    it("renders the Archive-It link", () => {
        const links = wrapper.findAll('a');
        const found = links.find(l => l.attributes('href') === 'https://archive-it.org/collections/3491');
        expect(found).toBeDefined();
    });

    it("renders the Wilson Library website link", () => {
        const links = wrapper.findAll('a');
        const found = links.find(l => l.attributes('href') === 'https://library.unc.edu/wilson/');
        expect(found).toBeDefined();
    });

    it("renders the contact email link", () => {
        const links = wrapper.findAll('a');
        const found = links.find(l => l.attributes('href') === 'mailto:wilsonlibrary@unc.edu');
        expect(found).toBeDefined();
        expect(found.text()).toBe('wilsonlibrary@unc.edu');
    });
});