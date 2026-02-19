import { shallowMount } from '@vue/test-utils'
import { describe, it, expect, beforeEach } from 'vitest';
import collectionBrowseWrapper from '@/components/collectionBrowseWrapper.vue';
import mockAxios from 'vitest-mock-axios';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';

let response = {
    "metadata": [
        {
            "added": "2019-10-18T13:23:17.626Z",
            "counts": {
                "child": 3
            },
            "title": "DansAdminUnit",
            "type": "AdminUnit",
            "objectPath": [
                {
                    "pid": "collections",
                    "name": "Content Collections Root",
                    "container": true
                },
                {
                    "pid": "80c1438a-dd65-4712-a1b4-9ee2d855fc6d",
                    "name": "DansAdminUnit",
                    "container": true
                }
            ],
            "id": "80c1438a-dd65-4712-a1b4-9ee2d855fc6d",
            "updated": "2019-10-18T13:23:17.626Z"
        },
        {
            "added": "2019-07-29T17:21:38.344Z",
            "counts": {
                "child": 16
            },
            "title": "DeansAdminUnit",
            "type": "AdminUnit",
            "objectPath": [
                {
                    "pid": "collections",
                    "name": "Content Collections Root",
                    "container": true
                },
                {
                    "pid": "0410e5c1-a036-4b7c-8d7d-63bfda2d6a36",
                    "name": "DeansAdminUnit",
                    "container": true
                }
            ],
            "id": "0410e5c1-a036-4b7c-8d7d-63bfda2d6a36",
            "updated": "2019-09-16T19:16:07.225Z"
        }
    ]
};

let wrapper;

describe('collectionBrowseWrapper.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        wrapper = shallowMount(collectionBrowseWrapper, {
            global: {
                plugins: [i18n]
            }
        });
    });

    it("retrieves data", async () => {
        mockAxios.mockResponse({
            status: 200,
            response: response,
            url: 'api/collectionsJson'
        });
        wrapper.vm.retrieveData();
        await wrapper.vm.$nextTick();

        expect(wrapper.vm.records).toEqual(response.metadata);
    });

    it("displays a '503 page' if JSON responds with an error", async () => {
        mockAxios.mockResponse({
            status: 503,
            response: { message: 'bad stuff happened' },
            url: 'api/collectionsJson'
        });
        wrapper.vm.retrieveData();
        await wrapper.vm.$nextTick();

        expect(wrapper.findComponent({ name: 'notAvailable'}).exists()).toBe(true);
    });
});