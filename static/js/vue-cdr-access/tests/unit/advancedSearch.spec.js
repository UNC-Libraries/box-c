import {shallowMount, RouterLinkStub, flushPromises} from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import {createTestingPinia} from '@pinia/testing';
import { useAccessStore } from '@/stores/access';
import advancedSearch from '@/components/advancedSearch.vue';
import displayWrapper from "@/components/displayWrapper.vue";
import {createI18n} from "vue-i18n";
import translations from "@/translations";

let wrapper, router, store;

describe('advancedSearch.vue', () => {
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
                    path: '/advancedSearch',
                    name: 'advancedSearch',
                    component: advancedSearch
                },
                { // Add route to avoid test warnings
                    path: '/record/:uuid',
                    name: 'displayRecords',
                    component: displayWrapper
                }
            ]
        });
        wrapper = shallowMount(advancedSearch, {
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
        fetchMock.disableMocks();
    });


    it("loads the advanced search form", () => {
        wrapper.find('form');
        expect(wrapper.find('h2').text()).toBe('Advanced Search');
        expect(wrapper.find('form').exists()).toBe(true);
    });

    it("loads the list of collections", async () => {
        const collections = [{
            "objectPath":[
                {"pid":"collections","name":"Content Collections Root", "container":true},
                {"pid":"353ee09f-a4ed-461e-a436-18a1bee77b01", "name":"testAdminUnit","container":true},
                {"pid":"fc77a9be-b49d-4f4e-b656-1644c9e964fc","name":"testCollection","container":true}
            ],
            "ancestorPath":[
                {"id":"collections","title":"collections"},
                {"id":"353ee09f-a4ed-461e-a436-18a1bee77b01","title":"353ee09f-a4ed-461e-a436-18a1bee77b01"}
            ],
            "id":"fc77a9be-b49d-4f4e-b656-1644c9e964fc", "title":"testCollection"
        }];

        fetchMock.mockResponseOnce(JSON.stringify(collections));
        await wrapper.vm.getCollections();
        await flushPromises();

        expect(wrapper.vm.collections).toEqual(collections);
    });

    it("loads the list of available file formats", async () => {
        const formats = [
            "Archive File",
            "Audio",
            "Database",
            "Disk Image",
            "Email",
            "Image",
            "Software",
            "Spreadsheet",
            "Text",
            "Unknown",
            "Video"
        ];

        fetchMock.mockResponseOnce(JSON.stringify(formats));
        await wrapper.vm.getFormats();

        await flushPromises();

        expect(wrapper.vm.formats).toEqual(formats);
    });
});