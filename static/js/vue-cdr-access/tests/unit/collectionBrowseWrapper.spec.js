import { shallowMount } from '@vue/test-utils'
import collectionBrowseWrapper from '@/components/collectionBrowseWrapper.vue';
import moxios from "moxios";
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
        moxios.install();

        wrapper = shallowMount(collectionBrowseWrapper, {
            global: {
                plugins: [i18n]
            }
        });
    });

    it("retrieves data", (done) => {
        moxios.stubRequest('collectionsJson', {
            status: 200,
            response: JSON.stringify(response)
        });
        wrapper.vm.retrieveData();

        moxios.wait(() => {
            expect(wrapper.vm.records).toEqual(response.metadata);
            done();
        });
    });

    it("displays a '503 page' if JSON responds with an error", (done) => {
        moxios.stubRequest('collectionsJson', {
            status: 503,
            response: JSON.stringify({ message: 'bad stuff happened' })
        });
        wrapper.vm.retrieveData();

        moxios.wait(() => {
            expect(wrapper.findComponent({ name: 'notAvailable'}).exists()).toBe(true);
            done();
        });
    });

    afterEach(() => {
        moxios.uninstall();
    });
});