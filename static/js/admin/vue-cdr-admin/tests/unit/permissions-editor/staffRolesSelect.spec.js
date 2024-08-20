import { shallowMount } from '@vue/test-utils';
import staffRolesSelect from '@/components/permissions-editor/staffRolesSelect.vue';
import { createTestingPinia } from '@pinia/testing';
import { usePermissionsStore } from '@/stores/permissions';

let wrapper, store;

describe('staffRolesSelect.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(staffRolesSelect, {
            props: {
                areDeleted: [],
                containerType: 'Collection',
                user: { principal: 'test_user', role: 'canAccess' }
            },
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

    it("displays the correct role if preset", () => {
        let select = wrapper.find('select');
        expect(select.element.value).toEqual('canAccess');
    });

    it("updates data store with updated user role when user role changes", () => {
        wrapper.findAll('option')[2].setSelected();
        expect(store.staffRole).toEqual({ principal: 'test_user', role: 'canDescribe' });
    });
});