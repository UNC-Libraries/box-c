import { shallowMount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import aggregateRecord from '@/components/full_record/aggregateRecord.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';
import moxios from 'moxios';
import cloneDeep from 'lodash.clonedeep';

const record = {
    briefObject: {
        added: "2023-03-07T14:47:46.863Z",
        counts: {
            child: 1
        },
        format: [
            "Audio"
        ],
        title: "Listen for real",
        type: "Work",
        fileDesc: [
            "MP3"
        ],
        parentCollectionName: "deansCollection",
        contentStatus: [
            "Described",
            "Has Primary Object",
            "Members Are Unordered"
        ],
        rollup: "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
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
                pid: "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
                name: "Listen for real",
                container: true
            }
        ],
        datastream: [
            "techmd_fits|text/xml|techmd_fits.xml|xml|4192|urn:sha1:e3150af2b1e846cc96a8e6da428ae619b1502240|8a2f05e5-d2b7-4857-ae71-c24aa28484c1|",
            "original_file|audio/mpeg|180618_003.MP3|MP3|35845559|urn:sha1:6ad9e78fef388cc7e1e6aa075eaf2cee3699f181|8a2f05e5-d2b7-4857-ae71-c24aa28484c1|",
            "md_descriptive|text/xml|md_descriptive.xml|xml|366|urn:sha1:bc6aa00b1b046a01298cd0dfff7ad250cc29a74d||",
            "event_log|application/n-triples|event_log.nt|nt|4521|urn:sha1:ed5db93d079a62f7a939396c721833808409748e||"
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
        id: "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
        updated: "2023-03-07T14:47:49.519Z",
        fileType: [
            "audio/mpeg"
        ],
        status: [
            "Inherited Patron Settings"
        ],
        timestamp: 1678973289392
    },
    viewerType: "audio",
    neighborList: [
        {
            added: "2023-01-17T13:53:47.387Z",
            created: 631152000000,
            format: [
                "Image"
            ],
            thumbnail_url: "https://localhost:8080/services/api/thumb/4053adf7-7bdc-4c9c-8769-8cc5da4ce81d/large",
            title: "Bees",
            type: "Work",
            fileDesc: [
                "JPEG Image"
            ],
            parentCollectionName: "deansCollection",
            contentStatus: [
                "Described",
                "Has Primary Object",
                "Members Are Unordered"
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
                }
            ],
            datastream: [
                "original_file|image/jpeg|bee1.jpg|jpg|69481|urn:sha1:87d7bed6cb33c87c589cfcdc2a2ce6110712fabb|4053adf7-7bdc-4c9c-8769-8cc5da4ce81d|607x1024",
                "techmd_fits|text/xml|techmd_fits.xml|xml|7013|urn:sha1:0c4a500c73146214d5fa08f278c0cdaadede79d0|4053adf7-7bdc-4c9c-8769-8cc5da4ce81d|",
                "jp2|image/jp2|4053adf7-7bdc-4c9c-8769-8cc5da4ce81d.jp2|jp2|415163||4053adf7-7bdc-4c9c-8769-8cc5da4ce81d|",
                "thumbnail_small|image/png|4053adf7-7bdc-4c9c-8769-8cc5da4ce81d.png|png|4802||4053adf7-7bdc-4c9c-8769-8cc5da4ce81d|",
                "thumbnail_large|image/png|4053adf7-7bdc-4c9c-8769-8cc5da4ce81d.png|png|16336||4053adf7-7bdc-4c9c-8769-8cc5da4ce81d|",
                "event_log|application/n-triples|event_log.nt|nt|4504|urn:sha1:b15940ee90b0f6d2b3ab1639eb7a266e54b621f2||",
                "md_descriptive|text/xml|md_descriptive.xml|xml|459|urn:sha1:17e8dec55960ac14f100541993793dc8da231788||",
                "md_descriptive_history|text/xml|||4976|urn:sha1:35e4b19ea3c58148c607b6537cc9af510406700c||"
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
                }
            ],
            _version_: 1760531096505680000,
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
            id: "7d6c30fe-ca72-4362-931d-e9fe28a8ec83",
            updated: "2023-01-17T14:04:34.419Z",
            fileType: [
                "image/jpeg"
            ],
            status: [
                "Inherited Patron Settings"
            ],
            timestamp: 1678973289513
        },
        {
            added: "2023-03-07T14:47:46.863Z",
            counts: {
                child: 1
            },
            format: [
                "Audio"
            ],
            title: "Listen for real",
            type: "Work",
            fileDesc: [
                "MP3"
            ],
            parentCollectionName: "deansCollection",
            contentStatus: [
                "Described",
                "Has Primary Object",
                "Members Are Unordered"
            ],
            rollup: "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
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
                    pid: "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
                    name: "Listen for real",
                    container: true
                }
            ],
            datastream: [
                "techmd_fits|text/xml|techmd_fits.xml|xml|4192|urn:sha1:e3150af2b1e846cc96a8e6da428ae619b1502240|8a2f05e5-d2b7-4857-ae71-c24aa28484c1|",
                "original_file|audio/mpeg|180618_003.MP3|MP3|35845559|urn:sha1:6ad9e78fef388cc7e1e6aa075eaf2cee3699f181|8a2f05e5-d2b7-4857-ae71-c24aa28484c1|",
                "md_descriptive|text/xml|md_descriptive.xml|xml|366|urn:sha1:bc6aa00b1b046a01298cd0dfff7ad250cc29a74d||",
                "event_log|application/n-triples|event_log.nt|nt|4521|urn:sha1:ed5db93d079a62f7a939396c721833808409748e||"
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
                }
            ],
            _version_: 1760531096497291300,
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
            id: "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
            updated: "2023-03-07T14:47:49.519Z",
            fileType: [
                "audio/mpeg"
            ],
            status: [
                "Inherited Patron Settings"
            ],
            timestamp: 1678973289392
        },
        {
            added: "2023-03-07T14:41:15.746Z",
            format: [
                "Video"
            ],
            title: "Listen!",
            type: "Work",
            fileDesc: [
                "MPEG"
            ],
            parentCollectionName: "deansCollection",
            contentStatus: [
                "Described",
                "Has Primary Object",
                "Members Are Unordered"
            ],
            rollup: "90d9b849-b9ff-4afc-910d-2833a9ed7850",
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
                    pid: "90d9b849-b9ff-4afc-910d-2833a9ed7850",
                    name: "Listen!",
                    container: true
                }
            ],
            datastream: [
                "techmd_fits|text/xml|techmd_fits.xml|xml|4598|urn:sha1:03b2309e865aff690806f53d34e7d58bfbd4bcdc|a2824907-7404-4266-925d-302f89c9d215|",
                "original_file|video/mpeg|Blood_rain_Swallow_loop__from_installation_.mpeg|mpeg|116736|urn:md5:5f7973ff9e4152827994d4149c8af39d|a2824907-7404-4266-925d-302f89c9d215|",
                "md_descriptive|text/xml|md_descriptive.xml|xml|358|urn:sha1:904729ec5b33b6a660f9f2375dfa1165e844f4ee||",
                "event_log|application/n-triples|event_log.nt|nt|4521|urn:sha1:4fa4a001f078f1ba50ada50f2ae21417821e5101||"
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
            id: "90d9b849-b9ff-4afc-910d-2833a9ed7850",
            updated: "2023-03-07T14:41:20.577Z",
            fileType: [
                "video/mpeg"
            ],
            status: [
                "Inherited Patron Settings"
            ],
            timestamp: 1678973289204
        },
        {
            added: "2023-03-03T20:34:01.799Z",
            format: [
                "Text"
            ],
            title: "Stuff",
            type: "Work",
            fileDesc: [
                "Portable Document Format"
            ],
            parentCollectionName: "deansCollection",
            contentStatus: [
                "Described",
                "Has Primary Object",
                "Members Are Unordered"
            ],
            rollup: "c9aec262-d793-4db2-9db6-f5ad4825c670",
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
                    pid: "c9aec262-d793-4db2-9db6-f5ad4825c670",
                    name: "Stuff",
                    container: true
                }
            ],
            datastream: [
                "original_file|application/pdf|COVID-Booster-Incentive-Leave-Attestation-Form.pdf|pdf|228201|urn:md5:db3ef1b490310aa3e0906d433cbc33a7|ee714894-2773-46a8-9ca8-b64922d84765|",
                "techmd_fits|text/xml|techmd_fits.xml|xml|4308|urn:sha1:d4f176ff6d78dccb4dd1ac00dc91be631f8a3104|ee714894-2773-46a8-9ca8-b64922d84765|",
                "fulltext|text/plain|ee714894-2773-46a8-9ca8-b64922d84765.txt|txt|2517||ee714894-2773-46a8-9ca8-b64922d84765|",
                "event_log|application/n-triples|event_log.nt|nt|4521|urn:sha1:97c651c37aee4c3905d545f155fa47687f90a111||",
                "md_descriptive|text/xml|md_descriptive.xml|xml|356|urn:sha1:66c48b391679049a8d57181643a9a671830a7811||"
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
            id: "c9aec262-d793-4db2-9db6-f5ad4825c670",
            updated: "2023-03-03T20:34:04.241Z",
            fileType: [
                "application/pdf"
            ],
            status: [
                "Inherited Patron Settings"
            ],
            timestamp: 1678973289046
        }
    ],
    viewerPid: "8a2f05e5-d2b7-4857-ae71-c24aa28484c1",
    dataFileUrl: "content/8a2f05e5-d2b7-4857-ae71-c24aa28484c1",
    markedForDeletion: false,
    resourceType: "Work"
}

