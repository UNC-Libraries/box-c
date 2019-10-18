import { createLocalVue, shallowMount } from '@vue/test-utils';
import patronRoles from '@/components/patronRoles.vue';
import moxios from 'moxios';

const localVue = createLocalVue();
const STAFF_ONLY_ROLE_TEXT = '\u2014';

let response = {
    inherited: { roles: [{ principal: 'everyone', role: 'canAccess' }], deleted: false, embargo: null },
    assigned: { roles: [{ principal: 'everyone', role: 'canViewMetadata' }], deleted: false, embargo: null }
};

let empty_response = {
    inherited: { roles: [], deleted: false, embargo: null },
    assigned: { roles: [], deleted: false, embargo: null }
};

let empty_defaults = [
        { principal: 'everyone', role: 'canAccess' },
        { principal: 'authenticated', role: 'canAccess' }
];

let same_roles = {
    inherited: { roles: [{ principal: 'everyone', role: 'canViewMetadata' }, {principal: 'authenticated', role: 'canAccess'}],
        deleted: false, embargo: null  },
    assigned: { roles: [{ principal: 'everyone', role: 'canViewMetadata' }, { principal: 'authenticated', role: 'canViewMetadata'}],
        deleted: false, embargo: null  }
};

const setRoles = jest.fn();
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
                containerType: 'Collection',
                uuid: '73bc003c-9603-4cd9-8a65-93a22520ef6a'
            },
            methods: {setRoles}
        });

        global.confirm = jest.fn().mockReturnValue(true);
        selects = wrapper.findAll('select');
    });

    it("submits updated roles to the server", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            wrapper.setData({
                submit_roles: {
                    embargo: '2099-01-01'
                },
                unsaved_changes: true
            });
            wrapper.find('#is-submitting').trigger('click');
            expect(setRoles).toHaveBeenCalled();
            done();
        });
    });

    it("retrieves patron roles from the server", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            expect(wrapper.vm.display_roles).toEqual(response);
            expect(wrapper.vm.patron_roles).toEqual(response);
            expect(wrapper.vm.submit_roles).toEqual(response.assigned);
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
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([{ principal: 'everyone', role: 'canAccess' }]);
          //  expect(wrapper.vm.patron_roles.inherited.roles).toEqual(empty_defaults);
          //  expect(wrapper.vm.patron_roles.assigned.roles).toEqual(empty_defaults);
            done();
        });
    });

    it("sets form values for assigned roles", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            expect(wrapper.vm.everyone_role).toEqual('canViewMetadata');
            expect(wrapper.vm.authenticated_role).toEqual('none');
            expect(wrapper.vm.user_type).toEqual('patron');
            done();
        });
    });

    it("disables select boxes if 'Staff only access' is checked", (done) => {
        stubDataLoad();

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
        stubDataLoad();

        moxios.wait(() => {
            expect(wrapper.vm.everyone_role).toBe('canViewMetadata');
            expect(wrapper.vm.authenticated_role).toBe('none');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(response.assigned.roles);

            wrapper.find('#staff').trigger('click');

            expect(wrapper.vm.everyone_role).toBe('none');
            expect(wrapper.vm.authenticated_role).toBe('none');

            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                {principal: 'Staff', role: STAFF_ONLY_ROLE_TEXT}
            ]);
            expect(wrapper.vm.submit_roles.roles).toEqual([
                {principal: 'everyone', role: 'none'},
                {principal: 'authenticated', role: 'none'}
            ]);
            done();
        });
    });

    it("updates permissions for public users", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            expect(wrapper.vm.everyone_role).toBe('canViewMetadata');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(response.assigned.roles);

            wrapper.findAll('#public option').at(1).setSelected();

            let updated_public_roles =  [
                { principal: 'everyone', role: 'canDiscover' },
                { principal: 'authenticated', role: 'none' }
            ];

            expect(wrapper.vm.everyone_role).toBe('canDiscover');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(updated_public_roles);
            expect(wrapper.vm.submit_roles.roles).toEqual(updated_public_roles);

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
            expect(wrapper.vm.authenticated_role).toBe('canViewMetadata');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([{ principal: 'everyone', role: 'canViewMetadata' }]);

            wrapper.findAll('#authenticated option').at(1).setSelected();

            let updated_authenticated_roles =  [
                { principal: 'everyone', role: 'canViewMetadata' },
                { principal: 'authenticated', role: 'canDiscover' }
            ];

            expect(wrapper.vm.authenticated_role).toBe('canDiscover');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(updated_authenticated_roles);
            expect(wrapper.vm.submit_roles.roles).toEqual(updated_authenticated_roles);

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
            expect(wrapper.vm.everyone_role).toBe('canViewMetadata');
            expect(wrapper.vm.authenticated_role).toBe('canViewMetadata');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([{ principal: 'everyone', role: 'canViewMetadata' }]);
            expect(wrapper.vm.patron_roles.assigned.roles).toEqual(same_roles.assigned.roles);
            done();
        });
    });

    it("adds embargoes", () => {
        expect(wrapper.vm.submit_roles.embargo).toEqual(null);
        wrapper.vm.$refs.embargoInfo.$emit('embargo-info', '2099-01-01');
        expect(wrapper.vm.submit_roles.embargo).toEqual('2099-01-01');
    });

    it("removes embargoes", () => {
        wrapper.setData({
            submit_roles: {
                embargo: '2099-01-01'
            }
        });

        expect(wrapper.vm.submit_roles.embargo).toEqual('2099-01-01');
        wrapper.vm.$refs.embargoInfo.$emit('embargo-info', null);
        expect(wrapper.vm.submit_roles.embargo).toEqual(null);
    });

    it("disables 'submit' by default", () => {
        let btn = wrapper.find('#is-submitting');
        let is_disabled = expect.stringContaining('disabled');
        expect(btn.html()).toEqual(is_disabled);
    });

    it("enables 'submit' button if user/role has been added or changed", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            let btn = wrapper.find('#is-submitting');
            let is_disabled = expect.stringContaining('disabled');
            wrapper.findAll('option').at(0).setSelected();
            expect(btn.html()).not.toEqual(is_disabled);
            done();
        });
    });

    it("checks for unsaved changes", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            expect(wrapper.vm.unsaved_changes).toBe(false);

            wrapper.findAll('option').at(0).setSelected();
            expect(wrapper.vm.unsaved_changes).toBe(true);
            done();
        });
    });

    it("prompts the user if 'Cancel' is clicked and there are unsaved changes", () => {
        wrapper.setData({
            unsaved_changes: true
        });
        wrapper.find('#is-canceling').trigger('click');
        expect(global.confirm).toHaveBeenCalled();
    });

    afterEach(() => {
        moxios.uninstall();
    });

    function stubDataLoad() {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(response)
        });
    }
});