import { createLocalVue, shallowMount } from '@vue/test-utils'
import VueRouter from 'vue-router';
import filterTags from '@/components/filterTags.vue';

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
let wrapper, facet_tags;

describe('filterTags.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(filterTags, {
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

        facet_tags = wrapper.findAll('ul li');
    });

    it("displays selected tags", async () => {
        wrapper.vm.$router.push('/search?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
        await wrapper.vm.$nextTick();
        expect(wrapper.find('.search-text').text()).toMatch(/Collection.*?testCollection/s);
    });

    it("displays multiple selected tags", async () => {
        wrapper.vm.$router.push('/search?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e&format=image');
        await wrapper.vm.$nextTick();
        const selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags.at(0).text()).toMatch(/Collection.*?testCollection/s);
        expect(selected_tags.at(1).text()).toMatch(/Format.*?image/s);
    });

    it("displays multiple selected tags of same type", async () => {
        wrapper.vm.$router.push('/search?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e||88386d31-6931-467d-add5-1d109f335302');
        await wrapper.vm.$nextTick();
        const selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags.at(0).text()).toMatch(/Collection.*?testCollection/s);
        expect(selected_tags.at(1).text()).toMatch(/Collection.*?test2Collection/s);
    });

    it("displays multiple selected tags of different types", async () => {
        wrapper.vm.$router.push('/search?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e||' +
        '88386d31-6931-467d-add5-1d109f335302&format=image||text');
        await wrapper.vm.$nextTick();
        const selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags.at(0).text()).toMatch(/Collection.*?testCollection/s);
        expect(selected_tags.at(1).text()).toMatch(/Collection.*?test2Collection/s);
        expect(selected_tags.at(2).text()).toMatch(/Format.*?image/s);
        expect(selected_tags.at(3).text()).toMatch(/Format.*?text/s);
    });

    it("clears a selected tag", async () => {
        wrapper.vm.$router.push('/search?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
        await wrapper.vm.$nextTick();
        expect(wrapper.find('.search-text').text()).toMatch(/Collection.*?testCollection/s);

        wrapper.find('.search-text').trigger('click');
        await wrapper.vm.$nextTick();
        expect(wrapper.find('.search-text').exists()).toBe(false);
    });

    it("clears sub tags of a selected tag", async () => {
        wrapper.vm.$router.push('/search?format=image||image/png');
        await wrapper.vm.$nextTick();
        const selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags.at(0).text()).toMatch(/Format.*?image/s);
        expect(selected_tags.at(1).text()).toMatch(/Format.*?image\/png/s);

        wrapper.find('.search-text').trigger('click'); // image tag
        await wrapper.vm.$nextTick();
        expect(wrapper.find('.search-text').exists()).toBe(false);
    });

    it("does not clear parent tag of a selected tag", async () => {
        wrapper.vm.$router.push('/search?format=image||image/png');
        await wrapper.vm.$nextTick();
        let selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags.at(0).text()).toMatch(/Format.*?image/s);
        expect(selected_tags.at(1).text()).toMatch(/Format.*?image\/png/s);

        selected_tags.at(1).trigger('click'); // image/png tag
        await wrapper.vm.$nextTick();
        selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags.at(0).text()).toMatch(/Format.*?image/s);
        expect(selected_tags.length).toEqual(1);
    });

    it("displays search query", async () => {
        wrapper.vm.$router.push('/search?anywhere=stones');
        await wrapper.vm.$nextTick();
        expect(wrapper.find('.search-text').text()).toMatch(/stones/);
    });

    it("does not display search query if empty", async () => {
        wrapper.vm.$router.push('/search?anywhere=');
        await wrapper.vm.$nextTick();
        expect(wrapper.find('.search-text').exists()).toBe(false);
    });

    it("displays a title search", async () => {
        wrapper.vm.$router.push('/search?titleIndex=stones');
        await wrapper.vm.$nextTick();
        expect(wrapper.find('.search-text').text()).toMatch(/Title.*?stones/s);
    });

    it("displays a title search and a keyword search", async () => {
        wrapper.vm.$router.push('/search?titleIndex=wooden buildings&anywhere=barns');
        await wrapper.vm.$nextTick();
        let selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags.at(0).text()).toMatch(/barns/);
        expect(selected_tags.at(1).text()).toMatch(/Title.*?wooden\sbuildings/s);
    });

    it("correctly displays a date search", async () => {
        wrapper.vm.$router.push('/search?added=1999,2005');
        await wrapper.vm.$nextTick();
        expect(wrapper.find('.search-text').text()).toMatch(/Date\sAdded.*?1999\sto\s2005/s);
    });

    it("correctly displays a date search with no end date", async () => {
        wrapper.vm.$router.push('/search?created=1999,');
        await wrapper.vm.$nextTick();
        expect(wrapper.find('.search-text').text()).toMatch(/Date\sCreated.*?1999\sto\spresent\sdate/s);
    });

    it("correctly displays a date search with no start date", async () => {
        wrapper.vm.$router.push('/search?added=,2005');
        await wrapper.vm.$nextTick();
        expect(wrapper.find('.search-text').text()).toMatch(/Date\sAdded.*?All\sdates\sthrough\s2005/s);
    });
});