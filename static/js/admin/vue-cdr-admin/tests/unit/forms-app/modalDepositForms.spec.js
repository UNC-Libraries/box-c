import { shallowMount } from '@vue/test-utils';
import ModalDepositForms from '@/components/forms-app/modalDepositForms.vue';

let wrapper;

describe('modalDepositForms.vue', () => {
    beforeEach(async () => {
        const el = document.createElement('div')
        el.id = 'vue-cdr-admin-add-work'
        document.body.appendChild(el)
        wrapper = shallowMount(ModalDepositForms);
    });

    afterEach(() => {
        wrapper = null;
    });
});