import { createLocalVue, shallowMount } from '@vue/test-utils';
import VueRouter from 'vue-router';
import fetchMock from 'fetch-mock';
import flushPromises from 'flush-promises';
import modalMetadata from '@/components/modalMetadata.vue';

const localVue = createLocalVue();
const router = new VueRouter();
localVue.use(VueRouter);

const updated_uuid = 'c03a4bd7-25f4-4a6c-a68d-fedc4251b680';
const title = 'Test Collection';
const response = `<table><tbody><tr><th>Creator</th>
    <td><p>Real Dean</p></td></tr></tbody>
    </table>`;
let wrapper;
let btn;

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

        btn = wrapper.find('#show-modal');
        fetchMock.get('*', response);
    });

    it("updates the metadata when the record uuid changes", async () => {
        // Trigger prop update to call fetch request
        wrapper.setProps({ uuid: updated_uuid });

        await flushPromises();
        expect(wrapper.vm.metadata).toEqual(response);
    });

    it("is hidden by default", () => {
        expect(wrapper.find('.meta-modal').contains('.modal-body')).toBe(false);
    });

    it("displays a record title when triggered", () => {
        btn.trigger('click');
        const record = wrapper.find('h3');
        expect(record.text()).toBe(title);
    });

    it("displays metadata when triggered", async () => {
        // Trigger prop update to call fetch request
        wrapper.setProps({ uuid: updated_uuid });

        // Trigger modal open
        btn.trigger('click');

        await flushPromises();
        const record = wrapper.find('table');
        expect(record.html()).toBe(wrapper.vm.metadata);
    });

    afterEach(() => {
        fetchMock.restore();
    })
});