import { createLocalVue, shallowMount } from '@vue/test-utils'
import VueRouter from 'vue-router';
import displayWrapper from '@/components/displayWrapper.vue';
import moxios from "moxios";

const localVue = createLocalVue();
localVue.use(VueRouter);
const router = new VueRouter({
    routes: [
        {
            path: '/record/98bc503c-9603-4cd9-8a65-93a22520ef68',
            name: 'displayRecords'
        }
    ]
});

let wrapper;
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
    beforeEach(() => {
        moxios.install();

        wrapper = shallowMount(displayWrapper, {
            localVue,
            router
        });

        wrapper.setData({
            container_name: '',
            container_metadata: {},
            is_admin_unit: false,
            is_collection: true,
            is_folder: false,
            record_count: 0,
            record_list: [],
            uuid: '0410e5c1-a036-4b7c-8d7d-63bfda2d6a36'
        });
    });

    it("retrieves data", (done) => {
        wrapper.vm.retrieveData();
        moxios.stubRequest(`listJson/${response.container.id}?rows=20&start=0&sort=default%2Cnormal&browse_type=list-display&works_only=false&types=Work%2CFolder`, {
            status: 200,
            response: JSON.stringify(response)
        });

        moxios.wait(() => {
            expect(wrapper.vm.search_method).toEqual('listJson');
            expect(wrapper.vm.record_count).toEqual(response.resultCount);
            expect(wrapper.vm.record_list).toEqual(response.metadata);
            expect(wrapper.vm.container_name).toEqual(response.container.title);
            expect(wrapper.vm.container_metadata).toEqual(response.container);
            done();
        });
    });

    it("uses the correct search parameter for non admin set browse works only browse", () => {
        wrapper.vm.$router.currentRoute.query.works_only = 'true';

        wrapper.vm.updateUrl();
        wrapper.vm.retrieveData();

        expect(wrapper.vm.search_method).toEqual('searchJson');
        expect(wrapper.vm.$router.currentRoute.query.types).toEqual('Work');
    });

    it("uses the correct search parameters for non admin works only browse", () => {
        wrapper.vm.$router.currentRoute.query.works_only = 'false';

        wrapper.vm.updateUrl();
        wrapper.vm.retrieveData();
        expect(wrapper.vm.search_method).toEqual('listJson');
        expect(wrapper.vm.$router.currentRoute.query.types).toEqual('Work,Folder');
    });

    it("uses the correct search parameters if search text is specified", () => {
        wrapper.vm.$router.currentRoute.query.anywhere = 'search query';

        wrapper.vm.updateUrl();
        wrapper.vm.retrieveData();
        expect(wrapper.vm.search_method).toEqual('searchJson');
        expect(wrapper.vm.$router.currentRoute.query.types).toEqual('Work,Folder');
    });

    it("uses the correct parameters for admin set browse", () => {
        wrapper.setData({
            is_admin_unit: true,
            is_collection: false,
            is_folder: false
        });
        wrapper.vm.$router.currentRoute.query.works_only = 'false';
        wrapper.vm.$router.currentRoute.query.anywhere = '';
        wrapper.vm.updateUrl();
        wrapper.vm.retrieveData();

        expect(wrapper.vm.search_method).toEqual('listJson');
        expect(wrapper.vm.$router.currentRoute.query.types).toEqual('Collection');
    });

    it("updates the url when work type changes", () => {
        wrapper.setData({
            is_admin_unit: false,
            is_collection: true
        });

        wrapper.vm.$router.currentRoute.query.browse_type = 'gallery-display';
        wrapper.vm.updateUrl();
        expect(wrapper.vm.$router.currentRoute.query.types).toEqual('Work,Folder');
    });

    it("displays a 'works only' option if the 'works only' box is checked and no records are works", async () => {
        wrapper.vm.$router.currentRoute.query.works_only = 'true';
        wrapper.vm.updateUrl();
        wrapper.vm.retrieveData();

        await wrapper.vm.$nextTick();
        let works_only = wrapper.find('.container-note');
        expect(works_only.exists()).toBe(true);
    });

    it("does not display a 'works only' option if the 'works only' box is not checked and no records are works", async () => {
        wrapper.vm.$router.currentRoute.query.works_only = 'false';
        wrapper.vm.updateUrl();
        wrapper.vm.retrieveData();

        await wrapper.vm.$nextTick();
        let works_only = wrapper.find('.container-note');
        expect(works_only.exists()).toBe(false)
    });

    afterEach(() => {
        moxios.uninstall();
        wrapper = null;
    });
});