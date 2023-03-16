import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import adminUnit from '@/components/full_record/adminUnit.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';
import cloneDeep from 'lodash.clonedeep';

const recordData = {
    pageSubtitle: 'testAdminUnit',
    briefObject: {
        pid: {
            id: '353ee09f-a4ed-461e-a436-18a1bee77b01',
            qualifier: 'content',
            qualifiedId: 'content/353ee09f-a4ed-461e-a436-18a1bee77b01',
            componentId: '353ee09f-a4ed-461e-a436-18a1bee77b01',
            repositoryUri: 'http://localhost:8181/fcrepo/rest/content/35/3e/e0/9f/353ee09f-a4ed-461e-a436-18a1bee77b01',
            repositoryPath: 'http://localhost:8181/fcrepo/rest/content/35/3e/e0/9f/353ee09f-a4ed-461e-a436-18a1bee77b01',
            pid: 'uuid:353ee09f-a4ed-461e-a436-18a1bee77b01',
            uri: 'http://localhost:8181/fcrepo/rest/content/35/3e/e0/9f/353ee09f-a4ed-461e-a436-18a1bee77b01',
            uuid: '353ee09f-a4ed-461e-a436-18a1bee77b01'
        },
        fields: {
            adminGroup: [
                'admin_access'
            ],
            filesizeTotal: 6475,
            readGroup: [
                'everyone'
            ],
            abstract: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Nec nam aliquam sem et tortor consequat id. Ornare lectus sit amet est placerat. Imperdiet sed euismod nisi porta lorem mollis aliquam ut porttitor. Ac turpis egestas maecenas pharetra convallis posuere. Mattis nunc sed blandit libero volutpat. Scelerisque viverra mauris in aliquam sem fringilla ut. Ac turpis egestas sed tempus. Id cursus metus aliquam eleifend mi in. Sit amet purus gravida quis blandit turpis cursus in. Ullamcorper a lacus vestibulum sed arcu non odio. Scelerisque purus semper eget duis at tellus. In egestas erat imperdiet sed euismod nisi porta lorem. Massa sed elementum tempus egestas sed sed risus pretium quam. Donec massa sapien faucibus et molestie ac. Ac orci phasellus egestas tellus rutrum tellus pellentesque eu tincidunt. Tortor posuere ac ut consequat semper viverra nam. Nec sagittis aliquam malesuada bibendum.\n\nMagna fringilla urna porttitor rhoncus dolor purus non. Sit amet commodo nulla facilisi nullam vehicula ipsum a. Gravida in fermentum et sollicitudin ac orci phasellus. Ac placerat vestibulum lectus mauris ultrices eros in. Posuere urna nec tincidunt praesent semper feugiat nibh sed. Arcu odio ut sem nulla pharetra diam sit. Neque viverra justo nec ultrices dui sapien eget. Diam maecenas ultricies mi eget mauris pharetra et. Pulvinar pellentesque habitant morbi tristique senectus et. Nec feugiat nisl pretium fusce id velit ut tortor. Leo a diam sollicitudin tempor id eu nisl. Semper viverra nam libero justo laoreet sit amet cursus sit. Sem viverra aliquet eget sit amet tellus. Nullam eget felis eget nunc. Duis ultricies lacus sed turpis tincidunt. Elementum facilisis leo vel fringilla est ullamcorper eget nulla facilisi. Enim blandit volutpat maecenas volutpat. Nibh mauris cursus mattis molestie a iaculis.\n\nPellentesque diam volutpat commodo sed. Egestas sed tempus urna et pharetra pharetra massa. A condimentum vitae sapien pellentesque habitant. Suspendisse interdum consectetur libero id. Ut enim blandit volutpat maecenas. Proin nibh nisl condimentum id. Accumsan sit amet nulla facilisi morbi tempus iaculis urna id. Tincidunt lobortis feugiat vivamus at. Cursus eget nunc scelerisque viverra. Sed sed risus pretium quam vulputate dignissim. Ipsum suspendisse ultrices gravida dictum fusce. Diam phasellus vestibulum lorem sed risus ultricies tristique nulla. Id velit ut tortor pretium viverra suspendisse potenti nullam. Fames ac turpis egestas sed tempus urna et pharetra. Sit amet dictum sit amet justo donec enim diam vulputate. Morbi leo urna molestie at. Habitasse platea dictumst vestibulum rhoncus.\n\nHendrerit gravida rutrum quisque non tellus. Morbi tristique senectus et netus et malesuada fames. Egestas tellus rutrum tellus pellentesque. Amet nulla facilisi morbi tempus. Consequat ac felis donec et odio. Vitae aliquet nec ullamcorper sit amet. Vulputate sapien nec sagittis aliquam. Aliquam purus sit amet luctus venenatis lectus. Commodo nulla facilisi nullam vehicula ipsum a arcu cursus vitae. Nullam ac tortor vitae purus. Fermentum posuere urna nec tincidunt praesent semper. Purus viverra accumsan in nisl nisi scelerisque. Faucibus a pellentesque sit amet porttitor. Nisi est sit amet facilisis magna etiam tempor orci. Dignissim suspendisse in est ante in. Vestibulum morbi blandit cursus risus at. Ultrices dui sapien eget mi proin sed libero.',
            title: 'testAdminUnit',
            dateAdded: 1673963529616,
            rollup: '353ee09f-a4ed-461e-a436-18a1bee77b01',
            contentStatus: [
                'Described'
            ],
            dateUpdated: 1673964408838,
            datastream: [
                'event_log|application/n-triples|event_log.nt|nt|1431|urn:sha1:be44fc23ba7da95ba3ad67121144efc8224448ad||',
                'md_descriptive_history|text/xml|||1308|urn:sha1:5e7361d5029aefbae7216c8d5cf5fd1a29a8feca||',
                'md_descriptive|text/xml|md_descriptive.xml|xml|3736|urn:sha1:ca835bc6b3d6bd7dc445336a94d862c6be6fdc28||'
            ],
            ancestorPath: [
                '1,collections'
            ],
            dateCreated: 917049600000,
            _version_: 1757904316712091600,
            ancestorIds: '/collections/353ee09f-a4ed-461e-a436-18a1bee77b01',
            lastIndexed: 1673963540129,
            id: '353ee09f-a4ed-461e-a436-18a1bee77b01',
            keyword: [
                '353ee09f-a4ed-461e-a436-18a1bee77b01'
            ],
            roleGroup: [
                ''
            ],
            timestamp: 1676468197202,
            resourceType: 'AdminUnit'
        },
        objectPath: {
            entries: [
                {
                    pid: 'collections',
                    name: 'Content Collections Root',
                    container: true
                },
                {
                    pid: '353ee09f-a4ed-461e-a436-18a1bee77b01',
                    name: 'testAdminUnit',
                    container: true
                }
            ]
        },
        groupRoleMap: {},
        countMap: {
            child: 2
        },
        idWithoutPrefix: '353ee09f-a4ed-461e-a436-18a1bee77b01',
        ancestorPath: [
            '1,collections'
        ],
        datastream: [
            'event_log|application/n-triples|event_log.nt|nt|1431|urn:sha1:be44fc23ba7da95ba3ad67121144efc8224448ad||',
            'md_descriptive_history|text/xml|||1308|urn:sha1:5e7361d5029aefbae7216c8d5cf5fd1a29a8feca||',
            'md_descriptive|text/xml|md_descriptive.xml|xml|3736|urn:sha1:ca835bc6b3d6bd7dc445336a94d862c6be6fdc28||'
        ],
        roleGroup: [
            ''
        ],
        id: '353ee09f-a4ed-461e-a436-18a1bee77b01',
        keyword: [
            '353ee09f-a4ed-461e-a436-18a1bee77b01'
        ],
        timestamp: 1676468197202,
        title: 'testAdminUnit',
        abstractText: 'Lorem ipsum dolor sit amet',
        resourceType: 'AdminUnit',
        filesizeTotal: 6475,
        readGroup: [
            'everyone'
        ],
        adminGroup: [
            'admin_access'
        ],
        contentStatus: [
            'Described'
        ],
        dateCreated: 917049600000,
        dateAdded: 1673963529616,
        dateUpdated: 1673964408838
    },
    markedForDeletion: false,
    resourceType: 'AdminUnit'
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

    it('shows full text for short abstracts', () => {
        expect(wrapper.vm.truncateAbstract).toBe(false);
        expect(wrapper.find('.abstract-text').exists()).toBe(false);
        expect(wrapper.find('.abstract').text()).toEqual(recordData.briefObject.abstractText);
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
        expect(wrapper.vm.displayChildCount).toEqual('2 items');

        let updatedChildren = cloneDeep(recordData);
        updatedChildren.briefObject.countMap.child = 1;
        await wrapper.setProps({
            recordData: updatedChildren
        });
        expect(wrapper.vm.displayChildCount).toEqual('1 item');
    });

    it('checks for restricted content', async () => {
        expect(wrapper.vm.restrictedContent).toBe(true);

        let updatePerms = cloneDeep(recordData);
        updatePerms.briefObject.roleGroup = undefined;
        await wrapper.setProps({
            recordData: updatePerms
        });
        expect(wrapper.vm.restrictedContent).toBe(false);

        updatePerms.briefObject.roleGroup = ['canViewOriginals|everyone'];
        await wrapper.setProps({
            recordData: updatePerms
        });
        expect(wrapper.vm.restrictedContent).toBe(false);
    })

    it('allows full access for authenticated user', async () => {
        // No access rights set
        expect(wrapper.vm.hasAccess('canViewOriginals')).toBe(false);

        let canViewMetadata = cloneDeep(recordData);
        canViewMetadata.briefObject.groupRoleMap = {
            authenticated: 'canViewMetadata',
            everyone: 'canViewMetadata'
        };
        await wrapper.setProps({
            recordData: canViewMetadata
        });
        expect(wrapper.vm.hasAccess('canViewOriginals')).toBe(false);

        let canViewOriginals = cloneDeep(recordData);
        canViewOriginals.briefObject.groupRoleMap = {
            authenticated: 'canViewOriginals',
            everyone: 'canViewMetadata'
        };
        await wrapper.setProps({
            recordData: canViewOriginals
        });
        expect(wrapper.vm.hasAccess('canViewOriginals')).toBe(true);
    });

    it('determines whether a user is logged in', async () => {
        expect(wrapper.vm.isLoggedIn).toEqual(false);
        await wrapper.setProps({ onyen: 'test_user' });
        expect(wrapper.vm.isLoggedIn).toEqual(true);
    });
});