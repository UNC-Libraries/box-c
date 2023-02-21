import { shallowMount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import breadCrumbs from '@/components/full_record/breadCrumbs.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';

const objectPath = {
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
}

const longObjectPath = {
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
        },
        {
            pid: '353ee09f-a4ed-461e-a436-18a1bee77b23',
            name: 'testFolder',
            container: true
        },
        {
            pid: '353ee09f-a4ed-461e-a436-18a1bee77c91',
            name: 'testFolder2',
            container: true
        },
        {
            pid: '813ee09f-a4ed-461e-a436-18a1bee77b23',
            name: 'testFolder3',
            container: true
        },
        {
            pid: '813ee09f-a4ed-461e-a436-18a1bee77b23',
            name: 'testWork',
            container: false
        }
    ]
}

let wrapper, router, windowSpy;

describe('breadCrumbs.vue', () => {
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

        wrapper = shallowMount(breadCrumbs, {
            global: {
                plugins: [i18n, router]
            },
            props: {
                objectPath: objectPath
            }
        });
    });

    it('displays breadcrumbs', () => {
        expect(wrapper.find('#full_record_trail').text()).toEqual(expect.stringMatching(/Collection.*testAdminUnit/));
    });

    it('displays an ellipse after a breadcrumb', () => {
        expect(wrapper.find('.quote').text()).toEqual('»');
    });

    it('truncates long breadcrumbs', async () => {
        const regx = /Collections»testAdminUnit»testFolder».»testFolder3»testWork/
        await wrapper.setProps({ objectPath: longObjectPath });
        expect(wrapper.find('#full_record_trail').text()).toEqual(expect.stringMatching(regx));
    });

    it('expands long breadcrumbs on click', async () => {
        await wrapper.setProps({ objectPath: longObjectPath });
        await wrapper.find('#expand-breadcrumb').trigger('click');
        expect(wrapper.find('#full_record_trail').text()).toEqual('Collections»testAdminUnit»testFolder»testFolder2»testFolder3»testWork');
    });

    it('adds the current search state, if present', async () => {
        windowSpy = jest.spyOn(window, 'location', 'get');
        windowSpy.mockImplementation(() => ({
            search: '?anywhere='
        }));

        await wrapper.setProps({ objectPath: longObjectPath });
        const crumbs = wrapper.findAll('.quote + a');
        expect(crumbs[1].attributes('href')).toEqual(expect.stringContaining('?anywhere='));
        windowSpy.mockRestore();
    });

    it('does not add a current search state, if search state is absent', async () => {
        windowSpy = jest.spyOn(window, 'location', 'get');
        windowSpy.mockImplementation(() => ({
            search: ''
        }));

        await wrapper.setProps({ objectPath: longObjectPath });
        const crumbs = wrapper.findAll('.quote + a');
        expect(crumbs[1].attributes('href')).toEqual(expect.not.stringContaining('?anywhere='));
        windowSpy.mockRestore();
    });
});