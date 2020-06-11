import { createLocalVue, shallowMount } from '@vue/test-utils';
import VueRouter from 'vue-router';
import modalEditor from '@/components/modalEditor.vue';

const localVue = createLocalVue();
const router = new VueRouter();
localVue.use(VueRouter);

const title = 'Test Collection';
const metadata = {
    id: 'd77fd8c9-744b-42ab-8e20-5ad9bdf8194e',
    title: 'Test Collection',
    type: 'Collection',
    objectPath: [
        { container: true,  name: 'Root' },
        { container: true,  name: 'Test Unit' },
        { container: true, name: 'Test Collection' }
    ]
};
let wrapper;

describe('modalEditor.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(modalEditor, {
            localVue,
            router
        });

        wrapper.setData({
            metadata: metadata,
            permissionType: 'Staff',
            showModal: false,

        });
    });

    it("is hidden by default", () => {
        expect(wrapper.find('.meta-modal .modal-body').exists()).toBe(false);
    });

    it("displays a record title when triggered from admin interface", async () => {
        wrapper.setData({
            showModal: true
        });

        await wrapper.vm.$nextTick();
        const record = wrapper.find('h3');
        expect(record.text()).toContain(metadata.title);
    });

   // See staffRoles.spec.js for tests on modal closure with and without unsaved changes
});