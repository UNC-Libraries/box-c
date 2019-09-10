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
        wrapper.findAll('option').at(2).setSelected();
        btn.trigger('click');

        expect(wrapper.emitted()['add-user'][0]).toEqual([{ principal: 'test_user', role: 'canDescribe', type: 'new' }]);
    });

    it("emits an event when the username field is filled out and input loses focus", () => {
        let input_field = wrapper.find('input');
        input_field.setValue('test_user');
        input_field.trigger('focusout');

        expect(wrapper.emitted()['username-set'][0]).toEqual([true]);
    });

    it("emits an event if the user cancels the modal and there are un-saved updates", () => {
        wrapper.find('input').setValue('test_user');
        wrapper.setProps({
            isCanceling: true
        });
        expect(wrapper.emitted()['add-user'][0]).toEqual([{ principal: 'test_user', role: 'canAccess', type: 'new' }]);
    });

    it("emits an event if the user clicks 'Save' and the form is filled out but updates haven't been added to user list", () => {
        wrapper.find('input').setValue('test_user');
        wrapper.setProps({
            isSubmitting: true
        });
        expect(wrapper.emitted()['add-user'][0]).toEqual([{ principal: 'test_user', role: 'canAccess', type: 'new' }]);
    });

    it("resets the form when user successfully submitted", () => {
        wrapper.find('input').setValue('test_user');
        wrapper.findAll('option').at(2).setSelected();
        btn.trigger('click');

        expect(wrapper.vm.user_name).toEqual('');
        expect(wrapper.vm.selected_role).toEqual('canAccess');
    });

    it("emits an error event when username is empty", () => {
        wrapper.findAll('option').at(2).setSelected();
        btn.trigger('click');

        expect(wrapper.emitted()['form-error'][0]).toEqual(['Please add a user before submitting']);
    });

    it("emits an event that clears the error text when the username input is focused", () => {
        wrapper.find('input').trigger('focus');
        expect(wrapper.emitted()['form-error'][0]).toEqual(['']);
    });
});