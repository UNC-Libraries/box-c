import {mount, flushPromises, RouterLinkStub} from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import displayWrapper from '@/components/displayWrapper.vue';
import store from '@/store';
import moxios from "moxios";
import {createI18n} from "vue-i18n";
import translations from "@/translations";

let wrapper, router;
const record_list = [
    {
        "added": "2017-12-20T13:44:46.154Z",
        "counts": {
            "child": "73"
        },
        "title": "Test Collection",
        "type": "Collection",
        "uri": "https://dcr.lib.unc.edu/record/dd8890d6-5756-4924-890c-48bc54e3edda",
        "id": "dd8890d6-5756-4924-890c-48bc54e3edda",
        "updated": "2018-06-29T18:38:22.588Z",
        "objectPath": [{ pid: "collections" }, { pid: "34e9ce20-0c7a-44a6-9fa4-d7cd27f7c502" }]
    },
    {
        "added": "2018-07-19T20:24:41.477Z",
        "counts": {
            "child": "1"
        },
        "title": "Test Collection 2",
        "type": "Collection",
        "uri": "https://dcr.lib.unc.edu/record/87f54f12-5c50-4a14-bf8c-66cf64b00533",
        "id": "87f54f12-5c50-4a14-bf8c-66cf64b00533",
        "updated": "2018-07-19T20:24:41.477Z",
        "objectPath": [{ pid: "collections" }, { pid: "34e9ce20-0c7a-44a6-9fa4-d7cd27f7c502" }]
    }
];

