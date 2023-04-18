import { shallowMount, RouterLinkStub } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import neighborList from '@/components/full_record/neighborList.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';

const neighbor_list = [
    {
        "pid": {
            "id": "7d6c30fe-ca72-4362-931d-e9fe28a8ec83",
            "componentId": "7d6c30fe-ca72-4362-931d-e9fe28a8ec83",
            "pid": "uuid:7d6c30fe-ca72-4362-931d-e9fe28a8ec83",
            "uuid": "7d6c30fe-ca72-4362-931d-e9fe28a8ec83"
        },
        "fields": {
            "adminGroup": [
                "unc:onyen:test_user"
            ],
            "readGroup": [
                "authenticated",
                "everyone",
                "unc:onyen:test_user"
            ],
            "title": "Bees",
            "status": [
                "Inherited Patron Settings"
            ],
            "parentCollection": "testCollection|fc77a9be-b49d-4f4e-b656-1644c9e964fc",
            "ancestorPath": [
                "1,collections",
                "2,353ee09f-a4ed-461e-a436-18a1bee77b01",
                "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc"
            ],
            "id": "7d6c30fe-ca72-4362-931d-e9fe28a8ec83",
            "roleGroup": [
                "canDescribe|unc:onyen:test_user",
                "canViewOriginals|authenticated",
                "canViewMetadata|everyone"
            ],
            "parentUnit": "testAdminUnit|353ee09f-a4ed-461e-a436-18a1bee77b01"
        },
        "objectPath": {
            "entries": [
                {
                    "pid": "collections",
                    "name": "Content Collections Root",
                    "container": true
                },
                {
                    "pid": "353ee09f-a4ed-461e-a436-18a1bee77b01",
                    "name": "testAdminUnit",
                    "container": true
                },
                {
                    "pid": "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                    "name": "testCollection",
                    "container": true
                },
                {
                    "pid": "7d6c30fe-ca72-4362-931d-e9fe28a8ec83",
                    "name": "Bees",
                    "container": true
                }
            ]
        },
        "ancestorNames": "/Content Collections Root/testAdminUnit/testCollection/Bees",
        "groupRoleMap": {
            "authenticated": [
                "canViewOriginals"
            ],
            "everyone": [
                "canViewMetadata"
            ],
            "unc:onyen:test_user": [
                "canDescribe"
            ]
        },
        "countMap": {},
        "thumbnailId": "4053adf7-7bdc-4c9c-8769-8cc5da4ce81d",
        "parentCollectionName": "testCollection",
        "parentCollectionId": "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
        "ancestorPath": [
            "1,collections",
            "2,353ee09f-a4ed-461e-a436-18a1bee77b01",
            "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc"
        ],
        "roleGroup": [
            "canDescribe|unc:onyen:test_user",
            "canViewOriginals|authenticated",
            "canViewMetadata|everyone"
        ],
        "id": "7d6c30fe-ca72-4362-931d-e9fe28a8ec83",
        "title": "Bees have really long titles. I have no idea why, do you? That's just the way it is. There's nothing that can be done about it, except to truncate the title.",
        "status": [
            "Inherited Patron Settings"
        ],
        "resourceType": "Work",
        "parentCollection": "testCollection|fc77a9be-b49d-4f4e-b656-1644c9e964fc",
        "ancestorIds": "/collections/353ee09f-a4ed-461e-a436-18a1bee77b01/fc77a9be-b49d-4f4e-b656-1644c9e964fc/7d6c30fe-ca72-4362-931d-e9fe28a8ec83",
        "readGroup": [
            "authenticated",
            "everyone",
            "unc:onyen:test_user"
        ],
        "adminGroup": [
            "unc:onyen:test_user"
        ],
        "parentUnit": "testAdminUnit|353ee09f-a4ed-461e-a436-18a1bee77b01"
    },
    {
        "pid": {
            "id": "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
            "componentId": "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
            "pid": "uuid:e2f0d544-4f36-482c-b0ca-ba11f1251c01",
            "uuid": "e2f0d544-4f36-482c-b0ca-ba11f1251c01"
        },
        "fields": {
            "adminGroup": [
                "unc:onyen:test_user"
            ],
            "readGroup": [
                "authenticated",
                "everyone",
                "unc:onyen:test_user"
            ],
            "title": "Listen for real",
            "parentCollection": "testCollection|fc77a9be-b49d-4f4e-b656-1644c9e964fc",
            "ancestorPath": [
                "1,collections",
                "2,353ee09f-a4ed-461e-a436-18a1bee77b01",
                "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc"
            ],
            "ancestorIds": "/collections/353ee09f-a4ed-461e-a436-18a1bee77b01/fc77a9be-b49d-4f4e-b656-1644c9e964fc/e2f0d544-4f36-482c-b0ca-ba11f1251c01",
            "id": "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
            "roleGroup": [
                "canDescribe|unc:onyen:test_user",
                "canViewOriginals|authenticated",
                "canViewMetadata|everyone"
            ],
            "parentUnit": "testAdminUnit|353ee09f-a4ed-461e-a436-18a1bee77b01",
            "status": [
                "Inherited Patron Settings"
            ],
            "resourceType": "Work"
        },
        "objectPath": {
            "entries": [
                {
                    "pid": "collections",
                    "name": "Content Collections Root",
                    "container": true
                },
                {
                    "pid": "353ee09f-a4ed-461e-a436-18a1bee77b01",
                    "name": "testAdminUnit",
                    "container": true
                },
                {
                    "pid": "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                    "name": "testCollection",
                    "container": true
                },
                {
                    "pid": "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
                    "name": "Listen for real",
                    "container": true
                }
            ]
        },
        "ancestorNames": "/Content Collections Root/testAdminUnit/testCollection/Listen for real",
        "groupRoleMap": {
            "authenticated": [
                "canViewOriginals"
            ],
            "everyone": [
                "canViewMetadata"
            ],
            "unc:onyen:test_user": [
                "canDescribe"
            ]
        },
        "countMap": {
            "child": 1
        },
        "parentCollectionName": "testCollection",
        "parentCollectionId": "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
        "ancestorPath": [
            "1,collections",
            "2,353ee09f-a4ed-461e-a436-18a1bee77b01",
            "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc"
        ],
        "roleGroup": [
            "canDescribe|unc:onyen:test_user",
            "canViewOriginals|authenticated",
            "canViewMetadata|everyone"
        ],
        "id": "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
        "status": [
            "Inherited Patron Settings"
        ],
        "title": "Listen for real",
        "resourceType": "Work",
        "parentCollection": "testCollection|fc77a9be-b49d-4f4e-b656-1644c9e964fc",
        "ancestorIds": "/collections/353ee09f-a4ed-461e-a436-18a1bee77b01/fc77a9be-b49d-4f4e-b656-1644c9e964fc/e2f0d544-4f36-482c-b0ca-ba11f1251c01",
        "readGroup": [
            "authenticated",
            "everyone",
            "unc:onyen:test_user"
        ],
        "adminGroup": [
            "unc:onyen:test_user"
        ],
        "contentStatus": [
            "Described",
            "Has Primary Object",
            "Members Are Unordered"
        ],
        "parentUnit": "testAdminUnit|353ee09f-a4ed-461e-a436-18a1bee77b01"
    },
    {
        "pid": {
            "id": "90d9b849-b9ff-4afc-910d-2833a9ed7850",
            "componentId": "90d9b849-b9ff-4afc-910d-2833a9ed7850",
            "pid": "uuid:90d9b849-b9ff-4afc-910d-2833a9ed7850",
            "uuid": "90d9b849-b9ff-4afc-910d-2833a9ed7850"
        },
        "fields": {
            "adminGroup": [
                "unc:onyen:test_user"
            ],
            "readGroup": [
                "authenticated",
                "everyone",
                "unc:onyen:test_user"
            ],
            "title": "Listen!",
            "parentCollection": "testCollection|fc77a9be-b49d-4f4e-b656-1644c9e964fc",
            "ancestorPath": [
                "1,collections",
                "2,353ee09f-a4ed-461e-a436-18a1bee77b01",
                "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc"
            ],
            "ancestorIds": "/collections/353ee09f-a4ed-461e-a436-18a1bee77b01/fc77a9be-b49d-4f4e-b656-1644c9e964fc/90d9b849-b9ff-4afc-910d-2833a9ed7850",
            "id": "90d9b849-b9ff-4afc-910d-2833a9ed7850",
            "roleGroup": [
                "canDescribe|unc:onyen:test_user",
                "canViewOriginals|authenticated",
                "canViewMetadata|everyone"
            ],
            "parentUnit": "testAdminUnit|353ee09f-a4ed-461e-a436-18a1bee77b01",
            "status": [
                "Inherited Patron Settings"
            ],
            "resourceType": "Work"
        },

        "objectPath": {
            "entries": [
                {
                    "pid": "collections",
                    "name": "Content Collections Root",
                    "container": true
                },
                {
                    "pid": "353ee09f-a4ed-461e-a436-18a1bee77b01",
                    "name": "testAdminUnit",
                    "container": true
                },
                {
                    "pid": "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                    "name": "testCollection",
                    "container": true
                },
                {
                    "pid": "90d9b849-b9ff-4afc-910d-2833a9ed7850",
                    "name": "Listen!",
                    "container": true
                }
            ]
        },
        "ancestorNames": "/Content Collections Root/testAdminUnit/testCollection/Listen!",
        "groupRoleMap": {
            "authenticated": [
                "canViewOriginals"
            ],
            "everyone": [
                "canViewMetadata"
            ],
            "unc:onyen:test_user": [
                "canDescribe"
            ]
        },
        "countMap": {},
        "parentCollectionName": "testCollection",
        "parentCollectionId": "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
        "ancestorPath": [
            "1,collections",
            "2,353ee09f-a4ed-461e-a436-18a1bee77b01",
            "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc"
        ],
        "roleGroup": [
            "canDescribe|unc:onyen:test_user",
            "canViewOriginals|authenticated",
            "canViewMetadata|everyone"
        ],
        "id": "90d9b849-b9ff-4afc-910d-2833a9ed7850",
        "title": "Listen!",
        "status": [
            "Inherited Patron Settings"
        ],
        "resourceType": "Work",
        "parentCollection": "testCollection|fc77a9be-b49d-4f4e-b656-1644c9e964fc",
        "ancestorIds": "/collections/353ee09f-a4ed-461e-a436-18a1bee77b01/fc77a9be-b49d-4f4e-b656-1644c9e964fc/90d9b849-b9ff-4afc-910d-2833a9ed7850",
        "readGroup": [
            "authenticated",
            "everyone",
            "unc:onyen:test_user"
        ],
        "adminGroup": [
            "unc:onyen:test_user"
        ],
        "parentUnit": "testAdminUnit|353ee09f-a4ed-461e-a436-18a1bee77b01"
    },
    {
        "pid": {
            "id": "c9aec262-d793-4db2-9db6-f5ad4825c670",
            "componentId": "c9aec262-d793-4db2-9db6-f5ad4825c670",
            "pid": "uuid:c9aec262-d793-4db2-9db6-f5ad4825c670",
            "uri": "http://localhost:8181/fcrepo/rest/content/c9/ae/c2/62/c9aec262-d793-4db2-9db6-f5ad4825c670",
            "uuid": "c9aec262-d793-4db2-9db6-f5ad4825c670"
        },
        "fields": {
            "adminGroup": [
                "unc:onyen:test_user"
            ],
            "readGroup": [
                "authenticated",
                "everyone",
                "unc:onyen:test_user"
            ],
            "title": "Stuff",
            "status": [
                "Inherited Patron Settings"
            ],
            "parentCollection": "testCollection|fc77a9be-b49d-4f4e-b656-1644c9e964fc",
            "contentStatus": [
                "Described",
                "Has Primary Object",
                "Members Are Unordered"
            ],
            "ancestorPath": [
                "1,collections",
                "2,353ee09f-a4ed-461e-a436-18a1bee77b01",
                "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc"
            ],
            "ancestorIds": "/collections/353ee09f-a4ed-461e-a436-18a1bee77b01/fc77a9be-b49d-4f4e-b656-1644c9e964fc/c9aec262-d793-4db2-9db6-f5ad4825c670",
            "id": "c9aec262-d793-4db2-9db6-f5ad4825c670",
            "roleGroup": [
                "canDescribe|unc:onyen:test_user",
                "canViewOriginals|authenticated",
                "canViewMetadata|everyone"
            ],
            "parentUnit": "testAdminUnit|353ee09f-a4ed-461e-a436-18a1bee77b01",
            "resourceType": "Work"
        },
        "objectPath": {
            "entries": [
                {
                    "pid": "collections",
                    "name": "Content Collections Root",
                    "container": true
                },
                {
                    "pid": "353ee09f-a4ed-461e-a436-18a1bee77b01",
                    "name": "testAdminUnit",
                    "container": true
                },
                {
                    "pid": "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                    "name": "testCollection",
                    "container": true
                },
                {
                    "pid": "c9aec262-d793-4db2-9db6-f5ad4825c670",
                    "name": "Stuff",
                    "container": true
                }
            ]
        },
        "ancestorNames": "/Content Collections Root/testAdminUnit/testCollection/Stuff",
        "groupRoleMap": {
            "authenticated": [
                "canViewOriginals"
            ],
            "everyone": [
                "canViewMetadata"
            ],
            "unc:onyen:test_user": [
                "canDescribe"
            ]
        },
        "countMap": {},
        "parentCollectionName": "testCollection",
        "parentCollectionId": "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
        "ancestorPath": [
            "1,collections",
            "2,353ee09f-a4ed-461e-a436-18a1bee77b01",
            "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc"
        ],
        "roleGroup": [
            "canDescribe|unc:onyen:test_user",
            "canViewOriginals|authenticated",
            "canViewMetadata|everyone"
        ],
        "id": "c9aec262-d793-4db2-9db6-f5ad4825c670",
        "status": [
            "Inherited Patron Settings",
            "Marked For Deletion"
        ],
        "title": "Stuff",
        "resourceType": "Work",
        "parentCollection": "testCollection|fc77a9be-b49d-4f4e-b656-1644c9e964fc",
        "fileFormatCategory": [
            "Text"
        ],
        "ancestorIds": "/collections/353ee09f-a4ed-461e-a436-18a1bee77b01/fc77a9be-b49d-4f4e-b656-1644c9e964fc/c9aec262-d793-4db2-9db6-f5ad4825c670",
        "readGroup": [
            "authenticated",
            "everyone",
            "unc:onyen:test_user"
        ],
        "adminGroup": [
            "unc:onyen:test_user"
        ],
        "contentStatus": [
            "Described",
            "Has Primary Object",
            "Members Are Unordered"
        ],
        "parentUnit": "testAdminUnit|353ee09f-a4ed-461e-a436-18a1bee77b01"
    }
];
let wrapper, router;

