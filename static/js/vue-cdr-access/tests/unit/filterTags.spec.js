import { shallowMount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import filterTags from '@/components/filterTags.vue';
import displayWrapper from "@/components/displayWrapper.vue";
import searchWrapper from '@/components/searchWrapper.vue';

let wrapper, facet_tags, router;

describe('filterTags.vue', () => {
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
        wrapper = shallowMount(filterTags, {
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
            }
        });

        facet_tags = wrapper.findAll('ul li');
    });

    afterEach(() => router = null);

    it("displays selected tags", async () => {
        await router.push('/search?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
        expect(wrapper.find('.search-text').text()).toMatch(/Collection.*?testCollection/s);
    });

    it("displays multiple selected tags", async () => {
        await router.push('/search?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e&format=image');
        const selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags[0].text()).toMatch(/Collection.*?testCollection/s);
        expect(selected_tags[1].text()).toMatch(/Format.*?image/s);
    });

    it("displays multiple selected tags of same type", async () => {
        await router.push('/search?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e||88386d31-6931-467d-add5-1d109f335302');;
        const selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags[0].text()).toMatch(/Collection.*?testCollection/s);
        expect(selected_tags[1].text()).toMatch(/Collection.*?test2Collection/s);
    });

    it("displays multiple selected tags of different types", async () => {
        await router.push('/search?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e||' +
        '88386d31-6931-467d-add5-1d109f335302&format=image||text');
        const selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags[0].text()).toMatch(/Collection.*?testCollection/s);
        expect(selected_tags[1].text()).toMatch(/Collection.*?test2Collection/s);
        expect(selected_tags[2].text()).toMatch(/Format.*?image/s);
        expect(selected_tags[3].text()).toMatch(/Format.*?text/s);
    });

    it("clears a selected tag", async () => {
        await router.push('/search?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
        expect(wrapper.find('.search-text').text()).toMatch(/Collection.*?testCollection/s);

        await wrapper.find('.search-text').trigger('click');
        await flushPromises();
        expect(wrapper.find('.search-text').exists()).toBe(false);
    });

    it("clears sub tags of a selected tag", async () => {
        await router.push('/search?format=image||image/png');
        const selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags[0].text()).toMatch(/Format.*?image/s);
        expect(selected_tags[1].text()).toMatch(/Format.*?image\/png/s);

        wrapper.find('.search-text').trigger('click'); // image tag
        await flushPromises();
        expect(wrapper.find('.search-text').exists()).toBe(false);
    });

    it("does not clear parent tag of a selected tag", async () => {
        await router.push('/search?format=image||image/png');
        let selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags[0].text()).toMatch(/Format.*?image/s);
        expect(selected_tags[1].text()).toMatch(/Format.*?image\/png/s);

        selected_tags[1].trigger('click'); // image/png tag
        await flushPromises();
        selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags[0].text()).toMatch(/Format.*?image/s);
        expect(selected_tags.length).toEqual(1);
    });

    it("displays search query", async () => {
        await router.push('/search?anywhere=stones');
        expect(wrapper.find('.search-text').text()).toMatch(/stones/);
    });

    it("does not display search query if empty", async () => {
        await router.push('/search?anywhere=');
        expect(wrapper.find('.search-text').exists()).toBe(false);
    });

    it("displays a title search", async () => {
        await router.push('/search?titleIndex=stones');
        expect(wrapper.find('.search-text').text()).toMatch(/Title.*?stones/s);
    });

    it("displays a title search and a keyword search", async () => {
        await router.push('/search?titleIndex=wooden buildings&anywhere=barns');
        let selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags[0].text()).toMatch(/barns/);
        expect(selected_tags[1].text()).toMatch(/Title.*?wooden\sbuildings/s);
    });

    it("correctly displays a date search", async () => {
        await router.push('/search?added=1999,2005');
        expect(wrapper.find('.search-text').text()).toMatch(/Date\sAdded.*?1999\sto\s2005/s);
    });

    it("correctly displays a date search with no end date", async () => {
        await router.push('/search?created=1999,');
        expect(wrapper.find('.search-text').text()).toMatch(/Date\sCreated.*?1999\sto\spresent\sdate/s);
    });

    it("correctly displays a date search with no start date", async () => {
        await router.push('/search?added=,2005');
        expect(wrapper.find('.search-text').text()).toMatch(/Date\sAdded.*?All\sdates\sthrough\s2005/s);
    });
});