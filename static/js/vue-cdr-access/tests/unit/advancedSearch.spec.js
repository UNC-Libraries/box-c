import { shallowMount, RouterLinkStub } from '@vue/test-utils';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { createRouter, createWebHistory } from 'vue-router';
import { createTestingPinia } from '@pinia/testing';
import { useAccessStore } from '@/stores/access';
import advancedSearch from '@/components/advancedSearch.vue';
import displayWrapper from "@/components/displayWrapper.vue";
import { createI18n } from "vue-i18n";
import translations from "@/translations";
import mockAxios from 'vitest-mock-axios';

let wrapper, router, store;

describe('advancedSearch.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        router = createRouter({
            history: createWebHistory(),
            routes: [
                { path: '/advancedSearch', name: 'advancedSearch', component: advancedSearch },
                { path: '/record/:uuid', name: 'displayRecords', component: displayWrapper }
            ]
        });

        wrapper = shallowMount(advancedSearch, {
            global: {
                plugins: [i18n, router, createTestingPinia({ stubActions: false })],
                stubs: { RouterLink: RouterLinkStub }
            }
        });
        store = useAccessStore();
    });

    afterEach(() => {
        store.$reset();
    });

    it("loads the advanced search form", () => {
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

        // Trigger the request
        wrapper.vm.getCollections();

        mockAxios.mockResponse({ data: collections });

        expect(wrapper.vm.collections).toEqual(collections);
        expect(mockAxios.get).toHaveBeenCalledWith('/api/advancedSearch/collectionsJson');
    });

    it("loads the list of available file formats", async () => {
        const formats = ["Audio", "Image", "Video"];

        // Trigger request
        wrapper.vm.getFormats();

        mockAxios.mockResponse({
            data: formats,
            url: '/api/advancedSearch/formats'
        });

        expect(wrapper.vm.formats).toEqual(formats);
    });
});