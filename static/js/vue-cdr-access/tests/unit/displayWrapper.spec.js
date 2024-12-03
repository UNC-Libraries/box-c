import {mount, flushPromises, RouterLinkStub} from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import { createTestingPinia } from '@pinia/testing';
import { useAccessStore } from '@/stores/access';
import displayWrapper from '@/components/displayWrapper.vue';
import moxios from "moxios";
import {createI18n} from "vue-i18n";
import translations from "@/translations";
import { response, briefObjectData } from "../fixtures/displayWrapperFixtures";

let wrapper, router, store;

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

    afterEach(() => {
        store.$reset();
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
                plugins: [router, i18n, createTestingPinia({
                    stubActions: false
                })],
                stubs: {
                    RouterLink: RouterLinkStub
                }
            },
            data() {
                return data;
            }
        });
        store = useAccessStore();
    }

    function stubQueryResponse(url_pattern, response) {
        moxios.stubRequest(new RegExp(url_pattern), {
            status: 200,
            response: JSON.stringify(response)
        });
    }

    it("retrieves data", async () => {
        stubQueryResponse(`listJson/${response.container.id}?.+`, response);
        await router.push(`/record/${response.container.id}`);
        mountApp();
        wrapper.vm.getBriefObject();
        wrapper.vm.retrieveSearchResults();
        await flushPromises();

        expect(wrapper.vm.search_method).toEqual('listJson');
        expect(wrapper.vm.record_count).toEqual(response.resultCount);
        expect(wrapper.vm.record_list).toEqual(response.metadata);
        expect(wrapper.vm.container_name).toEqual(response.container.title);
        expect(wrapper.vm.container_metadata).toEqual(response.container);
        expect(wrapper.findComponent({ name: 'notFound'}).exists()).toBe(false);
    });

    it("uses the correct search parameter for non admin set browse works only browse", async () => {
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a/?works_only=true');
        mountApp();

        wrapper.vm.getBriefObject();
        wrapper.vm.retrieveSearchResults();
        await flushPromises();
        expect(wrapper.vm.search_method).toEqual('searchJson');
    });

    it("uses the correct search parameters for non admin works only browse",  async () => {
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a/?works_only=false');
        mountApp();

        wrapper.vm.getBriefObject();
        wrapper.vm.retrieveSearchResults();
        await flushPromises();
        expect(wrapper.vm.search_method).toEqual('listJson');
    });

    it("uses the correct search parameters if search text is specified", async () => {
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a?anywhere=search query');
        mountApp({
            filter_parameters: { "anywhere" : "search query"}
        });

        wrapper.vm.getBriefObject();
        wrapper.vm.retrieveSearchResults();
        await flushPromises();
        expect(wrapper.vm.search_method).toEqual('searchJson');
    });

    it("uses the correct search parameters if facet parameter is specified", async () => {
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a?subject=subj value');
        mountApp({
            filter_parameters: { "subject" : "subj value" }
        });

        wrapper.vm.getBriefObject();
        wrapper.vm.retrieveSearchResults();
        await flushPromises();
        expect(wrapper.vm.search_method).toEqual('searchJson');
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
        wrapper.vm.retrieveSearchResults();
        await flushPromises();
        expect(wrapper.vm.search_method).toEqual('listJson');
        expect(wrapper.find(".container-note").exists()).toBe(true);
        expect(wrapper.find('#browse-display-type').exists()).toBe(true);
    });

    it("displays a 'works only' option if the 'works only' box is checked and no records are works", async () => {
        stubQueryResponse(`searchJson/73bc003c-9603-4cd9-8a65-93a22520ef6a?.+`, response);
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a?works_only=true');
        mountApp();

        wrapper.vm.getBriefObject();
        wrapper.vm.retrieveSearchResults();
        await flushPromises();
        let works_only = wrapper.find('.container-note');
        expect(works_only.exists()).toBe(true);
        expect(wrapper.find('#browse-display-type').exists()).toBe(true);
    });

    it("does not display a 'works only' option if the 'works only' box is not checked and no records are works", async () => {
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a?works_only=false');
        mountApp();

        let works_only = wrapper.find('.container-note');
        expect(works_only.exists()).toBe(false)
    });

    it("adjusts facets retrieved for admin unit", async () => {
        stubQueryResponse(`listJson/73bc003c-9603-4cd9-8a65-93a22520ef6a?.+&facetSelect=collection%2Cformat%2Cgenre%2Clanguage%2Csubject%2Clocation%2CcreatedYear%2CcreatorContributor%2Cpublisher&.*`, response);
        stubQueryResponse(`record/73bc003c-9603-4cd9-8a65-93a22520ef6a/json`,
            {
                'briefObject': {
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
                'resourceType': 'AdminUnit',
                'markedForDeletion': false
            });
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a');
        mountApp();
        wrapper.vm.getBriefObject();
        wrapper.vm.retrieveSearchResults();
        await flushPromises();

        // Verify that there are still other facets, but that the unit facet has been removed
        expect(store.possibleFacetFields.length).toBeGreaterThan(0);
        expect(store.possibleFacetFields.indexOf('unit')).toEqual(-1);
        // Verify that record list is displaying, indicating that a request was made which did not include unit facet
        expect(wrapper.find('#fullRecordSearchResultDisplay').exists()).toBe(true);
    });

    it("adjusts facets retrieved for collection object", async () => {
        stubQueryResponse(`listJson/73bc003c-9603-4cd9-8a65-93a22520ef6a?.+&facetSelect=format%2Cgenre%2Clanguage%2Csubject%2Clocation%2CcreatedYear%2CcreatorContributor%2Cpublisher&.*`, response);
        stubQueryResponse(`record/73bc003c-9603-4cd9-8a65-93a22520ef6a/json`,
            {
                'briefObject': briefObjectData,
                'resourceType': 'Collection',
                'markedForDeletion': false
            });
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a');
        mountApp();
        await flushPromises();

        // Verify that there are still other facets, but that the unit and collection facets have been removed
        expect(store.possibleFacetFields.length).toBeGreaterThan(0);
        expect(store.possibleFacetFields.indexOf('unit')).toEqual(-1);
        expect(store.possibleFacetFields.indexOf('collection')).toEqual(-1);
        // Verify that record list is displaying, indicating that a request was made which did not include unwanted facets
        expect(wrapper.find('#fullRecordSearchResultDisplay').exists()).toBe(true);
    });

    it("adjusts facets retrieved for folder object", async () => {
        stubQueryResponse(`listJson/73bc003c-9603-4cd9-8a65-93a22520ef6a?.+&facetSelect=format%2Cgenre%2Clanguage%2Csubject%2Clocation%2CcreatedYear%2CcreatorContributor%2Cpublisher&.*`, response);
        stubQueryResponse(`record/73bc003c-9603-4cd9-8a65-93a22520ef6a/json`,
            {
                'briefObject': {
                    type: 'Folder',
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
                                pid: '6d824655-b2a0-4d4b-9f8c-d304bbe20286',
                                name: 'A Collection',
                                container: true
                            }
                        ]
                    }
                },
                'resourceType': 'Folder',
                'markedForDeletion': false
            });
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a');
        mountApp();
        await flushPromises();

        // Verify that there are still other facets, but that the unit and collection facets have been removed
        expect(store.possibleFacetFields.length).toBeGreaterThan(0);
        expect(store.possibleFacetFields.indexOf('unit')).toEqual(-1);
        expect(store.possibleFacetFields.indexOf('collection')).toEqual(-1);
        // Verify that record list is displaying, indicating that a request was made which did not include unwanted facets
        expect(wrapper.find('#fullRecordSearchResultDisplay').exists()).toBe(true);
    });

    it("adjusts facets retrieved for admin unit and maintains them after checking works only", async () => {
        stubQueryResponse(`listJson/73bc003c-9603-4cd9-8a65-93a22520ef6a?.+&facetSelect=collection%2Cformat%2Cgenre%2Clanguage%2Csubject%2Clocation%2CcreatedYear%2CcreatorContributor%2Cpublisher&.*`, response);
        stubQueryResponse(`record/73bc003c-9603-4cd9-8a65-93a22520ef6a/json`,
            {
                'briefObject': {
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
                'resourceType': 'AdminUnit',
                'markedForDeletion': false
            });
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a/?browse_type=list-display');
        mountApp();
        await flushPromises();

        // Verify that there are still other facets, but that the unit facet has been removed
        let num_facets = store.possibleFacetFields.length;
        expect(num_facets).toBeGreaterThan(0);
        expect(store.possibleFacetFields.indexOf('unit')).toEqual(-1);

        // Trigger works only filter and make sure that the set of facets does not change
        await wrapper.find('#works-only').trigger('click');
        await flushPromises();

        expect(store.possibleFacetFields.length).toEqual(num_facets);
        expect(store.possibleFacetFields.indexOf('unit')).toEqual(-1);
        expect(wrapper.vm.$route.query.facetSelect.indexOf('unit')).toEqual(-1);
    });

    it("shows a 'not found' message if no data is returned", async () => {
        stubQueryResponse(`/record/73bc003c-9603-4cd9-8a65-93a22520ef6a/json`, '');
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6a/?browse_type=list-display');
        mountApp();

        await wrapper.vm.getBriefObject()
        expect(wrapper.findComponent({ name: 'notFound' }).exists()).toBe(true);
    });

    it("shows a 'not found' message if a 4xx status code is returned", async () => {
        moxios.stubRequest('/record/73bc003c-9603-4cd9-8a65-93a22520ef6b/json', {
            status: 404,
            response: JSON.stringify({ message: 'Nothing to see here' })
        });
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6b/?browse_type=list-display');
        mountApp();

        await wrapper.vm.getBriefObject()
        expect(wrapper.findComponent({ name: 'notFound' }).exists()).toBe(true);
    });

    it("displays a '503 page' if JSON responds with an error", async () => {
        moxios.stubRequest('/record/73bc003c-9603-4cd9-8a65-93a22520ef6b/json', {
            status: 503,
            response: JSON.stringify({ message: 'bad stuff happened' })
        });
        await router.push('/record/73bc003c-9603-4cd9-8a65-93a22520ef6b/?browse_type=list-display');
        mountApp();
        await wrapper.vm.getBriefObject();
        expect(wrapper.findComponent({ name: 'notAvailable' }).exists()).toBe(true);
    });

    afterEach(() => {
        moxios.uninstall();
        store.$reset();
        wrapper = null;
        router = null;
        // Reset the dom to avoid tags added persisting across tests
        document.getElementsByTagName('html')[0].innerHTML = '';
    });
});