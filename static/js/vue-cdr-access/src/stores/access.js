import { defineStore } from 'pinia'

const POSSIBLE_FACET_FIELDS = ['unit', 'collection', 'format', 'genre', 'language', 'subject', 'location',
    'createdYear', 'creatorContributor', 'publisher'];

export const useAccessStore = defineStore({
    id: 'access',
    state: () => ({
        isLoggedIn: false,
        possibleFacetFields: POSSIBLE_FACET_FIELDS.slice(),
        username: '',
        viewAdmin: false
    }),
    actions: {
        removePossibleFacetFields (removeFacets) {
            this.possibleFacetFields = POSSIBLE_FACET_FIELDS.slice().filter(f => removeFacets.indexOf(f) < 0);
        },
        resetPossibleFacetFields () {
            this.possibleFacetFields = POSSIBLE_FACET_FIELDS.slice();
        },
        setIsLoggedIn () {
            this.isLoggedIn = this.username !== undefined && this.username !== '';
        },
        setUsername (username) {
            this.username = username || '';
        },
        setViewAdmin (viewAdmin) {
            this.viewAdmin = viewAdmin === 'true';
        },
        /**
         * Used to reset the store back to its initial state, primarily for testing
         */
        resetState () {
            this.possibleFacetFields = POSSIBLE_FACET_FIELDS;
        }
    }
});