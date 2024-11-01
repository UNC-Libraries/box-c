import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import {createTestingPinia} from '@pinia/testing';
import { useAccessStore } from '@/stores/access';
import restrictedContent from '@/components/full_record/restrictedContent.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import singleUseLink from '@/components/full_record/singleUseLink.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';
import cloneDeep from 'lodash.clonedeep';

const record = {
    containingWorkUUID: "7d6c30fe-ca72-4362-931d-e9fe28a8ec83",
    briefObject: {
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
        parentCollectionName: "deansCollection",
        contentStatus: [
            "Not Described"
        ],
        rollup: "7d6c30fe-ca72-4362-931d-e9fe28a8ec83",
        objectPath: [
            {
                pid: "collections",
                name: "Content Collections Root",
                container: true
            },
            {
                pid: "353ee09f-a4ed-461e-a436-18a1bee77b01",
                name: "deansAdminUnit",
                container: true
            },
            {
                pid: "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                name: "deansCollection",
                container: true
            },
            {
                pid: "7d6c30fe-ca72-4362-931d-e9fe28a8ec83",
                name: "Bees",
                container: true
            },
            {
                pid: "4db695c0-5fd5-4abf-9248-2e115d43f57d",
                name: "beez",
                container: true
            }
        ],
        datastream: [
            "techmd_fits|text/xml|techmd_fits.xml|xml|4709|urn:sha1:5b0eabd749222a7c0bcdb92002be9fe3eff60128||",
            "original_file|image/jpeg|beez||694904|urn:sha1:0d48dadb5d61ae0d41b4998280a3c39577a2f94a||2848x1536",
            "jp2|image/jp2|4db695c0-5fd5-4abf-9248-2e115d43f57d.jp2|jp2|2189901|||",
            "event_log|application/n-triples|event_log.nt|nt|4334|urn:sha1:aabf004766f954db4ac4ab9aa0a115bb10b708b4||"
        ],
        parentCollectionId: "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
        ancestorPath: [
            {
                id: "collections",
                title: "collections"
            },
            {
                id: "353ee09f-a4ed-461e-a436-18a1bee77b01",
                title: "353ee09f-a4ed-461e-a436-18a1bee77b01"
            },
            {
                id: "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                title: "fc77a9be-b49d-4f4e-b656-1644c9e964fc"
            },
            {
                id: "7d6c30fe-ca72-4362-931d-e9fe28a8ec83",
                title: "7d6c30fe-ca72-4362-931d-e9fe28a8ec83"
            }
        ],
        permissions: [
            "viewMetadata"
        ],
        groupRoleMap: {
            authenticated: 'canViewOriginals',
            everyone: 'canViewMetadata'
        },
        id: "4db695c0-5fd5-4abf-9248-2e115d43f57d",
        updated: "2023-03-27T13:01:58.067Z",
        fileType: [
            "image/jpeg"
        ],
        status: [
            "Parent Is Embargoed",
            "Parent Has Staff-Only Access",
            "Inherited Patron Settings"
        ],
        timestamp: 1679922126871
    },
    viewerType: "clover",
    dataFileUrl: "content/4db695c0-5fd5-4abf-9248-2e115d43f57d",
    markedForDeletion: false,
    resourceType: "File"
}

let wrapper, router, store;

describe('restrictedContent.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        const div = document.createElement('div')
        div.id = 'root'
        document.body.appendChild(div);

        router = createRouter({
            history: createWebHistory(process.env.BASE_URL),
            routes: [
                {
                    path: '/record/:uuid',
                    name: 'displayRecords',
                    component: displayWrapper
                }
            ]
        });

        wrapper = mount(restrictedContent, {
            attachTo: '#root',
            global: {
                plugins: [i18n, router, createTestingPinia({
                    stubActions: false
                })]
            },
            props: {
                recordData: record
            }
        });
        store = useAccessStore();
    });

    afterEach(() => {
        store.$reset();
    });

    it('does not show view options if a user is logged in', async () => {
        logUserIn();
        expect(wrapper.find('.restricted-access').exists()).toBe(false);
    });

    it('shows an edit option if user has edit permissions', async () => {
        logUserIn();
        await setRecordPermissions(record, ['viewMetadata', 'viewAccessCopies', 'viewReducedResImages',
            'viewOriginal', 'viewHidden', 'editDescription']);
        expect(wrapper.find('a.edit').exists()).toBe(true);
    });

    it('does not show an edit option if user does not have edit permissions', async () => {
        await setRecordPermissions(record, []);

        expect(wrapper.find('a.edit').exists()).toBe(false);
    });

    it('does not show embargo info if there is no dataFileUrl', async () => {
        const updated_data = cloneDeep(record);
        updated_data.dataFileUrl = "";
        await wrapper.setProps({
            recordData: updated_data
        });
        expect(wrapper.find('.noaction').exists()).toBe(false);
    });

    it('shows a view option if user can view originals and resource is a file', async () => {
        await setRecordPermissions(record, ['viewMetadata', 'viewAccessCopies', 'viewReducedResImages',
            'viewOriginal']);

        expect(wrapper.find('a.view').exists()).toBe(true);
    });

    it('shows does not show view option if user can view originals and resource is a work', async () => {
        const updated_data = cloneDeep(record);
        updated_data.resourceType = 'Work';
        await wrapper.setProps({
            recordData: updated_data
        });
        expect(wrapper.find('a.view').exists()).toBe(false);
    });

    it('does not display a download button for works even with the showImageDownload permissions', async () => {
        const updated_data = cloneDeep(record);
        updated_data.dataFileUrl = 'content/4db695c0-5fd5-4abf-9248-2e115d43f57d';
        updated_data.resourceType = 'Work';
        await setRecordPermissions(updated_data, ['viewAccessCopies', 'viewReducedResImages', 'viewOriginal']);

        expect(wrapper.find('.download').exists()).toBe(false);
    });

    it('displays a single use link button for files with the proper permissions', async () => {
        const updated_data = cloneDeep(record);
        updated_data.briefObject.permissions = ['viewAccessCopies', 'viewHidden', 'viewOriginal'];
        await wrapper.setProps({
            recordData: updated_data
        });
        expect(wrapper.findComponent(singleUseLink).exists()).toBe(true);
    });

    it('does not display a single use link button for files without the proper permissions', async () => {
        const updated_data = cloneDeep(record);
        updated_data.dataFileUrl = 'content/4db695c0-5fd5-4abf-9248-2e115d43f57d';
        updated_data.resourceType = 'File';
        updated_data.briefObject.permissions = ['viewAccessCopies'];
        await wrapper.setProps({
            recordData: updated_data
        });
        expect(wrapper.findComponent(singleUseLink).exists()).toBe(false);
    });

    async function setRecordPermissions(rec, permissions) {
        const updated_data = cloneDeep(rec);
        updated_data.briefObject.permissions = permissions;
        wrapper.setProps({
            recordData: updated_data
        });
    }

    function logUserIn() {
        wrapper = mount(restrictedContent, {
            global: {
                plugins: [i18n, router, createTestingPinia({
                    initialState: {
                        access: {
                            isLoggedIn: true,
                            username: 'test_user'
                        }
                    },
                    stubActions: false
                })]
            },
            props: {
                recordData: record
            }
        });
        store = useAccessStore();
    }
});