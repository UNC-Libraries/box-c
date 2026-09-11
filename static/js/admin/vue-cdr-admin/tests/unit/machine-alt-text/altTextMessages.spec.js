import { shallowMount } from '@vue/test-utils';
import { createTestingPinia } from '@pinia/testing';
import altTextMessages from '@/components/machine-alt-text/altTextMessages.vue';
import { useAltTextStore } from '@/stores/alt-text';

const mountMessages = (initialState = {}) => {
    return shallowMount(altTextMessages, {
        global: {
            plugins: [createTestingPinia({
                initialState: {
                    'alt-text': {
                        alertMessage: '',
                        alertMessageType: '',
                        ...initialState
                    }
                },
                stubActions: false
            })]
        }
    });
};

describe('altTextMessages.vue', () => {
    it('hides the notification when there is no alert message', () => {
        const wrapper = mountMessages();

        expect(wrapper.find('#machine-alt-text').classes()).toContain('is-hidden');
    });

    it('shows the notification and message text when an alert message exists', () => {
        const wrapper = mountMessages({
            alertMessage: 'Alt text updated successfully',
            alertMessageType: 'success'
        });

        const notification = wrapper.find('#machine-alt-text');
        expect(notification.classes()).not.toContain('is-hidden');
        expect(notification.text()).toContain('Alt text updated successfully');
    });

    it('applies styling when alert type is success', () => {
        const wrapper = mountMessages({
            alertMessage: 'Alt text updated successfully',
            alertMessageType: 'success'
        });

        const notification = wrapper.find('#machine-alt-text');
        expect(notification.classes()).toContain('is-success');
        expect(notification.classes()).not.toContain('is-danger');
    });

    it('applies styling for non-success alert types', () => {
        const wrapper = mountMessages({
            alertMessage: 'Could not save alt text',
            alertMessageType: 'error'
        });

        const notification = wrapper.find('#machine-alt-text');
        expect(notification.classes()).toContain('is-danger');
        expect(notification.classes()).not.toContain('is-success');
    });

    it('clears and hides the message when the close button is clicked', async () => {
        const wrapper = mountMessages({
            alertMessage: 'Could not save alt text',
            alertMessageType: 'error'
        });
        const store = useAltTextStore();

        await wrapper.find('button.delete').trigger('click');
        await wrapper.vm.$nextTick();

        expect(store.alertMessage).toBe('');
        expect(store.alertMessageType).toBe('');
        expect(wrapper.find('#machine-alt-text').classes()).toContain('is-hidden');
    });

    it('keeps notification hidden even when type is success but message is empty', () => {
        const wrapper = mountMessages({
            alertMessage: '',
            alertMessageType: 'success'
        });

        expect(wrapper.find('#machine-alt-text').classes()).toContain('is-hidden');
    });
});

