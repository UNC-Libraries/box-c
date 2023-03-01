import { shallowMount } from '@vue/test-utils';
import dcrHeader from '@/components/dcrHeader.vue';

describe('dcrHeader.vue', () => {
    it("loads the dcrHeader", () => {
        const wrapper = shallowMount(dcrHeader);
        wrapper.find('header');
        expect(wrapper.find('header').exists()).toBe(true);
    });

});