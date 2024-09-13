import { shallowMount } from '@vue/test-utils';
import preIngest from '@/components/chompb/preIngest.vue';

let wrapper;

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
        "allowedActions": ["color_bar_crop", "color_bar_report"],
        "projectPath": "/test_path_one/file_source_test"
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
        "projectPath": "/test_path_two/file_source_test"
    }
];

describe('preIngest.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(preIngest, {
            global: {
                stubs: {
                    teleport: true
                }
            },
            data() {
                return {
                    dataSet: project_info
                }
            }
        });
    });

    it("contains a table of projects", () => {
        expect(wrapper.findComponent({ name: 'dataTable' }).exists()).toBe(true);
    });
});