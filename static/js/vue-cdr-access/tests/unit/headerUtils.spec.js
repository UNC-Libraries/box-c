import {mount, RouterLinkStub} from '@vue/test-utils';
import headerSmall from '@/components/header/headerSmall.vue';

describe('headerSmallUtils', () => {
    it("jumpToAdminUrl is record-specific admin url", () => {
        const $store = {
            state: {
                isLoggedIn: true,
                username: 'test_user',
                viewAdmin: true
            },
            commit: jest.fn()
        };
        const current_page = window.location;
        const testUrl = `https://${current_page.hostname}/record/73bc003c-9603-4cd9-8a65-93a22520ef6a`;
        const adminUrl = `https://${current_page.hostname}/admin/list/73bc003c-9603-4cd9-8a65-93a22520ef6a`;
        window.location = Object.assign(new URL(testUrl));
        const wrapper = mount(headerSmall, {
            props: {
                isHomepage: false
            },
            global: {
                mocks: {
                    $store
                },
                stubs: {
                    RouterLink: RouterLinkStub
                }
            }
        });
        expect(wrapper.html()).toContain(adminUrl);
    });

    it("jumpToAdminUrl is admin url", () => {
        const $store = {
            state: {
                isLoggedIn: true,
                username: 'test_user',
                viewAdmin: true
            },
            commit: jest.fn()
        };
        const adminUrl = `https://${window.location.host}/admin/`;
        const wrapper = mount(headerSmall, {
            global: {
                mocks: {
                    $store
                },
                stubs: {
                    RouterLink: RouterLinkStub
                }
            }
        });
        expect(wrapper.html()).toContain(adminUrl);
    });
});