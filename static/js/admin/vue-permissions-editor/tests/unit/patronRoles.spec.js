import { createLocalVue, shallowMount } from '@vue/test-utils';
import patronRoles from '@/components/patronRoles.vue';
import moxios from 'moxios';
import cloneDeep from 'lodash.clonedeep';

const localVue = createLocalVue();

const UUID = '73bc003c-9603-4cd9-8a65-93a22520ef6a';
const embargo_date = '2099-01-01';
const inherited_roles = [{ principal: 'everyone', role: 'canViewOriginals', assignedTo: null },
    { principal: 'authenticated', role: 'canViewOriginals', assignedTo: null }];
const assigned_roles = [{ principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID  },
    { principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID  }];
const response = {
    inherited: { roles: inherited_roles, deleted: false, embargo: null, assignedTo: null },
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
    inherited: { roles: [{ principal: 'everyone', role: 'canViewMetadata', assignedTo: null },
            {principal: 'authenticated', role: 'canViewOriginals', assignedTo: null}],
        deleted: false, embargo: null },
    assigned: { roles: [{ principal: 'everyone', role: 'canViewMetadata', assignedTo: UUID },
            { principal: 'authenticated', role: 'canViewMetadata', assignedTo: UUID }],
        deleted: false, embargo: null }
};
const full_roles = {
    inherited: { roles: [{ principal: 'everyone', role: 'canViewOriginals', assignedTo: null},
            { principal: 'authenticated', role: 'canViewMetadata', assignedTo: null }
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
const staff_only_roles = [
    { principal: "everyone", role: "none", assignedTo: UUID },
    { principal: "authenticated", role: "none", assignedTo: UUID }
];

let wrapper, selects;

describe('patronRoles.vue', () => {
    beforeEach(() => {
        moxios.install();

        wrapper = shallowMount(patronRoles, {
            localVue,
            propsData: {
                actionHandler: {
                    addEvent: jest.fn()
                },
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
        stubDataLoad();

        moxios.wait(async () => {
            wrapper.setData({
                embargo: embargo_date
            });

            await wrapper.vm.$nextTick();
            stubDataSaveResponse();
            wrapper.find('#is-submitting').trigger('click');
            moxios.wait(async () => {
                let request = moxios.requests.mostRecent()
                expect(request.config.method).toEqual('put');
                expect(JSON.parse(request.config.data)).toEqual(
                    { roles: assigned_roles, deleted: false, embargo: embargo_date, assignedTo: UUID }
                );
                expect(wrapper.vm.hasUnsavedChanges).toBe(false);

                done();
            });
        });
    });

    it("unsaved changes flag resets after saving to the server", (done) => {
        stubDataLoad(full_roles);

        moxios.wait(async () => {
            expect(wrapper.vm.hasUnsavedChanges).toBe(false);

            wrapper.find('#user_type_staff').trigger('click');
            await wrapper.vm.$nextTick();
            expect(wrapper.vm.user_type).toEqual('staff');

            expect(wrapper.vm.hasUnsavedChanges).toBe(true);

            stubDataSaveResponse();
            wrapper.find('#is-submitting').trigger('click');
            moxios.wait(async () => {
                expect(wrapper.vm.hasUnsavedChanges).toBe(false);

                done();
            });
        });
    });

    it("commits uncommitted patron assignment", (done) => {
        const resp_with_allowed_patrons = {
            inherited: { roles: inherited_roles, deleted: false, embargo: null, assignedTo: null },
            assigned: { roles: assigned_roles,  deleted: false, embargo: null, assignedTo: UUID },
            allowedPrincipals: [{ principal: "my:special:group", name: "Special Group" }]
        };
        stubDataLoad(resp_with_allowed_patrons);

        moxios.wait(async () => {
            // Click to show the add other principal inputs
            wrapper.find('#add-principal').trigger('click');
            // Select the existing patron principal to add
            wrapper.findAll('#add-new-patron-principal-id option').at(0).setSelected();
            wrapper.findAll('#add-new-patron-principal-role option').at(4).setSelected();
            wrapper.find('#add-principal').trigger('click');

            await wrapper.vm.$nextTick();
            stubDataSaveResponse();
            wrapper.find('#is-submitting').trigger('click');
            moxios.wait(async () => {
                let request = moxios.requests.mostRecent()
                expect(request.config.method).toEqual('put');
                expect(JSON.parse(request.config.data)).toEqual(
                    { roles: [{ principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID  },
                            { principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID  },
                            { principal: 'my:special:group', role: 'canViewOriginals', assignedTo: UUID  }],
                        deleted: false, embargo: null, assignedTo: UUID }
                );
                expect(wrapper.vm.hasUnsavedChanges).toBe(false);

                done();
            });
        });
    });

    it("new assignment remove button hides inputs", (done) => {
        const resp_with_allowed_patrons = {
            inherited: { roles: inherited_roles, deleted: false, embargo: null, assignedTo: null },
            assigned: { roles: assigned_roles,  deleted: false, embargo: null, assignedTo: UUID },
            allowedPrincipals: [{ principal: "my:special:group", name: "Special Group" }]
        };
        stubDataLoad(resp_with_allowed_patrons);

        moxios.wait(async () => {
            expect(wrapper.find('#add-new-patron-principal').isVisible()).toBe(false);

            // Click to show the add principal inputs
            wrapper.find('#add-principal').trigger('click');
            wrapper.findAll('#add-new-patron-principal-id option').at(0).setSelected();
            wrapper.findAll('#add-new-patron-principal-role option').at(3).setSelected();
            await wrapper.vm.$nextTick();
            expect(wrapper.find('#add-new-patron-principal').isVisible()).toBe(true);

            wrapper.find('#add-new-patron-principal .btn-remove').trigger('click');
            await wrapper.vm.$nextTick();
            expect(wrapper.find('#add-new-patron-principal').isVisible()).toBe(false);
            expect(wrapper.find('#add-new-patron-principal-role').element.value).toEqual('canViewOriginals');
            expect(wrapper.find('#add-new-patron-principal-id').element.value).toEqual('');

            done();
        });
    });

    it("retrieves patron roles from the server", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            expect(wrapper.vm.assignedPatronRoles).toEqual(response.assigned.roles);
            expect(wrapper.vm.embargo).toEqual(response.assigned.embargo);
            expect(wrapper.vm.submissionAccessDetails()).toEqual(response.assigned);
            expect(wrapper.vm.displayAssignments).toEqual([{
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

    it("adds an custom patron group", (done) => {
        const resp_with_allowed_patrons = {
            inherited: { roles: inherited_roles, deleted: false, embargo: null, assignedTo: null },
            assigned: { roles: assigned_roles,  deleted: false, embargo: null, assignedTo: UUID },
            allowedPrincipals: [{ principal: "my:special:group", name: "Special Group" }]
        };
        stubDataLoad(resp_with_allowed_patrons);

        moxios.wait(async () => {
            expect(wrapper.vm.assignedPatronRoles).toEqual(resp_with_allowed_patrons.assigned.roles);

            // Click to show the add other principal inputs
            wrapper.find('#add-principal').trigger('click');
            // Select values for new patron role and then click the add button again
            wrapper.findAll('#add-new-patron-principal-id option').at(0).setSelected();
            wrapper.findAll('#add-new-patron-principal-role option').at(4).setSelected();
            wrapper.find('#add-principal').trigger('click');

            // Model should have updated by adding the new role to the list of assigned roles
            const assigned_other_roles = [{ principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID  },
                { principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID  },
                { principal: 'my:special:group', role: 'canViewOriginals', assignedTo: UUID  }];
            expect(wrapper.vm.assignedPatronRoles).toEqual(assigned_other_roles);

            // Once the UI has refreshed it should now show added entry
            await wrapper.vm.$nextTick();
            let assigned_patrons = wrapper.findAll('.patron-assigned');
            expect(assigned_patrons.length).toEqual(3);
            expect(assigned_patrons.at(0).findAll('p').at(0).text()).toEqual('Public users');
            expect(assigned_patrons.at(0).findAll('select').at(0).element.value).toEqual('canViewAccessCopies');
            expect(assigned_patrons.at(1).findAll('p').at(0).text()).toEqual('Authenticated users');
            expect(assigned_patrons.at(1).findAll('select').at(0).element.value).toEqual('canViewAccessCopies');
            expect(assigned_patrons.at(2).findAll('p').at(0).text()).toEqual('Special Group');
            expect(assigned_patrons.at(2).findAll('select').at(0).element.value).toEqual('canViewOriginals');

            // The new entry inputs should be cleared
            expect(wrapper.vm.new_assignment_principal).toEqual('');
            expect(wrapper.vm.new_assignment_role).toEqual('canViewOriginals');

            // Added group not active since no inherited role for it
            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: 'everyone', role: 'canViewAccessCopies', type: 'assigned', assignedTo: UUID, deleted: false, embargo: false },
                { principal: 'authenticated', role: 'canViewAccessCopies', type: 'assigned', assignedTo: UUID, deleted: false, embargo: false },
                { principal: 'my:special:group', role: 'none', type: 'inherited', assignedTo: null, deleted: false, embargo: false }
            ]);

            done();
        });
    });

    it("can only add non-duplicate principals", (done) => {
        const assigned_other_roles = [{ principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID  },
            { principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID  },
            { principal: 'my:special:group', role: 'canViewOriginals', assignedTo: UUID  }];
        const resp_with_allowed_patrons = {
            inherited: { roles: inherited_roles, deleted: false, embargo: null, assignedTo: null },
            assigned: { roles: assigned_other_roles,  deleted: false, embargo: null, assignedTo: UUID },
            allowedPrincipals: [{ principal: "my:special:group", name: "Special Group" },
                { principal: "the:extra:special:group", name: "Extra Special Group" }]
        };
        stubDataLoad(resp_with_allowed_patrons);

        moxios.wait(async () => {
            expect(wrapper.vm.assignedPatronRoles).toEqual(assigned_other_roles);

            // Click to show the add other principal inputs
            wrapper.find('#add-principal').trigger('click');
            // Select the existing patron principal to add
            wrapper.findAll('#add-new-patron-principal-id option').at(0).setSelected();
            wrapper.findAll('#add-new-patron-principal-role option').at(3).setSelected();
            wrapper.find('#add-principal').trigger('click');

            // Try adding principal that has not already been assigned
            wrapper.findAll('#add-new-patron-principal-id option').at(1).setSelected();
            wrapper.find('#add-principal').trigger('click');

            expect(wrapper.vm.assignedPatronRoles).toEqual([assigned_other_roles[0], assigned_other_roles[1], assigned_other_roles[2],
                { principal: 'the:extra:special:group', role: 'canViewAccessCopies', assignedTo: UUID }]);

            await wrapper.vm.$nextTick();
            let assigned_patrons = wrapper.findAll('.patron-assigned');
            expect(assigned_patrons.length).toEqual(4);
            expect(assigned_patrons.at(0).findAll('p').at(0).text()).toEqual('Public users');
            expect(assigned_patrons.at(0).findAll('select').at(0).element.value).toEqual('canViewAccessCopies');
            expect(assigned_patrons.at(1).findAll('p').at(0).text()).toEqual('Authenticated users');
            expect(assigned_patrons.at(1).findAll('select').at(0).element.value).toEqual('canViewAccessCopies');
            expect(assigned_patrons.at(2).findAll('p').at(0).text()).toEqual('Special Group');
            expect(assigned_patrons.at(2).findAll('select').at(0).element.value).toEqual('canViewOriginals');
            expect(assigned_patrons.at(3).findAll('p').at(0).text()).toEqual('Extra Special Group');
            expect(assigned_patrons.at(3).findAll('select').at(0).element.value).toEqual('canViewAccessCopies');
            done();
        });
    });

    it("removes other patron principals", (done) => {
        const assigned_other_roles = [{ principal: 'everyone', role: 'canViewMetadata', assignedTo: UUID  },
            { principal: 'authenticated', role: 'canViewMetadata', assignedTo: UUID  },
            { principal: 'less:special:group', role: 'canViewAccessCopies', assignedTo: UUID  },
            { principal: 'my:special:group', role: 'canViewAccessCopies', assignedTo: UUID  }];
        const inherited_roles = [{ principal: 'everyone', role: 'canViewOriginals', assignedTo: null },
            { principal: 'authenticated', role: 'canViewOriginals', assignedTo: null },
            { principal: 'less:special:group', role: 'canViewOriginals', assignedTo: null  },
            { principal: 'my:special:group', role: 'canViewOriginals', assignedTo: null  }];
        const resp_with_allowed_patrons = {
            inherited: { roles: inherited_roles, deleted: false, embargo: null, assignedTo: null },
            assigned: { roles: assigned_other_roles,  deleted: false, embargo: null, assignedTo: UUID },
            allowedPrincipals: [{ principal: "my:special:group", name: "Special Group" },
                { principal: "less:special:group", name: "Another Group" }]
        };
        stubDataLoad(resp_with_allowed_patrons);

        moxios.wait(async () => {
            expect(wrapper.vm.assignedPatronRoles.length).toEqual(4);
            expect(wrapper.vm.submissionAccessDetails()).toEqual(resp_with_allowed_patrons.assigned);
            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: 'everyone', role: 'canViewMetadata', type: 'assigned', assignedTo: UUID, deleted: false, embargo: false },
                { principal: 'authenticated', role: 'canViewMetadata', type: 'assigned', assignedTo: UUID, deleted: false, embargo: false },
                { principal: 'less:special:group', role: 'canViewAccessCopies', type: 'assigned', assignedTo: UUID, deleted: false, embargo: false },
                { principal: 'my:special:group', role: 'canViewAccessCopies', type: 'assigned', assignedTo: UUID, deleted: false, embargo: false }
            ]);
            expect(wrapper.vm.assignedPatronRoles).toEqual(assigned_other_roles);

            // Click the remove button for the first principal assignment
            wrapper.findAll("#assigned_principals_editor .btn-remove").at(1).trigger('click');

            await wrapper.vm.$nextTick();
            expect(wrapper.vm.assignedPatronRoles.length).toEqual(3);
            expect(wrapper.vm.assignedPatronRoles).toEqual(
                [assigned_other_roles[0], assigned_other_roles[1], assigned_other_roles[2]]);

            let assigned_patrons = wrapper.findAll('.patron-assigned');
            expect(assigned_patrons.length).toEqual(3);
            expect(assigned_patrons.at(0).findAll('p').at(0).text()).toEqual('Public users');
            expect(assigned_patrons.at(0).findAll('select').at(0).element.value).toEqual('canViewMetadata');
            expect(assigned_patrons.at(1).findAll('p').at(0).text()).toEqual('Authenticated users');
            expect(assigned_patrons.at(1).findAll('select').at(0).element.value).toEqual('canViewMetadata');
            expect(assigned_patrons.at(2).findAll('p').at(0).text()).toEqual('Another Group');
            expect(assigned_patrons.at(2).findAll('select').at(0).element.value).toEqual('canViewAccessCopies');

            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: 'everyone', role: 'canViewMetadata', type: 'assigned', assignedTo: UUID, deleted: false, embargo: false },
                { principal: 'authenticated', role: 'canViewMetadata', type: 'assigned', assignedTo: UUID, deleted: false, embargo: false },
                { principal: 'less:special:group', role: 'canViewAccessCopies', type: 'assigned', assignedTo: UUID, deleted: false, embargo: false },
                { principal: 'my:special:group', role: 'canViewOriginals', type: 'inherited', assignedTo: null, deleted: false, embargo: false }
            ]);

            expect(wrapper.vm.submissionAccessDetails().roles).toEqual(
                [assigned_other_roles[0], assigned_other_roles[1], assigned_other_roles[2]]);
            done();
        });
    });

    it("other patron principals assigned to collection", (done) => {
        const assigned_other_roles = [{ principal: 'everyone', role: 'canViewMetadata', assignedTo: UUID  },
            { principal: 'authenticated', role: 'canViewMetadata', assignedTo: UUID  },
            { principal: 'less:special:group', role: 'canViewAccessCopies', assignedTo: UUID  },
            { principal: 'my:special:group', role: 'canViewOriginals', assignedTo: UUID  }];
        const resp_with_allowed_patrons = {
            inherited: { roles: [], deleted: false, embargo: null, assignedTo: null },
            assigned: { roles: assigned_other_roles,  deleted: false, embargo: null, assignedTo: UUID },
            allowedPrincipals: [{ principal: "my:special:group", name: "Special Group" },
                { principal: "less:special:group", name: "Another Group" }]
        };
        wrapper.setProps({
            containerType: 'Collection'
        });
        stubDataLoad(resp_with_allowed_patrons);

        moxios.wait(async () => {
            expect(wrapper.vm.assignedPatronRoles).toEqual(assigned_other_roles);
            expect(wrapper.vm.submissionAccessDetails()).toEqual(resp_with_allowed_patrons.assigned);
            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: 'everyone', role: 'canViewMetadata', type: 'assigned', assignedTo: UUID, deleted: false, embargo: false },
                { principal: 'authenticated', role: 'canViewMetadata', type: 'assigned', assignedTo: UUID, deleted: false, embargo: false },
                { principal: 'less:special:group', role: 'canViewAccessCopies', type: 'assigned', assignedTo: UUID, deleted: false, embargo: false },
                { principal: 'my:special:group', role: 'canViewOriginals', type: 'assigned', assignedTo: UUID, deleted: false, embargo: false }
            ]);

            let assigned_patrons = wrapper.findAll('.patron-assigned');
            expect(assigned_patrons.length).toEqual(4);
            expect(assigned_patrons.at(0).findAll('p').at(0).text()).toEqual('Public users');
            expect(assigned_patrons.at(0).findAll('select').at(0).element.value).toEqual('canViewMetadata');
            expect(assigned_patrons.at(1).findAll('p').at(0).text()).toEqual('Authenticated users');
            expect(assigned_patrons.at(1).findAll('select').at(0).element.value).toEqual('canViewMetadata');
            expect(assigned_patrons.at(2).findAll('p').at(0).text()).toEqual('Another Group');
            expect(assigned_patrons.at(2).findAll('select').at(0).element.value).toEqual('canViewAccessCopies');
            expect(assigned_patrons.at(3).findAll('p').at(0).text()).toEqual('Special Group');
            expect(assigned_patrons.at(3).findAll('select').at(0).element.value).toEqual('canViewOriginals');

            done();
        });
    });

    it("inherited principals not present on object are displayed by default", (done) => {
        const inherited_other_roles = [{ principal: 'everyone', role: 'canViewAccessCopies', assignedTo: null  },
            { principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: null  },
            { principal: 'my:special:group', role: 'canViewOriginals', assignedTo: null  }
        ];
        const resp_with_allowed_patrons = {
            inherited: { roles: inherited_other_roles, deleted: false, embargo: null, assignedTo: null },
            assigned: { roles: assigned_roles,  deleted: false, embargo: null, assignedTo: UUID },
            allowedPrincipals: [{ principal: "my:special:group", name: "Special Group" }]
        };
        const expected_assigned = [
            { principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID  },
            { principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID  },
            { principal: 'my:special:group', role: 'canViewOriginals', assignedTo: UUID  }
        ];
        stubDataLoad(resp_with_allowed_patrons);

        moxios.wait(async () => {
            expect(wrapper.vm.assignedPatronRoles).toEqual(expected_assigned);

            wrapper.find('#user_type_parent').trigger('click');

            await wrapper.vm.$nextTick();
            expect(wrapper.vm.assignedPatronRoles).toEqual([]);

            let assigned_patrons = wrapper.findAll('.patron-assigned');
            expect(assigned_patrons.length).toEqual(3);
            expect(assigned_patrons.at(0).findAll('p').at(0).text()).toEqual('Public users');
            expect(assigned_patrons.at(0).findAll('select').at(0).element.value).toEqual('canViewAccessCopies');
            expect(assigned_patrons.at(1).findAll('p').at(0).text()).toEqual('Authenticated users');
            expect(assigned_patrons.at(1).findAll('select').at(0).element.value).toEqual('canViewAccessCopies');
            expect(assigned_patrons.at(2).findAll('p').at(0).text()).toEqual('Special Group');
            expect(assigned_patrons.at(2).findAll('select').at(0).element.value).toEqual('canViewOriginals');

            wrapper.find('#user_type_patron').trigger('click');
            await wrapper.vm.$nextTick();

            expect(wrapper.vm.assignedPatronRoles).toEqual(expected_assigned);

            done();
        });
    });

    it("sets default roles if no inherited roles are returned", (done) => {
        const assigned_roles = [
            { principal: "everyone", role: "canViewAccessCopies", assignedTo: UUID },
            { principal: "authenticated", role: "canViewOriginals", assignedTo: UUID }
        ];
        const no_inherited_assigned_roles = {
            inherited: { roles: [], deleted: false, embargo: null },
            assigned: {
                roles: assigned_roles,
                embargo: null, deleted: false
            }
        };
        const staff_only_roles = [
            { principal: "everyone", role: "none", assignedTo: null },
            { principal: "authenticated", role: "none", assignedTo: null }
        ];

        stubDataLoad(no_inherited_assigned_roles);

        moxios.wait(() => {
            expect(wrapper.vm.inherited.roles).toEqual(staff_only_roles);
            expect(wrapper.vm.assignedPatronRoles).toEqual(assigned_roles);
            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: 'staff', role: 'none', deleted: false, embargo: false, type: 'inherited', assignedTo: null }
            ]);
            expect(wrapper.vm.submissionAccessDetails().roles).toEqual(assigned_roles);
            done();
        });
    });

    it("sets default roles on load if neither inherited or assigned roles are returned", (done) => {
        const staff_only_roles = [
            { principal: "everyone", role: "none", assignedTo: null },
            { principal: "authenticated", role: "none", assignedTo: null }
        ];
        stubDataLoad(empty_response)

        moxios.wait(() => {
            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: 'staff', role: 'none', deleted: false, embargo: false, type: 'inherited', assignedTo: null }
            ]);
            expect(wrapper.vm.inherited.roles).toEqual(staff_only_roles);
            expect(wrapper.vm.assignedPatronRoles).toEqual([]);
            expect(wrapper.vm.submissionAccessDetails().roles).toEqual([]);
            done();
        });
    });

    it("collection with no assigned roles shows preview of staff only", (done) => {
        wrapper.setProps({
            containerType: 'Collection'
        });

        const response = {
            inherited: { roles: [], deleted: false, embargo: null },
            assigned: { roles: [], deleted: false, embargo: null }
        };

        stubDataLoad(response);

        moxios.wait(() => {
            expect(wrapper.vm.user_type).toEqual('staff');

            let assigned_patrons = wrapper.findAll('.patron-assigned');
            expect(assigned_patrons.length).toEqual(2);
            expect(assigned_patrons.at(0).findAll('p').at(0).text()).toEqual('Public users');
            expect(assigned_patrons.at(0).findAll('select').at(0).element.value).toEqual('none');
            expect(assigned_patrons.at(1).findAll('p').at(0).text()).toEqual('Authenticated users');
            expect(assigned_patrons.at(1).findAll('select').at(0).element.value).toEqual('none');

            expect(wrapper.vm.submissionAccessDetails().roles).toEqual([
                {principal: 'everyone', role: 'none', assignedTo: UUID },
                {principal: 'authenticated', role: 'none', assignedTo: UUID }]);
            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: 'staff', role: 'none', assignedTo: UUID, type: 'assigned', deleted: false, embargo: false }
            ]);
            done();
        })
    });

    it("does not set default inherited display roles for collections and sets assigned permissions to returned roles", (done) => {
        wrapper.setProps({
            containerType: 'Collection'
        });

        const assigned_roles =  [
            { principal: 'everyone', role: 'canViewMetadata', assignedTo: UUID },
            { principal: 'authenticated', role: 'canViewOriginals', assignedTo: UUID }
        ];
        const response = {
            inherited: { roles: null, deleted: false, embargo: null },
            assigned: { roles: assigned_roles, deleted: false, embargo: null }
        };

        stubDataLoad(response);

        moxios.wait(() => {
            expect(wrapper.vm.submissionAccessDetails().roles).toEqual(assigned_roles);
            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: 'everyone', role: 'canViewMetadata', assignedTo: UUID, type: 'assigned', deleted: false, embargo: false },
                { principal: 'authenticated', role: 'canViewOriginals', assignedTo: UUID,type: 'assigned', deleted: false, embargo: false }
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
            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: 'staff', role: 'none', deleted: false, embargo: false, type: 'assigned', assignedTo: UUID }
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

        moxios.wait(async () => {
            expect(wrapper.vm.user_type).toEqual('parent');
            let selected = wrapper.findAll('.patron-assigned');
            expect(selected.length).toEqual(2);
            expect(selected.at(0).findAll('p').at(0).text()).toEqual('Public users');
            expect(selected.at(0).findAll('select').at(0).element.value).toEqual('canViewOriginals');
            expect(selected.at(1).findAll('p').at(0).text()).toEqual('Authenticated users');
            expect(selected.at(1).findAll('select').at(0).element.value).toEqual('canViewOriginals');
            done();
        })
    });

    it("sets permission display to 'staff' and form roles to returned values if item is marked for deletion and roles are returned from the server", (done) => {
        const roleList = [{principal: 'everyone', role: 'canViewMetadata', assignedTo: UUID },
            { principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID }];

        const returned_roles = {
            inherited: {
                roles: [{principal: 'everyone', role: 'canViewMetadata', assignedTo: null},
                    {principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: null}],
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
            expect(wrapper.vm.assignedPatronRoles).toEqual(roleList);
            expect(wrapper.vm.submissionAccessDetails()).toEqual({
                deleted: true,
                embargo: null,
                roles: roleList,
                assignedTo: UUID
            });
            expect(wrapper.vm.displayAssignments).toEqual([{
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

    it("sets a radio button if button or it's text is clicked", (done) => {
        stubDataLoad();

        moxios.wait(async () => {
            wrapper.find('label[for="user_type_parent"]').trigger('click');
            await wrapper.vm.$nextTick();
            expect(wrapper.vm.user_type).toBe('parent');

            wrapper.find('label[for="user_type_patron"]').trigger('click');
            await wrapper.vm.$nextTick();
            expect(wrapper.vm.user_type).toBe('patron');

            wrapper.find('label[for="user_type_staff"]').trigger('click');
            await wrapper.vm.$nextTick();
            expect(wrapper.vm.user_type).toBe('staff');

            wrapper.find('#user_type_parent').trigger('click');
            await wrapper.vm.$nextTick();
            expect(wrapper.vm.user_type).toBe('parent');

            wrapper.find('#user_type_patron').trigger('click');
            await wrapper.vm.$nextTick();
            expect(wrapper.vm.user_type).toBe('patron');

            wrapper.find('#user_type_staff').trigger('click');
            await wrapper.vm.$nextTick();
            expect(wrapper.vm.user_type).toBe('staff');

            done();
        });
    });

    it("disables select boxes if 'Staff or Parent' wrapper text is clicked", (done) => {
        stubDataLoad();

        moxios.wait(async () => {
            expect(selects.at(0).attributes('disabled')).not.toBe('disabled');
            expect(selects.at(1).attributes('disabled')).not.toBe('disabled');

            wrapper.find('label[for="user_type_staff"]').trigger('click');
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

            wrapper.find('label[for="user_type_parent"]').trigger('click');
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

            wrapper.find('#user_type_staff').trigger('click');
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

            wrapper.find('label[for="user_type_parent"]').trigger('click');
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
            expect(wrapper.vm.assignedPatronRoles).toEqual([
                {principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID },
                {principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID }
            ]);

            wrapper.find('label[for="user_type_staff"]').trigger('click');

            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: "staff", role: 'none', type: 'assigned', deleted: false, embargo: false, assignedTo: UUID }
            ]);
            expect(wrapper.vm.submissionAccessDetails().roles).toEqual([
                {principal: 'everyone', role: 'none', assignedTo: UUID },
                {principal: 'authenticated', role: 'none', assignedTo: UUID }
            ]);
            done();
        });
    });

    it("sets patron permissions to 'No Access' if 'Staff only access' radio button is checked", (done) => {
        stubDataLoad(response);

        moxios.wait(() => {
            expect(wrapper.vm.assignedPatronRoles).toEqual([
                {principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID },
                {principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID }
            ]);

            wrapper.find('#user_type_staff').trigger('click');

            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: "staff", role: 'none', type: 'assigned', deleted: false, embargo: false, assignedTo: UUID }
            ]);
            expect(wrapper.vm.submissionAccessDetails().roles).toEqual([
                {principal: 'everyone', role: 'none', assignedTo: UUID },
                {principal: 'authenticated', role: 'none', assignedTo: UUID }
            ]);
            done();
        });
    });

    it("sets form patron permissions to 'CanViewOriginals' if 'Parent' wrapper text is checked", (done) => {
        stubDataLoad(response);

        moxios.wait(() => {
            expect(wrapper.vm.assignedPatronRoles).toEqual([
                {principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID },
                {principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID }
            ]);

            wrapper.find('label[for="user_type_parent"]').trigger('click');

            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: 'patron', role: 'canViewOriginals', type: 'inherited', deleted: false, embargo: false, assignedTo: null }
            ]);
            expect(wrapper.vm.submissionAccessDetails().roles).toEqual([]);
            done();
        });
    });

    it("sets patron form permissions to 'CanViewOriginals' if 'Parent' radio button is checked", (done) => {
        const role = 'canViewOriginals';
        stubDataLoad(response);

        moxios.wait(() => {
            expect(wrapper.vm.assignedPatronRoles).toEqual([
                {principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID },
                {principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID }
            ]);

            wrapper.find('#user_type_parent').trigger('click');

            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: "patron", role: 'canViewOriginals', type: 'inherited', deleted: false, embargo: false, assignedTo: null }
            ]);
            expect(wrapper.vm.submissionAccessDetails().roles).toEqual([]);
            done();
        });
    });

    it("retains selected roles when changing the selected access type", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            wrapper.find('#user_type_staff').trigger('click');
            let selected = wrapper.findAll('.patron-assigned');
            expect(selected.length).toEqual(2);
            expect(selected.at(0).findAll('select').at(0).element.value).toEqual('canViewAccessCopies');
            expect(selected.at(1).findAll('select').at(0).element.value).toEqual('canViewAccessCopies');

            wrapper.find('#user_type_patron').trigger('click');
            selected = wrapper.findAll('.patron-assigned');
            expect(selected.length).toEqual(2);
            expect(selected.at(0).findAll('select').at(0).element.value).toEqual('canViewAccessCopies');
            expect(selected.at(1).findAll('select').at(0).element.value).toEqual('canViewAccessCopies');
            done();
        });
    });

    it("compacts ui permission display if 'winning' permissions are the same and of the same type", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            expect(wrapper.vm.displayAssignments).toEqual(compacted_assigned_can_view_roles);
            done();
        });
    });

    it("does not compact ui permission display if 'winning' permissions are the same and of different types", (done) => {
        const roles = {
            inherited: { roles: [{ principal: 'everyone', role: 'none', assignedTo: null},
                    { principal: 'authenticated', role: 'canViewMetadata', assignedTo: null }
                ], deleted: false, embargo: null },
            assigned: { roles: [
                    { principal: 'everyone', role: 'none', assignedTo: UUID },
                    { principal: 'authenticated', role: "none", assignedTo: UUID }
                ], deleted: false, embargo: null }
        };

        stubDataLoad(roles);

        moxios.wait(() => {
            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: 'everyone', role: 'none', deleted: false, embargo: false, type: 'inherited', assignedTo: null },
                { principal: 'authenticated', role: 'none', deleted: false, embargo: false, type: 'assigned', assignedTo: UUID }
            ]);
            done();
        });
    });

    it("updates permissions for public users", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            expect(wrapper.vm.assignedPatronRoles).toEqual([
                {principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID },
                {principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID }
            ]);

            wrapper.findAll('.patron-assigned option').at(0).setSelected();

            expect(wrapper.vm.assignedPatronRoles).toEqual([
                {principal: 'everyone', role: 'none', assignedTo: UUID },
                {principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID }
            ]);
            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: 'everyone', role: 'none', deleted: false, embargo: false, type: 'assigned', assignedTo: UUID },
                { principal: 'authenticated', role: 'canViewAccessCopies', deleted: false, embargo: false, type: 'assigned', assignedTo: UUID }
            ]);
            expect(wrapper.vm.submissionAccessDetails().roles).toEqual([
                {principal: 'everyone', role: 'none', assignedTo: UUID },
                {principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID }
            ]);
            done();
        });
    });

    it("updates permissions for authenticated users", (done) => {
        stubDataLoad(same_assigned_roles);

        moxios.wait(() => {
            expect(wrapper.vm.assignedPatronRoles).toEqual([
                {principal: 'everyone', role: 'canViewMetadata', assignedTo: UUID },
                {principal: 'authenticated', role: 'canViewMetadata', assignedTo: UUID }
            ]);
            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: 'everyone', role: 'canViewMetadata', deleted: false, embargo: false, type: 'inherited', assignedTo: null },
                { principal: 'authenticated', role: 'canViewMetadata', deleted: false, embargo: false, type: 'assigned', assignedTo: UUID }
            ]);

            wrapper.findAll('.patron-assigned').at(1).findAll('option').at(3).setSelected();

            let updated_authenticated_roles =  [
                { principal: 'everyone', role: 'canViewMetadata', assignedTo: UUID },
                { principal: 'authenticated', role: 'canViewOriginals', assignedTo: UUID }
            ];
            expect(wrapper.vm.assignedPatronRoles).toEqual(updated_authenticated_roles);
            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: 'everyone', role: 'canViewMetadata', deleted: false, embargo: false, type: 'inherited', assignedTo: null },
                { principal: 'authenticated', role: 'canViewOriginals', deleted: false, embargo: false, type: 'inherited', assignedTo: null }
            ]);
            expect(wrapper.vm.submissionAccessDetails().roles).toEqual(updated_authenticated_roles);

            done();
        });
    });

    it("does not update display roles if an embargo is not set from server response", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            expect(wrapper.vm.displayAssignments).toEqual(compacted_assigned_can_view_roles);
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
            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: 'patron', role: 'canViewMetadata', deleted: false, embargo: true, type: 'inherited' }
            ]);
            done();
        })
    });

    it("updates display roles if an embargo is set from the form", (done) => {
        let values = {
            inherited: {
                roles: [
                    {principal: 'everyone', role: 'canViewOriginals', assignedTo: null},
                    {principal: 'authenticated', role: "canViewOriginals", assignedTo: null}
                ], deleted: false, embargo: null
            },
            assigned: {
                roles: [
                    {principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID},
                    {principal: 'authenticated', role: "canViewOriginals", assignedTo: UUID}
                ], deleted: false, embargo: null
            }
        };

        stubDataLoad(values);

        moxios.wait(() => {
            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: 'everyone', role: 'canViewAccessCopies', deleted: false, embargo: false, type: 'assigned', assignedTo: UUID },
                { principal: 'authenticated', role: 'canViewOriginals', deleted: false, embargo: false, type: 'inherited', assignedTo: null }
            ]);

            wrapper.vm.$refs.embargoInfo.$emit('embargo-info', embargo_date);
            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: 'patron', role: 'canViewMetadata', deleted: false, embargo: true, type: 'assigned', assignedTo: UUID }
            ]);
            expect(wrapper.vm.embargo).toEqual(embargo_date);
            let submissionDetails = wrapper.vm.submissionAccessDetails();
            expect(submissionDetails.embargo).toEqual(embargo_date);
            expect(submissionDetails.roles).toEqual([
                { principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID },
                { principal: 'authenticated', role: 'canViewOriginals', assignedTo: UUID },
            ]);
            done();
        });
    });

    it("assigning an embargo does not change displayed roles if already 'canViewMetadata' or 'none'", (done) => {
        stubDataLoad(full_roles);

        moxios.wait(() => {
            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: 'everyone', role: 'none', deleted: false, embargo: false, type: 'assigned', assignedTo: UUID },
                { principal: 'authenticated', role: 'canViewMetadata', deleted: false, embargo: false, type: 'inherited', assignedTo: null }
            ]);

            wrapper.vm.$refs.embargoInfo.$emit('embargo-info', embargo_date);
            expect(wrapper.vm.displayAssignments).toEqual([
                { principal: 'everyone', role: 'none', deleted: false, embargo: true, type: 'assigned', assignedTo: UUID },
                { principal: 'authenticated', role: 'canViewMetadata', deleted: false, embargo: false, type: 'inherited', assignedTo: null }
            ]);
            done();
        })
    });

    it("updates submit roles if an embargo is added or removed", (done) => {
        stubDataLoad(full_roles);

        moxios.wait(() => {
            expect(wrapper.vm.submissionAccessDetails().embargo).toEqual(null);

            wrapper.vm.$refs.embargoInfo.$emit('embargo-info', embargo_date);
            expect(wrapper.vm.submissionAccessDetails().embargo).toEqual(embargo_date);

            wrapper.vm.$refs.embargoInfo.$emit('embargo-info', null);
            expect(wrapper.vm.submissionAccessDetails().embargo).toEqual(null);
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

        moxios.wait(async () => {
            await wrapper.vm.$nextTick();
            expect(wrapper.vm.hasUnsavedChanges).toBe(false);

            wrapper.findAll('option').at(1).setSelected();
            expect(wrapper.vm.hasUnsavedChanges).toBe(true);
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

    function stubDataSaveResponse() {
        moxios.stubRequest(`/services/api/edit/acl/patron/${wrapper.vm.uuid}`, {
            status: 200
        });
    }
});