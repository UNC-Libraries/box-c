import { createLocalVue, shallowMount } from '@vue/test-utils';
import embargo from '@/components/embargo.vue';

const localVue = createLocalVue();
let wrapper;
let btn;

describe('embargo.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(embargo, {
            localVue
        });

        btn = wrapper.find('#add-embargo');
    });

    it("emits an event when an embargo is added", () => {
        btn.trigger('click');
        expect(wrapper.emitted()['embargo-info'][0]).toEqual([{ text: 'May 5 2099' }]);
    });

    it("emits an event when an embargo is removed", () => {
        wrapper.setData({
            embargo_info: { text: 'May 5 2099' },
            has_embargo: true
        });

        btn.trigger('click');
        expect(wrapper.emitted()['embargo-info'][0]).toEqual([{}]);
    });

    it("updates button text when an embargo is added", () => {
        expect(btn.text()).toBe('Add Embargo');
        btn.trigger('click');
        expect(btn.text()).toBe('Remove Embargo');
    });

    it("updates button text when an embargo is removed", () => {
        wrapper.setData({
            embargo_info: { text: 'May 5 2099' },
            has_embargo: true
        });

        expect(btn.text()).toBe('Remove Embargo');
        btn.trigger('click');
        expect(btn.text()).toBe('Add Embargo');
    });
});