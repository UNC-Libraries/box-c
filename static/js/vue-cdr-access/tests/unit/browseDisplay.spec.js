import { createLocalVue, shallowMount } from '@vue/test-utils'
import browseDisplay from '@/components/browseDisplay.vue'
import pagination from '@/components/pagination.vue'

const localVue = createLocalVue();
let wrapper;

describe('browseDisplay.vue', () => {
    beforeEach(() => {
        const retrieveData = jest.fn();

        wrapper = shallowMount(browseDisplay, {
            localVue,
            propsData: {
                path: '/listJson/73bc003c-9603-4cd9-8a65-93a22520ef6a',
                recordsPerPage: 20
            },
            methods: { retrieveData }
        });

        wrapper.setData({
            container_metadata: {
            "added": "2017-12-20T13:44:46.119Z",
                "title": "Test Admin Unit",
                "type": "AdminUnit",
                "uri": "https://dcr.lib.unc.edu/record/73bc003c-9603-4cd9-8a65-93a22520ef6a",
                "id": "73bc003c-9603-4cd9-8a65-93a22520ef6a",
                "updated": "2017-12-20T13:44:46.264Z",
            },
            record_list: [
                {
                    "added": "2017-12-20T13:44:46.154Z",
                    "counts": {
                        "child": 73
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
                        "child": 1
                    },
                    "title": "Test Collection 2",
                    "type": "Collection",
                    "uri": "https://dcr.lib.unc.edu/record/87f54f12-5c50-4a14-bf8c-66cf64b00533",
                    "id": "87f54f12-5c50-4a14-bf8c-66cf64b00533",
                    "updated": "2018-07-19T20:24:41.477Z",
                }
            ] })
    });

    it("displays count of returned child records for a container", () => {
        const record = wrapper.find('.record-count');
        expect(record.text()).toBe('73')
    })
});