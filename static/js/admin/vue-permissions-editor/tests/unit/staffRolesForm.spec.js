import { createLocalVue, shallowMount } from '@vue/test-utils';
import staffRolesForm from '@/components/staffRolesForm.vue';

const localVue = createLocalVue();
let wrapper, btn;

describe('staffRolesForm.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(staffRolesForm, {
            localVue,
            propsData: {
                containerType: 'Collection'
            }
        });

        btn = wrapper.find('.btn-add');
    });

    it("emits an event with updated user when username or role changes", () => {
        wrapper.find('input').setValue('test_user');
        wrapper.findAll('option').at(4).setSelected();
        btn.trigger('click');

        expect(wrapper.emitted()['add-user'][0]).toEqual([{ principal: 'test_user', role: 'canManage', type: 'new' }]);
    });

    it("resets the form when user successfully submitted", () => {
        wrapper.find('input').setValue('test_user');
        wrapper.findAll('option').at(4).setSelected();
        btn.trigger('click');

        expect(wrapper.vm.user_name).toEqual('');
        expect(wrapper.vm.selected_role).toEqual('');
    });


    it("emits an error event when username is empty", () => {
        wrapper.findAll('option').at(4).setSelected();
        btn.trigger('click');

        expect(wrapper.emitted()['form-error'][0]).toEqual(['Please add a user and role before submitting']);
    });

    it("emits an event that clears the error text when the username input is focused", () => {
        wrapper.find('input').trigger('focus');
        expect(wrapper.emitted()['form-error'][0]).toEqual(['']);
    });

    it("emits an error event when role is empty", () => {
        wrapper.find('input').setValue('test_user');
        btn.trigger('click');

        expect(wrapper.emitted()['form-error'][0]).toEqual(['Please add a user and role before submitting']);
    });

    it("emits an event that clears the error text when the role select box is focused", () => {
        wrapper.find('select').trigger('focus');
        expect(wrapper.emitted()['form-error'][0]).toEqual(['']);
    });
});