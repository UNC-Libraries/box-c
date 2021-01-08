import { createLocalVue, shallowMount } from '@vue/test-utils';
import patronDisplayRow from '@/components/patronDisplayRow.vue';

const localVue = createLocalVue();
let wrapper, icons, columns, principal, role;

describe('patronRoles.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(patronDisplayRow, {
            localVue,
            propsData: {
                containerType: 'Folder',
                userType: 'patron',
                user: { principal: 'everyone', role: 'canViewOriginals', type: 'assigned', deleted: false, embargo: false }
            }
        });

        columns = wrapper.findAll('td');
        principal = columns.at(0);
        role = columns.at(1);
    });

    it("displays patron roles", () => {
        wrapper = shallowMount(patronDisplayRow, {
            localVue,
            propsData: {
                containerType: 'Folder',
                userType: 'patron',
                user: { principal: 'authenticated', role: 'canViewMetadata', type: 'assigned', deleted: false, embargo: false }
            }
        });

        columns = wrapper.findAll('td');
        principal = columns.at(0);
        role = columns.at(1);

        expect(principal.text()).toMatch(/^authenticated/);
        expect(role.text()).toMatch(/^Metadata.Only/);
    });

    it("displays an override note", () => {
        wrapper = shallowMount(patronDisplayRow, {
            localVue,
            propsData: {
                containerType: 'Folder',
                userType: 'patron',
                user: { principal: 'authenticated', role: 'canViewMetadata', type: 'inherited', deleted: false, embargo: false }
            }
        });

        columns = wrapper.findAll('td');
        principal = columns.at(0);
        role = columns.at(1);

        expect(principal.text()).toMatch(/^authenticated/);
        expect(role.text()).toMatch(/^Metadata.Only.\(Overridden.by.parent\)/);
    });

    it("does not display an override note if patron access is set to 'inherit from parent'", () => {
        wrapper = shallowMount(patronDisplayRow, {
            localVue,
            propsData: {
                containerType: 'Folder',
                userType: 'parent',
                user: { principal: 'authenticated', role: 'canViewMetadata', type: 'inherited', deleted: false, embargo: false }
            }
        });

        columns = wrapper.findAll('td');
        principal = columns.at(0);
        role = columns.at(1);

        expect(principal.text()).toMatch(/^authenticated/);
        expect(role.text()).not.toMatch(/^Metadata.Only.\(Overridden.by.parent\)/);
        expect(role.text()).toMatch(/^Metadata.Only/);
    });

    it("does not display an override note for assigned permissions", () => {
        wrapper = shallowMount(patronDisplayRow, {
            localVue,
            propsData: {
                containerType: 'Folder',
                userType: 'patron',
                user: { principal: 'authenticated', role: 'canViewMetadata', type: 'assigned', deleted: false, embargo: false }
            }
        });

        columns = wrapper.findAll('td');
        principal = columns.at(0);
        role = columns.at(1);

        expect(principal.text()).toMatch(/^authenticated/);
        expect(role.text()).not.toMatch(/^Metadata.Only.\(Overridden.by.parent\)/);
        expect(role.text()).toMatch(/^Metadata.Only/);
    });

    it("displays public assigned patron roles", () => {
        expect(principal.text()).toMatch(/^Public.Users/);
        expect(role.text()).toMatch(/^All.of.this.Folder/);
    });

    it("displays staff roles", () => {
        wrapper = shallowMount(patronDisplayRow, {
            localVue,
            propsData: {
                containerType: 'Folder',
                userType: 'staff',
                user: { principal: 'staff', role: 'none', type: 'assigned', deleted: false, embargo: false }
            }
        });

        columns = wrapper.findAll('td');
        principal = columns.at(0);
        role = columns.at(1);

        expect(principal.text()).toMatch(/^No.Patron.Access/);
        expect(role.text()).toMatch(/^N\/A/);
    });

    it("display a 'more info' icon for 'Public Users' users", () => {
        icons = wrapper.findAll('i.fa-question-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(1);
        expect(icons.at(0).classes()).toContain('fa-question-circle');
    });

    it("does not display a 'more info' icon for 'authenticated' users", async () => {
        wrapper.setProps({
            user: { principal: 'authenticated', role: 'canViewOriginals', type: 'assigned', deleted: false, embargo: false }
        });

        await wrapper.vm.$nextTick();
        icons = wrapper.findAll('i.fa-question-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(0);
    });

    it("does not display a 'more info' icon for 'Staff' users", async () => {
        wrapper.setProps({
            user: { principal: 'staff', role: 'none', type: 'assigned', deleted: false, embargo: false }
        });

        await wrapper.vm.$nextTick();
        icons = wrapper.findAll('i.fa-question-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toEqual(0);
    });

    it("displays an embargoed icon if an item is embargoed", async () => {
        wrapper.setProps({
            user: { principal: 'authenticated', role: 'canViewMetadata', type: 'assigned', deleted: false, embargo: true }
        });

        await wrapper.vm.$nextTick();
        icons = wrapper.findAll('div.circle').filter(i => !i.classes('hidden'));
        expect(icons.at(0).classes()).toContain('circle');
    });

    it("does not display an embargo icon if an item is not embargoed", () => {
        wrapper.setProps({
            user: { principal: 'authenticated', role: 'canViewAccessCopies', type: 'assigned', deleted: false, embargo: false }
        });

        icons = wrapper.findAll('div.circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toBe(0);
    });

    it("displays a deleted icon if an item is marked for deletion", async () => {
        wrapper.setProps({
            user: { principal: 'authenticated', role: 'canViewAccessCopies', type: 'assigned', deleted: true, embargo: false }
        });

        await wrapper.vm.$nextTick();
        icons = wrapper.findAll('i').filter(i => !i.classes('hidden'));
        expect(icons.at(0).classes()).toContain('fa-times-circle');
    });

    it("does not display a deleted icon if an item is not marked for deletion", async () => {
        wrapper.setProps({
            user: { principal: 'authenticated', role: 'canViewAccessCopies', type: 'assigned', deleted: false, embargo: false }
        });

        await wrapper.vm.$nextTick();
        icons = wrapper.findAll('i.fa-times-circle').filter(i => !i.classes('hidden'));
        expect(icons.length).toBe(0);
    });
});