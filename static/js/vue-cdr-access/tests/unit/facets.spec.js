import {flushPromises, mount} from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import facets from '@/components/facets.vue';
import searchWrapper from '@/components/searchWrapper.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import moxios from 'moxios';
import store from '@/store';
import {createI18n} from "vue-i18n";
import translations from "@/translations";

const end_year = new Date().getFullYear();
let router, wrapper, collection, selected_facet, selected_sub_facet;

describe('facets.vue', () => {
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

        wrapper = mount(facets, {
            global: {
                plugins: [router, store, i18n]
            },
            props: {
                minCreatedYear: 2011,
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
                        name: "FILE_FORMAT_CATEGORY",
                        values: [
                            {
                                count: 8,
                                displayValue: "Image",
                                limitToValue: "Image",
                                value: "Image",
                                fieldName: "FILE_FORMAT_CATEGORY"
                            },
                            {
                                count: 2,
                                displayValue: "Text",
                                limitToValue: "Text",
                                value: "Text",
                                fieldName: "FILE_FORMAT_CATEGORY"
                            }
                        ]
                    },   {
                        name: "LOCATION",
                        values: [
                            {
                                count: 1,
                                displayValue: "California",
                                limitToValue: "California",
                                value: "California",
                                fieldName: "LOCATION"
                            },
                            {
                                count: 1,
                                displayValue: "Delaware",
                                limitToValue: "Delaware",
                                value: "Delaware",
                                fieldName: "LOCATION"
                            },
                            {
                                count: 1,
                                displayValue: "Georgia",
                                limitToValue: "Georgia",
                                value: "Georgia",
                                fieldName: "LOCATION"
                            },
                            {
                                count: 1,
                                displayValue: "Indiana",
                                limitToValue: "Indiana",
                                value: "Indiana",
                                fieldName: "LOCATION"
                            },
                            {
                                count: 1,
                                displayValue: "North Carolina",
                                limitToValue: "North Carolina",
                                value: "North Carolina",
                                fieldName: "LOCATION"
                            },
                            {
                                count: 1,
                                displayValue: "South Carolina",
                                limitToValue: "South Carolina",
                                value: "South Carolina",
                                fieldName: "LOCATION"
                            }
                        ]
                    },
                    {
                        name: "DATE_CREATED_YEAR",
                        values: [
                            {
                                count: 4,
                                displayValue: "unknown",
                                limitToValue: "unknown",
                                value: "unknown",
                                fieldName: "DATE_CREATED_YEAR"
                            }
                        ]
                    }
                ]
            },
            data() {
                return {
                    min_year: 2011,
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
        expect(facets[3].find('a').text()).toBe('Text (2)');

        expect(facet_headers[3].text()).toBe('Date Created');
        expect(facets[10].find('a').text()).toBe('unknown (4)');
    });

    it("displays 'Date Created' facet if a minimum search year is set", () => {
        let facet_headers = wrapper.findAll('.facet-display h3');

        expect(facet_headers[3].text()).toBe('Date Created');
        expect(wrapper.find('form').isVisible()).toBe(true);
        expect(wrapper.vm.dates.selected_dates.start).toEqual(2011);
        expect(wrapper.vm.dates.selected_dates.end).toEqual(end_year);

        let facets = wrapper.findAll('.facet-display li');
        expect(facets[10].find('a').text()).toBe('unknown (4)');
    });

    it("does not display slider/form for 'Date Created' facet if unknown is set", async () => {
        await router.push('/search/?createdYear=unknown');

        let facet_headers = wrapper.findAll('.facet-display h3');

        expect(facet_headers[3].text()).toBe('Date Created');
        expect(wrapper.find('form').exists()).toBe(false);
        expect(wrapper.find('form input[name="start_date"]').exists()).toBe(false);
        expect(wrapper.find('form input[name="end_date"]').exists()).toBe(false);

        let facets = wrapper.findAll('.facet-display li');
        expect(facets[10].find('a').text()).toBe('unknown (4)');
    });

    it("deselects existing date created value when unknown is selected", async () => {
        await router.push('/search/');

        let facet_headers = wrapper.findAll('.facet-display h3');

        expect(facet_headers[3].text()).toBe('Date Created');
        let date_boxes = wrapper.findAll('form input[type=number');
        date_boxes[0].setValue(2019);
        date_boxes[1].setValue(2022);
        await wrapper.find('form input[type=submit]').trigger('click');
        expect(wrapper.vm.selected_facets).toEqual(["createdYear=2019,2022"]);

        // Now select unknown value
        let facets = wrapper.findAll('.facet-display li');
        await facets[10].find('a').trigger('click');

        // date created form should be gone
        expect(facet_headers[3].text()).toBe('Date Created');
        expect(wrapper.find('form').exists()).toBe(false);
        expect(wrapper.vm.selected_facets).toEqual(["createdYear=unknown"]);

        // Click unknown again to clear it
        await facets[10].find('a').trigger('click');
        await flushPromises();

        expect(wrapper.vm.selected_facets).toEqual([]);
        expect(wrapper.find('form').exists()).toBe(true);
        expect(wrapper.vm.selected_facets).toEqual([]);
        expect(wrapper.vm.dates.selected_dates.start).toBe(2011);
        expect(wrapper.vm.dates.selected_dates.end).toBe(wrapper.vm.currentYear);

        // Unknown should still be visible
        facets = wrapper.findAll('.facet-display li');
        expect(facets[10].find('a').text()).toBe('unknown (4)');
    });

    it("does not display facets with no returned results", () => {
        let emptyFacetWrapper = mount(facets, {
            global: {
                plugins: [router, store, i18n]
            },
            props: {
                minSearchYear: 2011,
                facetList: [
                    {
                        name: "FILE_FORMAT_CATEGORY",
                        values: [
                            {
                                count: 8,
                                displayValue: "Image",
                                limitToValue: "image",
                                value: "Image",
                                fieldName: "FILE_FORMAT_CATEGORY"
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
                            },
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

    it("does not display date facets with no minimum search year set", () => {
        let emptyFacetWrapper = mount(facets, {
            global: {
                plugins: [router, store, i18n]
            },
            props: {
                minCreatedYear: undefined,
                facetList: [
                    {
                        name: "FILE_FORMAT_CATEGORY",
                        values: [
                            {
                                count: 8,
                                displayValue: "Image",
                                limitToValue: "Image",
                                value: "Image",
                                fieldName: "FILE_FORMAT_CATEGORY"
                            }
                        ]
                    },
                    {
                        name: "DATE_CREATED_YEAR",
                        values: []
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

        expect(facet_headers.length).toBe(1);
        expect(facet_list.length).toBe(1);
        expect(facet_headers.map((d) => d.text())).not.toContain('Date Created');
        expect(emptyFacetWrapper.find('form').exists()).toBe(false);
    });

    it("displays a listing of selected facets", async () => {
        await router.push('/search/?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
        selected_facet.trigger('click');
        await flushPromises();
        let facet_list = wrapper.findAll('.facet-display a');
        selected_facet = facet_list[2];
        expect(selected_facet.html()).toMatch(/Image.*8.*fas.fa-times/); // facet value and checkmark
        expect(wrapper.vm.selected_facets).toContain('format=Image');
    });

    it("displays a 'More' link if facet values equal FACET_RESULT_COUNT for a facet", async () => {
        const facet_list = wrapper.findAll('.facet-display');
        expect(facet_list[2].find('h3').text()).toEqual('Location');
        expect(facet_list[2].findAll('li').length).toEqual(6);
        expect(facet_list[2].find('.meta-modal a').exists()).toBe(true);
    });

    it("does not display a 'More' link if facet values are less than FACET_RESULT_COUNT for a facet", async () => {
        const facet_list = wrapper.findAll('.facet-display');
        expect(facet_list[0].find('h3').text()).toEqual('Collection');
        expect(facet_list[0].findAll('li').length).toEqual(2);
        expect(facet_list[0].find('.meta-modal a').exists()).toBe(false);
    });

    it("does not display a 'More' link for date created", async () => {
        const facet_list = wrapper.findAll('.facet-display');
        expect(facet_list[3].find('h3').text()).toEqual('Date Created')
        expect(facet_list[3].find('.meta-modal a').exists()).toBe(false);
    });

    it("clears a selected facet if it is unchecked", async () => {
        await router.push('/search/?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
        // Add facet
        selected_facet.trigger('click');
        await flushPromises();

        let facet_list = wrapper.findAll('.facet-display a');
        selected_facet = facet_list[2];

        expect(selected_facet.html()).toContain('fas fa-times'); // Look for X checkmark
        expect(wrapper.vm.selected_facets).toContain('format=Image');

        // Remove facet
        selected_facet.trigger('click');
        await flushPromises();
        facet_list = wrapper.findAll('.facet-display a');
        selected_facet = facet_list[2];
        expect(selected_facet.html()).not.toContain('fas fa-times'); // Look for X checkmark
        expect(wrapper.vm.selected_facets).not.toContain('format=Image');
    });

    it("updates the query parameters if a facet is selected", async () => {
        expect(wrapper.vm.$router.currentRoute.value.query.format).toBe(undefined);

        // Add facet
        await router.push('/search/?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
        selected_facet.trigger('click');
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.format).toEqual('Image');
    });

    it("updates the query parameters if a facet is removed", async () => {
        // Add facet
        await router.push('/search/?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
        selected_facet.trigger('click');
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.format).toEqual('Image');

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
        expect(wrapper.vm.selected_facets).toEqual(['collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e', 'format=Image']);
    });

    it("accepts multiple facets and facets of the same type", async () => {
        await router.push('/search?anywhere=""');
        collection.trigger('click');
        await flushPromises();
        selected_facet.trigger('click');
        await flushPromises();
        selected_sub_facet.trigger('click');
        await flushPromises();
        expect(wrapper.vm.selected_facets).toEqual(['collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e', 'format=Image||Text']);
    });

    it("sets selected facets, including multiple from same facet, if the page is reloaded", async () => {
        await router.push('/search/?format=Image%257C%257CText');
        await flushPromises();
        expect(wrapper.vm.selected_facets).toEqual(['format=Image||Text']);
        let facet_list = wrapper.findAll('.facet-display a');
        selected_facet = facet_list[2];
        selected_sub_facet = facet_list[3];
        expect(selected_facet.html()).toMatch(/Image.*8.*fas.fa-times/); // facet values and checkmark
        expect(selected_sub_facet.html()).toMatch(/Text.*2.*fas.fa-times/); // facet values and checkmark
    });

    it("it doesn't allow start date to be after the end date for the 'created date' picker", async () => {
        expect(wrapper.vm.dates.invalid_date_range).toEqual(false);
        expect(wrapper.find('.date_error').exists()).toBe(false);

        let date_boxes = wrapper.findAll('form input[type=number');
        date_boxes[0].setValue(2021);
        date_boxes[1].setValue(2019);
        await wrapper.find('form input[type=submit]').trigger('click');

        expect(wrapper.vm.dates.invalid_date_range).toEqual(true);
        expect(wrapper.vm.selected_facets).toEqual([]);
        expect(wrapper.find('.date_error').exists()).toBe(true);
    });

    it("sets the 'created date' picker values from the url", async () => {
        await router.push('/search/?createdYear=2019,2021');
        await flushPromises();

        expect(wrapper.vm.selected_facets).toEqual(['createdYear=2019,2021']);
        expect(wrapper.vm.dates.selected_dates.start).toEqual(2019);
        expect(wrapper.vm.dates.selected_dates.end).toEqual(2021);
    });

    // This isn't very satisfying, but something seems to have changed between vue 2 and 3
    // regarding emitting an event
    it("sets the date picker values if the slider is updated", () => {
        expect(wrapper.vm.dates.selected_dates).toEqual({"start": 2011, "end": 2022, });
        wrapper.vm.sliderUpdated([1991, 2000])
        expect(wrapper.vm.dates.selected_dates).toEqual({"start": 1991, "end": 2000, });
    });
});