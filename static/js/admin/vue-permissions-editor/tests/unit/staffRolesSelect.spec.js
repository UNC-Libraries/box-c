import { shallowMount } from '@vue/test-utils';
import staffRolesSelect from '@/components/staffRolesSelect.vue';
import store from '../../src/store';

let wrapper;

describe('staffRolesSelect.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(staffRolesSelect, {
            props: {
                areDeleted: [],
                containerType: 'Collection',
                user: { principal: 'test_user', role: 'canAccess' }
            },
            global: {
                plugins: [store]
            }
        });
    });

    it("displays the correct role if preset", () => {
        let select = wrapper.find('select');
        expect(select.element.value).toEqual('canAccess');
    });

    it("updates data store with updated user role when user role changes", () => {
        wrapper.findAll('option')[2].setSelected();
        expect(wrapper.vm.$store.state.staffRole).toEqual({ principal: 'test_user', role: 'canDescribe' });
    });
});