import { shallowMount } from '@vue/test-utils';
import modalPermissionsEditor from '@/components/permissions-editor/modalPermissionsEditor.vue';
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

describe('modalPermissionsEditor.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(modalPermissionsEditor,
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
        await store.setShowPermissionsModal(true);
        await store.setMetadata(metadata);

        const record = wrapper.find('h3');
        expect(record.text()).toContain(metadata.title);
    });

    it("displays the correct icon for a Collection", async () => {
        await store.setMetadata(metadata);
        expect(wrapper.vm.iconType).toBe('fa-archive');
    });

    it("displays the correct icon for an AdminUnit", async () => {
        await store.setMetadata({ ...metadata, type: 'AdminUnit' });
        expect(wrapper.vm.iconType).toBe('fa-university');
    });

    it("displays the correct icon for a Folder", async () => {
        await store.setMetadata({ ...metadata, type: 'Folder' });
        expect(wrapper.vm.iconType).toBe('fa-folder');
    });

    it("displays no icon for other record types", async () => {
        await store.setMetadata({ ...metadata, type: 'Work' });
        expect(wrapper.vm.iconType).toBe('');
    });

    it("returns the parent container name", async () => {
        await store.setMetadata(metadata);
        expect(wrapper.vm.parentContainerName).toBe('Test Unit');
    });

    it("returns an empty string if the record title is not found in the object path", async () => {
        await store.setMetadata({ ...metadata, title: 'Nonexistent Title' });
        expect(wrapper.vm.parentContainerName).toBe('');
    });

    it("triggers check for unsaved changes when the close button is clicked", async () => {
        await store.setShowPermissionsModal(true);
        await store.setMetadata(metadata);
        await wrapper.find('.close-icon').trigger('click');
        expect(store.checkForUnsavedChanges).toBe(true);
    });

    it("hides the modal when closeModal is called", async () => {
        await store.setShowPermissionsModal(true);
        wrapper.vm.closeModal();
        await wrapper.vm.$nextTick();
        expect(store.showPermissionsModal).toBe(false);
    });

    it("resets the check for unsaved changes flag", async () => {
        await store.setCheckForUnsavedChanges(true);
        wrapper.vm.resetChangesCheck(false);
        expect(store.checkForUnsavedChanges).toBe(false);
    });

    it("shows patron roles when permission type is 'Patron'", async () => {
        await store.setShowPermissionsModal(true);
        await store.setMetadata(metadata);
        await store.setPermissionType('Patron');
        expect(wrapper.findComponent({ name: 'patronRoles' }).exists()).toBe(true);
        expect(wrapper.findComponent({ name: 'staffRoles' }).exists()).toBe(false);
    });

    it("shows staff roles when permission type is not 'Patron'", async () => {
        await store.setShowPermissionsModal(true);
        await store.setMetadata(metadata);
        await store.setPermissionType('Staff');
        expect(wrapper.findComponent({ name: 'staffRoles' }).exists()).toBe(true);
        expect(wrapper.findComponent({ name: 'patronRoles' }).exists()).toBe(false);
    });

    it("displays the permission type in the modal header", async () => {
        await store.setShowPermissionsModal(true);
        await store.setMetadata(metadata);
        await store.setPermissionType('Patron');
        expect(wrapper.find('h3').text()).toContain('Patron');
    });

   // See staffRoles.spec.js for tests on modal closure with and without unsaved changes
});