import { mount } from '@vue/test-utils';
import preIngest from '@/components/chompb/preIngest.vue';
import moxios from 'moxios';
import structuredClone from '@ungap/structured-clone';

global.structuredClone = structuredClone;

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
            push: jest.fn(),
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
        moxios.install();
    });

    afterEach(function () {
        moxios.uninstall();
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
        let updatedInfo = structuredClone(project_info);
        updatedInfo[0].processingJobs['velocicroptor'] = { 'status' : 'completed' };
        setupWrapper(updatedInfo);

        let rows = wrapper.findAll('.datatable tbody tr');
        let actions1 = rows[0].findAll('a');
        expect(actions1[0].text()).toBe('Copy Path');
        expect(actions1[1].text()).toBe('Crop color bars');
        expect(actions1[2].text()).toBe('View crop report');
        expect(actions1.length).toBe(3);

        await actions1[2].trigger('click');

        expect(mockRouter.push).toHaveBeenCalledWith('/admin/chompb/project/file_source_test/processing_results/velocicroptor');
    });

    it("clicking on the crop button causes request to be made", (done) => {
        // suppressing error spam from jsdom when making http requests with moxios/axios
        const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});

        setupWrapper(project_info);

        moxios.stubRequest(`/admin/chompb/project/file_source_test/action/velocicroptor`, {
            status: 200,
            response: JSON.stringify({'action' : 'Start cropping for project file_source_test'})
        });

        // Mock window.confirm
        const confirmMock = jest.spyOn(window, 'confirm');

        // Mock return values for confirm
        confirmMock.mockImplementationOnce(() => true);  // Simulate "Yes" click


        moxios.wait(async () => {
            let rows = wrapper.findAll('.datatable tbody tr');
            let actions1 = rows[0].findAll('a');
            expect(actions1[1].text()).toBe('Crop color bars');
            await actions1[1].trigger('click');

            moxios.wait(() => {
                let request = moxios.requests.mostRecent();

                expect(request.config.method).toEqual('post');
                expect(window.confirm).toHaveBeenCalledWith('Are you sure you want to crop color bars for this project?');
                // crop option should have changed from a link to a span
                rows = wrapper.findAll('.datatable tbody tr');
                let actions2 = rows[0].findAll('span');
                expect(actions2[0].text()).toBe('Crop in progress');

                consoleErrorSpy.mockRestore();
                confirmMock.mockRestore();
                done();
            });
        });
    });
});