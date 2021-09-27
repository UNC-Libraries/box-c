import { createLocalVue, shallowMount } from '@vue/test-utils'
import '@testing-library/jest-dom'
import staffRoles from '@/components/staffRoles.vue'
import moxios from "moxios";

const localVue = createLocalVue();
const response = {
    inherited: { roles: [{ principal: 'test_admin', role: 'administrator' }] },
    assigned: { roles: [{ principal: 'test_user', role: 'canIngest' }] }
};

const user_role = { principal: 'test_user_2', role: 'canManage', type: 'new' };

let wrapper;

describe('staffRoles.vue', () => {
    beforeEach(() => {
        moxios.install();

        wrapper = shallowMount(staffRoles, {
            localVue,
            propsData: {
                alertHandler: {
                    alertHandler: jest.fn() // This method lives outside of the Vue app
                },
                objectPath: [{
                    pid: 'collections',
                    name: 'Content Collections Root',
                    container: true
                }, {
                    pid: '73bc003c-9603-4cd9-8a65-93a22520ef6a',
                    name: 'Test Stuff',
                    container: true
                }],
                containerType: 'AdminUnit',
                title: 'Test Stuff',
                uuid: '73bc003c-9603-4cd9-8a65-93a22520ef6a'
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

    it("shows help text", async () => {
        expect(wrapper.find('#role-list').exists()).toBe(false);

        wrapper.find('.info').trigger('click');
        await wrapper.vm.$nextTick();
        expect(wrapper.find('#role-list').isVisible()).toBe(true);
    });

    it("triggers a submission", async () => {
        // Mount separately to mock methods to test that they're called
        let updateUsers = jest.spyOn(wrapper.vm, 'updateUserList');
        let setRoles = jest.spyOn(wrapper.vm, 'setRoles');

        // Add a new user
        wrapper.find('input').setValue('test_user_71');
        wrapper.findAll('option').at(2).setSelected();
        wrapper.find('.btn-add').trigger('click');

        await wrapper.vm.$nextTick();
        wrapper.find('#is-submitting').trigger('click');

        await wrapper.vm.$nextTick();
        expect(updateUsers).toHaveBeenCalled();
        expect(setRoles).toHaveBeenCalled();

        updateUsers.mockRestore();
        setRoles.mockRestore();
    });

    it("sends current staff roles to the server", async (done) => {
        // Add a new user to enable submit button
        wrapper.find('input').setValue('test_user_7');
        wrapper.findAll('option').at(2).setSelected();
        wrapper.find('.btn-add').trigger('click');

        await wrapper.vm.$nextTick();
        wrapper.find('#is-submitting').trigger('click');

        moxios.wait(() => {
            let request = moxios.requests.mostRecent();
            expect(request.config.method).toEqual('put');
            expect(JSON.parse(request.config.data)).toEqual( { roles: [...response.assigned.roles, ...[{ principal: 'test_user_7', role: 'canDescribe', type: 'new'}]] } );
            done();
        });
    });

    it("it adds un-added users and then sends current staff roles to the server", async (done) => {
        let added_user = { principal: 'dean', role: 'canAccess', type: 'new' };
        let all_users = { roles: [...response.assigned.roles, ...[added_user]] };

        wrapper.setData({
            user_name: 'dean'
        });

        await wrapper.vm.$nextTick();
        wrapper.find('#is-submitting').trigger('click');

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
            expect(cells.at(0).text()).toEqual(response.inherited.roles[0].principal);
            expect(cells.at(1).text()).toEqual(response.inherited.roles[0].role);
            done();
        });
    });

    it("displays names of containers that roles are assigned to in inherited table", (done) => {
        wrapper = shallowMount(staffRoles, {
            localVue,
            propsData: {
                alertHandler: {
                    alertHandler: jest.fn() // This method lives outside of the Vue app
                },
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
                    name: 'Test Collecton',
                    container: true
                }, {
                    pid: '4f2be243-ce9e-4f26-91fc-08f1b592734d',
                    name: 'Some Subfolder',
                    container: true
                }],
                containerType: 'Folder',
                title: 'Some Subfolder',
                uuid: '4f2be243-ce9e-4f26-91fc-08f1b592734d'
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
            expect(cells.at(0).text()).toEqual(response.inherited.roles[0].principal);
            expect(cells.at(1).text()).toEqual(response.inherited.roles[0].role);
            expect(cells.at(2).text()).toEqual('Test Unit');
            expect(cells.at(3).text()).toEqual(response.inherited.roles[1].principal);
            expect(cells.at(4).text()).toEqual(response.inherited.roles[1].role);
            expect(cells.at(5).text()).toEqual('Test Collecton');
            done();
        });
    });

    it("does not display an inherited roles table if there are no inherited roles", (done) => {
        moxios.wait(async () => {
            wrapper.setData({
                current_staff_roles: { inherited: { roles: [] }, assigned: { roles: [] } }
            });

            await wrapper.vm.$nextTick();
            expect(wrapper.find('p').text()).toEqual('There are no inherited staff permissions.');
            done()
        });
    });

    it("displays assigned staff roles", (done) => {
        moxios.wait(() => {
            let cells = wrapper.findAll('.assigned-permissions td');
            expect(cells.at(0).text()).toEqual(response.assigned.roles[0].principal);
            // See test in staffRolesSelect.spec.js for test asserting that the correct option is displayed
            done();
        });
    });

    it("disables 'submit' by default", () => {
        let btn = wrapper.find('#is-submitting');
        let is_disabled = expect.stringContaining('disabled');
        expect(btn.html()).toEqual(is_disabled);
    });

    it("enables 'submit' button if user/role has been added or changed", async () => {
        let btn = wrapper.find('#is-submitting');
        let is_disabled = expect.stringContaining('disabled');

        // Add a user
        wrapper.find('input').setValue('test_user_77');
        wrapper.findAll('option').at(1).setSelected();
        wrapper.find('.btn-add').trigger('click');

        await wrapper.vm.$nextTick();
        expect(btn.html()).not.toEqual(is_disabled);
    });

    it("adds new assigned roles", (done) => {
        moxios.wait(() => {
            wrapper.setData({
                user_name: 'test_user_2',
                selected_role: 'canManage'
            });

            wrapper.find('.btn-add').trigger('click');

            expect(wrapper.vm.updated_staff_roles).toEqual(response.assigned.roles.concat([user_role]));
            done();
        });
    });

    it("resets the form after adding a user", (done) => {
        moxios.wait(() => {
            wrapper.find('input').setValue('test_user_11');
            wrapper.findAll('option').at(2).setSelected();
            wrapper.find('.btn-add').trigger('click');

            expect(wrapper.vm.user_name).toEqual('');
            expect(wrapper.vm.selected_role).toEqual('canAccess');
            done();
        });
    });

    it("does not add a new user with roles if user already exists", (done) => {
        moxios.wait(() => {
            wrapper.find('input').setValue('test_user');
            wrapper.findAll('option').at(2).setSelected();
            wrapper.find('.btn-add').trigger('click');

            expect(wrapper.vm.updated_staff_roles).toEqual(response.assigned.roles);
            expect(wrapper.vm.response_message).toEqual('User: test_user already exists. User not added.');
            done();
        });
    });

    it("marks user for deletion if user had previously assigned role", (done) => {
        moxios.wait(() => {
            wrapper.find('.btn-remove').trigger('click');
            expect(wrapper.vm.deleted_users).toEqual(response.assigned.roles);
            done();
        });
    });

    it("removes deleted users before submitting", (done) => {
        moxios.wait(() => {
            let first_user_role = { principal: 'testy', role: 'canManage' };

            wrapper.setData({
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
            button.trigger('click');
            await wrapper.vm.$nextTick();
            expect(button.text()).toEqual('Undo Remove');

            // Undo marking previously assigned role for deletion
            button.trigger('click');
            await wrapper.vm.$nextTick();
            expect(button.text()).toEqual('Remove');

            done();
        });
    });

    it("displays roles form if the container is of the proper type", (done) => {
        moxios.wait(async () => {
            wrapper.setProps({containerType: 'AdminUnit'});

            await wrapper.vm.$nextTick();
            expect(wrapper.find('.assigned').exists()).toBe(true);

            wrapper.setProps({containerType: 'Collection'});

            await wrapper.vm.$nextTick();
            expect(wrapper.find('.assigned').exists()).toBe(true);
            done();
        });
    });

    it("doesn't display roles form if the container isn't of the proper type", (done) => {
        moxios.wait(async () => {
            wrapper.setProps({containerType: 'Folder'});
            await wrapper.vm.$nextTick();
            expect(wrapper.find('.assigned').exists()).toBe(false);

            wrapper.setProps({containerType: 'Work'});
            await wrapper.vm.$nextTick();
            expect(wrapper.find('.assigned').exists()).toBe(false);

            wrapper.setProps({containerType: 'File'});
            await wrapper.vm.$nextTick();
            expect(wrapper.find('.assigned').exists()).toBe(false);
            done();
        });
    });

    it("displays a submit button for admin units and collections", async () => {
        wrapper.setProps({containerType: 'AdminUnit'});
        await wrapper.vm.$nextTick();
        let btn = wrapper.find('#is-submitting');
        expect(btn.isVisible()).toBe(true);

        wrapper.setProps({containerType: 'Collection'});

        await wrapper.vm.$nextTick();
        expect(btn.isVisible()).toBe(true);
    });

    it("emits an event to reset 'changesCheck' in parent component", async () => {
        wrapper.setProps({changesCheck: true});

        await wrapper.vm.$nextTick();
        expect(wrapper.vm.user_name).toEqual('');
        expect(wrapper.vm.selected_role).toEqual('canAccess');
    });

    it("emits an event to close the modal if 'Cancel' is clicked and there are no unsaved changes", (done) => {
        moxios.wait(() => {
            wrapper.find('#is-canceling').trigger('click');
            expect(wrapper.emitted()['show-modal'][0]).toEqual([false]);
            done();
        });
    });

    it("does not prompt the user if 'Submit' is clicked and there are unsaved changes", (done) => {
        moxios.wait(() => {
            wrapper.setData({
                deleted_users: response.assigned.roles
            });
            wrapper.find('#is-submitting').trigger('click');
            expect(global.confirm).toHaveBeenCalledTimes(0);
            done();
        });
    });

    it("prompts the user if 'Cancel' is clicked and there are unsaved changes", (done) => {
        moxios.wait(() => {
            wrapper.setData({
                deleted_users: response.assigned.roles
            });
            wrapper.find('#is-canceling').trigger('click');
            expect(global.confirm).toHaveBeenCalled();
            done();
        });
    });

    it("checks for un-saved user and permissions", (done) => {
        moxios.wait(() => {
            wrapper.vm.unsavedUpdates();
            expect(wrapper.vm.unsaved_changes).toBe(false);

            wrapper.setData({
                deleted_users: response.assigned.roles
            });
            wrapper.vm.unsavedUpdates();
            expect(wrapper.vm.unsaved_changes).toBe(true);

            wrapper.setData({
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