const response = {
    container: {
        added: "2017-12-20T13:44:46.119Z",
        title: "Test Admin Unit",
        type: "AdminUnit",
        uri: "https://dcr.lib.unc.edu/record/73bc003c-9603-4cd9-8a65-93a22520ef6a",
        id: "73bc003c-9603-4cd9-8a65-93a22520ef6a",
        updated: "2017-12-20T13:44:46.264Z",
    },
    metadata: [...record_list, ...record_list, ...record_list, ...record_list], // Creates 8 returned records
    resultCount: 8,
    facetFields: [],
    filterParameters: {}
};
const briefObjectData = {
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

describe('displayWrapper.vue', () => {
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
    });

    function mountApp(data_overrides = {}) {
        const default_data = {
            container_name: '',
            container_info: briefObjectData,
            record_count: 0,
            record_list: [],
            uuid: '0410e5c1-a036-4b7c-8d7d-63bfda2d6a36',
            filter_parameters: {}
        };
        let data = {...default_data, ...data_overrides};
        wrapper = mount(displayWrapper, {
            global: {
                plugins: [router, store, i18n],
                stubs: {
                    RouterLink: RouterLinkStub
                }
            },
            data() {
                return data;
            }
        });
    }

    function stubQueryResponse(url_pattern, response) {
        moxios.stubRequest(new RegExp(url_pattern), {
            status: 200,
            response: JSON.stringify(response)
        });
    }

    it("retrieves data", (done) => {
        stubQueryResponse(`listJson/${response.container.id}?.+`, response);
        mountApp();

        moxios.wait(() => {
            expect(wrapper.vm.search_method).toEqual('listJson');
            expect(wrapper.vm.record_count).toEqual(response.resultCount);
            expect(wrapper.vm.record_list).toEqual(response.metadata);
            expect(wrapper.vm.container_name).toEqual(response.container.title);
            expect(wrapper.vm.container_metadata).toEqual(response.container);
            done();
        });
    });

    it("uses the correct search parameter for non admin set browse works only browse", async () => {
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a/?works_only=true');
        mountApp();

        wrapper.vm.getBriefObject();
        wrapper.vm.updateUrl();
        wrapper.vm.retrieveSearchResults();
        await flushPromises();
        expect(wrapper.vm.search_method).toEqual('searchJson');
        expect(wrapper.vm.$router.currentRoute.value.query.types).toEqual('Work,File');
    });

    it("uses the correct search parameters for non admin works only browse",  async () => {
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a/?works_only=false');
        mountApp();

        wrapper.vm.getBriefObject();
        wrapper.vm.updateUrl();
        wrapper.vm.retrieveSearchResults();
        await flushPromises();
        expect(wrapper.vm.search_method).toEqual('listJson');
        expect(wrapper.vm.$router.currentRoute.value.query.types).toEqual('Work,Folder,Collection,File');
    });

    it("uses the correct search parameters if search text is specified", async () => {
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a?anywhere=search query');
        mountApp({
            filter_parameters: { "anywhere" : "search query"}
        });

        wrapper.vm.getBriefObject();
        wrapper.vm.updateUrl();
        wrapper.vm.retrieveSearchResults();
        await flushPromises();
        expect(wrapper.vm.search_method).toEqual('searchJson');
        expect(wrapper.vm.$router.currentRoute.value.query.types).toEqual('Work,Folder,Collection,File');
    });

    it("uses the correct search parameters if facet parameter is specified", async () => {
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a?subject=subj value');
        mountApp({
            filter_parameters: { "subject" : "subj value" }
        });

        wrapper.vm.getBriefObject();
        wrapper.vm.updateUrl();
        wrapper.vm.retrieveSearchResults();
        await flushPromises();
        expect(wrapper.vm.search_method).toEqual('searchJson');
        expect(wrapper.vm.$router.currentRoute.value.query.types).toEqual('Work,Folder,Collection,File');
    });

    it("uses the correct parameters for admin unit browse", async () => {
        stubQueryResponse(`listJson/73bc003c-9603-4cd9-8a65-93a22520ef6a?.+`, response);
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a?works_only=false');
        mountApp({
            container_info: {
                briefObject: {
                    type: 'AdminUnit',
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
                            }
                        ]
                    }
                }
            },
            resourceType: 'AdminUnit'
        });

        wrapper.vm.getBriefObject();
        wrapper.vm.updateUrl();
        wrapper.vm.retrieveSearchResults();
        await flushPromises();
        expect(wrapper.vm.search_method).toEqual('listJson');
        expect(wrapper.vm.$router.currentRoute.value.query.types).toEqual('Work,Folder,Collection,File');
        expect(wrapper.find(".container-note").exists()).toBe(true);
        expect(wrapper.find('#browse-display-type').exists()).toBe(true);
    });

    it("updates the url when work type changes", async () => {
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a?browse_type=gallery-display');
        mountApp({
            container_info: {
                briefObject: {
                    type: 'Collection',
                    objectPath: [
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
                resourceType: 'Collection'
            }
        });

        wrapper.vm.updateUrl();
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.types).toEqual('Work,Folder,Collection,File');
    });

    it("displays a 'works only' option if the 'works only' box is checked and no records are works", async () => {
        stubQueryResponse(`searchJson/73bc003c-9603-4cd9-8a65-93a22520ef6a?.+`, response);
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a?works_only=true');
        mountApp();

        wrapper.vm.getBriefObject();
        wrapper.vm.updateUrl();
        wrapper.vm.retrieveSearchResults();
        await flushPromises();
        let works_only = wrapper.find('.container-note');
        expect(works_only.exists()).toBe(true);
        expect(wrapper.find('#browse-display-type').exists()).toBe(true);
    });

    it("does not display a 'works only' option if the 'works only' box is not checked and no records are works", async () => {
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a?works_only=false');
        mountApp();
        // wrapper.vm.getBriefObject();
        // wrapper.vm.updateUrl();
        // wrapper.vm.retrieveSearchResults();
        // await flushPromises();
        let works_only = wrapper.find('.container-note');
        expect(works_only.exists()).toBe(false)
    });

    it("adjusts facets retrieved for admin unit", async () => {
        stubQueryResponse(`listJson/73bc003c-9603-4cd9-8a65-93a22520ef6a?.+&facetSelect=collection%2Cformat%2Cgenre%2Clanguage%2Csubject%2Clocation%2CcreatedYear%2CcreatorContributor%2Cpublisher&.*`, response);
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a');
        mountApp({
            container_info: {
                briefObject: {
                    type: 'AdminUnit',
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
                            }
                        ]
                    }
                },
                resourceType: 'AdminUnit'
            }
        });
        await flushPromises();

        // Verify that there are still other facets, but that the unit facet has been removed
        expect(wrapper.vm.$store.state.possibleFacetFields.length).toBeGreaterThan(0);
        expect(wrapper.vm.$store.state.possibleFacetFields.indexOf('unit')).toEqual(-1);
        // Verify that record list is displaying, indicating that a request was made which did not include unit facet
        expect(wrapper.find('#fullRecordSearchResultDisplay').exists()).toBe(true);
    });

    it("adjusts facets retrieved for collection object", async () => {
        stubQueryResponse(`listJson/73bc003c-9603-4cd9-8a65-93a22520ef6a?.+&facetSelect=format%2Cgenre%2Clanguage%2Csubject%2Clocation%2CcreatedYear%2CcreatorContributor%2Cpublisher&.*`, response);
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a');
        mountApp();
        await flushPromises();

        // Verify that there are still other facets, but that the unit and collection facets have been removed
        expect(wrapper.vm.$store.state.possibleFacetFields.length).toBeGreaterThan(0);
        expect(wrapper.vm.$store.state.possibleFacetFields.indexOf('unit')).toEqual(-1);
        expect(wrapper.vm.$store.state.possibleFacetFields.indexOf('collection')).toEqual(-1);
        // Verify that record list is displaying, indicating that a request was made which did not include unwanted facets
        expect(wrapper.find('#fullRecordSearchResultDisplay').exists()).toBe(true);
    });

    it("adjusts facets retrieved for folder object", async () => {
        stubQueryResponse(`listJson/73bc003c-9603-4cd9-8a65-93a22520ef6a?.+&facetSelect=format%2Cgenre%2Clanguage%2Csubject%2Clocation%2CcreatedYear%2CcreatorContributor%2Cpublisher&.*`, response);
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a');
        mountApp();
        await flushPromises();

        // Verify that there are still other facets, but that the unit and collection facets have been removed
        expect(wrapper.vm.$store.state.possibleFacetFields.length).toBeGreaterThan(0);
        expect(wrapper.vm.$store.state.possibleFacetFields.indexOf('unit')).toEqual(-1);
        expect(wrapper.vm.$store.state.possibleFacetFields.indexOf('collection')).toEqual(-1);
        // Verify that record list is displaying, indicating that a request was made which did not include unwanted facets
        expect(wrapper.find('#fullRecordSearchResultDisplay').exists()).toBe(true);
    });

    it("adjusts facets retrieved for admin unit and maintains them after checking works only", async () => {
        stubQueryResponse(`listJson/73bc003c-9603-4cd9-8a65-93a22520ef6a?.+&facetSelect=collection%2Cformat%2Cgenre%2Clanguage%2Csubject%2Clocation%2CcreatedYear%2CcreatorContributor%2Cpublisher&.*`, response);
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a/?browse_type=list-display');
        mountApp({
            container_info: {
                briefObject: {
                    type: 'AdminUnit',
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
                            }
                        ]
                    }
                },
                resourceType: "AdminUnit"
            }
        });
        await flushPromises();

        // Verify that there are still other facets, but that the unit facet has been removed
        let num_facets = wrapper.vm.$store.state.possibleFacetFields.length;
        expect(num_facets).toBeGreaterThan(0);
        expect(wrapper.vm.$store.state.possibleFacetFields.indexOf('unit')).toEqual(-1);
        expect(wrapper.vm.$route.query.facetSelect.indexOf('unit')).toEqual(-1);

        // Trigger works only filter and make sure that the set of facets does not change
        await wrapper.find('#works-only').trigger('click');
        await flushPromises();

        expect(wrapper.vm.$store.state.possibleFacetFields.length).toEqual(num_facets);
        expect(wrapper.vm.$store.state.possibleFacetFields.indexOf('unit')).toEqual(-1);
        expect(wrapper.vm.$route.query.facetSelect.indexOf('unit')).toEqual(-1);
    });

    afterEach(() => {
        moxios.uninstall();
        wrapper.vm.$store.dispatch("resetState");
        wrapper = null;
        router = null;
        // Reset the dom to avoid tags added persisting across tests
        document.getElementsByTagName('html')[0].innerHTML = '';
    });
});