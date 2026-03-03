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

    beforeEach(async () => {
        fetchMock.resetMocks();
        // Consume the two mounted() calls (getCollections + getFormats)
        fetchMock.mockResponseOnce(JSON.stringify([]), { headers: { 'Content-Type': 'application/json' } });
        fetchMock.mockResponseOnce(JSON.stringify([]), { headers: { 'Content-Type': 'application/json' } });
        // Fallback for any additional calls
        fetchMock.mockResponse(JSON.stringify([]), { headers: { 'Content-Type': 'application/json' } });

        router = createRouter({
            history: createWebHistory(process.env.BASE_URL),
            routes: [
                {
                    path: '/advancedSearch',
                    name: 'advancedSearch',
                    component: advancedSearch
                },
                {
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

    it("renders the headerSmall component", () => {
        expect(wrapper.findComponent({ name: 'headerSmall' }).exists()).toBe(true);
    });

    it("renders the form with correct action and method", () => {
        const form = wrapper.find('form#advanced-search-form');
        expect(form.attributes('action')).toBe('/api/advancedSearch');
        expect(form.attributes('method')).toBe('get');
    });

    it("renders the anywhere input field", () => {
        const input = wrapper.find('input#anywhere');
        expect(input.exists()).toBe(true);
        expect(input.attributes('name')).toBe('anywhere');
    });

    it("renders the title input field", () => {
        const input = wrapper.find('input#title');
        expect(input.exists()).toBe(true);
        expect(input.attributes('name')).toBe('titleIndex');
    });

    it("renders the contributor input field", () => {
        const input = wrapper.find('input#contributor');
        expect(input.exists()).toBe(true);
        expect(input.attributes('name')).toBe('contributorIndex');
    });

    it("renders the subject input field", () => {
        const input = wrapper.find('input#subject');
        expect(input.exists()).toBe(true);
        expect(input.attributes('name')).toBe('subjectIndex');
    });

    it("renders the collection id input field", () => {
        const input = wrapper.find('input#collection_id');
        expect(input.exists()).toBe(true);
        expect(input.attributes('name')).toBe('collectionId');
    });

    it("renders deposited start and end date inputs", () => {
        expect(wrapper.find('input#addedStart').exists()).toBe(true);
        expect(wrapper.find('input#addedEnd').exists()).toBe(true);
    });

    it("renders created start and end date inputs", () => {
        expect(wrapper.find('input#createdYearStart').exists()).toBe(true);
        expect(wrapper.find('input#createdYearEnd').exists()).toBe(true);
    });

    it("renders a submit button", () => {
        expect(wrapper.find('input#advsearch_submit[type="submit"]').exists()).toBe(true);
    });

    it("renders collections in the collection select after loading", async () => {
        const collections = [
            { id: 'fc77a9be-b49d-4f4e-b656-1644c9e964fc', title: 'testCollection' }
        ];
        fetchMock.mockResponseOnce(JSON.stringify(collections));
        await wrapper.vm.getCollections();
        await flushPromises();

        const options = wrapper.find('select[name="collection"]').findAll('option');
        expect(options).toHaveLength(2); // default + 1 collection
        expect(options[1].text()).toBe('testCollection');
        expect(options[1].attributes('value')).toBe('fc77a9be-b49d-4f4e-b656-1644c9e964fc');
    });

    it("renders formats in the format select after loading", async () => {
        const formats = ['Audio', 'Image', 'Video'];
        fetchMock.mockResponseOnce(JSON.stringify(formats));
        await wrapper.vm.getFormats();
        await flushPromises();

        const options = wrapper.find('select[name="format"]').findAll('option');
        expect(options).toHaveLength(4); // default + 3 formats
        expect(options[1].text()).toBe('Audio');
        expect(options[2].text()).toBe('Image');
        expect(options[3].text()).toBe('Video');
    });

    it("handles error in getCollections gracefully", async () => {
        const consoleLogSpy = vi.spyOn(console, 'log').mockImplementation(() => {});
        fetchMock.mockRejectOnce(new Error('Network failure'));
        await wrapper.vm.getCollections();
        await flushPromises();

        expect(wrapper.vm.collections).toEqual([]);
        expect(consoleLogSpy).toHaveBeenCalled();
        consoleLogSpy.mockRestore();
    });

    it("handles error in getFormats gracefully", async () => {
        const consoleLogSpy = vi.spyOn(console, 'log').mockImplementation(() => {});
        fetchMock.mockRejectOnce(new Error('Network failure'));
        await wrapper.vm.getFormats();
        await flushPromises();

        expect(wrapper.vm.formats).toEqual([]);
        expect(consoleLogSpy).toHaveBeenCalled();
        consoleLogSpy.mockRestore();
    });

    it("collection select has a default empty option", () => {
        const options = wrapper.find('select[name="collection"]').findAll('option');
        expect(options[0].text()).toBe('Collection');
        expect(options[0].attributes('value')).toBe('');
    });

    it("format select has a default empty option", () => {
        const options = wrapper.find('select[name="format"]').findAll('option');
        expect(options[0].text()).toBe('Format');
        expect(options[0].attributes('value')).toBe('');
    });
});
