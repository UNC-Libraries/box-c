import {flushPromises, shallowMount} from '@vue/test-utils';
import fileHistory from '@/components/full_record/fileHistory.vue';

const fileHistoryResponse = [
    {
        note: "ingested as PID: content/de0e6d38-c741-401c-847f-46f26ce68802 ingested as filename: test.jpg",
        username: "test_user",
        timestamp: "2026-07-17T15:42:39.313Z"
    },
    {
        note: "object renamed from: test.jpg to test-update.jpg",
        username: "test_user_1",
        timestamp: "2026-07-19T12:56:45.367Z"
    }
];
let wrapper;

describe('fileHistory.vue', () => {
    beforeEach(async () => {
        fetchMock.mockResponseOnce(JSON.stringify(fileHistoryResponse));

        wrapper = shallowMount(fileHistory, {
            propsData: {
                uuid: 'ba474b29-a250-4690-b0bd-186df93674be'
            }
        });

        await flushPromises();
    });

    afterEach(() => {
        fetchMock.resetMocks();
    })

    it('renders the component', () => {
        expect(wrapper.exists()).toBe(true);
    });

    it('renders the correct number of rows in the table', () => {
        const rows = wrapper.findAll('tbody tr');
        expect(rows.length).toBe(2);
    });

    it('renders the correct data in the first row', () => {
        const firstRow = wrapper.findAll('tbody tr').at(0);
        const cells = firstRow.findAll('td');
        expect(cells.at(0).text()).toBe(fileHistoryResponse[0].note);
        expect(cells.at(1).text()).toBe(fileHistoryResponse[0].username);
        expect(cells.at(2).text()).toBe(wrapper.vm.formatDate(fileHistoryResponse[0].timestamp));
    });

    it('renders the correct data in the second row', () => {
        const secondRow = wrapper.findAll('tbody tr').at(1);
        const cells = secondRow.findAll('td');
        expect(cells.at(0).text()).toBe(fileHistoryResponse[1].note);
        expect(cells.at(1).text()).toBe(fileHistoryResponse[1].username);
        expect(cells.at(2).text()).toBe(wrapper.vm.formatDate(fileHistoryResponse[1].timestamp));
    });

    it('formats dates correctly', () => {
        const date = '2026-07-19T19:56:45.367Z';
        expect(wrapper.vm.formatDate(date)).toEqual('July 19, 2026 at 03:56:45 PM');
    });

    it('toggles the file history', async () => {
        expect(wrapper.find('.card-content').classes()).toContain('is-hidden');
        expect(wrapper.vm.toggleArrow).toEqual('fa-angle-right');
        await wrapper.find('button').trigger('click');
        expect(wrapper.find('.card-content').classes()).not.toContain('is-hidden');
        expect(wrapper.vm.toggleArrow).toEqual('fa-angle-down');
    });
});