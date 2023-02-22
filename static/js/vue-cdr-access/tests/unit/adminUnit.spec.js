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
                plugins: [i18n, router]
            },
            props: {
                recordData: recordData
            }
        });
    });

    it('displays breadcrumbs', () => {
        expect(wrapper.find('#full_record_trail').text()).toEqual(expect.stringMatching(/Collection.*testAdminUnit/));
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