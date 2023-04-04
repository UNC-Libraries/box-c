import { shallowMount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import player from '@/components/full_record/player.vue';
import displayWrapper from '@/components/displayWrapper.vue';
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
            "original_file|image/jpeg|beez||694904|urn:sha1:0d48dadb5d61ae0d41b4998280a3c39577a2f94a||2048x1536",
            "jp2|image/jp2|4db695c0-5fd5-4abf-9248-2e115d43f57d.jp2|jp2|2189901|||",
            "thumbnail_small|image/png|4db695c0-5fd5-4abf-9248-2e115d43f57d.png|png|6768|||",
            "thumbnail_large|image/png|4db695c0-5fd5-4abf-9248-2e115d43f57d.png|png|23535|||",
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
            "viewAccessCopies",
            "viewHidden",
            "assignStaffRoles",
            "viewMetadata",
            "markForDeletion",
            "editDescription",
            "createCollection"
        ],
        groupRoleMap: {},
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
    viewerType: "uv",
    neighborList: [
        {
            filesizeTotal: 69481,
            added: "2023-01-17T13:53:48.103Z",
            format: [
                "Image"
            ],
            thumbnail_url: "https://localhost:8080/services/api/thumb/4053adf7-7bdc-4c9c-8769-8cc5da4ce81d/large",
            title: "bee1.jpg",
            type: "File",
            fileDesc: [
                "JPEG Image"
            ],
            parentCollectionName: "deansCollection",
            contentStatus: [
                "Not Described",
                "Is Primary Object"
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
                    pid: "4053adf7-7bdc-4c9c-8769-8cc5da4ce81d",
                    name: "bee1.jpg",
                    container: true
                }
            ],
            datastream: [
                "original_file|image/jpeg|bee1.jpg|jpg|69481|urn:sha1:87d7bed6cb33c87c589cfcdc2a2ce6110712fabb||607x1024",
                "techmd_fits|text/xml|techmd_fits.xml|xml|7013|urn:sha1:0c4a500c73146214d5fa08f278c0cdaadede79d0||",
                "jp2|image/jp2|4053adf7-7bdc-4c9c-8769-8cc5da4ce81d.jp2|jp2|415163|||",
                "thumbnail_small|image/png|4053adf7-7bdc-4c9c-8769-8cc5da4ce81d.png|png|4802|||",
                "thumbnail_large|image/png|4053adf7-7bdc-4c9c-8769-8cc5da4ce81d.png|png|16336|||",
                "event_log|application/n-triples|event_log.nt|nt|5852|urn:sha1:8d80f0de467fa650d4bc8568d4a958e5ced85f96||"
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
                "viewAccessCopies",
                "viewHidden",
                "assignStaffRoles",
                "viewMetadata",
                "markForDeletion",
                "editDescription",
                "createCollection"
            ],
            groupRoleMap: {},
            id: "4053adf7-7bdc-4c9c-8769-8cc5da4ce81d",
            updated: "2023-03-27T16:43:35.724Z",
            fileType: [
                "image/jpeg"
            ],
            status: [
                "Marked For Deletion",
                "Parent Is Embargoed",
                "Parent Has Staff-Only Access"
            ],
            timestamp: 1679935418494
        },
        {
            filesizeTotal: 694904,
            added: "2023-03-27T13:01:58.067Z",
            format: [
                "Image"
            ],
            thumbnail_url: "https://localhost:8080/services/api/thumb/4db695c0-5fd5-4abf-9248-2e115d43f57d/large",
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
                "original_file|image/jpeg|beez||694904|urn:sha1:0d48dadb5d61ae0d41b4998280a3c39577a2f94a||2048x1536",
                "jp2|image/jp2|4db695c0-5fd5-4abf-9248-2e115d43f57d.jp2|jp2|2189901|||",
                "thumbnail_small|image/png|4db695c0-5fd5-4abf-9248-2e115d43f57d.png|png|6768|||",
                "thumbnail_large|image/png|4db695c0-5fd5-4abf-9248-2e115d43f57d.png|png|23535|||",
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
                "viewAccessCopies",
                "viewHidden",
                "assignStaffRoles",
                "viewMetadata",
                "markForDeletion",
                "editDescription",
                "createCollection"
            ],
            groupRoleMap: {},
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
        }
    ],
    dataFileUrl: "content/4db695c0-5fd5-4abf-9248-2e115d43f57d",
    markedForDeletion: false,
    resourceType: "File"
}

let wrapper, router;

describe('player.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
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

        wrapper = shallowMount(player, {
            global: {
                plugins: [i18n, router]
            },
            props: {
                recordData: record
            }
        });
    });

    it("displays an iframe viewer for images", async () => {
        let updated_record = cloneDeep(record);
        updated_record.briefObject.groupRoleMap = {
            everyone: 'canViewAccessCopies'
        }
        await wrapper.setProps({
            recordData: updated_record
        });
        expect(wrapper.find('iframe').exists()).toBe(true);
    });

    it("displays an iframe viewer for pdfs", async () => {
        let updated_record = cloneDeep(record);
        updated_record.viewerType = 'pdf';
        updated_record.briefObject.groupRoleMap = {
            everyone: 'canViewOriginals'
        }
        await wrapper.setProps({
            recordData: updated_record
        });
        expect(wrapper.find('iframe').exists()).toBe(true);
    });

    it("uses the audio player component for audio files", async () => {
        let updated_record = cloneDeep(record);
        updated_record.viewerType = 'audio';
        updated_record.briefObject.groupRoleMap = {
            everyone: 'canViewAccessCopies'
        }
        await wrapper.setProps({
            recordData: updated_record
        });
        expect(wrapper.findComponent({ name: 'audioPlayer' }).exists()).toBe(true);
    });

    it("does not display a viewer if no permissions are set", async () => {
        let updated_record = cloneDeep(record);
        updated_record.viewerType = 'audio';
        updated_record.briefObject.permissions = undefined
        await wrapper.setProps({
            recordData: updated_record
        });
        expect(wrapper.findComponent({ name: 'audioPlayer' }).exists()).toBe(false);
        expect(wrapper.find('iframe').exists()).toBe(false);
    });
});