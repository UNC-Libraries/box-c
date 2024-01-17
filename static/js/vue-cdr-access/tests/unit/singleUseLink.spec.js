import { shallowMount } from '@vue/test-utils'
import singleUseLink from '@/components/full_record/singleUseLink.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';
import moxios from 'moxios';

const uuid = '9f7f3746-0237-4261-96a2-4b4765d4ae03';
const response_date = { link: `https://test.edu`, key: `12345`, expires: '24hrs', id: uuid };
let wrapper;

describe('singleUseLink.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        wrapper = shallowMount(singleUseLink, {
            global: {
                plugins: [i18n]
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
                .toEqual(`Created link ${response_date.key} expires in ${response_date.expires}`);
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
});
