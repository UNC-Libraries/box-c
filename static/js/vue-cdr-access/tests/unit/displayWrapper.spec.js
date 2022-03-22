import { shallowMount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import displayWrapper from '@/components/displayWrapper.vue';
import moxios from "moxios";
import {createI18n} from "vue-i18n";
import translations from "@/translations";

let wrapper, router;
const record_list = [
    {
        "added": "2017-12-20T13:44:46.154Z",
        "counts": {
            "child": "73"
        },
        "title": "Test Collection",
        "type": "Collection",
        "uri": "https://dcr.lib.unc.edu/record/dd8890d6-5756-4924-890c-48bc54e3edda",
        "id": "dd8890d6-5756-4924-890c-48bc54e3edda",
        "updated": "2018-06-29T18:38:22.588Z",
    },
    {
        "added": "2018-07-19T20:24:41.477Z",
        "counts": {
            "child": "1"
        },
        "title": "Test Collection 2",
        "type": "Collection",
        "uri": "https://dcr.lib.unc.edu/record/87f54f12-5c50-4a14-bf8c-66cf64b00533",
        "id": "87f54f12-5c50-4a14-bf8c-66cf64b00533",
        "updated": "2018-07-19T20:24:41.477Z",
    }
];

const response = {
    container: {
        added: "2017-12-20T13:44:46.119Z",
        title: "Test Admin Unit",
        type: "AdminUnit",
        uri: "https://dcr.lib.unc.edu/record/73bc003c-9603-4cd9-8a65-93a22520ef6a",
        id: "73bc003c-9603-4cd9-8a65-93a22520ef6a",
        updated: "2017-12-20T13:44:46.264Z",
    },
    metadata: [...record_list, ...record_list, ...record_list, ...record_list], // Creates 8 returned records
    resultCount: 8
};

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

        wrapper = shallowMount(displayWrapper, {
            global: {
                plugins: [router, i18n]
            },
            data() {
                return {
                    container_name: '',
                    container_metadata: {},
                    is_admin_unit: false,
                    is_collection: true,
                    is_folder: false,
                    record_count: 0,
                    record_list: [],
                    uuid: '0410e5c1-a036-4b7c-8d7d-63bfda2d6a36'
                }
            }
        });
    });

    it("retrieves data", (done) => {
        moxios.stubRequest(`listJson/${response.container.id}?rows=20&start=0&sort=default%2Cnormal&browse_type=list-display&works_only=false&types=Work%2CFolder%2CCollection&getFacets=true`, {
            status: 200,
            response: JSON.stringify(response)
        });
        wrapper.vm.retrieveData();

        moxios.wait(() => {
            expect(wrapper.vm.search_method).toEqual('listJson');
            expect(wrapper.vm.record_count).toEqual(response.resultCount);
            expect(wrapper.vm.record_list).toEqual(response.metadata);
            expect(wrapper.vm.container_name).toEqual(response.container.title);
            expect(wrapper.vm.container_metadata).toEqual(response.container);
            done();
        });
    });

    it("uses the correct search parameter for non admin set browse works only browse", async () => {
        await router.push('/record/1234/?works_only=true')

        wrapper.vm.updateUrl();
        wrapper.vm.retrieveData();
        await flushPromises();
        expect(wrapper.vm.search_method).toEqual('searchJson');
        expect(wrapper.vm.$router.currentRoute.value.query.types).toEqual('Work');
    });

    it("uses the correct search parameters for non admin works only browse",  async () => {
        await router.push('/record/1234/?works_only=false');

        wrapper.vm.updateUrl();
        wrapper.vm.retrieveData();
        await flushPromises();
        expect(wrapper.vm.search_method).toEqual('listJson');
        expect(wrapper.vm.$router.currentRoute.value.query.types).toEqual('Work,Folder,Collection');
    });

    it("uses the correct search parameters if search text is specified", async () => {
        await router.push('/record/1234?anywhere=search query');

        wrapper.vm.updateUrl();
        wrapper.vm.retrieveData();
        await flushPromises();
        expect(wrapper.vm.search_method).toEqual('searchJson');
        expect(wrapper.vm.$router.currentRoute.value.query.types).toEqual('Work,Folder,Collection');
    });

    it("uses the correct parameters for admin set browse", async () => {
        await wrapper.setData({
            is_admin_unit: true,
            is_collection: false,
            is_folder: false
        });
        await router.push('/record/1234?works_only=false');

        wrapper.vm.updateUrl();
        wrapper.vm.retrieveData();
        await flushPromises();
        expect(wrapper.vm.search_method).toEqual('listJson');
        expect(wrapper.vm.$router.currentRoute.value.query.types).toEqual('Work,Folder,Collection');
    });

    it("updates the url when work type changes", async () => {
        await wrapper.setData({
            is_admin_unit: false,
            is_collection: true
        });

        await router.push('/record/1234?browse_type=gallery-display');
        wrapper.vm.updateUrl();
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.types).toEqual('Work,Folder,Collection');
    });

    it("displays a 'works only' option if the 'works only' box is checked and no records are works", async () => {
        await router.push('/record/1234?works_only=true');

        wrapper.vm.updateUrl();
        wrapper.vm.retrieveData();
        await flushPromises();
        let works_only = wrapper.find('.container-note');
        expect(works_only.exists()).toBe(true);
    });

    it("does not display a 'works only' option if the 'works only' box is not checked and no records are works", async () => {
        await router.push('/record/1234?works_only=false');
        wrapper.vm.updateUrl();
        wrapper.vm.retrieveData();
        await flushPromises();
        let works_only = wrapper.find('.container-note');
        expect(works_only.exists()).toBe(false)
    });

    afterEach(() => {
        moxios.uninstall();
        wrapper = null;
        router = null;
    });
});