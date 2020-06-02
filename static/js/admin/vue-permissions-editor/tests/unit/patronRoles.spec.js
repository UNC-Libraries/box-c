import { createLocalVue, shallowMount } from '@vue/test-utils';
import patronRoles from '@/components/patronRoles.vue';
import moxios from 'moxios';

const localVue = createLocalVue();

let embargo_date = '2099-01-01';

let response = {
    inherited: { roles: [{ principal: 'everyone', role: 'canViewOriginals'}], deleted: false, embargo: null },
    assigned: { roles: [{ principal: 'everyone', role: 'canViewMetadata' }], deleted: false, embargo: null }
};

let response_all_same = {
    inherited: { roles: [{ principal: 'everyone', role: 'canViewOriginals'},
            { principal: 'authenticated', role: 'canViewOriginals'}], deleted: false, embargo: null },
    assigned: { roles: [{ principal: 'everyone', role: 'canViewOriginals' },
            { principal: 'authenticated', role: 'canViewOriginals'}], deleted: false, embargo: null }
};

let response_all_same_display = {
    inherited: { roles: [{ principal: 'everyone', role: 'canViewOriginals', principal_display: 'patron'},
            { principal: 'authenticated', role: 'canViewOriginals', principal_display: 'patron'}], deleted: false, embargo: null },
    assigned: { roles: [{ principal: 'everyone', role: 'canViewOriginals', principal_display: 'patron' },
            { principal: 'authenticated', role: 'canViewOriginals', principal_display: 'patron' }], deleted: false, embargo: null }
};

let response_display = {
    inherited: { roles: [{ principal: 'everyone', role: 'canViewOriginals', principal_display: 'everyone' }], deleted: false, embargo: null },
    assigned: { roles: [{ principal: 'everyone', role: 'canViewMetadata', principal_display: 'everyone' }], deleted: false, embargo: null }
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
    { principal: 'everyone', role: 'canViewOriginals', principal_display: 'everyone' },
    { principal: 'authenticated', role: 'canViewOriginals', principal_display: 'authenticated' }
];

let empty_response_no_inherited = {
    inherited: { roles: [], deleted: false, embargo: null },
    assigned: { roles: empty_defaults, deleted: false, embargo: null }
};

let same_inherited_roles = {
    inherited: { roles: [{ principal: 'everyone', role: 'canViewMetadata' },
            {principal: 'authenticated', role: 'canViewMetadata' }],
        deleted: false, embargo: null },
    assigned: { roles: [{ principal: 'everyone', role: 'canViewMetadata' },
            { principal: 'authenticated', role: 'canViewOriginals' }],
        deleted: false, embargo: null }
};

let same_assigned_roles = {
    inherited: { roles: [{ principal: 'everyone', role: 'canViewMetadata' },
            {principal: 'authenticated', role: 'canViewOriginals' }],
        deleted: false, embargo: null },
    assigned: { roles: [{ principal: 'everyone', role: 'canViewMetadata' },
            { principal: 'authenticated', role: 'canViewMetadata' }],
        deleted: false, embargo: null }
};

let same_all_roles = {
    inherited: { roles: [{ principal: 'everyone', role: 'canViewOriginals' },
            {principal: 'authenticated', role: 'canViewOriginals' }],
        deleted: false, embargo: null },
    assigned: { roles: [{ principal: 'everyone', role: 'canViewMetadata' },
            { principal: 'authenticated', role: 'canViewMetadata' }],
        deleted: false, embargo: null }
};

let full_roles = {
    inherited: { roles: [{ principal: 'everyone', role: 'canViewOriginals' },
            { principal: 'authenticated', role: 'canViewMetadata' }
        ], deleted: false, embargo: null },
    assigned: { roles: [
            { principal: 'everyone', role: 'none' },
            { principal: 'authenticated', role: "canViewOriginals" }
        ],
        deleted: false, embargo: null }
};

let full_roles_display = {
    inherited: { roles: [{ principal: 'everyone', role: 'canViewOriginals', principal_display: 'everyone' },
            { principal: 'authenticated', role: 'canViewMetadata', principal_display: 'authenticated' }
        ], deleted: false, embargo: null },
    assigned: { roles: [
            { principal: 'everyone', role: 'none', principal_display: 'everyone'},
            { principal: 'authenticated', role: "canViewOriginals", principal_display: 'authenticated' }
        ],
        deleted: false, embargo: null }
};

