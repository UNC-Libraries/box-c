import { createLocalVue, shallowMount } from '@vue/test-utils'
import VueRouter from 'vue-router';
import listDisplay from '@/components/listDisplay.vue';

const localVue = createLocalVue();
localVue.use(VueRouter);
const router = new VueRouter({
    routes: [
        {
            path: '/record/98bc503c-9603-4cd9-8a65-93a22520ef68',
            name: 'displayRecords'
        },
        {
            path: '/search/?anywhere=',
            name: 'searchRecords'
        }
    ]
});
let wrapper;

describe('listDisplay.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(listDisplay, {
            localVue,
            router,
            propsData: {
                recordList: [
                    {
                        "added": "2019-10-31T18:11:02.238Z",
                        "counts": {
                            "child": 0
                        },
                        "title": "boingo",
                        "type": "Folder",
                        "contentStatus": [
                            "Not Described"
                        ],
                        "rollup": "87e49168-551b-4127-8252-9d13ed8abe24",
                        "objectPath": [
                            {
                                "pid": "collections",
                                "name": "Content Collections Root",
                                "container": true
                            },
                            {
                                "pid": "0410e5c1-a036-4b7c-8d7d-63bfda2d6a36",
                                "name": "testAdminUnit",
                                "container": true
                            },
                            {
                                "pid": "d77fd8c9-744b-42ab-8e20-5ad9bdf8194e",
                                "name": "testCollection",
                                "container": true
                            },
                            {
                                "pid": "87e49168-551b-4127-8252-9d13ed8abe24",
                                "name": "boingo",
                                "container": true
                            }
                        ],
                        "id": "87e49168-551b-4127-8252-9d13ed8abe24",
                        "updated": "2019-10-31T18:22:41.858Z",
                        "timestamp": 1572621100487
                    },
                    {
                        "added": "2019-07-29T19:50:43.588Z",
                        "counts": {
                            "child": 0
                        },
                        "thumbnail_url": "https:\/\/localhost:8080\/services\/api\/thumb\/aaa66f91-4870-4937-b7ba-06b015959e4f\/large",
                        "title": "Imagy",
                        "type": "Work",
                        "contentStatus": [
                            "Described",
                            "Has Primary Object"
                        ],
                        "rollup": "e3931f0d-a32f-4b84-b6a4-4baf0ca9b576",
                        "objectPath": [
                            {
                                "pid": "collections",
                                "name": "Content Collections Root",
                                "container": true
                            },
                            {
                                "pid": "0410e5c1-a036-4b7c-8d7d-63bfda2d6a36",
                                "name": "testAdminUnit",
                                "container": true
                            },
                            {
                                "pid": "d77fd8c9-744b-42ab-8e20-5ad9bdf8194e",
                                "name": "testCollection",
                                "container": true
                            },
                            {
                                "pid": "e3931f0d-a32f-4b84-b6a4-4baf0ca9b576",
                                "name": "Imagy",
                                "container": true
                            }
                        ],
                        "datastream": [
                            "techmd_fits|text\/xml|aaa66f91-4870-4937-b7ba-06b015959e4f.xml|xml|5480|urn:sha1:82c71472051b8279a0fbaa537a340c57e3d842f6|aaa66f91-4870-4937-b7ba-06b015959e4f",
                            "original_file|image\/png|Screen+Shot+2018-10-17+at+3.02.53+PM.png|png|232738|urn:md5:f5397b230bb5dfa4d53f57ad35514405|aaa66f91-4870-4937-b7ba-06b015959e4f",
                            "thumbnail_small|image\/png|aaa66f91-4870-4937-b7ba-06b015959e4f.png|png|2454||aaa66f91-4870-4937-b7ba-06b015959e4f",
                            "thumbnail_large|image\/png|aaa66f91-4870-4937-b7ba-06b015959e4f.png|png|5892||aaa66f91-4870-4937-b7ba-06b015959e4f"
                        ],
                        "ancestorPath": [
                            {
                                "id": "collections",
                                "title": "collections"
                            },
                            {
                                "id": "0410e5c1-a036-4b7c-8d7d-63bfda2d6a36",
                                "title": "0410e5c1-a036-4b7c-8d7d-63bfda2d6a36"
                            },
                            {
                                "id": "d77fd8c9-744b-42ab-8e20-5ad9bdf8194e",
                                "title": "d77fd8c9-744b-42ab-8e20-5ad9bdf8194e"
                            }
                        ],
                        "_version_": 1.6490127429584e+18,
                        "id": "e3931f0d-a32f-4b84-b6a4-4baf0ca9b576",
                        "updated": "2019-10-29T17:22:01.830Z",
                        "timestamp": 1572621100384
                    }
                ]
            }
        });
    });

    it("displays records", () => {
        expect(wrapper.vm.recordList.length).toEqual(2);
    });

    it("finds a record's collection", () => {
        expect(wrapper.vm.collectionInfo(wrapper.vm.recordList[1].objectPath)).toEqual('testCollection');
    });

    it("finds a record's file type", () => {
        expect(wrapper.vm.getFileType(wrapper.vm.recordList[0].datastream)).toEqual('');
        expect(wrapper.vm.getFileType(wrapper.vm.recordList[1].datastream)).toEqual('png');
    });
});