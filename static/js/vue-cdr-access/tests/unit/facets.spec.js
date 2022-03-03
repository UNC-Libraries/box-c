import { shallowMount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import facets from '@/components/facets.vue';
import searchWrapper from '@/components/searchWrapper.vue'
import displayWrapper from '@/components/displayWrapper';


let router, wrapper, collection, selected_facet, selected_sub_facet;

describe('facets.vue', () => {
    beforeEach(() => {
        router = createRouter({
            history: createWebHistory(process.env.BASE_URL),
            routes: [
                {
                    path: '/search/:uuid?',
                    name: 'searchRecords',
                    component: searchWrapper
                },
                {
                    path: '/record/:uuid',
                    name: 'displayRecords',
                    component: displayWrapper
                }
            ]
        });

        wrapper = shallowMount(facets, {
            global: {
                plugins: [router]
            },
            props: {
                facetList: [
                    {
                        name: "PARENT_COLLECTION",
                        values: [
                            {
                                count: 19,
                                displayValue: "testCollection",
                                limitToValue: "d77fd8c9-744b-42ab-8e20-5ad9bdf8194e",
                                value: "d77fd8c9-744b-42ab-8e20-5ad9bdf8194e",
                                fieldName: "PARENT_COLLECTION"
                            },
                            {
                                count: 1,
                                displayValue: "test2Collection",
                                limitToValue: "88386d31-6931-467d-add5-1d109f335302",
                                value: "88386d31-6931-467d-add5-1d109f335302",
                                fieldName: "PARENT_COLLECTION"
                            }
                        ]
                    },
                    {
                        name: "CONTENT_TYPE",
                        values: [
                            {
                                count: 8,
                                displayValue: "Image",
                                limitToValue: "image",
                                value: "^image,Image",
                                fieldName: "CONTENT_TYPE"
                            },
                            {
                                count: 2,
                                displayValue: "Text",
                                limitToValue: "text",
                                value: "^text,Text",
                                fieldName: "CONTENT_TYPE"
                            },
                            {
                                count: 2,
                                displayValue: "png",
                                limitToValue: "image/png",
                                value: "/image^png,png",
                                fieldName: "CONTENT_TYPE"
                            }
                        ]
                    }
                ]
            },
            data() {
                return {
                    selected_facets: []
                }
            }
        });

        let facet_list = wrapper.findAll('.facet-display a');
        collection = facet_list[0];
        selected_facet = facet_list[2];
        selected_sub_facet = facet_list[3];
    });

    afterEach(() => router = null);

    it("displays returned facets with counts", () => {
        let facet_headers = wrapper.findAll('.facet-display h3');
        let facets = wrapper.findAll('.facet-display li');

        expect(facet_headers[0].text()).toBe('Collection');
        expect(facets[0].find('a').text()).toBe('testCollection (19)');
        expect(facets[1].find('a').text()).toBe('test2Collection (1)');

        expect(facet_headers[1].text()).toBe('Format');
        expect(facets[2].find('a').text()).toBe('Image (8)');
        expect(facets[3].find('a').text()).toBe('png (2)');
    });

    it("does not display facets with no returned results", () => {
        let emptyFacetWrapper = shallowMount(facets, {
            global: {
                plugins: [router]
            },
            props: {
                facetList: [
                    {
                        name: "CONTENT_TYPE",
                        values: [
                            {
                                count: 8,
                                displayValue: "Image",
                                limitToValue: "image",
                                value: "^image,Image",
                                fieldName: "CONTENT_TYPE"
                            }
                        ]
                    },
                    {
                        name: "SUBJECT",
                        values: []
                    },
                    {
                        name: "LOCATION",
                        values: [
                            {
                                count: 1,
                                displayValue: "North Carolina",
                                limitToValue: "North Carolina",
                                value: "North Carolina",
                                fieldName: "LOCATION"
                            }
                        ]
                    }
                ]
            },
            data() {
                return {
                    selected_facets: []
                }
            }
        });

        let facet_headers = emptyFacetWrapper.findAll('.facet-display h3');
        let facet_list = emptyFacetWrapper.findAll('.facet-display li');

        expect(facet_headers[0].text()).toBe('Format');
        expect(facet_list[0].find('a').text()).toBe('Image (8)');

        expect(facet_headers[1].text()).toBe('Location');
        expect(facet_list[1].find('a').text()).toBe('North Carolina (1)');

        expect(facet_headers.length).toBe(2);
        expect(facet_list.length).toBe(2);
        expect(facet_headers.map((d) => d.text())).not.toContain('Subject');
    });

    it("displays a listing of selected facets", async () => {
        await router.push('/search/?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
        selected_facet.trigger('click');
        await flushPromises();
        let facet_list = wrapper.findAll('.facet-display a');
        selected_facet = facet_list[2];
        expect(selected_facet.html()).toMatch(/Image.*8.*fas.fa-times/); // facet value and checkmark
        expect(wrapper.vm.selected_facets).toContain('format=image');
    });

    it("displays a clear all facets button", async () => {
        expect(wrapper.find('#clear-all').exists()).toBe(false);
        await router.push('/search/?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
        selected_facet.trigger('click');
        await flushPromises();
        expect(wrapper.find('#clear-all').isVisible()).toBe(true);
    });

    it("does not display a clear all facets button if no facets are selected", async () => {
        expect(wrapper.find('#clear-all').exists()).toBe(false);
    });

    it("clears all selected facets if 'Clear Filters' button is clicked", async () => {
        await router.push('/search/?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
        selected_facet.trigger('click');
        await flushPromises();
        expect(wrapper.vm.selected_facets).toContain('format=image');

        wrapper.find('#clear-all').trigger('click');
        await flushPromises();
        expect(wrapper.vm.selected_facets).toEqual([]);
    });

    it("clears a selected facet if it is unchecked", async () => {
        await router.push('/search/?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
        // Add facet
        selected_facet.trigger('click');
        await flushPromises();

        let facet_list = wrapper.findAll('.facet-display a');
        selected_facet = facet_list[2];

        expect(selected_facet.html()).toContain('fas fa-times'); // Look for X checkmark
        expect(wrapper.vm.selected_facets).toContain('format=image');

        // Remove facet
        selected_facet.trigger('click');
        await flushPromises();
        facet_list = wrapper.findAll('.facet-display a');
        selected_facet = facet_list[2];
        expect(selected_facet.html()).not.toContain('fas fa-times'); // Look for X checkmark
        expect(wrapper.vm.selected_facets).not.toContain('format=image');
    });

    it("updates the query parameters if a facet is selected", async () => {
        expect(wrapper.vm.$router.currentRoute.value.query.format).toBe(undefined);

        // Add facet
        await router.push('/search/?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
        selected_facet.trigger('click');
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.format).toEqual('image');
    });

    it("updates the query parameters if a facet is removed", async () => {
        // Add facet
        await router.push('/search/?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
        selected_facet.trigger('click');
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.format).toEqual('image');

        // Remove facet
        let facet_list = wrapper.findAll('.facet-display a');
        selected_facet = facet_list[2];
        selected_facet.trigger('click');
        await flushPromises();

        expect(wrapper.vm.$router.currentRoute.value.query.format).toBe(undefined);
    });

    it("updates facet display for a collection", async () => {
        await router.push('/search/?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
        expect(wrapper.vm.selected_facets).toEqual(['collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e']);
    });

    it("accepts multiple facets", async () => {
        await router.push('/search/?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
        collection.trigger('click');
        await flushPromises();
        selected_facet.trigger('click');
        await flushPromises();
        expect(wrapper.vm.selected_facets).toEqual(['collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e', 'format=image']);
    });

    it("accepts multiple facets and facets of the same type", async () => {
        await router.push('/search?anywhere=""');
        collection.trigger('click');
        await flushPromises();
        selected_facet.trigger('click');
        await flushPromises();
        selected_sub_facet.trigger('click');
        await flushPromises();
        expect(wrapper.vm.selected_facets).toEqual(['collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e', 'format=image||image/png']);
    });

    it("removes the child facet if a parent facet is removed", async () => {
        await router.push('/search?format=image%257C%257Cimage%252Fpng');
        expect(wrapper.vm.selected_facets).toEqual(['format=image||image/png']);

        // Should always be above child facet
        let facet_list = wrapper.findAll('.facet-display a');
        selected_facet = facet_list[2];
        selected_facet.trigger('click');
        await flushPromises();
        expect(wrapper.vm.selected_facets).toEqual([]);
    });

    it("sets selected facets, including parent facets, if the page is reloaded", async () => {
        await router.push('/search/?format=image%257C%257Cimage%252Fpng');
        await flushPromises();
        expect(wrapper.vm.selected_facets).toEqual(['format=image||image/png']);
        let facet_list = wrapper.findAll('.facet-display a');
        selected_facet = facet_list[2];
        selected_sub_facet = facet_list[3];
        expect(selected_facet.html()).toMatch(/Image.*8.*fas.fa-times/); // facet values and checkmark
        expect(selected_sub_facet.html()).toMatch(/png.*2.*fas.fa-times/); // facet values and checkmark
    });
});