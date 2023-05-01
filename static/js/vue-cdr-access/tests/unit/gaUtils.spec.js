import {mount, RouterLinkStub} from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import advancedSearch from '@/components/advancedSearch.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import gaUtils from "../../src/mixins/gaUtils";
import store from '@/store';
import { createI18n } from "vue-i18n";
import translations from "@/translations";
import { $gtag } from '../fixtures/testHelpers';
import moxios from "moxios";

let wrapper;
describe('gaUtils', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    let router = createRouter({
        history: createWebHistory(process.env.BASE_URL),
        routes: [
            {
                path: '/record/:uuid',
                name: 'displayRecords',
                component: displayWrapper
            }
        ]
    });

    const pageEvent = jest.spyOn(gaUtils.methods, 'pageEvent');
    const pageView = jest.spyOn(gaUtils.methods, 'pageView');

    beforeEach(() => {
        jest.resetAllMocks();
        moxios.install();
    });

    afterEach(() => {
        moxios.uninstall()
    });

    it("sends pageviews to Google Analytics", () => {
        wrapper = mount(advancedSearch, {
            global: {
                plugins: [router, store, i18n],
                mocks: { $gtag },
                stubs: {
                    RouterLink: RouterLinkStub
                }
            }
        });

        expect(pageView).toHaveBeenCalledWith("Advanced Search");
    });

    it("sends events to Google Analytics", (done) => {
        const briefObj = {
            briefObject: {
                filesizeTotal: 35845559,
                added: "2023-03-07T14:47:46.863Z",
                counts: {
                    child: 1
                },
                format: [
                    "Audio"
                ],
                title: "Listen for real",
                type: "Work",
                fileDesc: [
                    "MP3"
                ],
                parentCollectionName: "testCollection",
                objectPath: [
                    {
                        pid: "collections",
                        name: "Content Collections Root",
                        container: true
                    },
                    {
                        pid: "353ee09f-a4ed-461e-a436-18a1bee77b01",
                        name: "testAdminUnit",
                        container: true
                    },
                    {
                        pid: "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                        name: "testCollection",
                        container: true
                    },
                    {
                        pid: "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
                        name: "Listen for real",
                        container: true
                    }
                ],
                parentCollectionId: "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                ancestorPath: [
                    {
                        id: "collections",
                        title: "collections"
                    },
                    {
                        id: "353ee09f-a4ed-461e-a436-18a1bee77b01",
                        title: "353ee09f-a4ed-461e-a436-18a1bee77b01"
                    },
                    {
                        id: "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                        title: "fc77a9be-b49d-4f4e-b656-1644c9e964fc"
                    }
                ],
                _version_: 1764089032947531800,
                permissions: [
                    "viewAccessCopies",
                    "viewOriginal",
                    "viewMetadata"
                ],
                groupRoleMap: {
                    authenticated: [
                        "canViewOriginals"
                    ],
                    everyone: [
                        "canViewOriginals"
                    ]
                },
                id: "e2f0d544-4f36-482c-b0ca-ba11f1251c01",
                updated: "2023-04-24T19:58:37.960Z",
                fileType: [
                    "audio/mpeg"
                ],
                status: [
                    "Public Access"
                ]
            }
        };
        moxios.stubRequest(new RegExp(`record/73bc003c-9603-4cd9-8a65-93a22520ef6a?.+`), {
            status: 200,
            response: JSON.stringify(briefObj)
        });

        const $route = {
            path: '/record/1234',
            name: 'displayRecords',
            query: { rows: 10 }
        };
        wrapper = mount(displayWrapper, {
            global: {
                plugins: [store, i18n],
                mocks: { $gtag, $route },
                stubs: {
                    RouterLink: RouterLinkStub
                }
            }
        });

        wrapper.vm.getBriefObject()

        moxios.wait(() => {
            expect(pageEvent).toHaveBeenCalledWith(briefObj);
            expect(pageView).toHaveBeenCalledWith(briefObj.title);
            done();
        })
    });
});