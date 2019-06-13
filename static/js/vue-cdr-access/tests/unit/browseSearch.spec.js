import { createLocalVue, shallowMount } from '@vue/test-utils'
import VueRouter from 'vue-router'
import browseSearch from '@/components/browseSearch.vue'

const localVue = createLocalVue();
localVue.use(VueRouter);
const router = new VueRouter();
let wrapper;

describe('browseSearch.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(browseSearch, {
            localVue,
            router
        });
    });
});