import { mount } from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import aboutRepository from '@/components/aboutRepository.vue';
import {createI18n} from "vue-i18n";
import translations from "@/translations";
import moxios from "moxios";

let wrapper, router;

describe('aboutRepository.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        moxios.install();

        router = createRouter({
            history: createWebHistory(process.env.BASE_URL),
            routes: [
                {
                    path: '/aboutRepository',
                    name: 'aboutRepository',
                    component: aboutRepository
                }
            ]
        });
        wrapper = mount(aboutRepository, {
            global: {
                plugins: [i18n, router]
            }
        });
    });

    afterEach(function () {
        moxios.uninstall();
    });

    it("loads the about repository page", () => {
        expect(wrapper.html()).toContain('about-repo');
        expect(wrapper.find('h2').text()).toBe('About this Repository');
    });
});