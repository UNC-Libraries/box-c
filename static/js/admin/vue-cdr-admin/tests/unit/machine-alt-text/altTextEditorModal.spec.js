import { shallowMount } from '@vue/test-utils';
import { createTestingPinia } from '@pinia/testing';
import altTextEditorModal from '@/components/machine-alt-text/altTextEditorModal.vue';
import { useAltTextStore } from '@/stores/alt-text';

const mountModal = (initialState = {}) => {
    return shallowMount(altTextEditorModal, {
        global: {
            plugins: [createTestingPinia({
                initialState: {
                    'alt-text': {
                        activeField: '',
                        alertMessage: '',
                        alertMessageType: '',
                        currentRow: null,
                        currentUuid: null,
                        error: null,
                        items: [],
                        showAltTextModal: false,
                        viewType: 'view',
                        ...initialState
                    }
                },
                stubActions: false
            })]
        }
    });
};

let wrapper;

describe('altTextEditorModal.vue', () => {
    beforeEach(() => {
        wrapper = mountModal();
    });

    afterEach(() => {
        wrapper?.unmount();
        wrapper = null;
    });

    it('renders viewing mode with modal text and close label', () => {
        wrapper = mountModal({
            showAltTextModal: true,
            viewType: 'view',
            activeField: 'alt_text',
            currentRow: {
                alt_text: 'Existing alt text',
                filename: '/path/to/image-1.jpg'
            }
        });

        expect(wrapper.find('.modal').classes()).toContain('is-active');
        expect(wrapper.find('.modal-view').text()).toBe('Existing alt text');
        expect(wrapper.find('textarea').exists()).toBe(false);
        expect(wrapper.find('.button.is-danger').text()).toBe('Close');
        expect(wrapper.find('.modal-card-title').text()).toContain('Viewing alt text for image-1.jpg');
    });

    it('renders edit mode with textarea and cancel label', async () => {
        wrapper = mountModal({
            showAltTextModal: true,
            viewType: 'edit',
            activeField: 'alt_text',
            currentRow: {
                alt_text: 'Editable text',
                filename: '/path/to/image-2.jpg'
            }
        });

        await wrapper.vm.$nextTick();

        expect(wrapper.find('textarea').exists()).toBe(true);
        expect(wrapper.find('textarea').element.value).toBe('');
        expect(wrapper.find('.button.is-info').exists()).toBe(true);
        expect(wrapper.find('.button.is-danger').text()).toBe('Cancel');
        expect(wrapper.find('.modal-card-title').text()).toContain('Editing alt text for image-2.jpg');
    });

    it('updates the active field value and sets success alert on update', async () => {
        wrapper = mountModal({
            showAltTextModal: true,
            viewType: 'edit',
            activeField: 'alt_text',
            currentRow: {
                alt_text: 'Old text',
                filename: '/path/to/image-3.jpg'
            }
        });
        const store = useAltTextStore();

        await wrapper.vm.$nextTick();
        await wrapper.find('textarea').setValue('Updated alt text');
        await wrapper.find('.button.is-info').trigger('click');

        expect(store.currentRow.alt_text).toBe('Updated alt text');
        expect(store.alertMessage).toBe('Value updated successfully updated');
        expect(store.alertMessageType).toBe('success');
        expect(wrapper.vm.saving_data).toBe(false);
    });

    it('resets modal state when closed from header button', async () => {
        wrapper = mountModal({
            showAltTextModal: true,
            viewType: 'edit',
            activeField: 'alt_text',
            currentRow: {
                alt_text: 'To be cleared',
                filename: '/path/to/image-4.jpg'
            }
        });
        const store = useAltTextStore();

        await wrapper.find('.modal-card-head .delete').trigger('click');

        expect(store.showAltTextModal).toBe(false);
        expect(store.viewType).toBe('view');
        expect(store.activeField).toBe('');
        expect(store.currentRow).toBe(null);
    });

    it('refreshes edited text when current row changes while editing', async () => {
        wrapper = mountModal({
            showAltTextModal: true,
            viewType: 'edit',
            activeField: 'alt_text',
            currentRow: {
                alt_text: 'Initial text',
                filename: '/path/to/image-5.jpg'
            }
        });
        const store = useAltTextStore();

        await wrapper.vm.$nextTick();
        expect(wrapper.vm.updated_text).toBe('');

        store.setCurrentRow({
            alt_text: 'Row changed text',
            filename: '/path/to/image-6.jpg'
        });
        await wrapper.vm.$nextTick();

        expect(wrapper.vm.updated_text).toBe('Row changed text');
    });
});
