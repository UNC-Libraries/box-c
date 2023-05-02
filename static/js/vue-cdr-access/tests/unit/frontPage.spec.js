import {shallowMount, RouterLinkStub} from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import frontPage from '@/components/frontPage.vue';
import displayWrapper from "@/components/displayWrapper.vue";
import {createI18n} from "vue-i18n";
import translations from "@/translations";
import store from '@/store';
import moxios from "moxios";
import { $gtag } from '../fixtures/testHelpers';

let wrapper, router;

describe('frontPage.vue', () => {
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
                    path: '/',
                    name: 'frontPage',
                    component: frontPage
                },
                { // Add route to avoid test warnings
                    path: '/record/:uuid',
                    name: 'displayRecords',
                    component: displayWrapper
                }
            ]
        });
        wrapper = shallowMount(frontPage, {
            global: {
                plugins: [i18n, router, store],
                mocks: { $gtag },
                stubs: {
                    RouterLink: RouterLinkStub
                }
            }
        });
    });

    afterEach(function () {
        moxios.uninstall();
    });

    it("loads the frontPage", () => {
        wrapper.find('frontPage');
        expect(wrapper.find('main').exists()).toBe(true);
    });

    it("loads the collectionStats", (done) => {
        const collectionStats = {
            "formatCounts":[
                {"image":"386710"},
                {"audio":"5027"},
                {"video":"21444"},
                {"text":"46936"}
            ]
        };
        moxios.stubRequest('/collectionStats', {
            status: 200,
            response: JSON.stringify(collectionStats)
        });
        wrapper.vm.getCollectionStats();

        moxios.wait(() => {
            expect(wrapper.vm.collectionStats).toEqual(collectionStats);
            done();
        });
    });
});

