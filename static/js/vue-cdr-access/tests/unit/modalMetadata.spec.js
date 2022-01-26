import { shallowMount } from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import moxios from 'moxios'
import pretty from 'pretty';
import modalMetadata from '@/components/modalMetadata.vue';
import displayWrapper from '@/components/displayWrapper';

const updated_uuid = 'c03a4bd7-25f4-4a6c-a68d-fedc4251b680';
const title = 'Test Collection';
const response = pretty(`<table><tbody><tr><th>Creator</th><td><p>Real Dean</p></td></tr></tbody></table>`);
let router, wrapper;

describe('modalMetadata.vue', () => {
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
                plugins: [router]
            },
            props: {
                uuid: '',
                title: title
            }
        });
        await router.push('/record/1234');
    });

    afterEach(() => router = null);

    it("fetches the record metadata when the metadata button is clicked", () => {
        // Mock event
        const event = { preventDefault: jest.fn() };
        moxios.install();
        moxios.stubRequest(`record/${updated_uuid}/metadataView`, {
            status: 200,
            responseText: {
                data: response
            }
        });

        // link is outside of Vue, so just trigger it
        wrapper.vm.showMetadata(event);

        moxios.wait(() => {
            expect(wrapper.vm.metadata).toEqual(response);
            done();
        });

        moxios.uninstall();
    });

    it("is hidden by default", () => {
        expect(wrapper.find('.meta-modal').element).not.toContain('.modal-container');
    });

    it("displays a record title when triggered", async () => {
        await wrapper.setData({
            showModal: true
        });

        const record = wrapper.find('h3');
        expect(record.text()).toBe(title);
    });

    it("displays metadata when triggered", async () => {
        await wrapper.setData({
            metadata: response,
            showModal: true
        });

        const record = wrapper.find('table');
        expect(record.html()).toBe(wrapper.vm.metadata);
    });
});