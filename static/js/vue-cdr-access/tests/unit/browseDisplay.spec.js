import { createLocalVue, shallowMount } from '@vue/test-utils'
import VueRouter from 'vue-router';
import browseDisplay from '@/components/browseDisplay.vue';

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

let records = [...record_list, ...record_list, ...record_list, ...record_list]; // Creates 8 returned records

describe('browseDisplay.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(browseDisplay, {
            localVue,
            router,
            propsData: {
                recordList: records
            }
        });

        wrapper.setData({
            column_size: 'is-3',
        });
    });

    it("chunks records into groups", () => {
        let four_per_row = [...record_list, ...record_list];
        expect(wrapper.vm.chunkedRecords).toEqual([four_per_row, four_per_row]);

        wrapper.setData({
            column_size: 'is-4'
        });
        let three_per_row = [
            wrapper.vm.recordList.slice(0, 3),
            wrapper.vm.recordList.slice(3, 6),
            wrapper.vm.recordList.slice(6)];
        expect(wrapper.vm.chunkedRecords).toEqual(three_per_row);

        wrapper.setData({
            column_size: 'is-6'
        });
        let two_per_row = [record_list, record_list, record_list, record_list];
        expect(wrapper.vm.chunkedRecords).toEqual(two_per_row);
    });
});