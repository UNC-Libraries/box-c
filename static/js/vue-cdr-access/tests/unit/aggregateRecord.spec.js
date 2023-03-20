import { shallowMount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import aggregateRecord from '@/components/full_record/aggregateRecord.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';
import moxios from 'moxios';
import cloneDeep from 'lodash.clonedeep';

const record = {
    "briefObject": {
        "pid": {
            "id": "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
            "qualifier": "content",
            "qualifiedId": "content/e2f0d544-4f36-482c-b0ca-ba11f1251c01",
            "componentId": "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
            "repositoryUri": "http://localhost:8181/fcrepo/rest/content/e2/f0/d5/44/e2f0d544-4f36-482c-b0ca-ba11f1251c01",
            "repositoryPath": "http://localhost:8181/fcrepo/rest/content/e2/f0/d5/44/e2f0d544-4f36-482c-b0ca-ba11f1251c01",
            "pid": "uuid:e2f0d544-4f36-482c-b0ca-ba11f1251c01",
            "uri": "http://localhost:8181/fcrepo/rest/content/e2/f0/d5/44/e2f0d544-4f36-482c-b0ca-ba11f1251c01",
            "uuid": "e2f0d544-4f36-482c-b0ca-ba11f1251c01"
        },
        "fields": {
            "adminGroup": [
                "unc:onyen:test_user"
            ],
            "filesizeTotal": 4887,
            "readGroup": [
                "authenticated",
                "everyone",
                "unc:onyen:test_user"
            ],
            "language": [
                "English"
            ],
            "fileFormatType": [
                "audio/mpeg"
            ],
            "title": "Listen for real",
            "dateAdded": 1678200466863,
            "rollup": "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
            "parentCollection": "testCollection|fc77a9be-b49d-4f4e-b656-1644c9e964fc",
            "contentStatus": [
                "Described",
                "Has Primary Object",
                "Members Are Unordered"
            ],
            "datastream": [
                "techmd_fits|text/xml|techmd_fits.xml|xml|4192|urn:sha1:e3150af2b1e846cc96a8e6da428ae619b1502240|8a2f05e5-d2b7-4857-ae71-c24aa28484c1|",
                "original_file|audio/mpeg|180618_003.MP3|MP3|35845559|urn:sha1:6ad9e78fef388cc7e1e6aa075eaf2cee3699f181|8a2f05e5-d2b7-4857-ae71-c24aa28484c1|",
                "md_descriptive|text/xml|md_descriptive.xml|xml|366|urn:sha1:bc6aa00b1b046a01298cd0dfff7ad250cc29a74d||",
                "event_log|application/n-triples|event_log.nt|nt|4521|urn:sha1:ed5db93d079a62f7a939396c721833808409748e||"
            ],
            "ancestorPath": [
                "1,collections",
                "2,353ee09f-a4ed-461e-a436-18a1bee77b01",
                "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc"
            ],
            "ancestorIds": "/collections/353ee09f-a4ed-461e-a436-18a1bee77b01/fc77a9be-b49d-4f4e-b656-1644c9e964fc/e2f0d544-4f36-482c-b0ca-ba11f1251c01",
            "lastIndexed": 1678200473004,
            "id": "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
            "keyword": [
                "text",
                "e2f0d544-4f36-482c-b0ca-ba11f1251c01"
            ],
            "roleGroup": [
                "canDescribe|unc:onyen:test_user",
                "canViewOriginals|authenticated",
                "canViewMetadata|everyone"
            ],
            "timestamp": 1678200478992,
            "parentUnit": "testAdminUnit|353ee09f-a4ed-461e-a436-18a1bee77b01",
            "dateUpdated": 1678200469519,
            "fileFormatDescription": [
                "MP3"
            ],
            "_version_": 1759720745467904000,
            "filesizeSort": 35845559,
            "fileFormatCategory": [
                "Audio"
            ],
            "status": [
                "Inherited Patron Settings"
            ],
            "resourceType": "Work"
        },
        "ancestorPathFacet": {
            "fieldName": "ANCESTOR_PATH",
            "count": 0,
            "displayValue": "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
            "facetNodes": [
                {
                    "searchValue": "1,collections",
                    "searchKey": "collections",
                    "facetValue": "1,collections",
                    "tier": 1,
                    "limitToValue": "1,collections!2",
                    "displayValue": "collections",
                    "pivotValue": "2,"
                },
                {
                    "searchValue": "2,353ee09f-a4ed-461e-a436-18a1bee77b01",
                    "searchKey": "353ee09f-a4ed-461e-a436-18a1bee77b01",
                    "facetValue": "2,353ee09f-a4ed-461e-a436-18a1bee77b01",
                    "tier": 2,
                    "limitToValue": "2,353ee09f-a4ed-461e-a436-18a1bee77b01!3",
                    "displayValue": "353ee09f-a4ed-461e-a436-18a1bee77b01",
                    "pivotValue": "3,"
                },
                {
                    "searchValue": "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                    "searchKey": "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                    "facetValue": "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                    "tier": 3,
                    "limitToValue": "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc!4",
                    "displayValue": "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                    "pivotValue": "4,"
                }
            ],
            "cutoff": 4,
            "searchKey": "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
            "searchValue": "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc",
            "limitToValue": "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc!4",
            "highestTier": 3,
            "highestTierNode": {
                "searchValue": "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                "searchKey": "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                "facetValue": "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                "tier": 3,
                "limitToValue": "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc!4",
                "displayValue": "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                "pivotValue": "4,"
            },
            "pivotValue": "4,"
        },
        "path": {
            "fieldName": "ANCESTOR_PATH",
            "count": 0,
            "displayValue": "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
            "facetNodes": [
                {
                    "searchValue": "1,collections",
                    "searchKey": "collections",
                    "facetValue": "1,collections",
                    "tier": 1,
                    "limitToValue": "1,collections!2",
                    "displayValue": "collections",
                    "pivotValue": "2,"
                },
                {
                    "searchValue": "2,353ee09f-a4ed-461e-a436-18a1bee77b01",
                    "searchKey": "353ee09f-a4ed-461e-a436-18a1bee77b01",
                    "facetValue": "2,353ee09f-a4ed-461e-a436-18a1bee77b01",
                    "tier": 2,
                    "limitToValue": "2,353ee09f-a4ed-461e-a436-18a1bee77b01!3",
                    "displayValue": "353ee09f-a4ed-461e-a436-18a1bee77b01",
                    "pivotValue": "3,"
                },
                {
                    "searchValue": "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                    "searchKey": "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                    "facetValue": "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                    "tier": 3,
                    "limitToValue": "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc!4",
                    "displayValue": "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                    "pivotValue": "4,"
                },
                {
                    "searchValue": "4,e2f0d544-4f36-482c-b0ca-ba11f1251c01",
                    "searchKey": "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
                    "facetValue": "4,e2f0d544-4f36-482c-b0ca-ba11f1251c01",
                    "tier": 4,
                    "limitToValue": "4,e2f0d544-4f36-482c-b0ca-ba11f1251c01!5",
                    "displayValue": "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
                    "pivotValue": "5,"
                }
            ],
            "searchKey": "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
            "searchValue": "4,e2f0d544-4f36-482c-b0ca-ba11f1251c01",
            "limitToValue": "4,e2f0d544-4f36-482c-b0ca-ba11f1251c01!5",
            "highestTier": 4,
            "highestTierNode": {
                "searchValue": "4,e2f0d544-4f36-482c-b0ca-ba11f1251c01",
                "searchKey": "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
                "facetValue": "4,e2f0d544-4f36-482c-b0ca-ba11f1251c01",
                "tier": 4,
                "limitToValue": "4,e2f0d544-4f36-482c-b0ca-ba11f1251c01!5",
                "displayValue": "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
                "pivotValue": "5,"
            },
            "pivotValue": "5,"
        },
        "objectPath": {
            "entries": [
                {
                    "pid": "collections",
                    "name": "Content Collections Root",
                    "container": true
                },
                {
                    "pid": "353ee09f-a4ed-461e-a436-18a1bee77b01",
                    "name": "testAdminUnit",
                    "container": true
                },
                {
                    "pid": "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                    "name": "testCollection",
                    "container": true
                },
                {
                    "pid": "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
                    "name": "Listen for real",
                    "container": true
                }
            ]
        },
        "ancestorNames": "/Content Collections Root/testAdminUnit/testCollection/Listen for real",
        "datastreamObjects": [
            {
                "owner": "8a2f05e5-d2b7-4857-ae71-c24aa28484c1",
                "name": "techmd_fits",
                "filesize": 4192,
                "mimetype": "text/xml",
                "filename": "techmd_fits.xml",
                "extension": "xml",
                "checksum": "urn:sha1:e3150af2b1e846cc96a8e6da428ae619b1502240",
                "extent": "",
                "datastreamIdentifier": "8a2f05e5-d2b7-4857-ae71-c24aa28484c1/techmd_fits"
            },
            {
                "owner": "8a2f05e5-d2b7-4857-ae71-c24aa28484c1",
                "name": "original_file",
                "filesize": 35845559,
                "mimetype": "audio/mpeg",
                "filename": "180618_003.MP3",
                "extension": "MP3",
                "checksum": "urn:sha1:6ad9e78fef388cc7e1e6aa075eaf2cee3699f181",
                "extent": "",
                "datastreamIdentifier": "8a2f05e5-d2b7-4857-ae71-c24aa28484c1/original_file"
            },
            {
                "owner": "",
                "name": "md_descriptive",
                "filesize": 366,
                "mimetype": "text/xml",
                "filename": "md_descriptive.xml",
                "extension": "xml",
                "checksum": "urn:sha1:bc6aa00b1b046a01298cd0dfff7ad250cc29a74d",
                "extent": "",
                "datastreamIdentifier": "/md_descriptive"
            },
            {
                "owner": "",
                "name": "event_log",
                "filesize": 4521,
                "mimetype": "application/n-triples",
                "filename": "event_log.nt",
                "extension": "nt",
                "checksum": "urn:sha1:ed5db93d079a62f7a939396c721833808409748e",
                "extent": "",
                "datastreamIdentifier": "/event_log"
            }
        ],
        "groupRoleMap": {
            "authenticated": [
                "canViewOriginals"
            ],
            "everyone": [
                "canViewMetadata"
            ],
            "unc:onyen:test_user": [
                "canDescribe"
            ]
        },
        "countMap": {
            "child": 1
        },
        "parentCollectionName": "testCollection",
        "parentCollectionId": "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
        "ancestorPath": [
            "1,collections",
            "2,353ee09f-a4ed-461e-a436-18a1bee77b01",
            "3,fc77a9be-b49d-4f4e-b656-1644c9e964fc"
        ],
        "datastream": [
            "techmd_fits|text/xml|techmd_fits.xml|xml|4192|urn:sha1:e3150af2b1e846cc96a8e6da428ae619b1502240|8a2f05e5-d2b7-4857-ae71-c24aa28484c1|",
            "original_file|audio/mpeg|180618_003.MP3|MP3|35845559|urn:sha1:6ad9e78fef388cc7e1e6aa075eaf2cee3699f181|8a2f05e5-d2b7-4857-ae71-c24aa28484c1|",
            "md_descriptive|text/xml|md_descriptive.xml|xml|366|urn:sha1:bc6aa00b1b046a01298cd0dfff7ad250cc29a74d||",
            "event_log|application/n-triples|event_log.nt|nt|4521|urn:sha1:ed5db93d079a62f7a939396c721833808409748e||"
        ],
        "roleGroup": [
            "canDescribe|unc:onyen:test_user",
            "canViewOriginals|authenticated",
            "canViewMetadata|everyone"
        ],
        "idWithoutPrefix": "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
        "id": "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
        "language": [
            "English"
        ],
        "keyword": [
            "text",
            "e2f0d544-4f36-482c-b0ca-ba11f1251c01"
        ],
        "timestamp": 1678200478992,
        "status": [
            "Inherited Patron Settings"
        ],
        "title": "Listen for real",
        "resourceType": "Work",
        "parentCollection": "testCollection|fc77a9be-b49d-4f4e-b656-1644c9e964fc",
        "fileFormatCategory": [
            "Audio"
        ],
        "rollup": "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
        "ancestorIds": "/collections/353ee09f-a4ed-461e-a436-18a1bee77b01/fc77a9be-b49d-4f4e-b656-1644c9e964fc/e2f0d544-4f36-482c-b0ca-ba11f1251c01",
        "_version_": 1759720745467904000,
        "filesizeSort": 35845559,
        "filesizeTotal": 4887,
        "fileFormatType": [
            "audio/mpeg"
        ],
        "fileFormatDescription": [
            "MP3"
        ],
        "lastIndexed": 1678200473004,
        "readGroup": [
            "authenticated",
            "everyone",
            "unc:onyen:test_user"
        ],
        "adminGroup": [
            "unc:onyen:test_user"
        ],
        "contentStatus": [
            "Described",
            "Has Primary Object",
            "Members Are Unordered"
        ],
        "dateAdded": 1678200466863,
        "dateUpdated": 1678200469519,
        "parentUnit": "testAdminUnit|353ee09f-a4ed-461e-a436-18a1bee77b01"
    },
    "viewerPid": "8a2f05e5-d2b7-4857-ae71-c24aa28484c1",
    "viewerType": "audio"
}

let wrapper, router;

describe('aggregateRecord.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        moxios.install();
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

        wrapper = shallowMount(aggregateRecord, {
            global: {
                plugins: [i18n, router]
            },
            props: {
                recordData: record
            }
        });
    });

    afterEach(() => {
        wrapper = null;
        router = null
    });

    it("displays breadcrumbs", () => {
        expect(wrapper.findComponent({ name: 'breadCrumbs' }).exists()).toBe(true);
    });

    it("displays a work title", () => {
        expect(wrapper.find('h2').text()).toEqual('Listen for real');
    });

    it("does not display a finding aid link, if absent", () => {
        expect(wrapper.find('.finding-aid').exists()).toBe(false);
    });

    it("displays a finding aid link, if present", async () => {
        let updated_record = cloneDeep(record);
        updated_record.findingAidUrl = 'https://unc-finding-aid.lib.unc.edu';
        await wrapper.setProps({
            recordData: updated_record
        });
        expect(wrapper.find('.finding-aid').exists()).toBe(true);
    });

    it("does not allow users to edit the work by default", () => {
        expect(wrapper.find('a.edit').exists()).toBe(false);
    });

    it("allows users to edit the work with the proper permissions", async () => {
        let updated_record = cloneDeep(record);
        updated_record.canEditDescription = true;
        await wrapper.setProps({
            recordData: updated_record
        });
        expect(wrapper.find('a.edit').exists()).toBe(true);
    });

    it("does not set a download link by default", async () => {
        expect(wrapper.find('a.download').exists()).toBe(false);
    });

    it("allows users to download the work if a download link is present", async () => {
        let updated_record = cloneDeep(record);
        updated_record.briefObject.dataFileUrl = 'https://download-link.lib.unc.edu';
        await wrapper.setProps({
            recordData: updated_record
        });
        expect(wrapper.find('a.download').exists()).toBe(true);
    });

    it("displays an iframe viewer for images", async () => {
        let updated_record = cloneDeep(record);
        updated_record.viewerType = 'uv';
        updated_record.briefObject.groupRoleMap = {
            everyone: 'canViewAccessCopies'
        }
        await wrapper.setProps({
            recordData: updated_record
        });
        expect(wrapper.find('iframe').exists()).toBe(true);
    });

    it("displays an iframe viewer for images for logged in users", async () => {
        let updated_record = cloneDeep(record);
        updated_record.viewerType = 'uv';
        updated_record.briefObject.groupRoleMap = {
            everyone: 'canViewMetadata',
            authenticated: 'canViewAccessCopies'
        }
        await wrapper.setProps({
            recordData: updated_record,
            username: 'test_user'
        });
        expect(wrapper.find('iframe').exists()).toBe(true);
    });

    it("displays an iframe viewer for pdfs", async () => {
        let updated_record = cloneDeep(record);
        updated_record.viewerType = 'pdf';
        updated_record.briefObject.groupRoleMap = {
            everyone: 'canViewOriginals'
        }
        await wrapper.setProps({
            recordData: updated_record
        });
        expect(wrapper.find('iframe').exists()).toBe(true);
    });

    it("displays an iframe viewer for pdfs for logged in users", async () => {
        let updated_record = cloneDeep(record);
        updated_record.viewerType = 'pdf';
        updated_record.briefObject.groupRoleMap = {
            everyone: 'canViewMetadata',
            authenticated: 'canViewOriginals'
        }
        await wrapper.setProps({
            recordData: updated_record,
            username: 'test_user'
        });
        expect(wrapper.find('iframe').exists()).toBe(true);
    });

    it("uses the audio player component for audio files", async () => {
        let updated_record = cloneDeep(record);
        updated_record.briefObject.groupRoleMap = {
            everyone: 'canViewAccessCopies'
        }
        await wrapper.setProps({
            recordData: updated_record
        });
        expect(wrapper.findComponent({ name: 'audioPlayer' }).exists()).toBe(true);
    });

    it("uses the audio player component for audio files for logged in users", async () => {
        let updated_record = cloneDeep(record);
        updated_record.briefObject.groupRoleMap = {
            everyone: 'canViewMetadata',
            authenticated: 'canViewAccessCopies'
        }
        await wrapper.setProps({
            recordData: updated_record,
            username: 'test_user'
        });
        expect(wrapper.findComponent({ name: 'audioPlayer' }).exists()).toBe(true);
    });

    it("displays a list of files associated with the work", () => {
        expect(wrapper.findComponent({ name: 'fileList' }).exists()).toBe(true);
    });

    it("displays a list of neighbor works", () => {
        expect(wrapper.findComponent({ name: 'neighborList' }).exists()).toBe(true);
    });

    it("sets a link to its parent collection", () => {
        expect(wrapper.find('.parent-collection').attributes('href')).toEqual(`record/${record.briefObject.parentCollectionId}`);
    });

    it("loads record MODS metadata as html", (done) => {
        moxios.install();

        const html_data = "<table><tr><th>Title</th><td><p>Listen for real</p></td></tr></table>";
        const url = `record/${record.briefObject.id}/metadataView`;
        moxios.stubRequest(new RegExp(url), {
            status: 200,
            response: html_data
        });
        wrapper.vm.loadMetadata();

        moxios.wait(() => {
            expect(wrapper.vm.metadata).toEqual(html_data);
            expect(wrapper.find('#mods_data_display').text()).toEqual(expect.stringContaining('Listen for real'));
            done();
        });

        moxios.uninstall();
    });
});