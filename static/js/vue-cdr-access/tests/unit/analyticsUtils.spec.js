import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { mount, flushPromises, RouterLinkStub } from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import { createTestingPinia } from '@pinia/testing';
import { useAccessStore } from '@/stores/access';
import advancedSearch from '@/components/advancedSearch.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import { createI18n } from "vue-i18n";
import translations from "@/translations";

let wrapper, store, router;

describe('analyticsUtils', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        fetchMock.enableMocks();
        fetchMock.resetMocks();

        // Initialize window._mtm before each test
        window._mtm = [];

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
    });

    afterEach(() => {
        fetchMock.disableMocks();
        delete window._mtm;
        if (store) {
            store.$reset();
        }
        wrapper = null;
    });

    it("sends pageviews to analytics platforms", () => {
        wrapper = mount(advancedSearch, {
            global: {
                plugins: [router, i18n, createTestingPinia({
                    stubActions: false
                })],
                stubs: {
                    RouterLink: RouterLinkStub
                }
            }
        });

        store = useAccessStore();

        // Check window._mtm array for the pageView event
        const pageViewEvent = window._mtm.find(e =>
            e.event === 'pageViewEvent' &&
            e.name === 'Advanced Search'
        );

        expect(pageViewEvent).toBeDefined();
        expect(pageViewEvent.name).toBe('Advanced Search');
    });

    it("sends events to analytics platforms", async () => {
        const briefObj = {
            briefObject: {
                filesize: 35845559,
                added: "2023-03-07T14:47:46.863Z",
                counts: { child: 1 },
                format: ["Audio"],
                title: "Listen for real",
                type: "Work",
                fileDesc: ["MP3"],
                parentCollectionName: "testCollection",
                objectPath: [
                    { pid: "collections", name: "Content Collections Root", container: true },
                    { pid: "353ee09f-a4ed-461e-a436-18a1bee77b01", name: "testAdminUnit", container: true },
                    { pid: "fc77a9be-b49d-4f4e-b656-1644c9e964fc", name: "testCollection", container: true },
                    { pid: "e2f0d544-4f36-482c-b0ca-ba11f1251c01", name: "Listen for real", container: true }
                ],
                parentCollectionId: "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                ancestorPath: [
                    { id: "collections", title: "collections" },
                    { id: "353ee09f-a4ed-461e-a436-18a1bee77b01", title: "353ee09f-a4ed-461e-a436-18a1bee77b01" },
                    { id: "fc77a9be-b49d-4f4e-b656-1644c9e964fc", title: "fc77a9be-b49d-4f4e-b656-1644c9e964fc" }
                ],
                _version_: 1764089032947531800,
                permissions: ["viewAccessCopies", "viewOriginal", "viewReducedResImages", "viewMetadata"],
                groupRoleMap: {
                    authenticated: ["canViewOriginals"],
                    everyone: ["canViewOriginals"]
                },
                id: "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
                updated: "2023-04-24T19:58:37.960Z",
                fileType: ["audio/mpeg"],
                status: []
            },
            pageSubtitle: "Listen for real", // IMPORTANT: Component uses this for pageView
            resourceType: "Work"
        };

        // Mock the fetch response
        fetchMock.mockResponseOnce(JSON.stringify(briefObj));

        const $route = {
            path: '/record/e2f0d544-4f36-482c-b0ca-ba11f1251c01',
            name: 'displayRecords',
            query: { rows: 10 }
        };

        wrapper = mount(displayWrapper, {
            global: {
                plugins: [i18n, createTestingPinia({ stubActions: false })],
                mocks: { $route },
                stubs: {
                    RouterLink: RouterLinkStub,
                    // Stub all child components to speed up test
                    'header-small': true,
                    'admin-unit': true,
                    'collection-record': true,
                    'folder-record': true,
                    'aggregate-record': true,
                    'file-record': true,
                    'bread-crumbs': true,
                    'browse-search': true,
                    'browse-sort': true,
                    'clear-filters': true,
                    'works-only': true,
                    'view-type': true,
                    'facets': true,
                    'gallery-display': true,
                    'list-display': true,
                    'pagination': true
                }
            }
        });

        store = useAccessStore();

        // Wait for all async operations to complete
        await flushPromises();
        await new Promise(resolve => setTimeout(resolve, 50));

        // Debug output
        console.log('window._mtm:', window._mtm);
        console.log('container_info:', wrapper.vm.container_info);
        console.log('Fetch called:', fetchMock.mock.calls.length, 'times');

        // Verify events were pushed to window._mtm
        expect(window._mtm.length).toBeGreaterThan(0);

        // Check for recordPageView event
        expect(window._mtm).toEqual(
            expect.arrayContaining([
                expect.objectContaining({
                    event: 'recordPageView',
                    recordId: 'e2f0d544-4f36-482c-b0ca-ba11f1251c01',
                    recordTitle: 'Listen for real',
                    resourceType: 'Work',
                    parentCollection: 'testCollection'
                })
            ])
        );

        // Check for pageViewEvent
        expect(window._mtm).toEqual(
            expect.arrayContaining([
                expect.objectContaining({
                    event: 'pageViewEvent',
                    name: 'Listen for real'
                })
            ])
        );
    });
});