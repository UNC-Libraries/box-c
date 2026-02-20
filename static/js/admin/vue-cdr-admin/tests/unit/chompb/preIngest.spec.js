import { mount, flushPromises } from '@vue/test-utils';
import { nextTick } from 'vue';
import preIngest from '@/components/chompb/preIngest.vue';
import structuredClone from '@ungap/structured-clone';

let wrapper;
let mockRouter;

const project_info = [
    {
        "projectProperties" : {
            "name": "file_source_test",
            "createdDate": "2024-07-16T15:20:08.579384Z",
            "creator": "bbpennel",
            "indexedDate": "2024-08-15T12:38:28.662654Z",
            "sourceFilesUpdatedDate": "2024-08-14T17:06:34.182417Z",
            "groupMappingsUpdatedDate": "2024-08-14T17:42:49.626664Z",
            "sipsSubmitted": [],
            "bxcEnvironmentId": "test",
            "projectSource": "files"
        },
        "status": "sources_mapped",
        "allowedActions": ["crop_color_bars"],
        "projectPath": "/test_path_one/file_source_test",
        "processingJobs" : {}
    },
    {
        "projectProperties" : {
            "name": "ncmaps_urls",
            "cdmCollectionId": "ncmaps",
            "createdDate": "2024-04-05T13:17:51.975599Z",
            "creator": "goslen",
            "exportedDate": "2024-04-05T13:18:30.212723Z",
            "indexedDate": "2024-04-05T13:20:32.083546Z",
            "cdmEnvironmentId": "dc-prod",
            "bxcEnvironmentId": "test",
            "projectSource": "CDM"
        },
        "status": "sources_mapped",
        "allowedActions": [],
        "projectPath": "/test_path_two/file_source_test",
        "processingJobs" : {}
    }
];

describe('preIngest.vue', () => {
    function setupWrapper(dataSet) {
        mockRouter = {
            push: vi.fn(),
        };

        wrapper = mount(preIngest, {
            global: {
                stubs: {
                    teleport: true
                },
                mocks: {
                    $router: mockRouter,
                }
            },
            data() {
                return {
                    dataSet: dataSet
                }
            }
        });
    }

    beforeEach(() => {
        fetchMock.enableMocks();
        fetchMock.resetMocks();
    });

    afterEach(() => {
        fetchMock.disableMocks();
        vi.unstubAllGlobals();
    });

    it("contains a table of projects", () => {
        setupWrapper(project_info);

        expect(wrapper.find('.datatable').exists()).toBe(true);
        let rows = wrapper.findAll('.datatable tbody tr');
        let actions1 = rows[0].findAll('a');
        expect(actions1[0].text()).toBe('Copy Path');
        expect(actions1[1].text()).toBe('Crop color bars');
        expect(actions1.length).toBe(2);
        let actions2 = rows[1].findAll('a');
        expect(actions2[0].text()).toBe('Copy Path');
        expect(actions2.length).toBe(1);
    });

    it("shows link to report if job is completed", async () => {
        // Don't stub globally, just use the polyfill directly
        let updatedInfo = structuredClone(project_info);
        updatedInfo[0].processingJobs['velocicroptor'] = { 'status' : 'completed' };
        setupWrapper(updatedInfo);

        await nextTick();

        let rows = wrapper.findAll('.datatable tbody tr');
        let actions1 = rows[0].findAll('a');
        expect(actions1[0].text()).toBe('Copy Path');
        expect(actions1[1].text()).toBe('Crop color bars');
        expect(actions1[2].text()).toBe('View crop report');
        expect(actions1.length).toBe(3);

        await actions1[2].trigger('click');

        expect(mockRouter.push).toHaveBeenCalledWith('/admin/chompb/project/file_source_test/processing_results/velocicroptor');
    });

    it("clicking on the crop button causes request to be made", async () => {
        // suppressing error spam from jsdom when making http requests
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

        setupWrapper(project_info);

        // Mock window.confirm before any actions
        const confirmMock = vi.fn().mockReturnValue(true);
        vi.stubGlobal('confirm', confirmMock);

        await nextTick();

        // Mock the fetch response
        fetchMock.mockResponseOnce(JSON.stringify({'action' : 'Start cropping for project file_source_test'}), {
            status: 200
        });

        let rows = wrapper.findAll('.datatable tbody tr');
        let actions1 = rows[0].findAll('a');
        expect(actions1[1].text()).toBe('Crop color bars');

        await actions1[1].trigger('click');
        await flushPromises();

        // Check the fetch call was made correctly
        const lastCall = fetchMock.mock.calls[fetchMock.mock.calls.length - 1];
        expect(lastCall[0]).toContain('/admin/chompb/project/file_source_test/action/velocicroptor');
        expect(lastCall[1].method).toEqual('POST');

        expect(confirmMock).toHaveBeenCalledWith('Are you sure you want to crop color bars for this project?');

        // crop option should have changed from a link to a span
        rows = wrapper.findAll('.datatable tbody tr');
        let actions2 = rows[0].findAll('span');
        expect(actions2[0].text()).toBe('Crop in progress');

        consoleErrorSpy.mockRestore();
    });
});