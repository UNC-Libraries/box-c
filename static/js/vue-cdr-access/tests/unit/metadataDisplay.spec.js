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
        fetchMock.enableMocks();
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

    afterEach(() => {
        fetchMock.disableMocks();
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
});