import { mount } from '@vue/test-utils';
import dcrHeader from '@/components/dcrHeader.vue';

describe('dcrHeader.vue', () => {
    it("jumpToAdminUrl is record-specific admin url", () => {
        document.body.innerHTML = `<div id="pagewrap" data-username="testUser" data-admin="true">`;
        const current_page = window.location;
        const testUrl = `https://${current_page.hostname}/record/73bc003c-9603-4cd9-8a65-93a22520ef6a`;
        const adminUrl = `https://${current_page.hostname}/admin/list/73bc003c-9603-4cd9-8a65-93a22520ef6a`;
        window.location = Object.assign(new URL(testUrl));
        const wrapper = mount(dcrHeader, {
            props: {
                isHomepage: false,
                adminAccess: true
            }
        });
        expect(wrapper.html()).toContain(adminUrl);
    });

    it("jumpToAdminUrl is admin url", () => {
        document.body.innerHTML = `<div id="pagewrap" data-username="testUser" data-admin="true">`;
        const current_page = window.location;
        const adminUrl = `https://${window.location.host}/admin/`;
        const wrapper = mount(dcrHeader);
        expect(wrapper.html()).toContain(adminUrl);
    });

});