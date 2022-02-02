import { shallowMount } from '@vue/test-utils'
import '@testing-library/jest-dom'
import staffRoles from '@/components/staffRoles.vue'
import moxios from "moxios";
import { createStore } from "vuex";

const response = {
    inherited: { roles: [{ principal: 'test_admin', role: 'administrator' }] },
    assigned: { roles: [{ principal: 'test_user', role: 'canIngest' }] }
};

const user_role = { principal: 'test_user_2', role: 'canManage', type: 'new' };
const metadata = () => {
    return { id: '73bc003c-9603-4cd9-8a65-93a22520ef6a', type: 'AdminUnit', title: 'Test Stuff', objectPath: [{
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
let wrapper;

describe('staffRoles.vue', () => {
    beforeEach(async () => {
        moxios.install();

        const store = createStore({
            state () {
                return {
                    actionHandler: { addEvent: jest.fn() },
                    alertHandler: { alertHandler: jest.fn() },
                    checkForUnsavedChanges: false,
                    embargoInfo: {
                        embargo: null,
                        skipEmbargo: true
                    },
                    metadata: metadata(),
                    permissionType: '',
                    resultObject: {},
                    resultObjects: [],
                    showModal: false,
                    staffRole: {}
                }
            },
            mutations: {
                setActionHandler (state, actionHandler) {
                    state.actionHandler = actionHandler;
                },
                setAlertHandler (state, alertHandler) {
                    state.alertHandler = alertHandler;
                },
                setCheckForUnsavedChanges (state, unsavedChanges) {
                    state.checkForUnsavedChanges = unsavedChanges;
                },
                setEmbargoInfo (state, embargoInfo) {
                    state.embargoInfo = embargoInfo;
                },
                setMetadata (state, metadata) {
                    state.metadata = metadata;
                },
                setPermissionType (state, permissionType) {
                    state.permissionType = permissionType;
                },
                setResultObject (state, resultObject) {
                    state.resultObject = resultObject;
                },
                setResultObjects (state, resultObjects) {
                    state.resultObjects = resultObjects;
                },
                setShowModal (state, showModal) {
                    state.showModal = showModal;
                },
                setStaffRole (state, staffRole) {
                    state.staffRole = staffRole;
                }
            }
        });

        wrapper = shallowMount(staffRoles, {
            global: {
                plugins: [store]
            }
        });

        moxios.stubRequest(`/services/api/acl/staff/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(response)
        });
        wrapper.vm.getRoles();
        global.confirm = jest.fn().mockReturnValue(true);
    });

    it("retrieves current staff roles data from the server", (done) => {
        moxios.wait(() => {
            expect(wrapper.vm.current_staff_roles).toEqual(response);
            expect(wrapper.vm.updated_staff_roles).toEqual(response.assigned.roles);
            done();
        });
    });

    it("shows help text",  (done) => {
        moxios.wait(async () => {
            expect(wrapper.find('#role-list').exists()).toBe(false);
            await wrapper.find('.info').trigger('click');
            expect(wrapper.find('#role-list').isVisible()).toBe(true);
            done();
        });
    });

    it("triggers a submission", async () => {
        // Mount separately to mock methods to test that they're called
        let updateUsers = jest.spyOn(wrapper.vm, 'updateUserList');
        let setRoles = jest.spyOn(wrapper.vm, 'setRoles');

        // Add a new user
        await wrapper.find('input').setValue('test_user_71');
        await wrapper.findAll('option')[2].setSelected();
        await wrapper.find('.btn-add').trigger('click');
        await wrapper.find('#is-submitting').trigger('click');

        expect(updateUsers).toHaveBeenCalled();
        expect(setRoles).toHaveBeenCalled();

        updateUsers.mockRestore();
        setRoles.mockRestore();
    });

    it("sends current staff roles to the server", (done) => {
        moxios.wait(async () => {
            // Add a new user to enable submit button
            await wrapper.find('input').setValue('test_user_7');
            await wrapper.findAll('option')[2].setSelected();
            await wrapper.find('.btn-add').trigger('click');
            await wrapper.find('#is-submitting').trigger('click');

            let request = moxios.requests.mostRecent();
            expect(request.config.method).toEqual('put');
            expect(JSON.parse(request.config.data)).toEqual( { roles: [...response.assigned.roles, ...[{ principal: 'test_user_7', role: 'canDescribe', type: 'new'}]] } );
            done();
        });
    });

    it("it adds un-added users and then sends current staff roles to the server", async (done) => {
        let added_user = { principal: 'dean', role: 'canAccess', type: 'new' };
        let all_users = { roles: [...response.assigned.roles, ...[added_user]] };

        await wrapper.setData({
            user_name: 'dean'
        });
        await wrapper.find('#is-submitting').trigger('click');

        moxios.wait(() => {
            let request = moxios.requests.mostRecent();
            expect(request.config.method).toEqual('put');
            expect(JSON.parse(request.config.data)).toEqual(all_users);
            done();
        });
    });

    it("displays inherited staff roles", (done) => {
        moxios.wait(() => {
            let cells = wrapper.findAll('.inherited-permissions td');
            expect(cells[0].text()).toEqual(response.inherited.roles[0].principal);
            expect(cells[1].text()).toEqual(response.inherited.roles[0].role);
            done();
        });
    });

    it("displays names of containers that roles are assigned to in inherited table", (done) => {
        const store = createStore({
            state () {
                return {
                    metadata: {
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
                    }
                }
            },
            mutations: {
                setMetadata (state, staffRole) {
                    state.staffRole = staffRole;
                }
            }
        });
        wrapper = shallowMount(staffRoles, {
            global: {
                plugins: [store]
            }

        });

        const response = {
            inherited: {
                roles: [{ principal: 'test_admin', role: 'unitOwner', assignedTo: '73bc003c-9603-4cd9-8a65-93a22520ef6a' },
                    { principal: 'test_manager', role: 'canManage', assignedTo: 'f88ff51e-7e74-4e0e-9ab9-259444393aeb' }]
            },
            assigned: {
                roles: []
            }
        };

        moxios.stubRequest(`/services/api/acl/staff/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(response)
        });
        wrapper.vm.getRoles();

        moxios.wait(() => {
            let cells = wrapper.findAll('.inherited-permissions td');
            expect(cells[0].text()).toEqual(response.inherited.roles[0].principal);
            expect(cells[1].text()).toEqual(response.inherited.roles[0].role);
            expect(cells[2].text()).toEqual('Test Unit');
            expect(cells[3].text()).toEqual(response.inherited.roles[1].principal);
            expect(cells[4].text()).toEqual(response.inherited.roles[1].role);
            expect(cells[5].text()).toEqual('Test Collection');
            done();
        });
    });

    it("does not display an inherited roles table if there are no inherited roles", (done) => {
        moxios.wait(async () => {
            await wrapper.setData({
                current_staff_roles: { inherited: { roles: [] }, assigned: { roles: [] } }
            });

            expect(wrapper.find('p').text()).toEqual('There are no inherited staff permissions.');
            done();
        });
    });

    it("displays assigned staff roles", (done) => {
        moxios.wait(() => {
            let cells = wrapper.findAll('.assigned-permissions td');
            expect(cells[0].text()).toEqual(response.assigned.roles[0].principal);
            // See test in staffRolesSelect.spec.js for test asserting that the correct option is displayed
            done();
        });
    });

    it("updates user roles, if a role is changed", async() => {
        // Role loaded in beforeEach action
        expect(wrapper.vm.updated_staff_roles).toEqual([{ principal: 'test_user', role: 'canIngest' }]);

        // staffSelectRole component updates the data store
        const updatedUser = { principal: 'test_user', role: 'canManage' };
        await wrapper.vm.$store.commit('setStaffRole', updatedUser);
        expect(wrapper.vm.updated_staff_roles).toEqual([updatedUser]);
    });

    it("disables 'submit' by default", (done) => {
        moxios.wait(() => {
            let btn = wrapper.find('#is-submitting');
            let is_disabled = expect.stringContaining('disabled');
            expect(btn.html()).toEqual(is_disabled);
            done();
        });
    });

    it("enables 'submit' button if user/role has been added or changed",  (done) => {
        let is_disabled = expect.stringContaining('disabled');

        // Add a user
        moxios.wait(async () => {
            await wrapper.find('input').setValue('test_user_77');
            await wrapper.findAll('option')[1].setSelected();
            await wrapper.find('.btn-add').trigger('click');
            expect(wrapper.find('#is-submitting').html()).not.toEqual(is_disabled);
            done();
        });
    });

    it("adds new assigned roles", (done) => {
        moxios.wait(async () => {
            await wrapper.setData({
                user_name: 'test_user_2',
                selected_role: 'canManage'
            });

            await wrapper.find('.btn-add').trigger('click');
            expect(wrapper.vm.updated_staff_roles).toEqual(response.assigned.roles.concat([user_role]));
            done();
        });
    });

    it("resets the form after adding a user", (done) => {
        moxios.wait(async () => {
            await wrapper.find('input').setValue('test_user_11');
            await wrapper.findAll('option')[2].setSelected();
            await wrapper.find('.btn-add').trigger('click');

            expect(wrapper.vm.user_name).toEqual('');
            expect(wrapper.vm.selected_role).toEqual('canAccess');
            done();
        });
    });

    it("does not add a new user with roles if user already exists", (done) => {
        moxios.wait(async () => {
            await wrapper.find('input').setValue('test_user');
            await wrapper.findAll('option')[2].setSelected();
            await wrapper.find('.btn-add').trigger('click');

            expect(wrapper.vm.updated_staff_roles).toEqual(response.assigned.roles);
            expect(wrapper.vm.response_message).toEqual('User: test_user already exists. User not added.');
            done();
        });
    });

    it("marks user for deletion if user had previously assigned role", (done) => {
        moxios.wait(async () => {
            await wrapper.find('.btn-remove').trigger('click');
            expect(wrapper.vm.deleted_users).toEqual(response.assigned.roles);
            done();
        });
    });

    it("removes deleted users before submitting", (done) => {
        moxios.wait(async () => {
            let first_user_role = { principal: 'testy', role: 'canManage' };

            await wrapper.setData({
                deleted_users: [user_role],
                updated_staff_roles: [first_user_role, user_role]
            });

            wrapper.vm.setRoles();

            expect(wrapper.vm.updated_staff_roles).toEqual([first_user_role]);
            done();
        });
    });

    it("it updates button text based on context", (done) => {
        moxios.wait(async () => {
            let button = wrapper.find('.btn button');
            expect(button.text()).toEqual('Remove');

            // Mark a previously assigned role for deletion
            await button.trigger('click');
            button = wrapper.find('.btn button');
            expect(button.text()).toEqual('Undo Remove');

            // Undo marking previously assigned role for deletion
            await button.trigger('click');
            button = wrapper.find('.btn button');
            expect(button.text()).toEqual('Remove');

            done();
        });
    });

    it("displays roles form if the container is of the proper type", (done) => {
        moxios.wait(async () => {
            let data = metadata();

            data.type = 'AdminUnit';
            await wrapper.vm.$store.commit('setMetadata', data);
            expect(wrapper.find('.assigned').exists()).toBe(true);

            data.type = 'Collection';
            await wrapper.vm.$store.commit('setMetadata', data);
            expect(wrapper.find('.assigned').exists()).toBe(true);
            done();
        });
    });

    it("doesn't display roles form if the container isn't of the proper type", (done) => {
        moxios.wait(async () => {
            let data = metadata();

            data.type = 'Folder';
            await wrapper.vm.$store.commit('setMetadata', data);
            expect(wrapper.find('.assigned').exists()).toBe(false);

            data.type = 'Work';
            await wrapper.vm.$store.commit('setMetadata', data);
            expect(wrapper.find('.assigned').exists()).toBe(false);

            data.type = 'File';
            await wrapper.vm.$store.commit('setMetadata', data);
            expect(wrapper.find('.assigned').exists()).toBe(false);
            done();
        });
    });

    it("displays a submit button for admin units and collections", async () => {
        let data = metadata();

        data.type = 'AdminUnit';
        await wrapper.vm.$store.commit('setMetadata', data);
        let btn = wrapper.find('#is-submitting');
        expect(btn.isVisible()).toBe(true);

        data.type = 'Collection';
        await wrapper.vm.$store.commit('setMetadata', data);
        expect(btn.isVisible()).toBe(true);
    });

    it("updates the data store to reset 'changesCheck' in parent component", async () => {
        await wrapper.vm.$store.commit('setCheckForUnsavedChanges', true);
        expect(wrapper.vm.user_name).toEqual('');
        expect(wrapper.vm.selected_role).toEqual('canAccess');
    });

    it("updates the data store to close the modal if 'Cancel' is clicked and there are no unsaved changes", (done) => {
        moxios.wait(() => {
            wrapper.find('#is-canceling').trigger('click');
            expect(wrapper.vm.$store.state.showModal).toEqual(false);
            done();
        });
    });

    it("does not prompt the user if 'Submit' is clicked and there are unsaved changes", (done) => {
        moxios.wait(async () => {
            await wrapper.setData({
                deleted_users: response.assigned.roles
            });
            await wrapper.find('#is-submitting').trigger('click');
            expect(global.confirm).toHaveBeenCalledTimes(0);
            done();
        });
    });

    it("prompts the user if 'Cancel' is clicked and there are unsaved changes", (done) => {
        moxios.wait(async () => {
            await wrapper.setData({
                deleted_users: response.assigned.roles
            });
            await wrapper.find('#is-canceling').trigger('click');
            expect(global.confirm).toHaveBeenCalled();
            done();
        });
    });

    it("checks for un-saved user and permissions", (done) => {
        moxios.wait(async () => {
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
            done();
        });
    });

    afterEach(() => {
        moxios.uninstall();
    });
});