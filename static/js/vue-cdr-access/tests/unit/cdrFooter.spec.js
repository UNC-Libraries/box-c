import { mount } from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import cdrFooter from '@/components/cdrFooter.vue';
import moxios from "moxios";

let wrapper, router;

describe('cdrFooter.vue', () => {

    beforeEach(() => {
        moxios.install();

        router = createRouter({
            history: createWebHistory(process.env.BASE_URL),
            routes: [
                {
                    component: cdrFooter
                }
            ]
        });
        wrapper = mount(cdrFooter, {
            global: {
                plugins: [router]
            }
        });
    });

    afterEach(function () {
        moxios.uninstall();
    });

    it("loads the cdrFooter", () => {
        wrapper.find('cdrFooter');
        expect(wrapper.find('cdrFooter').exists()).toBe(true);
    });

});