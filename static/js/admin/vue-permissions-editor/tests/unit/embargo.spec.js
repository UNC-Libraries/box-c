import { createLocalVue, shallowMount } from '@vue/test-utils';
import embargo from '@/components/embargo.vue';
import { addYears, format } from 'date-fns';

const localVue = createLocalVue();
let embargo_from_server = {
    custom_embargo_date: '',
    embargo_ends_date: '',
    has_embargo: false
};
let past_embargo = {
    embargo_ends_date: '2011-01-01',
    custom_embargo_date: '2011-01-01',
    fixed_embargo_date: '',
    has_embargo: false
};
let wrapper;
let btn;
let inputs;

describe('embargo.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(embargo, {
            localVue,
            props: {
                currentEmbargo: '',
                isDeleted: false
            }
        });

        wrapper.setData({
            custom_embargo_date: '2099-01-01',
            embargo_ends_date: '2099-01-01',
            has_embargo: true
        });

        btn = wrapper.find('#remove-embargo');
        inputs = wrapper.findAll('input');
        global.confirm = jest.fn().mockReturnValue(true);
    });

    it("sets an embargo if one is returned from the server", () => {
        let test_date = '2099-01-01';
        wrapper.setData(embargo_from_server);
        wrapper.setProps({ currentEmbargo: test_date });

        expect(wrapper.vm.embargo_ends_date).toEqual(test_date);
        expect(wrapper.vm.custom_embargo_date).toEqual(test_date);
    });

    it("does not set an embargo if one is not returned from the server", () => {
        wrapper.setData(embargo_from_server);
        wrapper.setProps({ currentEmbargo: null });

        expect(wrapper.vm.embargo_ends_date).toEqual('');
    });

    it("shows a 'Remove Embargo' button if an embargo is set", () => {
        let test_date = '2099-01-01';
        wrapper.setData(embargo_from_server);
        wrapper.setProps({ currentEmbargo: test_date });

        expect(btn.classes('hidden')).toBe(false);
    });

    it("hides the 'Remove Embargo' button if no embargo is set", () => {
        wrapper.setData(embargo_from_server);
        wrapper.setProps({ currentEmbargo: null });

        expect(btn.classes('hidden')).toBe(true);
    });

    it("emits an event when a new embargo is added", () => {
        wrapper.setData({
            has_embargo: false
        });

        inputs.at(2).trigger('focusout');
        wrapper.setProps({
            currentEmbargo: '2099-01-01'
        });

        expect(wrapper.emitted()['embargo-info'][0]).toEqual(['2099-01-01']);
        expect(wrapper.vm.has_embargo).toBe(true);
    });

    it("updates current embargo if one is already present", () => {
        expect(wrapper.vm.embargo_ends_date).toBe('2099-01-01');

        inputs.at(0).trigger('click');

        let next_year = format(addYears(new Date(), 1), 'yyyy-LL-dd');
        expect(wrapper.vm.fixed_embargo_date).toEqual('1');
        expect(wrapper.vm.embargo_ends_date).toEqual(next_year);
    });

    it("asks user to confirm that they want to delete an embargo", () => {
        btn.trigger('click');
        expect(global.confirm).toHaveBeenCalled();
    });

    it("emits an event when an embargo is removed", () => {
        btn.trigger('click');
        wrapper.setProps({
            currentEmbargo: null
        });
        expect(wrapper.emitted()['embargo-info'][0]).toEqual([null]);
        expect(wrapper.vm.has_embargo).toBe(false);
    });

    it("emits an error message if user tries to add embargo in the past", () => {
        wrapper.setData(past_embargo);

        inputs.at(2).trigger('focusout');
        expect(wrapper.emitted()['error-msg'][0]).toEqual(['Please enter a future date']);
    });

    it("if there is a current embargo it emits an error message and resets the form to the current embargo" +
        "if the user tries to set an embargo in the past", () => {
        wrapper.setData({
            embargo_ends_date: '2099-01-01',
            custom_embargo_date: '2011-01-01',
            fixed_embargo_date: '',
            has_embargo: true
        });

        inputs.at(2).trigger('focusout');
        expect(wrapper.emitted()['error-msg'][0]).toEqual(['Please enter a future date. Date reset to current embargo']);
        expect(wrapper.vm.custom_embargo_date).toEqual('2099-01-01');
    });

    it("it emits a message if there is a current embargo and the user clears the custom embargo field", () => {
        wrapper.setData({
            embargo_ends_date: '2099-01-01',
            custom_embargo_date: '',
            fixed_embargo_date: '',
            has_embargo: true
        });

        inputs.at(2).trigger('focusout');
        expect(wrapper.emitted()['error-msg'][0]).toEqual(['Embargo won\'t be removed until "Remove Embargo" is clicked']);
    });

    it("emits a message to clear error message if a form input is clicked", () => {
        wrapper.setData(past_embargo);

        inputs.at(2).trigger('focusout');
        expect(wrapper.emitted()['error-msg'][0]).toEqual(['Please enter a future date']);

        inputs.at(0).trigger('click');
        expect(wrapper.emitted()['error-msg'][1]).toEqual(['']);
    });

    it("disables the form if the object or its parent is marked for deletion", () => {
        wrapper.setProps({
            isDeleted: true
        });

        expect(wrapper.find('fieldset').html()).toEqual(expect.stringContaining('disabled="disabled"'));
    });
});