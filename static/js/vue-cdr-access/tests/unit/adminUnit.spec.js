import {mount, RouterLinkStub} from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import adminUnit from '@/components/full_record/adminUnit.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';
import cloneDeep from 'lodash.clonedeep';

const recordData = {
    briefObject: {
        added: "2023-01-17T13:52:09.616Z",
        counts: {
            child: 5
        },
        created: 917049600000,
        title: "testAdminUnit",
        type: "AdminUnit",
        contentStatus: [
            "Described"
        ],
        rollup: "353ee09f-a4ed-461e-a436-18a1bee77b01",
        objectPath: [
            {
                pid: "collections",
                name: "Content Collections Root",
                container: true
            },
            {
                pid: "353ee09f-a4ed-461e-a436-18a1bee77b01",
                name: "testAdminUnit",
                container: true
            }
        ],
        datastream: [
            "event_log|application/n-triples|event_log.nt|nt|1431|urn:sha1:be44fc23ba7da95ba3ad67121144efc8224448ad||",
            "md_descriptive_history|text/xml|||5209|urn:sha1:6082b819133f6be40326bcfe5fb73f4c3cc35da6||",
            "md_descriptive|text/xml|md_descriptive.xml|xml|3882|urn:sha1:e4c1581e4f978e74382c904a706a34e5942918fa||"
        ],
        ancestorPath: [
            {
                id: "collections",
                title: "collections"
            }
        ],
        permissions: [
            "markForDeletionUnit",
            "move",
            "reindex",
            "destroy",
            "editResourceType",
            "destroyUnit",
            "bulkUpdateDescription",
            "changePatronAccess",
            "runEnhancements",
            "createAdminUnit",
            "ingest",
            "orderMembers",
            "viewOriginal",
            "viewReducedResImages",
            "viewAccessCopies",
            "viewHidden",
            "assignStaffRoles",
            "viewMetadata",
            "markForDeletion",
            "editDescription",
            "createCollection"
        ],
        groupRoleMap: {},
        id: "353ee09f-a4ed-461e-a436-18a1bee77b01",
        updated: "2023-03-16T13:28:04.959Z"
    },
    markedForDeletion: false,
    resourceType: "AdminUnit"
};

let wrapper, router;

describe('adminUnit.vue', () => {
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

        wrapper = mount(adminUnit, {
            global: {
                plugins: [i18n, router],
                stubs: {
                    RouterLink: RouterLinkStub
                }
            },
            props: {
                recordData: recordData
            }
        });
    });

    it('displays an admin unit header', () => {
        expect(wrapper.find('h2').text()).toBe('testAdminUnit');
    });

    it('displays subjects', async () => {
        let updatedRecordData = cloneDeep(recordData);
        updatedRecordData.briefObject.subject = ['test', 'test2'];
        await wrapper.setProps({ recordData: updatedRecordData });
        expect(wrapper.find('p').text()).toEqual(expect.stringContaining('test, test2'));
    });

    it('displays a message if there are no subjects', () => {
        expect(wrapper.find('p').text()).toEqual(expect.stringContaining('There are no subjects listed for this record'));
    });
});