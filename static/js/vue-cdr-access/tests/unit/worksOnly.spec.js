import { shallowMount, flushPromises } from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import worksOnly from '@/components/worksOnly.vue';
import displayWrapper from '@/components/displayWrapper';
import {createI18n} from "vue-i18n";
import translations from "@/translations";


let wrapper, record_input, router;

describe('worksOnly.vue', () => {
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
        wrapper = shallowMount(worksOnly, {
            global: {
                plugins: [router, i18n]
            },
            props: {
                adminUnit: false
            },
            data() {
                return {
                    works_only: false
                }
            }
        });

        record_input = wrapper.find('input');
    });

    afterEach(() => router = null);

    it("does not display for admin unit records", async () => {
        await wrapper.setProps({
            adminUnit: true
        });
        expect(wrapper.find('#browse-display-type').exists()).toBe(false);
    });

    it("does display for non admin units", () => {
        expect(wrapper.find('#browse-display-type').exists()).toBe(true);
    });

    it("updates route to only show works if button is checked for a gallery view",  async () => {
        await router.push('/record/1234/?browse_type=gallery-display');
        await wrapper.setData({ works_only: false });
        await record_input.trigger('click');
        await flushPromises();
        expect(wrapper.vm.$router.currentRoute.value.query.types).toEqual('Work');
    });

    it("updates route to only show works and folders if button is unchecked for a gallery view", async  () => {
        await router.push('/record/1234/?browse_type=gallery-display');
        await wrapper.setData({ works_only: true });
        await record_input.trigger('click');
        await flushPromises();

        expect(wrapper.vm.$router.currentRoute.value.query.types).toEqual('Work,Folder,Collection');
    });

    it("updates route to only show works if button is checked for a list view", async() => {
        await router.push('/record/1234/?browse_type=list-display');
        await wrapper.setData({ works_only: false });
        await record_input.trigger('click');
        await flushPromises();

        expect(wrapper.vm.$router.currentRoute.value.query.types).toEqual('Work');
    });

    it("updates route to show works and folders if button is unchecked for a list view", async () => {
        await router.push('/record/1234/?browse_type=list-display');
        await wrapper.setData({ works_only: true });
        await record_input.trigger('click');
        await flushPromises();

        expect(wrapper.vm.$router.currentRoute.value.query.types).toEqual('Work,Folder,Collection');
    });

    afterEach(() => {
        // Make sure box is unchecked
        wrapper.setData({ works_only: false });
    });
});