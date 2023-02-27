import { shallowMount } from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import moxios from 'moxios'
import pretty from 'pretty';
import modalMetadata from '@/components/modalMetadata.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from "vue-i18n";
import translations from "@/translations";

const updated_uuid = 'c03a4bd7-25f4-4a6c-a68d-fedc4251b680';
const title = 'Test Collection';
const response = pretty(`<table><tbody><tr><th>Creator</th><td><p>Real Dean</p></td></tr></tbody></table>`);
let router, wrapper;

describe('modalMetadata.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(async () => {
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
        wrapper = shallowMount(modalMetadata, {
            global: {
                plugins: [router, i18n]
            },
            props: {
                openModal: true,
                uuid: '',
                title: title
            }
        });
        await router.push('/record/1234');
    });

    afterEach(() => {
        wrapper = null;
        router = null;
    });

    it("fetches the record metadata when the modal is opened", () => {
        moxios.install();
        moxios.stubRequest(`record/${updated_uuid}/metadataView`, {
            status: 200,
            responseText: {
                data: response
            }
        });

        moxios.wait(() => {
            expect(wrapper.vm.metadata).toEqual(response);
            done();
        });

        moxios.uninstall();
    });

    it("is hidden by default", async () => {
        await wrapper.setProps({
            openModal: false
        });
        expect(wrapper.find('.meta-modal').exists()).toBe(false);
    });

    it("displays a record title when triggered", async () => {
        const record = wrapper.find('h3');
        expect(record.text()).toBe(title);
    });

    it("displays metadata when triggered", async () => {
        await wrapper.setData({
            metadata: response,
            hasLoaded: true
        });

        const record = wrapper.find('table');
        expect(record.html()).toBe(wrapper.vm.metadata);
    });

    it("emits an action to close the modal", async () => {
        await wrapper.find('button').trigger('click');
        expect(wrapper.emitted()['display-metadata'][0][0]).toBe(false);
    });
});