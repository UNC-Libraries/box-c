import { shallowMount } from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import notAvailable from '@/components/error_pages/notAvailable.vue';
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

        wrapper = shallowMount(notAvailable, {
            global: {
                plugins: [router, store, i18n]
            }
        });
    });

    it('displays an error message', () => {
        expect(wrapper.find('p').text()).toEqual('An unexpected error occurred. We apologize for the inconvenience.');
    });

});