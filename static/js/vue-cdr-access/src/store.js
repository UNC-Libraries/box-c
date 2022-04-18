import { createStore } from 'vuex'

const POSSIBLE_FACET_FIELDS = ['unit', 'collection', 'createdYear', 'format', 'language', 'subject', 'location',
    'creatorContributor', 'publisher'];

// Create a new store instance.
const store = createStore({
    state () {
        return {
            possibleFacetFields: POSSIBLE_FACET_FIELDS.slice()
        }
    },
    mutations: {
        removePossibleFacetFields (state, removeFacets) {
            state.possibleFacetFields = state.possibleFacetFields.filter(f => removeFacets.indexOf(f) < 0);
        }
    }
});

export default store;