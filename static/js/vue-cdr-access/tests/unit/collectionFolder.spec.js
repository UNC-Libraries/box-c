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