import {mount, RouterLinkStub} from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import {createTestingPinia} from '@pinia/testing';
import { useAccessStore } from '@/stores/access';
import folderRecord from '@/components/full_record/folderRecord.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';
import cloneDeep from 'lodash.clonedeep';

const recordData = {
    briefObject: {
        added: "2022-06-15T16:42:42.925Z",
        counts: {
            child: 2
        },
        title: "Test Folder",
        type: "Folder",
        parentCollectionName: "Testing2",
        contentStatus: [
            "Described"
        ],
        rollup: "64acbfae-929f-4820-96da-65fdc19f529d",
        objectPath: [
            {
                pid: "collections",
                name: "Content Collections Root",
                container: true
            },
            {
                pid: "bfe93126-849a-43a5-b9d9-391e18ffacc6",
                name: "General Library Collections",
                container: true
            },
            {
                pid: "01cc3f06-7ba6-4270-b801-932f3b069610",
                name: "Testing",
                container: true
            },
            {
                pid: "64acbfae-929f-4820-96da-65fdc19f529d",
                name: "Test Folder",
                container: true
            }
        ],
        datastream: [
            "md_descriptive|text/xml|md_descriptive.xml|xml|191|urn:sha1:2f40cfb372dda202269cf227c88db88d3bf19b8c||",
            "event_log|application/n-triples|event_log.nt|nt|1599|urn:sha1:a1e278822844556df972c43beded080af0341b56||"
        ],
        parentCollectionId: "01cc3f06-7ba6-4270-b801-932f3b069610",
        ancestorPath: [
            {
                id: "collections",
                title: "collections"
            },
            {
                id: "bfe93126-849a-43a5-b9d9-391e18ffacc6",
                title: "bfe93126-849a-43a5-b9d9-391e18ffacc6"
            },
            {
                id: "01cc3f06-7ba6-4270-b801-932f3b069610",
                title: "01cc3f06-7ba6-4270-b801-932f3b069610"
            }
        ],
        permissions: [
            "viewAccessCopies",
            "viewOriginal",
            "viewMetadata",
            "viewReducedResImages"
        ],
        groupRoleMap: {
            authenticated: [
                "canViewOriginals"
            ],
            everyone: [
                "canViewMetadata"
            ]
        },
        id: "64acbfae-929f-4820-96da-65fdc19f529d",
        updated: "2023-06-26T12:37:49.701Z",
        status: [],
        timestamp: 1687783094875
    },
    markedForDeletion: false,
    resourceType: "Folder"
}

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

        wrapper = mount(folderRecord, {
            global: {
                plugins: [i18n, router, createTestingPinia({
                    stubActions: false
                })]
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
        expect(wrapper.find('h2').text()).toBe('Test Folder 2 items');
    });

    // First field is date added
    it('displays fields, if present', () => {
        expect(wrapper.find('p').text()).toBe('Date Added:  2022-06-15');
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
        await wrapper.setProps({ recordData: updatedRecordData });
        let collection_name_link = wrapper.find('.parent_collection a')
        expect(collection_name_link.text()).toEqual('testCollection')
        expect(collection_name_link.attributes('href')).toEqual('/record/7b7ff786-6772-4888-b020-e71261b926a6')
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