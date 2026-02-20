import { flushPromises, shallowMount } from '@vue/test-utils';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { nextTick } from 'vue';
import staffRoles from '@/components/permissions-editor/staffRoles.vue';
import { createTestingPinia } from '@pinia/testing';
import { usePermissionsStore } from '@/stores/permissions';

const response = {
    inherited: { roles: [{ principal: 'test_admin', role: 'administrator' }] },
    assigned: { roles: [{ principal: 'test_user', role: 'canIngest' }] }
};

const user_role = { principal: 'test_user_2', role: 'canManage', type: 'new' };
const metadata = () => {
    return {
        id: '73bc003c-9603-4cd9-8a65-93a22520ef6a',
        type: 'AdminUnit',
        title: 'Test Stuff',
        objectPath: [{
            pid: 'collections',
            name: 'Content Collections Root',
            container: true
        }, {
            pid: '73bc003c-9603-4cd9-8a65-93a22520ef6a',
            name: 'Test Stuff',
            container: true
        }]
    };
};

let wrapper, store, mockConfirm, mockAlertHandler;

describe('staffRoles.vue', () => {
    beforeEach(async () => {
        fetchMock.enableMocks();
        fetchMock.resetMocks();

        // Create mock functions
        mockConfirm = vi.fn().mockReturnValue(true);
        mockAlertHandler = vi.fn();

        wrapper = shallowMount(staffRoles, {
            global: {
                plugins: [createTestingPinia({
                    initialState: {
                        permissions: {
                            actionHandler: {
                                addEvent: vi.fn()
                            },
                            alertHandler: {
                                alertHandler: mockAlertHandler
                            },
                            metadata: metadata()
                        }
                    },
                    stubActions: false
                })]
            }
        });

        store = usePermissionsStore();

        fetchMock.mockResponseOnce(JSON.stringify(response));
        wrapper.vm.getRoles();

        vi.stubGlobal('confirm', mockConfirm);

        await flushPromises();
        await nextTick();
    });

    afterEach(() => {
        fetchMock.disableMocks();
        mockAlertHandler.mockClear();
        mockConfirm.mockClear();
        store.$reset();
        vi.unstubAllGlobals();
        wrapper = null;
    });

    it("retrieves current staff roles data from the server", () => {
        expect(wrapper.vm.current_staff_roles).toEqual(response);
        expect(wrapper.vm.updated_staff_roles).toEqual(response.assigned.roles);
    });

    it("shows help text", async () => {
        expect(wrapper.find('#role-list').exists()).toBe(false);
        await wrapper.find('.info').trigger('click');
        expect(wrapper.find('#role-list').isVisible()).toBe(true);
    });

    it("triggers a submission", async () => {
        // Spy on methods
        let updateUsers = vi.spyOn(wrapper.vm, 'updateUserList');
        let setRoles = vi.spyOn(wrapper.vm, 'setRoles');

        // Add a new user
        await wrapper.find('input').setValue('test_user_71');
        await wrapper.findAll('option')[2].setSelected();
        await wrapper.find('.btn-add').trigger('click');

        // Mock the getRoles response after submit
        fetchMock.mockResponseOnce(JSON.stringify(response));
        fetchMock.mockResponseOnce(JSON.stringify({ success: true }));

        await wrapper.find('#is-submitting').trigger('click');
        await flushPromises();

        expect(updateUsers).toHaveBeenCalled();
        expect(setRoles).toHaveBeenCalled();

        updateUsers.mockRestore();
        setRoles.mockRestore();
    });

    it("sends current staff roles to the server", async () => {
        // Add a new user to enable submit button
        await wrapper.find('input').setValue('test_user_7');
        await wrapper.findAll('option')[2].setSelected();
        await wrapper.find('.btn-add').trigger('click');

        // Mock both the PUT and the subsequent GET for getRoles()
        fetchMock.mockResponseOnce(JSON.stringify({ success: true }));
        fetchMock.mockResponseOnce(JSON.stringify(response));

        await wrapper.find('#is-submitting').trigger('click');
        await flushPromises();

        // Find the PUT request specifically
        const putCall = fetchMock.mock.calls.find(call =>
            call[0].includes('/services/api/edit/acl/staff/') &&
            call[1]?.method === 'PUT'
        );

        expect(putCall).toBeDefined();
        expect(putCall[0]).toContain(`/services/api/edit/acl/staff/${wrapper.vm.uuid}`);
        expect(putCall[1].method).toEqual('PUT');
        expect(JSON.parse(putCall[1].body)).toEqual({
            roles: [
                ...response.assigned.roles,
                { principal: 'test_user_7', role: 'canDescribe', type: 'new'}
            ]
        });
    });

    it("it adds un-added users and then sends current staff roles to the server", async () => {
        let added_user = { principal: 'dean', role: 'canAccess', type: 'new' };
        let all_users = { roles: [...response.assigned.roles, ...[added_user]] };

        await wrapper.setData({
            user_name: 'dean'
        });

        // Mock both PUT and subsequent GET
        fetchMock.mockResponseOnce(JSON.stringify({ success: true }));
        fetchMock.mockResponseOnce(JSON.stringify(response));

        await wrapper.find('#is-submitting').trigger('click');
        await flushPromises();

        const lastCall = fetchMock.mock.calls.find(call =>
            call[0].includes('/services/api/edit/acl/staff/') &&
            call[1]?.method === 'PUT'
        );

        expect(lastCall[1].method).toEqual('PUT');
        expect(JSON.parse(lastCall[1].body)).toEqual(all_users);
    });

    it("displays inherited staff roles", () => {
        let cells = wrapper.findAll('.inherited-permissions td');
        expect(cells[0].text()).toEqual(response.inherited.roles[0].principal);
        expect(cells[1].text()).toEqual(response.inherited.roles[0].role);
    });

    it("displays names of containers that roles are assigned to in inherited table", async () => {
        store.setMetadata({
            id: '4f2be243-ce9e-4f26-91fc-08f1b592734d',
            title: 'Some Subfolder',
            type: 'Folder',
            objectPath: [{
                pid: 'collections',
                name: 'Content Collections Root',
                container: true
            }, {
                pid: '73bc003c-9603-4cd9-8a65-93a22520ef6a',
                name: 'Test Unit',
                container: true
            }, {
                pid: 'f88ff51e-7e74-4e0e-9ab9-259444393aeb',
                name: 'Test Collection',
                container: true
            }, {
                pid: '4f2be243-ce9e-4f26-91fc-08f1b592734d',
                name: 'Some Subfolder',
                container: true
            }]
        });

        const newResponse = {
            inherited: {
                roles: [
                    { principal: 'test_admin', role: 'unitOwner', assignedTo: '73bc003c-9603-4cd9-8a65-93a22520ef6a' },
                    { principal: 'test_manager', role: 'canManage', assignedTo: 'f88ff51e-7e74-4e0e-9ab9-259444393aeb' }
                ]
            },
            assigned: {
                roles: []
            }
        };

        fetchMock.mockResponseOnce(JSON.stringify(newResponse));
        wrapper.vm.getRoles();

        await flushPromises();
        await nextTick();

        let cells = wrapper.findAll('.inherited-permissions td');
        expect(cells[0].text()).toEqual(newResponse.inherited.roles[0].principal);
        expect(cells[1].text()).toEqual(newResponse.inherited.roles[0].role);
        expect(cells[2].text()).toEqual('Test Unit');
        expect(cells[3].text()).toEqual(newResponse.inherited.roles[1].principal);
        expect(cells[4].text()).toEqual(newResponse.inherited.roles[1].role);
        expect(cells[5].text()).toEqual('Test Collection');
    });

    it("does not display an inherited roles table if there are no inherited roles", async () => {
        await wrapper.setData({
            current_staff_roles: { inherited: { roles: [] }, assigned: { roles: [] } }
        });

        expect(wrapper.find('p').text()).toEqual('There are no inherited staff permissions.');
    });

    it("displays assigned staff roles", () => {
        let cells = wrapper.findAll('.assigned-permissions td');
        expect(cells[0].text()).toEqual(response.assigned.roles[0].principal);
    });

    it("updates user roles, if a role is changed", async () => {
        // Role loaded in beforeEach action
        expect(wrapper.vm.updated_staff_roles).toEqual([{ principal: 'test_user', role: 'canIngest' }]);

        // staffSelectRole component updates the data store
        const updatedUser = { principal: 'test_user', role: 'canManage' };
        await store.setStaffRole(updatedUser);
        expect(wrapper.vm.updated_staff_roles).toEqual([updatedUser]);
    });

    it("disables 'submit' by default", () => {
        let btn = wrapper.find('#is-submitting');
        let is_disabled = expect.stringContaining('disabled');
        expect(btn.html()).toEqual(is_disabled);
    });

    it("enables 'submit' button if user/role has been added or changed", async () => {
        let is_disabled = expect.stringContaining('disabled');

        // Add a user
        await wrapper.find('input').setValue('test_user_77');
        await wrapper.findAll('option')[1].setSelected();
        await wrapper.find('.btn-add').trigger('click');
        expect(wrapper.find('#is-submitting').html()).not.toEqual(is_disabled);
    });

    it("adds new assigned roles", async () => {
        await wrapper.setData({
            user_name: 'test_user_2',
            selected_role: 'canManage'
        });

        await wrapper.find('.btn-add').trigger('click');
        expect(wrapper.vm.updated_staff_roles).toEqual(response.assigned.roles.concat([user_role]));
    });

    it("resets the form after adding a user", async () => {
        await wrapper.find('input').setValue('test_user_11');
        await wrapper.findAll('option')[2].setSelected();
        await wrapper.find('.btn-add').trigger('click');

        expect(wrapper.vm.user_name).toEqual('');
        expect(wrapper.vm.selected_role).toEqual('canAccess');
    });

    it("does not add a new user with roles if user already exists", async () => {
        await wrapper.find('input').setValue('test_user');
        await wrapper.findAll('option')[2].setSelected();
        await wrapper.find('.btn-add').trigger('click');

        expect(wrapper.vm.updated_staff_roles).toEqual(response.assigned.roles);
        expect(wrapper.vm.response_message).toEqual('User: test_user already exists. User not added.');
    });

    it("marks user for deletion if user had previously assigned role", async () => {
        await wrapper.find('.btn-remove').trigger('click');
        expect(wrapper.vm.deleted_users).toEqual(response.assigned.roles);
    });

    it("removes deleted users before submitting", () => {
        let first_user_role = { principal: 'testy', role: 'canManage' };

        wrapper.vm.deleted_users = [user_role];
        wrapper.vm.updated_staff_roles = [first_user_role, user_role];

        const result = wrapper.vm.removeDeletedAssignedRoles();
        expect(result).toEqual([first_user_role]);
    });

    it("it updates button text based on context", async () => {
        let button = wrapper.find('.btn button');
        expect(button.text()).toEqual('Remove');

        // Mark a previously assigned role for deletion
        await button.trigger('click');
        await nextTick();
        button = wrapper.find('.btn button');
        expect(button.text()).toEqual('Undo Remove');

        // Undo marking previously assigned role for deletion
        await button.trigger('click');
        await nextTick();
        button = wrapper.find('.btn button');
        expect(button.text()).toEqual('Remove');
    });

    it("displays roles form if the container is of the proper type", async () => {
        let data = metadata();

        data.type = 'AdminUnit';
        await store.setMetadata(data);
        await nextTick();
        expect(wrapper.find('.assigned').exists()).toBe(true);

        data.type = 'Collection';
        await store.setMetadata(data);
        await nextTick();
        expect(wrapper.find('.assigned').exists()).toBe(true);
    });

    it("doesn't display roles form if the container isn't of the proper type", async () => {
        let data = metadata();

        data.type = 'Folder';
        await store.setMetadata(data);
        await nextTick();
        expect(wrapper.find('.assigned').exists()).toBe(false);

        data.type = 'Work';
        await store.setMetadata(data);
        await nextTick();
        expect(wrapper.find('.assigned').exists()).toBe(false);

        data.type = 'File';
        await store.setMetadata(data);
        await nextTick();
        expect(wrapper.find('.assigned').exists()).toBe(false);
    });

    it("displays a submit button for admin units and collections", async () => {
        let data = metadata();

        data.type = 'AdminUnit';
        await store.setMetadata(data);
        await nextTick();
        let btn = wrapper.find('#is-submitting');
        expect(btn.isVisible()).toBe(true);

        data.type = 'Collection';
        await store.setMetadata(data);
        await nextTick();
        expect(btn.isVisible()).toBe(true);
    });

    it("updates the data store to reset 'changesCheck' in parent component", async () => {
        await store.setCheckForUnsavedChanges(true);
        expect(wrapper.vm.user_name).toEqual('');
        expect(wrapper.vm.selected_role).toEqual('canAccess');
    });

    it("updates the data store to close the modal if 'Cancel' is clicked and there are no unsaved changes", () => {
        wrapper.find('#is-canceling').trigger('click');
        expect(store.showPermissionsModal).toEqual(false);
    });

    it("does not prompt the user if 'Submit' is clicked and there are unsaved changes", async () => {
        await wrapper.setData({
            deleted_users: response.assigned.roles
        });

        // Mock both PUT and subsequent GET
        fetchMock.mockResponseOnce(JSON.stringify({ success: true }));
        fetchMock.mockResponseOnce(JSON.stringify(response));

        await wrapper.find('#is-submitting').trigger('click');
        await flushPromises();

        expect(mockConfirm).toHaveBeenCalledTimes(0);
    });

    it("prompts the user if 'Cancel' is clicked and there are unsaved changes", async () => {
        await wrapper.setData({
            deleted_users: response.assigned.roles
        });
        await wrapper.find('#is-canceling').trigger('click');
        expect(mockConfirm).toHaveBeenCalled();
    });

    it("checks for un-saved user and permissions", async () => {
        wrapper.vm.unsavedUpdates();
        expect(wrapper.vm.unsaved_changes).toBe(false);

        await wrapper.setData({
            deleted_users: response.assigned.roles
        });
        wrapper.vm.unsavedUpdates();
        expect(wrapper.vm.unsaved_changes).toBe(true);

        await wrapper.setData({
            deleted_users: [],
            updated_staff_roles: [{ principal: 'test_user', role: 'canMove' }]
        });
        wrapper.vm.unsavedUpdates();
        expect(wrapper.vm.unsaved_changes).toBe(true);
    });
});