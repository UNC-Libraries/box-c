import {RouterLinkStub, shallowMount} from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import fileList from '@/components/full_record/fileList.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';

let briefObject = {
    filesize: 694904,
    format: [
        "Image"
    ],
    title: "beez",
    type: "File",
    fileDesc: [
        "JPEG Image"
    ],
    parentCollectionName: "testCollection",
    datastream: [
        "original_file|image/jpeg|beez||694904|urn:sha1:0d48dadb5d61ae0d41b4998280a3c39577a2f94a||2048x1536",
        "jp2|image/jp2|4db695c0-5fd5-4abf-9248-2e115d43f57d.jp2|jp2|2189901|||"
    ],
    parentCollectionId: "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
    permissions: [
        "viewAccessCopies",
        "viewMetadata"
    ],
    groupRoleMap: {
        authenticated: [
            "canViewAccessCopies"
        ],
        everyone: [
            "canViewAccessCopies"
        ]
    },
    id: "4db695c0-5fd5-4abf-9248-2e115d43f57d",
    fileType: [
        "image/jpeg"
    ],
    status: [
        "Patron Settings",
        "Inherited Patron Settings"
    ]
};

let wrapper, router;

describe('fileList.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        router = createRouter({
            history: createWebHistory(process.env.BASE_URL),
            routes: [
                {
                    path: '/record/:uuid',
                    name: 'displayRecords',
                    component: displayWrapper
                }
            ]
        });

        wrapper = shallowMount(fileList, {
            global: {
                plugins: [i18n, router],
                stubs: {
                    RouterLink: RouterLinkStub
                }
            },

            props: {
                childCount: 3,
                editAccess: true,
                viewOriginalAccess: false,
                workId: 'e2f0d544-4f36-482c-b0ca-ba11f1251c01',
            }
        });
    });

    it("contains a table of files", () => {
        expect(wrapper.findComponent({ name: 'dataTable' }).exists()).toBe(true);
    });

    it("sets 'badge' options for thumbnails", () => {
        expect(wrapper.vm.showBadge({
                status: ['Marked for Deletion'],
                groupRoleMap: { authenticated: ['canViewOriginals'], everyone: ['canViewAccessCopies'] }
            })).toEqual({ markDeleted: true, restricted: false });
        expect(wrapper.vm.showBadge({
            status: [''],
            groupRoleMap: { authenticated: ['none'], everyone: ['none'] }
        })).toEqual({ markDeleted: false, restricted: true });
    });

    it("includes edit column when editAccess is true", () => {
        const defs = wrapper.vm.columnDefs;
        expect(wrapper.vm.columns.length).toBeGreaterThan(6);
        expect(defs.some(def => def.targets === 6)).toBe(true);
    });

    it("builds aria label text with title", () => {
        const label = wrapper.vm.ariaLabelText({ title: 'Sample Title' });
        expect(label).toContain('Sample Title');
    });

    it("showBadge returns neither badge when item is not deleted and is public", () => {
        expect(wrapper.vm.showBadge({
            status: [],
            groupRoleMap: { everyone: ['canViewAccessCopies'] }
        })).toEqual({ markDeleted: false, restricted: false });
    });

    it("showBadge returns both flags correctly for deleted and restricted item", () => {
        expect(wrapper.vm.showBadge({
            status: ['Marked for Deletion'],
            groupRoleMap: { authenticated: ['none'], everyone: ['none'] }
        })).toEqual({ markDeleted: true, restricted: true });
    });

    it("does not include edit column when editAccess is false", () => {
        const noEditWrapper = shallowMount(fileList, {
            global: {
                plugins: [i18n, router],
                stubs: { RouterLink: RouterLinkStub }
            },
            props: {
                childCount: 3,
                editAccess: false,
                viewOriginalAccess: false,
                workId: 'e2f0d544-4f36-482c-b0ca-ba11f1251c01'
            }
        });
        expect(noEditWrapper.vm.columns.length).toBe(6);
        expect(noEditWrapper.vm.columnDefs.some(def => def.targets === 6)).toBe(false);
    });

    it("ajaxOptions dataSrc returns metadata array for Work resourceType", () => {
        const fakeResponse = { metadata: [{ id: 'a' }], container: { id: 'b' } };
        const result = wrapper.vm.ajaxOptions.dataSrc(fakeResponse);
        expect(result).toEqual([{ id: 'a' }]);
    });

    it("ajaxOptions dataSrc wraps container in array for non-Work resourceType", async () => {
        await wrapper.setProps({ resourceTypeProp: 'File' });
        const fakeResponse = { metadata: [{ id: 'a' }], container: { id: 'b' } };
        const result = wrapper.vm.ajaxOptions.dataSrc(fakeResponse);
        expect(result).toEqual([{ id: 'b' }]);
    });

    it("ajaxOptions data callback maps sort column and direction correctly", () => {
        const d = { search: { value: 'test' }, order: [{ column: 1, dir: 'asc' }], length: 0 };
        wrapper.vm.ajaxOptions.data(d);
        expect(d.anywhere).toBe('test');
        expect(d.sort).toBe('title,normal');
        expect(d.rollup).toBe(false);
    });

    it("ajaxOptions data callback maps descending sort correctly", () => {
        const d = { search: { value: '' }, order: [{ column: 2, dir: 'desc' }], length: 0 };
        wrapper.vm.ajaxOptions.data(d);
        expect(d.sort).toBe('fileFormatDescription,reverse');
    });

    it("ajaxOptions data callback does not set sort when order is empty", () => {
        const d = { search: { value: '' }, order: [], length: 0 };
        wrapper.vm.ajaxOptions.data(d);
        expect(d.sort).toBeUndefined();
    });

    it("ajaxOptions dataFilter transforms resultCount into recordsTotal and recordsFiltered", () => {
        const input = JSON.stringify({ resultCount: 42, metadata: [] });
        const output = JSON.parse(wrapper.vm.ajaxOptions.dataFilter(input));
        expect(output.recordsTotal).toBe(42);
        expect(output.recordsFiltered).toBe(42);
    });

    it("title column renderer returns a link to the record with the correct aria label", () => {
        const titleDef = wrapper.vm.columnDefs.find(d => d.targets === 1);
        const row = { id: 'abc-123', title: 'My File' };
        const html = titleDef.render(null, 'display', row);
        expect(html).toContain('href="/record/abc-123"');
        expect(html).toContain('My File');
        expect(html).toContain('aria-label=');
    });

    it("file type column renderer returns the file type for the row", () => {
        const typeDef = wrapper.vm.columnDefs.find(d => d.targets === 2);
        const row = { fileDesc: ['JPEG Image'], fileType: ['image/jpeg'] };
        expect(typeDef.render(null, 'display', row)).toBe('JPEG Image');
    });

    it("filesize column renderer returns the formatted size from the original_file datastream", () => {
        const sizeDef = wrapper.vm.columnDefs.find(d => d.targets === 3 && typeof d.render === 'function');
        const row = { datastream: ['original_file|image/jpeg|beez||694904|||'] };
        const result = sizeDef.render(null, 'display', row);
        expect(result).toContain('KB');
    });

    it("view link column renderer returns a link with a search-plus icon", () => {
        const viewDef = wrapper.vm.columnDefs.find(d => d.targets === 4);
        const row = { id: 'abc-123', title: 'My File' };
        const html = viewDef.render(null, 'display', row);
        expect(html).toContain('href="/record/abc-123"');
        expect(html).toContain('fa-search-plus');
    });

    it("thumbnail column renderer shows default icon when no thumbnail_url is present", () => {
        const thumbDef = wrapper.vm.columnDefs.find(d => d.targets === 0);
        const row = { id: 'abc-123', title: 'My File', status: [], groupRoleMap: { everyone: ['canViewAccessCopies'] } };
        const html = thumbDef.render(null, 'display', row);
        expect(html).toContain('fa-file');
        expect(html).not.toContain('<img');
    });

    it("thumbnail column renderer shows img tag when thumbnail_url is present and user has access", () => {
        const thumbDef = wrapper.vm.columnDefs.find(d => d.targets === 0);
        const row = {
            id: 'abc-123',
            title: 'My File',
            thumbnail_url: '/services/thumb/abc-123/large',
            status: [],
            permissions: ['viewAccessCopies'],
            groupRoleMap: { everyone: ['canViewAccessCopies'] }
        };
        const html = thumbDef.render(null, 'display', row);
        expect(html).toContain('<img');
        expect(html).toContain('/small');
    });

    it("thumbnail column renderer adds trash badge for deleted items", () => {
        const thumbDef = wrapper.vm.columnDefs.find(d => d.targets === 0);
        const row = {
            id: 'abc-123',
            title: 'My File',
            status: ['Marked for Deletion'],
            groupRoleMap: { everyone: ['canViewAccessCopies'] }
        };
        const html = thumbDef.render(null, 'display', row);
        expect(html).toContain('thumbnail-badge-trash');
        expect(html).toContain('fa-trash');
    });

    it("thumbnail column renderer adds lock badge for restricted items", () => {
        const thumbDef = wrapper.vm.columnDefs.find(d => d.targets === 0);
        const row = {
            id: 'abc-123',
            title: 'My File',
            status: [],
            groupRoleMap: { authenticated: ['none'], everyone: ['none'] }
        };
        const html = thumbDef.render(null, 'display', row);
        expect(html).toContain('thumbnail-badge-lock');
        expect(html).toContain('fa-lock');
    });

    it("edit column renderer returns a link to the admin describe page", () => {
        const editDef = wrapper.vm.columnDefs.find(d => d.targets === 6);
        const row = { id: 'abc-123', title: 'My File' };
        const html = editDef.render(null, 'display', row);
        expect(html).toContain('href="/admin/describe/abc-123"');
        expect(html).toContain('fa-edit');
    });

    it("tableOptions rowCallback adds deleted class to rows marked for deletion", () => {
        const row = document.createElement('tr');
        const data = { status: ['Marked for Deletion'], groupRoleMap: { everyone: ['canViewAccessCopies'] } };
        wrapper.vm.tableOptions.rowCallback(row, data);
        expect(row.classList.contains('deleted')).toBe(true);
    });

    it("tableOptions rowCallback does not add deleted class for non-deleted rows", () => {
        const row = document.createElement('tr');
        const data = { status: [], groupRoleMap: { everyone: ['canViewAccessCopies'] } };
        wrapper.vm.tableOptions.rowCallback(row, data);
        expect(row.classList.contains('deleted')).toBe(false);
    });
});