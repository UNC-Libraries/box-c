import { mount } from '@vue/test-utils';
import dcrHeader from '@/components/dcrHeader.vue';

describe('dcrHeader.vue', () => {
    it("loads the dcrHeader", () => {
        document.body.innerHTML = `<div id="pagewrap" data-username="testUser" data-admin="true">`;
        const wrapper = mount(dcrHeader);
        expect(wrapper.find('header').exists()).toBe(true);
        expect(wrapper.html()).toContain('header');
    });

    it("loads headerHome on the home page", () => {
        document.body.innerHTML = `<div id="pagewrap" data-username="testUser" data-admin="true">`;
        const wrapper = mount(dcrHeader, {
            props: {
                isHomepage: true
            }
        });
        expect(wrapper.html()).toContain('logo-row');
    });

    it("loads headerSmall on the non-home pages", () => {
        document.body.innerHTML = `<div id="pagewrap" data-username="testUser" data-admin="true">`;
        const wrapper = mount(dcrHeader, {
            props: {
                isHomepage: false
            }
        });
        expect(wrapper.html()).toContain('logo-row-small');
    });

    it("login button present when not logged in", () => {
        document.body.innerHTML = `<div id="pagewrap" data-username="" data-admin="false">`;
        const current_page = window.location;
        const loginUrl = `https://${current_page.host}/Shibboleth.sso/Login?target=${encodeURIComponent(current_page)}`;
        const wrapper = mount(dcrHeader);
        expect(wrapper.html()).toContain(loginUrl);
    });

    it("logout button present when logged in", () => {
        document.body.innerHTML = `<div id="pagewrap" data-username="testUser" data-admin="true">`;
        const current_page = window.location;
        const logoutUrl = `https://${current_page.host}/Shibboleth.sso/Logout?return=https://sso.unc.edu/idp/logout.jsp?return_url=${encodeURIComponent(current_page)}`;
        const wrapper = mount(dcrHeader);
        expect(wrapper.html()).toContain(logoutUrl);
    });

    it("admin link present when user logged in as admin", () => {
        document.body.innerHTML = `<div id="pagewrap" data-username="testUser" data-admin="true">`;
        const current_page = window.location;
        const adminUrl = `https://${window.location.host}/admin/`;
        const wrapper = mount(dcrHeader);
        expect(wrapper.html()).toContain(adminUrl);
    });

});