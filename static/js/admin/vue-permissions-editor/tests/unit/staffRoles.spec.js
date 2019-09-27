import { createLocalVue, shallowMount } from '@vue/test-utils'
import staffRoles from '@/components/staffRoles.vue'
import staffRolesForm from '@/components/staffRolesForm';
import moxios from "moxios";

const localVue = createLocalVue();
const response = {
    inherited:[{ principal: 'test_admin', role: 'administrator' }],
    assigned:[{ principal: 'test_user', role: 'canIngest' }]
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
                containerName: 'Test Unit',
                containerType: 'AdminUnit',
                title: 'Test Stuff',
                uuid: '73bc003c-9603-4cd9-8a65-93a22520ef6a'
            }
        });

        moxios.stubRequest(`/services/api/acl/staff/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(response)
        });

        global.confirm = jest.fn().mockReturnValue(true);
    });

    it("retrieves current staff roles data from the server", (done) => {
        moxios.wait(() => {
            expect(wrapper.vm.current_staff_roles).toEqual(response);
            expect(wrapper.vm.updated_staff_roles).toEqual(response.assigned);
            done();
        });
    });

    it("shows help text", () => {
        expect(wrapper.find('#role-list').exists()).toBe(false);

        wrapper.find('.info').trigger('click');
        expect(wrapper.find('#role-list').isVisible()).toBe(true);
    });

    it("triggers a submission", () => {
        expect(wrapper.vm.is_submitting).toBe(false);
        wrapper.find('#is-submitting').trigger('click');
        expect(wrapper.vm.is_submitting).toBe(true);
    });

    it("sends current staff roles to the server", (done) => {
        wrapper.find('#is-submitting').trigger('click');
        wrapper.find(staffRolesForm).vm.$emit('username-set', false);

        moxios.wait(() => {
            let request = moxios.requests.mostRecent();
            expect(request.config.method).toEqual('put');
            expect(JSON.parse(request.config.data)).toEqual(response.assigned);
            done();
        });
    });

    it("it adds un-added users and then sends current staff roles to the server", (done) => {
        let added_user = { principal: 'dean', role: 'canAccess', type: 'new' };
        let all_users = [...response.assigned, ...[added_user]];
        wrapper.find(staffRolesForm).vm.$emit('add-user', added_user);
        wrapper.find('#is-submitting').trigger('click');
        wrapper.find(staffRolesForm).vm.$emit('username-set', false);

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
            expect(cells.at(0).text()).toEqual(response.inherited[0].principal);
            expect(cells.at(1).text()).toEqual(response.inherited[0].role);
            done();
        });
    });

    it("does not display an inherited roles table if there are no inherited roles", (done) => {
        moxios.wait(() => {
            wrapper.setData({
                current_staff_roles: { inherited: [], assigned: [] }
            });
            expect(wrapper.find('p').text()).toEqual('There are no inherited staff permissions.');
            done()
        });
    });

    it("displays assigned staff roles", (done) => {
        moxios.wait(() => {
            let cells = wrapper.findAll('.assigned-permissions td');
            expect(cells.at(0).text()).toEqual(response.assigned[0].principal);
            // See test in staffRolesSelect.spec.js for test asserting that the correct option is displayed
            done();
        });
    });

    it("adds new assigned roles", (done) => {
        moxios.wait(() => {
            wrapper.find(staffRolesForm).vm.$emit('add-user', user_role);
            expect(wrapper.vm.updated_staff_roles).toEqual(response.assigned.concat([user_role]));
            done();
        });
    });

    it("does not add a new user with roles if user already exists", (done) => {
        moxios.wait(() => {
            let current_staff_roles = response.assigned.concat([user_role]);
            wrapper.setData({
                updated_staff_roles: current_staff_roles
            });

            wrapper.find(staffRolesForm).vm.$emit('add-user', user_role);
            expect(wrapper.vm.updated_staff_roles).toEqual(current_staff_roles);
            done();
        });
    });

    it("removes assigned roles for previously assigned users", (done) => {
        moxios.wait(() => {
            wrapper.vm.markUserForDeletion(0);
            expect(wrapper.vm.deleted_users).toEqual(response.assigned);
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
        moxios.wait(() => {
            let button = wrapper.find('.btn button');
            expect(button.text()).toEqual('Remove');

            // Mark a previously assigned role for deletion
            button.trigger('click');
            expect(button.text()).toEqual('Undo Remove');

            // Undo marking previously assigned role for deletion
            button.trigger('click');
            expect(button.text()).toEqual('Remove');

            done();
        });
    });

    it("displays roles form if the container is of the proper type", (done) => {
        moxios.wait(() => {
            wrapper.setProps({containerType: 'AdminUnit'});
            expect(wrapper.find('.assigned').exists()).toBe(true);

            wrapper.setProps({containerType: 'Collection'});
            expect(wrapper.find('.assigned').exists()).toBe(true);
            done();
        });
    });

    it("doesn't display roles form if the container isn't of the proper type", (done) => {
        moxios.wait(() => {
            wrapper.setProps({containerType: 'Folder'});
            expect(wrapper.find('.assigned').exists()).toBe(false);

            wrapper.setProps({containerType: 'Work'});
            expect(wrapper.find('.assigned').exists()).toBe(false);

            wrapper.setProps({containerType: 'File'});
            expect(wrapper.find('.assigned').exists()).toBe(false);
            done();
        });
    });

    it("displays a submit button for admin units and collections", () => {
        wrapper.setProps({containerType: 'AdminUnit'});
        let btn = wrapper.find('#is-submitting');
        expect(btn.isVisible()).toEqual(true);

        wrapper.setProps({containerType: 'Collection'});
        expect(btn.isVisible()).toEqual(true)
    });

    it("emits an event to reset 'changesCheck' in parent component", () => {
        wrapper.setProps({ changesCheck: true });
        expect(wrapper.emitted()['reset-changes-check'][0]).toEqual([false]);
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
                deleted_users: response.assigned
            });
            wrapper.find('#is-submitting').trigger('click');
            expect(global.confirm).toHaveBeenCalledTimes(0);
            done();
        });
    });

    it("prompts the user if 'Cancel' is clicked and there are unsaved changes", (done) => {
        moxios.wait(() => {
            wrapper.setData({
                deleted_users: response.assigned
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
                deleted_users: response.assigned
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