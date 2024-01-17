import {mount, RouterLinkStub} from '@vue/test-utils';
import {createTestingPinia} from '@pinia/testing';
import { useAccessStore } from '@/stores/access';
import headerSmall from '@/components/header/headerSmall.vue';

let wrapper, store;
describe('headerSmallUtils', () => {
    beforeEach(() => {
        wrapper = mount(headerSmall, {
            global: {
                plugins: [createTestingPinia({
                    initialState: {
                        access: {
                            isLoggedIn: true,
                            username: 'test_user',
                            viewAdmin: true
                        }
                    },
                    stubActions: false
                })],
                stubs: {
                    RouterLink: RouterLinkStub
                }
            }
        });
        store = useAccessStore();
    })

    afterEach(() => {
        store.$reset();
    });

    it("jumpToAdminUrl is record-specific admin url", () => {
        const current_page = window.location;
        const testUrl = `https://${current_page.hostname}/record/73bc003c-9603-4cd9-8a65-93a22520ef6a`;
        const adminUrl = `https://${current_page.hostname}/admin/list/73bc003c-9603-4cd9-8a65-93a22520ef6a`;
        window.location = Object.assign(new URL(testUrl));
        expect(wrapper.html()).toContain(adminUrl);
    });

    it("jumpToAdminUrl is admin url", () => {
        const adminUrl = `https://${window.location.host}/admin/`;
        expect(wrapper.html()).toContain(adminUrl);
    });

    it("opens and closes the mobile menu", async () => {
        // Default
        expect(wrapper.find('#navbar-burger').attributes('aria-expanded')).toEqual('false');
        expect(wrapper.find('#navbar').classes()).not.toContain('active');

        // Open menu
        await wrapper.find('#navbar-burger').trigger('click');
        expect(wrapper.find('#navbar-burger').attributes('aria-expanded')).toEqual('true');
        expect(wrapper.find('#navbar').classes()).toContain('is-active');
        // Close menu
        await wrapper.find('#navbar-burger').trigger('click');
        expect(wrapper.find('#navbar-burger').attributes('aria-expanded')).toEqual('false');
        expect(wrapper.find('#navbar').classes()).not.toContain('is-active');
    });
});