import { shallowMount, RouterLinkStub } from '@vue/test-utils';
import cdrFooter from '@/components/dcrFooter.vue';

let wrapper;
describe('dcrFooter.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(cdrFooter, {
            global: {
                stubs: {
                    RouterLink: RouterLinkStub
                }
            }
        });
    })

    it("loads the dcrFooter", () => {
        expect(wrapper.find('footer').exists()).toBe(true);
    });

    it("verify Home url", () => {
        expect(wrapper.findComponent(RouterLinkStub).props().to).toEqual('/');
    });
});