import { createLocalVue, shallowMount } from '@vue/test-utils';
import Vue from 'Vue'
import embargo from '@/components/embargo.vue';
import { addYears, format } from 'date-fns';

const localVue = createLocalVue();
const testDate = '2099-01-01';
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
    beforeEach(async () => {
        wrapper = shallowMount(embargo, {
            localVue,
            props: {
                currentEmbargo: '',
                isDeleted: false
            }
        });

        wrapper.setData({
            custom_embargo_date: testDate,
            embargo_ends_date: testDate,
            has_embargo: true
        });

        await wrapper.vm.$nextTick();
        btn = wrapper.find('#remove-embargo');
        inputs = wrapper.findAll('input');
        global.confirm = jest.fn().mockReturnValue(true);
    });

    it("sets an embargo if one is returned from the server", async () => {
        wrapper.setData(embargo_from_server);
        wrapper.setProps({ currentEmbargo: testDate });

        await wrapper.vm.$nextTick();
        expect(wrapper.vm.embargo_ends_date).toEqual(testDate);
    });

    it("does not set an embargo if one is not returned from the server", async () => {
        wrapper.setData(embargo_from_server);
        wrapper.setProps({ currentEmbargo: null });

        await wrapper.vm.$nextTick();
        expect(wrapper.vm.embargo_ends_date).toEqual('');
    });

    it("shows the embargo form if an embargo is set", async () => {
        wrapper.setData(embargo_from_server);
        wrapper.setProps({ currentEmbargo: testDate });

        await wrapper.vm.$nextTick();
        expect(wrapper.contains('.form')).toBe(true);
    });

    it("hides the embargo form if no embargo is set and displays an 'Add Embargo' button", async () => {
        wrapper.setData(embargo_from_server);
        wrapper.setProps({ currentEmbargo: null });

        await wrapper.vm.$nextTick();
        expect(wrapper.contains('.form')).toBe(false);
        expect(wrapper.contains('#show-form')).toBe(true);
    });

    it("shows the embargo form if the 'set embargo' button is clicked", async () => {
        wrapper.setData(embargo_from_server);
        wrapper.setProps({ currentEmbargo: null });

        await wrapper.vm.$nextTick();
        wrapper.find('#show-form').trigger('click');
        await wrapper.vm.$nextTick();
        expect(wrapper.contains('.form')).toBe(true);
    });

    it("shows a 'Remove Embargo' button if an embargo is set", async () => {
        wrapper.setData(embargo_from_server);
        wrapper.setProps({ currentEmbargo: testDate });

        await wrapper.vm.$nextTick();
        expect(wrapper.find('#remove-embargo').classes('hidden')).toBe(false);
    });

    it("hides the 'Remove Embargo' button if no embargo is set and the form is visible", async () => {
        wrapper.setData(embargo_from_server);
        wrapper.setProps({ currentEmbargo: null });

        await wrapper.vm.$nextTick();
        wrapper.find('#show-form').trigger('click');

        await wrapper.vm.$nextTick();
        expect(wrapper.find('#remove-embargo').classes('hidden')).toBe(true);
    });

    it("hides the form and displays an 'Add Embargo' button if an embargo is removed", async() => {
        wrapper.setData(embargo_from_server);
        wrapper.setProps({ currentEmbargo: testDate });

        await wrapper.vm.$nextTick();
        wrapper.find('#remove-embargo').trigger('click');

        await wrapper.vm.$nextTick();
        expect(wrapper.contains('.form')).toBe(false);
        expect(wrapper.contains('#show-form')).toBe(true);
    });

    it("emits an event when a new embargo is added", async () => {
        wrapper.setData({
            has_embargo: false
        });

        await wrapper.vm.$nextTick();
        inputs.at(2).trigger('focusout');
        wrapper.setProps({
            currentEmbargo: testDate
        });

        await wrapper.vm.$nextTick();
        expect(wrapper.emitted()['embargo-info'][0]).toEqual([testDate]);
        expect(wrapper.vm.has_embargo).toBe(true);
    });

    it("updates current embargo if one is already present", async () => {
        expect(wrapper.vm.embargo_ends_date).toBe(testDate);
        await wrapper.vm.$nextTick();
        inputs.at(0).trigger('click');

        let next_year = format(addYears(new Date(), 1), 'yyyy-LL-dd');
        expect(wrapper.vm.fixed_embargo_date).toEqual('1');
        expect(wrapper.vm.embargo_ends_date).toEqual(next_year);
    });

    it("asks user to confirm that they want to delete an embargo", () => {
        btn.trigger('click');
        expect(global.confirm).toHaveBeenCalled();
    });

    it("emits an event when an embargo is removed", async() => {
        btn.trigger('click');
        wrapper.setProps({
            currentEmbargo: null
        });

        await wrapper.vm.$nextTick();
        expect(wrapper.emitted()['embargo-info'][0]).toEqual([null]);
        expect(wrapper.vm.has_embargo).toBe(false);
    });

    it("emits an error message if user tries to add embargo with the wrong date format", async() => {
        wrapper.setData({
            custom_embargo_date: '2099-01'
        });

        await wrapper.vm.$nextTick();
        inputs.at(2).trigger('focusout');
        expect(wrapper.emitted()['error-msg'][0]).toEqual(['Please enter a valid date in the following format YYYY-MM-DD']);

        wrapper.setData({
            custom_embargo_date: '2099-60-34'
        });

        await wrapper.vm.$nextTick();
        inputs.at(2).trigger('focusout');
        expect(wrapper.emitted()['error-msg'][0]).toEqual(['Please enter a valid date in the following format YYYY-MM-DD']);
    });

    it("emits an error message if user tries to add embargo in the past", async () => {
        wrapper.setData(past_embargo);
        await wrapper.vm.$nextTick();
        inputs.at(2).trigger('focusout');
        expect(wrapper.emitted()['error-msg'][0]).toEqual(['Please enter a future date']);
    });

    it("emits a message to clear error message if a form input is clicked", async () => {
        wrapper.setData(past_embargo);
        await wrapper.vm.$nextTick();
        inputs.at(2).trigger('focusout');

        expect(wrapper.emitted()['error-msg'][0]).toEqual(['Please enter a future date']);

        inputs.at(2).trigger('click');
        expect(wrapper.emitted()['error-msg'][1]).toEqual(['']);
    });

    it("disables the form if the object or its parent is marked for deletion", async () => {
        wrapper.setProps({
            isDeleted: true
        });

        await wrapper.vm.$nextTick();
        expect(wrapper.find('fieldset').html()).toEqual(expect.stringContaining('disabled="disabled"'));
    });
});