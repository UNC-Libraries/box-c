import {shallowMount} from '@vue/test-utils'
import bulkDownload from '@/components/full_record/bulkDownload.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';

const work_id = 'e2f0d544-4f36-482c-b0ca-ba11f1251c01'
let wrapper;

describe('fileList.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        wrapper = shallowMount(bulkDownload, {
            global: {
                plugins: [i18n],
            },
            props: {
                totalDownloadSize: "2 MB",
                viewOriginalAccess: true,
                workId: work_id,
            }
        });
    });

    it ("displays a bulk download button for downloads less than 1 GB", () => {
        let download_link = wrapper.find('.bulk-download-link');
        let download_email = wrapper.find('.bulk-download-email');

        expect(download_link.exists()).toBe(true);
        expect(download_email.exists()).toBe(false);
        expect(download_link.attributes('href')).toEqual(expect.stringContaining(`/services/api/bulkDownload/${work_id}`));
        expect(download_link.text()).toEqual(expect.stringContaining('Download All Files (2 MB)'));
    });

    it ("displays a contact wilson button for downloads greater than 1 GB", async () => {
        await wrapper.setProps({
            totalDownloadSize: "-1"
        });

        let download_link = wrapper.find('.bulk-download-link');
        let download_email = wrapper.find('.bulk-download-email');

        expect(download_link.exists()).toBe(false);
        expect(download_email.exists()).toBe(true);
        expect(download_email.attributes('href')).toEqual('https://library.unc.edu/contact-us/?destination=wilson');
        expect(download_email.text()).toEqual(expect.stringContaining('Contact Wilson Library for access'));
    });

    it ("does not display a button if the user doesn't have viewOriginalAccess", async () => {
        await wrapper.setProps({
            viewOriginalAccess: false
        });

        let download_link = wrapper.find('.actionlink');
        expect(download_link.exists()).toBe(false);
    });

    it ("does not display a button if the download size is not set", async () => {
        await wrapper.setProps({
            totalDownloadSize: null
        });

        let download_link = wrapper.find('.actionlink');
        expect(download_link.exists()).toBe(false);
    });
});