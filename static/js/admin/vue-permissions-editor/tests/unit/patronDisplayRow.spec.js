import { createLocalVue, shallowMount } from '@vue/test-utils';
import patronDisplayRow from '@/components/patronDisplayRow.vue';
import {getMilliseconds} from "date-fns";

const localVue = createLocalVue();
const STAFF_ONLY_ROLE_TEXT = '\u2014';
const staff_user = { principal: 'Staff', role: STAFF_ONLY_ROLE_TEXT };
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
                            { principal: 'everyone', role: 'canViewMetadata' },
                            { principal: 'authenticated', role: 'canViewOriginals' }
                        ],
                        embargo: null,
                        deleted: false
                    },
                    assigned: {
                        roles: [
                            { principal: 'everyone', role: 'canAccess' },
                            { principal: 'authenticated', role: 'canViewOriginals' }
                        ],
                        embargo: null,
                        deleted: false
                    }
                },
                possibleRoles: [
                    { text: STAFF_ONLY_ROLE_TEXT , role: STAFF_ONLY_ROLE_TEXT },
                    { text: 'No Access', role: 'none' },
                    { text: 'Can Discover', role: 'canDiscover' },
                    { text: 'Metadata Only', role: 'canViewMetadata' },
                    { text: 'Access Copies', role: 'canViewAccessCopies' },
                    { text: 'Can View Originals', role: 'canViewOriginals' },
                    { text: 'All of this folder', role: 'canAccess' }
                ],
                type: 'assigned',
                user: { principal: 'everyone', role: 'canAccess' }
            }
        });

        columns = wrapper.findAll('td');
        permission_type = columns.at(0);
        public_principal = columns.at(1);
        public_role = columns.at(2);
    });

    it("displays public assigned patron roles", () => {
        expect(permission_type.text()).toBe('From assigned');
        expect(public_principal.text()).toMatch(/^Everyone/);
        expect(public_role.text()).toMatch(/^All.of.this.folder/);
    });

    it("displays onyen assigned patron roles", () => {
        wrapper.setProps({
           user:  { principal: 'authenticated', role: 'canViewOriginals' }
        });

        expect(permission_type.text()).toBe('From assigned');
        expect(public_principal.text()).toMatch(/^Authenticated/);
        expect(public_role.text()).toMatch(/^Can.View.Originals/);
    });

    it("displays staff only assigned patron roles", () => {
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

        expect(permission_type.text()).toBe('From assigned');
        expect(public_principal.text()).toMatch(/^Staff/);
        expect(public_role.text()).toMatch(dash_regex);
    });

    it("displays public inherited patron roles", () => {
        wrapper.setProps({
            type: 'inherited',
            user: { principal: 'everyone', role: 'canViewMetadata' }
        });

        expect(permission_type.text()).toBe('From inherited');
        expect(public_principal.text()).toMatch(/^Everyone/);
        expect(public_role.text()).toMatch(/^Metadata.Only/);
    });

    it("displays onyen inherited patron roles", () => {
        wrapper.setProps({
            type: 'inherited',
            user:  { principal: 'authenticated', role: 'canViewOriginals' }
        });

        expect(permission_type.text()).toBe('From inherited');
        expect(public_principal.text()).toMatch(/^Authenticated/);
        expect(public_role.text()).toMatch(/^Can.View.Originals/);
    });

    it("displays staff only inherited patron roles", () => {
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

        expect(permission_type.text()).toBe('From inherited');
        expect(public_principal.text()).toMatch(/^Staff/);
        expect(public_role.text()).toMatch(dash_regex);
    });

    it("display a 'more info' icon for 'Everyone' users", () => {
        icons = wrapper.findAll('i').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(1);
        expect(icons.at(0).classes()).toContain('fa-question-circle');
    });

    it("does not display a 'more info' icon for 'authenticated' users", () => {
        wrapper.setProps({ user: { principal: 'authenticated', role: 'canAccess' } });
        icons = wrapper.findAll('i.fa-question-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(0);
    });

    it("does not display a 'more info' icon for 'Staff' users", () => {
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
        icons = wrapper.findAll('i.fa-question-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(0);
    });

    it("displays 'effective permissions' icons for most restrictive permission", () => {
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

        icons = wrapper.findAll('i').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(2);
        expect(icons.at(0).classes()).toContain('fa-check-circle');
        expect(icons.at(1).classes()).toContain('fa-check-circle');
    });

    it("displays 'effective permissions' icons for most restrictive public permission", () => {
        wrapper.setProps({
            displayRoles: {
                inherited: { roles: [
                        { principal: 'everyone', role: 'none' },
                        { principal: 'authenticated', role: 'none' }
                    ],
                    embargo: null,
                    deleted: false
                },
                assigned: {
                    roles: [
                        { principal: 'everyone', role: 'canAccess' },
                        { principal: 'authenticated', role: 'canViewOriginals' }
                    ],
                    embargo: null,
                    deleted: true
                }
            },
            type: 'assigned',
            user:  { principal: 'everyone', role: 'canAccess' }
        });

        // Assigned permissions should not have a check icon
        icons = wrapper.findAll('i.fa-check-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(0);

        // Inherited permissions should have a check icon
        wrapper.setProps({
            type: 'inherited',
            user:  { principal: 'everyone', role: 'none' }
        });
        icons = wrapper.findAll('i').filter(i => !i.classes('hidden') && !i.classes('fa-question-circle'));
        expect(icons.at(0).classes()).toContain('fa-check-circle');
        expect(icons.at(1).classes()).toContain('fa-check-circle');
    });

    it("displays 'effective permissions' icons for most restrictive onyen permission", () => {
        wrapper.setProps({
            displayRoles: {
                inherited: { roles: [
                        { principal: 'everyone', role: 'none' },
                        { principal: 'authenticated', role: 'canViewOriginals' }
                    ],
                    embargo: null,
                    deleted: false
                },
                assigned: {
                    roles: [
                        { principal: 'everyone', role: 'canAccess' },
                        { principal: 'authenticated', role: 'none' }
                    ],
                    embargo: null,
                    deleted: false
                }
            },
            type: 'assigned',
            user:  { principal: 'authenticated', role: 'none' }
        });

        // Assigned permissions should have a check icon
        icons = wrapper.findAll('i').filter(i => !i.classes('hidden'));
        expect(icons.at(0).classes()).toContain('fa-check-circle');
        expect(icons.at(1).classes()).toContain('fa-check-circle');


        // Inherited permissions should not have a check icon
        wrapper.setProps({
            type: 'inherited',
            user:  { principal: 'authenticated', role: 'canViewOriginals' }
        });

        icons = wrapper.findAll('i.fa-check-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(0);
    });

    it("displays an embargoed icon if an item is embargoed", () => {
       wrapper.setProps({
           displayRoles: {
               inherited: { roles: [
                       { principal: 'everyone', role: 'canViewMetadata' },
                       { principal: 'authenticated', role: 'canDiscover' }
                   ],
                   embargo: null,
                   deleted: false
               },
               assigned: {
                   roles: [
                       { principal: 'everyone', role: 'canAccess' },
                       { principal: 'authenticated', role: 'canViewOriginals' }
                   ],
                   embargo: getMilliseconds(new Date()),
                   deleted: false
               }
           },
           user:  { principal: 'authenticated', role: 'canViewOriginals' }
       });

        icons = wrapper.findAll('i').filter(i => !i.classes('hidden'));
        expect(icons.at(0).classes()).toContain('fa-circle');
    });

    it("does not display an embargo icon if an item is not embargoed", () => {
        wrapper.setProps({
            user:  { principal: 'authenticated', role: 'canViewOriginals' }
        });

        icons = wrapper.findAll('i.fa-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toBe(0);
    });

    it("displays a deleted icon if an item is marked for deletion", () => {
        wrapper.setProps({
            displayRoles: {
                inherited: { roles: [
                        { principal: 'everyone', role: 'canViewMetadata' },
                        { principal: 'authenticated', role: 'canDiscover' }
                    ],
                    embargo: null,
                    deleted: false
                },
                assigned: {
                    roles: [
                        { principal: 'everyone', role: 'canAccess' },
                        { principal: 'authenticated', role: 'canViewOriginals' }
                    ],
                    embargo: null,
                    deleted: true
                }
            },
            user:  { principal: 'authenticated', role: 'canViewOriginals' }
        });

        icons = wrapper.findAll('i').filter(i => !i.classes('hidden'));
        expect(icons.at(0).classes()).toContain('fa-times-circle');
    });

    it("does not display a deleted icon if an item is not marked for deletion", () => {
        wrapper.setProps({
            user:  { principal: 'authenticated', role: 'canViewOriginals' }
        });

        icons = wrapper.findAll('i.fa-times-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toBe(0);
    });
});