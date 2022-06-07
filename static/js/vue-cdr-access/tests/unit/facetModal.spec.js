import { shallowMount } from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import moxios from 'moxios'
import facetModal from '@/components/facetModal.vue';
import searchWrapper from "@/components/searchWrapper.vue";
import {createI18n} from "vue-i18n";
import translations from "@/translations";

describe('modalMetadata.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    let router, wrapper;

    beforeEach(async () => {
        router = createRouter({
            history: createWebHistory(process.env.BASE_URL),
            routes: [
                {
                    path: '/search/:uuid?/',
                    name: 'searchRecords',
                    component: searchWrapper
                }
            ]
        });

        wrapper = shallowMount(facetModal, {
            global: {
                plugins: [router, i18n]
            },
            props: {
                facetId: 'subject',
                facetName: 'Subject'
            }
        });

        it("opens the modal when clicked", () => {

        });
    });
});