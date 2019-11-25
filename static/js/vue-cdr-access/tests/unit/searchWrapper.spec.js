import { createLocalVue, shallowMount } from '@vue/test-utils'
import VueRouter from 'vue-router';
import searchWrapper from '@/components/searchWrapper.vue';
import moxios from "moxios";

const localVue = createLocalVue();
localVue.use(VueRouter);
const router = new VueRouter({
    routes: [
        {
            path: '/search/:uuid?',
            name: 'searchRecords'
        }
    ]
});
const response = {
    "container": {
        "ancestorNames": "/Content Collections Root",
        "added": "2019-07-29T17:20:57.813Z",
        "id": "collections",
        "title": "Content Collections Root",
        "type": "ContentRoot",
        "updated": "2019-07-29T17:20:57.883Z",
        "contentStatus": [
            "Not Described"
        ],
        "rollup": "collections",
        "timestamp": 1564420860864
    },
    "pageRows": 20,
    "searchQueryUrl": "anywhere=",
    "metadata": [
        {
            "added": "2019-08-21T15:11:02.034Z",
            "counts": {
                "child": 1
            },
            "thumbnail_url": "https://localhost:8080/services/api/thumb/0367e115-fc95-4f61-8539-e9a77cf00d8a/large",
            "title": "test stuff",
            "type": "Work",
            "contentStatus": [
                "Described",
                "Has Primary Object"
            ],
            "rollup": "64e6883d-3667-4e5b-893d-c6e851ac738a",
            "datastream": [
                "techmd_fits|text/xml|0367e115-fc95-4f61-8539-e9a77cf00d8a.xml|xml|5480|urn:sha1:4b74895e0f119890a1c0ae1e329683e4cb8264af|0367e115-fc95-4f61-8539-e9a77cf00d8a",
                "original_file|image/png|agent.png|png|174979|urn:md5:68e519e0c42adada7cb543a7c0193101|0367e115-fc95-4f61-8539-e9a77cf00d8a",
                "jp2|image/jp2|0367e115-fc95-4f61-8539-e9a77cf00d8a.jp2|jp2|427985||0367e115-fc95-4f61-8539-e9a77cf00d8a",
                "thumbnail_small|image/png|0367e115-fc95-4f61-8539-e9a77cf00d8a.png|png|1445||0367e115-fc95-4f61-8539-e9a77cf00d8a",
                "thumbnail_large|image/png|0367e115-fc95-4f61-8539-e9a77cf00d8a.png|png|3602||0367e115-fc95-4f61-8539-e9a77cf00d8a"
            ],
            "_version_": 1649012742492782600,
            "ancestorNames": "/Content Collections Root/DeansAdminUnit/deansCollection/DeansStuff/sportz/dean",
            "id": "64e6883d-3667-4e5b-893d-c6e851ac738a",
            "updated": "2019-08-21T15:11:02.034Z",
            "timestamp": 1572621099939
        }
    ],
    "resultCount": 1
};
let wrapper;

describe('searchWrapper.vue', () => {
    beforeEach(() => {
        moxios.install();

        wrapper = shallowMount(searchWrapper, {
            localVue,
            router
        });

        wrapper.vm.$router.currentRoute.query.anywhere = '';
        wrapper.vm.retrieveData();
        moxios.stubRequest(`searchJson/?anywhere=`, {
            status: 200,
            response: JSON.stringify(response)
        });
    });

    it("retrieves data", (done) => {
        moxios.wait(() => {
            expect(wrapper.vm.records).toEqual(response.metadata);
            done();
        });
    });

    it("displays start and end records for current page", (done) => {
        moxios.wait(() => {
            expect(wrapper.vm.recordDisplayCounts).toEqual('1-1');
            done();
        });
    });

    afterEach(() => {
        moxios.uninstall();
    });
});