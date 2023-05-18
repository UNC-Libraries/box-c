import { shallowMount } from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import notFound from '@/components/error_pages/notFound.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from "vue-i18n";
import translations from "@/translations";
import store from '@/store';


let wrapper, router;

describe('notAvailable.vue', () => {
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
                plugins: [router, store, i18n]
            }
        });
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
        const $store = {
            state: {
                isLoggedIn: true,
                username: 'testUser'
            }
        }
        const wrapper = shallowMount(notFound, {
            global: {
                plugins: [store, i18n],
                mocks: {
                    $store
                }
            }
        })

        const links = wrapper.findAll('a');
        expect(links.length).toEqual(2);
        expect(links[0].text()).toEqual('report');
        expect(links[1].text()).toEqual('Contact Wilson Library for access information');
    });
});