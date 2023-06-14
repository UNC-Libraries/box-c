import {mount} from '@vue/test-utils'
import fileDownload from '@/components/full_record/fileDownload.vue';
import cloneDeep from 'lodash.clonedeep';

const briefObject = {
    id: '4db695c0-5fd5-4abf-9248-2e115d43f57d',
    permissions: [
        'viewAccessCopies',
        'viewOriginal'
    ],
    datastream: [
        'original_file|image/jpeg|beez||694904|urn:sha1:0d48dadb5d61ae0d41b4998280a3c39577a2f94a||3000x2048',
        'jp2|image/jp2|4db695c0-5fd5-4abf-9248-2e115d43f57d.jp2|jp2|2189901|||'
    ]
};

let wrapper;

describe('fileDownload.vue', () => {
    beforeEach(() => {
        const div = document.createElement('div')
        div.id = 'root'
        document.body.appendChild(div);

        wrapper = mount(fileDownload, {
            attachTo: '#root',
            props: {
                briefObject: briefObject
            }
        });
    });

    it('displays a download button if the user has viewAccessCopies permissions', () => {
       expect(wrapper.find('button').exists()).toBe(true);
    });

    it('does not display a download button if the user does not have viewAccessCopies permissions', async () => {
        let updatedBriefObj = cloneDeep(briefObject);
        updatedBriefObj.permissions = ['viewMetadata'];
        await wrapper.setProps({
            briefObject: updatedBriefObj
        });
        expect(wrapper.find('button').exists()).toBe(false);
    });

    it('does not display a download button if there is no original file', async () => {
        let updatedBriefObj = cloneDeep(briefObject);
        updatedBriefObj.datastream = [
            'jp2|image/jp2|4db695c0-5fd5-4abf-9248-2e115d43f57d.jp2|jp2|2189901|||'
        ]
        await wrapper.setProps({
            briefObject: updatedBriefObj
        });
        expect(wrapper.find('button').exists()).toBe(false);
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

    it('does not display an option to download original if user does not have viewOriginal access', async () => {
        let updatedBriefObj = cloneDeep(briefObject);
        updatedBriefObj.permissions = ['viewAccessCopies'];
        await wrapper.setProps({
            briefObject: updatedBriefObj
        });
        await wrapper.find('button').trigger('click');
        expect(wrapper.find('#image-download-options').isVisible()).toBe(true);
        let options = wrapper.findAll('a');
        expect(options.length).toEqual(4);
        assertHasOptionText(options[0], 'Small JPG (800px)');
        assertHasOptionText(options[1], 'Medium JPG (1600px)');
        assertHasOptionText(options[2], 'Large JPG (2500px)');
        assertHasOptionText(options[3], 'Full Size JPG');
    });

    it('hides the list of visible options when the options button is clicked', async () => {
        await wrapper.find('button').trigger('click'); // Open
        await wrapper.find('button').trigger('click'); // Close
        expect(wrapper.find('#image-download-options').classes('is-active')).toBe(false);
    });

   it('hides the list of visible options when any page element is clicked', async () => {
        await wrapper.find('button').trigger('click'); // Open
        await wrapper.trigger('click'); // Close
        expect(wrapper.find('#image-download-options').classes('is-active')).toBe(false);
    });

    it('hides the list of visible options when the "ESC" key is hit', async () => {
        await wrapper.find('button').trigger('click'); // Open
        await wrapper.trigger('keyup.esc'); // Close
        expect(wrapper.find('#image-download-options').classes('is-active')).toBe(false);
    });

    function assertHasOptionText(option, text) {
        expect(option.text()).toEqual(text);
    }
});