import { mount } from '@vue/test-utils';
import ModalDepositForms from '@/components/forms-app/modalDepositForms.vue';
import {createTestingPinia} from '@pinia/testing';
import { useFormsStore } from '@/stores/forms';

let wrapper, store;

describe('modalDepositForms.vue', () => {
    const vueForm = jest.fn();

    beforeEach(async () => {
        wrapper = mount(ModalDepositForms,
            {
                global: {
                    components: {
                        Vueform: vueForm
                    },
                    plugins: [createTestingPinia({
                        stubActions: false
                    })]
                }
            });
        store = useFormsStore();
    });

    afterEach(() => {
        store.$reset();
        wrapper = null;
    });

    // Check for bulma class since we're not hiding it via a data value
    it("is hidden by default", () => {
        expect(wrapper.find('.bulma-modal').classes('bulma-is-active')).toBe(false);
    });

    it("displays the modal when triggered from the admin interface", async () => {
        expect(wrapper.find('.bulma-modal').classes('bulma-is-active')).toBe(false);
        await store.setShowFormsModal(true);
        expect(wrapper.find('.bulma-modal').classes('bulma-is-active')).toBe(true);
        expect(wrapper.find('.bulma-modal-content h1').text()).toEqual('Add a work to the current collection');
    });

    it("displays the generic form when selected", async () => {
        await store.setShowFormsModal(true);
        await wrapper.find('.bulma-select select').setValue('generic_work');

        expect(wrapper.find('.bulma-modal').classes('bulma-is-active')).toBe(true);
        expect(wrapper.vm.form).toEqual('generic_work');
        expect(vueForm).toHaveBeenCalled();
    });

    it("displays the continuing resource form when selected", async () => {
        await store.setShowFormsModal(true);
        await wrapper.find('.bulma-select select').setValue('continuing_resource_item');

        expect(wrapper.find('.bulma-modal').classes('bulma-is-active')).toBe(true);
        expect(wrapper.vm.form).toEqual('continuing_resource_item');
        expect(vueForm).toHaveBeenCalled();
    });

    it("closes the modal when the close button is clicked", async () => {
        await store.setShowFormsModal(true);
        expect(store.showFormsModal).toEqual(true);

        await wrapper.find('.bulma-modal-close').trigger('click');
        expect(store.showFormsModal).toEqual(false);
    });

    it("closes the modal when the user clicks outside the modal window", async () => {
        await store.setShowFormsModal(true);
        expect(store.showFormsModal).toEqual(true);

        await wrapper.find('.bulma-modal-background').trigger('click');
        expect(store.showFormsModal).toEqual(false);
    });
});