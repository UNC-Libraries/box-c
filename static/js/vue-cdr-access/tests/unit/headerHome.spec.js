import {mount, RouterLinkStub} from '@vue/test-utils';
import {createTestingPinia} from '@pinia/testing';
import { useAccessStore } from '@/stores/access';
import dcrHeader from '@/components/header/headerHome.vue';

let wrapper, store;
describe('headerHome.vue', () => {
    beforeEach(() => {
        wrapper = mount(dcrHeader, {
            global: {
                plugins: [createTestingPinia({
                    stubActions: false
                })],
                stubs: {
                    RouterLink: RouterLinkStub
                }
            }
        });
        store = useAccessStore();
    });

    afterEach(() => {
        store.$reset();
    });

    it("loads headerHome on the home page", () => {
        expect(wrapper.find('header').exists()).toBe(true);
        expect(wrapper.html()).toContain('header');
        expect(wrapper.html()).toContain('logo-row');
    });

    it("login button present when not logged in", () => {
        const current_page = window.location;
        const loginUrl = `https://${current_page.host}/Shibboleth.sso/Login?target=${encodeURIComponent(current_page)}`;
        expect(wrapper.html()).toContain(loginUrl);
    });

    it("logout button present when logged in", () => {
        const current_page = window.location;
        const logoutUrl = `https://${current_page.host}/Shibboleth.sso/Logout?return=https://sso.unc.edu/idp/logout.jsp?return_url=${encodeURIComponent(current_page)}`;
        wrapper = mount(dcrHeader, {
            global: {
                plugins: [createTestingPinia({
                    initialState: {
                        access: {
                            isLoggedIn: true,
                            username: 'test_user',
                            viewAdmin: false
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
        expect(wrapper.html()).toContain(logoutUrl);
    });

    it("admin link present when user logged in as admin", () => {
        const adminUrl = `https://${window.location.host}/admin/`;
        wrapper = mount(dcrHeader, {
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
        expect(wrapper.html()).toContain(adminUrl);
    });
});