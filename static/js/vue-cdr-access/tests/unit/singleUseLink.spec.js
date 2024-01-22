import { shallowMount } from '@vue/test-utils'
import singleUseLink from '@/components/full_record/singleUseLink.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from 'vue-i18n';
import  { createRouter, createWebHistory } from 'vue-router';
import translations from '@/translations';
import moxios from 'moxios';

const uuid = '9f7f3746-0237-4261-96a2-4b4765d4ae03';
const oneDay = 86400000;
const response_date = { key: `12345`, expires: Date.now() + oneDay, id: uuid };
let wrapper, router;

describe('singleUseLink.vue', () => {
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
        wrapper = shallowMount(singleUseLink, {
            global: {
                plugins: [i18n, router]
            },
            props: {
                uuid: '9f7f3746-0237-4261-96a2-4b4765d4ae03'
            }
        });

        moxios.install();
    });

    afterEach(function () {
        moxios.uninstall();
    });

    it("creates single use links", (done) => {
        expect(wrapper.find('.download-link-wrapper').exists()).toBe(false);

        moxios.stubRequest(`/services/api/single_use_link/create/${uuid}`, {
            status: 200,
            response: JSON.stringify(response_date)
        });

        moxios.wait(async () => {
            await wrapper.find('#single-use-link').trigger('click');
            expect(wrapper.find('.download-link-wrapper').exists()).toBe(true);
            expect(wrapper.find('.download-link-wrapper div').text())
                .toEqual(`Created link ${response_date.key} expires in 1 day`);
            expect(wrapper.find('.download-link-wrapper a').exists()).toBe(true); // Copy button
            done();
        });
    });

    it("does not create single use links on response errors", (done) => {
        expect(wrapper.find('.download-link-wrapper').exists()).toBe(false);

        moxios.stubRequest(`/services/api/single_use_link/create/${uuid}`, {
            status: 404,
            response: JSON.stringify('No record here')
        });

        moxios.wait(async () => {
            await wrapper.find('#single-use-link').trigger('click');
            expect(wrapper.find('.download-link-wrapper').exists()).toBe(false);
            done();
        });
    });

    it("copies single use links", async () => {
        Object.assign(window.navigator, {
            clipboard: {
                writeText: jest.fn().mockImplementation(() => Promise.resolve()),
            },
        });

        await wrapper.setData({ single_use_links: [response_date] });
        await wrapper.find('.download-link-wrapper a').trigger('click');
        expect(window.navigator.clipboard.writeText)
            .toHaveBeenCalledWith(response_date.link);
    });

    it("clears single use links if the route changes", async () => {
        await wrapper.setData({
            single_use_links: [{
                link: 'https://localhost/services/api/single_use_link/2a8b7520c2634be78168caf3ab67b52c202bf89c7d8',
                accessCode: '2a8b7520',
                expires: '1 day'
            }]
        });
        expect(wrapper.vm.single_use_links.length).toEqual(1);

        await router.push('/record/1234');
        expect(wrapper.vm.single_use_links.length).toEqual(0);
    });
});
