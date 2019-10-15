import { createLocalVue, shallowMount } from '@vue/test-utils';
import patronRoles from '@/components/patronRoles.vue';
import moxios from "moxios";

const localVue = createLocalVue();
const STAFF_ONLY_ROLE_TEXT = '\u2014';

let response = {
    inherited: { roles: [{ principal: 'Public', role: 'canAccess' }], deleted: false },
    assigned: { roles: [{ principal: 'Public', role: 'canViewMetadata' }], deleted: false }
};
let response_defaults = {
    inherited: { roles: [{ principal: 'Public', role: 'canAccess' }], deleted: false, embargoed: false },
    assigned: { roles: [{ principal: 'Public', role: 'canViewMetadata' }], deleted: false, embargoed: false }
};

let empty_response = {
    inherited: { roles: [], deleted: false },
    assigned: { roles: [], deleted: false }
};

let empty_defaults = [
        { principal: 'Public', role: 'canAccess' },
        { principal: 'Onyen', role: 'canAccess' }
];

let same_roles = {
    inherited: { roles: [{ principal: 'Public', role: 'canViewMetadata' }, {principal: 'Onyen', role: 'canAccess'}], deleted: false },
    assigned: { roles: [{ principal: 'Public', role: 'canViewMetadata' }, { principal: 'Onyen', role: 'canViewMetadata'}], deleted: false }
};

let wrapper, selects;

describe('patronRoles.vue', () => {
    beforeEach(() => {
        moxios.install();

        wrapper = shallowMount(patronRoles, {
            localVue,
            propsData: {
                alertHandler: {
                    alertHandler: jest.fn() // This method lives outside of the Vue app
                },
                changesCheck: false,
                containerType: 'AdminUnit',
                uuid: '73bc003c-9603-4cd9-8a65-93a22520ef6a'
            }
        });

        global.confirm = jest.fn().mockReturnValue(true);
        selects = wrapper.findAll('select');
    });

    it("retrieves patron roles from the server and sets embargoed to 'false' if no embargo returned", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(response)
        });

        moxios.wait(() => {
            expect(wrapper.vm.display_roles).toEqual(response_defaults);
            expect(wrapper.vm.patron_roles).toEqual(response_defaults);
            done();
        });
    });

    it("sets default roles if no roles are returned", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(empty_response)
        });

        moxios.wait(() => {
            expect(wrapper.vm.display_roles.inherited.roles).toEqual([{ principal: 'Staff', role: STAFF_ONLY_ROLE_TEXT }]);
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([{ principal: 'Public', role: 'canAccess' }]);
            expect(wrapper.vm.patron_roles.inherited.roles).toEqual([]);
            expect(wrapper.vm.patron_roles.assigned.roles).toEqual(empty_defaults);
            expect(wrapper.vm.submit_roles).toEqual(empty_defaults);
            done();
        });
    });

    it("sets form values for assigned roles", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(response)
        });

        moxios.wait(() => {
            expect(wrapper.vm.patrons_role).toEqual('canViewMetadata');
            expect(wrapper.vm.onyen_role).toEqual('none');
            expect(wrapper.vm.user_type).toEqual('patron');
            done();
        });
    });

    it("disables select boxes if 'Staff only access' is checked", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(response)
        });

        moxios.wait(() => {
            expect(selects.at(0).attributes('disabled')).not.toBe('disabled');
            expect(selects.at(1).attributes('disabled')).not.toBe('disabled');

            let radio = wrapper.find('#staff');
            radio.element.selected = true;
            radio.trigger('change');

            expect(wrapper.vm.user_type).toEqual('staff');
            expect(selects.at(0).attributes('disabled')).toBe('disabled');
            expect(selects.at(1).attributes('disabled')).toBe('disabled');
            done();
        });
    });

    it("sets patron permissions to 'No Access' if 'Staff only access' is checked", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(response)
        });

        moxios.wait(() => {
            expect(wrapper.vm.patrons_role).toBe('canViewMetadata');
            expect(wrapper.vm.onyen_role).toBe('none');

            expect(wrapper.vm.display_roles.assigned.roles).toEqual(response.assigned.roles);
            expect(wrapper.vm.submit_roles).toEqual(response.assigned.roles);

            wrapper.find('#staff').trigger('click');

            expect(wrapper.vm.patrons_role).toBe('none');
            expect(wrapper.vm.onyen_role).toBe('none');

            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                {principal: 'Staff', role: STAFF_ONLY_ROLE_TEXT}
            ]);
            expect(wrapper.vm.submit_roles).toEqual([
                {principal: 'Public', role: 'none'},
                {principal: 'Onyen', role: 'none'}
            ]);
            done();
        });
    });

    it("updates permissions for public users", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(response)
        });

        moxios.wait(() => {
            expect(wrapper.vm.patrons_role).toBe('canViewMetadata');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(response.assigned.roles);
            expect(wrapper.vm.submit_roles).toEqual(response.assigned.roles);

            wrapper.findAll('#public option').at(1).setSelected();

            let updated_public_roles =  [
                { principal: 'Public', role: 'canDiscover' },
                { principal: 'Onyen', role: 'none' }
            ];

            expect(wrapper.vm.patrons_role).toBe('canDiscover');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(updated_public_roles);
            expect(wrapper.vm.submit_roles).toEqual(updated_public_roles);

            done();
        });
    });

    it("updates permissions for authenticated users", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(same_roles)
        });

        moxios.wait(() => {
            expect(wrapper.vm.onyen_role).toBe('canViewMetadata');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([{ principal: 'Public', role: 'canViewMetadata' }]);
            expect(wrapper.vm.submit_roles).toEqual(same_roles.assigned.roles);

            wrapper.findAll('#onyen option').at(1).setSelected();

            let updated_onyen_roles =  [
                { principal: 'Public', role: 'canViewMetadata' },
                { principal: 'Onyen', role: 'canDiscover' }
            ];

            expect(wrapper.vm.onyen_role).toBe('canDiscover');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(updated_onyen_roles);
            expect(wrapper.vm.submit_roles).toEqual(updated_onyen_roles);

            done();
        });
    });

    it("merges display permissions if public and public principals have the same permissions", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(same_roles)
        });

        moxios.wait(() => {
            expect(wrapper.vm.patrons_role).toBe('canViewMetadata');
            expect(wrapper.vm.onyen_role).toBe('canViewMetadata');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([{ principal: 'Public', role: 'canViewMetadata' }]);
            expect(wrapper.vm.patron_roles.assigned.roles).toEqual(same_roles.assigned.roles);
            expect(wrapper.vm.submit_roles).toEqual(same_roles.assigned.roles);
            done();
        });
    });

    it("disables 'submit' by default", (done) => {
        let btn = wrapper.find('#is-submitting');
        let is_disabled = expect.stringContaining('disabled');
        expect(btn.html()).toEqual(is_disabled);
        done();
    });

    it("enables 'submit' button if user/role has been added or changed", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(response)
        });

        moxios.wait(() => {
            let btn = wrapper.find('#is-submitting');
            let is_disabled = expect.stringContaining('disabled');

            wrapper.findAll('#onyen option').at(1).setSelected();
            expect(btn.html()).not.toEqual(is_disabled);
            done();
        });
    });

    afterEach(() => {
        moxios.uninstall();
    });
});