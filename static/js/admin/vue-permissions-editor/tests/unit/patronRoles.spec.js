import { createLocalVue, shallowMount } from '@vue/test-utils';
import patronRoles from '@/components/patronRoles.vue';
import moxios from 'moxios';

const localVue = createLocalVue();
const STAFF_ONLY_ROLE_TEXT = '\u2014';

let embargo_date = '2099-01-01';

let response = {
    inherited: { roles: [{ principal: 'everyone', role: 'canViewOriginals' }], deleted: false, embargo: null },
    assigned: { roles: [{ principal: 'everyone', role: 'canViewMetadata' }], deleted: false, embargo: null }
};

let response_display = {
    inherited: { roles: [{ principal: 'Public Users', role: 'canViewOriginals' }], deleted: false, embargo: null },
    assigned: { roles: [{ principal: 'Public Users', role: 'canViewMetadata' }], deleted: false, embargo: null }
}

let none_response = {
    inherited: { roles: null, deleted: false, embargo: null },
    assigned: { roles: [
            { principal: 'everyone', role: 'none' },
            { principal: 'authenticated', role: 'none' }
        ],
        deleted: false,
        embargo: null
    }
};

let empty_response = {
    inherited: { roles: [], deleted: false, embargo: null },
    assigned: { roles: [], deleted: false, embargo: null }
};

let empty_defaults = [
    { principal: 'everyone', role: 'canViewOriginals' },
    { principal: 'authenticated', role: 'canViewOriginals' }
];

let empty_response_no_inherited = {
    inherited: { roles: [], deleted: false, embargo: null },
    assigned: { roles: empty_defaults, deleted: false, embargo: null }
};

let same_roles = {
    inherited: { roles: [{ principal: 'everyone', role: 'canViewMetadata' }, {principal: 'authenticated', role: 'canViewOriginals'}],
        deleted: false, embargo: null  },
    assigned: { roles: [{ principal: 'everyone', role: 'canViewMetadata' }, { principal: 'authenticated', role: 'canViewMetadata'}],
        deleted: false, embargo: null  }
};

let full_roles = {
    inherited: { roles: [{ principal: 'everyone', role: 'canViewOriginals' }], deleted: false, embargo: null },
    assigned: { roles: [
            { principal: 'everyone', role: 'none'},
            { principal: 'authenticated', role: "canViewOriginals" }
        ],
        deleted: false, embargo: null }
};

