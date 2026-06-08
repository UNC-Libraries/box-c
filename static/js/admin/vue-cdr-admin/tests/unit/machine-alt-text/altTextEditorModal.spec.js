import { shallowMount, flushPromises } from '@vue/test-utils';
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

const findButtonByText = (buttonText) => wrapper.findAll('button')
    .find(button => button.text() === buttonText);

let wrapper;

describe('altTextEditorModal.vue', () => {
    beforeEach(() => {
        global.fetchMock.resetMocks();
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
                title: 'image-1.jpg'
            }
        });

        expect(wrapper.find('.modal').classes()).toContain('is-active');
        expect(wrapper.find('.modal-view').text()).toBe('Existing alt text');
        expect(wrapper.find('textarea').exists()).toBe(false);
        expect(findButtonByText('Edit Text')?.exists()).toBe(true);
        expect(findButtonByText('Update Text')).toBeUndefined();
        expect(wrapper.find('.button.is-danger').text()).toBe('Close');
        expect(wrapper.find('.modal-card-title').text()).toContain('Viewing alt text for image-1.jpg');
    });

    it('returns an empty fieldTitle when activeField is null', () => {
        wrapper = mountModal({
            showAltTextModal: true,
            viewType: 'view',
            activeField: null,
            currentRow: {
                title: 'image-null-field.jpg'
            }
        });

        expect(wrapper.vm.fieldTitle).toBe('');
    });

    it('switches from viewing mode to editing mode when edit text is clicked', async () => {
        wrapper = mountModal({
            showAltTextModal: true,
            viewType: 'view',
            activeField: 'alt_text',
            currentRow: {
                alt_text: 'Big image',
                title: 'image-view.jpg'
            }
        });
        const store = useAltTextStore();

        await findButtonByText('Edit Text').trigger('click');
        await wrapper.vm.$nextTick();

        expect(store.viewType).toBe('edit');
        expect(wrapper.find('textarea').exists()).toBe(true);
        expect(wrapper.find('textarea').element.value).toBe('Big image');
        expect(findButtonByText('Edit Text')).toBeUndefined();
        expect(findButtonByText('Update Text')?.exists()).toBe(true);
    });

    it('renders edit mode with textarea and cancel label', async () => {
        wrapper = mountModal({
            showAltTextModal: true,
            viewType: 'edit',
            activeField: 'alt_text',
            currentRow: {
                alt_text: 'Strange picture',
                title: 'image-2.jpg'
            }
        });

        await wrapper.vm.$nextTick();

        expect(wrapper.find('textarea').exists()).toBe(true);
        expect(wrapper.find('textarea').element.value).toBe('');
        expect(findButtonByText('Update Text')?.exists()).toBe(true);
        expect(findButtonByText('Edit Text')).toBeUndefined();
        expect(wrapper.find('.button.is-danger').text()).toBe('Cancel');
        expect(wrapper.find('.modal-card-title').text()).toContain('Editing alt text for image-2.jpg');
    });

    it('updates the active field value and sets success alert on update', async () => {
         wrapper = mountModal({
             showAltTextModal: true,
             viewType: 'edit',
             activeField: 'mgDescription',
             currentUuid: 'uuid-3',
             currentRow: {
                 id: 'abc-123',
                 mgDescription: 'Old text',
                 title: 'image-3.jpg'
             }
         });
         const store = useAltTextStore();
         global.fetchMock.mockResponseOnce(JSON.stringify({}));
 
         await wrapper.vm.$nextTick();
         await wrapper.find('textarea').setValue('Updated alt text');
         await findButtonByText('Update Text').trigger('click');
         await flushPromises();
 
         expect(store.setCurrentRowFieldValue).toHaveBeenCalledWith('mgDescription', 'Updated alt text');
         expect(store.setLastSuccessfulEdit).toHaveBeenCalledWith({
             id: 'abc-123',
             field: 'mgDescription',
             value: 'Updated alt text'
         });
         expect(store.alertMessage).toBe('mgDescription updated successfully for image-3.jpg');
         expect(store.alertMessageType).toBe('success');
         expect(store.currentRow).toBe(null);
         expect(wrapper.vm.saving_data).toBe(false);
     });

    it('sends FormData with urlencoded content-type on update', async () => {
         wrapper = mountModal({
             showAltTextModal: true,
             viewType: 'edit',
             activeField: 'alt_text',
             currentUuid: 'uuid-4',
             currentRow: {
                 id: 'row-456',
                 alt_text: 'Old text',
                 title: 'image-4.jpg'
             }
         });
         global.fetchMock.mockResponseOnce(JSON.stringify({}));
 
         await wrapper.vm.$nextTick();
         await wrapper.find('textarea').setValue('New alt text');
         await findButtonByText('Update Text').trigger('click');
         await flushPromises();
 
         expect(global.fetchMock).toHaveBeenCalledWith(
             expect.stringContaining('/services/api/edit/alt_text/row-456'),
             expect.objectContaining({
                 method: 'POST',
                 headers: {
                     'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
                 }
             })
         );
    });

    it('handles update errors and sets error alert', async () => {
         wrapper = mountModal({
             showAltTextModal: true,
             viewType: 'edit',
             activeField: 'alt_text',
             currentUuid: 'uuid-5',
             currentRow: {
                 id: 'row-789',
                 alt_text: 'Text',
                 title: 'image-5.jpg'
             }
         });
         const store = useAltTextStore();
         global.fetchMock.mockRejectOnce(new Error('Network error'));
 
         await wrapper.vm.$nextTick();
         await wrapper.find('textarea').setValue('New text');
         await findButtonByText('Update Text').trigger('click');
         await flushPromises();
 
         expect(store.alertMessage).toBe('Unable to update alt_text for image-5.jpg');
         expect(store.alertMessageType).toBe('error');
         expect(wrapper.vm.saving_data).toBe(false);
    });

    it('resets modal state when closed from header button', async () => {
         wrapper = mountModal({
             showAltTextModal: true,
             viewType: 'edit',
             activeField: 'alt_text',
             currentRow: {
                 alt_text: 'To be cleared',
                 title: 'image-6.jpg'
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
                 title: 'image-7.jpg'
             }
         });
         const store = useAltTextStore();
 
         await wrapper.vm.$nextTick();
         expect(wrapper.vm.updated_text).toBe('');
 
         store.setCurrentRow({
             alt_text: 'Row changed text',
             filename: 'image-8.jpg'
         });
         await wrapper.vm.$nextTick();
 
         expect(wrapper.vm.updated_text).toBe('Row changed text');
     });
});
