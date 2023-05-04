import { shallowMount, RouterLinkStub } from '@vue/test-utils'
import galleryDisplay from '@/components/galleryDisplay.vue';

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

describe('galleryDisplay.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(galleryDisplay, {
            global: {
                stubs: {
                    RouterLink: RouterLinkStub
                }
            },
            props: {
                recordList: records
            },
            data() {
                return {
                    column_size: 'is-3'
                }
            }
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

    it('changes number of columns to 6 for tiny window', async () => {
        Object.defineProperty(window, 'innerWidth', {
            writable: true,
            configurable: true,
            value: 150,
        });

        wrapper.vm.numberOfColumns();
        expect(wrapper.vm.column_size).toEqual("is-6");
    });

    it('changes number of columns to 4 for medium window', async () => {
        Object.defineProperty(window, 'innerWidth', {
            writable: true,
            configurable: true,
            value: 800,
        });

        wrapper.vm.numberOfColumns();
        expect(wrapper.vm.column_size).toEqual("is-4");
    });

    it('changes number of columns to 3 for larger window', async () => {
        Object.defineProperty(window, 'innerWidth', {
            writable: true,
            configurable: true,
            value: 1200,
        });

        wrapper.vm.numberOfColumns();
        expect(wrapper.vm.column_size).toEqual("is-3");
    });
});