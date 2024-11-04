import {mount, RouterLinkStub, shallowMount} from "@vue/test-utils";
import {createTestingPinia} from "@pinia/testing";
import {useAccessStore} from '@/stores/access';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';
import cloneDeep from 'lodash.clonedeep';
import downloadOptions from '@/components/full_record/downloadOptions.vue';

const record = {
        filesizeTotal: 694904,
        added: "2023-03-27T13:01:58.067Z",
        format: [
            "Image"
        ],
        title: "beez",
        type: "File",
        fileDesc: [
            "JPEG Image"
        ],
        datastream: [
            "techmd_fits|text/xml|techmd_fits.xml|xml|4709|urn:sha1:5b0eabd749222a7c0bcdb92002be9fe3eff60128||",
            "original_file|image/jpeg|beez||694904|urn:sha1:0d48dadb5d61ae0d41b4998280a3c39577a2f94a||2848x1536",
            "jp2|image/jp2|4db695c0-5fd5-4abf-9248-2e115d43f57d.jp2|jp2|2189901|||",
            "event_log|application/n-triples|event_log.nt|nt|4334|urn:sha1:aabf004766f954db4ac4ab9aa0a115bb10b708b4||"
        ],
        permissions: [
            "viewMetadata"
        ],
        groupRoleMap: {
            authenticated: 'canViewOriginals',
            everyone: 'canViewMetadata'
        },
        id: "4db695c0-5fd5-4abf-9248-2e115d43f57d",
        fileType: [
            "image/jpeg"
        ],
        status: [
            "Parent Is Embargoed",
            "Parent Has Staff-Only Access",
            "Inherited Patron Settings"
        ]
}

const div = document.createElement('div')
div.id = 'document'
document.body.appendChild(div);


let wrapper, store;

