import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import collectionFolder from '@/components/full_record/collectionFolder.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from 'vue-i18n';
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

        wrapper = mount(collectionFolder, {
            global: {
                plugins: [i18n, router]
            },
            props: {
                recordData: recordData
            }
        });
    });

    it('displays breadcrumbs', () => {
        expect(wrapper.find('#full_record_trail').text()).toEqual(expect.stringMatching(/Collection.*testAdminUnit.*testCollection/));
    });

    it('displays a header', () => {
        expect(wrapper.find('h2').text()).toBe('testCollection 1 item');
    });

    // First field is date added
    it('displays fields, if present', () => {
        expect(wrapper.find('p').text()).toBe('Date Added:  2023-01-17');
    });

    it('does not display fields, if not present', () => {
        expect(wrapper.find('h2').text()).toBe('testCollection 1 item');
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

    it('displays a contact link if items are restricted', () => {
        expect(wrapper.find('.contact').text())
            .toEqual('Contact Wilson Library for access');
    });

    it('does not display restricted access info if items are unrestricted', async () => {
        let updatedRecordData = cloneDeep(recordData);
        updatedRecordData.briefObject.roleGroup = [
            'canViewOriginals|authenticated',
            'canViewOriginals|everyone'
        ];
        await wrapper.setProps({ recordData: updatedRecordData });
        expect(wrapper.find('.restricted-access').exists()).toBe(false);
    });
});