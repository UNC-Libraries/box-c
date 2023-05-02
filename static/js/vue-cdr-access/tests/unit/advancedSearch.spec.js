import { shallowMount, RouterLinkStub } from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import advancedSearch from '@/components/advancedSearch.vue';
import displayWrapper from "@/components/displayWrapper.vue";
import {createI18n} from "vue-i18n";
import translations from "@/translations";
import store from '@/store';
import moxios from "moxios";
import { $gtag } from '../fixtures/testHelpers';

let wrapper, router;

describe('advancedSearch.vue', () => {
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


    it("loads the advanced search form", () => {
        wrapper.find('form');
        expect(wrapper.find('h2').text()).toBe('Advanced Search');
        expect(wrapper.find('form').exists()).toBe(true);
    });

    it("loads the list of collections", (done) => {
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
        moxios.stubRequest('/advancedSearch/collections', {
            status: 200,
            response: JSON.stringify(collections)
        });
        wrapper.vm.getCollections();

        moxios.wait(() => {
            expect(wrapper.vm.collections).toEqual(collections);
            done();
        });
    });

    it("loads the list of available file formats", (done) => {
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

        moxios.stubRequest('/advancedSearch/formats', {
            status: 200,
            response: JSON.stringify(formats)
        });
        wrapper.vm.getFormats();

        moxios.wait(() => {
            expect(wrapper.vm.formats).toEqual(formats);
            done();
        });
    });
});