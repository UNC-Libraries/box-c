import { createLocalVue, shallowMount } from '@vue/test-utils';
import patronDisplayRow from '@/components/patronDisplayRow.vue';
import {getMilliseconds} from "date-fns";

const localVue = createLocalVue();
const STAFF_ONLY_ROLE_TEXT = '\u2014';
const staff_user = { principal: 'staff', role: STAFF_ONLY_ROLE_TEXT, principal_display: 'staff' };
let dash_regex = new RegExp(STAFF_ONLY_ROLE_TEXT);
let wrapper, icons;
let columns, permission_type, public_principal, public_role;

describe('patronRoles.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(patronDisplayRow, {
            localVue,
            propsData: {
                displayRoles: {
                    inherited: { roles: [
                            { principal: 'everyone', role: 'canViewMetadata', principal_display: 'everyone' },
                            { principal: 'authenticated', role: 'canViewAccessCopies', principal_display: 'authenticated' }
                        ],
                        embargo: null,
                        deleted: false
                    },
                    assigned: {
                        roles: [
                            { principal: 'everyone', role: 'canViewOriginals', principal_display: 'everyone' },
                            { principal: 'authenticated', role: 'canViewAccessCopies', principal_display: 'authenticated' }
                        ],
                        embargo: null,
                        deleted: false
                    }
                },
                possibleRoles: [
                    { text: STAFF_ONLY_ROLE_TEXT , role: STAFF_ONLY_ROLE_TEXT },
                    { text: 'No Access', role: 'none' },
                    { text: 'Can Discover', role: 'none' },
                    { text: 'Metadata Only', role: 'canViewMetadata' },
                    { text: 'Access Copies', role: 'canViewAccessCopies' },
                    { text: 'All of this folder', role: 'canViewOriginals' }
                ],
                type: 'assigned',
                user: { principal: 'everyone', role: 'canViewOriginals', principal_display: 'everyone' }
            }
        });

        columns = wrapper.findAll('td');
        permission_type = columns.at(0);
        public_principal = columns.at(1);
        public_role = columns.at(2);
    });

    it("displays public assigned patron roles", () => {
        expect(permission_type.text()).toBe('From Object');
        expect(public_principal.text()).toMatch(/^Public.Users/);
        expect(public_role.text()).toMatch(/^All.of.this.folder/);
    });

    it("displays authenticated assigned patron roles", async () => {
        wrapper.setProps({
            user:  { principal: 'authenticated', role: 'canViewAccessCopies', principal_display: 'authenticated' }
        });

        await wrapper.vm.$nextTick();
        expect(permission_type.text()).toBe('');
        expect(public_principal.text()).toMatch(/^authenticated/);
        expect(public_role.text()).toMatch(/^Access.Copies/);
    });

    it("displays staff only assigned patron roles", async () => {
        wrapper.setProps({
            displayRoles: {
                inherited: {
                    roles: [],
                    embargo: null,
                    deleted: false,
                },
                assigned: {
                    roles: [staff_user],
                    embargo: null,
                    deleted: false
                }
            },
            user: staff_user
        });

        await wrapper.vm.$nextTick();
        expect(permission_type.text()).toBe('From Object');
        expect(public_principal.text()).toMatch(/^staff/);
        expect(public_role.text()).toMatch(dash_regex);
    });

    it("displays public inherited patron roles", async () => {
        wrapper.setProps({
            type: 'inherited',
            user: { principal: 'everyone', role: 'canViewMetadata', principal_display: 'everyone' }
        });

        await wrapper.vm.$nextTick();
        expect(permission_type.text()).toBe('From Parent');
        expect(public_principal.text()).toMatch(/^Public.Users/);
        expect(public_role.text()).toMatch(/^Metadata.Only/);
    });

    it("displays authenticated inherited patron roles", async () => {
        wrapper.setProps({
            type: 'inherited',
            user:  { principal: 'authenticated', role: 'canViewAccessCopies', principal_display: 'authenticated' }
        });

        await wrapper.vm.$nextTick();
        expect(permission_type.text()).toBe('');
        expect(public_principal.text()).toMatch(/^authenticated/);
        expect(public_role.text()).toMatch(/^Access.Copies/);
    });

    it("displays 'Patron' for everyone user if permission is the same as authenticated user", async () => {
        wrapper.setProps({
            user: { principal: 'everyone', role: 'canViewAccessCopies', principal_display: 'patron' }
        });

        await wrapper.vm.$nextTick();
        let user_text = wrapper.find('.access-display');
        expect(user_text.text()).toMatch(/^patron/);
    });

    it("displays staff only inherited patron roles", async () => {
        wrapper.setProps({
            displayRoles: {
                inherited: {
                    roles: [staff_user],
                    embargo: null,
                    deleted: false,
                },
                assigned: {
                    roles: [],
                    embargo: null,
                    deleted: false
                }
            },
            type: 'inherited',
            user: staff_user
        });

        await wrapper.vm.$nextTick();
        expect(permission_type.text()).toBe('From Parent');
        expect(public_principal.text()).toMatch(/^staff/);
        expect(public_role.text()).toMatch(dash_regex);
    });

    it("display a 'more info' icon for 'Public Users' users", () => {
        icons = wrapper.findAll('i.fa-question-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(1);
        expect(icons.at(0).classes()).toContain('fa-question-circle');
    });

    it("does not display a 'more info' icon for 'authenticated' users", async () => {
        wrapper.setProps({ user: { principal: 'authenticated', role: 'canViewOriginals', principal_display: 'authenticated' } });

        await wrapper.vm.$nextTick();
        icons = wrapper.findAll('i.fa-question-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(0);
    });

    it("does not display a 'more info' icon for 'Staff' users", async () => {
        wrapper.setProps({
            displayRoles: {
                inherited: { roles: [staff_user],
                    embargo: null,
                    deleted: false
                },
                assigned: {
                    roles: [staff_user],
                    embargo: null,
                    deleted: false
                }
            },
            user: staff_user
        });

        await wrapper.vm.$nextTick();
        icons = wrapper.findAll('i.fa-question-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(0);
    });

    it("displays 'effective permissions' icons for most restrictive permission", async () => {
        wrapper.setProps({
            displayRoles: {
                inherited: {
                    roles: [staff_user],
                    embargo: null,
                    deleted: false,
                },
                assigned: {
                    roles: [{ principal: 'everyone', role: 'canViewOriginals', principal_display: 'everyone' },
                        { principal: 'authenticated', role: 'canViewAccessCopies', principal_display: 'authenticated' }],
                    embargo: null,
                    deleted: false
                }
            },
            type: 'inherited',
            user: staff_user
        });

        await wrapper.vm.$nextTick();
        icons = wrapper.findAll('i').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(2);
        expect(icons.at(0).classes()).toContain('fa-check-circle');
        expect(icons.at(1).classes()).toContain('fa-check-circle');
    });

    it("displays 'effective permissions' icons for most restrictive public permission if there are no inherited roles", async () => {
        wrapper.setProps({
            displayRoles: {
                inherited: { roles: [],
                    embargo: null,
                    deleted: false
                },
                assigned: {
                    roles: [
                        { principal: 'everyone', role: 'canViewOriginals', principal_display: 'everyone' },
                        { principal: 'authenticated', role: 'canViewAccessCopies', principal_display: 'authenticated' }
                    ],
                    embargo: null,
                    deleted: true
                }
            },
            type: 'assigned',
            user:  { principal: 'everyone', role: 'canViewOriginals', principal_display: 'everyone' }
        });

        await wrapper.vm.$nextTick();
        icons = wrapper.findAll('i.fa-check-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(2);
    });

    it("displays 'effective permissions' icons for most restrictive public permission", async () => {
        wrapper.setProps({
            displayRoles: {
                inherited: { roles: [
                        { principal: 'everyone', role: 'none', principal_display: 'patron' },
                        { principal: 'authenticated', role: 'none', principal_display: 'patron' }
                    ],
                    embargo: null,
                    deleted: false
                },
                assigned: {
                    roles: [
                        { principal: 'everyone', role: 'canViewOriginals', principal_display: 'everyone' },
                        { principal: 'authenticated', role: 'canViewAccessCopies', principal_display: 'authenticated' }
                    ],
                    embargo: null,
                    deleted: true
                }
            },
            type: 'assigned',
            user:  { principal: 'everyone', role: 'canViewOriginals', principal_display: 'everyone' }
        });

        await wrapper.vm.$nextTick();
        // Assigned permissions should not have a check icon
        icons = wrapper.findAll('i.fa-check-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(0);

        // Inherited permissions should have a check icon
        wrapper.setProps({
            type: 'inherited',
            user:  { principal: 'everyone', role: 'none', principal_display: 'everyone' }
        });

        await wrapper.vm.$nextTick();
        icons = wrapper.findAll('i').filter(i => !i.classes('hidden') && !i.classes('fa-question-circle'));
        expect(icons.at(0).classes()).toContain('fa-check-circle');
        expect(icons.at(1).classes()).toContain('fa-check-circle');
    });

    it("displays 'effective permissions' icons for most restrictive authenticated permission", async () => {
        wrapper.setProps({
            displayRoles: {
                inherited: { roles: [
                        { principal: 'everyone', role: 'none', principal_display: 'everyone' },
                        { principal: 'authenticated', role: 'canViewOriginals', principal_display: 'authenticated' }
                    ],
                    embargo: null,
                    deleted: false
                },
                assigned: {
                    roles: [
                        { principal: 'everyone', role: 'canViewAccessCopies', principal_display: 'everyone' },
                        { principal: 'authenticated', role: 'none', principal_display: 'authenticated' }
                    ],
                    embargo: null,
                    deleted: false
                }
            },
            type: 'assigned',
            user:  { principal: 'authenticated', role: 'none', principal_display: 'authenticated' }
        });

        await wrapper.vm.$nextTick();
        // Assigned permissions should have a check icon
        icons = wrapper.findAll('i').filter(i => !i.classes('hidden'));
        expect(icons.at(0).classes()).toContain('fa-check-circle');
        expect(icons.at(1).classes()).toContain('fa-check-circle');

        // Inherited permissions should not have a check icon
        wrapper.setProps({
            type: 'inherited',
            user:  { principal: 'authenticated', role: 'canViewAccessCopies', principal_display: 'authenticated' }
        });

        await wrapper.vm.$nextTick();
        icons = wrapper.findAll('i.fa-check-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(0);
    });

    it("displays 'effective permissions' icons for most restrictive 'patron' permission", async () => {
        wrapper.setProps({
            displayRoles: {
                inherited: { roles: [
                        { principal: 'everyone', role: 'canViewAccessCopies', principal_display: 'everyone' },
                        { principal: 'authenticated', role: 'canViewOriginals', principal_display: 'authenticated' }
                    ],
                    embargo: null,
                    deleted: false
                },
                assigned: {
                    roles: [
                        { principal: 'everyone', role: 'none', principal_display: 'patron' },
                        { principal: 'authenticated', role: 'none', principal_display: 'patron' }
                    ],
                    embargo: null,
                    deleted: false
                }
            },
            type: 'assigned',
            user:  { principal: 'authenticated', role: 'none', principal_display: 'authenticated' }
        });

        await wrapper.vm.$nextTick();
        // Assigned permissions should have a check icon
        icons = wrapper.findAll('i').filter(i => !i.classes('hidden'));
        expect(icons.at(0).classes()).toContain('fa-check-circle');
        expect(icons.at(1).classes()).toContain('fa-check-circle');


        // Inherited permissions should not have a check icon
        wrapper.setProps({
            type: 'inherited'
        });

        await wrapper.vm.$nextTick();
        icons = wrapper.findAll('i.fa-check-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(0);
    });

    it("displays an embargoed icon if an item is embargoed", async () => {
        wrapper.setProps({
            displayRoles: {
                inherited: { roles: [
                        { principal: 'everyone', role: 'canViewMetadata', principal_display: 'everyone' },
                        { principal: 'authenticated', role: 'none', principal_display: 'authenticated' }
                    ],
                    embargo: null,
                    deleted: false
                },
                assigned: {
                    roles: [
                        { principal: 'everyone', role: 'canViewOriginals', principal_display: 'everyone' },
                        { principal: 'authenticated', role: 'canViewAccessCopies', principal_display: 'authenticated' }
                    ],
                    embargo: getMilliseconds(new Date()),
                    deleted: false
                }
            },
            user:  { principal: 'authenticated', role: 'canViewAccessCopies', principal_display: 'authenticated' }
        });

        await wrapper.vm.$nextTick();
        icons = wrapper.findAll('div.circle').filter(i => !i.classes('hidden'));
        expect(icons.at(0).classes()).toContain('circle');
    });

    it("does not display an embargo icon if an item is not embargoed", () => {
        wrapper.setProps({
            user:  { principal: 'authenticated', role: 'canViewAccessCopies', principal_display: 'authenticated' }
        });

        icons = wrapper.findAll('div.circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toBe(0);
    });

    it("displays a deleted icon if an item is marked for deletion", async () => {
        wrapper.setProps({
            displayRoles: {
                inherited: { roles: [
                        { principal: 'everyone', role: 'canViewMetadata', principal_display: 'everyone' },
                        { principal: 'authenticated', role: 'none', principal_display: 'authenticated' }
                    ],
                    embargo: null,
                    deleted: false
                },
                assigned: {
                    roles: [
                        { principal: 'everyone', role: 'canViewOriginals', principal_display: 'everyone' },
                        { principal: 'authenticated', role: 'canViewAccessCopies', principal_display: 'authenticated' }
                    ],
                    embargo: null,
                    deleted: true
                }
            },
            user:  { principal: 'authenticated', role: 'canViewAccessCopies', principal_display: 'authenticated' }
        });

        await wrapper.vm.$nextTick();
        icons = wrapper.findAll('i').filter(i => !i.classes('hidden'));
        expect(icons.at(0).classes()).toContain('fa-times-circle');
    });

    it("does not display a deleted icon if an item is not marked for deletion", async () => {
        wrapper.setProps({
            user:  { principal: 'authenticated', role: 'canViewAccessCopies', principal_display: 'authenticated' }
        });

        await wrapper.vm.$nextTick();
        icons = wrapper.findAll('i.fa-times-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toBe(0);
    });
});