let wrapper, router;

describe('aggregateRecord.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        moxios.install();
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

        wrapper = shallowMount(aggregateRecord, {
            global: {
                plugins: [i18n, router]
            },
            props: {
                recordData: record
            }
        });
    });

    afterEach(() => {
        wrapper = null;
        router = null
    });

    it("displays breadcrumbs", () => {
        expect(wrapper.findComponent({ name: 'breadCrumbs' }).exists()).toBe(true);
    });

    it("displays a work title", () => {
        expect(wrapper.find('h2').text()).toEqual('Listen for real');
    });

    it("does not display a finding aid link, if absent", () => {
        expect(wrapper.find('.finding-aid').exists()).toBe(false);
    });

    it("displays a finding aid link, if present", async () => {
        let updated_record = cloneDeep(record);
        updated_record.findingAidUrl = 'https://unc-finding-aid.lib.unc.edu';
        await wrapper.setProps({
            recordData: updated_record
        });
        expect(wrapper.find('.finding-aid').exists()).toBe(true);
    });

    it("does not allow users to edit the work by default", async () => {
        let updated_record = cloneDeep(record);
        updated_record.briefObject.permissions = [];
        await wrapper.setProps({
            recordData: updated_record
        });
        expect(wrapper.find('a.edit').exists()).toBe(false);
    });

    it("allows users to edit the work with the proper permissions", async () => {
        expect(wrapper.find('a.edit').exists()).toBe(true);
    });

    it("does not set a download link by default", async () => {
        let updated_record = cloneDeep(record);
        updated_record.briefObject.permissions = [];
        updated_record.briefObject.groupRoleMap = {
            everyone: 'canViewMetadata',
            authenticated: 'canViewAccessCopies'
        }
        await wrapper.setProps({
            recordData: updated_record
        });
        expect(wrapper.find('a.download').exists()).toBe(false);
    });

    it("allows users to download the work if a download link is present", async () => {
        let updated_record = cloneDeep(record);
        updated_record.briefObject.dataFileUrl = 'https://download-link.lib.unc.edu';
        await wrapper.setProps({
            recordData: updated_record
        });
        expect(wrapper.find('a.download').exists()).toBe(true);
    });

    it("displays an iframe viewer for images", async () => {
        let updated_record = cloneDeep(record);
        updated_record.viewerType = 'uv';
        updated_record.briefObject.groupRoleMap = {
            everyone: 'canViewAccessCopies'
        }
        await wrapper.setProps({
            recordData: updated_record
        });
        expect(wrapper.find('iframe').exists()).toBe(true);
    });

    it("displays an iframe viewer for images for logged in users", async () => {
        let updated_record = cloneDeep(record);
        updated_record.viewerType = 'uv';
        updated_record.briefObject.groupRoleMap = {
            everyone: 'canViewMetadata',
            authenticated: 'canViewAccessCopies'
        }
        await wrapper.setProps({
            recordData: updated_record,
            username: 'test_user'
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

    it("displays an iframe viewer for pdfs for logged in users", async () => {
        let updated_record = cloneDeep(record);
        updated_record.viewerType = 'pdf';
        updated_record.briefObject.groupRoleMap = {
            everyone: 'canViewMetadata',
            authenticated: 'canViewOriginals'
        }
        await wrapper.setProps({
            recordData: updated_record,
            username: 'test_user'
        });
        expect(wrapper.find('iframe').exists()).toBe(true);
    });

    it("uses the audio player component for audio files", async () => {
        let updated_record = cloneDeep(record);
        updated_record.briefObject.groupRoleMap = {
            everyone: 'canViewAccessCopies'
        }
        await wrapper.setProps({
            recordData: updated_record
        });
        expect(wrapper.findComponent({ name: 'audioPlayer' }).exists()).toBe(true);
    });

    it("uses the audio player component for audio files for logged in users", async () => {
        let updated_record = cloneDeep(record);
        updated_record.briefObject.groupRoleMap = {
            everyone: 'canViewMetadata',
            authenticated: 'canViewAccessCopies'
        }
        await wrapper.setProps({
            recordData: updated_record,
            username: 'test_user'
        });
        expect(wrapper.findComponent({ name: 'audioPlayer' }).exists()).toBe(true);
    });

    it("displays a list of files associated with the work", () => {
        expect(wrapper.findComponent({ name: 'fileList' }).exists()).toBe(true);
    });

    it("displays a list of neighbor works", () => {
        expect(wrapper.findComponent({ name: 'neighborList' }).exists()).toBe(true);
    });

    it("sets a link to its parent collection", () => {
        expect(wrapper.find('.parent-collection').attributes('href')).toEqual(`record/${record.briefObject.parentCollectionId}`);
    });

    it("loads record MODS metadata as html", (done) => {
        moxios.install();

        const html_data = "<table><tr><th>Title</th><td><p>Listen for real</p></td></tr></table>";
        const url = `record/${record.briefObject.id}/metadataView`;
        moxios.stubRequest(new RegExp(url), {
            status: 200,
            response: html_data
        });
        wrapper.vm.loadMetadata();

        moxios.wait(() => {
            expect(wrapper.vm.metadata).toEqual(html_data);
            expect(wrapper.find('#mods_data_display').text()).toEqual(expect.stringContaining('Listen for real'));
            done();
        });

        moxios.uninstall();
    });
});