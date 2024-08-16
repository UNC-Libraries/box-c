import { shallowMount } from '@vue/test-utils';
import patronDisplayRow from '@/components/patronDisplayRow.vue';

let wrapper, icons, columns, principal, role;

describe('patronRoles.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(patronDisplayRow, {
            props: {
                containerType: 'Folder',
                userType: 'patron',
                user: { principal: 'everyone', role: 'canViewOriginals', type: 'assigned', deleted: false, embargo: false }
            }
        });

        columns = wrapper.findAll('td');
        principal = columns[0];
        role = columns[1];
    });

    afterEach(() => {
        wrapper = null;
    });

    it("displays patron roles", () => {
        wrapper = shallowMount(patronDisplayRow, {
            props: {
                containerType: 'Folder',
                userType: 'patron',
                user: { principal: 'authenticated', role: 'canViewMetadata', type: 'assigned', deleted: false, embargo: false }
            }
        });

        columns = wrapper.findAll('td');
        principal = columns[0];
        role = columns[1];

        expect(principal.text()).toMatch(/^authenticated/i);
        expect(role.text()).toMatch(/^Metadata.Only/i);
    });

    it("displays an override note", () => {
        wrapper = shallowMount(patronDisplayRow, {
            props: {
                containerType: 'Folder',
                userType: 'patron',
                user: { principal: 'authenticated', role: 'canViewMetadata', type: 'inherited', deleted: false, embargo: false }
            }
        });

        columns = wrapper.findAll('td');
        principal = columns[0];
        role = columns[1];

        expect(principal.text()).toMatch(/^authenticated/i);
        expect(role.text()).toMatch(/^Metadata.Only.\(Overridden.by.parent\)/i);
    });

    it("does not display an override note if patron access is set to 'inherit from parent'", () => {
        wrapper = shallowMount(patronDisplayRow, {
            props: {
                containerType: 'Folder',
                userType: 'parent',
                user: { principal: 'authenticated', role: 'canViewMetadata', type: 'inherited', deleted: false, embargo: false }
            }
        });

        columns = wrapper.findAll('td');
        principal = columns[0];
        role = columns[1];

        expect(principal.text()).toMatch(/^authenticated/i);
        expect(role.text()).not.toMatch(/^Metadata.Only.\(Overridden.by.parent\)/i);
        expect(role.text()).toMatch(/^Metadata.Only/i);
    });

    it("does not display an override note for assigned permissions", () => {
        wrapper = shallowMount(patronDisplayRow, {
            props: {
                containerType: 'Folder',
                userType: 'patron',
                user: { principal: 'authenticated', role: 'canViewMetadata', type: 'assigned', deleted: false, embargo: false }
            }
        });

        columns = wrapper.findAll('td');
        principal = columns[0];
        role = columns[1];

        expect(principal.text()).toMatch(/^authenticated/i);
        expect(role.text()).not.toMatch(/^Metadata.Only.\(Overridden.by.parent\)/i);
        expect(role.text()).toMatch(/^Metadata.Only/i);
    });

    it("displays public assigned patron roles", () => {
        expect(principal.text()).toMatch(/^Public.Users/i);
        expect(role.text()).toMatch(/^All.of.this.Folder/i);
    });

    it("displays staff roles", () => {
        wrapper = shallowMount(patronDisplayRow, {
            propsData: {
                containerType: 'Folder',
                userType: 'staff',
                user: { principal: 'staff', role: 'none', type: 'assigned', deleted: false, embargo: false }
            }
        });

        columns = wrapper.findAll('td');
        principal = columns[0];
        role = columns[1];

        expect(principal.text()).toMatch(/^No.Patron.Access/i);
        expect(role.text()).toMatch(/^N\/A/);
    });

    it("display a 'more info' icon for 'Public Users' users", () => {
        icons = wrapper.findAll('i.fa-question-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(1);
        expect(icons[0].classes()).toContain('fa-question-circle');
    });

    it("does not display a 'more info' icon for 'authenticated' users", async () => {
        await wrapper.setProps({
            user: { principal: 'authenticated', role: 'canViewOriginals', type: 'assigned', deleted: false, embargo: false }
        });

        icons = wrapper.findAll('i.fa-question-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(0);
    });

    it("does not display a 'more info' icon for 'Staff' users", async () => {
        await wrapper.setProps({
            user: { principal: 'staff', role: 'none', type: 'assigned', deleted: false, embargo: false }
        });

        icons = wrapper.findAll('i.fa-question-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(0);
    });

    it("displays an embargoed icon if an item is embargoed", async () => {
        await wrapper.setProps({
            user: { principal: 'authenticated', role: 'canViewMetadata', type: 'assigned', deleted: false, embargo: true }
        });

        icons = wrapper.findAll('div.circle').filter(i => !i.classes('hidden'));
        expect(icons[0].classes()).toContain('circle');
    });

    it("does not display an embargo icon if an item is not embargoed", async () => {
        await wrapper.setProps({
            user: { principal: 'authenticated', role: 'canViewAccessCopies', type: 'assigned', deleted: false, embargo: false }
        });

        icons = wrapper.findAll('div.circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toBe(0);
    });

    it("displays a deleted icon if an item is marked for deletion", async () => {
        await wrapper.setProps({
            user: { principal: 'authenticated', role: 'canViewAccessCopies', type: 'assigned', deleted: true, embargo: false }
        });

        icons = wrapper.findAll('i').filter(i => !i.classes('hidden'));
        expect(icons[0].classes()).toContain('fa-times-circle');
    });

    it("does not display a deleted icon if an item is not marked for deletion", async () => {
       await wrapper.setProps({
            user: { principal: 'authenticated', role: 'canViewAccessCopies', type: 'assigned', deleted: false, embargo: false }
        });

        icons = wrapper.findAll('i.fa-times-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toBe(0);
    });
});