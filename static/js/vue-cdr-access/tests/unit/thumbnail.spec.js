import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import thumbnail from '@/components/full_record/thumbnail.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from "vue-i18n";
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
        thumbnail_url: "https://localhost:8080/services/api/thumb/fc77a9be-b49d-4f4e-b656-1644c9e964fc/large",
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
        id: "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
        updated: "2023-02-21T18:37:17.705Z",
        status: [
            "Patron Settings"
        ],
        timestamp: 1678973288810
    },
    markedForDeletion: false
};

let wrapper, router;

describe('thumbnail.vue', () => {
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

        wrapper = mount(thumbnail, {
            global: {
                plugins: [i18n, router]
            },
            props: {
                thumbnailData: recordData
            }
        });
    });

    it('displays a thumbnail, if present', () => {
        expect(wrapper.find('.thumbnail-preview img').exists()).toBe(true);
        expect(wrapper.find('a').attributes('class'))
            .toEqual('thumbnail thumbnail-size-large has_tooltip')
    });

    it('displays a placeholder, if no thumbnail', async () => {
        let updatedRecordData = cloneDeep(recordData);
        updatedRecordData.briefObject.thumbnail_url = undefined;
        await wrapper.setProps({ thumbnailData: updatedRecordData });
        expect(wrapper.find('.thumbnail-placeholder .thumbnail-content-type').exists()).toBe(true);
        expect(wrapper.find('a').attributes('class'))
            .toEqual('thumbnail thumbnail-size-large placeholder thumbnail-resource-type-collection has_tooltip')
    });

    it('displays a "document" placeholder for files, if no thumbnail', async () => {
        let updatedRecordData = cloneDeep(recordData);
        updatedRecordData.briefObject.thumbnail_url = undefined;
        updatedRecordData.briefObject.type = 'File';
        await wrapper.setProps({ thumbnailData: updatedRecordData });
        expect(wrapper.find('a').attributes('class')).toContain('thumbnail-resource-type-document')
    });

    it('displays a "document" placeholder for works, if no thumbnail', async () => {
        let updatedRecordData = cloneDeep(recordData);
        updatedRecordData.briefObject.thumbnail_url = undefined;
        updatedRecordData.briefObject.type = 'Work';
        await wrapper.setProps({ thumbnailData: updatedRecordData });
        expect(wrapper.find('a').attributes('class')).toContain('thumbnail-resource-type-document')
    });

    it('displays a "document" placeholder for admin units resource types, if no thumbnail', async () => {
        let updatedRecordData = cloneDeep(recordData);
        updatedRecordData.briefObject.thumbnail_url = undefined;
        updatedRecordData.briefObject.type = 'AdminUnit';
        await wrapper.setProps({ thumbnailData: updatedRecordData });
        expect(wrapper.find('a').attributes('class')).toContain('thumbnail-resource-type-document')
    });

    it('displays placeholder text, if no thumbnail and format is set', async () => {
        let updatedRecordData = cloneDeep(recordData);
        updatedRecordData.briefObject.thumbnail_url = undefined;
        updatedRecordData.briefObject.format = ['Image'];
        await wrapper.setProps({ thumbnailData: updatedRecordData });
        expect(wrapper.find('.thumbnail-placeholder span').text()).toEqual('Image');
    });

    it('displays a lock icon if an item is restricted', async () => {
        await wrapper.setProps({ allowsFullAccess: false });
        expect(wrapper.find('.fa-lock').exists()).toBe(true);
    });

    it('displays a trash icon if an item is marked for deletion', async () => {
        let updatedRecordData = cloneDeep(recordData);
        updatedRecordData.markedForDeletion = true;
        await wrapper.setProps({ thumbnailData: updatedRecordData });
        expect(wrapper.find('.fa-trash').exists()).toBe(true);
        expect(wrapper.find('a').attributes('class'))
            .toEqual('thumbnail thumbnail-size-large deleted has_tooltip')
    });

    it('displays no icon if an item is not restricted or marked for deletion', () => {
        expect(wrapper.find('.fa-lock').exists()).toBe(false);
        expect(wrapper.find('.fa-trash').exists()).toBe(false);
    });

    it('sets the src for the image', () => {
        expect(wrapper.find('.thumbnail-preview img').attributes('src'))
            .toEqual('https://localhost:8080/services/api/thumb/fc77a9be-b49d-4f4e-b656-1644c9e964fc/large');
    });

    it('sets the url for the image', () => {
        expect(wrapper.find('a').attributes('href'))
            .toEqual('https://localhost/record/73bc003c-9603-4cd9-8a65-93a22520ef6a');
    });

    it('has aria label text', () => {
        expect(wrapper.find('a').attributes('aria-label')).toEqual('Visit testCollection')
    });

    it('has tooltip text for admin units, collections, folders and works', () => {
        expect(wrapper.find('a').attributes('title')).toEqual('View details for testCollection')
    });

    it('has tooltip text for files', async () => {
        let updatedRecordData = cloneDeep(recordData);
        updatedRecordData.briefObject.type = 'File';
        await wrapper.setProps({ thumbnailData: updatedRecordData });
        expect(wrapper.find('a').attributes('title')).toEqual('View testCollection')
    });

    it('has tooltip text for lists', async () => {
        let updatedRecordData = cloneDeep(recordData);
        updatedRecordData.briefObject.type = 'List';
        await wrapper.setProps({ thumbnailData: updatedRecordData });
        expect(wrapper.find('a').attributes('title')).toEqual('View the contents of testCollection')
    });
});