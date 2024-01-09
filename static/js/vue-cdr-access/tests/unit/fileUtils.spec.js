import { shallowMount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import displayWrapper from '@/components/displayWrapper.vue';
import listDisplay from '@/components/listDisplay.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';
import {records_list} from "../fixtures/recordListFixture";

let wrapper, router;

describe('fileUtils', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        router = createRouter({
            history: createWebHistory(process.env.BASE_URL),
            routes: [
                {
                    path: '/record/:uuid',
                    name: 'displayRecords',
                    component: displayWrapper
                }
            ]
        });

        wrapper = shallowMount(listDisplay, {
            global: {
                plugins: [i18n, router]
            },
            props: {
                recordList: records_list
            }
        });
    });

    it("gets original file values", () => {
        const datastream = [
                "techmd_fits|text/xml|techmd_fits.xml|xml|4192|urn:sha1:e3150af2b1e846cc96a8e6da428ae619b1502240|8a2f05e5-d2b7-4857-ae71-c24aa28484c1|",
                "original_file|audio/mpeg|180618_003.MP3|MP3|35845559|urn:sha1:6ad9e78fef388cc7e1e6aa075eaf2cee3699f181|8a2f05e5-d2b7-4857-ae71-c24aa28484c1|",
                "md_descriptive|text/xml|md_descriptive.xml|xml|366|urn:sha1:bc6aa00b1b046a01298cd0dfff7ad250cc29a74d||",
                "event_log|application/n-triples|event_log.nt|nt|4521|urn:sha1:ed5db93d079a62f7a939396c721833808409748e||"
            ];
        expect(wrapper.vm.getOriginalFileValue(datastream, 'file_size')).toEqual('34.2 MB');
        expect(wrapper.vm.getOriginalFileValue(datastream, 'file_type')).toEqual('MP3');
    })

    it("formats file size", () => {
        expect(wrapper.vm.formatFilesize(10000)).toEqual('9.8 KB');
    });

    it("formats file size to O B if invalid data is given", () => {
        expect(wrapper.vm.formatFilesize('bad data')).toEqual('0 B');
    });

    it("file type empty when no file format info", () => {
        expect(wrapper.vm.getFileType(wrapper.vm.recordList[0])).toEqual('');
    });

    it("file type from description", () => {
        expect(wrapper.vm.getFileType(wrapper.vm.recordList[1])).toEqual('Portable Network Graphics');
    });

    it("file type from description and displays 'Various' if more than one fileType and they are different", () => {
        expect(wrapper.vm.getFileType({
            "title": "Imagy File",
            "type": "File",
            "datastream": [
                "techmd_fits|text\/xml|aaa66f91-4870-4937-b7ba-06b015959e4f.xml|xml|5480|urn:sha1:82c71472051b8279a0fbaa537a340c57e3d842f6|aaa66f91-4870-4937-b7ba-06b015959e4f",
                "original_file|image\/png|Screen+Shot+2018-10-17+at+3.02.53+PM.png|png|232738|urn:md5:f5397b230bb5dfa4d53f57ad35514405|aaa66f91-4870-4937-b7ba-06b015959e4f",
                "thumbnail_small|image\/png|aaa66f91-4870-4937-b7ba-06b015959e4f.png|png|2454||aaa66f91-4870-4937-b7ba-06b015959e4f",
                "thumbnail_large|image\/png|aaa66f91-4870-4937-b7ba-06b015959e4f.png|png|5892||aaa66f91-4870-4937-b7ba-06b015959e4f"
            ],
            "fileDesc": ["Portable Network Graphics", "Joint Photographic Experts Group"],
            "fileType": [
                "image/png", "image/jpeg"
            ],
            "fileCategory": [
                "Image"
            ],
            "id": "e3931f0d-a32f-4b84-b6a4-4baf0ca9b576",
            "updated": "2019-10-29T17:22:01.830Z",
        })).toEqual('Various');
    });

    it("file type from description and displays the filetype if more than one fileType and they are the same", () => {
        expect(wrapper.vm.getFileType({
            "title": "Imagy File",
            "type": "File",
            "datastream": [
                "techmd_fits|text\/xml|aaa66f91-4870-4937-b7ba-06b015959e4f.xml|xml|5480|urn:sha1:82c71472051b8279a0fbaa537a340c57e3d842f6|aaa66f91-4870-4937-b7ba-06b015959e4f",
                "original_file|image\/png|Screen+Shot+2018-10-17+at+3.02.53+PM.png|png|232738|urn:md5:f5397b230bb5dfa4d53f57ad35514405|aaa66f91-4870-4937-b7ba-06b015959e4f",
                "thumbnail_small|image\/png|aaa66f91-4870-4937-b7ba-06b015959e4f.png|png|2454||aaa66f91-4870-4937-b7ba-06b015959e4f",
                "thumbnail_large|image\/png|aaa66f91-4870-4937-b7ba-06b015959e4f.png|png|5892||aaa66f91-4870-4937-b7ba-06b015959e4f"
            ],
            "fileDesc": ["Portable Network Graphics", "Portable Network Graphics"],
            "fileType": [
                "image/png", "image/png"
            ],
            "fileCategory": [
                "Image"
            ],
            "id": "e3931f0d-a32f-4b84-b6a4-4baf0ca9b576",
            "updated": "2019-10-29T17:22:01.830Z",
        })).toEqual('Portable Network Graphics');
    });

    it("file type falls back to mimetype when description empty", () => {
        expect(wrapper.vm.getFileType({
            "title": "Imagy File",
            "type": "File",
            "datastream": [
                "techmd_fits|text\/xml|aaa66f91-4870-4937-b7ba-06b015959e4f.xml|xml|5480|urn:sha1:82c71472051b8279a0fbaa537a340c57e3d842f6|aaa66f91-4870-4937-b7ba-06b015959e4f",
                "original_file|image\/png|Screen+Shot+2018-10-17+at+3.02.53+PM.png|png|232738|urn:md5:f5397b230bb5dfa4d53f57ad35514405|aaa66f91-4870-4937-b7ba-06b015959e4f",
                "thumbnail_small|image\/png|aaa66f91-4870-4937-b7ba-06b015959e4f.png|png|2454||aaa66f91-4870-4937-b7ba-06b015959e4f",
                "thumbnail_large|image\/png|aaa66f91-4870-4937-b7ba-06b015959e4f.png|png|5892||aaa66f91-4870-4937-b7ba-06b015959e4f"
            ],
            "fileDesc": [],
            "fileType": [
                "image/png"
            ],
            "fileCategory": [
                "Image"
            ],
            "id": "e3931f0d-a32f-4b84-b6a4-4baf0ca9b576",
            "updated": "2019-10-29T17:22:01.830Z",
        })).toEqual('image/png');
    });

    it("file type falls back to mimetype when there's no description and displays 'Various' if more than one fileType", () => {
        expect(wrapper.vm.getFileType({
            "title": "Imagy File",
            "type": "File",
            "datastream": [
                "techmd_fits|text\/xml|aaa66f91-4870-4937-b7ba-06b015959e4f.xml|xml|5480|urn:sha1:82c71472051b8279a0fbaa537a340c57e3d842f6|aaa66f91-4870-4937-b7ba-06b015959e4f",
                "original_file|image\/png|Screen+Shot+2018-10-17+at+3.02.53+PM.png|png|232738|urn:md5:f5397b230bb5dfa4d53f57ad35514405|aaa66f91-4870-4937-b7ba-06b015959e4f",
                "thumbnail_small|image\/png|aaa66f91-4870-4937-b7ba-06b015959e4f.png|png|2454||aaa66f91-4870-4937-b7ba-06b015959e4f",
                "thumbnail_large|image\/png|aaa66f91-4870-4937-b7ba-06b015959e4f.png|png|5892||aaa66f91-4870-4937-b7ba-06b015959e4f"
            ],
            "fileDesc": [],
            "fileType": [
                "image/png", "image/jpeg"
            ],
            "fileCategory": [
                "Image"
            ],
            "id": "e3931f0d-a32f-4b84-b6a4-4baf0ca9b576",
            "updated": "2019-10-29T17:22:01.830Z",
        })).toEqual('Various');
    });

    it("file type falls back to mimetype when there's no description and displays filetype if all filetypes are the same", () => {
        expect(wrapper.vm.getFileType({
            "title": "Imagy File",
            "type": "File",
            "datastream": [
                "techmd_fits|text\/xml|aaa66f91-4870-4937-b7ba-06b015959e4f.xml|xml|5480|urn:sha1:82c71472051b8279a0fbaa537a340c57e3d842f6|aaa66f91-4870-4937-b7ba-06b015959e4f",
                "original_file|image\/png|Screen+Shot+2018-10-17+at+3.02.53+PM.png|png|232738|urn:md5:f5397b230bb5dfa4d53f57ad35514405|aaa66f91-4870-4937-b7ba-06b015959e4f",
                "thumbnail_small|image\/png|aaa66f91-4870-4937-b7ba-06b015959e4f.png|png|2454||aaa66f91-4870-4937-b7ba-06b015959e4f",
                "thumbnail_large|image\/png|aaa66f91-4870-4937-b7ba-06b015959e4f.png|png|5892||aaa66f91-4870-4937-b7ba-06b015959e4f"
            ],
            "fileDesc": [],
            "fileType": [
                "image/png", "image/png"
            ],
            "fileCategory": [
                "Image"
            ],
            "id": "e3931f0d-a32f-4b84-b6a4-4baf0ca9b576",
            "updated": "2019-10-29T17:22:01.830Z",
        })).toEqual('image/png');
    });

    it("file type falls back to mimetype when description not present", () => {
        expect(wrapper.vm.getFileType({
            "title": "Imagy File",
            "type": "File",
            "datastream": [
                "techmd_fits|text\/xml|aaa66f91-4870-4937-b7ba-06b015959e4f.xml|xml|5480|urn:sha1:82c71472051b8279a0fbaa537a340c57e3d842f6|aaa66f91-4870-4937-b7ba-06b015959e4f",
                "original_file|image\/png|Screen+Shot+2018-10-17+at+3.02.53+PM.png|png|232738|urn:md5:f5397b230bb5dfa4d53f57ad35514405|aaa66f91-4870-4937-b7ba-06b015959e4f",
                "thumbnail_small|image\/png|aaa66f91-4870-4937-b7ba-06b015959e4f.png|png|2454||aaa66f91-4870-4937-b7ba-06b015959e4f",
                "thumbnail_large|image\/png|aaa66f91-4870-4937-b7ba-06b015959e4f.png|png|5892||aaa66f91-4870-4937-b7ba-06b015959e4f"
            ],
            "fileType": [
                "image/png"
            ],
            "fileCategory": [
                "Image"
            ],
            "id": "e3931f0d-a32f-4b84-b6a4-4baf0ca9b576",
            "updated": "2019-10-29T17:22:01.830Z",
        })).toEqual('image/png');
    });
});