import { mount, flushPromises } from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import displayWrapper from "@/components/displayWrapper.vue";
import facetModal from '@/components/facetModal.vue';
import searchWrapper from "@/components/searchWrapper.vue";
import {createI18n} from "vue-i18n";
import translations from "@/translations";
import axios from 'axios';

describe('modalMetadata.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    let router, wrapper;

    beforeEach(async () => {
        router = createRouter({
            history: createWebHistory(process.env.BASE_URL),
            routes: [
                {
                    path: '/record/:uuid',
                    name: 'displayRecords',
                    component: displayWrapper
                },
                {
                    path: '/search/:uuid?/',
                    name: 'searchRecords',
                    component: searchWrapper
                }
            ]
        });

        wrapper = mount(facetModal, {
            data() {
                return {
                    facet_data: {},
                    num_rows: 21,
                    show_modal: false,
                }
            },
            global: {
                plugins: [router, i18n]
            },
            props: {
                facetId: 'language',
                facetName: 'Language'
            }
        });
    });

    it("opens the modal when 'More' link is clicked", async () => {
        jest.spyOn(axios, 'get').mockResolvedValueOnce(defaultData());
        expect(wrapper.vm.show_modal).toBeFalsy();
        await openModal();
        expect(wrapper.vm.show_modal).toBeTruthy();
        expect(wrapper.find('h3').text()).toEqual('Language');
    });

    it("displays facets in the modal", async () => {
        await openModal();
        expect(axios.get).toHaveBeenCalledWith('/services/api/facet/language/listValues?facetSort=count&facetRows=21&facetStart=0');

        const facets = wrapper.findAll('li');
        expect(facets).toHaveLength(6);
        expect(facets[0].text()).toContain('English (10)');
        expect(facets[1].text()).toContain('Danish (1)');
        expect(facets[2].text()).toContain('Dinka (1)');
        expect(facets[3].text()).toContain('Efik (1)');
        expect(facets[4].text()).toContain('Estonian (1)');
        expect(facets[5].text()).toContain('Ewe (1)');
    });

    it("filters a query to a collection, if one is present", async () => {
        const uuid = '05209c77-30a1-43a0-9c94-fbd58ebf104e';
        wrapper.vm.$route.params.uuid = uuid;
        await openModal();
        expect(axios.get).toHaveBeenCalledWith(`/services/api/facet/language/listValues/${uuid}?facetSort=count&facetRows=21&facetStart=0`);
    });

    it("disables 'Previous/Next' buttons if there is only one page of results", async () => {
        await openModal();
        const sorting = wrapper.findAll('.paging button');
        expect(sorting[0].attributes()).toHaveProperty('disabled');
        expect(sorting[1].attributes()).toHaveProperty('disabled');
    });

    it("disables 'Previous' button if on the first page of results and there's more than one page", async () => {
        wrapper.setData({
            num_rows: 3
        });
        await openModal(pageOneData());
        const sorting = wrapper.findAll('.paging button');
        expect(sorting[0].attributes()).toHaveProperty('disabled');
        expect(sorting[1].attributes()).not.toHaveProperty('disabled');
    });

    it("disables 'Next' button if on the last page of results and there's more than one page", async () => {
        wrapper.setData({
            num_rows: 3,
            start_row: 4
        });
        await openModal(pageThreeData());
        const sorting = wrapper.findAll('.paging button');
        expect(sorting[0].attributes()).not.toHaveProperty('disabled');
        expect(sorting[1].attributes()).toHaveProperty('disabled');
    });

    it("enables paging buttons if there's a previous page and a next page", async () => {
        wrapper.setData({
            num_rows: 3,
            start_row: 2
        });
        await openModal(pageTwoData());
        const sorting = wrapper.findAll('.paging button');
        expect(sorting[0].attributes()).not.toHaveProperty('disabled');
        expect(sorting[1].attributes()).not.toHaveProperty('disabled');
    });

    it("shows the next page of results when 'Next' is clicked", async  () => {
        wrapper.setData({
            num_rows: 3,
            start_row: 2
        });
        jest.spyOn(axios, 'get')
            .mockResolvedValueOnce(pageTwoData())
            .mockResolvedValueOnce(pageThreeData());
        await wrapper.find('a').trigger('click');
        await flushPromises();

        const facets = wrapper.findAll('li');
        expect(facets).toHaveLength(2);
        expect(facets[0].text()).toContain('Dinka (1)');
        expect(facets[1].text()).toContain('Efik (1)');

        const sorting = wrapper.findAll('.paging button');
        await sorting[1].trigger('click');
        await flushPromises();

        const facets_next = wrapper.findAll('li');
        expect(facets_next).toHaveLength(1);
        expect(facets_next[0].text()).toContain('Estonian (1)');
    });

    it("shows the previous page of results when 'Previous' is clicked", async  () => {
        wrapper.setData({
            current_page: 3,
            num_rows: 3,
            start_row: 4
        });
        jest.spyOn(axios, 'get')
            .mockResolvedValueOnce( pageThreeData())
            .mockResolvedValueOnce(pageTwoData());
        await wrapper.find('a').trigger('click');
        await flushPromises();

        const facets = wrapper.findAll('li');
        expect(facets).toHaveLength(1);
        expect(facets[0].text()).toContain('Estonian (1)');

        const sorting = wrapper.findAll('.paging button');
        await sorting[0].trigger('click');
        await flushPromises();

        const facets_previous = wrapper.findAll('li');
        expect(facets_previous).toHaveLength(2);
        expect(facets_previous[0].text()).toContain('Dinka (1)');
        expect(facets_previous[1].text()).toContain('Efik (1)');
    });

    it("updates the current page number when navigating between pages", async () => {
        wrapper.setData({
            current_page: 3,
            num_rows: 3,
            start_row: 4
        });
        jest.spyOn(axios, 'get')
            .mockResolvedValueOnce( pageThreeData())
            .mockResolvedValueOnce(pageTwoData());
        await wrapper.find('a').trigger('click');
        await flushPromises();

        expect(wrapper.find('.current-page').text()).toEqual('Page: 3');

        const sorting = wrapper.findAll('.paging button');
        await sorting[0].trigger('click');
        await flushPromises();

        expect(wrapper.find('.current-page').text()).toEqual('Page: 2');
    });

    it("sorts results alphabetically", async () => {
        jest.spyOn(axios, 'get')
            .mockResolvedValueOnce(defaultData())
            .mockResolvedValueOnce(defaultDataAlpha());
        await wrapper.find('a').trigger('click');
        await flushPromises();

        const facets = wrapper.findAll('li');
        expect(facets[0].text()).toContain('English (10)');
        expect(facets[1].text()).toContain('Danish (1)');
        expect(facets[2].text()).toContain('Dinka (1)');
        expect(facets[3].text()).toContain('Efik (1)');
        expect(facets[4].text()).toContain('Estonian (1)');
        expect(facets[5].text()).toContain('Ewe (1)');

        const sort = wrapper.findAll('.sorting button');
        await sort[1].trigger('click');

        const facets_alpha = wrapper.findAll('li');
        expect(facets_alpha[0].text()).toContain('Danish (1)');
        expect(facets_alpha[1].text()).toContain('Dinka (1)');
        expect(facets_alpha[2].text()).toContain('Efik (1)');
        expect(facets_alpha[3].text()).toContain('English (10)');
        expect(facets_alpha[4].text()).toContain('Estonian (1)');
        expect(facets_alpha[5].text()).toContain('Ewe (1)');
    });

    it("sorts results by count", async () => {
        jest.spyOn(axios, 'get')
            .mockResolvedValueOnce(defaultDataAlpha())
            .mockResolvedValueOnce(defaultData());
        await wrapper.find('a').trigger('click');
        await flushPromises();

        const facets_alpha = wrapper.findAll('li');
        expect(facets_alpha[0].text()).toContain('Danish (1)');
        expect(facets_alpha[1].text()).toContain('Dinka (1)');
        expect(facets_alpha[2].text()).toContain('Efik (1)');
        expect(facets_alpha[3].text()).toContain('English (10)');
        expect(facets_alpha[4].text()).toContain('Estonian (1)');
        expect(facets_alpha[5].text()).toContain('Ewe (1)');

        const sort = wrapper.findAll('.sorting button');
        await sort[0].trigger('click');

        const facets = wrapper.findAll('li');
        expect(facets[0].text()).toContain('English (10)');
        expect(facets[1].text()).toContain('Danish (1)');
        expect(facets[2].text()).toContain('Dinka (1)');
        expect(facets[3].text()).toContain('Efik (1)');
        expect(facets[4].text()).toContain('Estonian (1)');
        expect(facets[5].text()).toContain('Ewe (1)');
    });

    it("resets the start row to 0 when sorting", async () => {
        wrapper.setData({
            num_rows: 3,
            start_row: 2
        });
        await openModal(pageTwoData());
        expect(wrapper.vm.start_row).toEqual(2);
        const sort = wrapper.findAll('.sorting button');
        await sort[0].trigger('click');
        expect(wrapper.vm.start_row).toEqual(0);
    });

    it("emits an event if a facet is clicked", async () => {
        await openModal();
        await wrapper.find('.modal-body ul li a').trigger('click');
        const facet_event = wrapper.emitted('facetValueAdded');
        expect(facet_event[0][0]).toEqual({
            "count": 10,
            "displayValue": "English",
            "fieldName": "LANGUAGE",
            "limitToValue": "English",
            "searchValue": "English",
            "value": "English"
        });
    });

    it("closes the modal", async () => {
        await openModal();
        expect(wrapper.find('.modal-wrapper').exists()).toBe(true);
        await wrapper.find('button').trigger('click');
        expect(wrapper.find('.modal-wrapper').exists()).toBe(false);
    });

    async function openModal(data = defaultData()) {
        jest.spyOn(axios, 'get').mockResolvedValue(data);
        await wrapper.find('a').trigger('click');
        await flushPromises();
    }

    function defaultData() {
        return {
            "data": {
                "facetName": "LANGUAGE",
                "facetRows": 21,
                "filterParameters": {},
                "facetSort": "count",
                "values": [
                    {
                        "fieldName": "LANGUAGE",
                        "count": 10,
                        "value": "English",
                        "displayValue": "English",
                        "searchValue": "English",
                        "limitToValue": "English"
                    },
                    {
                        "fieldName": "LANGUAGE",
                        "count": 1,
                        "value": "Danish",
                        "displayValue": "Danish",
                        "searchValue": "Danish",
                        "limitToValue": "Danish"
                    },
                    {
                        "fieldName": "LANGUAGE",
                        "count": 1,
                        "value": "Dinka",
                        "displayValue": "Dinka",
                        "searchValue": "Dinka",
                        "limitToValue": "Dinka"
                    },
                    {
                        "fieldName": "LANGUAGE",
                        "count": 1,
                        "value": "Efik",
                        "displayValue": "Efik",
                        "searchValue": "Efik",
                        "limitToValue": "Efik"
                    },
                    {
                        "fieldName": "LANGUAGE",
                        "count": 1,
                        "value": "Estonian",
                        "displayValue": "Estonian",
                        "searchValue": "Estonian",
                        "limitToValue": "Estonian"
                    },
                    {
                        "fieldName": "LANGUAGE",
                        "count": 1,
                        "value": "Ewe",
                        "displayValue": "Ewe",
                        "searchValue": "Ewe",
                        "limitToValue": "Ewe"
                    }
                ],
                "facetStart": 0
            }
        };
    }

    function defaultDataAlpha() {
        return {
            "data": {
                "facetName": "LANGUAGE",
                "facetRows": 21,
                "filterParameters": {},
                "facetSort": "count",
                "values": [
                    {
                        "fieldName": "LANGUAGE",
                        "count": 1,
                        "value": "Danish",
                        "displayValue": "Danish",
                        "searchValue": "Danish",
                        "limitToValue": "Danish"
                    },
                    {
                        "fieldName": "LANGUAGE",
                        "count": 1,
                        "value": "Dinka",
                        "displayValue": "Dinka",
                        "searchValue": "Dinka",
                        "limitToValue": "Dinka"
                    },
                    {
                        "fieldName": "LANGUAGE",
                        "count": 1,
                        "value": "Efik",
                        "displayValue": "Efik",
                        "searchValue": "Efik",
                        "limitToValue": "Efik"
                    },
                    {
                        "fieldName": "LANGUAGE",
                        "count": 10,
                        "value": "English",
                        "displayValue": "English",
                        "searchValue": "English",
                        "limitToValue": "English"
                    },
                    {
                        "fieldName": "LANGUAGE",
                        "count": 1,
                        "value": "Estonian",
                        "displayValue": "Estonian",
                        "searchValue": "Estonian",
                        "limitToValue": "Estonian"
                    },
                    {
                        "fieldName": "LANGUAGE",
                        "count": 1,
                        "value": "Ewe",
                        "displayValue": "Ewe",
                        "searchValue": "Ewe",
                        "limitToValue": "Ewe"
                    }
                ],
                "facetStart": 0
            }
        };
    }

    function pageOneData() {
        return {
            "data": {
                "facetName": "LANGUAGE",
                "facetRows": 3,
                "filterParameters": {},
                "facetSort": "count",
                "values": [
                    {
                        "fieldName": "LANGUAGE",
                        "count": 10,
                        "value": "English",
                        "displayValue": "English",
                        "searchValue": "English",
                        "limitToValue": "English"
                    },
                    {
                        "fieldName": "LANGUAGE",
                        "count": 1,
                        "value": "Danish",
                        "displayValue": "Danish",
                        "searchValue": "Danish",
                        "limitToValue": "Danish"
                    },
                    {
                        "fieldName": "LANGUAGE",
                        "count": 1,
                        "value": "Dinka",
                        "displayValue": "Dinka",
                        "searchValue": "Dinka",
                        "limitToValue": "Dinka"
                    }
                ],
                "facetStart": 0
            }
        }
    }

    function pageTwoData() {
        return {
            "data": {
                "facetName": "LANGUAGE",
                "facetRows": 3,
                "filterParameters": {},
                "facetSort": "count",
                "values": [
                    {
                        "fieldName": "LANGUAGE",
                        "count": 1,
                        "value": "Dinka",
                        "displayValue": "Dinka",
                        "searchValue": "Dinka",
                        "limitToValue": "Dinka"
                    },
                    {
                        "fieldName": "LANGUAGE",
                        "count": 1,
                        "value": "Efik",
                        "displayValue": "Efik",
                        "searchValue": "Efik",
                        "limitToValue": "Efik"
                    },
                    {
                        "fieldName": "LANGUAGE",
                        "count": 1,
                        "value": "Estonian",
                        "displayValue": "Estonian",
                        "searchValue": "Estonian",
                        "limitToValue": "Estonian"
                    }
                ],
                "facetStart": 2
            }
        };
    }

    function pageThreeData() {
        return {
            "data": {
                "facetName": "LANGUAGE",
                "facetRows": 3,
                "filterParameters": {},
                "facetSort": "count",
                "values": [
                    {
                        "fieldName": "LANGUAGE",
                        "count": 1,
                        "value": "Estonian",
                        "displayValue": "Estonian",
                        "searchValue": "Estonian",
                        "limitToValue": "Estonian"
                    }
                ],
                "facetStart": 4
            }
        };
    }
});