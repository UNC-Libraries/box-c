import { createLocalVue, shallowMount } from '@vue/test-utils'
import staffRolesSelect from '@/components/staffRolesSelect.vue'
import staffRoleList from '@/mixins/staffRoleList.js';

const localVue = createLocalVue();
const collection_roles = [
    { text: 'can Access', value: 'canAccess' },
    { text: 'can Ingest', value: 'canIngest' },
    { text: 'can Describe', value: 'canDescribe' },
    { text: 'can Process', value: 'canProcess' },
    { text: 'can Manage', value: 'canManage' }
];
const all_roles = collection_roles.concat([
    { text: 'Unit Owner', value: 'unitOwner' }
]);
let wrapper;

describe('staffRoleList', () => {
    // Set wrapper using any component that uses staffRoleList mixin to avoid test warnings about missing template
    it("displays all options for Admin Units", () => {
        wrapper = shallowMount(staffRolesSelect, {
            localVue,
            propsData: {
                containerType: 'AdminUnit',
                user: { principal: 'test_user', role: 'canAccess' }
            }
        });
        expect(wrapper.vm.containerRoles(wrapper.vm.containerType)).toEqual(all_roles);
    });

    it("displays a subset of options for Collections", () => {
        wrapper = shallowMount(staffRolesSelect, {
            localVue,
            propsData: {
                containerType: 'Collection',
                user: { principal: 'test_user', role: 'canAccess' }
            }
        });
        expect(wrapper.vm.containerRoles(wrapper.vm.containerType)).toEqual(collection_roles);
    });
});