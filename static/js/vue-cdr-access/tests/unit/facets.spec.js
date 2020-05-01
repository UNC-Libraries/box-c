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
let wrapper;
let selected_facet;
let collection;

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
                                fieldName: "ANCESTOR_PATH"
                            },
                            {
                                count: 1,
                                displayValue: "test2Collection",
                                limitToValue: "88386d31-6931-467d-add5-1d109f335302",
                                value: "88386d31-6931-467d-add5-1d109f335302",
                                fieldName: "ANCESTOR_PATH"
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
                                image: "^text,Text",
                                fieldName: "CONTENT_TYPE"
                            }
                        ]
                    }
                ]
            }
        });

        selected_facet = wrapper.findAll('.facet-display a').at(2);
        collection = wrapper.find('.facet-display a');
    });

    it("displays returned facets with counts", () => {
        let facet_headers = wrapper.findAll('.facet-display h3');
        let facets = wrapper.findAll('.facet-display li');

        expect(facet_headers.at(0).text()).toBe('Collection');
        expect(facets.at(0).find('a').text()).toBe('testCollection (19)');
        expect(facets.at(1).find('a').text()).toBe('test2Collection (1)');

        expect(facet_headers.at(1).text()).toBe('Format');
        expect(facets.at(2).find('a').text()).toBe('Image (8)');
        expect(facets.at(3).find('a').text()).toBe('Text (2)');
    });

    it("displays a listing of selected facets", async () => {
        expect(wrapper.find('.selected_facets').exists()).toBe(false);

        selected_facet.trigger('click');

        await wrapper.vm.$nextTick();
        expect(wrapper.find('.selected_facets').exists()).toBe(true);
        expect(wrapper.find('.selected_facets div').text()).toBe('Image');
        expect(wrapper.vm.selected_facets).toEqual(['format=image']);
        expect(wrapper.vm.facet_info).toEqual([JSON.stringify({
            displayValue: "Image",
            limitToValue: "image",
            value: "^image,Image",
            fieldName: "CONTENT_TYPE"
        })]);
    });

    it("clears a selected facet if it is unchecked", async () => {
        // Add facet
        selected_facet.trigger('click');

        await wrapper.vm.$nextTick();
        expect(wrapper.find('.selected_facets div').text()).toBe('Image');
        expect(wrapper.vm.selected_facets).toEqual(['format=image']);
        expect(wrapper.vm.facet_info).toEqual([JSON.stringify({
            displayValue: "Image",
            limitToValue: "image",
            value: "^image,Image",
            fieldName: "CONTENT_TYPE"
        })]);

        // Remove facet
        let selected = wrapper.find('.selected_facets div');
        selected.trigger('click');

        await wrapper.vm.$nextTick();
        expect(wrapper.find('.selected_facets').exists()).toBe(false);
        expect(wrapper.vm.selected_facets).toEqual([]);
        expect(wrapper.vm.facet_info).toEqual([])
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
        let selected = wrapper.find('.selected_facets div');
        selected.trigger('click');

        await wrapper.vm.$nextTick();
        expect(wrapper.vm.$router.currentRoute.query.format).toBe(undefined);
    });

    it("updates the url if a 'collection' is selected", async () => {
        collection.trigger('click');

        await wrapper.vm.$nextTick();
        expect(wrapper.vm.$router.currentRoute.path).toBe('/search/d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
    });

    it("updates the url if a 'collection' is removed", async () => {
        collection.trigger('click');

        await wrapper.vm.$nextTick();
        expect(wrapper.vm.$router.currentRoute.path).toBe('/search/d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');

        let selected = wrapper.find('.selected_facets div');
        selected.trigger('click');

        await wrapper.vm.$nextTick();
        expect(wrapper.vm.$router.currentRoute.path).toBe('/search');
    });

    it("updates facet display if a collection uuid is in the url when page is loaded", async () => {
        wrapper.vm.$router.push('/search/d77fd8c9-744b-42ab-8e20-5ad9bdf8194e?collection_name=testCollection');

        await wrapper.vm.$nextTick();
        expect(wrapper.vm.selected_facets).toEqual(['d77fd8c9-744b-42ab-8e20-5ad9bdf8194e']);

        // Returns a sub-set of facet info when building values from the page url
        expect(wrapper.vm.facet_info).toEqual([JSON.stringify({
            displayValue: "testCollection",
            limitToValue: "d77fd8c9-744b-42ab-8e20-5ad9bdf8194e",
            value: "d77fd8c9-744b-42ab-8e20-5ad9bdf8194e",
            fieldName: "ANCESTOR_PATH",
        })]);
    });

    it("accepts multiple facets", async () => {
        collection.trigger('click');
        selected_facet.trigger('click');

        await wrapper.vm.$nextTick();
        expect(wrapper.vm.selected_facets).toEqual(['d77fd8c9-744b-42ab-8e20-5ad9bdf8194e', 'format=image']);
        expect(wrapper.vm.facet_info).toEqual([JSON.stringify({
            displayValue: "testCollection",
            limitToValue: "d77fd8c9-744b-42ab-8e20-5ad9bdf8194e",
            value: "d77fd8c9-744b-42ab-8e20-5ad9bdf8194e",
            fieldName: "ANCESTOR_PATH"
        }), JSON.stringify( {
            displayValue: "Image",

            limitToValue: "image",
            value: "^image,Image",
            fieldName: "CONTENT_TYPE"
        })]);
    });

    it("removes the child facet facet if a parent facet is removed", async () => {
        wrapper.vm.$router.push('/search?format=image%252Fpng');

        await wrapper.vm.$nextTick();
        expect(wrapper.vm.selected_facets).toEqual(['format=image', 'format=image/png']);
        expect(wrapper.vm.facet_info).toEqual([JSON.stringify({
            displayValue: "Image",
            limitToValue: "image",
            value: "^image,Image",
            fieldName: "CONTENT_TYPE",
        }), JSON.stringify({
            displayValue: "png",
            limitToValue: "image/png",
            value: "/image^png,png",
            fieldName: "CONTENT_TYPE",
        })]);

        // Should always be above child facet
        let parent_facet = wrapper.find('.selected_facets div');
        parent_facet.trigger('click');

        expect(wrapper.vm.selected_facets).toEqual([]);
        expect(wrapper.vm.facet_info).toEqual([]);
    });

    it("sets selected facets, including parent facets, if the page is reloaded", async () => {
        wrapper.vm.$router.push('/search/d77fd8c9-744b-42ab-8e20-5ad9bdf8194e?collection_name=testCollection&format=image%252Fpng');

        await wrapper.vm.$nextTick();
        expect(wrapper.vm.selected_facets).toEqual(['d77fd8c9-744b-42ab-8e20-5ad9bdf8194e', 'format=image', 'format=image/png']);

        expect(wrapper.vm.facet_info).toEqual([JSON.stringify({
            displayValue: "testCollection",
            limitToValue: "d77fd8c9-744b-42ab-8e20-5ad9bdf8194e",
            value: "d77fd8c9-744b-42ab-8e20-5ad9bdf8194e",
            fieldName: "ANCESTOR_PATH"
        }), JSON.stringify({
            displayValue: "Image",
            limitToValue: "image",
            value: "^image,Image",
            fieldName: "CONTENT_TYPE",
        }), JSON.stringify({
            displayValue: "png",
            limitToValue: "image/png",
            value: "/image^png,png",
            fieldName: "CONTENT_TYPE",
        })]);

        let display = wrapper.findAll('.selected_facets div');
        expect(display.at(0).text()).toBe('testCollection');
        expect(display.at(1).text()).toBe('Image');
        expect(display.at(2).text()).toBe('png');
    });

    afterEach(() => {
        wrapper.vm.facet_info = [];
        wrapper.vm.selected_facets = [];
    });
});