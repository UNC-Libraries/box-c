import { mount } from '@vue/test-utils';
import dcrHeader from '@/components/dcrHeader.vue';

let wrapper;
describe('dcrHeader.vue', () => {
    const $store = {
        state: {
            isLoggedIn: false,
            username: '',
            viewAdmin: false
        },
        commit: jest.fn()
    };

    beforeEach(() => {
        wrapper = mount(dcrHeader, {
            global: {
                mocks: {
                    $store
                }
            }
        });
    });

    it("loads the dcrHeader", () => {
        expect(wrapper.find('header').exists()).toBe(true);
        expect(wrapper.html()).toContain('header');
    });

    it("loads headerHome on the home page", async () => {
        await wrapper.setProps({
            isHomepage: true
        });
        expect(wrapper.html()).toContain('logo-row');
    });

    it("loads headerSmall on the non-home pages", async () => {
        await wrapper.setProps({
            isHomepage: false
        });
        expect(wrapper.html()).toContain('logo-row-small');
    });

    it("login button present when not logged in", () => {
        const current_page = window.location;
        const loginUrl = `https://${current_page.host}/Shibboleth.sso/Login?target=${encodeURIComponent(current_page)}`;
        expect(wrapper.html()).toContain(loginUrl);
    });

    it("logout button present when logged in", () => {
        const $store = {
            state: {
                isLoggedIn: true,
                username: 'test_user',
                viewAdmin: false
            },
            commit: jest.fn()
        };
        const current_page = window.location;
        const logoutUrl = `https://${current_page.host}/Shibboleth.sso/Logout?return=https://sso.unc.edu/idp/logout.jsp?return_url=${encodeURIComponent(current_page)}`;
        wrapper = mount(dcrHeader, {
            global: {
                mocks: {
                    $store
                }
            }
        });
        expect(wrapper.html()).toContain(logoutUrl);
    });

    it("admin link present when user logged in as admin", () => {
        const $store = {
            state: {
                isLoggedIn: true,
                username: 'test_user',
                viewAdmin: true
            },
            commit: jest.fn()
        };
        const adminUrl = `https://${window.location.host}/admin/`;
        wrapper = mount(dcrHeader, {
            global: {
                mocks: {
                    $store
                }
            }
        });
        expect(wrapper.html()).toContain(adminUrl);
    });
});