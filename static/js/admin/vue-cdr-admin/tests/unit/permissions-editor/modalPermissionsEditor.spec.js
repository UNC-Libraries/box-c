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

    it("shows a Collection icon for Collection type", async () => {
        await store.setShowPermissionsModal(true);
        await store.setMetadata(metadata);
        expect(wrapper.find('i.fa').classes()).toContain('fa-archive');
    });

    it("shows an AdminUnit icon for AdminUnit type", async () => {
        await store.setShowPermissionsModal(true);
        await store.setMetadata({ ...metadata, type: 'AdminUnit' });
        expect(wrapper.find('i.fa').classes()).toContain('fa-university');
    });

    it("shows a Folder icon for Folder type", async () => {
        await store.setShowPermissionsModal(true);
        await store.setMetadata({ ...metadata, type: 'Folder' });
        expect(wrapper.find('i.fa').classes()).toContain('fa-folder');
    });

    it("shows no icon class for Work type", async () => {
        await store.setShowPermissionsModal(true);
        await store.setMetadata({ ...metadata, type: 'Work' });
        expect(wrapper.find('i.fa').classes()).not.toContain('fa-archive');
        expect(wrapper.find('i.fa').classes()).not.toContain('fa-university');
        expect(wrapper.find('i.fa').classes()).not.toContain('fa-folder');
    });

    it("returns the parent container name from the object path", async () => {
        await store.setMetadata(metadata);
        expect(wrapper.vm.parentContainerName).toBe('Test Unit');
    });

    it("returns empty string for parentContainerName when title not in objectPath", async () => {
        await store.setMetadata({ ...metadata, title: 'Not In Path' });
        expect(wrapper.vm.parentContainerName).toBe('');
    });

    it("sets checkForUnsavedChanges to true when close button is clicked", async () => {
        await store.setShowPermissionsModal(true);
        await store.setMetadata(metadata);
        await wrapper.find('button.close-icon').trigger('click');
        expect(store.checkForUnsavedChanges).toBe(true);
    });

    it("closeModal sets showPermissionsModal to false", async () => {
        await store.setShowPermissionsModal(true);
        wrapper.vm.closeModal();
        expect(store.showPermissionsModal).toBe(false);
    });

    it("resetChangesCheck updates checkForUnsavedChanges", async () => {
        wrapper.vm.resetChangesCheck(true);
        expect(store.checkForUnsavedChanges).toBe(true);
        wrapper.vm.resetChangesCheck(false);
        expect(store.checkForUnsavedChanges).toBe(false);
    });

    it("renders patron-roles when permissionType is 'Patron'", async () => {
        await store.setShowPermissionsModal(true);
        await store.setMetadata(metadata);
        store.permissionType = 'Patron';
        await wrapper.vm.$nextTick();
        expect(wrapper.findComponent({ name: 'patronRoles' }).exists()).toBe(true);
        expect(wrapper.findComponent({ name: 'staffRoles' }).exists()).toBe(false);
    });

    it("renders staff-roles when permissionType is not 'Patron'", async () => {
        await store.setShowPermissionsModal(true);
        await store.setMetadata(metadata);
        store.permissionType = 'Staff';
        await wrapper.vm.$nextTick();
        expect(wrapper.findComponent({ name: 'staffRoles' }).exists()).toBe(true);
        expect(wrapper.findComponent({ name: 'patronRoles' }).exists()).toBe(false);
    });

   // See staffRoles.spec.js for tests on modal closure with and without unsaved changes
});