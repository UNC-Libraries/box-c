import { createLocalVue, shallowMount } from '@vue/test-utils'
import VueRouter from 'vue-router';
import viewType from '@/components/viewType.vue'
const localVue = createLocalVue();
localVue.use(VueRouter);

const router = new VueRouter({
    routes: [
        {
            path: '/record/uuid1234',
            name: 'displayRecords'
        }
    ]
});

let wrapper, btns;

describe('viewType.vue', () => {
    beforeEach(() => {
        sessionStorage.clear();
        wrapper = shallowMount(viewType, {
            localVue,
            router
        });

        btns = wrapper.findAll('#browse-btns i');
    });

    it("sets a browse type when clicked", () => {
        btns.at(1).trigger('click');
        expect(wrapper.vm.$router.currentRoute.query.browse_type).toEqual(encodeURIComponent('list-display'));

        btns.at(0).trigger('click');
        expect(wrapper.vm.$router.currentRoute.query.browse_type).toEqual(encodeURIComponent('gallery-display'));
    });

    it("sets the browse type in sessionStorage when clicked", () => {
        const KEY = 'browse-type';
        btns.at(1).trigger('click');
        expect(sessionStorage.setItem).toHaveBeenLastCalledWith(KEY, 'list-display');

        btns.at(0).trigger('click');
        expect(sessionStorage.setItem).toHaveBeenLastCalledWith(KEY, 'gallery-display');
    });

    it("highlights the correct selected browse type", () => {
        btns.at(1).trigger('click');
        expect(btns.at(0).classes()).not.toContain('is-selected');
        expect(btns.at(1).classes()).toContain('is-selected');

        btns.at(0).trigger('click');
        expect(btns.at(0).classes()).toContain('is-selected');
        expect(btns.at(1).classes()).not.toContain('is-selected');
    });

    it("sets browse_type from url, if present", () => {
        expect(wrapper.vm.browse_type).toEqual('gallery-display');

        wrapper.vm.$router.currentRoute.query.browse_type = 'list-display';
        wrapper = shallowMount(viewType, {
            localVue,
            router
        });
        expect(wrapper.vm.browse_type).toEqual('list-display');
    });
});