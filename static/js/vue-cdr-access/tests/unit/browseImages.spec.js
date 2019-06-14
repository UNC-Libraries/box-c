import { createLocalVue, shallowMount } from '@vue/test-utils';
import VueRouter from 'vue-router';
import browseImages from '@/components/browseImages.vue';

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

describe('browseImages.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(browseImages, {
            localVue,
            router,
            propsData: {
                container_type: 'Collection'
            }
        });

        wrapper.setData({
            images_only: false,
            update_params: {}
        });

        record = wrapper.find('.checkbox');

        // trigger both events so v-model updates
        record.trigger('click');
        record.trigger('change');
    });

    it("selects only images when the checkbox is checked", () => {
        expect(wrapper.vm.images_only).toBe(true);
        expect(wrapper.vm.$router.currentRoute.query).toEqual({
            format: "image",
            page: 1,
            rows: 20,
            sort: "title,normal",
            start: 0
        });
    });

    it("selects all records when the checkbox is unchecked", () => {
        // Uncheck the checkbox
        record.trigger('click');
        record.trigger('change');

        expect(wrapper.vm.images_only).toBe(false);
        expect(wrapper.vm.$router.currentRoute.query).toEqual({
            page: 1,
            rows: 20,
            sort: "title,normal",
            start: 0
        });
    });

    it("does not display a checkbox for non-collections", () => {
        wrapper.setProps({container_type: 'AdminUnit' });
        expect(wrapper.find('.imgs-only').contains('.checkbox')).toBe(false);
    });
});