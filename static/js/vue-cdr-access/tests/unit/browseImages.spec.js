import { createLocalVue, shallowMount } from '@vue/test-utils'
import VueRouter from 'vue-router'
import browseImages from '@/components/browseImages.vue'

const localVue = createLocalVue();
localVue.use(VueRouter);
const router = new VueRouter();
let wrapper;

describe('browseImages.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(browseImages, {
            localVue,
            router
        });
    });
});