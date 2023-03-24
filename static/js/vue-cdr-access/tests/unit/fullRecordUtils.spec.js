import { mount } from '@vue/test-utils'
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
                plugins: [i18n, router]
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

    it('shows full text for short abstracts', async () => {
        expect(wrapper.vm.truncateAbstract).toBe(false);
        let shortAbstract = cloneDeep(recordData);
        shortAbstract.briefObject.abstractText = 'short abstract';
        await wrapper.setProps({
            recordData: shortAbstract
        });
        expect(wrapper.vm.truncateAbstract).toBe(false);
        expect(wrapper.find('.abstract-text').exists()).toBe(false);
        expect(wrapper.find('.abstract').text()).toEqual(shortAbstract.briefObject.abstractText);
    });

    it('truncates long abstracts', async () => {
        expect(wrapper.vm.truncateAbstract).toBe(false);
        let longAbstract = cloneDeep(recordData);
        longAbstract.briefObject.abstractText = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit,' +
            ' sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Id cursus metus aliquam eleifend' +
            ' mi in nulla. In hendrerit gravida rutrum quisque non. Libero volutpat sed cras ornare arcu dui. Diam' +
            ' maecenas sed enim ut sem viverra aliquet eget. Malesuada fames ac turpis egestas sed tempus. In' +
            ' egestas erat imperdiet sed euismod nisi. Ut pharetra sit amet aliquam id diam maecenas ultricies.' +
            ' Eget nullam non nisi est. Risus viverra adipiscing at in tellus integer feugiat scelerisque varius.' +
            ' Sit amet risus nullam eget felis eget nunc lobortis. Dui vivamus arcu felis bibendum ut tristique' +
            ' et egestas quis. Eget nunc scelerisque viverra mauris in aliquam sem. Facilisi nullam vehicula' +
            ' ipsum a. Odio facilisis mauris sit amet massa vitae tortor. Donec et odio pellentesque diam.' +
            ' Commodo quis imperdiet massa tincidunt. Sagittis eu volutpat odio facilisis mauris sit amet massa.' +
            'Nunc aliquet bibendum enim facilisis gravida neque convallis. Arcu non sodales neque sodales' +
            ' ut etiam. Posuere lorem ipsum dolor sit amet consectetur. Non pulvinar neque laoreet suspendisse' +
            ' interdum consectetur libero id faucibus. Netus et malesuada fames ac turpis. Sit amet mauris' +
            ' commodo quis imperdiet massa tincidunt. Venenatis urna cursus eget nunc. Aliquet lectus proin' +
            ' nibh nisl condimentum id. Pellentesque habitant morbi tristique senectus et netus et malesuada.' +
            ' Pulvinar elementum integer enim neque volutpat ac tincidunt. Sed viverra tellus in hac habitasse' +
            ' platea. Dui id ornare arcu odio ut sem. Tincidunt arcu non sodales neque sodales ut etiam sit.' +
            ' Quisque non tellus orci ac auctor augue. Vitae aliquet nec ullamcorper sit amet risus. Tempor' +
            ' nec feugiat nisl pretium. Posuere lorem ipsum dolor sit amet consectetur adipiscing elit.' +
            ' Pellentesque eu tincidunt tortor aliquam.';

        await wrapper.setProps({
            recordData: longAbstract
        });

        let show_more = wrapper.find('.abstract-text');
        expect(wrapper.vm.truncateAbstract).toBe(true);
        expect(show_more.exists()).toBe(true);
        expect(wrapper.vm.abstractLinkText).toEqual('Read more');

        // Show full abstract
        await show_more.trigger('click');
        expect(wrapper.find('.abstract').text()).toEqual(longAbstract.briefObject.abstractText + '... Read less');

        // Close abstract
        await show_more.trigger('click');
        expect(wrapper.find('.abstract').text()).toEqual(longAbstract.briefObject.abstractText.substring(0, 350) + '... Read more');
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
        expect(wrapper.vm.hasGroupRole('canViewOriginals', 'authenticated')).toBe(false);

        let canViewMetadata = cloneDeep(recordData);
        canViewMetadata.briefObject.groupRoleMap = {
            authenticated: 'canViewMetadata',
            everyone: 'canViewMetadata'
        };
        await wrapper.setProps({
            recordData: canViewMetadata
        });
        expect(wrapper.vm.hasGroupRole('canViewOriginals', 'authenticated')).toBe(false);

        let canViewOriginals = cloneDeep(recordData);
        canViewOriginals.briefObject.groupRoleMap = {
            authenticated: 'canViewOriginals',
            everyone: 'canViewMetadata'
        };
        await wrapper.setProps({
            recordData: canViewOriginals
        });
        expect(wrapper.vm.hasGroupRole('canViewOriginals', 'authenticated')).toBe(true);
    });

    it('determines whether a user is logged in', async () => {
        expect(wrapper.vm.isLoggedIn).toEqual(false);
        await wrapper.setProps({ username: 'test_user' });
        expect(wrapper.vm.isLoggedIn).toEqual(true);
    });

    it('formats string dates', () => {
        expect(wrapper.vm.formatDate(recordData.briefObject.added)).toEqual('2023-01-17');
    });

    it('formats timestamps to dates', () => {
        expect(wrapper.vm.formatDate(recordData.briefObject.created)).toEqual('1999-01-22');
    });
});