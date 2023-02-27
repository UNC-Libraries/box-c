import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import thumbnail from '@/components/full_record/thumbnail.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from "vue-i18n";
import translations from '@/translations';
import cloneDeep from 'lodash.clonedeep';

const recordData = {
    pageSubtitle: 'testCollection',
    briefObject: {
        pid: {
            id: 'fc77a9be-b49d-4f4e-b656-1644c9e964fc',
            qualifier: 'content',
            qualifiedId: 'content/fc77a9be-b49d-4f4e-b656-1644c9e964fc',
            componentId: 'fc77a9be-b49d-4f4e-b656-1644c9e964fc',
            repositoryUri: 'http://localhost:8181/fcrepo/rest/content/fc/77/a9/be/fc77a9be-b49d-4f4e-b656-1644c9e964fc',
            repositoryPath: 'http://localhost:8181/fcrepo/rest/content/fc/77/a9/be/fc77a9be-b49d-4f4e-b656-1644c9e964fc',
            pid: 'uuid:fc77a9be-b49d-4f4e-b656-1644c9e964fc',
            uri: 'http://localhost:8181/fcrepo/rest/content/fc/77/a9/be/fc77a9be-b49d-4f4e-b656-1644c9e964fc',
            uuid: 'fc77a9be-b49d-4f4e-b656-1644c9e964fc'
        },
        fields: {
            adminGroup: [
                ''
            ],
            filesizeTotal: 36698,
            parentUnit: 'testAdminUnit|353ee09f-a4ed-461e-a436-18a1bee77b01',
            readGroup: [
                'authenticated',
                'everyone'
            ],
            title: 'testCollection',
            contentStatus: [
                'Described'
            ],
            dateUpdated: 1676571326304,
            datastream: [
                'thumbnail_small|image/png|fc77a9be-b49d-4f4e-b656-1644c9e964fc.png|png|6768|||',
                'thumbnail_large|image/png|fc77a9be-b49d-4f4e-b656-1644c9e964fc.png|png|23535|||',
                'event_log|application/n-triples|event_log.nt|nt|5633|urn:sha1:afc8ffad2c76ceef21e6256a6c3d0b046a5ce5ce||',
                'md_descriptive_history|text/xml|||527|urn:sha1:563b7938cd45259679eb183ae2219820e287ccd8||',
                'md_descriptive|text/xml|md_descriptive.xml|xml|235|urn:sha1:bbf36218009159506760b51dd30467839d454277||'
            ],
            ancestorPath: [
                '1,collections',
                '2,353ee09f-a4ed-461e-a436-18a1bee77b01'
            ],
            dateCreated: 1041379200000,
            _version_: 1758374645159952400,
            ancestorIds: '/collections/353ee09f-a4ed-461e-a436-18a1bee77b01/fc77a9be-b49d-4f4e-b656-1644c9e964fc',
            lastIndexed: 1676662833090,
            id: 'fc77a9be-b49d-4f4e-b656-1644c9e964fc',
            keyword: [
                'fc77a9be-b49d-4f4e-b656-1644c9e964fc'
            ],
            roleGroup: [
                'canViewOriginals|authenticated',
                'canViewMetadata|everyone'
            ],
            timestamp: 1676916737707,
            status: [
                'Patron Settings'
            ],
            resourceType: 'Collection'
        },
        ancestorPathFacet: {
            fieldName: 'ANCESTOR_PATH',
            count: 0,
            displayValue: '353ee09f-a4ed-461e-a436-18a1bee77b01',
            facetNodes: [
                {
                    searchValue: '1,collections',
                    searchKey: 'collections',
                    facetValue: '1,collections',
                    tier: 1,
                    limitToValue: '1,collections!2',
                    displayValue: 'collections',
                    pivotValue: '2,'
                },
                {
                    searchValue: '2,353ee09f-a4ed-461e-a436-18a1bee77b01',
                    searchKey: '353ee09f-a4ed-461e-a436-18a1bee77b01',
                    facetValue: '2,353ee09f-a4ed-461e-a436-18a1bee77b01',
                    tier: 2,
                    limitToValue: '2,353ee09f-a4ed-461e-a436-18a1bee77b01!3',
                    displayValue: '353ee09f-a4ed-461e-a436-18a1bee77b01',
                    pivotValue: '3,'
                }
            ],
            searchKey: '353ee09f-a4ed-461e-a436-18a1bee77b01',
            searchValue: '2,353ee09f-a4ed-461e-a436-18a1bee77b01',
            limitToValue: '2,353ee09f-a4ed-461e-a436-18a1bee77b01!3',
            highestTier: 2,
            highestTierNode: {
                searchValue: '2,353ee09f-a4ed-461e-a436-18a1bee77b01',
                searchKey: '353ee09f-a4ed-461e-a436-18a1bee77b01',
                facetValue: '2,353ee09f-a4ed-461e-a436-18a1bee77b01',
                tier: 2,
                limitToValue: '2,353ee09f-a4ed-461e-a436-18a1bee77b01!3',
                displayValue: '353ee09f-a4ed-461e-a436-18a1bee77b01',
                pivotValue: '3,'
            },
            pivotValue: '3,'
        },
        path: {
            fieldName: 'ANCESTOR_PATH',
            count: 0,
            displayValue: 'fc77a9be-b49d-4f4e-b656-1644c9e964fc',
            facetNodes: [
                {
                    searchValue: '1,collections',
                    searchKey: 'collections',
                    facetValue: '1,collections',
                    tier: 1,
                    limitToValue: '1,collections!2',
                    displayValue: 'collections',
                    pivotValue: '2,'
                },
                {
                    searchValue: '2,353ee09f-a4ed-461e-a436-18a1bee77b01',
                    searchKey: '353ee09f-a4ed-461e-a436-18a1bee77b01',
                    facetValue: '2,353ee09f-a4ed-461e-a436-18a1bee77b01',
                    tier: 2,
                    limitToValue: '2,353ee09f-a4ed-461e-a436-18a1bee77b01!3',
                    displayValue: '353ee09f-a4ed-461e-a436-18a1bee77b01',
                    pivotValue: '3,'
                },
                {
                    searchValue: '3,fc77a9be-b49d-4f4e-b656-1644c9e964fc',
                    searchKey: 'fc77a9be-b49d-4f4e-b656-1644c9e964fc',
                    facetValue: '3,fc77a9be-b49d-4f4e-b656-1644c9e964fc',
                    tier: 3,
                    limitToValue: '3,fc77a9be-b49d-4f4e-b656-1644c9e964fc!4',
                    displayValue: 'fc77a9be-b49d-4f4e-b656-1644c9e964fc',
                    pivotValue: '4,'
                }
            ],
            searchKey: 'fc77a9be-b49d-4f4e-b656-1644c9e964fc',
            searchValue: '3,fc77a9be-b49d-4f4e-b656-1644c9e964fc',
            limitToValue: '3,fc77a9be-b49d-4f4e-b656-1644c9e964fc!4',
            highestTier: 3,
            highestTierNode: {
                searchValue: '3,fc77a9be-b49d-4f4e-b656-1644c9e964fc',
                searchKey: 'fc77a9be-b49d-4f4e-b656-1644c9e964fc',
                facetValue: '3,fc77a9be-b49d-4f4e-b656-1644c9e964fc',
                tier: 3,
                limitToValue: '3,fc77a9be-b49d-4f4e-b656-1644c9e964fc!4',
                displayValue: 'fc77a9be-b49d-4f4e-b656-1644c9e964fc',
                pivotValue: '4,'
            },
            pivotValue: '4,'
        },
        objectPath: {
            entries: [
                {
                    pid: 'collections',
                    name: 'Content Collections Root',
                    container: true
                },
                {
                    pid: '353ee09f-a4ed-461e-a436-18a1bee77b01',
                    name: 'testAdminUnit',
                    container: true
                },
                {
                    pid: 'fc77a9be-b49d-4f4e-b656-1644c9e964fc',
                    name: 'testCollection',
                    container: true
                }
            ]
        },
        ancestorNames: '/Content Collections Root/testAdminUnit/testCollection',
        datastreamObjects: [
            {
                owner: '',
                name: 'thumbnail_small',
                filesize: 6768,
                mimetype: 'image/png',
                filename: 'fc77a9be-b49d-4f4e-b656-1644c9e964fc.png',
                extension: 'png',
                checksum: '',
                extent: '',
                datastreamIdentifier: '/thumbnail_small'
            },
            {
                owner: '',
                name: 'thumbnail_large',
                filesize: 23535,
                mimetype: 'image/png',
                filename: 'fc77a9be-b49d-4f4e-b656-1644c9e964fc.png',
                extension: 'png',
                checksum: '',
                extent: '',
                datastreamIdentifier: '/thumbnail_large'
            },
            {
                owner: '',
                name: 'event_log',
                filesize: 5633,
                mimetype: 'application/n-triples',
                filename: 'event_log.nt',
                extension: 'nt',
                checksum: 'urn:sha1:afc8ffad2c76ceef21e6256a6c3d0b046a5ce5ce',
                extent: '',
                datastreamIdentifier: '/event_log'
            },
            {
                owner: '',
                name: 'md_descriptive_history',
                filesize: 527,
                mimetype: 'text/xml',
                filename: '',
                extension: '',
                checksum: 'urn:sha1:563b7938cd45259679eb183ae2219820e287ccd8',
                extent: '',
                datastreamIdentifier: '/md_descriptive_history'
            },
            {
                owner: '',
                name: 'md_descriptive',
                filesize: 235,
                mimetype: 'text/xml',
                filename: 'md_descriptive.xml',
                extension: 'xml',
                checksum: 'urn:sha1:bbf36218009159506760b51dd30467839d454277',
                extent: '',
                datastreamIdentifier: '/md_descriptive'
            }
        ],
        groupRoleMap: {
            authenticated: [
                'canViewOriginals'
            ],
            everyone: [
                'canViewMetadata'
            ]
        },
        countMap: {
            child: 1
        },
        thumbnailId: 'fc77a9be-b49d-4f4e-b656-1644c9e964fc',
        idWithoutPrefix: 'fc77a9be-b49d-4f4e-b656-1644c9e964fc',
        ancestorPath: [
            '1,collections',
            '2,353ee09f-a4ed-461e-a436-18a1bee77b01'
        ],
        datastream: [
            'thumbnail_small|image/png|fc77a9be-b49d-4f4e-b656-1644c9e964fc.png|png|6768|||',
            'thumbnail_large|image/png|fc77a9be-b49d-4f4e-b656-1644c9e964fc.png|png|23535|||',
            'event_log|application/n-triples|event_log.nt|nt|5633|urn:sha1:afc8ffad2c76ceef21e6256a6c3d0b046a5ce5ce||',
            'md_descriptive_history|text/xml|||527|urn:sha1:563b7938cd45259679eb183ae2219820e287ccd8||',
            'md_descriptive|text/xml|md_descriptive.xml|xml|235|urn:sha1:bbf36218009159506760b51dd30467839d454277||'
        ],
        roleGroup: [
            'canViewOriginals|authenticated',
            'canViewMetadata|everyone'
        ],
        id: 'fc77a9be-b49d-4f4e-b656-1644c9e964fc',
        keyword: [
            'fc77a9be-b49d-4f4e-b656-1644c9e964fc'
        ],
        timestamp: 1676916737707,
        status: [
            'Patron Settings'
        ],
        title: 'testCollection',
        resourceType: 'Collection',
        rollup: 'fc77a9be-b49d-4f4e-b656-1644c9e964fc',
        ancestorIds: '/collections/353ee09f-a4ed-461e-a436-18a1bee77b01/fc77a9be-b49d-4f4e-b656-1644c9e964fc',
        readGroup: [
            'authenticated',
            'everyone'
        ],
        adminGroup: [
            ''
        ],
        contentStatus: [
            'Described'
        ],
        dateAdded: 1673963549596,
        parentUnit: 'testAdminUnit|353ee09f-a4ed-461e-a436-18a1bee77b01'
    },
    markedForDeletion: false,
    resourceType: 'Collection'
}

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
        updatedRecordData.briefObject.thumbnailId = undefined;
        await wrapper.setProps({ thumbnailData: updatedRecordData });
        expect(wrapper.find('.thumbnail-placeholder .thumbnail-content-type').exists()).toBe(true);
        expect(wrapper.find('a').attributes('class'))
            .toEqual('thumbnail thumbnail-size-large placeholder thumbnail-resource-type-collection has_tooltip')
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
            .toEqual('https://localhost/services/api/thumb/fc77a9be-b49d-4f4e-b656-1644c9e964fc/large');
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
        updatedRecordData.resourceType = 'File';
        await wrapper.setProps({ thumbnailData: updatedRecordData });
        expect(wrapper.find('a').attributes('title')).toEqual('View testCollection')
    });

    it('has tooltip text for lists', async () => {
        let updatedRecordData = cloneDeep(recordData);
        updatedRecordData.resourceType = 'List';
        await wrapper.setProps({ thumbnailData: updatedRecordData });
        expect(wrapper.find('a').attributes('title')).toEqual('View the contents of testCollection')
    });
});