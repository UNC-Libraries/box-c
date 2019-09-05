import { createLocalVue, shallowMount } from '@vue/test-utils'
import staffRoles from '@/components/staffRoles.vue'
import staffRolesForm from '@/components/staffRolesForm';
import moxios from "moxios";

const localVue = createLocalVue();
const response = {
    inherited:[{ principal: 'test_admin', role: 'administrator' }],
    assigned:[{ principal: 'test_user', role: 'canIngest' }]
};
const update_response = { "action":"editStaffRoles" }
const user_role = { principal: 'test_user_2', role: 'canManage', type: 'new' };
let wrapper;

describe('staffRoles.vue', () => {
    beforeEach(() => {
        moxios.install();

        wrapper = shallowMount(staffRoles, {
            localVue,
            propsData: {
                containerName: 'Test Unit',
                containerType: 'AdminSet',
                uuid: '73bc003c-9603-4cd9-8a65-93a22520ef6a'
            }
        });

        moxios.stubRequest(`/services/api/acl/staff/${wrapper.vm.uuid}`, {
            status: 200,
            response: JSON.stringify(response)
        });
    });

    it("retrieves current staff roles data from the server", (done) => {
        moxios.wait(() => {
            expect(wrapper.vm.current_staff_roles).toEqual(response);
            expect(wrapper.vm.updated_staff_roles).toEqual(response.assigned);
            done();
        });
    });

    it("sends current staff roles to the server", (done) => {
        wrapper.vm.setRoles();
        moxios.stubOnce('put', `/services/api/edit/acl/staff/${wrapper.vm.uuid}`, {
            status: 200,
            response: update_response
        });

        moxios.wait(() => {
            let request = moxios.requests.mostRecent();
            expect(request.config.method).toEqual('put');
            expect(JSON.parse(request.config.data)).toEqual(response.assigned);
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

    it("removes assigned roles", (done) => {
        moxios.wait(() => {
            wrapper.vm.removeUser(0);
            expect(wrapper.vm.deleted_users).toEqual(response.assigned);
            done();
        });
    });

    it("it updates button text based on context", () => {
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

    it("displays a submit button for admin units and collections", () => {
        wrapper.vm.canSetPermissions();
        let btn = wrapper.find('#is-submitting');
        expect(btn.isVisible()).toBe(true);

        wrapper.setProps({ containerType: 'Collection' });
        wrapper.vm.canSetPermissions();
        expect(btn.isVisible()).toBe(true)
    });

    it("emits an event to close the modal if 'Cancel' is clicked", () => {
        wrapper.find('.cancel').trigger('click');
        expect(wrapper.emitted()['show-modal'][0]).toEqual([false]);
    });

    afterEach(() => {
        moxios.uninstall();
    });
});