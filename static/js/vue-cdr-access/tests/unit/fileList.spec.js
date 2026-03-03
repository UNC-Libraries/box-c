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

    it("does not include edit column when editAccess is false", () => {
        const noEditWrapper = shallowMount(fileList, {
            global: { plugins: [i18n, router], stubs: { RouterLink: RouterLinkStub } },
            props: { childCount: 3, editAccess: false, viewOriginalAccess: false, workId: 'e2f0d544-4f36-482c-b0ca-ba11f1251c01' }
        });
        expect(noEditWrapper.vm.columns.length).toBe(6);
        expect(noEditWrapper.vm.columnDefs.some(def => def.targets === 6)).toBe(false);
    });

    it("builds aria label text with title", () => {
        const label = wrapper.vm.ariaLabelText({ title: 'Sample Title' });
        expect(label).toContain('Sample Title');
    });

    it("builds correct ajax URL from workId", () => {
        expect(wrapper.vm.ajaxOptions.url).toBe('/api/listJson/e2f0d544-4f36-482c-b0ca-ba11f1251c01?rows=10');
    });

    it("dataSrc returns metadata array for Work type", () => {
        const data = { metadata: [briefObject], container: {} };
        const result = wrapper.vm.ajaxOptions.dataSrc(data);
        expect(result).toBe(data.metadata);
    });

    it("dataSrc returns container array for non-Work type", () => {
        const noEditWrapper = shallowMount(fileList, {
            global: { plugins: [i18n, router], stubs: { RouterLink: RouterLinkStub } },
            props: { childCount: 1, editAccess: false, viewOriginalAccess: false, workId: 'abc', resourceTypeProp: 'File' }
        });
        const data = { metadata: [], container: briefObject };
        expect(noEditWrapper.vm.ajaxOptions.dataSrc(data)).toEqual([briefObject]);
    });

    it("dataFilter parses JSON and sets recordsTotal and recordsFiltered from resultCount", () => {
        const input = JSON.stringify({ resultCount: 42, metadata: [] });
        const output = JSON.parse(wrapper.vm.ajaxOptions.dataFilter(input));
        expect(output.recordsTotal).toBe(42);
        expect(output.recordsFiltered).toBe(42);
    });

    it("ajax data callback sets sort from order when order is present", () => {
        const data = { search: { value: 'foo' }, order: [{ column: 1, dir: 'asc' }], length: 0, rollup: true };
        wrapper.vm.ajaxOptions.data(data);
        expect(data.anywhere).toBe('foo');
        expect(data.sort).toBe('title,normal');
        expect(data.length).toBe(10);
        expect(data.rollup).toBe(false);
    });

    it("ajax data callback sets sort from order with desc direction", () => {
        const data = { search: { value: '' }, order: [{ column: 2, dir: 'desc' }] };
        wrapper.vm.ajaxOptions.data(data);
        expect(data.sort).toBe('fileFormatDescription,reverse');
    });

    it("ajax data callback does not set sort when order is empty", () => {
        const data = { search: { value: '' }, order: [] };
        wrapper.vm.ajaxOptions.data(data);
        expect(data.sort).toBeUndefined();
    });

    it("tableOptions has serverSide enabled", () => {
        expect(wrapper.vm.tableOptions.serverSide).toBe(true);
    });

    it("tableOptions rowCallback adds deleted class for marked-for-deletion rows", () => {
        const row = document.createElement('tr');
        wrapper.vm.tableOptions.rowCallback(row, {
            status: ['Marked for Deletion'],
            groupRoleMap: { authenticated: ['canViewOriginals'], everyone: ['canViewAccessCopies'] }
        });
        expect(row.classList.contains('deleted')).toBe(true);
    });

    it("tableOptions rowCallback does not add deleted class for normal rows", () => {
        const row = document.createElement('tr');
        wrapper.vm.tableOptions.rowCallback(row, {
            status: [],
            groupRoleMap: { authenticated: ['canViewOriginals'], everyone: ['canViewAccessCopies'] }
        });
        expect(row.classList.contains('deleted')).toBe(false);
    });

    it("showBadge returns restricted true when everyone has no access", () => {
        expect(wrapper.vm.showBadge({
            status: [],
            groupRoleMap: { authenticated: ['none'], everyone: ['none'] }
        })).toEqual({ markDeleted: false, restricted: true });
    });

    it("showBadge returns both false for fully accessible non-deleted object", () => {
        expect(wrapper.vm.showBadge({
            status: [],
            groupRoleMap: { authenticated: ['canViewOriginals'], everyone: ['canViewAccessCopies'] }
        })).toEqual({ markDeleted: false, restricted: false });
    });

    it("resourceType computed returns resourceTypeProp value", () => {
        expect(wrapper.vm.resourceType).toBe('Work');
    });
});