import { shallowMount } from '@vue/test-utils';
import modalEditor from '@/components/modalEditor.vue';
import {createStore} from "vuex";

const metadata = {
    id: 'd77fd8c9-744b-42ab-8e20-5ad9bdf8194e',
    title: 'Test Collection',
    type: 'Collection',
    objectPath: [
        { container: true,  name: 'Root' },
        { container: true,  name: 'Test Unit' },
        { container: true, name: 'Test Collection' }
    ]
};
let wrapper;

describe('modalEditor.vue', () => {
    const store = createStore({
        state () {
            return {
                metadata: {},
                showModal: false,
            }
        },
        mutations: {
            setMetadata (state, metadata) {
                state.metadata = metadata;
            },
            setShowModal (state, showModal) {
                state.showModal = showModal;
            }
        }
    });

    beforeEach(() => {
        wrapper = shallowMount(modalEditor,
            {
                global: {
                    plugins: [store]
                }
            });
    });

    it("is hidden by default", () => {
        expect(wrapper.find('.meta-modal .modal-body').exists()).toBe(false);
    });

    it("displays a record title when triggered from admin interface", async () => {
        await store.commit('setShowModal', true);
        await store.commit('setMetadata', metadata);

        const record = wrapper.find('h3');
        expect(record.text()).toContain(metadata.title);
    });

   // See staffRoles.spec.js for tests on modal closure with and without unsaved changes
});