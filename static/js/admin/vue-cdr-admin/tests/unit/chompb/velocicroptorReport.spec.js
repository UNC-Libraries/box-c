import { shallowMount, flushPromises } from '@vue/test-utils';
import velocicroptorReport from '@/components/chompb/velocicroptorReport.vue';
import { createTestingPinia } from '@pinia/testing';
import { createRouter, createWebHistory } from 'vue-router';

// Mock datatables.net-vue3 since it relies on the DOM and jQuery, which are not available in this testing environment.
// We will just mock the component and the use function since we don't need to test
// the actual functionality of datatables in this unit test.
vi.mock('datatables.net-vue3', () => ({
    default: {
        name: 'DataTable',
        template: '<div><slot /></div>',
        use: vi.fn()
    }
}));
vi.mock('datatables.net-bm', () => ({ default: {} }));
vi.mock('datatables.net-searchpanes-bm', () => ({ default: {} }));
vi.mock('datatables.net-select-bm', () => ({ default: {} }));

let wrapper, router;

const report_data = {
    "data": [
        {
            "original": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0001.tif",
            "pred_class": "1",
            "pred_conf": "0.9998",
            "problem": false,
            "image": "images/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0001.jpg.jpg"
        },
        {
            "original": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0002.tif",
            "pred_class": "1",
            "pred_conf": "0.9998",
            "problem": false,
            "image": "images/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0002.jpg.jpg"
        }
    ]
};

describe('velocicroptorReport.vue', () => {
    beforeEach(async () => {
        router = createRouter({
            history: createWebHistory(process.env.BASE_URL),
            routes: [
                {
                    path: '/admin/chompb/project/:project/processing_results/velocicroptor',
                    name: 'velocicroptorReport',
                    component: velocicroptorReport
                }
            ]
        });

        fetchMock.mockResponseOnce(JSON.stringify(report_data));

        await router.push('/admin/chompb/project/file_source_test/processing_results/velocicroptor');
        await flushPromises();

        global.URL.createObjectURL = vi.fn();

        wrapper = shallowMount(velocicroptorReport, {
            global: {
                plugins: [router, createTestingPinia({
                    stubActions: false
                })],
                stubs: {
                    teleport: true
                }
            }
        });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("contains a data tables", () => {
        expect(wrapper.findComponent({ name: 'dataTable' }).exists()).toBe(true);
    });

    it("populates CSV download link", () => {
        expect(wrapper.find('.is-selected a').attributes('href')).toBe('/admin/chompb/project/file_source_test/processing_results/velocicroptor/files?path=data.csv');
    });

    it("marking entry as problematic adds entries to problem csv export", () => {
        class MockBlob {
            constructor(content, options) {
                this.content = content;
                this.options = options;
            }
        }
        const mockBlobConstructor = vi.fn(MockBlob);

        vi.stubGlobal('Blob', mockBlobConstructor);

        const e = {
            target: {
                classList: {
                    contains: vi.fn().mockReturnValue(true),
                    add: vi.fn(),
                    remove: vi.fn()
                },
                dataset: {
                    path: '/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0001.tif',
                    predicted: '0'
                }
            }
        };
        wrapper.vm.markAsProblem(e);
        expect(wrapper.vm.problems.length).toBe(1);
        wrapper.vm.markAsProblem(e);
        expect(wrapper.vm.problems.length).toBe(0);
        wrapper.vm.markAsProblem(e);
        expect(wrapper.vm.problems.length).toBe(1);

        // Since URL.createObjectURL is mocked in this environment, we will just test that it received the right CSV blob
        wrapper.vm.downloadProblems;
        const csv_result = mockBlobConstructor.mock.calls[0][0][0];
        expect(csv_result).toBe('path,predicted_class,corrected_class\n' +
            '/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0001.tif,0,1');
    });
});