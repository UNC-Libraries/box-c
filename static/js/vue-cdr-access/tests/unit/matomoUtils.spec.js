import {mount, RouterLinkStub} from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import { createTestingPinia } from '@pinia/testing';
import { useAccessStore } from '@/stores/access';
import App from '@/App.vue';
import matomoUtils from "../../src/mixins/matomoUtils";
import displayWrapper from '@/components/displayWrapper.vue';
import { createI18n } from "vue-i18n";
import translations from "@/translations";


let wrapper, store;
describe('matomoUtils', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    let router = createRouter({
        history: createWebHistory(process.env.BASE_URL),
        routes: [
            {
                path: '/record/:uuid',
                name: 'displayRecords',
                component: displayWrapper
            }
        ]
    });

    const matomoSetup = jest.spyOn(matomoUtils.methods, 'matomoSetup');

    beforeEach(() => {
        const div = document.createElement('div');
        div.id = 'root'
        div.appendChild(document.createElement('script'));
        document.body.appendChild(div);

        jest.resetAllMocks();
    });

    afterEach(() => {
        store.$reset();
    })

    it("loads the matamo script", () => {
        wrapper = mount(App, {
            attachTo: '#root',
            global: {
                plugins: [router, i18n, createTestingPinia({
                    stubActions: false
                })],
                stubs: {
                    RouterLink: RouterLinkStub
                }
            }
        });
        store = useAccessStore();
        expect(matomoSetup).toHaveBeenCalled();
    });
});