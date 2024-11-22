import {shallowMount} from '@vue/test-utils'
import bulkDownload from '@/components/full_record/bulkDownload.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';

const work_id = 'e2f0d544-4f36-482c-b0ca-ba11f1251c01'
let wrapper;

describe('bulkDownload.vue', () => {
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
                totalDownloadSize: 8193,
                hasBulkDownloadAccess: true,
                workId: work_id,
                childCount: 5
            }
        });
    });

    it ("displays a bulk download button for downloads less than 1 GB", () => {
        let download_link = wrapper.find('.bulk-download-link');
        let download_email = wrapper.find('.bulk-download-email');

        expect(download_link.exists()).toBe(true);
        expect(download_email.exists()).toBe(false);
        expect(download_link.attributes('href')).toEqual(expect.stringContaining(`/services/api/bulkDownload/${work_id}`));
        expect(download_link.text()).toEqual(expect.stringContaining('Download All Files (8 KB)'));
    });

    it ("displays a contact wilson button for downloads greater than 1 GB", async () => {
        const TWO_GIGABYTE_FILE = 2147483648;
        await wrapper.setProps({
            totalDownloadSize: TWO_GIGABYTE_FILE
        });

        let download_link = wrapper.find('.bulk-download-link');
        let download_email = wrapper.find('.bulk-download-email');

        expect(download_link.exists()).toBe(false);
        expect(download_email.exists()).toBe(true);
        expect(download_email.attributes('href')).toEqual('https://library.unc.edu/contact-us/?destination=wilson');
        expect(download_email.text()).toEqual(expect.stringContaining('Contact Wilson Library for access'));
    });

    it ("does not display a button if the user doesn't have hasBulkDownloadAccess", async () => {
        await wrapper.setProps({
            hasBulkDownloadAccess: false
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

    it ("prompts user for confirmation if there are more than 100 files", async () => {
        // Mock window.confirm
        const confirmMock = jest.spyOn(window, 'confirm');

        // Mock return values for confirm
        confirmMock.mockImplementationOnce(() => false); // Simulate "No" click
        confirmMock.mockImplementationOnce(() => true);  // Simulate "Yes" click

        // Mock navigation
        const locationMock = jest.spyOn(window, 'location', 'get');
        const mockLocation = { href: '' };
        locationMock.mockReturnValue(mockLocation);

        await wrapper.setProps({
            childCount: 200
        });

        let download_link = wrapper.find('.bulk-download-link');
        let download_email = wrapper.find('.bulk-download-email');

        expect(download_link.exists()).toBe(true);
        expect(download_email.exists()).toBe(false);
        expect(download_link.attributes('href')).toEqual(expect.stringContaining(`/services/api/bulkDownload/${work_id}`));
        expect(download_link.text()).toEqual(expect.stringContaining('Download All Files (8 KB)'));

        // Trigger the click (simulating user action)
        await download_link.trigger('click');
        expect(window.confirm).toHaveBeenCalledWith(
            "Number of files exceeds the download limit, only the first 100 will be exported, do you want continue?"
        );

        // Verify that "No" prevents navigation
        expect(mockLocation.href).toBe('');

        // Simulate another click with "Yes"
        await download_link.trigger('click');
        expect(window.confirm).toBeCalledTimes(2);
        // Download should occur this time
        expect(mockLocation.href).toContain(`/services/api/bulkDownload/${work_id}`);

        // Cleanup mocks
        confirmMock.mockRestore();
        locationMock.mockRestore();
    });
});