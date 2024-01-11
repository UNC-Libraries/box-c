import { shallowMount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import listDisplay from '@/components/listDisplay.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from "vue-i18n";
import translations from "@/translations";
import {records_list} from "../fixtures/recordListFixture";

const router = createRouter({
    history: createWebHistory(process.env.BASE_URL),
    routes: [
        {
            path: '/record/:uuid',
            name: 'displayRecords',
            component: displayWrapper
        }
    ]
});
let wrapper;

describe('listDisplay.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        sessionStorage.clear();
        wrapper = shallowMount(listDisplay, {
            global: {
                plugins: [router, i18n]
            },
            props: {
                recordList: records_list
            }
        });
    });

    afterEach(() => {
        wrapper = null;
    });

    it("displays records", () => {
        expect(wrapper.vm.recordList.length).toEqual(2);
    });

    it("finds a record's collection", () => {
        expect(wrapper.vm.collectionInfo(wrapper.vm.recordList[1].objectPath)).toEqual('testCollection');
    });

    it("set a default browse type for record links when not excluding them", async () => {
        await wrapper.setProps({
            excludeBrowseTypeFromRecordUrls: false
        });
        expect(wrapper.vm.linkBrowseType).toBe('list-display');
    });

    it("sets no browse type if parameter indicates it should be excluded", async () => {
        await wrapper.setProps({
            excludeBrowseTypeFromRecordUrls: true
        });
        expect(wrapper.vm.linkBrowseType).toBeNull();
    });

    it("sets browse type form sessionStorage for record links when saved browse type should be used", async () => {
        sessionStorage.setItem('browse-type', 'list-display');
        wrapper = shallowMount(listDisplay, {
            global: {
                plugins: [router]
            },
            props: {
                useSavedBrowseType: true
            }
        });
        await wrapper.vm.$nextTick();
        expect(wrapper.vm.linkBrowseType).toBe('list-display');
    });

    it("doesn't display a collection number if one isn't set", async () => {
        wrapper = shallowMount(listDisplay, {
            global: {
                plugins: [router, i18n]
            },
            props: {
                useSavedBrowseType: true
            }
        });
        await wrapper.vm.$nextTick();
        expect(wrapper.findAll('.collection_id').length).toEqual(0);
    });

    it("displays a collection number if one is set", async () => {
        wrapper = shallowMount(listDisplay, {
            global: {
                plugins: [router, i18n]
            },
            props: {
                useSavedBrowseType: true
            }
        });
        await wrapper.vm.$nextTick();
        expect(wrapper.findAll('.collection_id').length).toEqual(0);
    });

    it("displays a collection number if one is set", async () => {
        wrapper = shallowMount(listDisplay, {
            global: {
                plugins: [router, i18n]
            },
            props: {
                useSavedBrowseType: true,
                recordList: [
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
                                "container": true,
                                "collectionId": "12345"
                            }
                        ]
                    }
                ]
            }
        });
        await wrapper.vm.$nextTick();
        expect(wrapper.findAll('.collection_id').length).toEqual(1);
    });
});