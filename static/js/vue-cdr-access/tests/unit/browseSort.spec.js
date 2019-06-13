import { createLocalVue, shallowMount } from '@vue/test-utils'
import VueRouter from 'vue-router'
import browseSort from '@/components/browseSort.vue'

const localVue = createLocalVue();
localVue.use(VueRouter);
const router = new VueRouter();
let wrapper;

describe('browseSearch.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(browseSort, {
            localVue,
            router
        });
    });
});