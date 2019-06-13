import { createLocalVue, shallowMount } from '@vue/test-utils'
import VueRouter from 'vue-router'
import pagination from '@/components/pagination.vue'

const localVue = createLocalVue();
localVue.use(VueRouter);
const router = new VueRouter();
let wrapper;

describe('browseSearch.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(pagination, {
            localVue,
            router
        });
    });
});