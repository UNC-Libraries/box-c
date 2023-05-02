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
        subject: [
            "Test data",
            "Test2 data"
        ],
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
            "editResourceType",
            "destroyUnit",
            "bulkUpdateDescription",
            "changePatronAccess",
            "runEnhancements",
            "createAdminUnit",
            "ingest",
            "orderMembers",
            "viewOriginal",
            "viewAccessCopies",
            "viewHidden",
            "assignStaffRoles",
            "viewMetadata",
            "markForDeletion",
            "editDescription",
            "createCollection"
        ],
        groupRoleMap: {
            everyone: "canViewMetadata",
            authenticated: "canViewMetadata"
        },
        id: "353ee09f-a4ed-461e-a436-18a1bee77b01",
        updated: "2023-03-16T13:28:04.959Z",
        timestamp: 1678973288794
    },
    markedForDeletion: false,
    resourceType: "AdminUnit"
}

let wrapper, router;

describe('permissionUtils', () => {
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

    it("checks for groups", () => {
        expect(wrapper.vm.hasGroups(recordData)).toBe(true);
    });

    it("checks for groups when none are defined", () => {
        let updatedRecord = cloneDeep(recordData);
        updatedRecord.briefObject.groupRoleMap = undefined;
        expect(wrapper.vm.hasGroups(updatedRecord)).toBe(false);
    });

    it("checks for groups when the group role map is empty", () => {
        let updatedRecord = cloneDeep(recordData);
        updatedRecord.briefObject.groupRoleMap = {};
        expect(wrapper.vm.hasGroups(updatedRecord)).toBe(false);
    });

    it("checks for group roles", () => {
        expect(wrapper.vm.hasGroupRole(recordData, 'canViewMetadata')).toBe(true);
        expect(wrapper.vm.hasGroupRole(recordData, 'canViewOriginals')).toBe(false);

        let updatedRecord = cloneDeep(recordData);
        updatedRecord.briefObject.groupRoleMap = undefined;
        expect(wrapper.vm.hasGroupRole(updatedRecord, 'canViewMetadata')).toBe(false);
        expect(wrapper.vm.hasGroupRole(updatedRecord, 'canViewOriginals')).toBe(false);

    });

    it("checks for permissions", () => {
        expect(wrapper.vm.hasPermission(recordData, 'viewOriginal')).toBe(true);
        expect(wrapper.vm.hasPermission(recordData, 'destroy')).toBe(false);

        let updatedRecord = cloneDeep(recordData);
        updatedRecord.briefObject.permissions = undefined;
        expect(wrapper.vm.hasPermission(updatedRecord, 'viewOriginal')).toBe(false);
        expect(wrapper.vm.hasPermission(updatedRecord, 'destroy')).toBe(false);
    });
});