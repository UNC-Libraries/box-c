import { createLocalVue, shallowMount } from '@vue/test-utils';
import VueRouter from 'vue-router';
import moxios from 'moxios'
import pretty from 'pretty';
import modalMetadata from '@/components/modalMetadata.vue';

const localVue = createLocalVue();
const router = new VueRouter();
localVue.use(VueRouter);

const updated_uuid = 'c03a4bd7-25f4-4a6c-a68d-fedc4251b680';
const title = 'Test Collection';
const response = pretty(`<table><tbody><tr><th>Creator</th><td><p>Real Dean</p></td></tr></tbody></table>`);
let wrapper;

describe('modalMetadata.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(modalMetadata, {
            localVue,
            router,
            propsData: {
                uuid: '',
                title: title
            }
        });
    });

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
        expect(wrapper.find('.meta-modal').contains('.modal-container')).toBe(false);
    });

    it("displays a record title when triggered", async () => {
        wrapper.setData({
            showModal: true
        });

        await wrapper.vm.$nextTick();
        const record = wrapper.find('h3');
        expect(record.text()).toBe(title);
    });

    it("displays metadata when triggered", async () => {
        wrapper.setData({
            metadata: response,
            showModal: true
        });

        await wrapper.vm.$nextTick();
        const record = wrapper.find('table');
        expect(record.html()).toBe(wrapper.vm.metadata);
    });
});