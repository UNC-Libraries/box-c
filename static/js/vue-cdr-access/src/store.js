import {createStore} from 'vuex'

const POSSIBLE_FACET_FIELDS = ['unit', 'collection', 'format', 'genre', 'language', 'subject', 'location',
    'createdYear', 'creatorContributor', 'publisher'];

// Create a new store instance.
const store = createStore({
    state () {
        return {
            isLoggedIn: false,
            possibleFacetFields: POSSIBLE_FACET_FIELDS.slice(),
            username: '',
            viewAdmin: false
        }
    },
    mutations: {
        removePossibleFacetFields (state, removeFacets) {
            state.possibleFacetFields = state.possibleFacetFields.filter(f => removeFacets.indexOf(f) < 0);
        },
        resetPossibleFacetFields (state) {
            state.possibleFacetFields = POSSIBLE_FACET_FIELDS.slice();
        },
        setIsLoggedIn (state) {
            state.isLoggedIn = state.username !== undefined && state.username !== '';
        },
        setUsername (state, username) {
            state.username = username || '';
        },
        setViewAdmin (state, viewAdmin) {
            state.viewAdmin = viewAdmin === 'true';
        }
    },
    actions: {
        /**
         * Used to reset the store back to its initial state, primarily for testing
         * @param context
         */
        resetState (context) {
            context.commit("resetPossibleFacetFields");
        }
    }
});

export default store;