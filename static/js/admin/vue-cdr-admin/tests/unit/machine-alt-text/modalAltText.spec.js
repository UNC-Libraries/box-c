import { shallowMount } from '@vue/test-utils';
import { createTestingPinia } from '@pinia/testing';
import AltTextViewer from '@/components/machine-alt-text/altTextViewer.vue';

vi.mock('datatables.net-vue3', () => ({
    default: {
        name: 'DataTable',
        props: ['columns', 'options'],
        template: '<table class="datatable"></table>',
        use: vi.fn()
    }
}));
vi.mock('datatables.net-bm', () => ({ default: {} }));
vi.mock('datatables.net-searchpanes-bm', () => ({}));
vi.mock('datatables.net-select-bm', () => ({}));

let wrapper;

const createWrapper = () => shallowMount(AltTextViewer, {
    global: {
        plugins: [createTestingPinia({
            stubActions: false,
            initialState: {
                'alt-text': {
                    showAltTextModal: true,
                    containerObject: {
                        metadata: {
                            title: 'Test object'
                        }
                    }
                }
            }
        })]
    }
});

describe('AltTextViewer.vue', () => {
    beforeEach(() => {
        wrapper = createWrapper();
    });

    afterEach(() => {
        wrapper.unmount();
        vi.clearAllMocks();
    });

    it('binds explicit columns for object-based alt text rows', () => {
        const columns = wrapper.findComponent({ name: 'DataTable' }).props('columns');

        expect(columns).toHaveLength(8);
        expect(columns.map(column => column.data)).toEqual([
            'filename',
            'filename',
            'full_desc',
            'alt_text',
            'transcript',
            'safety_review',
            'safety_form',
            null
        ]);
    });

    it('passes ajax and column definition options to DataTables', () => {
        expect(wrapper.vm.tableOptions.ajax).toEqual({
            url: '/static/alt-text.json',
            dataSrc: ''
        });
        expect(wrapper.vm.tableOptions.columnDefs).toEqual([
            { width: '10%', targets: 0 },
            { width: '17%', targets: [2, 3, 4, 5, 6] },
            { orderable: false, targets: [0, 2, 3, 4, 5, 6, 7] },
            { searchable: false, targets: [0, 7] }
        ]);
    });

    it('renders filename and safety cells using component helpers', () => {
        const filenameHtml = wrapper.vm.columns[1].render('https://example.com/images/test-image.jpg');
        const safetyHtml = wrapper.vm.columns[5].render({
            biased_language: 'NO',
            concerns_for_review: []
        });

        expect(filenameHtml).toContain('test-image.jpg');
        expect(safetyHtml).toContain('biased language');
        expect(safetyHtml).toContain('no');
        expect(safetyHtml).toContain('concerns for review');
    });
});

