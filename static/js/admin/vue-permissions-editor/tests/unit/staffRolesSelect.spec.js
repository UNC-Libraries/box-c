import { shallowMount } from '@vue/test-utils';
import staffRolesSelect from '@/components/staffRolesSelect.vue';
import {createStore} from "vuex";

let wrapper;

describe('staffRolesSelect.vue', () => {
    beforeEach(() => {
        const store = createStore({
            state () {
                return {
                    staffRole: {}
                }
            },
            mutations: {
                setStaffRole (state, staffRole) {
                    state.staffRole = staffRole;
                }
            }
        });

        wrapper = shallowMount(staffRolesSelect, {
            props: {
                areDeleted: [],
                containerType: 'Collection',
                user: { principal: 'test_user', role: 'canAccess' }
            },
            global: {
                plugins: [store]
            }
        });
    });

    it("displays the correct role if preset", () => {
        let select = wrapper.find('select');
        expect(select.element.value).toEqual('canAccess');
    });

    it("updates data store with updated user role when user role changes", () => {
        wrapper.findAll('option')[2].setSelected();
        expect(wrapper.vm.$store.state.staffRole).toEqual({ principal: 'test_user', role: 'canDescribe' });
    });
});