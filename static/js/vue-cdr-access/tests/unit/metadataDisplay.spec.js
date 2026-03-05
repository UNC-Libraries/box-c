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

    it("calls loadMetadata on mount when canViewMetadata is true", async () => {
        const html_data = "<p>Mounted content</p>";
        fetchMock.mockResponseOnce(html_data);

        const mountedWrapper = shallowMount(metadataDisplay, {
            global: { plugins: [i18n, router] },
            props: { canViewMetadata: true, uuid: record.uuid }
        });

        await flushPromises();
        expect(mountedWrapper.vm.metadata).toEqual(html_data);
        expect(mountedWrapper.vm.hasLoaded).toBe(true);
    });

    it("does not call loadMetadata on mount when canViewMetadata is false", async () => {
        const mountedWrapper = shallowMount(metadataDisplay, {
            global: { plugins: [i18n, router] },
            props: { canViewMetadata: false, uuid: record.uuid }
        });

        await flushPromises();
        expect(mountedWrapper.vm.metadata).toEqual('');
        expect(mountedWrapper.vm.hasLoaded).toBe(false);
    });

    it("sets hasLoaded to true after successful fetch", async () => {
        fetchMock.mockResponseOnce("<p>Data</p>");
        expect(wrapper.vm.hasLoaded).toBe(false);

        wrapper.vm.loadMetadata();
        await flushPromises();

        expect(wrapper.vm.hasLoaded).toBe(true);
    });

    it("sets hasLoaded to true and clears metadata on fetch error response", async () => {
        fetchMock.mockResponseOnce('', { status: 500 });

        wrapper.vm.loadMetadata();
        await flushPromises();

        expect(wrapper.vm.hasLoaded).toBe(true);
        expect(wrapper.vm.metadata).toEqual('');
    });

    it("sets hasLoaded to true and clears metadata on network failure", async () => {
        fetchMock.mockRejectOnce(new Error('Network failure'));

        wrapper.vm.loadMetadata();
        await flushPromises();

        expect(wrapper.vm.hasLoaded).toBe(true);
        expect(wrapper.vm.metadata).toEqual('');
    });

    it("fetches correct URL for the given uuid", async () => {
        fetchMock.mockResponseOnce("<p>Data</p>");

        wrapper.vm.loadMetadata();
        await flushPromises();

        const calledUrl = fetchMock.mock.calls[0][0];
        expect(calledUrl).toContain(`/api/record/${record.uuid}/metadataView`);
    });

    it("reloads metadata when uuid prop changes and canViewMetadata is true", async () => {
        const newUuid = 'aaaabbbb-cccc-dddd-eeee-ffffaaaabbbb';
        fetchMock.mockResponseOnce("<p>Original</p>");
        wrapper.vm.loadMetadata();
        await flushPromises();

        fetchMock.mockResponseOnce("<p>Updated</p>");
        await wrapper.setProps({ uuid: newUuid });
        await flushPromises();

        expect(wrapper.vm.metadata).toEqual("<p>Updated</p>");
        const lastUrl = fetchMock.mock.calls[fetchMock.mock.calls.length - 1][0];
        expect(lastUrl).toContain(newUuid);
    });

    it("does not reload metadata when uuid changes and canViewMetadata is false", async () => {
        await wrapper.setProps({ canViewMetadata: false });
        fetchMock.resetMocks();

        await wrapper.setProps({ uuid: 'new-uuid-1234' });
        await flushPromises();

        expect(fetchMock).not.toHaveBeenCalled();
        expect(wrapper.vm.metadata).toEqual('');
    });

    it("does not render the metadata section when hasLoaded is false", () => {
        expect(wrapper.find('#mods_data_display').exists()).toBe(false);
    });

    it("does not render the metadata section when metadata is empty after load", async () => {
        fetchMock.mockResponseOnce('');
        wrapper.vm.loadMetadata();
        await flushPromises();

        expect(wrapper.find('#mods_data_display').exists()).toBe(false);
    });
});