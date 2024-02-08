import {mount, RouterLinkStub} from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import {createTestingPinia} from '@pinia/testing';
import { useAccessStore } from '@/stores/access';
import collectionFolderTemplate from '@/components/full_record/collectionFolderTemplate.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';
import cloneDeep from 'lodash.clonedeep';

const recordData = {
    briefObject: {
        added: "2023-01-17T13:52:29.596Z",
        counts: {
            child: 4
        },
        created: 1041379200000,
        title: "testCollection",
        type: "Collection",
        contentStatus: [
            "Described"
        ],
        rollup: "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
        objectPath: [
            {
                pid: "collections",
                name: "Content Collections Root",
                container: true
            },
            {
                pid: "353ee09f-a4ed-461e-a436-18a1bee77b01",
                name: "testAdminUnit",
                container: true
            },
            {
                pid: "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                name: "testCollection",
                container: true
            }
        ],
        datastream: [
            "thumbnail_small|image/png|fc77a9be-b49d-4f4e-b656-1644c9e964fc.png|png|6768|||",
            "thumbnail_large|image/png|fc77a9be-b49d-4f4e-b656-1644c9e964fc.png|png|23535|||",
            "event_log|application/n-triples|event_log.nt|nt|8206|urn:sha1:54fe67d57b965651e813eea1777c7f0332253168||",
            "md_descriptive_history|text/xml|||916|urn:sha1:efb4f2b6226d2932229f0e2b89128ec9a651de71||",
            "md_descriptive|text/xml|md_descriptive.xml|xml|283|urn:sha1:97f7dbdb806f724f9301445820ff1e0c9691cd6b||"
        ],
        ancestorPath: [
            {
                id: "collections",
                title: "collections"
            },
            {
                id: "353ee09f-a4ed-461e-a436-18a1bee77b01",
                title: "353ee09f-a4ed-461e-a436-18a1bee77b01"
            }
        ],
        _version_: 1760531096449056800,
        permissions: [
            "markForDeletionUnit",
            "move",
            "reindex",
            "destroy",
            "editResourceType",
            "destroyUnit",
            "bulkUpdateDescription",
            "changePatronAccess",
            "runEnhancements",
            "createAdminUnit",
            "ingest",
            "orderMembers",
            "viewOriginal",
            "viewReducedResImages",
            "viewAccessCopies",
            "viewMetadata",
            "viewHidden",
            "assignStaffRoles",
            "markForDeletion",
            "editDescription",
            "createCollection"
        ],
        groupRoleMap: {
            authenticated: [
                "canViewOriginals"
            ],
            everyone: [
                "canViewMetadata"
            ]
        },
        id: "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
        updated: "2023-02-21T18:37:17.705Z",
        status: [
            "Patron Settings"
        ],
        timestamp: 1678973288810
    },
    markedForDeletion: false,
    resourceType: "Collection"
};

let wrapper, router, store;

describe('collectionFolder.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(async () => {
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

        wrapper = mount(collectionFolderTemplate, {
            global: {
                plugins: [i18n, router, createTestingPinia({
                    stubActions: false
                })],
                stubs: {
                    RouterLink: RouterLinkStub
                }
            },
            props: {
                recordData: recordData
            }
        });

        store = useAccessStore();
    });

    afterEach(function () {
        store.$reset();
    });

    it('displays a header', () => {
        expect(wrapper.find('h2').text()).toBe('testCollection 4 items');
    });

    // First field is date added
    it('displays fields, if present', () => {
        expect(wrapper.find('p').text()).toBe('Date Added:  2023-01-17');
    });

    it('displays restricted access info if items are restricted', () => {
        expect(wrapper.find('.restricted-access').exists()).toBe(true);
    });

    it('displays a login link if authenticated users have full access to restricted items', () => {
        expect(wrapper.find('.login-link').text()).toEqual('Log in for access (UNC Onyen)');
    });

    it('does not display a login link if authenticated users don\'t have full access to restricted items', async () => {
        let updatedRecordData = cloneDeep(recordData);
        updatedRecordData.briefObject.groupRoleMap = {
            authenticated: [
                'canViewMetadata'
            ],
                everyone: [
                'canViewMetadata'
            ]
        };
        await wrapper.setProps({ recordData: updatedRecordData });
        expect(wrapper.find('.login-link').exists()).toBe(false);
    });

    it('displays a collection name with link, if present', async () => {
        let updatedRecordData = cloneDeep(recordData);
        updatedRecordData.briefObject.parentCollectionId = '7b7ff786-6772-4888-b020-e71261b926a6';
        updatedRecordData.briefObject.parentCollectionName = 'testCollection';
        updatedRecordData.briefObject.title = 'testFolder'
        updatedRecordData.briefObject.type = 'Folder';
        updatedRecordData.resourceType = 'Folder';
        await wrapper.setProps({ recordData: updatedRecordData });
        let collection_name_link = wrapper.find('.parent_collection a')
        expect(collection_name_link.text()).toEqual('testCollection')
        expect(collection_name_link.attributes('href')).toEqual('record/7b7ff786-6772-4888-b020-e71261b926a6')
    });

    it('displays a collection id, if present', async () => {
        let updatedRecordData = cloneDeep(recordData);
        updatedRecordData.briefObject.parentCollectionId = '7b7ff786-6772-4888-b020-e71261b926a6';
        updatedRecordData.briefObject.parentCollectionName = 'testFolder';
        updatedRecordData.briefObject.title = 'testFolder'
        updatedRecordData.briefObject.type = 'Folder';
        updatedRecordData.resourceType = 'Folder';
        await wrapper.setProps({ recordData: updatedRecordData });
        expect(wrapper.find('.parent_collection_id').text())
            .toEqual(expect.stringMatching(/Archival Collection ID:\s+7b7ff786-6772-4888-b020-e71261b926a6/))
    });

    it('displays a contact link if items are restricted', () => {
        expect(wrapper.find('.contact').text())
            .toEqual('Contact Wilson Library for access');
    });

    it('does not display restricted access info if items are unrestricted', async () => {
        let updatedRecordData = cloneDeep(recordData);
        updatedRecordData.briefObject.groupRoleMap = {
            authenticated: [
                'canViewOriginals'
            ],
            everyone: [
                'canViewOriginals'
            ]
        };
        await wrapper.setProps({ recordData: updatedRecordData });
        expect(wrapper.find('.restricted-access').exists()).toBe(false);
    });
});