import { createLocalVue, shallowMount } from '@vue/test-utils';
import VueRouter from 'vue-router';
import browseFilters from '@/components/browseFilters.vue';

const localVue = createLocalVue();
localVue.use(VueRouter);
const router = new VueRouter({
    routes: [
        {
            path: '/record/uuid1234',
            name: 'browseDisplay'
        }
    ]
});

let wrapper;
let record;

describe('browseFilters.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(browseFilters, {
            localVue,
            router,
            propsData: {
                browseType: 'gallery-display',
                containerType: 'Collection'
            }
        });

        wrapper.setData({
            filtered: false,
            update_params: {}
        });

        record = wrapper.find('.checkbox');

        // trigger both events so v-model updates
        record.trigger('click');
        record.trigger('change');
    });

    it("does not display a checkbox for non-collections", () => {
        wrapper.setProps({containerType: 'AdminUnit' });
        expect(wrapper.find('.filter-format').contains('.checkbox')).toBe(false);
    });

    it("updates checkbox text when browse type changes", () => {
        let record = wrapper.find('.filter-format');
        expect(record.text()).toBe('Show images only?');

        wrapper.setProps({browseType: 'structure-display' });
        expect(record.text()).toBe('Show folders only?');
    });

    it("resets the filter parameters to be unchecked when browse type changes", () => {
        expect(wrapper.vm.browse_type_updated).toBe(false);
        expect(wrapper.vm.$router.currentRoute.query).toEqual({
            format: "image",
            rows: 20,
            sort: "title,normal",
            start: 0,
            types: "Work"
        });

        wrapper.setProps({browseType: 'structure-display' });
        expect(wrapper.vm.$router.currentRoute.query).toEqual({
            rows: 20,
            sort: "title,normal",
            start: 0,
            types: "Work"
        });
        // Make sure field is reset to false
        expect(wrapper.vm.browse_type_updated).toBe(false);
    });

    it("selects only images when the checkbox is checked in gallery browse mode", () => {
        expect(wrapper.vm.filtered).toBe(true);
        expect(wrapper.vm.$router.currentRoute.query).toEqual({
            format: "image",
            rows: 20,
            sort: "title,normal",
            start: 0,
            types: "Work"
        });
    });

    it("selects all gallery browse records when the checkbox is unchecked", () => {
        // Uncheck the checkbox
        record.trigger('click');
        record.trigger('change');

        expect(wrapper.vm.filtered).toBe(false);
        expect(wrapper.vm.$router.currentRoute.query).toEqual({
            rows: 20,
            sort: "title,normal",
            start: 0,
            types: "Work"
        });
    });

    it("selects only folders when the checkbox is checked in structures browse mode", () => {
        wrapper.setProps({browseType: 'structure-display' });
        // Check the checkbox
        record.trigger('click');
        record.trigger('change');

        expect(wrapper.vm.filtered).toBe(true);
        expect(wrapper.vm.$router.currentRoute.query).toEqual({
            rows: 20,
            sort: "title,normal",
            start: 0,
            types: "Work,Folder"
        });
    });

    it("selects all structure browse records when the checkbox is unchecked in structures browse mode", () => {
        wrapper.setProps({browseType: 'structure-display' });

        expect(wrapper.vm.filtered).toBe(false);
        expect(wrapper.vm.$router.currentRoute.query).toEqual({
            rows: 20,
            sort: "title,normal",
            start: 0,
            types: "Work"
        });
    });
});