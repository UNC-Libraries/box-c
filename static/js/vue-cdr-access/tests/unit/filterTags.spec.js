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
    });

    function mountWithFilterParameters(params) {
        wrapper = shallowMount(filterTags, {
            global: {
                plugins: [router]
            },
            props: {
                filterParameters: params
            }
        });

        facet_tags = wrapper.findAll('ul li');
    }

    afterEach(() => router = null);

    it("displays selected tags", async () => {
        mountWithFilterParameters({
            collection: [{
                displayValue: "testCollection",
                value: "d77fd8c9-744b-42ab-8e20-5ad9bdf8194e"
            }]
        });
        await router.push('/search?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
        expect(wrapper.find('.search-text').text()).toMatch(/Collection.*?testCollection/s);
    });

    it("displays multiple selected tags", async () => {
        mountWithFilterParameters({
            collection: [{
                displayValue: "testCollection",
                value: "d77fd8c9-744b-42ab-8e20-5ad9bdf8194e"
            }],
            format: [{
                displayValue: "image",
                value: "image"
            }]
        });
        await router.push('/search?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e&format=image');
        const selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags[0].text()).toMatch(/Collection.*?testCollection/s);
        expect(selected_tags[1].text()).toMatch(/Format.*?image/s);
    });

    it("displays multiple selected tags of same type", async () => {
        mountWithFilterParameters({
            collection: [{
                displayValue: "testCollection",
                value: "d77fd8c9-744b-42ab-8e20-5ad9bdf8194e"
            }, {
                displayValue: "test2Collection",
                value: "88386d31-6931-467d-add5-1d109f335302"
            }]
        });
        await router.push('/search?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e||88386d31-6931-467d-add5-1d109f335302');;
        const selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags[0].text()).toMatch(/Collection.*?testCollection/s);
        expect(selected_tags[1].text()).toMatch(/Collection.*?test2Collection/s);
    });

    it("displays multiple selected tags of different types", async () => {
        mountWithFilterParameters({
            collection: [{
                displayValue: "testCollection",
                value: "d77fd8c9-744b-42ab-8e20-5ad9bdf8194e"
            }, {
                displayValue: "test2Collection",
                value: "88386d31-6931-467d-add5-1d109f335302"
            }],
            format: [{
                displayValue: "image",
                value: "image"
            }, {
                displayValue: "text",
                value: "text"
            }]
        });
        await router.push('/search?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e||' +
        '88386d31-6931-467d-add5-1d109f335302&format=image||text');
        const selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags[0].text()).toMatch(/Collection.*?testCollection/s);
        expect(selected_tags[1].text()).toMatch(/Collection.*?test2Collection/s);
        expect(selected_tags[2].text()).toMatch(/Format.*?image/s);
        expect(selected_tags[3].text()).toMatch(/Format.*?text/s);
    });

    it("clears a selected tag", async () => {
        mountWithFilterParameters({
            collection: [{
                displayValue: "testCollection",
                value: "d77fd8c9-744b-42ab-8e20-5ad9bdf8194e"
            }]
        });
        await router.push('/search?collection=d77fd8c9-744b-42ab-8e20-5ad9bdf8194e');
        expect(wrapper.find('.search-text').text()).toMatch(/Collection.*?testCollection/s);

        await wrapper.find('.search-text').trigger('click');
        await wrapper.setProps({ filterParameters: {} });
        await flushPromises();
        expect(global.window.location.href).toEqual('https://localhost/search');
        expect(wrapper.find('.search-text').exists()).toBe(false);
    });

    it("clears sub tags of a selected tag", async () => {
        mountWithFilterParameters({
            format: [{
                displayValue: "image",
                value: "image"
            }, {
                displayValue: "png",
                value: "image/png"
            }]
        });
        await router.push('/search?format=image||image/png');
        const selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags[0].text()).toMatch(/Format.*?image/s);
        expect(selected_tags[1].text()).toMatch(/Format.*?png/s);

        wrapper.find('.search-text').trigger('click'); // image tag
        await wrapper.setProps({ filterParameters: {} });
        await flushPromises();
        expect(global.window.location.href).toEqual('https://localhost/search');
        expect(wrapper.find('.search-text').exists()).toBe(false);
    });

    it("does not clear parent tag of a selected tag", async () => {
        mountWithFilterParameters({
            format: [{
                displayValue: "image",
                value: "image"
            }, {
                displayValue: "png",
                value: "image/png"
            }]
        });
        await router.push('/search?format=image||image/png');
        let selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags[0].text()).toMatch(/Format.*?image/s);
        expect(selected_tags[1].text()).toMatch(/Format.*?png/s);

        selected_tags[1].trigger('click'); // image/png tag
        await wrapper.setProps({ filterParameters: {
                format: [{
                    displayValue: "image",
                    value: "image"
                }] } });
        await flushPromises();
        expect(global.window.location.href).toEqual('https://localhost/search?format=image');
        selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags[0].text()).toMatch(/Format.*?image/s);
        expect(selected_tags.length).toEqual(1);
    });

    it("displays search query", async () => {
        mountWithFilterParameters({
            anywhere: "stones"
        });
        await router.push('/search?anywhere=stones');
        expect(wrapper.find('.search-text').text()).toMatch(/stones/);
    });

    it("does not display search query if empty", async () => {
        mountWithFilterParameters({
            anywhere: ""
        });
        await router.push('/search?anywhere=');
        expect(wrapper.find('.search-text').exists()).toBe(false);
    });

    it("displays a title search", async () => {
        mountWithFilterParameters({
            titleIndex: "stones"
        });
        await router.push('/search?titleIndex=stones');
        expect(wrapper.find('.search-text').text()).toMatch(/Title.*?stones/s);
    });

    it("displays a title search and a keyword search", async () => {
        mountWithFilterParameters({
            titleIndex: "wooden buildings",
            anywhere: "barns"
        });
        await router.push('/search?titleIndex=wooden buildings&anywhere=barns');
        let selected_tags = wrapper.findAll('.search-text');
        expect(selected_tags[0].text()).toMatch(/barns/);
        expect(selected_tags[1].text()).toMatch(/Title.*?wooden\sbuildings/s);
    });

    it("correctly displays a date search", async () => {
        mountWithFilterParameters({
            added: "1999,2005"
        });
        await router.push('/search?added=1999,2005');
        expect(wrapper.find('.search-text').text()).toMatch(/Date\sAdded.*?1999\sto\s2005/s);
    });

    it("correctly displays a date search with no end date", async () => {
        mountWithFilterParameters({
            created: "1999,"
        });
        await router.push('/search?created=1999,');
        expect(wrapper.find('.search-text').text()).toMatch(/Date\sCreated.*?1999\sto\spresent\sdate/s);
    });

    it("correctly displays a date search with no start date", async () => {
        mountWithFilterParameters({
            added: ",2005"
        });
        await router.push('/search?added=,2005');
        expect(wrapper.find('.search-text').text()).toMatch(/Date\sAdded.*?All\sdates\sthrough\s2005/s);
    });
});