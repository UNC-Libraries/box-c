import {shallowMount, RouterLinkStub, flushPromises} from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import {createTestingPinia} from '@pinia/testing';
import { useAccessStore } from '@/stores/access';
import frontPage from '@/components/frontPage.vue';
import displayWrapper from "@/components/displayWrapper.vue";
import {createI18n} from "vue-i18n";
import translations from "@/translations";

let wrapper, router, store;

describe('frontPage.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        fetchMock.enableMocks();
        fetchMock.resetMocks();

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

    afterEach(() => {
        store.$reset();
        fetchMock.disableMocks();
    });

    it("loads the frontPage", () => {
        wrapper.find('frontPage');
        expect(wrapper.find('main').exists()).toBe(true);
    });

    it("loads the collectionStats", async () => {
        const collectionStats = {
            "formatCounts":[
                {"image":"386710"},
                {"audio":"5027"},
                {"video":"21444"},
                {"text":"46936"}
            ]
        };

        fetchMock.mockResponseOnce(JSON.stringify(collectionStats));
        wrapper.vm.getCollectionStats();

        await flushPromises();

        expect(wrapper.vm.collectionStats).toEqual(collectionStats);
    });
});
