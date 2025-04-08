import { shallowMount } from '@vue/test-utils';
import formsContainer from '@/components/forms-app/formsContainer.vue';

let wrapper;

describe('formsContainer.vue', () => {
    beforeEach(async () => {
        const el = document.createElement('div')
        el.id = 'vue-cdr-admin-add-work'
        document.body.appendChild(el)
        wrapper = shallowMount(formsContainer);
    });

    afterEach(() => {
        wrapper = null;
    });
});