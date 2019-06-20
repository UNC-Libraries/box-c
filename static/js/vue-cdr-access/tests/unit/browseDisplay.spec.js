import { createLocalVue, shallowMount } from '@vue/test-utils'
import VueRouter from 'vue-router';
import browseDisplay from '@/components/browseDisplay.vue'
import moxios from "moxios";

const localVue = createLocalVue();
localVue.use(VueRouter);
const router = new VueRouter({
    routes: [
        {
            path: '/record/98bc503c-9603-4cd9-8a65-93a22520ef68',
            name: 'browseDisplay'
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
    "container": {
        "added": "2017-12-20T13:44:46.119Z",
        "title": "Test Admin Unit",
        "type": "AdminUnit",
        "uri": "https://dcr.lib.unc.edu/record/73bc003c-9603-4cd9-8a65-93a22520ef6a",
        "id": "73bc003c-9603-4cd9-8a65-93a22520ef6a",
        "updated": "2017-12-20T13:44:46.264Z",
    },
    "metadata": [...record_list, ...record_list, ...record_list, ...record_list] // Creates 8 returned records
};

describe('browseDisplay.vue', () => {
    beforeEach(() => {
        moxios.install();

        wrapper = shallowMount(browseDisplay, {
            localVue,
            router
        });

        wrapper.setData({
            column_size: 'is-3',
            container_name: '',
            container_metadata: {},
            is_collection: false,
            record_count: 0,
            record_list: []
        });
    });

    it("retrieves data", () => {
        moxios.stubRequest('listJson/73bc003c-9603-4cd9-8a65-93a22520ef6a?rows=20&start=0&sort=title%2Cnormal', {
            status: 200,
            responseText: {
                data: response
            }
        });

        moxios.wait(() => {
            expect(wrapper.vm.record_count).toEqual(8);
            expect(wrapper.vm.record_list).toEqual(response.metadata);
            expect(wrapper.vm.container_name).toEqual('Test Admin Unit');
            expect(wrapper.vm.container_metadata).toEqual(response.container);
            done();
        });
    });

    it("updates the url when collection only changes", () => {
        expect(wrapper.vm.$router.currentRoute.query.types).toEqual(undefined);

        wrapper.setData({
            is_collection: true
        });

        wrapper.vm.updateUrl();
        expect(wrapper.vm.$router.currentRoute.query.types).toEqual('Work');
    });

    it("prints out text for type of children", () => {
        wrapper.setData({
            container_metadata: response.container
        });

        expect(wrapper.find('.spacing p').text()).toContain('collection');
    });

    it("chunks records into groups", () => {
        wrapper.setData({
            container_metadata: response.container,
            record_list: response.metadata
        });

        let four_per_row = [...record_list, ...record_list];
        expect(wrapper.vm.chunkedRecords).toEqual([four_per_row, four_per_row]);

        wrapper.setData({
            column_size: 'is-4'
        });
        let three_per_row = [
            wrapper.vm.record_list.slice(0, 3),
            wrapper.vm.record_list.slice(3, 6),
            wrapper.vm.record_list.slice(6)];
        expect(wrapper.vm.chunkedRecords).toEqual(three_per_row);

        wrapper.setData({
            column_size: 'is-6'
        });
        let two_per_row = [record_list, record_list, record_list, record_list];
        expect(wrapper.vm.chunkedRecords).toEqual(two_per_row);
    });

    afterEach(() => {
        moxios.uninstall();
    });
});