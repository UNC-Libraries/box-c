import { shallowMount, RouterLinkStub } from '@vue/test-utils'
import { describe, it, expect, beforeEach } from 'vitest';
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
            }
        });
    });

    it("displays records in a grid", () => {
        let grid = wrapper.find('.grid');
        expect(grid.exists()).toBe(true);
        let cells = grid.findAll('.cell');
        expect(cells.length).toEqual(8);

        let first_cell = cells[0];
        expect(first_cell.find('.record-title').text()).toEqual('Test Collection');
        let first_thumbnail = first_cell.find('thumbnail-stub');
        expect(first_thumbnail.exists()).toBe(true);
        expect(first_thumbnail.attributes('linktourl')).toEqual('/record/dd8890d6-5756-4924-890c-48bc54e3edda?browse_type=gallery-display');
        expect(first_thumbnail.attributes('size')).toEqual('large');

        let last_cell = cells[7];
        expect(last_cell.find('.record-title').text()).toEqual('Test Collection 2');
        let last_thumbnail = last_cell.find('thumbnail-stub');
        expect(last_thumbnail.exists()).toBe(true);
        expect(last_thumbnail.attributes('linktourl')).toEqual('/record/87f54f12-5c50-4a14-bf8c-66cf64b00533?browse_type=gallery-display');
        expect(last_thumbnail.attributes('size')).toEqual('large');
    });
});