import {mount, RouterLinkStub} from '@vue/test-utils';
import {createTestingPinia} from '@pinia/testing';
import { reactive } from 'vue';
import { useAccessStore } from '@/stores/access';
import headerSmall from '@/components/header/headerSmall.vue';

let wrapper, store, routeMock;
describe('headerSmallUtils', () => {
    beforeEach(() => {
        routeMock = reactive({
            name: 'searchRecords',
            params: {}
        });

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
                mocks: {
                    $route: routeMock
                },
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
        const id = '73bc003c-9603-4cd9-8a65-93a22520ef6a';
        const adminUrl = `https://${window.location.hostname}/admin/list/${id}`;

        const recordWrapper = mount(headerSmall, {
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
                mocks: {
                    $route: {
                        name: 'displayRecords',
                        params: { id }
                    }
                },
                stubs: {
                    RouterLink: RouterLinkStub
                }
            }
        });

        expect(recordWrapper.html()).toContain(adminUrl);
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

    it("jumpToAdminUrl updates when route changes", async () => {
        const id = '2b1f6e34-7d3e-4db6-a0d8-2fca9643f9bd';
        const adminBaseUrl = `https://${window.location.host}/admin/`;
        const recordAdminUrl = `https://${window.location.hostname}/admin/list/${id}`;

        expect(wrapper.html()).toContain(adminBaseUrl);

        routeMock.name = 'displayRecords';
        routeMock.params = { id };
        await wrapper.vm.$nextTick();

        expect(wrapper.html()).toContain(recordAdminUrl);
    });
});