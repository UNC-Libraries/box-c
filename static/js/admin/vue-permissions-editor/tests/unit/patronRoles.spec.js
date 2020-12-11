import { createLocalVue, shallowMount } from '@vue/test-utils';
import patronRoles from '@/components/patronRoles.vue';
import moxios from 'moxios';

const localVue = createLocalVue();

const UUID = '73bc003c-9603-4cd9-8a65-93a22520ef6a';
const embargo_date = '2099-01-01';
const inherited_roles = [{ principal: 'everyone', role: 'canViewOriginals', assignedTo: 'null' },
    { principal: 'authenticated', role: 'canViewOriginals', assignedTo: 'null' }];
const assigned_roles = [{ principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID  },
    { principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID  }];
const response = {
    inherited: { roles: inherited_roles, deleted: false, embargo: null, assignedTo: 'null' },
    assigned: { roles: assigned_roles,  deleted: false, embargo: null, assignedTo: UUID }
};
const role_history = {
    authenticated: 'canViewAccessCopies',
    patron: 'canViewAccessCopies'
};
const none_response = {
    inherited: { roles: null, deleted: false, embargo: null },
    assigned: { roles: [
            { principal: 'everyone', role: 'none', assignedTo: UUID  },
            { principal: 'authenticated', role: 'none', assignedTo: UUID  }
        ],
        deleted: false,
        embargo: null
    }
};
const empty_response = {
    inherited: { roles: [], deleted: false, embargo: null },
    assigned: { roles: [], deleted: false, embargo: null }
};
const same_assigned_roles = {
    inherited: { roles: [{ principal: 'everyone', role: 'canViewMetadata', assignedTo: 'null' },
            {principal: 'authenticated', role: 'canViewOriginals', assignedTo: 'null'}],
        deleted: false, embargo: null },
    assigned: { roles: [{ principal: 'everyone', role: 'canViewMetadata', assignedTo: UUID },
            { principal: 'authenticated', role: 'canViewMetadata', assignedTo: UUID }],
        deleted: false, embargo: null }
};
const full_roles = {
    inherited: { roles: [{ principal: 'everyone', role: 'canViewOriginals', assignedTo: 'null'},
            { principal: 'authenticated', role: 'canViewMetadata', assignedTo: 'null' }
        ], deleted: false, embargo: null },
    assigned: { roles: [
            { principal: 'everyone', role: 'none', assignedTo: UUID },
            { principal: 'authenticated', role: "canViewOriginals", assignedTo: UUID }
        ],
        deleted: false, embargo: null }
};
let compacted_assigned_can_view_roles = [
    { principal: 'patron', role: 'canViewAccessCopies', deleted: false, embargo: false, type: 'assigned', assignedTo: UUID },
];
const STAFF_PERMS = [
    { principal: 'everyone', role: 'none' },
    { principal: 'authenticated', role: 'none' }
];
const DEFAULT_PERMS = [
    { principal: 'everyone', role: 'canViewOriginals' },
    { principal: 'authenticated', role: 'canViewOriginals' }
];

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
                uuid: UUID
            }
        });

        global.confirm = jest.fn().mockReturnValue(true);
        selects = wrapper.findAll('select');
    });

    it("submits updated roles to the server", (done) => {
        wrapper = shallowMount(patronRoles, {
            localVue,
            propsData: {
                alertHandler: {
                    alertHandler: jest.fn() // This method lives outside of the Vue app
                },
                changesCheck: false,
                containerType: 'Folder',
                uuid: '73bc003c-9603-4cd9-8a65-93a22520ef6a'
            }
        });

        let saveRoles = jest.spyOn(wrapper.vm, 'saveRoles');
        stubDataLoad();

        moxios.wait(async () => {
            wrapper.setData({
                submit_roles: {
                    embargo: embargo_date
                },
                unsaved_changes: true
            });

            await wrapper.vm.$nextTick();
            wrapper.find('#is-submitting').trigger('click');

            expect(saveRoles).toHaveBeenCalled();
            saveRoles.mockRestore();
            done();
        });
    });

    it("updates assigned permissions after saving to the server", () => {
        wrapper.setData({
            patron_roles: full_roles,
            submit_roles: { roles: [
                    { principal: 'everyone', role: 'none' },
                    { principal: 'authenticated', role: "none" }
                ],
                deleted: false, embargo: null
            },
            unsaved_changes: true
        });

        expect(wrapper.vm.patron_roles.inherited).toEqual(full_roles.inherited);
        expect(wrapper.vm.patron_roles.assigned).toEqual(full_roles.assigned);

        wrapper.find('#is-submitting').trigger('click');

        // moxios.wait() from moxios and wrapper.vm.$nextTick() from vue-test-utils don't work
        // So using a timeout
        setTimeout(function() {
            expect(wrapper.vm.patron_roles.assigned).toEqual({ roles: [
                    { principal: 'everyone', role: 'none' },
                    { principal: 'authenticated', role: "none" }
                ],
                deleted: false, embargo: null
            });
        }, 5000);
    });

    it("retrieves patron roles from the server", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            expect(wrapper.vm.display_roles).toEqual(response);
            expect(wrapper.vm.patron_roles).toEqual(response);
            expect(wrapper.vm.submit_roles).toEqual(response.assigned);
            expect(wrapper.vm.dedupedRoles).toEqual([{
                principal: 'patron',
                role: 'canViewAccessCopies',
                type: 'assigned',
                assignedTo: UUID,
                deleted: false,
                embargo: false
            }]);
            done();
        });
    });

    it("sets default roles if no inherited roles are returned", (done) => {
        const assigned_roles = [
            { principal: "authenticated", role: "canViewOriginals" },
            { principal: "everyone", role: "canViewAccessCopies" }
        ];
        const no_inherited_assigned_roles = {
            inherited: { roles: [], deleted: false, embargo: null },
            assigned: {
                roles: assigned_roles,
                embargo: null, deleted: false
            }
        };

        stubDataLoad(no_inherited_assigned_roles);

        moxios.wait(() => {
            expect(wrapper.vm.display_roles.inherited.roles).toEqual(STAFF_PERMS);
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(assigned_roles);
            expect(wrapper.vm.patron_roles.inherited.roles).toEqual([]);
            expect(wrapper.vm.patron_roles.assigned.roles).toEqual(assigned_roles);
            expect(wrapper.vm.dedupedRoles).toEqual([
                { principal: 'staff', role: 'none', deleted: false, embargo: false, type: 'inherited' }
            ])
            expect(wrapper.vm.submit_roles.roles).toEqual(assigned_roles);
            done();
        });
    });

    it("sets default roles on load if neither inherited or assigned roles are returned", (done) => {
        stubDataLoad(empty_response)

        moxios.wait(() => {
            expect(wrapper.vm.display_roles.inherited.roles).toEqual(STAFF_PERMS);
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(DEFAULT_PERMS);
            expect(wrapper.vm.patron_roles.inherited.roles).toEqual([]);
            expect(wrapper.vm.patron_roles.assigned.roles).toEqual([]);
            expect(wrapper.vm.submit_roles.roles).toEqual([]);
            done();
        });
    });

    it("does not set default inherited display roles for collections and sets assigned permissions to 'none'" +
        "if no assigned permissions are returned", (done) => {
        wrapper.setProps({
            containerType: 'Collection'
        });

        stubDataLoad(empty_response);

        moxios.wait(() => {
            expect(wrapper.vm.display_roles.inherited.roles).toEqual([]);
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                {principal: 'everyone', role: 'none'},
                {principal: 'authenticated', role: 'none'}
            ]);
            expect(wrapper.vm.submit_roles.roles).toEqual([]);
            expect(wrapper.vm.dedupedRoles).toEqual([
                { principal: 'staff', role: 'none', type: 'assigned', deleted: false, embargo: false },
            ]);
            done();
        })
    });

    it("sets display 'staff' if assigned everyone and authenticated principals both have the 'none' and inherited principals have the same role", (done) => {
        const staff_assigned = {
            inherited: { roles: [
                    { principal: 'everyone', role: 'canViewOriginals' },
                    { principal: 'authenticated', role: 'canViewOriginals' }
                ], deleted: false, embargo: null },
            assigned: { roles: [
                    { principal: 'everyone', role: 'none' },
                    { principal: 'authenticated', role: 'none' }
                ], deleted: false, embargo: null
            }
        };

        stubDataLoad(staff_assigned);

        moxios.wait(() => {
            expect(wrapper.vm.dedupedRoles).toEqual([
                { principal: 'staff', role: 'none', deleted: false, embargo: false, type: 'assigned' }
            ]);
            done();
        })
    });

    it("sets form user type to 'staff' on form load if public and authenticated principals both have the 'none' role", (done) => {
        stubDataLoad(none_response);

        moxios.wait(() => {
            expect(wrapper.vm.user_type).toEqual('staff');
            done();
        })
    });

    it("sets form user type to 'parent' and form roles to 'canViewOriginals' if no assigned roles returned from the server", (done) => {
        const no_roles = {
            inherited: { roles: [], deleted: false, embargo: null },
            assigned: { roles: [], deleted: true, embargo: null }
        };

        stubDataLoad(no_roles);

        moxios.wait(() => {
            expect(wrapper.vm.user_type).toEqual('parent');
            expect(wrapper.vm.everyone_role).toEqual('canViewOriginals');
            expect(wrapper.vm.authenticated_role).toEqual('canViewOriginals');
            done();
        })
    });

    it("sets permission display to 'staff' and form roles to returned values if item is marked for deletion and roles are returned from the server", (done) => {
        const roleList = [{principal: 'everyone', role: 'canViewMetadata', assignedTo: UUID },
            { principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID }];

        const returned_roles = {
            inherited: {
                roles: [{principal: 'everyone', role: 'canViewMetadata', assignedTo: 'null'},
                    {principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: 'null'}],
                deleted: false,
                embargo: null
            },
            assigned: {
                roles: roleList,
                deleted: true,
                embargo: null
            }
        };

        stubDataLoad(returned_roles);

        moxios.wait(() => {
            expect(wrapper.vm.user_type).toEqual('patron');
            expect(wrapper.vm.everyone_role).toEqual('canViewMetadata');
            expect(wrapper.vm.authenticated_role).toEqual('canViewAccessCopies');
            expect(wrapper.vm.submit_roles).toEqual({
                deleted: true,
                embargo: null,
                roles: roleList
            })
            expect(wrapper.vm.dedupedRoles).toEqual([{
                principal: 'staff',
                role: 'none',
                deleted: true,
                embargo: false,
                type: 'assigned',
                assignedTo: UUID
            }])
            done();
        })
    });

    it("sets a radio button if button or it's text is clicked", () => {
        wrapper.find('#parent').trigger('click');
        expect(wrapper.vm.user_type).toBe('parent');

        wrapper.find('#patron').trigger('click');
        expect(wrapper.vm.user_type).toBe('patron');

        wrapper.find('#staff').trigger('click');
        expect(wrapper.vm.user_type).toBe('staff');

        wrapper.find('#parent input').trigger('click');
        expect(wrapper.vm.user_type).toBe('parent');

        wrapper.find('#patron input').trigger('click');
        expect(wrapper.vm.user_type).toBe('patron');

        wrapper.find('#staff input').trigger('click');
        expect(wrapper.vm.user_type).toBe('staff');
    });

    it("disables select boxes if 'Staff or Parent' wrapper text is clicked", (done) => {
        stubDataLoad();

        moxios.wait(async () => {
            expect(selects.at(0).attributes('disabled')).not.toBe('disabled');
            expect(selects.at(1).attributes('disabled')).not.toBe('disabled');

            wrapper.find('#staff').trigger('click');
            await wrapper.vm.$nextTick();

            expect(wrapper.vm.user_type).toEqual('staff');
            expect(selects.at(0).attributes('disabled')).toBe('disabled');
            expect(selects.at(1).attributes('disabled')).toBe('disabled');
            done();
        });
    });

    it("disables select boxes if 'Parent' wrapper text is clicked", (done) => {
        stubDataLoad();

        moxios.wait(async () => {
            expect(selects.at(0).attributes('disabled')).not.toBe('disabled');
            expect(selects.at(1).attributes('disabled')).not.toBe('disabled');

            wrapper.find('#parent').trigger('click');
            await wrapper.vm.$nextTick();

            expect(wrapper.vm.user_type).toEqual('parent');
            expect(selects.at(0).attributes('disabled')).toBe('disabled');
            expect(selects.at(1).attributes('disabled')).toBe('disabled');
            done();
        });
    });

    it("disables select boxes if 'Staff only access' radio button is checked", (done) => {
        stubDataLoad();

        moxios.wait(async () => {
            expect(selects.at(0).attributes('disabled')).not.toBe('disabled');
            expect(selects.at(1).attributes('disabled')).not.toBe('disabled');

            wrapper.find('#staff input').trigger('click');
            await wrapper.vm.$nextTick();

            expect(wrapper.vm.user_type).toEqual('staff');
            expect(selects.at(0).attributes('disabled')).toBe('disabled');
            expect(selects.at(1).attributes('disabled')).toBe('disabled');
            done();
        });
    });

    it("disables select boxes if 'Parent' radio button is checked", (done) => {
        stubDataLoad();

        moxios.wait(async () => {
            expect(selects.at(0).attributes('disabled')).not.toBe('disabled');
            expect(selects.at(1).attributes('disabled')).not.toBe('disabled');

            wrapper.find('#parent input').trigger('click');
            await wrapper.vm.$nextTick();

            expect(wrapper.vm.user_type).toEqual('parent');
            expect(selects.at(0).attributes('disabled')).toBe('disabled');
            expect(selects.at(1).attributes('disabled')).toBe('disabled');
            done();
        });
    });

    it("sets patron permissions to 'No Access' if 'Staff only access' wrapper text is clicked", (done) => {
        stubDataLoad(response);

        moxios.wait(() => {
            expect(wrapper.vm.everyone_role).toBe('canViewAccessCopies');
            expect(wrapper.vm.authenticated_role).toBe('canViewAccessCopies');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(response.assigned.roles);

            wrapper.find('#staff').trigger('click');

            expect(wrapper.vm.everyone_role).toBe('none');
            expect(wrapper.vm.authenticated_role).toBe('none');

            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                { principal: "everyone", role: 'none'},
                { principal: "authenticated", role: 'none'}
            ]);
            expect(wrapper.vm.dedupedRoles).toEqual([
                { principal: "staff", role: 'none', type: 'assigned', deleted: false, embargo: false }
            ]);
            expect(wrapper.vm.submit_roles.roles).toEqual([
                {principal: 'everyone', role: 'none', assignedTo: UUID },
                {principal: 'authenticated', role: 'none', assignedTo: UUID }
            ]);
            done();
        });
    });

    it("sets patron permissions to 'No Access' if 'Staff only access' radio button is checked", (done) => {
        stubDataLoad(response);

        moxios.wait(() => {
            expect(wrapper.vm.everyone_role).toBe('canViewAccessCopies');
            expect(wrapper.vm.authenticated_role).toBe('canViewAccessCopies');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(response.assigned.roles);

            wrapper.find('#staff input').trigger('click');

            expect(wrapper.vm.everyone_role).toBe('none');
            expect(wrapper.vm.authenticated_role).toBe('none');

            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                { principal: "everyone", role: 'none'},
                { principal: "authenticated", role: 'none'}
            ]);
            expect(wrapper.vm.dedupedRoles).toEqual([
                { principal: "staff", role: 'none', type: 'assigned', deleted: false, embargo: false }
            ]);
            expect(wrapper.vm.submit_roles.roles).toEqual([
                {principal: 'everyone', role: 'none', assignedTo: UUID },
                {principal: 'authenticated', role: 'none', assignedTo: UUID }
            ]);
            done();
        });
    });

    it("sets form patron permissions to 'CanViewOriginals' if 'Parent' wrapper text is checked", (done) => {
        const role = 'canViewOriginals';
        stubDataLoad(response);

        moxios.wait(() => {
            expect(wrapper.vm.everyone_role).toBe('canViewAccessCopies');
            expect(wrapper.vm.authenticated_role).toBe('canViewAccessCopies');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(response.assigned.roles);

            wrapper.find('#parent').trigger('click');

            expect(wrapper.vm.everyone_role).toBe(role);
            expect(wrapper.vm.authenticated_role).toBe(role);

            expect(wrapper.vm.display_roles.assigned.roles).toEqual([]);
            expect(wrapper.vm.dedupedRoles).toEqual([
                { principal: 'patron', role: role, type: 'inherited', deleted: false, embargo: false, assignedTo: 'null' }
            ]);
            expect(wrapper.vm.submit_roles.roles).toEqual([]);
            done();
        });
    });

    it("sets patron form permissions to 'CanViewOriginals' if 'Parent' radio button is checked", (done) => {
        const role = 'canViewOriginals';
        stubDataLoad(response);

        moxios.wait(() => {
            expect(wrapper.vm.everyone_role).toBe('canViewAccessCopies');
            expect(wrapper.vm.authenticated_role).toBe('canViewAccessCopies');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(response.assigned.roles);

            wrapper.find('#parent input').trigger('click');

            expect(wrapper.vm.everyone_role).toBe(role);
            expect(wrapper.vm.authenticated_role).toBe(role);

            expect(wrapper.vm.display_roles.assigned.roles).toEqual([]);
            expect(wrapper.vm.dedupedRoles).toEqual([
                { principal: 'patron', role: role, type: 'inherited', deleted: false, embargo: false, assignedTo: 'null' }
            ]);
            expect(wrapper.vm.submit_roles.roles).toEqual([]);
            done();
        });
    });

    it("sets role history", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            wrapper.find('#staff input').trigger('click');

            expect(wrapper.vm.everyone_role).toBe('none');
            expect(wrapper.vm.authenticated_role).toBe('none');
            expect(wrapper.vm.role_history).toEqual(role_history);
            expect(wrapper.vm.history_set).toBe(true);
            done();
        });
    });

    it("loads role history", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            expect(wrapper.vm.role_history).toEqual(role_history);
            expect(wrapper.vm.history_set).toBe(true);

            wrapper.find('#staff input').trigger('click');
            expect(wrapper.vm.everyone_role).toBe('none');
            expect(wrapper.vm.authenticated_role).toBe('none');
            expect(wrapper.vm.role_history).toEqual(role_history);
            expect(wrapper.vm.history_set).toBe(true);

            wrapper.find('#patron input').trigger('click');
            expect(wrapper.vm.everyone_role).toBe('canViewAccessCopies');
            expect(wrapper.vm.authenticated_role).toBe('canViewAccessCopies');
            expect(wrapper.vm.role_history).toEqual(role_history);
            expect(wrapper.vm.history_set).toBe(false);
            done();
        });
    });

    it("compacts ui permission display if 'winning' permissions are the same and of the same type", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            expect(wrapper.vm.dedupedRoles).toEqual(compacted_assigned_can_view_roles);
            done();
        });
    });

    it("does not compact ui permission display if 'winning' permissions are the same and of different types", (done) => {
        const roles = {
            inherited: { roles: [{ principal: 'everyone', role: 'none', assignedTo: 'null'},
                    { principal: 'authenticated', role: 'canViewMetadata', assignedTo: 'null' }
                ], deleted: false, embargo: null },
            assigned: { roles: [
                    { principal: 'everyone', role: 'none', assignedTo: UUID },
                    { principal: 'authenticated', role: "none", assignedTo: UUID }
                ], deleted: false, embargo: null }
        };

        stubDataLoad(roles);

        moxios.wait(() => {
            expect(wrapper.vm.dedupedRoles).toEqual([
                { principal: 'everyone', role: 'none', deleted: false, embargo: false, type: 'inherited', assignedTo: 'null' },
                { principal: 'authenticated', role: 'none', deleted: false, embargo: false, type: 'assigned', assignedTo: UUID }
            ]);
            done();
        });
    });

    it("updates permissions for public users", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            expect(wrapper.vm.everyone_role).toBe('canViewAccessCopies');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(assigned_roles);

            wrapper.findAll('#public option').at(0).setSelected();

            expect(wrapper.vm.everyone_role).toBe('none');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                { principal: 'everyone', role: 'none' },
                { principal: 'authenticated', role: 'canViewAccessCopies' }
            ]);
            expect(wrapper.vm.dedupedRoles).toEqual([
                { principal: 'everyone', role: 'none', deleted: false, embargo: false, type: 'assigned' },
                { principal: 'authenticated', role: 'canViewAccessCopies', deleted: false, embargo: false, type: 'assigned' }
            ]);
            expect(wrapper.vm.submit_roles.roles).toEqual([
                { principal: 'everyone', role: 'none', assignedTo: UUID },
                { principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID }
            ]);

            done();
        });
    });

    it("updates permissions for authenticated users", (done) => {
        stubDataLoad(same_assigned_roles);

        moxios.wait(() => {
            expect(wrapper.vm.authenticated_role).toBe('canViewMetadata');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(same_assigned_roles.assigned.roles);

            wrapper.findAll('#authenticated option').at(3).setSelected();

            let updated_authenticated_roles =  [
                { principal: 'everyone', role: 'canViewMetadata', assignedTo: UUID },
                { principal: 'authenticated', role: 'canViewOriginals', assignedTo: UUID }
            ];

            expect(wrapper.vm.authenticated_role).toBe('canViewOriginals');
            expect(wrapper.vm.dedupedRoles).toEqual([
                { principal: 'everyone', role: 'canViewMetadata', deleted: false, embargo: false, type: 'inherited', assignedTo: 'null' },
                { principal: 'authenticated', role: 'canViewOriginals', deleted: false, embargo: false, type: 'inherited', assignedTo: 'null' }
            ])
            expect(wrapper.vm.submit_roles.roles).toEqual(updated_authenticated_roles);

            done();
        });
    });

    it("does not update display roles if an embargo is not set from server response", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            expect(wrapper.vm.dedupedRoles).toEqual(compacted_assigned_can_view_roles);
            done();
        })
    });

    it("updates display roles if an embargo is set from server response on the object", (done) => {
        let values = {
            inherited: {
                roles: [
                    {principal: 'everyone', role: 'canViewOriginals'},
                    {principal: 'authenticated', role: 'canViewOriginals'}
                ], deleted: false, embargo: embargo_date
            },
            assigned: {
                roles: [
                    {principal: 'everyone', role: 'canViewAccessCopies'},
                    {principal: 'authenticated', role: 'canViewOriginals'}
                ], deleted: false, embargo: null
            }
        };

        stubDataLoad(values);

        moxios.wait(() => {
            expect(wrapper.vm.dedupedRoles).toEqual([
                { principal: 'patron', role: 'canViewMetadata', deleted: false, embargo: true, type: 'inherited' }
            ]);
            done();
        })
    });

    it("updates display roles if an embargo is set from the form", (done) => {
        let values = {
            inherited: {
                roles: [
                    {principal: 'everyone', role: 'canViewOriginals'},
                    {principal: 'authenticated', role: "canViewOriginals"}
                ], deleted: false, embargo: null
            },
            assigned: {
                roles: [
                    {principal: 'everyone', role: 'canViewAccessCopies'},
                    {principal: 'authenticated', role: "canViewOriginals"}
                ], deleted: false, embargo: null
            }
        };

        stubDataLoad(values);

        moxios.wait(() => {
            expect(wrapper.vm.display_roles).toEqual(values);
            wrapper.vm.$refs.embargoInfo.$emit('embargo-info', embargo_date);
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(values.assigned.roles);
            expect(wrapper.vm.submit_roles.embargo).toEqual(embargo_date);
            expect(wrapper.vm.dedupedRoles).toEqual([
                { principal: 'patron', role: 'canViewMetadata', deleted: false, embargo: true, type: 'assigned' }
            ]);
            done();
        });
    });

    it("updates display roles to 'canViewMetadata or lowest assigned role if an embargo is set", (done) => {
        stubDataLoad(full_roles);

        moxios.wait(() => {
            expect(wrapper.vm.display_roles).toEqual(full_roles);
            wrapper.vm.$refs.embargoInfo.$emit('embargo-info', embargo_date);
            expect(wrapper.vm.dedupedRoles).toEqual([
                { principal: 'everyone', role: 'none', deleted: false, embargo: true, type: 'assigned', assignedTo: UUID },
                { principal: 'authenticated', role: 'canViewMetadata', deleted: false, embargo: false, type: 'inherited', assignedTo: 'null' }
            ]);
            done();
        })
    });

    it("updates submit roles if an embargo is added or removed", (done) => {
        stubDataLoad(full_roles);

        moxios.wait(() => {
            expect(wrapper.vm.submit_roles.embargo).toEqual(null);

            wrapper.vm.$refs.embargoInfo.$emit('embargo-info', embargo_date);
            expect(wrapper.vm.submit_roles.embargo).toEqual(embargo_date);

            wrapper.vm.$refs.embargoInfo.$emit('embargo-info', null);
            expect(wrapper.vm.submit_roles.embargo).toEqual(null);
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

        moxios.wait(async () => {
            let btn = wrapper.find('#is-submitting');
            let is_disabled = expect.stringContaining('disabled');
            wrapper.findAll('option').at(1).setSelected();
            await wrapper.vm.$nextTick();
            expect(btn.html()).not.toEqual(is_disabled);
            done();
        });
    });

    it("checks for unsaved changes", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            expect(wrapper.vm.unsaved_changes).toBe(false);

            wrapper.findAll('option').at(1).setSelected();
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

    function stubDataLoad(load = response) {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(load)
        });
    }
});