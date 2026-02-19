import {RouterLinkStub, shallowMount} from '@vue/test-utils';
import { describe, it, expect } from 'vitest';
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
});