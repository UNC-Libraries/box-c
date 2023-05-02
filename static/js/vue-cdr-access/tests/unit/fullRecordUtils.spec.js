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

describe('fullrecordUtils', () => {
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

    it('displays full metadata on click', async () => {
        expect(wrapper.vm.showMetadata).toBe(false);
        await wrapper.find('.metadata-link').trigger('click');
        expect(wrapper.vm.showMetadata).toBe(true);
    });

    it('returns a class name for deleted records', async () => {
        expect(wrapper.vm.isDeleted).toEqual('');

        let addDeletion = cloneDeep(recordData);
        addDeletion.markedForDeletion = true;
        await wrapper.setProps({
            recordData: addDeletion
        });
        expect(wrapper.vm.isDeleted).toEqual('deleted');
    });

    it('sets display text for child count', async () => {
        expect(wrapper.vm.displayChildCount).toEqual('5 items');

        let updatedChildren = cloneDeep(recordData);
        updatedChildren.briefObject.counts.child = 1;
        await wrapper.setProps({
            recordData: updatedChildren
        });
        expect(wrapper.vm.displayChildCount).toEqual('1 item');
    });

    it('checks for restricted content', async () => {
        expect(wrapper.vm.restrictedContent).toBe(true);

        let updatePerms = cloneDeep(recordData);
        updatePerms.briefObject.groupRoleMap = {};
        await wrapper.setProps({
            recordData: updatePerms
        });
        expect(wrapper.vm.restrictedContent).toBe(false);

        updatePerms.briefObject.roleGroup = {
            authenticated: 'canViewOriginals',
            everyone: 'canViewOriginals',
        };
        await wrapper.setProps({
            recordData: updatePerms
        });
        expect(wrapper.vm.restrictedContent).toBe(false);
    })

    it('allows full access for authenticated user', async () => {
        expect(wrapper.vm.hasGroupRole(recordData, 'canViewOriginals', 'authenticated')).toBe(false);

        let canViewMetadata = cloneDeep(recordData);
        canViewMetadata.briefObject.groupRoleMap = {
            authenticated: 'canViewMetadata',
            everyone: 'canViewMetadata'
        };
        await wrapper.setProps({
            recordData: canViewMetadata
        });
        expect(wrapper.vm.hasGroupRole(canViewMetadata, 'canViewOriginals', 'authenticated')).toBe(false);

        let canViewOriginals = cloneDeep(recordData);
        canViewOriginals.briefObject.groupRoleMap = {
            authenticated: 'canViewOriginals',
            everyone: 'canViewMetadata'
        };
        await wrapper.setProps({
            recordData: canViewOriginals
        });
        expect(wrapper.vm.hasGroupRole(canViewOriginals, 'canViewOriginals', 'authenticated')).toBe(true);
    });

    it('formats string dates', () => {
        expect(wrapper.vm.formatDate(recordData.briefObject.added)).toEqual('2023-01-17');
    });

    it('formats timestamps to dates', () => {
        expect(wrapper.vm.formatDate(recordData.briefObject.created)).toEqual('1999-01-22');
    });
});