describe('downloadOption.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        wrapper = mount(downloadOptions, {
            attachTo: '#document',
            global: {
                plugins: [i18n, createTestingPinia({
                    stubActions: false,
                        initialState: {
                            access: {
                                isLoggedIn: true,
                                username: 'test_user'
                            }
                        }
                })],
                stubs: {
                    RouterLink: RouterLinkStub
                }
            },
            props: {
                recordData: record,
                t: jest.fn()
            }
        });
        store = useAccessStore();
    });

    afterEach(() => {
        store.$reset();
    });

    it('does not display a download button if there is no original file', async () => {
        let updated_data = cloneDeep(record);
        updated_data.datastream = [
            'jp2|image/jp2|4db695c0-5fd5-4abf-9248-2e115d43f57d.jp2|jp2|2189901|||'
        ]
        await wrapper.setProps({
            recordData: updated_data
        });
        expect(wrapper.find('.download').exists()).toBe(false);
    });

    it('displays a download button for non-image with the proper permissions', async () => {
        logUserIn();
        const updated_data = cloneDeep(record);
        updated_data.dataFileUrl = 'content/4db695c0-5fd5-4abf-9248-2e115d43f57d';
        updated_data.resourceType = 'File';
        updated_data.format = ['Text'];
        await setRecordPermissions(updated_data, ['viewAccessCopies', 'viewReducedResImages', 'viewOriginal']);

        expect(wrapper.find('.download.button.action').exists()).toBe(true);
        expect(wrapper.find('.download.button.action').attributes('href')).toEqual('/content/4db695c0-5fd5-4abf-9248-2e115d43f57d?dl=true');
    });

    it('displays a download button with all download options for image with viewOriginal', async () => {
        const updated_data = cloneDeep(record);
        updated_data.dataFileUrl = 'content/4db695c0-5fd5-4abf-9248-2e115d43f57d';
        updated_data.resourceType = 'File';
        await setRecordPermissions(updated_data, ['viewAccessCopies', 'viewReducedResImages', 'viewOriginal']);

        expect(wrapper.find('.dropdown-menu').exists()).toBe(true);
        await wrapper.find('button.download-images').trigger('click'); // Open
        const dropdown_items = wrapper.findAll('.dropdown-item');
        expect(dropdown_items[0].text()).toEqual('Small JPG (800px)');
        expect(dropdown_items[1].text()).toEqual('Medium JPG (1600px)');
        expect(dropdown_items[2].text()).toEqual('Large JPG (2500px)');
        expect(dropdown_items[3].text()).toEqual('Full Size JPG');
        expect(dropdown_items[4].text()).toEqual('Original File');
        expect(dropdown_items.length).toEqual(5);
    });

    it('displays a download button with reduced download options for image with only viewReducedResImages', async () => {
        const updated_data = cloneDeep(record);
        updated_data.dataFileUrl = 'content/4db695c0-5fd5-4abf-9248-2e115d43f57d';
        updated_data.resourceType = 'File';
        await setRecordPermissions(updated_data, ['viewAccessCopies', 'viewReducedResImages']);

        expect(wrapper.find('.dropdown-menu').exists()).toBe(true);

        await wrapper.find('button.download-images').trigger('click'); // Open
        const dropdown_items = wrapper.findAll('.dropdown-item');
        expect(dropdown_items[0].text()).toEqual('Small JPG (800px)');
        expect(dropdown_items[1].text()).toEqual('Medium JPG (1600px)');
        expect(dropdown_items[2].text()).toEqual('Large JPG (2500px)');
        expect(dropdown_items.length).toEqual(3);
    });

    it('shows a download button with no reduced download options when viewOriginal set and image is smaller than min size', async () => {
        const updated_data = cloneDeep(record);
        updated_data.dataFileUrl = 'content/4db695c0-5fd5-4abf-9248-2e115d43f57d';
        updated_data.resourceType = 'File';
        updated_data.datastream[1] = "original_file|image/jpeg|tinyz||69490|urn:sha1:0d48dadb5d61ae0d41b4998280a3c39577a2f94a||640x480";
        await setRecordPermissions(updated_data, ['viewAccessCopies', 'viewReducedResImages', 'viewOriginal']);

        expect(wrapper.find('.dropdown-menu').exists()).toBe(true);
        await wrapper.find('button.download-images').trigger('click'); // Open
        const dropdown_items = wrapper.findAll('.dropdown-item');
        expect(dropdown_items[0].text()).toEqual('Full Size JPG');
        expect(dropdown_items[1].text()).toEqual('Original File');
        expect(dropdown_items.length).toEqual(2);
    });

    it('hides a download button with viewReducedResImages when image is smaller than min size', async () => {
        const updated_data = cloneDeep(record);
        updated_data.dataFileUrl = 'content/4db695c0-5fd5-4abf-9248-2e115d43f57d';
        updated_data.resourceType = 'File';
        updated_data.datastream[1] = "original_file|image/jpeg|tinyz||69490|urn:sha1:0d48dadb5d61ae0d41b4998280a3c39577a2f94a||640x480";
        await setRecordPermissions(updated_data, ['viewAccessCopies', 'viewReducedResImages']);

        expect(wrapper.find('.dropdown-menu').exists()).toBe(false);
    });

    it('shows a download button with partial reduced download options with viewReducedResImages when image is smaller than largest size', async () => {
        const updated_data = cloneDeep(record);
        updated_data.dataFileUrl = 'content/4db695c0-5fd5-4abf-9248-2e115d43f57d';
        updated_data.resourceType = 'File';
        updated_data.datastream[1] = "original_file|image/jpeg|midz||69490|urn:sha1:0d48dadb5d61ae0d41b4998280a3c39577a2f94a||1700x1200";
        await setRecordPermissions(updated_data, ['viewAccessCopies', 'viewReducedResImages']);

        expect(wrapper.find('.dropdown-menu').exists()).toBe(true);
        await wrapper.find('button.download-images').trigger('click'); // Open
        const dropdown_items = wrapper.findAll('.dropdown-item');
        expect(dropdown_items[0].text()).toEqual('Small JPG (800px)');
        expect(dropdown_items[1].text()).toEqual('Medium JPG (1600px)');
        expect(dropdown_items.length).toEqual(2);
    });

    it("sets download options for image files with canViewOriginal permission from JP2 dimensions", async () => {
        let updatedBriefObj = cloneDeep(record);
        updatedBriefObj.permissions = [
            "viewAccessCopies",
            "viewMetadata",
            "viewReducedResImages",
            "viewOriginal"
        ];
        updatedBriefObj.datastream = [
            "original_file|image/jpeg|beez||694904|urn:sha1:0d48dadb5d61ae0d41b4998280a3c39577a2f94a||2048x1536",
            "jp2|image/jp2|4db695c0-5fd5-4abf-9248-2e115d43f57d.jp2|jp2|2189901|||1024x800"
        ];
        updatedBriefObj.groupRoleMap = {
            authenticated: ["canViewOriginals"],
            everyone: ["canViewOriginals"]
        };

        await  wrapper.setProps({
            recordData: updatedBriefObj
        });

        expect(wrapper.find('.dropdown-menu').exists()).toBe(true);
        await wrapper.find('button.download-images').trigger('click'); // Open
        const dropdown_items = wrapper.findAll('.dropdown-item');
        expect(dropdown_items[0].text()).toEqual('Small JPG (800px)');
        expect(dropdown_items[1].text()).toEqual('Full Size JPG');
        expect(dropdown_items[2].text()).toEqual('Original File');
        expect(dropdown_items.length).toEqual(3);
    });

    it("sets download options for image files with canViewOriginal permission from Original file if JP2 has no listed dimensions", async () => {
        let updatedBriefObj = cloneDeep(record)
        updatedBriefObj.groupRoleMap = {
            authenticated: ["canViewOriginals"],
            everyone: ["canViewOriginals"]
        };
        await setRecordPermissions(updatedBriefObj, ['viewAccessCopies', 'viewMetadata', 'viewReducedResImages', 'viewOriginal']);

        expect(wrapper.find('.dropdown-menu').exists()).toBe(true);
        await wrapper.find('button.download-images').trigger('click'); // Open
        const dropdown_items = wrapper.findAll('.dropdown-item');
        expect(dropdown_items[0].text()).toEqual('Small JPG (800px)');
        expect(dropdown_items[1].text()).toEqual('Medium JPG (1600px)');
        expect(dropdown_items[2].text()).toEqual('Large JPG (2500px)');
        expect(dropdown_items[3].text()).toEqual('Full Size JPG');
        expect(dropdown_items[4].text()).toEqual('Original File');
        expect(dropdown_items.length).toEqual(5);
    });

    it("sets download options for non-image files", async () => {
        let updatedBriefObj = cloneDeep(record)
        updatedBriefObj.fileType = ['application/pdf']
        updatedBriefObj.format = ['Text']
        updatedBriefObj.permissions = [
            "viewAccessCopies",
            "viewMetadata",
            "viewReducedResImages",
            "viewOriginal"
        ]
        updatedBriefObj.datastream = ['original_file|application/pdf|pdf file||416330|urn:sha1:4945153c9f5ce152ef8eda495deba043f536f388||'];

        await wrapper.setProps({
            recordData: updatedBriefObj
        });
        // Download button
        expect(wrapper.find('.download.button').exists()).toBe(true);
    });

    it("does not show a button for non-image files without viewOriginal permission", async () => {
        let updatedBriefObj = cloneDeep(record)
        updatedBriefObj.fileType = ['application/pdf']
        updatedBriefObj.format = ['Text']
        updatedBriefObj.datastream = ['original_file|application/pdf|pdf file||416330|urn:sha1:4945153c9f5ce152ef8eda495deba043f536f388||'];

        await wrapper.setProps({
            recordData: updatedBriefObj
        });
        expect(wrapper.find('div.download').exists()).toBe(false);
    });

    it("does not show a button for image files without viewOriginal permission", async () => {
        let updatedBriefObj = cloneDeep(record)
        updatedBriefObj.permissions = [
            "viewMetadata"
        ];

        await wrapper.setProps({
            recordData: updatedBriefObj
        });
        expect(wrapper.find('div.image-download-options').exists()).toBe(false);
    });

    it('shows a login modal', async () => {
        await setRecordPermissions(record, ['viewAccessCopies', 'viewReducedResImages', 'viewOriginal']);
        await store.$patch({ username: '' })
        await store.$patch({ isLoggedIn: false })
        await wrapper.find('.login-modal-link').trigger('click'); // Open
        expect(wrapper.find('.modal').classes('is-active')).toBe(true);
    });

    it('closes the login modal', async () => {
        await setRecordPermissions(record, ['viewAccessCopies', 'viewReducedResImages', 'viewOriginal']);
        await store.$patch({ username: '' });
        await store.$patch({ isLoggedIn: false });
        // await wrapper.find('.login-modal-link').trigger('click'); // Open
        await wrapper.find('button.close').trigger('click'); // Close
        expect(wrapper.find('.modal').classes('is-active')).toBe(false);
    });

    it('hides the list of visible options when any non dropdown page element is clicked', async () => {
        await setRecordPermissions(record, ['viewAccessCopies', 'viewReducedResImages', 'viewOriginal']);

        await wrapper.find('.login-link').trigger('click'); // Open
        await wrapper.trigger('click'); // Close
        expect(wrapper.find('.modal').classes('is-active')).toBe(false);
    });

    it('hides the list of visible options when the "ESC" key is hit', async () => {
        await setRecordPermissions(record, ['viewAccessCopies', 'viewReducedResImages', 'viewOriginal']);
        await store.$patch({ username: '' });
        await store.$patch({ isLoggedIn: false });
        await wrapper.find('.login-modal-link').trigger('click'); // Open
        await wrapper.trigger('keyup.esc'); // Close
        expect(wrapper.find('.modal').classes('is-active')).toBe(false);
    });

    async function setRecordPermissions(rec, permissions) {
        const updated_data = cloneDeep(rec);
        updated_data.permissions = permissions;
        await wrapper.setProps({
            recordData: updated_data
        });
    }

    function logUserIn() {
        wrapper = mount(downloadOptions, {
            global: {
                plugins: [i18n, createTestingPinia({
                    initialState: {
                        access: {
                            isLoggedIn: true,
                            username: 'test_user'
                        }
                    },
                    stubActions: false
                })],

            },
            props: {
                recordData: record,
                t: jest.fn()
            }
        });
        store = useAccessStore();
    }
});