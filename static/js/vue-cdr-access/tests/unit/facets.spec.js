import { createLocalVue, shallowMount } from '@vue/test-utils'
import VueRouter from 'vue-router';
import facets from '@/components/facets.vue';
import moxios from "moxios";

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
                        name: 'PARENT_COLLECTION',
                        values: [
                            {
                                count:19,
                                displayValue:"testCollection",
                                fieldName:"ANCESTOR_PATH",
                                limitToValue:"d77fd8c9-744b-42ab-8e20-5ad9bdf8194e"
                            },
                            {
                                count:1,
                                displayValue:"test2Collection",
                                fieldName:"ANCESTOR_PATH",
                                limitToValue:"88386d31-6931-467d-add5-1d109f335302"
                            }
                        ]
                    },
                    {
                        name:"CONTENT_TYPE",
                        values: [
                            {
                                count:8,
                                displayValue:"Image",
                                fieldName:"CONTENT_TYPE",
                                limitToValue:"image"
                            },
                            {
                                count:2,
                                displayValue:"Text",
                                fieldName:"CONTENT_TYPE",
                                limitToValue:"text"
                            }
                        ]
                    }
                ]
            }
        });

        selected_facet = wrapper.findAll('.facet-display input[type="checkbox"]').at(2);
        collection = wrapper.find('.facet-display input[type="checkbox"]');
    });

    it("displays returned facets with counts", () => {
        let facet_headers = wrapper.findAll('.facet-display h3');
        let facets = wrapper.findAll('.facet-display li');

        expect(facet_headers.at(0).text()).toBe('Collection');
        expect(facets.at(0).find('label').text()).toBe('testCollection (19)');
        expect(facets.at(1).find('label').text()).toBe('test2Collection (1)');

        expect(facet_headers.at(1).text()).toBe('Format');
        expect(facets.at(2).find('label').text()).toBe('Image (8)');
        expect(facets.at(3).find('label').text()).toBe('Text (2)');
    });

    it("displays a listing of selected facets", () => {
        expect(wrapper.find('.selected_facets').exists()).toBe(false);

        selected_facet.trigger('click');
        selected_facet.setChecked();

        expect(wrapper.find('.selected_facets').exists()).toBe(true);
        expect(wrapper.find('.selected_facets label').text()).toBe('Image');
        expect(wrapper.vm.selected_facets).toEqual(['format=image']);
        expect(wrapper.vm.facet_info).toEqual([{
            count:8,
            displayValue:"Image",
            fieldName:"CONTENT_TYPE",
            limitToValue:"image"
        }]);
    });

    it("clears a selected facet if it is unchecked", () => {
        // Add facet
        selected_facet.trigger('click');
        selected_facet.setChecked();

        expect(wrapper.find('.selected_facets label').text()).toBe('Image');
        expect(wrapper.vm.selected_facets).toEqual(['format=image']);
        expect(wrapper.vm.facet_info).toEqual([{
            count:8,
            displayValue:"Image",
            fieldName:"CONTENT_TYPE",
            limitToValue:"image"
        }]);

        // Remove facet
        selected_facet.trigger('click');
        selected_facet.trigger('change');

        expect(wrapper.find('.selected_facets').exists()).toBe(false);
        expect(wrapper.vm.selected_facets).toEqual([]);
        expect(wrapper.vm.facet_info).toEqual([])
    });

    it("updates the query parameters if a facet is selected", () => {
        expect(wrapper.vm.$router.currentRoute.query.format).toBe(undefined);

        // Add facet
        selected_facet.trigger('click');
        selected_facet.setChecked();

        expect(wrapper.vm.$router.currentRoute.query.format).toEqual('image');
    });

    it("updates the query parameters if a facet is removed", () => {
        // Add facet
        selected_facet.trigger('click');
        selected_facet.setChecked();
        expect(wrapper.vm.$router.currentRoute.query.format).toEqual('image');

        // Remove facet
        selected_facet.trigger('click');
        selected_facet.trigger('change');
        expect(wrapper.vm.$router.currentRoute.query.format).toBe(undefined);
    });

    it("emits an event with the current collection when facets change", () => {
        collection.setChecked();
        expect(wrapper.emitted()['search-collection'][0]).toEqual(['uuid:d77fd8c9-744b-42ab-8e20-5ad9bdf8194e']);

        collection.setChecked(false);
        expect(wrapper.emitted()['search-collection'][1]).toEqual(['']);
    });

    it("updates the url if a 'collection' is selected", () => {
        collection.trigger('click');
        collection.setChecked();
        expect(wrapper.vm.$router.currentRoute.path).toBe('/search/uuid:d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
    });

    it("updates the url if a 'collection' is removed", () => {
        collection.trigger('click');
        collection.setChecked();
        expect(wrapper.vm.$router.currentRoute.path).toBe('/search/uuid:d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');

        collection.trigger('click');
        collection.trigger('change');
        expect(wrapper.vm.$router.currentRoute.path).toBe('/search');
    });

    afterEach(() => {
        selected_facet.setChecked(false);
        collection.setChecked(false);
    });
});