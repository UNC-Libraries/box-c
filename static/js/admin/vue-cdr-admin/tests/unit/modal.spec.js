import {shallowMount} from '@vue/test-utils';
import {createTestingPinia} from "@pinia/testing";
import modal from '@/components/modal.vue';
import { useFormsStore } from '@/stores/forms';

let wrapper, store;

describe('modal.vue', () => {
    beforeEach(async () => {
        wrapper = shallowMount(modal, {
            global: {
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

    it("shows the deposit forms modal", async () => {
        expect(wrapper.findComponent({ name: 'modalDepositForms' }).exists()).toBe(false);
        await store.setShowFormsModal(true);
        expect(wrapper.findComponent({ name: 'modalDepositForms' }).exists()).toBe(true);
    });

    it("shows the permissions editor modal", async () => {
        await store.setShowFormsModal(true);
        expect(wrapper.findComponent({ name: 'modalPermissionsEditor' }).exists()).toBe(false);
        await store.setShowFormsModal(false);
        expect(wrapper.findComponent({ name: 'modalPermissionsEditor' }).exists()).toBe(true);
    });
});