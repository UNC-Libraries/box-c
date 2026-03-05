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

    it("renders all four section headings", () => {
        const headings = wrapper.findAll('h3').map(h => h.text());
        expect(headings).toContain('Related Wilson Special Collection Library Collections');
        expect(headings).toContain('What are my responsibilities when using the digital files available through this website?');
        expect(headings).toContain('Policies Governing Use');
        expect(headings).toContain('How We Preserve Digital Materials');
        expect(headings).toContain('How can I get more information or assistance?');
    });

    it("renders the related collections links", () => {
        const links = wrapper.findAll('a').map(a => a.attributes('href'));
        expect(links).toContain('https://library.unc.edu/wilson/search-the-special-collections/');
        expect(links).toContain('https://library.unc.edu/special-collection/');
        expect(links).toContain('https://docsouth.unc.edu/docsouthdata/');
        expect(links).toContain('https://archive-it.org/collections/3491');
    });

    it("renders the more info section links", () => {
        const links = wrapper.findAll('a').map(a => a.attributes('href'));
        expect(links).toContain('https://library.unc.edu/wilson/');
        expect(links).toContain('mailto:wilsonlibrary@unc.edu');
    });

    it("sends a pageView analytics event with 'About' on mount", () => {
        expect(window._mtm).toBeDefined();
        const event = window._mtm.find(e => e.event === 'pageViewEvent' && e.name === 'About');
        expect(event).toBeDefined();
    });

    it("renders the repository description text", () => {
        expect(wrapper.text()).toContain("University of North Carolina at Chapel Hill Library\u2019s Digital Collections Repository");
    });

    it("renders the responsibilities section text", () => {
        expect(wrapper.text()).toContain("Wilson Special Collections Library's Policies Governing Use");
    });

    it("renders the preservation section text", () => {
        expect(wrapper.text()).toContain("preserve the materials effectively");
    });

    it("renders the usage policies text", () => {
        expect(wrapper.text()).toContain("Researchers agree to abide by restrictions placed on the library\u2019s collections");
    });

    it("renders the contact us section", () => {
        expect(wrapper.text()).toContain("You can also contact us:");
        expect(wrapper.text()).toContain("You can visit our website:");
    });
});