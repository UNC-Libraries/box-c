import { createLocalVue, shallowMount } from '@vue/test-utils'
import VueRouter from 'vue-router';
import facets from '@/components/facets.vue';

const localVue = createLocalVue();
localVue.use(VueRouter);
const router = new VueRouter({
    routes: [
        {
            path: '/search/:uuid?',
            name: 'searchRecords'
        }
    ]
});
let wrapper, collection, selected_facet, selected_sub_facet;

describe('facets.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(facets, {
            localVue,
            router,
            propsData: {
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
            }
        });

        wrapper.setData({
            selected_facets: [],
        });

        let facet_list = wrapper.findAll('.facet-display a');
        collection = facet_list.at(0);
        selected_facet = facet_list.at(2);
        selected_sub_facet = facet_list.at(3);

    });

    it("displays returned facets with counts", () => {
        let facet_headers = wrapper.findAll('.facet-display h3');
        let facets = wrapper.findAll('.facet-display li');

        expect(facet_headers.at(0).text()).toBe('Collection');
        expect(facets.at(0).find('a').text()).toBe('testCollection (19)');
        expect(facets.at(1).find('a').text()).toBe('test2Collection (1)');

        expect(facet_headers.at(1).text()).toBe('Format');
        expect(facets.at(2).find('a').text()).toBe('Image (8)');
        expect(facets.at(3).find('a').text()).toBe('png (2)');
    });

    it("displays a listing of selected facets", async () => {
        selected_facet.trigger('click');

        await wrapper.vm.$nextTick();
        expect(selected_facet.html()).toMatch(/Image.*8.*fas.fa-times/); // facet value and checkmark
        expect(wrapper.vm.selected_facets).toEqual(['format=image']);
    });

    it("displays a clear all facets button", async () => {
        expect(wrapper.find('#clear-all').exists()).toBe(false);
        selected_facet.trigger('click');

        await wrapper.vm.$nextTick();
        expect(wrapper.find('#clear-all').isVisible()).toBe(true);
    });

    it("does not display a clear all facets button if no facets are selected", async () => {
        expect(wrapper.find('#clear-all').exists()).toBe(false);
    });

    it("clears all selected facets if 'Clear Filters' button is clicked", async () => {
        selected_facet.trigger('click');
        await wrapper.vm.$nextTick();
        expect(wrapper.vm.selected_facets).toEqual(['format=image']);

        wrapper.find('#clear-all').trigger('click');
        await wrapper.vm.$nextTick();
        expect(wrapper.vm.selected_facets).toEqual([]);
    });

    it("clears a selected facet if it is unchecked", async () => {
        // Add facet
        selected_facet.trigger('click');

        await wrapper.vm.$nextTick();
        expect(selected_facet.html()).toContain('fas fa-times'); // Look for X checkmark
        expect(wrapper.vm.selected_facets).toEqual(['format=image']);

        // Remove facet
        selected_facet.trigger('click');

        await wrapper.vm.$nextTick();
        expect(selected_facet.html()).not.toContain('fas fa-times'); // Look for X checkmark
        expect(wrapper.vm.selected_facets).toEqual([]);
    });

    it("updates the query parameters if a facet is selected", async () => {
        expect(wrapper.vm.$router.currentRoute.query.format).toBe(undefined);

        // Add facet
        selected_facet.trigger('click');

        await wrapper.vm.$nextTick();
        expect(wrapper.vm.$router.currentRoute.query.format).toEqual('image');
    });

    it("updates the query parameters if a facet is removed", async () => {
        // Add facet
        selected_facet.trigger('click');

        await wrapper.vm.$nextTick();
        expect(wrapper.vm.$router.currentRoute.query.format).toEqual('image');

        // Remove facet
        selected_facet.trigger('click');

        await wrapper.vm.$nextTick();
        expect(wrapper.vm.$router.currentRoute.query.format).toBe(undefined);
    });

    it("updates facet display for a collection", async () => {
        wrapper.vm.$router.push('/search/?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');

        await wrapper.vm.$nextTick();
        expect(wrapper.vm.selected_facets).toEqual(['collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e']);
    });

    it("accepts multiple facets", async () => {
        collection.trigger('click');
        selected_facet.trigger('click');

        await wrapper.vm.$nextTick();
        expect(wrapper.vm.selected_facets).toEqual(['collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e', 'format=image']);
    });

    it("accepts multiple facets and facets of the same type", async () => {
        collection.trigger('click');
        selected_facet.trigger('click');
        selected_sub_facet.trigger('click');

        await wrapper.vm.$nextTick();
        expect(wrapper.vm.selected_facets).toEqual(['collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e', 'format=image||image/png']);
    });

    it("removes the child facet if a parent facet is removed", async () => {
        wrapper.vm.$router.push('/search?format=image%257C%257Cimage%252Fpng');

        await wrapper.vm.$nextTick();
        expect(wrapper.vm.selected_facets).toEqual(['format=image||image/png']);

        // Should always be above child facet
        selected_facet.trigger('click');
        expect(wrapper.vm.selected_facets).toEqual([]);
    });

    it("sets selected facets, including parent facets, if the page is reloaded", async () => {
        wrapper.vm.$router.push('/search/?format=image%257C%257Cimage%252Fpng');
        await wrapper.vm.$nextTick();
        expect(wrapper.vm.selected_facets).toEqual(['format=image||image/png']);
        expect(selected_facet.html()).toMatch(/Image.*8.*fas.fa-times/); // facet values and checkmark
        expect(selected_sub_facet.html()).toMatch(/png.*2.*fas.fa-times/); // facet values and checkmark
    });
});