import {mount, RouterLinkStub} from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import advancedSearch from '@/components/advancedSearch.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import gaUtils from "../../src/mixins/gaUtils";
import store from '@/store';
import { createI18n } from "vue-i18n";
import translations from "@/translations";
import { $gtag } from '../fixtures/testHelpers';

let wrapper;
describe('gaUtils', () => {
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

    const pageEvent = jest.spyOn(gaUtils.methods, 'pageEvent');
    const pageView = jest.spyOn(gaUtils.methods, 'pageView');

    beforeEach(() => {
        jest.resetAllMocks();
    })

    it("sends pageviews to Google Analytics", () => {
        wrapper = mount(advancedSearch, {
            global: {
                plugins: [router, store, i18n],
                mocks: { $gtag },
                stubs: {
                    RouterLink: RouterLinkStub
                }
            }
        });

        expect(pageView).toHaveBeenCalledWith("Advanced Search");
    });

    //@TODO run inside an async in a lifecycle hook that possibly? runs before the mock is added
    // Would be easier to test it didn't run in async event
   /*  it("sends events to Google Analytics", async () => {
        const $route = {
            path: '/record/1234',
            name: 'displayRecords',
            query: { rows: 10 }
        };
        wrapper = mount(displayWrapper, {
            global: {
                plugins: [store, i18n],
                mocks: { $gtag, $route },
                stubs: {
                    RouterLink: RouterLinkStub
                }
            },
            data() {
                return   {
                    container_metadata: {
                        added: "2017-12-20T13:44:46.119Z",
                        title: "Test Collection",
                        type: "Collection",
                        uri: "https://dcr.lib.unc.edu/record/73bc003c-9603-4cd9-8a65-93a22520ef6a",
                        id: "73bc003c-9603-4cd9-8a65-93a22520ef6a",
                        parentCollectionName: "AdminUnit",
                        parentCollectionId: "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                        updated: "2017-12-20T13:44:46.264Z",
                    }
                }
            }
        });
        wrapper.vm.getBriefObject();
        await wrapper.vm.$nextTick();

        expect(pageEvent).toHaveBeenCalled();
        expect(pageView).toHaveBeenCalled();
    });*/
});