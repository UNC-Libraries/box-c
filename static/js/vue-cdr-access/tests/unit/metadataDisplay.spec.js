import { shallowMount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import metadataDisplay from '@/components/full_record/metadataDisplay.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';
import moxios from 'moxios';
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
        moxios.install();
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
        moxios.uninstall();
    });

    it("loads record MODS metadata as html", (done) => {
        const html_data = "<table><tr><th>Title</th><td><p>Listen for real</p></td></tr></table>";
        const url = `record/${record.uuid}/metadataView`;
        moxios.stubRequest(new RegExp(url), {
            status: 200,
            response: html_data
        });
        wrapper.vm.loadMetadata();

        moxios.wait(() => {
            expect(wrapper.vm.metadata).toEqual(html_data);
            expect(wrapper.find('#mods_data_display').text()).toEqual(expect.stringContaining('Listen for real'));
            done();
        });
    });

    it("does not load record MODS metadata if empty", (done) => {
        const html_data = "";
        const url = `record/${record.uuid}/metadataView`;
        moxios.stubRequest(new RegExp(url), {
            status: 200,
            response: html_data
        });
        wrapper.vm.loadMetadata();

        moxios.wait(() => {
            expect(wrapper.vm.metadata).toEqual('');
            expect(wrapper.find('#mods_data_display').exists()).toBe(false);
            done();
        });
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