import { shallowMount } from '@vue/test-utils';
import cdrFooter from '@/components/dcrFooter.vue';

describe('dcrFooter.vue', () => {

    it("loads the dcrFooter", () => {
        const wrapper = shallowMount(cdrFooter);
        wrapper.find('footer');
        expect(wrapper.find('footer').exists()).toBe(true);
    });

    it("verify Home url", () => {
        const homeUrl = window.location.hostname;
        const wrapper = shallowMount(cdrFooter);
        expect(wrapper.html()).toContain(homeUrl);
    });

});