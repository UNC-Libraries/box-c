import {mount} from '@vue/test-utils'
import cloneDeep from 'lodash.clonedeep';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';
import fileDownload from '@/components/full_record/fileDownload.vue';

const briefObject = {
    id: '4db695c0-5fd5-4abf-9248-2e115d43f57d',
    permissions: [
        'viewAccessCopies',
        'viewOriginal'
    ],
    datastream: [
        'original_file|image/jpeg|beez||694904|urn:sha1:0d48dadb5d61ae0d41b4998280a3c39577a2f94a||3000x2048',
        'jp2|image/jp2|4db695c0-5fd5-4abf-9248-2e115d43f57d.jp2|jp2|2189901|||'
    ],
    format: ['Image']
};

let wrapper;

describe('fileDownload.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        const div = document.createElement('div')
        div.id = 'root'
        document.body.appendChild(div);

        wrapper = mount(fileDownload, {
            attachTo: '#root',
            global: {
                plugins: [i18n],
            },
            props: {
                briefObject: briefObject,
                downloadLink: '/content/0d48dadb5d61ae0d41b4998280a3c39577a2f94a?dl=true'
            }
        });
    });

    it('displays an image download button if the item is a image file and the user has viewAccessCopies permissions ', () => {
        expect(wrapper.find('#download-images').exists()).toBe(true);
        expect(wrapper.find('a.download').exists()).toBe(false);
    });

    it('does not display an image download button if the item is a image file and the user does not have viewAccessCopies permissions', async () => {
        let updatedBriefObj = cloneDeep(briefObject);
        updatedBriefObj.permissions = ['viewMetadata'];
        await wrapper.setProps({
            briefObject: updatedBriefObj
        });
        expect(wrapper.find('.button').exists()).toBe(false);
    });

    it('does not display a download button if there is no original file', async () => {
        let updatedBriefObj = cloneDeep(briefObject);
        updatedBriefObj.datastream = [
            'jp2|image/jp2|4db695c0-5fd5-4abf-9248-2e115d43f57d.jp2|jp2|2189901|||'
        ]
        await wrapper.setProps({
            briefObject: updatedBriefObj
        });
        expect(wrapper.find('.button').exists()).toBe(false);
    });

    it('displays a download button for image files with viewAccessCopies but not viewOriginal access', async () => {
        const updated_data = cloneDeep(briefObject);
        updated_data.permissions = ['viewAccessCopies'];
        await wrapper.setProps({
            briefObject: updated_data
        });
        expect(wrapper.find('#download-images').exists()).toBe(true);
        expect(wrapper.find('a.download').exists()).toBe(false);
    });

    it('does not display a download button for non-image files with viewAccessCopies but not viewOriginal access', async () => {
        const updated_data = cloneDeep(briefObject);
        updated_data.format = ['Text']
        updated_data.permissions = ['viewAccessCopies'];
        await wrapper.setProps({
            briefObject: updated_data
        });
        expect(wrapper.find('.button').exists()).toBe(false);
    });

    it('displays a list of download options when clicked', async () => {
        await wrapper.find('button').trigger('click');
        expect(wrapper.find('#image-download-options').classes('is-active')).toBe(true);
        let options = wrapper.findAll('a');
        expect(options.length).toEqual(5);
        assertHasOptionText(options[0], 'Small JPG (800px)');
        assertHasOptionText(options[1], 'Medium JPG (1600px)');
        assertHasOptionText(options[2], 'Large JPG (2500px)');
        assertHasOptionText(options[3], 'Full Size JPG');
        assertHasOptionText(options[4], 'Original File');
    });

    it('displays fewer download options if the original is smaller than a derivative size', async () => {
        let updatedBriefObj = cloneDeep(briefObject);
        updatedBriefObj.datastream[0] = 'original_file|image/jpeg|beez||694904|urn:sha1:0d48dadb5d61ae0d41b4998280a3c39577a2f94a||1000x2048';
        await wrapper.setProps({
            briefObject: updatedBriefObj
        });
        await wrapper.find('button').trigger('click');
        expect(wrapper.find('#image-download-options').classes('is-active')).toBe(true);
        let options = wrapper.findAll('a');
        expect(options.length).toEqual(4);
        assertHasOptionText(options[0], 'Small JPG (800px)');
        assertHasOptionText(options[1], 'Medium JPG (1600px)');
        assertHasOptionText(options[2], 'Full Size JPG');
        assertHasOptionText(options[3], 'Original File');
    });

    it('does not display an option to download full size or original if user does not have viewOriginal access', async () => {
        let updatedBriefObj = cloneDeep(briefObject);
        updatedBriefObj.permissions = ['viewAccessCopies'];
        await wrapper.setProps({
            briefObject: updatedBriefObj
        });
        await wrapper.find('button').trigger('click');
        expect(wrapper.find('#image-download-options').isVisible()).toBe(true);
        let options = wrapper.findAll('a');
        expect(options.length).toEqual(3);
        assertHasOptionText(options[0], 'Small JPG (800px)');
        assertHasOptionText(options[1], 'Medium JPG (1600px)');
        assertHasOptionText(options[2], 'Large JPG (2500px)');
    });

    it('hides the list of visible options when the options button is clicked', async () => {
        await wrapper.find('button').trigger('click'); // Open
        await wrapper.find('button').trigger('click'); // Close
        expect(wrapper.find('#image-download-options').classes('is-active')).toBe(false);
    });

   it('hides the list of visible options when any non dropdown page element is clicked', async () => {
        await wrapper.find('button').trigger('click'); // Open
        await wrapper.trigger('click'); // Close
        expect(wrapper.find('#image-download-options').classes('is-active')).toBe(false);
    });

    it('hides the list of visible options when the "ESC" key is hit', async () => {
        await wrapper.find('button').trigger('click'); // Open
        await wrapper.trigger('keyup.esc'); // Close
        expect(wrapper.find('#image-download-options').classes('is-active')).toBe(false);
    });

    it('displays a download link if the item is a non-image file', async () => {
        let updatedBriefObj = cloneDeep(briefObject);
        updatedBriefObj.format = ['Text'];
        await wrapper.setProps({
            briefObject: updatedBriefObj,
            resourceType: 'File',
            downloadLink: 'content/e6b92640-6847-45e4-9b64-e6f23e123c6a?dl=true'
        });
        expect(wrapper.find('a.download').exists()).toBe(true);
        expect(wrapper.find('#download-images').exists()).toBe(false);
    });

    it('does not show a download button if there is no download link', async () => {
        let updatedBriefObj = cloneDeep(briefObject);
        updatedBriefObj.format = ['Text'];
        await wrapper.setProps({
            briefObject: updatedBriefObj,
            downloadLink: '',
            resourceType: 'Work'
        });
        expect(wrapper.find('a.download').exists()).toBe(false);
        expect(wrapper.find('#download-images').exists()).toBe(false);
    });

    it('does not show a download option for a work if user does not have download permissions', async () => {
        const updated_data = cloneDeep(briefObject);
        updated_data.permissions = [];
        await wrapper.setProps({
            briefObject: updated_data,
            resourceType: 'Work'
        });
        expect(wrapper.find('a.download').exists()).toBe(false);
        expect(wrapper.find('#download-images').exists()).toBe(false);
    });

    function assertHasOptionText(option, text) {
        expect(option.text()).toEqual(text);
    }
});