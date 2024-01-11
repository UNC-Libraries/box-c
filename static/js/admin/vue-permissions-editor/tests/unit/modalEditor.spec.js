import { shallowMount } from '@vue/test-utils';
import modalEditor from '@/components/modalEditor.vue';
import { createTestingPinia } from '@pinia/testing';
import { usePermissionsStore } from '@/stores/permissions';

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
let wrapper, store;

describe('modalEditor.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(modalEditor,
            {
                global: {
                    plugins: [createTestingPinia({
                        stubActions: false
                    })]
                }
            });
        store = usePermissionsStore();
    });

    afterEach(() => {
        store.$reset();
        wrapper = null;
    });

    it("is hidden by default", () => {
        expect(wrapper.find('.meta-modal .modal-body').exists()).toBe(false);
    });

    it("displays a record title when triggered from admin interface", async () => {
        await store.setShowModal(true);
        await store.setMetadata(metadata);

        const record = wrapper.find('h3');
        expect(record.text()).toContain(metadata.title);
    });

   // See staffRoles.spec.js for tests on modal closure with and without unsaved changes
});