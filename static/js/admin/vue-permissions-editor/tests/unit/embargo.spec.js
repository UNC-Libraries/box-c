import { shallowMount } from '@vue/test-utils';
import embargo from '@/components/embargo.vue';
import { addYears, format } from 'date-fns';

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
            props: {
                currentEmbargo: '',
                isDeleted: false
            }
        });

        await wrapper.vm.$nextTick();
        inputs = wrapper.findAll('input');
        global.confirm = jest.fn().mockReturnValue(true);
    });

    it("sets an embargo if one is returned from the server", async () => {
        await wrapper.setData(embargo_from_server);
        await wrapper.setProps({ currentEmbargo: testDate });
        expect(wrapper.vm.embargo_ends_date).toEqual(testDate);
    });

    it("does not set an embargo if one is not returned from the server", async () => {
        await wrapper.setData(embargo_from_server);
        await wrapper.setProps({ currentEmbargo: null });
        expect(wrapper.vm.embargo_ends_date).toEqual('');
    });

    it("shows the embargo form if an embargo is set", async () => {
        await wrapper.setData(embargo_from_server);
        await wrapper.setProps({ currentEmbargo: testDate });
        expect(wrapper.find('.form').exists()).toBe(true);
    });

    it("hides the embargo form if no embargo is set and displays an 'Add Embargo' button", async () => {
        await wrapper.setData(embargo_from_server);
        await wrapper.setProps({ currentEmbargo: null });

        expect(wrapper.find('.form').exists()).toBe(false);
        expect(wrapper.find('#show-form').exists()).toBe(true);
    });

    it("shows the embargo form if the 'set embargo' button is clicked", async () => {
        await wrapper.setData(embargo_from_server);
        await wrapper.setProps({ currentEmbargo: null });
        await showForm();
        expect(wrapper.find('.form').exists()).toBe(true);
    });

    it("shows a 'Remove Embargo' button if an embargo is set", async () => {
        await wrapper.setData(embargo_from_server);
        await wrapper.setProps({ currentEmbargo: testDate });
        expect(wrapper.find('#remove-embargo').classes('hidden')).toBe(false);
    });

    it("hides the 'Remove Embargo' button if no embargo is set and the form is visible", async () => {
        await wrapper.setData(embargo_from_server);
        await wrapper.setProps({ currentEmbargo: null });
        await showForm();
        expect(wrapper.find('#remove-embargo').classes('hidden')).toBe(true);
    });

    it("hides the form and displays an 'Add Embargo' button if an embargo is removed", async() => {
        await wrapper.setData(embargo_from_server);
        await wrapper.setProps({ currentEmbargo: testDate });
        await wrapper.find('#remove-embargo').trigger('click');

        expect(wrapper.find('.form').exists()).toBe(false);
        expect(wrapper.find('#show-form').exists()).toBe(true);
    });

    it("emits an event when a new embargo is added", async () => {
        await showForm();
        wrapper.find("#custom-embargo").setValue(testDate);
        wrapper.find("#custom-embargo").trigger('focusout');

        await wrapper.vm.$nextTick();
        expect(wrapper.emitted()['embargo-info'][0]).toEqual([{
            embargo: testDate,
            skip_embargo: false
        }]);
    });

    it("updates current embargo if one is already present", async () => {
        await setStartingEmbargo();
        expect(wrapper.vm.embargo_ends_date).toBe(testDate);

        await wrapper.find("#embargo-1year").trigger('click');

        let next_year = format(addYears(new Date(), 1), 'yyyy-LL-dd');
        expect(wrapper.vm.embargo_ends_date).toEqual(next_year);
    });

    it("asks user to confirm that they want to delete an embargo", async() => {
        await setStartingEmbargo();
        await wrapper.find('#remove-embargo').trigger('click');
        expect(global.confirm).toHaveBeenCalled();
    });

    it("emits an event when an embargo is removed", async() => {
        await setStartingEmbargo();
        await wrapper.find('#remove-embargo').trigger('click');

        expect(wrapper.emitted()['embargo-info'][0]).toEqual([{
            embargo: null,
            skip_embargo: false
        }]);
        expect(wrapper.vm.has_embargo).toBe(false);
    });

    it("emits an error message if user tries to add embargo with the wrong date format", async() => {
        await showForm();
        wrapper.find("#custom-embargo").setValue('2099-01');
        wrapper.find("#custom-embargo").trigger('focusout');
        await wrapper.vm.$nextTick();
        expect(wrapper.emitted()['error-msg'][0]).toEqual(['Please enter a valid date in the following format YYYY-MM-DD']);

        wrapper.find("#custom-embargo").setValue('2099-60-34');
        wrapper.find("#custom-embargo").trigger('focusout');
        await wrapper.vm.$nextTick();
        expect(wrapper.emitted()['error-msg'][0]).toEqual(['Please enter a valid date in the following format YYYY-MM-DD']);
    });

    it("emits an error message if user tries to add embargo in the past", async () => {
        await showForm();
        wrapper.find("#custom-embargo").setValue('2011-01-01');
        wrapper.find("#custom-embargo").trigger('focusout');
        await wrapper.vm.$nextTick();
        expect(wrapper.emitted()['error-msg'][0]).toEqual(['Please enter a future date']);
    });

    it("emits a message to clear error message if a form input is clicked", async () => {
        await showForm();
        wrapper.find("#custom-embargo").setValue('2011-01-01');
        wrapper.find("#custom-embargo").trigger('focusout');
        await wrapper.vm.$nextTick();

        expect(wrapper.emitted()['error-msg'][0]).toEqual(['Please enter a future date']);

        await wrapper.find("#custom-embargo").trigger('click');
        expect(wrapper.emitted()['error-msg'][1]).toEqual(['']);
    });

    it("Custom radio button selected when custom input is focused", async() => {
        await showForm();
        await wrapper.find("#custom-embargo").trigger('click');
        expect(wrapper.vm.embargo_type).toEqual('custom');
    });

    it("disables the form if the object or its parent is marked for deletion", async () => {
        await wrapper.setProps({
            isDeleted: true
        });
        await setStartingEmbargo();
        expect(wrapper.find('fieldset').html()).toEqual(expect.stringContaining('disabled'));
    });

    it("does not disable the form if the object or its parent is marked for deletion", async () => {
        await wrapper.setProps({
            isDeleted: false
        });
        await setStartingEmbargo();
        expect(wrapper.find('fieldset').html()).not.toEqual(expect.stringContaining('disabled'));
    });

    it("Shows correct embargo type options in single mode", async() => {
        await setStartingEmbargo();

        expect(wrapper.find('#embargo-ignore').exists()).toBe(false);
        expect(wrapper.find('#embargo-clear').exists()).toBe(false);
        expect(wrapper.find('#embargo-1year').exists()).toBe(true);
        expect(wrapper.find('#embargo-2years').exists()).toBe(true);
        expect(wrapper.find('#embargo-custom').exists()).toBe(true);
        expect(wrapper.find('#remove-embargo').exists()).toBe(true);
    });

    it("Defaults to and shows correct embargo type options in bulk mode", async() => {
        await setToBulkMode();

        expect(wrapper.find('#show-form').exists()).toBe(false);
        expect(wrapper.find('#embargo-ignore').exists()).toBe(true);
        expect(wrapper.find('#embargo-clear').exists()).toBe(true);
        expect(wrapper.find('#embargo-1year').exists()).toBe(true);
        expect(wrapper.find('#embargo-2years').exists()).toBe(true);
        expect(wrapper.find('#embargo-custom').exists()).toBe(true);
        expect(wrapper.find('#remove-embargo').exists()).toBe(false);

        expect(wrapper.vm.embargo_type).toEqual('ignore');
    });

    it("In bulk mode, clearing embargo sends event", async () => {
        await setToBulkMode();
        await wrapper.find("#embargo-clear").trigger('click');

        expect(wrapper.vm.embargo_type).toEqual('clear');
        expect(wrapper.emitted()['embargo-info'][0]).toEqual([{
            embargo: null,
            skip_embargo: false
        }]);
    });

    it("In bulk mode, setting no change to embargo sends event", async () => {
        await setToBulkMode();
        await wrapper.find("#embargo-1year").trigger('click');
        await wrapper.find("#embargo-ignore").trigger('click');

        expect(wrapper.vm.embargo_type).toEqual('ignore');
        expect(wrapper.emitted()['embargo-info'][1]).toEqual([{
            embargo: null,
            skip_embargo: true
        }]);
    });

    it("In bulk mode, setting embargo duration sends event", async () => {
        await setToBulkMode();
        await wrapper.find("#embargo-1year").trigger('click');

        expect(wrapper.vm.embargo_type).toEqual('1year');
        let next_year = format(addYears(new Date(), 1), 'yyyy-LL-dd');
        expect(wrapper.emitted()['embargo-info'][0]).toEqual([{
            embargo: next_year,
            skip_embargo: false
        }]);
    });

    it("In bulk mode, setting custom duration sends event", async() => {
        await setToBulkMode();
        wrapper.find("#custom-embargo").trigger('click');
        wrapper.find("#custom-embargo").setValue(testDate);
        wrapper.find("#custom-embargo").trigger('focusout');
        await wrapper.vm.$nextTick();

        expect(wrapper.vm.embargo_type).toEqual('custom');
        expect(wrapper.emitted()['embargo-info'][0]).toEqual([{
            embargo: testDate,
            skip_embargo: false
        }]);
    });

    async function setToBulkMode() {
        await wrapper.setProps({ isBulkMode: true });
    }

    async function setStartingEmbargo(embargoDate = testDate) {
        await wrapper.setProps({ currentEmbargo: embargoDate });
    }

    async function showForm() {
        await wrapper.find("#show-form").trigger('click');
    }
});
