import {shallowMount, flushPromises} from '@vue/test-utils';
import patronRoles from '@/components/permissions-editor/patronRoles.vue';
import { createTestingPinia } from '@pinia/testing';
import { usePermissionsStore } from '@/stores/permissions';

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

const mockConfirm = vi.fn().mockReturnValue(true);
let wrapper, selects, store;

describe('patronRoles.vue', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
        wrapper = null;
    });

    it("submits updated roles to the server", async () => {
        stubDataLoad();
        await flushPromises();
        await store.setEmbargoInfo({
            embargo: embargo_date,
            skip_embargo: false
        });

        fetchMock.mockResponseOnce(JSON.stringify({ success: true }));
        await wrapper.find('#is-submitting').trigger('click');
        await flushPromises();

        const lastCall = fetchMock.mock.calls[fetchMock.mock.calls.length - 1];
        expect(lastCall[1].method).toEqual('PUT');
        expect(JSON.parse(lastCall[1].body)).toEqual(
            { roles: assigned_roles, deleted: false, embargo: embargo_date, assignedTo: UUID }
        );
        expect(wrapper.vm.hasUnsavedChanges).toBe(false);
    });

    it("unsaved changes flag resets after saving to the server", async () => {
        stubDataLoad(full_roles);
        await flushPromises();

        expect(wrapper.vm.hasUnsavedChanges).toBe(false);

        await wrapper.find('#user_type_staff').trigger('change');
        expect(wrapper.vm.user_type).toEqual('staff');
        expect(wrapper.vm.hasUnsavedChanges).toBe(true);

        fetchMock.mockResponseOnce(JSON.stringify({ success: true }));
        await wrapper.find('#is-submitting').trigger('click');
        await flushPromises();

        expect(wrapper.vm.hasUnsavedChanges).toBe(false);
    });

    it("commits uncommitted patron assignment", async () => {
        const resp_with_allowed_patrons = {
            inherited: { roles: inherited_roles, deleted: false, embargo: null, assignedTo: null },
            assigned: { roles: assigned_roles,  deleted: false, embargo: null, assignedTo: UUID },
            allowedPrincipals: [{ principal: "my:special:group", name: "Special Group" }]
        };
        stubDataLoad(resp_with_allowed_patrons);
        await flushPromises();

        // Click to show the add other principal inputs
        await wrapper.find('#add-principal').trigger('click');
        // Select the existing patron principal to add
        await wrapper.findAll('#add-new-patron-principal-id option')[0].setSelected();
        await wrapper.findAll('#add-new-patron-principal-role option')[5].setSelected();
        await wrapper.find('#add-principal').trigger('click');

        fetchMock.mockResponseOnce(JSON.stringify({ success: true }));
        await wrapper.find('#is-submitting').trigger('click');
        await flushPromises();

        const lastCall = fetchMock.mock.calls[fetchMock.mock.calls.length - 1];
        expect(lastCall[1].method).toEqual('PUT');
        expect(JSON.parse(lastCall[1].body)).toEqual(
            { roles: [{ principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID  },
                    { principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID  },
                    { principal: 'my:special:group', role: 'canViewOriginals', assignedTo: UUID  }],
                deleted: false, embargo: null, assignedTo: UUID }
        );
        expect(wrapper.vm.hasUnsavedChanges).toBe(false);
    });

    it("new assignment remove button hides inputs", async () => {
        const resp_with_allowed_patrons = {
            inherited: { roles: inherited_roles, deleted: false, embargo: null, assignedTo: null },
            assigned: { roles: assigned_roles, deleted: false, embargo: null, assignedTo: UUID },
            allowedPrincipals: [{ principal: "my:special:group", name: "Special Group" }]
        };

        stubDataLoad(resp_with_allowed_patrons);
        await flushPromises();

        expect(wrapper.vm.user_type).toBe('patron');

        // Check v-show via data property instead of isVisible()
        expect(wrapper.vm.should_show_add_principal).toBe(false);

        await wrapper.find('#add-principal').trigger('click');
        expect(wrapper.vm.should_show_add_principal).toBe(true);

        // Check element doesn't have display:none
        const addNewElement = wrapper.find('#add-new-patron-principal');
        const style = addNewElement.attributes('style');
        expect(style).not.toContain('display: none');

        // Set values and test removal
        await wrapper.findAll('#add-new-patron-principal-id option')[0].setSelected();
        await wrapper.findAll('#add-new-patron-principal-role option')[3].setSelected();
        await wrapper.find('#add-new-patron-principal .btn-remove').trigger('click');

        // Verify state changed
        expect(wrapper.vm.should_show_add_principal).toBe(false);
        expect(wrapper.vm.new_assignment_role).toEqual('canViewOriginals');
        expect(wrapper.vm.new_assignment_principal).toEqual('');
    });

    it("retrieves patron roles from the server", async () => {
        stubDataLoad();
        await flushPromises();

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
    });

    it("adds an custom patron group", async () => {
        const resp_with_allowed_patrons = {
            inherited: { roles: inherited_roles, deleted: false, embargo: null, assignedTo: null },
            assigned: { roles: assigned_roles,  deleted: false, embargo: null, assignedTo: UUID },
            allowedPrincipals: [{ principal: "my:special:group", name: "Special Group" }]
        };
        stubDataLoad(resp_with_allowed_patrons);
        await flushPromises();

        expect(wrapper.vm.assignedPatronRoles).toEqual(resp_with_allowed_patrons.assigned.roles);

        // Click to show the add other principal inputs
        await wrapper.find('#add-principal').trigger('click');
        // Select values for new patron role and then click the add button again
        await wrapper.findAll('#add-new-patron-principal-id option')[0].setSelected();
        await wrapper.findAll('#add-new-patron-principal-role option')[5].setSelected();
        await wrapper.find('#add-principal').trigger('click');

        // Model should have updated by adding the new role to the list of assigned roles
        const assigned_other_roles = [{ principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID  },
            { principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID  },
            { principal: 'my:special:group', role: 'canViewOriginals', assignedTo: UUID  }];
        expect(wrapper.vm.assignedPatronRoles).toEqual(assigned_other_roles);

        let assigned_patrons = wrapper.findAll('.patron-assigned');
        expect(assigned_patrons.length).toEqual(3);
        expect(assigned_patrons[0].findAll('p')[0].text()).toEqual('Public users');
        expect(assigned_patrons[0].findAll('select')[0].element.value).toEqual('canViewAccessCopies');
        expect(assigned_patrons[1].findAll('p')[0].text()).toEqual('Authenticated users');
        expect(assigned_patrons[1].findAll('select')[0].element.value).toEqual('canViewAccessCopies');
        expect(assigned_patrons[2].findAll('p')[0].text()).toEqual('Special Group');
        expect(assigned_patrons[2].findAll('select')[0].element.value).toEqual('canViewOriginals');

        // The new entry inputs should be cleared
        expect(wrapper.vm.new_assignment_principal).toEqual('');
        expect(wrapper.vm.new_assignment_role).toEqual('canViewOriginals');

        // Added group not active since no inherited role for it
        expect(wrapper.vm.displayAssignments).toEqual([
            { principal: 'everyone', role: 'canViewAccessCopies', type: 'assigned', assignedTo: UUID, deleted: false, embargo: false },
            { principal: 'authenticated', role: 'canViewAccessCopies', type: 'assigned', assignedTo: UUID, deleted: false, embargo: false },
            { principal: 'my:special:group', role: 'none', type: 'inherited', assignedTo: null, deleted: false, embargo: false }
        ]);
    });

    it("can only add non-duplicate principals", async () => {
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
        await flushPromises();

        expect(wrapper.vm.assignedPatronRoles).toEqual(assigned_other_roles);

        // Click to show the add other principal inputs
        await wrapper.find('#add-principal').trigger('click');
        // Select the existing patron principal to add
        await wrapper.findAll('#add-new-patron-principal-id option')[0].setSelected();
        await wrapper.findAll('#add-new-patron-principal-role option')[3].setSelected();
        await wrapper.find('#add-principal').trigger('click');

        // Try adding principal that has not already been assigned
        await wrapper.findAll('#add-new-patron-principal-id option')[1].setSelected();
        await wrapper.find('#add-principal').trigger('click');

        expect(wrapper.vm.assignedPatronRoles).toEqual([assigned_other_roles[0], assigned_other_roles[1], assigned_other_roles[2],
            { principal: 'the:extra:special:group', role: 'canViewAccessCopies', assignedTo: UUID }]);

        let assigned_patrons = wrapper.findAll('.patron-assigned');
        expect(assigned_patrons.length).toEqual(4);
        expect(assigned_patrons[0].findAll('p')[0].text()).toEqual('Public users');
        expect(assigned_patrons[0].findAll('select')[0].element.value).toEqual('canViewAccessCopies');
        expect(assigned_patrons[1].findAll('p')[0].text()).toEqual('Authenticated users');
        expect(assigned_patrons[1].findAll('select')[0].element.value).toEqual('canViewAccessCopies');
        expect(assigned_patrons[2].findAll('p')[0].text()).toEqual('Special Group');
        expect(assigned_patrons[2].findAll('select')[0].element.value).toEqual('canViewOriginals');
        expect(assigned_patrons[3].findAll('p')[0].text()).toEqual('Extra Special Group');
        expect(assigned_patrons[3].findAll('select')[0].element.value).toEqual('canViewAccessCopies');
    });

    it("removes other patron principals", async () => {
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
        await flushPromises();

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
        await wrapper.findAll("#assigned_principals_editor .btn-remove")[1].trigger('click');
        expect(wrapper.vm.assignedPatronRoles.length).toEqual(3);
        expect(wrapper.vm.assignedPatronRoles).toEqual(
            [assigned_other_roles[0], assigned_other_roles[1], assigned_other_roles[2]]);

        let assigned_patrons = wrapper.findAll('.patron-assigned');
        expect(assigned_patrons.length).toEqual(3);
        expect(assigned_patrons[0].findAll('p')[0].text()).toEqual('Public users');
        expect(assigned_patrons[0].findAll('select')[0].element.value).toEqual('canViewMetadata');
        expect(assigned_patrons[1].findAll('p')[0].text()).toEqual('Authenticated users');
        expect(assigned_patrons[1].findAll('select')[0].element.value).toEqual('canViewMetadata');
        expect(assigned_patrons[2].findAll('p')[0].text()).toEqual('Another Group');
        expect(assigned_patrons[2].findAll('select')[0].element.value).toEqual('canViewAccessCopies');

        expect(wrapper.vm.displayAssignments).toEqual([
            { principal: 'everyone', role: 'canViewMetadata', type: 'assigned', assignedTo: UUID, deleted: false, embargo: false },
            { principal: 'authenticated', role: 'canViewMetadata', type: 'assigned', assignedTo: UUID, deleted: false, embargo: false },
            { principal: 'less:special:group', role: 'canViewAccessCopies', type: 'assigned', assignedTo: UUID, deleted: false, embargo: false },
            { principal: 'my:special:group', role: 'canViewOriginals', type: 'inherited', assignedTo: null, deleted: false, embargo: false }
        ]);

        expect(wrapper.vm.submissionAccessDetails().roles).toEqual(
            [assigned_other_roles[0], assigned_other_roles[1], assigned_other_roles[2]]);
    });

    it("other patron principals assigned to collection", async () => {
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

        stubDataLoad(resp_with_allowed_patrons);
        store.setMetadata({ type: 'Collection', id: UUID });

        await flushPromises();

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
        expect(assigned_patrons[0].findAll('p')[0].text()).toEqual('Public users');
        expect(assigned_patrons[0].findAll('select')[0].element.value).toEqual('canViewMetadata');
        expect(assigned_patrons[1].findAll('p')[0].text()).toEqual('Authenticated users');
        expect(assigned_patrons[1].findAll('select')[0].element.value).toEqual('canViewMetadata');
        expect(assigned_patrons[2].findAll('p')[0].text()).toEqual('Another Group');
        expect(assigned_patrons[2].findAll('select')[0].element.value).toEqual('canViewAccessCopies');
        expect(assigned_patrons[3].findAll('p')[0].text()).toEqual('Special Group');
        expect(assigned_patrons[3].findAll('select')[0].element.value).toEqual('canViewOriginals');
    });

    it("inherited principals not present on object are displayed by default", async () => {
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
        await flushPromises();

        expect(wrapper.vm.assignedPatronRoles).toEqual(expected_assigned);

        await wrapper.find('#user_type_parent').trigger('change');
        expect(wrapper.vm.assignedPatronRoles).toEqual([]);

        let assigned_patrons = wrapper.findAll('.patron-assigned');
        expect(assigned_patrons.length).toEqual(3);
        expect(assigned_patrons[0].findAll('p')[0].text()).toEqual('Public users');
        expect(assigned_patrons[0].findAll('select')[0].element.value).toEqual('canViewAccessCopies');
        expect(assigned_patrons[1].findAll('p')[0].text()).toEqual('Authenticated users');
        expect(assigned_patrons[1].findAll('select')[0].element.value).toEqual('canViewAccessCopies');
        expect(assigned_patrons[2].findAll('p')[0].text()).toEqual('Special Group');
        expect(assigned_patrons[2].findAll('select')[0].element.value).toEqual('canViewOriginals');

        await wrapper.find('#user_type_patron').trigger('change');
        expect(wrapper.vm.assignedPatronRoles).toEqual(expected_assigned);
    });

    it("sets default roles if no inherited roles are returned", async () => {
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
        await flushPromises();

        expect(wrapper.vm.inherited.roles).toEqual(staff_only_roles);
        expect(wrapper.vm.assignedPatronRoles).toEqual(assigned_roles);
        expect(wrapper.vm.displayAssignments).toEqual([
            { principal: 'staff', role: 'none', deleted: false, embargo: false, type: 'inherited', assignedTo: null }
        ]);
        expect(wrapper.vm.submissionAccessDetails().roles).toEqual(assigned_roles);
    });

    it("sets default roles on load if neither inherited or assigned roles are returned", async () => {
        const staff_only_roles = [
            { principal: "everyone", role: "none", assignedTo: null },
            { principal: "authenticated", role: "none", assignedTo: null }
        ];
        stubDataLoad(empty_response);
        await flushPromises();

        expect(wrapper.vm.displayAssignments).toEqual([
            { principal: 'staff', role: 'none', deleted: false, embargo: false, type: 'inherited', assignedTo: null }
        ]);
        expect(wrapper.vm.inherited.roles).toEqual(staff_only_roles);
        expect(wrapper.vm.assignedPatronRoles).toEqual([]);
        expect(wrapper.vm.submissionAccessDetails().roles).toEqual([]);
    });

    it("collection with no assigned roles shows preview of staff only", async () => {
        const response = {
            inherited: { roles: [], deleted: false, embargo: null },
            assigned: { roles: [], deleted: false, embargo: null }
        };

        stubDataLoad(response);
        store.setMetadata({ type: 'Collection', id: UUID });
        await flushPromises();

        expect(wrapper.vm.user_type).toEqual('staff');

        let assigned_patrons = wrapper.findAll('.patron-assigned');
        expect(assigned_patrons.length).toEqual(2);
        expect(assigned_patrons[0].findAll('p')[0].text()).toEqual('Public users');
        expect(assigned_patrons[0].findAll('select')[0].element.value).toEqual('none');
        expect(assigned_patrons[1].findAll('p')[0].text()).toEqual('Authenticated users');
        expect(assigned_patrons[1].findAll('select')[0].element.value).toEqual('none');

        expect(wrapper.vm.submissionAccessDetails().roles).toEqual([
            {principal: 'everyone', role: 'none', assignedTo: UUID },
            {principal: 'authenticated', role: 'none', assignedTo: UUID }]);
        expect(wrapper.vm.displayAssignments).toEqual([
            { principal: 'staff', role: 'none', assignedTo: UUID, type: 'assigned', deleted: false, embargo: false }
        ]);
    });

    it("does not set default inherited display roles for collections and sets assigned permissions to returned roles", async () => {
        const assigned_roles =  [
            { principal: 'everyone', role: 'canViewMetadata', assignedTo: UUID },
            { principal: 'authenticated', role: 'canViewOriginals', assignedTo: UUID }
        ];
        const response = {
            inherited: { roles: null, deleted: false, embargo: null },
            assigned: { roles: assigned_roles, deleted: false, embargo: null }
        };

        stubDataLoad(response);
        store.setMetadata({ type: 'Collection', id: UUID });
        await flushPromises();

        expect(wrapper.vm.submissionAccessDetails().roles).toEqual(assigned_roles);
        expect(wrapper.vm.displayAssignments).toEqual([
            { principal: 'everyone', role: 'canViewMetadata', assignedTo: UUID, type: 'assigned', deleted: false, embargo: false },
            { principal: 'authenticated', role: 'canViewOriginals', assignedTo: UUID,type: 'assigned', deleted: false, embargo: false }
        ]);
    });

    it("sets display 'staff' if assigned everyone and authenticated principals both have the 'none' and inherited principals have the same role", async () => {
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
        await flushPromises();

        expect(wrapper.vm.displayAssignments).toEqual([
            { principal: 'staff', role: 'none', deleted: false, embargo: false, type: 'assigned', assignedTo: UUID }
        ]);
    });

    it("sets form user type to 'staff' on form load if public and authenticated principals both have the 'none' role", async () => {
        stubDataLoad(none_response);
        await flushPromises();

        expect(wrapper.vm.user_type).toEqual('staff');
    });

    it("sets form user type to 'parent' and form roles to 'canViewOriginals' if no assigned roles returned from the server", async () => {
        const no_roles = {
            inherited: { roles: [], deleted: false, embargo: null },
            assigned: { roles: [], deleted: true, embargo: null }
        };

        stubDataLoad(no_roles);
        await flushPromises();

        expect(wrapper.vm.user_type).toEqual('parent');
        let selected = wrapper.findAll('.patron-assigned');
        expect(selected.length).toEqual(2);
        expect(selected[0].findAll('p')[0].text()).toEqual('Public users');
        expect(selected[0].findAll('select')[0].element.value).toEqual('canViewOriginals');
        expect(selected[1].findAll('p')[0].text()).toEqual('Authenticated users');
        expect(selected[1].findAll('select')[0].element.value).toEqual('canViewOriginals');
    });

    it("sets form user type to 'parent' and form roles to 'canViewOriginals' if no assigned roles returned from the server", async () => {
        const no_roles = {
            inherited: { roles: [], deleted: false, embargo: null },
            assigned: { roles: [], deleted: true, embargo: null }
        };

        stubDataLoad(no_roles);
        await flushPromises();

        expect(wrapper.vm.user_type).toEqual('parent');
        let selected = wrapper.findAll('.patron-assigned');
        expect(selected.length).toEqual(2);
        expect(selected[0].findAll('p')[0].text()).toEqual('Public users');
        expect(selected[0].findAll('select')[0].element.value).toEqual('canViewOriginals');
        expect(selected[1].findAll('p')[0].text()).toEqual('Authenticated users');
        expect(selected[1].findAll('select')[0].element.value).toEqual('canViewOriginals');
    });


    it("sets permission display to 'staff' and form roles to returned values if item is marked for deletion and roles are returned from the server", async () => {
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
        await flushPromises();

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
        }]);
    });

    it("sets a radio button if button or it's text is clicked", async () => {
        stubDataLoad();
        await flushPromises();

        await wrapper.find('label[for="user_type_parent"]').trigger('click');
        expect(wrapper.vm.user_type).toBe('parent');

        await wrapper.find('label[for="user_type_patron"]').trigger('click');
        expect(wrapper.vm.user_type).toBe('patron');

        await wrapper.find('label[for="user_type_staff"]').trigger('click');
        expect(wrapper.vm.user_type).toBe('staff');

        await wrapper.find('#user_type_parent').trigger('change');
        expect(wrapper.vm.user_type).toBe('parent');

        await wrapper.find('#user_type_patron').trigger('change');
        expect(wrapper.vm.user_type).toBe('patron');

        await wrapper.find('#user_type_staff').trigger('change');
        expect(wrapper.vm.user_type).toBe('staff');
    });

    it("disables select boxes if 'Staff or Parent' wrapper text is clicked", async () => {
        stubDataLoad();
        await flushPromises();

        expect(selects[0].attributes()).not.toHaveProperty('disabled');
        expect(selects[1].attributes()).not.toHaveProperty('disabled');

        await wrapper.find('label[for="user_type_staff"]').trigger('click');
        expect(wrapper.vm.user_type).toEqual('staff');
        let updated_selects = wrapper.findAll('select');
        expect(updated_selects[0].attributes()).toHaveProperty('disabled');
        expect(updated_selects[1].attributes()).toHaveProperty('disabled');
    });

    it("disables select boxes if 'Parent' wrapper text is clicked", async () => {
        stubDataLoad();
        await flushPromises();

        expect(selects[0].attributes()).not.toHaveProperty('disabled');
        expect(selects[1].attributes()).not.toHaveProperty('disabled');

        await wrapper.find('label[for="user_type_parent"]').trigger('click');
        expect(wrapper.vm.user_type).toEqual('parent');
        let updated_selects = wrapper.findAll('select');
        expect(updated_selects[0].attributes()).toHaveProperty('disabled');
        expect(updated_selects[1].attributes()).toHaveProperty('disabled');
    });

    it("disables select boxes if 'Staff only access' radio button is checked", async () => {
        stubDataLoad();
        await flushPromises();

        expect(selects[0].attributes()).not.toHaveProperty('disabled');
        expect(selects[1].attributes()).not.toHaveProperty('disabled');

        await wrapper.find('#user_type_staff').trigger('change');
        expect(wrapper.vm.user_type).toEqual('staff');
        let updated_selects = wrapper.findAll('select');
        expect(updated_selects[0].attributes()).toHaveProperty('disabled');
        expect(updated_selects[1].attributes()).toHaveProperty('disabled');
    });


    it("disables select boxes if 'Parent' radio button is checked", async () => {
        stubDataLoad();
        await flushPromises();

        expect(selects[0].attributes()).not.toHaveProperty('disabled');
        expect(selects[1].attributes()).not.toHaveProperty('disabled');

        await wrapper.find('label[for="user_type_parent"]').trigger('click');
        expect(wrapper.vm.user_type).toEqual('parent');
        let updated_selects = wrapper.findAll('select');
        expect(updated_selects[0].attributes()).toHaveProperty('disabled');
        expect(updated_selects[1].attributes()).toHaveProperty('disabled');
    });

    it("sets patron permissions to 'No Access' if 'Staff only access' wrapper text is clicked", async () => {
        stubDataLoad(response);
        await flushPromises();

        expect(wrapper.vm.assignedPatronRoles).toEqual([
            {principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID },
            {principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID }
        ]);

        await wrapper.find('label[for="user_type_staff"]').trigger('click');

        expect(wrapper.vm.displayAssignments).toEqual([
            { principal: "staff", role: 'none', type: 'assigned', deleted: false, embargo: false, assignedTo: UUID }
        ]);
        expect(wrapper.vm.submissionAccessDetails().roles).toEqual([
            {principal: 'everyone', role: 'none', assignedTo: UUID },
            {principal: 'authenticated', role: 'none', assignedTo: UUID }
        ]);
    });

    it("sets patron permissions to 'No Access' if 'Staff only access' radio button is checked", async () => {
        stubDataLoad(response);
        await flushPromises();

        expect(wrapper.vm.assignedPatronRoles).toEqual([
            {principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID },
            {principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID }
        ]);

        await wrapper.find('#user_type_staff').trigger('change');

        expect(wrapper.vm.displayAssignments).toEqual([
            { principal: "staff", role: 'none', type: 'assigned', deleted: false, embargo: false, assignedTo: UUID }
        ]);
        expect(wrapper.vm.submissionAccessDetails().roles).toEqual([
            {principal: 'everyone', role: 'none', assignedTo: UUID },
            {principal: 'authenticated', role: 'none', assignedTo: UUID }
        ]);
    });

    it("sets form patron permissions to 'CanViewOriginals' if 'Parent' wrapper text is checked", async () => {
        stubDataLoad(response);
        await flushPromises();

        expect(wrapper.vm.assignedPatronRoles).toEqual([
            {principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID },
            {principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID }
        ]);

        await wrapper.find('label[for="user_type_parent"]').trigger('click');

        expect(wrapper.vm.displayAssignments).toEqual([
            { principal: 'patron', role: 'canViewOriginals', type: 'inherited', deleted: false, embargo: false, assignedTo: null }
        ]);
        expect(wrapper.vm.submissionAccessDetails().roles).toEqual([]);
    });

    it("sets patron form permissions to 'CanViewOriginals' if 'Parent' radio button is checked", async () => {
        const role = 'canViewOriginals';
        stubDataLoad(response);
        await flushPromises();

        expect(wrapper.vm.assignedPatronRoles).toEqual([
            {principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID },
            {principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID }
        ]);

        await wrapper.find('#user_type_parent').trigger('change');

        expect(wrapper.vm.displayAssignments).toEqual([
            { principal: "patron", role: 'canViewOriginals', type: 'inherited', deleted: false, embargo: false, assignedTo: null }
        ]);
        expect(wrapper.vm.submissionAccessDetails().roles).toEqual([]);
    });

    it("retains selected roles when changing the selected access type", async () => {
        stubDataLoad();
        await flushPromises();

        await wrapper.find('#user_type_staff').trigger('click');
        let selected = wrapper.findAll('.patron-assigned');
        expect(selected.length).toEqual(2);
        expect(selected[0].findAll('select')[0].element.value).toEqual('canViewAccessCopies');
        expect(selected[1].findAll('select')[0].element.value).toEqual('canViewAccessCopies');

        await wrapper.find('#user_type_patron').trigger('click');
        selected = wrapper.findAll('.patron-assigned');
        expect(selected.length).toEqual(2);
        expect(selected[0].findAll('select')[0].element.value).toEqual('canViewAccessCopies');
        expect(selected[1].findAll('select')[0].element.value).toEqual('canViewAccessCopies');
    });

    it("compacts ui permission display if 'winning' permissions are the same and of the same type", async () => {
        stubDataLoad();
        await flushPromises();

        expect(wrapper.vm.displayAssignments).toEqual(compacted_assigned_can_view_roles);
    });

    it("does not compact ui permission display if 'winning' permissions are the same and of different types", async () => {
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
        await flushPromises();

        expect(wrapper.vm.displayAssignments).toEqual([
            { principal: 'everyone', role: 'none', deleted: false, embargo: false, type: 'inherited', assignedTo: null },
            { principal: 'authenticated', role: 'none', deleted: false, embargo: false, type: 'assigned', assignedTo: UUID }
        ]);
    });

    it("updates permissions for public users", async () => {
        stubDataLoad();
        await flushPromises();

        expect(wrapper.vm.assignedPatronRoles).toEqual([
            {principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID },
            {principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID }
        ]);

        await wrapper.findAll('.patron-assigned option')[0].setSelected();

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
    });

    it("updates permissions for authenticated users", async () => {
        stubDataLoad(same_assigned_roles);
        await flushPromises();

        expect(wrapper.vm.assignedPatronRoles).toEqual([
            {principal: 'everyone', role: 'canViewMetadata', assignedTo: UUID },
            {principal: 'authenticated', role: 'canViewMetadata', assignedTo: UUID }
        ]);
        expect(wrapper.vm.displayAssignments).toEqual([
            { principal: 'everyone', role: 'canViewMetadata', deleted: false, embargo: false, type: 'inherited', assignedTo: null },
            { principal: 'authenticated', role: 'canViewMetadata', deleted: false, embargo: false, type: 'assigned', assignedTo: UUID }
        ]);

        await wrapper.findAll('.patron-assigned')[1].findAll('option')[4].setSelected();

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
    });


    it("updates permissions for public and auth users with canViewReducedQuality", async () => {
        stubDataLoad();
        await flushPromises();

        expect(wrapper.vm.assignedPatronRoles).toEqual([
            {principal: 'everyone', role: 'canViewAccessCopies', assignedTo: UUID },
            {principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: UUID }
        ]);

        await wrapper.findAll('.patron-assigned option')[3].setSelected();
        await wrapper.findAll('.patron-assigned')[1].findAll('option')[3].setSelected();

        expect(wrapper.vm.assignedPatronRoles).toEqual([
            {principal: 'everyone', role: 'canViewReducedQuality', assignedTo: UUID },
            {principal: 'authenticated', role: 'canViewReducedQuality', assignedTo: UUID }
        ]);
        expect(wrapper.vm.displayAssignments).toEqual([
            { principal: 'patron', role: 'canViewReducedQuality', deleted: false, embargo: false, type: 'assigned', assignedTo: UUID }
        ]);
        expect(wrapper.vm.submissionAccessDetails().roles).toEqual([
            {principal: 'everyone', role: 'canViewReducedQuality', assignedTo: UUID },
            {principal: 'authenticated', role: 'canViewReducedQuality', assignedTo: UUID }
        ]);
    });

    it("does not update display roles if an embargo is not set from server response", async () => {
        stubDataLoad();
        await flushPromises();

        expect(wrapper.vm.displayAssignments).toEqual(compacted_assigned_can_view_roles);
    });

    it("updates display roles if an embargo is set from server response on the object", async () => {
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
        await flushPromises();

        expect(wrapper.vm.displayAssignments).toEqual([
            { principal: 'patron', role: 'canViewMetadata', deleted: false, embargo: true, type: 'inherited' }
        ]);
    });

    it("updates display roles if an embargo is set from the form", async () => {
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
        await flushPromises();

        expect(wrapper.vm.displayAssignments).toEqual([
            { principal: 'everyone', role: 'canViewAccessCopies', deleted: false, embargo: false, type: 'assigned', assignedTo: UUID },
            { principal: 'authenticated', role: 'canViewOriginals', deleted: false, embargo: false, type: 'inherited', assignedTo: null }
        ]);

        await store.setEmbargoInfo({ embargo: embargo_date, skip_embargo: false });
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
    });

    it("assigning an embargo does not change displayed roles if already 'canViewMetadata' or 'none'", async () => {
        stubDataLoad(full_roles);
        await flushPromises();

        expect(wrapper.vm.displayAssignments).toEqual([
            { principal: 'everyone', role: 'none', deleted: false, embargo: false, type: 'assigned', assignedTo: UUID },
            { principal: 'authenticated', role: 'canViewMetadata', deleted: false, embargo: false, type: 'inherited', assignedTo: null }
        ]);

        await store.setEmbargoInfo(embargo_date);
        expect(wrapper.vm.displayAssignments).toEqual([
            { principal: 'everyone', role: 'none', deleted: false, embargo: true, type: 'assigned', assignedTo: UUID },
            { principal: 'authenticated', role: 'canViewMetadata', deleted: false, embargo: false, type: 'inherited', assignedTo: null }
        ]);
    });

    it("updates submit roles if an embargo is added or removed", async () => {
        stubDataLoad(full_roles);
        await flushPromises();

        expect(wrapper.vm.submissionAccessDetails().embargo).toEqual(null);

        await store.setEmbargoInfo({ embargo: embargo_date, skip_embargo: false });
        expect(wrapper.vm.submissionAccessDetails().embargo).toEqual(embargo_date);

        await store.setEmbargoInfo({ embargo: null, skip_embargo: false });
        expect(wrapper.vm.submissionAccessDetails().embargo).toEqual(null);
    });

    it("disables 'submit' by default", () => {
        mountApp();
        expectSaveButtonDisabled();
    });

    it("enables 'submit' button if user/role has been added or changed", async () => {
        stubDataLoad();
        await flushPromises();
        await wrapper.findAll('option')[1].setSelected();
        expectSaveButtonDisabled(false);
    });

    it("checks for unsaved changes", async () => {
        stubDataLoad();
        await flushPromises();

        expect(wrapper.vm.hasUnsavedChanges).toBe(false);

        await wrapper.findAll('option')[1].setSelected();
        expect(wrapper.vm.hasUnsavedChanges).toBe(true);
    });

    it("prompts the user if 'Cancel' is clicked and saved and unsaved changes arrays aren't the same size", async () => {
        mountApp();
        await wrapper.setData({
            saved_details: {
                roles: []
            },
            selected_patron_assignments: [{
                assignedTo: '1234',
                principal: 'everyone',
                role: 'canViewAccessCopies'
            }, {
                assignedTo: '1234',
                principal: 'authenticated',
                role: 'canViewOriginals'
            }]
        });

        await wrapper.find('#is-canceling').trigger('click');
        expect(mockConfirm).toHaveBeenCalled();
    });

    it("prompts the user if 'Cancel' is clicked and saved and unsaved changes arrays don't have the same values", async () => {
        mountApp();
        await wrapper.setData({
            saved_details: {
                roles: [{
                    assignedTo: '1234',
                    principal: 'everyone',
                    role: 'canViewAccessCopies'
                }, {
                    assignedTo: '1234',
                    principal: 'authenticated',
                    role: 'canViewAccessCopies'
                }]
            },
            selected_patron_assignments: [{
                    assignedTo: '1234',
                    principal: 'everyone',
                    role: 'canViewAccessCopies'
                }, {
                    assignedTo: '1234',
                    principal: 'authenticated',
                    role: 'canViewOriginals'
                }]
        });

        await wrapper.find('#is-canceling').trigger('click');
        expect(mockConfirm).toHaveBeenCalled();
    });

    it("prompts the user if 'Cancel' is clicked and an embargo has been added", async() => {
        mountApp();
        wrapper.setData({
            saved_details: {
                embargo: null
            },
            embargo: '2099-12-31'
        });
        await wrapper.find('#is-canceling').trigger('click');
        expect(mockConfirm).toHaveBeenCalled();
    });

    it("prompts the user if 'Cancel' is clicked and an embargo has been removed", async() => {
        mountApp();
        await wrapper.setData({
            saved_details: {
                embargo: '2099-12-31'
            },
            embargo: null
        });
        await wrapper.find('#is-canceling').trigger('click');
        expect(mockConfirm).toHaveBeenCalled();
    });

    it("prompts the user if 'Cancel' is clicked and a new user has been added", async() => {
        mountApp();
        wrapper.setData({
            saved_details: {
                embargo: null
            },
            new_assignment_principal: 'bigGroup',
            new_assignment_role: 'canViewOriginals'
        });
        await wrapper.find('#is-canceling').trigger('click');
        expect(mockConfirm).toHaveBeenCalled();
    });

    it("Updates 'cancel' button text if there are unsaved changes", async () => {
        mountApp();
        await wrapper.setData({
            saved_details: null
        });
        expect(wrapper.find('#is-canceling').text()).toBe('Close');

        await wrapper.setData({
            saved_details:  {
                roles: [
                    { principal: 'everyone', role: 'none', assignedTo: UUID  },
                    { principal: 'authenticated', role: 'none', assignedTo: UUID  }
                ],
                deleted: false,
                embargo: null,
                assignedTo: '1234'
            }
        });
        expect(wrapper.find('#is-canceling').text()).toBe('Cancel');
    })

    it("Save button to disabled when no changes for bulk update", async () => {
        stubAllowedPrincipals([]);
        mountApp(true, resultObjectsTwoFolders);

        await flushPromises();

        expect(wrapper.find('.inherited-permissions').exists()).toBe(false);
        expect(wrapper.vm.user_type).toEqual('ignore');
        expect(wrapper.vm.assignedPatronRoles).toEqual([]);
        expect(wrapper.vm.displayAssignments).toEqual([]);
        expect(wrapper.vm.submissionAccessDetails().roles).toEqual([]);
        expectSaveButtonDisabled();

        await store.setEmbargoInfo({embargo: embargo_date, skipEmbargo: false});
        expectSaveButtonDisabled(false);

        await store.setEmbargoInfo({embargo: null, skipEmbargo: true});
        expectSaveButtonDisabled();

        await wrapper.find('#user_type_staff').trigger('change');
        expectSaveButtonDisabled(false);

        await wrapper.find('#user_type_ignore').trigger('change');
        expectSaveButtonDisabled();
    });

    it("User type changes to staff only during bulk update", async () => {
        stubAllowedPrincipals([]);
        mountApp(true, resultObjectsTwoFolders);

        await flushPromises();

        await wrapper.find('#user_type_staff').trigger('change');

        expect(wrapper.vm.user_type).toEqual('staff');
        expect(wrapper.vm.assignedPatronRoles).toEqual([
            {principal: 'everyone', role: 'none', assignedTo: null },
            {principal: 'authenticated', role: 'none', assignedTo: null }
        ]);
        expect(wrapper.vm.displayAssignments).toEqual([]);
        expect(wrapper.vm.submissionAccessDetails().roles).toEqual([
            {principal: 'everyone', role: 'none', assignedTo: null },
            {principal: 'authenticated', role: 'none', assignedTo: null }
        ]);
    });

    it("Can submit custom groups during bulk update", async () => {
        stubAllowedPrincipals([{principal: "my:special:group", name: "Special Group"}]);
        mountApp(true, resultObjectsTwoFolders);
        await flushPromises();

        await wrapper.find('#user_type_patron').trigger('change');
        await wrapper.findAll('.patron-assigned')[0].findAll('option')[1].setSelected();
        await wrapper.findAll('.patron-assigned')[1].findAll('option')[2].setSelected();

        // Click to show the add other principal inputs
        await wrapper.find('#add-principal').trigger('click');
        // Select values for new patron role and then click the add button again
        await wrapper.findAll('#add-new-patron-principal-id option')[0].setSelected();
        await wrapper.findAll('#add-new-patron-principal-role option')[5].setSelected();
        await wrapper.find('#add-principal').trigger('click');

        fetchMock.mockResponseOnce(JSON.stringify({ success: true }), { status: 200 });
        await wrapper.find('#is-submitting').trigger('click');

        await flushPromises();

        // Get all PUT calls, then take the last one
        // Find the PUT request to the bulk endpoint
        const putCalls = fetchMock.mock.calls.filter(call =>
            call[0].includes('/services/api/edit/acl/patron') &&
            !call[0].includes('allowedPrincipals') &&
            call[1]?.method === 'PUT'
        );

        const putCall = putCalls[putCalls.length - 1]; // Get the last one

        expect(putCall).toBeDefined();
        expect(putCall[1].method).toEqual('PUT');
        expect(JSON.parse(putCall[1].body)).toEqual({
            ids: ["73bc003c-9603-4cd9-8a65-93a22520ef6a", "0dfda46a-7812-44e9-8ad3-056b493622e7"],
            accessDetails: {
                roles: [
                    {principal: 'everyone', role: 'canViewMetadata', assignedTo: null},
                    {principal: 'authenticated', role: 'canViewAccessCopies', assignedTo: null},
                    {principal: 'my:special:group', role: 'canViewOriginals', assignedTo: null}],
                deleted: false,
                embargo: null,
                assignedTo: null
            },
            skipEmbargo: true,
            skipRoles: false
        });
        expect(mockConfirm).toHaveBeenCalled();
        expectSaveButtonDisabled();
    });

    it("Bulk update submits added embargo", async () => {
        stubAllowedPrincipals([]);
        mountApp(true, resultObjectsTwoFolders);

        await flushPromises();
        await store.setEmbargoInfo({ embargo: embargo_date, skipEmbargo: false });


        // Store the count before submitting
        const callCountBefore = fetchMock.mock.calls.length;

        fetchMock.mockResponseOnce(JSON.stringify({ success: true }), { status: 200 });

        await wrapper.find('#is-submitting').trigger('click');
        await flushPromises();

        // Get all PUT calls after the button click
        const putCalls = fetchMock.mock.calls
            .slice(callCountBefore) // Only look at calls after the submit
            .filter(call =>
                call[0].includes('/services/api/edit/acl/patron') &&
                !call[0].includes('allowedPrincipals') &&
                call[1]?.method === 'PUT'
            );

        expect(putCalls.length).toBe(1);
        const putCall = putCalls[0];

        expect(putCall[1].method).toEqual('PUT');
        expect(JSON.parse(putCall[1].body)).toEqual({
            ids: ["73bc003c-9603-4cd9-8a65-93a22520ef6a", "0dfda46a-7812-44e9-8ad3-056b493622e7"],
            accessDetails: {
                roles: [],
                deleted: false,
                embargo: embargo_date,
                assignedTo: null
            },
            skipEmbargo: false,
            skipRoles: true
        });
        expect(mockConfirm).toHaveBeenCalled();
        expectSaveButtonDisabled();

        let btn = wrapper.find('#is-canceling');
        expect(btn.text()).toBe('Close');
    });

    it("Enables 'save' by default in bulk mode", async () => {
        stubAllowedPrincipals([]);
        mountApp(true, resultObjectsTwoFolders);
        await flushPromises();
        expectSaveButtonDisabled();
    });

    const resultObjectsTwoFolders = [
        {
            pid: "73bc003c-9603-4cd9-8a65-93a22520ef6a",
            metadata: {
                title: "Folder 1",
                type: "Folder"
            }
        },
        {
            pid: "0dfda46a-7812-44e9-8ad3-056b493622e7",
            metadata: {
                title: "Folder 2",
                type: "Folder"
            }
        }
    ];

    function mountApp(use_bulk = false, resultObjects = []) {
        let initial_permissions = {
            actionHandler: { addEvent: vi.fn() },
            alertHandler: { alertHandler: vi.fn() },
            metadata: { id: UUID, type: 'Folder', deleted: false, embargo: null }
        }
        if (use_bulk) {
            initial_permissions.resultObjects = resultObjects;
            initial_permissions.metadata = { type: null, id: null }
        }

        wrapper = shallowMount(patronRoles, {
            global: {
                plugins: [createTestingPinia({
                    initialState: {
                        permissions: initial_permissions
                    },
                    stubActions: false
                })]
            }
        });

        store = usePermissionsStore();
        vi.stubGlobal('confirm', mockConfirm);
        selects = wrapper.findAll('select');
    }

    function stubDataLoad(load = response, uuid = UUID) {
        fetchMock.mockResponseOnce(JSON.stringify(load), {
            status: 200,
            headers: { 'content-type': 'application/json' }
        });
        mountApp();
    }

    function stubAllowedPrincipals(load) {
        fetchMock.mockResponseOnce(JSON.stringify(load));
    }

    function expectSaveButtonDisabled(expectDisabled = true) {
        let disabledValue = wrapper.find('#is-submitting').attributes();

        if (expectDisabled) {
            expect(disabledValue).toHaveProperty('disabled');
        } else {
            expect(disabledValue).not.toHaveProperty('disabled');
        }
    }
});

