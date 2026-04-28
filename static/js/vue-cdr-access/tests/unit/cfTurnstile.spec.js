import { shallowMount } from '@vue/test-utils';
import cfTurnstile from '@/components/cfTurnstile.vue';

const mountTurnstile = ({ route = null, token = '' } = {}) => {
    return shallowMount(cfTurnstile, {
        data() {
            return {
                error: false,
                token
            };
        },
        global: {
            stubs: {
                VueTurnstile: true
            },
            mocks: {
                $router: {
                    replace: vi.fn()
                },
                $route: route || {
                    redirectedFrom: {
                        path: '/record/123',
                        query: { rows: '10' }
                    }
                }
            }
        }
    });
};

describe('cfTurnstile.vue', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        fetchMock.resetMocks();
        window.turnstileSiteKey = 'site-key-123';
    });

    it('returns the configured site key from window', () => {
        const wrapper = mountTurnstile();
        expect(wrapper.vm.siteKey).toBe('site-key-123');
    });

    it('does not call challenge endpoint when token is missing', async () => {
        const wrapper = mountTurnstile({ token: '' });
        await wrapper.vm.turnstileCallBack();

        expect(fetchMock).not.toHaveBeenCalled();
    });

    it('calls challenge endpoint and redirects after successful validation', async () => {
        fetchMock.mockResponse(JSON.stringify({ data: { success: true } }));
        const wrapper = mountTurnstile();
        wrapper.vm.token = 'token-abc';

        await wrapper.vm.turnstileCallBack();

        expect(fetchMock).toHaveBeenCalledWith('/api/challenge', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ cfTurnstileToken: 'token-abc' })
        });
        expect(wrapper.vm.$router.replace).toHaveBeenCalledWith({
            path: '/record/123',
            query: { rows: '10' }
        });
    });

    it('does not redirect when challenge response is not successful', async () => {
        fetchMock.mockResponse(JSON.stringify({ data: { success: false } }));
        const wrapper = mountTurnstile();
        wrapper.vm.token = 'token-abc';

        await wrapper.vm.turnstileCallBack();

        expect(wrapper.vm.$router.replace).not.toHaveBeenCalled();
        expect(wrapper.vm.error).toBe(false);
    });

    it('sets error state when challenge request fails', async () => {
        fetchMock.mockRejectOnce(new Error('network failed'));
        const wrapper = mountTurnstile();
        wrapper.vm.token = 'token-abc';

        await wrapper.vm.turnstileCallBack();

        expect(wrapper.vm.error).toBe(true);
        expect(wrapper.find('.notification.is-danger').exists()).toBe(true);
    });

    it('resets error and token state before unmount', () => {
        const wrapper = mountTurnstile({ token: 'token-abc' });
        wrapper.vm.error = true;

        wrapper.vm.$options.beforeUnmount.call(wrapper.vm);

        expect(wrapper.vm.error).toBe(false);
        expect(wrapper.vm.token).toBe('');
    });
});
