import { shallowMount } from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import {createTestingPinia} from '@pinia/testing';
import { useAccessStore } from '@/stores/access';
import notFound from '@/components/error_pages/notFound.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from "vue-i18n";
import translations from "@/translations";


let wrapper, router, store;

describe('notFound.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        router = createRouter({
            history: createWebHistory(),
            routes: [
                {
                    path: '/record/:uuid/',
                    name: 'displayRecords',
                    component: displayWrapper
                }
            ]
        });

        wrapper = shallowMount(notFound, {
            global: {
                plugins: [router, i18n, createTestingPinia({
                    stubActions: false
                })]
            }
        });
        store = useAccessStore();
    });

    afterEach(() => {
        store.$reset();
    });

    it('displays the DCR header and a "not found" message by default', () => {
        expect(wrapper.findComponent({ name: 'headerSmall' }).exists()).toBe(true);
        expect(wrapper.find('p').text()).toEqual('The record you attempted to access either does not exist or you do not have sufficient rights to view it.');
    });

    it('displays error reporting, login and contact links if a user is not logged in', () => {
        const links = wrapper.findAll('a');
        expect(links.length).toEqual(3);
        expect(links[0].text()).toEqual('report');
        expect(links[1].text()).toEqual('logging in (UNC Onyen)');
        expect(links[2].text()).toEqual('Contact Wilson Library for access information');
    });

    it('does not display the DCR header when set to "false"', async () => {
        await wrapper.setProps({
            displayHeader: false
        })
        expect(wrapper.findComponent({ name: 'headerSmall' }).exists()).toBe(false);
    });

    it('does not display a login link if a user is already logged in"', async () => {
        const wrapper = shallowMount(notFound, {
            global: {
                plugins: [store, i18n, createTestingPinia({
                    initialState: {
                        access: {
                            isLoggedIn: true,
                            username: 'testUser'
                        }
                    },
                    stubActions: false
                })
                ]
            }
        });

        const links = wrapper.findAll('a');
        expect(links.length).toEqual(2);
        expect(links[0].text()).toEqual('report');
        expect(links[1].text()).toEqual('Contact Wilson Library for access information');
    });

    it('report and contact links point to the correct URLs', () => {
        const links = wrapper.findAll('a');
        expect(links[0].attributes('href')).toEqual('https://library.unc.edu/contact-us/');
        expect(links[2].attributes('href')).toEqual('https://library.unc.edu/contact-us/');
    });

    it('login link points to the correct Shibboleth URL', () => {
        const current_page = window.location;
        const expectedLoginUrl = `https://${current_page.host}/Shibboleth.sso/Login?target=${encodeURIComponent(current_page)}`;
        const links = wrapper.findAll('a');
        const loginLink = links.find(l => l.text() === 'logging in (UNC Onyen)');
        expect(loginLink.attributes('href')).toEqual(expectedLoginUrl);
    });

    it('displays the header by default when displayHeader prop is not set', () => {
        expect(wrapper.findComponent({ name: 'headerSmall' }).exists()).toBe(true);
    });

    it('displays all three links when user is not logged in', () => {
        const links = wrapper.findAll('a');
        const linkTexts = links.map(l => l.text());
        expect(linkTexts).toContain('report');
        expect(linkTexts).toContain('logging in (UNC Onyen)');
        expect(linkTexts).toContain('Contact Wilson Library for access information');
    });

    it('hides the header when displayHeader prop is passed as false at mount time', () => {
        const localWrapper = shallowMount(notFound, {
            props: { displayHeader: false },
            global: {
                plugins: [router, i18n, createTestingPinia({ stubActions: false })]
            }
        });
        expect(localWrapper.findComponent({ name: 'headerSmall' }).exists()).toBe(false);
    });

    it('shows the header when displayHeader prop is passed as true at mount time', () => {
        const localWrapper = shallowMount(notFound, {
            props: { displayHeader: true },
            global: {
                plugins: [router, i18n, createTestingPinia({ stubActions: false })]
            }
        });
        expect(localWrapper.findComponent({ name: 'headerSmall' }).exists()).toBe(true);
    });

    it('displays "return to the previous page" text', () => {
        expect(wrapper.text()).toContain('return to the previous page in your browser');
    });

    it('displays "try logging in" suggestion text when user is not logged in', () => {
        expect(wrapper.text()).toContain('try');
        expect(wrapper.text()).toContain('logging in (UNC Onyen)');
    });

    it('does not display "try logging in" suggestion text when user is logged in', async () => {
        const localWrapper = shallowMount(notFound, {
            global: {
                plugins: [router, i18n, createTestingPinia({
                    initialState: {
                        access: {
                            isLoggedIn: true,
                            username: 'testUser'
                        }
                    },
                    stubActions: false
                })]
            }
        });
        expect(localWrapper.text()).not.toContain('logging in (UNC Onyen)');
    });

    it('report and contact links have the same href', () => {
        const links = wrapper.findAll('a');
        const reportLink = links.find(l => l.text() === 'report');
        const contactLink = links.find(l => l.text() === 'Contact Wilson Library for access information');
        expect(reportLink.attributes('href')).toEqual(contactLink.attributes('href'));
    });

    it('renders three paragraphs of text', () => {
        const paragraphs = wrapper.findAll('p');
        expect(paragraphs.length).toBe(3);
    });

    it('only shows one link when logged in and displayHeader is false', async () => {
        const localWrapper = shallowMount(notFound, {
            props: { displayHeader: false },
            global: {
                plugins: [router, i18n, createTestingPinia({
                    initialState: {
                        access: {
                            isLoggedIn: true,
                            username: 'testUser'
                        }
                    },
                    stubActions: false
                })]
            }
        });
        expect(localWrapper.findComponent({ name: 'headerSmall' }).exists()).toBe(false);
        const links = localWrapper.findAll('a');
        expect(links.length).toEqual(2);
        expect(links[0].text()).toEqual('report');
        expect(links[1].text()).toEqual('Contact Wilson Library for access information');
    });
});