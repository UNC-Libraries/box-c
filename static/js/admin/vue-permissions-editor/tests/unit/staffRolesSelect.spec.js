import { createLocalVue, shallowMount } from '@vue/test-utils';
import staffRolesSelect from '@/components/staffRolesSelect.vue';

const localVue = createLocalVue();
let wrapper;

describe('staffRolesSelect.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(staffRolesSelect, {
            localVue,
            propsData: {
                areDeleted: [],
                containerType: 'Collection',
                user: { principal: 'test_user', role: 'canAccess' }
            }
        });
    });

    it("displays the correct role if preset", () => {
        let select = wrapper.find('select');
        expect(select.element.value).toEqual('canAccess');
    });

    it("emits an event with updated user role when user role changes", () => {
        wrapper.findAll('option').at(2).setSelected();
        expect(wrapper.emitted()['staff-role-update'][0]).toEqual([{ principal: 'test_user', role: 'canDescribe' }]);
    });
});