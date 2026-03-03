import {flushPromises, shallowMount} from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import metadataDisplay from '@/components/full_record/metadataDisplay.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';
import cloneDeep from 'lodash.clonedeep';

const record = {
    canViewMetadata: true,
    uuid: '4053adf7-7bdc-4c9c-8769-8cc5da4ce81d'
}

let wrapper, router;

describe('metadataDisplay.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        fetchMock.resetMocks();

        router = createRouter({
            history: createWebHistory(process.env.BASE_URL),
            routes: [
                {
                    path: '/record/:uuid',
                    name: 'displayRecords',
                    component: displayWrapper
                }
            ]
        });

        wrapper = shallowMount(metadataDisplay, {
            global: {
                plugins: [i18n, router]
            },
            props: {
                canViewMetadata: record.canViewMetadata,
                uuid: record.uuid
            }
        });
    });

    it("loads record MODS metadata as html", async () => {
        const html_data = "<table><tr><th>Title</th><td><p>Listen for real</p></td></tr></table>";

        fetchMock.mockResponseOnce(html_data);
        wrapper.vm.loadMetadata();

        await flushPromises();

        expect(wrapper.vm.metadata).toEqual(html_data);
        expect(wrapper.find('#mods_data_display').text()).toEqual(expect.stringContaining('Listen for real'));
    });

    it("does not load record MODS metadata if empty", async () => {
        const html_data = "";

        fetchMock.mockResponseOnce(html_data);
        wrapper.vm.loadMetadata();

        await flushPromises();

        expect(wrapper.vm.metadata).toEqual('');
        expect(wrapper.find('#mods_data_display').exists()).toBe(false);
    });

    it('does not display metadata when a user does not have view access', () => {
        let updated_record = cloneDeep(record);
        updated_record.canViewMetadata = false;
        wrapper.setProps({
            canViewMetadata: false,
            uuid: record.uuid
        });
        expect(wrapper.vm.metadata).toEqual('');
        expect(wrapper.find('#mods_data_display').exists()).toBe(false);
    });

    it("sets hasLoaded to true after metadata loads", async () => {
        const html_data = "<table><tr><th>Title</th><td><p>Test</p></td></tr></table>";
        fetchMock.mockResponseOnce(html_data);
        wrapper.vm.loadMetadata();
        await flushPromises();
        expect(wrapper.vm.hasLoaded).toBe(true);
    });

    it("sets hasLoaded to true even when metadata is empty", async () => {
        fetchMock.mockResponseOnce('');
        wrapper.vm.loadMetadata();
        await flushPromises();
        expect(wrapper.vm.hasLoaded).toBe(true);
    });

    it("clears metadata and sets hasLoaded on fetch error", async () => {
        const consoleLogSpy = vi.spyOn(console, 'log').mockImplementation(() => {});
        fetchMock.mockRejectOnce(new Error('Network failure'));
        wrapper.vm.loadMetadata();
        await flushPromises();
        expect(wrapper.vm.metadata).toBe('');
        expect(wrapper.vm.hasLoaded).toBe(true);
        consoleLogSpy.mockRestore();
    });

    it("does not call loadMetadata on mount when canViewMetadata is false", async () => {
        const loadSpy = vi.spyOn(metadataDisplay.methods, 'loadMetadata');
        shallowMount(metadataDisplay, {
            global: { plugins: [i18n, router] },
            props: { canViewMetadata: false, uuid: record.uuid }
        });
        await flushPromises();
        expect(loadSpy).not.toHaveBeenCalled();
        loadSpy.mockRestore();
    });

    it("calls loadMetadata on mount when canViewMetadata is true", async () => {
        const html_data = "<p>Mounted metadata</p>";
        fetchMock.mockResponseOnce(html_data);
        const mountedWrapper = shallowMount(metadataDisplay, {
            global: { plugins: [i18n, router] },
            props: { canViewMetadata: true, uuid: record.uuid }
        });
        await flushPromises();
        expect(mountedWrapper.vm.metadata).toBe(html_data);
    });

    it("reloads metadata when uuid changes and canViewMetadata is true", async () => {
        const html_data = "<p>New metadata</p>";
        fetchMock.mockResponseOnce(html_data);
        await wrapper.setProps({ uuid: 'new-uuid-1234' });
        await flushPromises();
        expect(wrapper.vm.metadata).toBe(html_data);
    });

    it("does not reload metadata when uuid changes and canViewMetadata is false", async () => {
        const loadSpy = vi.spyOn(wrapper.vm, 'loadMetadata');
        await wrapper.setProps({ canViewMetadata: false });
        await wrapper.setProps({ uuid: 'another-uuid-5678' });
        await flushPromises();
        expect(loadSpy).not.toHaveBeenCalled();
        loadSpy.mockRestore();
    });

    it("fetches from the correct URL using the uuid prop", async () => {
        fetchMock.mockResponseOnce('<p>data</p>');
        wrapper.vm.loadMetadata();
        await flushPromises();
        const calledUrl = fetchMock.mock.calls[0][0];
        const url = typeof calledUrl === 'string' ? calledUrl : calledUrl.url;
        expect(url).toBe(`/api/record/${record.uuid}/metadataView`);
    });

    it("does not render the metadata section before loading completes", () => {
        expect(wrapper.find('#mods_data_display').exists()).toBe(false);
    });
});