const STAFF_PERMS = [
    {principal: 'everyone', principal_display: 'staff', role: 'none'},
    {principal: 'authenticated', principal_display: 'staff', role: 'none'}
];

const DEFAULT_PERMS =  [
    {principal: 'everyone', principal_display: 'patron', role: 'canViewOriginals'},
    {principal: 'authenticated', principal_display: 'patron', role: 'canViewOriginals'}
];

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
            },
            methods: {setRoles}
        });

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

            expect(setRoles).toHaveBeenCalled();
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
            expect(wrapper.vm.display_roles.inherited.roles).toEqual(STAFF_PERMS);
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(DEFAULT_PERMS);
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
            expect(wrapper.vm.display_roles.inherited.roles).toEqual(STAFF_PERMS);
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(DEFAULT_PERMS);
            expect(wrapper.vm.patron_roles.inherited.roles).toEqual([]);
            expect(wrapper.vm.patron_roles.assigned.roles).toEqual(empty_defaults);
            expect(wrapper.vm.submit_roles.roles).toEqual(empty_defaults);
            done();
        });
    });

    it("does not set default inherited display roles for collections and sets assigned permissions to 'none'" +
        "if no assigned permissions are returned", (done) => {
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
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                {principal: 'everyone', principal_display: 'staff', role: 'none'},
                {principal: 'authenticated', principal_display: 'staff', role: 'none'}
            ]);
            done();
        })
    });

    it("sets display 'staff' if assigned everyone and authenticated principals both have the 'none' and inherited principals have the same role", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify({
                inherited: { roles: [
                        { principal: 'everyone', role: 'canViewOriginals' },
                        { principal: 'authenticated', role: 'canViewOriginals' }
                    ], deleted: false, embargo: null },
                assigned: { roles: [
                        { principal: 'everyone', role: 'none' },
                        { principal: 'authenticated', role: 'none' }
                    ], deleted: false, embargo: null
                }
            })
        });

        moxios.wait(() => {
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                {principal: "everyone", principal_display: "staff", role: 'none'},
                {principal: "authenticated", principal_display: "staff", role: 'none'}
            ]);
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

    it("sets a radio button if button or it's text is clicked", () => {
        wrapper.find('#patron').trigger('click');
        expect(wrapper.vm.user_type).toBe('patron');

        wrapper.find('#staff').trigger('click');
        expect(wrapper.vm.user_type).toBe('staff');

        wrapper.find('#patron input').trigger('click');
        expect(wrapper.vm.user_type).toBe('patron');

        wrapper.find('#staff input').trigger('click');
        expect(wrapper.vm.user_type).toBe('staff');
    });

    it("disables select boxes if 'Staff only access' wrapper text is clicked", (done) => {
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

    it("sets patron permissions to 'No Access' if 'Staff only access' wrapper text is clicked", (done) => {
        stubDataLoad(response_all_same);

        moxios.wait(() => {
            expect(wrapper.vm.everyone_role).toBe('canViewOriginals');
            expect(wrapper.vm.authenticated_role).toBe('canViewOriginals');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(response_all_same_display.assigned.roles);

            wrapper.find('#staff').trigger('click');

            expect(wrapper.vm.everyone_role).toBe('none');
            expect(wrapper.vm.authenticated_role).toBe('none');

            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                {principal: 'everyone', role: 'none', principal_display: 'staff' },
                {principal: 'authenticated', role: 'none', principal_display: 'staff' }
            ]);
            expect(wrapper.vm.submit_roles.roles).toEqual([
                {principal: 'everyone', role: 'none', principal_display: 'staff' },
                {principal: 'authenticated', role: 'none', principal_display: 'staff' }
            ]);
            done();
        });
    });

    it("sets patron permissions to 'No Access' if 'Staff only access' radio button is checked", (done) => {
        stubDataLoad(response_all_same);

        moxios.wait(() => {
            expect(wrapper.vm.everyone_role).toBe('canViewOriginals');
            expect(wrapper.vm.authenticated_role).toBe('canViewOriginals');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual(response_all_same_display.assigned.roles);

            wrapper.find('#staff input').trigger('click');

            expect(wrapper.vm.everyone_role).toBe('none');
            expect(wrapper.vm.authenticated_role).toBe('none');

            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                {principal: "everyone", principal_display: "staff", role: 'none'},
                {principal: "authenticated", principal_display: "staff", role: 'none'}
            ]);
            expect(wrapper.vm.submit_roles.roles).toEqual([
                {principal: 'everyone', role: 'none', principal_display: 'staff'},
                {principal: 'authenticated', role: 'none', principal_display: 'staff'}
            ]);
            done();
        });
    });

    it("sets role history", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            expect(wrapper.vm.everyone_role).toBe('canViewMetadata');
            expect(wrapper.vm.authenticated_role).toBe('none');
            expect(wrapper.vm.role_history).toEqual({});
            expect(wrapper.vm.history_set).toBe(false);

            wrapper.find('#staff input').trigger('click');

            expect(wrapper.vm.everyone_role).toBe('none');
            expect(wrapper.vm.authenticated_role).toBe('none');
            expect(wrapper.vm.role_history).toEqual({ patron: 'canViewMetadata', authenticated: 'none' });
            expect(wrapper.vm.history_set).toBe(true);
            done();
        });
    });

    it("loads role history", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            expect(wrapper.vm.role_history).toEqual({});
            expect(wrapper.vm.history_set).toBe(false);

            wrapper.find('#staff input').trigger('click');
            expect(wrapper.vm.everyone_role).toBe('none');
            expect(wrapper.vm.authenticated_role).toBe('none');
            expect(wrapper.vm.role_history).toEqual({ patron: 'canViewMetadata', authenticated: 'none' });
            expect(wrapper.vm.history_set).toBe(true);

            wrapper.find('#patron input').trigger('click');
            expect(wrapper.vm.everyone_role).toBe('canViewMetadata');
            expect(wrapper.vm.authenticated_role).toBe('none');
            expect(wrapper.vm.role_history).toEqual({ patron: 'canViewMetadata', authenticated: 'none' });
            expect(wrapper.vm.history_set).toBe(false);
            done();
        });
    });

    it("updates permissions for public users", (done) => {
        stubDataLoad();

        moxios.wait(() => {
            expect(wrapper.vm.everyone_role).toBe('canViewMetadata');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([{
                principal: 'everyone',
                role: 'canViewMetadata',
                principal_display: 'everyone'
            }]);

            wrapper.findAll('#public option').at(2).setSelected();

            let updated_public_roles =  [
                { principal: 'everyone', role: 'canViewAccessCopies', principal_display: 'everyone' },
                { principal: 'authenticated', role: 'none', principal_display: 'authenticated' }
            ];

            expect(wrapper.vm.everyone_role).toBe('canViewAccessCopies');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                { principal: 'everyone', role: 'canViewAccessCopies', principal_display: 'everyone' },
                { principal: 'authenticated', role: 'none', principal_display: 'authenticated' }
            ]);
            expect(wrapper.vm.submit_roles.roles).toEqual(updated_public_roles);

            done();
        });
    });

    it("updates permissions for authenticated users", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(same_assigned_roles)
        });

        moxios.wait(() => {
            expect(wrapper.vm.authenticated_role).toBe('canViewMetadata');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                { principal: 'everyone', role: 'canViewMetadata',  principal_display: 'everyone' },
                { principal: 'authenticated', role: 'canViewMetadata',  principal_display: 'authenticated' },
            ]);

            wrapper.findAll('#authenticated option').at(3).setSelected();

            let updated_authenticated_roles =  [
                { principal: 'everyone', role: 'canViewMetadata', principal_display: 'everyone' },
                { principal: 'authenticated', role: 'canViewOriginals', principal_display: 'authenticated' }
            ];

            expect(wrapper.vm.authenticated_role).toBe('canViewOriginals');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                { principal: 'everyone', role: 'canViewMetadata', principal_display: 'everyone' },
                { principal: 'authenticated', role: 'canViewOriginals', principal_display: 'authenticated' }
            ]);
            expect(wrapper.vm.submit_roles.roles).toEqual(updated_authenticated_roles);

            done();
        });
    });

    it("does not merge display permissions if assigned roles are different but inherited roles are the same", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(same_inherited_roles)
        });

        moxios.wait(() => {
            expect(wrapper.vm.everyone_role).toBe('canViewMetadata');
            expect(wrapper.vm.authenticated_role).toBe('canViewOriginals');

            expect(wrapper.vm.display_roles.inherited.roles).toEqual([
                { principal: 'everyone', role: 'canViewMetadata', principal_display: 'everyone' },
                {principal: 'authenticated', role: 'canViewMetadata',  principal_display: 'authenticated' }]);
            expect(wrapper.vm.patron_roles.inherited.roles).toEqual(same_inherited_roles.inherited.roles);
            done();
        });
    });

    it("does not merge display permissions if assigned roles are the same roles but inherited roles are different", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(same_assigned_roles)
        });

        moxios.wait(() => {
            expect(wrapper.vm.everyone_role).toBe('canViewMetadata');
            expect(wrapper.vm.authenticated_role).toBe('canViewMetadata');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                { principal: 'everyone', role: 'canViewMetadata',  principal_display: 'everyone' },
                { principal: 'authenticated', role: 'canViewMetadata',  principal_display: 'authenticated' },
            ]);
            expect(wrapper.vm.patron_roles.assigned.roles).toEqual(same_assigned_roles.assigned.roles);
            done();
        });
    });

    it("merges principal_display value if assigned principals roles are the same and inherited roles are the same", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(same_all_roles)
        });

        moxios.wait(() => {
            expect(wrapper.vm.everyone_role).toBe('canViewMetadata');
            expect(wrapper.vm.authenticated_role).toBe('canViewMetadata');
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                { principal: 'everyone', role: 'canViewMetadata',  principal_display: 'patron' },
                { principal: 'authenticated', role: 'canViewMetadata',  principal_display: 'patron' },
            ]);

            // Only adds principal_display if assigned roles are changed
            expect(wrapper.vm.patron_roles.assigned.roles).toEqual(same_all_roles.assigned.roles);
            done();
        });
    });

    it("calculates display rows if inherited roles are the same and assigned roles are the same", () => {
        wrapper.setData({
            display_roles: {
                inherited: {
                    roles: [
                        { principal: 'everyone', role: 'none',  principal_display: 'staff' },
                        { principal: 'authenticated', role: 'none',  principal_display: 'staff' },
                    ]},
                assigned: {
                    roles: [
                        { principal: 'everyone', role: 'canViewMetadata',  principal_display: 'patron' },
                        { principal: 'authenticated', role: 'canViewMetadata',  principal_display: 'patron' },
                    ]
                }

            }
        });

        expect(wrapper.vm.sortedRoles).toEqual({
            inherited: [
                { principal: 'everyone', role: 'none',  principal_display: 'staff' }
            ],
            assigned: [
                { principal: 'everyone', role: 'canViewMetadata',  principal_display: 'patron' }
            ]
        });
    });

    it("calculates display rows if inherited roles are the same and assigned roles are different", () => {
        wrapper.setData({
            display_roles: {
                inherited: {
                    roles: [
                        { principal: 'everyone', role: 'none',  principal_display: 'everyone' },
                        { principal: 'authenticated', role: 'none',  principal_display: 'authenticated' },
                    ]},
                assigned: {
                    roles: [
                        { principal: 'everyone', role: 'none',  principal_display: 'everyone' },
                        { principal: 'authenticated', role: 'canViewMetadata',  principal_display: 'authenticated' },
                    ]
                }

            }
        });

        expect(wrapper.vm.sortedRoles).toEqual({
            inherited: [
                { principal: 'everyone', role: 'none',  principal_display: 'everyone' },
                { principal: 'authenticated', role: 'none',  principal_display: 'authenticated' },
            ],
            assigned: [
                { principal: 'everyone', role: 'none',  principal_display: 'everyone' },
                { principal: 'authenticated', role: 'canViewMetadata',  principal_display: 'authenticated' },
            ]
        });
    });

    it("calculates display rows if inherited roles are different and assigned roles are the same, but not 'none'", () => {
        wrapper.setData({
            display_roles: {
                inherited: {
                    roles: [
                        { principal: 'everyone', role: 'none',  principal_display: 'everyone' },
                        { principal: 'authenticated', role: 'canViewOriginal',  principal_display: 'authenticated' },
                    ]},
                assigned: {
                    roles: [
                        { principal: 'everyone', role: 'canViewMetadata',  principal_display: 'everyone' },
                        { principal: 'authenticated', role: 'canViewMetadata',  principal_display: 'authenticated' },
                    ]
                }

            }
        });

        expect(wrapper.vm.sortedRoles).toEqual({
            inherited: [
                { principal: 'everyone', role: 'none',  principal_display: 'everyone' },
                { principal: 'authenticated', role: 'canViewOriginal',  principal_display: 'authenticated' },
            ],
            assigned: [
                { principal: 'everyone', role: 'canViewMetadata',  principal_display: 'everyone' },
                { principal: 'authenticated', role: 'canViewMetadata',  principal_display: 'authenticated' },
            ]
        });
    });

    it("calculates display rows if inherited roles are different and assigned roles are 'none'", () => {
        wrapper.setData({
            display_roles: {
                inherited: {
                    roles: [
                        { principal: 'everyone', role: 'none',  principal_display: 'everyone' },
                        { principal: 'authenticated', role: 'canViewOriginal',  principal_display: 'authenticated' },
                    ]},
                assigned: {
                    roles: [
                        { principal: 'everyone', role: 'none',  principal_display: 'staff' },
                        { principal: 'authenticated', role: 'none',  principal_display: 'staff' },
                    ]
                }

            }
        });

        expect(wrapper.vm.sortedRoles).toEqual({
            inherited: [
                { principal: 'everyone', role: 'none',  principal_display: 'everyone' },
                { principal: 'authenticated', role: 'canViewOriginal',  principal_display: 'authenticated' },
            ],
            assigned: [
                { principal: 'everyone', role: 'none',  principal_display: 'staff' },
                { principal: 'authenticated', role: 'none',  principal_display: 'staff' }
            ]
        });
    });

    it("calculates display rows if there are no inherited roles and assigned roles are 'none'", () => {
        wrapper.setData({
            display_roles: {
                inherited: {
                    roles: []},
                assigned: {
                    roles: [
                        { principal: 'everyone', role: 'none',  principal_display: 'staff' },
                        { principal: 'authenticated', role: 'none',  principal_display: 'staff' },
                    ]
                }

            }
        });

        expect(wrapper.vm.sortedRoles).toEqual({
            inherited: [],
            assigned: [
                { principal: 'everyone', role: 'none',  principal_display: 'staff' }
            ]
        });
    });

    it("calculates display rows if there are no inherited roles and assigned roles are the same", () => {
        wrapper.setData({
            display_roles: {
                inherited: {
                    roles: []},
                assigned: {
                    roles: [
                        { principal: 'everyone', role: 'canViewMetadata',  principal_display: 'patron' },
                        { principal: 'authenticated', role: 'canViewMetadata',  principal_display: 'patron' },
                    ]
                }

            }
        });

        expect(wrapper.vm.sortedRoles).toEqual({
            inherited: [],
            assigned: [
                { principal: 'everyone', role: 'canViewMetadata',  principal_display: 'patron' }
            ]
        });
    });

    it("calculates display rows if there are no inherited roles and assigned roles are different", () => {
        wrapper.setData({
            display_roles: {
                inherited: {
                    roles: []},
                assigned: {
                    roles: [
                        { principal: 'everyone', role: 'canViewMetadata',  principal_display: 'everyone' },
                        { principal: 'authenticated', role: 'canViewOriginals',  principal_display: 'authenticated' }
                    ]
                }

            }
        });

        expect(wrapper.vm.sortedRoles).toEqual({
            inherited: [],
            assigned: [
                { principal: 'everyone', role: 'canViewMetadata',  principal_display: 'everyone' },
                { principal: 'authenticated', role: 'canViewOriginals',  principal_display: 'authenticated' }
            ]
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
            response: JSON.stringify(same_assigned_roles)
        });

        moxios.wait(() => {
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                { principal: 'everyone', role: 'canViewMetadata',  principal_display: 'everyone' },
                { principal: 'authenticated', role: 'canViewMetadata',  principal_display: 'authenticated' },
            ]);
            done();
        })
    });

    it("updates display roles if an embargo is set from server response on the object", (done) => {
        let values = {
            inherited: { roles: [{ principal: 'everyone', role: 'canViewOriginals' }],
                deleted: false, embargo: null },
            assigned: { roles: [
                    { principal: 'everyone', role: 'canViewAccessCopies' },
                    { principal: 'authenticated', role: 'canViewOriginals' }
                ], deleted: false, embargo: embargo_date }
        };

        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(values)
        });

        moxios.wait(() => {
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                { principal: 'everyone', role: 'canViewMetadata', principal_display: 'everyone' },
                { principal: 'authenticated', role: "canViewMetadata", principal_display: 'authenticated' }
            ]);
            done();
        })
    });

    it("updates display roles if an embargo is set", (done) => {
        let values = {
            inherited: { roles: [{ principal: 'everyone', role: 'canViewOriginals' }], deleted: false, embargo: null },
            assigned: { roles: [
                    { principal: 'everyone', role: 'canViewAccessCopies' },
                    { principal: 'authenticated', role: "canViewOriginals" }
                ],
                deleted: false, embargo: null }
        };

        let values_display = {
            inherited: { roles: [{ principal: 'everyone', role: 'canViewOriginals', principal_display: 'everyone' }], deleted: false, embargo: null },
            assigned: { roles: [
                    { principal: 'everyone', role: 'canViewAccessCopies', principal_display: 'everyone' },
                    { principal: 'authenticated', role: 'canViewOriginals', principal_display: 'authenticated' }
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
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                { principal: 'everyone', role: 'canViewMetadata', principal_display: 'everyone' },
                { principal: 'authenticated', role: "canViewMetadata", principal_display: 'authenticated' }
            ]);
            done();
        });
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
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                { principal: 'everyone', role: 'none', principal_display: 'everyone' },
                { principal: 'authenticated', role: 'canViewMetadata', principal_display: 'authenticated'}
            ]);
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

    it("updates display roles if an embargo is added or removed", (done) => {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(full_roles)
        });

        moxios.wait(() => {
            wrapper.vm.$refs.embargoInfo.$emit('embargo-info', embargo_date);
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                { principal: 'everyone', role: 'none', principal_display: 'everyone' },
                { principal: 'authenticated', role: 'canViewMetadata', principal_display: 'authenticated'}
            ]);

            wrapper.vm.$refs.embargoInfo.$emit('embargo-info', null);
            expect(wrapper.vm.display_roles).toEqual(full_roles_display);
            done();
        });
    });

    it("does not set assigned principal display to staff if one assigned role is 'none' and the other is not", (done) => {
        let role_list = {
            inherited: { roles: [{ principal: 'everyone', role: 'canViewOriginals' },
                    { principal: 'authenticated', role: 'canViewMetadata' }
                ], deleted: false, embargo: null },
            assigned: { roles: [
                    { principal: 'everyone', role: 'none' },
                    { principal: 'authenticated', role: "canViewOriginals" }
                ], deleted: false, embargo: null }
        };

        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(role_list)
        });

        moxios.wait(() => {
            wrapper.vm.$refs.embargoInfo.$emit('embargo-info', embargo_date);
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                { principal: 'everyone', role: 'none', principal_display: 'everyone' },
                { principal: 'authenticated', role: 'canViewMetadata', principal_display: 'authenticated'}
            ]);

            wrapper.vm.$refs.embargoInfo.$emit('embargo-info', null);
            expect(wrapper.vm.display_roles.assigned.roles).toEqual([
                { principal: 'everyone', role: 'none', principal_display: 'everyone' },
                { principal: 'authenticated', role: 'canViewOriginals', principal_display: 'authenticated'}
            ]);
            done();
        });
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
            wrapper.findAll('option').at(0).setSelected();
            await wrapper.vm.$nextTick();
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

    function stubDataLoad(load = response) {
        wrapper.vm.getRoles();
        moxios.stubRequest(`/services/api/acl/patron/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(load)
        });
    }
});