describe('neighborList.vue', () => {
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

        wrapper = shallowMount(neighborList, {
            global: {
                plugins: [i18n, router],
                stubs: {
                    RouterLink: RouterLinkStub,
                }
            },
            props: {
                currentRecordId: 'e2f0d544-4f36-482c-b0ca-ba11f1251c01',
                neighbors: neighbor_list
            }
        });
    });

    it("displays neighbor records", () => {
        expect(wrapper.findAll('.relateditem').length).toEqual(4);
    });

    it("highlights the current record", () => {
        expect(wrapper.findAll('.relateditem')[1].classes()).toContain('current_item');
    });

    it("does not highlight other records", () => {
        expect(wrapper.findAll('.relateditem')[2].classes()).not.toContain('current_item');
    });

    it("truncates display of titles greater than 50 characters", () => {
        expect(wrapper.find('.relateditem p').text()).toEqual('Bees have really long titles. I have no idea why,');
    });

    it("does not truncate display of titles less than 50 characters", () => {
        expect(wrapper.findAll('.relateditem p')[1].text()).toEqual('Listen for real');
    });

    it("displays a url to go to the related item", () => {
        expect(wrapper.find('.relateditem p').findComponent(RouterLinkStub).props().to)
            .toEqual('/record/7d6c30fe-ca72-4362-931d-e9fe28a8ec83');
    });

    it("adds a deleted class for deleted items", () => {
        expect(wrapper.findAll('.relatedthumb')[3].classes()).toContain('deleted');
    });
});