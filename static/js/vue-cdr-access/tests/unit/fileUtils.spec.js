import { shallowMount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import fileList from '@/components/full_record/fileList.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';

const recordData = {}

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

        wrapper = shallowMount(fileList, {
            global: {
                plugins: [i18n, router]
            },
            props: {
                recordData: recordData
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

    it("determines a file type from record file type", () => {
        const briefObj = {
            fileType: ['audio/mpeg']
        }
        expect(wrapper.vm.getFileType(briefObj)).toEqual('audio/mpeg');
    });

    it("determines a file type from record file description", () => {
        const briefObj = {
            fileDesc: ['MP3']
        }
        expect(wrapper.vm.getFileType(briefObj)).toEqual('MP3');
    });

    it("returns an empty string if no file type is available", () => {
        expect(wrapper.vm.getFileType({})).toEqual('');
    });
});