import { shallowMount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import viewType from '@/components/viewType.vue'
import displayWrapper from "@/components/displayWrapper";
import {createI18n} from "vue-i18n";
import translations from "@/translations";

let wrapper, btns, router;

describe('viewType.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        router = createRouter({
            history: createWebHistory(),
            routes: [
                {
                    path: '/record/:uuid/',
                    name: 'displayRecords',
                    component: displayWrapper
                }
            ]
        });
        sessionStorage.clear();
        wrapper = shallowMount(viewType, {
            global: {
                plugins: [router, i18n]
            }
        });

        btns = wrapper.findAll('#browse-btns i');
    });

    afterEach(() => router = null);

    it("sets a browse type when clicked", async () => {
        await router.push('/record/1234/?browse_type=list-display');
        await btns[1].trigger('click');
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.browse_type).toEqual(encodeURIComponent('gallery-display'));

        await btns[0].trigger('click');
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.browse_type).toEqual(encodeURIComponent('list-display'));
    });

    it("sets the browse type in sessionStorage when clicked", async () => {
        await router.push('/record/1234/?browse_type=list-display');
        const KEY = 'browse-type';
        await btns[1].trigger('click');
        await flushPromises();
        expect(sessionStorage.setItem).toHaveBeenLastCalledWith(KEY, 'gallery-display');

        await btns[0].trigger('click');
        await flushPromises();
        expect(sessionStorage.setItem).toHaveBeenLastCalledWith(KEY, 'list-display');
    });

    it("highlights the correct selected browse type", async () => {
        await router.push('/record/1234');
        await btns[1].trigger('click');
        await flushPromises();
        expect(btns[0].classes()).not.toContain('is-selected');
        expect(btns[1].classes()).toContain('is-selected');

        await btns[0].trigger('click');
        await flushPromises();
        expect(btns[0].classes()).toContain('is-selected');
        expect(btns[1].classes()).not.toContain('is-selected');
    });

    it("sets browse_type from url, if present", async () => {
        await router.push('/record/1234/?browse_type=list-display');
        expect(wrapper.vm.browse_type).toEqual('list-display');

        await router.push('/record/1234/?browse_type=gallery-display');
        expect(wrapper.vm.browse_type).toEqual('gallery-display');
    });
});