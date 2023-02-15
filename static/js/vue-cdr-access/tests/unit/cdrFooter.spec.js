import { shallowMount } from '@vue/test-utils';
import cdrFooter from '@/components/cdrFooter.vue';

describe('cdrFooter.vue', () => {

    it("loads the cdrFooter", () => {
        const wrapper = shallowMount(cdrFooter);
        wrapper.find('footer');
        expect(wrapper.find('footer').exists()).toBe(true);
    });

    it("verify Home url", () => {
        const homeUrl = "https://dcr.lib.unc.edu/";
        const wrapper = shallowMount(cdrFooter);
        expect(wrapper.html()).toContain(homeUrl);
    });

});