let full_roles_display = {
    inherited: { roles: [{ principal: 'Public Users', role: 'canViewOriginals' }], deleted: false, embargo: null },
    assigned: { roles: [
            { principal: 'Public Users', role: 'none'},
            { principal: 'authenticated', role: "canViewOriginals" }
        ],
        deleted: false, embargo: null }
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
                containerType: 'Folder',
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
                    embargo: embargo_date
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
            expect(wrapper.vm.display_roles).toEqual(response_display);
            expect(wrapper.vm.patron_roles).toEqual(response);
            expect(wrapper.vm.submit_roles).toEqual(response.assigned);
            done();
        });
    });

    it("sets default roles if no inherited roles are returned", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(empty_response_no_inherited)
        });

        moxios.wait(() => {
            expect(wrapper.vm.display_roles.inherited.roles).toEqual([{ principal: 'staff', role: STAFF_ONLY_ROLE_TEXT }]);
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([{ principal: 'patron', role: 'canViewOriginals' }]);
            expect(wrapper.vm.patron_roles.inherited.roles).toEqual([]);
            expect(wrapper.vm.patron_roles.assigned.roles).toEqual(empty_defaults);
            expect(wrapper.vm.submit_roles.roles).toEqual(empty_defaults);
            done();
        });
    });

    it("sets default roles if neither inherited or assigned roles are returned", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(empty_response)
        });

        moxios.wait(() => {
            expect(wrapper.vm.display_roles.inherited.roles).toEqual([{ principal: 'staff', role: STAFF_ONLY_ROLE_TEXT }]);
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([{ principal: 'patron', role: 'canViewOriginals' }]);
            expect(wrapper.vm.patron_roles.inherited.roles).toEqual([]);
            expect(wrapper.vm.patron_roles.assigned.roles).toEqual(empty_defaults);
            expect(wrapper.vm.submit_roles.roles).toEqual(empty_defaults);
            done();
        });
    });

    it("does not set default inherited display roles for collections", (done) => {
        wrapper.setProps({
            containerType: 'Collection'
        });
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(empty_response)
        });

        moxios.wait(() => {
            expect(wrapper.vm.display_roles.inherited.roles).toEqual([]);
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([{ principal: 'patron', role: 'canViewOriginals' }]);
            done();
        })
    });

    it("sets display 'staff' if public and authenticated principals both have the 'none' role", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(none_response)
        });

        moxios.wait(() => {
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([{ principal: 'staff', role: STAFF_ONLY_ROLE_TEXT }]);
            done();
        })
    });

    it("sets form user type to 'staff' if public and authenticated principals both have the 'none' role", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(none_response)
        });

        moxios.wait(() => {
            expect(wrapper.vm.user_type).toEqual('staff');
            done();
        })
    });

    it("sets form user type to 'staff' and form roles to 'canViewOriginals' if item is marked for deletion and no roles returned from the server", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify({
                inherited: { roles: null, deleted: false, embargo: null },
                assigned: { roles: [],
                    deleted: true,
                    embargo: null
                }
            })
        });

        moxios.wait(() => {
            expect(wrapper.vm.user_type).toEqual('staff');
            expect(wrapper.vm.everyone_role).toEqual('canViewOriginals');
            expect(wrapper.vm.authenticated_role).toEqual('canViewOriginals');
            done();
        })
    });

    it("sets form user type to 'staff' and form roles to returned values if item is marked for deletion and roles are returned from the server", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify({
                inherited: { roles: null, deleted: false, embargo: null },
                assigned: { roles: [{ principal: 'everyone', role: 'canViewMetadata'}, { principal: 'authenticated', role: 'canViewAccessCopies'}],
                    deleted: true,
                    embargo: null
                }
            })
        });

        moxios.wait(() => {
            expect(wrapper.vm.user_type).toEqual('staff');
            expect(wrapper.vm.everyone_role).toEqual('canViewMetadata');
            expect(wrapper.vm.authenticated_role).toEqual('canViewAccessCopies');
            done();
        })
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
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(response_display.assigned.roles);

            wrapper.find('#staff').trigger('click');

            expect(wrapper.vm.everyone_role).toBe('none');
            expect(wrapper.vm.authenticated_role).toBe('none');

            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                {principal: 'staff', role: STAFF_ONLY_ROLE_TEXT}
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
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([{ principal: 'Public Users', role: 'canViewMetadata' }]);

            wrapper.findAll('#public option').at(2).setSelected();

            let updated_public_roles =  [
                { principal: 'everyone', role: 'canViewAccessCopies' },
                { principal: 'authenticated', role: 'none' }
            ];

            expect(wrapper.vm.everyone_role).toBe('canViewAccessCopies');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                { principal: 'Public Users', role: 'canViewAccessCopies' },
                { principal: 'authenticated', role: 'none' }
            ]);
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
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([{ principal: 'patron', role: 'canViewMetadata' }]);

            wrapper.findAll('#authenticated option').at(3).setSelected();

            let updated_authenticated_roles =  [
                { principal: 'everyone', role: 'canViewMetadata' },
                { principal: 'authenticated', role: 'canViewOriginals' }
            ];

            expect(wrapper.vm.authenticated_role).toBe('canViewOriginals');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                { principal: 'Public Users', role: 'canViewMetadata' },
                { principal: 'authenticated', role: 'canViewOriginals' }
            ]);
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
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([{ principal: 'patron', role: 'canViewMetadata' }]);
            expect(wrapper.vm.patron_roles.assigned.roles).toEqual(same_roles.assigned.roles);
            done();
        });
    });

    it("adds embargoes", () => {
        expect(wrapper.vm.submit_roles.embargo).toEqual(null);
        wrapper.vm.$refs.embargoInfo.$emit('embargo-info', embargo_date);
        expect(wrapper.vm.submit_roles.embargo).toEqual(embargo_date);
    });

    it("does not update display roles if an embargo is not set from server response", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(same_roles)
        });

        moxios.wait(() => {
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([{ principal: 'patron', role: 'canViewMetadata' }]);
            done();
        })
    });

    it("updates display roles if an embargo is set from server response on the object", (done) => {
        let values = {
            inherited: { roles: [{ principal: 'everyone', role: 'canViewOriginals' }], deleted: false, embargo: null },
            assigned: { roles: [
                    { principal: 'everyone', role: 'canViewAccessCopies'},
                    { principal: 'authenticated', role: "canViewOriginals" }
                ],
                deleted: false, embargo: embargo_date }
        };

        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(values)
        });

        moxios.wait(() => {
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([{ principal: 'patron', role: 'canViewMetadata' }]);
            done();
        })
    });

    it("updates display roles if an embargo is set", (done) => {
        let values = {
            inherited: { roles: [{ principal: 'everyone', role: 'canViewOriginals' }], deleted: false, embargo: null },
            assigned: { roles: [
                    { principal: 'everyone', role: 'canViewAccessCopies'},
                    { principal: 'authenticated', role: "canViewOriginals" }
                ],
                deleted: false, embargo: null }
        };

        let values_display = {
            inherited: { roles: [{ principal: 'Public Users', role: 'canViewOriginals' }], deleted: false, embargo: null },
            assigned: { roles: [
                    { principal: 'Public Users', role: 'canViewAccessCopies'},
                    { principal: 'authenticated', role: "canViewOriginals" }
                ],
                deleted: false, embargo: null }
        };

        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(values)
        });

        moxios.wait(() => {
            expect(wrapper.vm.display_roles).toEqual(values_display);
            wrapper.vm.$refs.embargoInfo.$emit('embargo-info', embargo_date);
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([{ principal: 'patron', role: 'canViewMetadata' }]);
            done();
        })
    });

    it("updates display roles to 'canViewMetadata or lowest assigned role if an embargo is set", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(full_roles)
        });

        moxios.wait(() => {
            expect(wrapper.vm.display_roles).toEqual(full_roles_display);
            wrapper.vm.$refs.embargoInfo.$emit('embargo-info', embargo_date);
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([{ principal: 'Public Users', role: 'none' },
                { principal: 'authenticated', role: 'canViewMetadata'}]);
            done();
        })
    });

    it("updates submit roles if an embargo is added or removed", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(full_roles)
        });

        moxios.wait(() => {
            expect(wrapper.vm.submit_roles.embargo).toEqual(null);

            wrapper.vm.$refs.embargoInfo.$emit('embargo-info', embargo_date);
            expect(wrapper.vm.submit_roles.embargo).toEqual(embargo_date);

            wrapper.vm.$refs.embargoInfo.$emit('embargo-info', null);
            expect(wrapper.vm.submit_roles.embargo).toEqual(null);
            done();
        })
    });

    it("removes embargoes", () => {
        wrapper.setData({
            submit_roles: {
                embargo: embargo_date
            }
        });

        expect(wrapper.vm.submit_roles.embargo).toEqual(embargo_date);
        wrapper.vm.$refs.embargoInfo.$emit('embargo-info', null);
        expect(wrapper.vm.submit_roles.embargo).toEqual(null);
    });

    it("updates display roles to their assigned values if an embargo is removed", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(full_roles)
        });

        moxios.wait(() => {
            wrapper.vm.$refs.embargoInfo.$emit('embargo-info', embargo_date);
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([{ principal: 'Public Users', role: 'none' }, { principal: 'authenticated', role: 'canViewMetadata'}]);

            wrapper.vm.$refs.embargoInfo.$emit('embargo-info', null);
            expect(wrapper.vm.display_roles).toEqual(full_roles_display);
            done();
        })
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