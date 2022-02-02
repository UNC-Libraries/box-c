import { createStore } from 'vuex'

// Create a new store instance.
const store = createStore({
    state () {
        return {
            actionHandler: {},
            alertHandler: {},
            checkForUnsavedChanges: false,
            embargoInfo: {
                embargo: null,
                skipEmbargo: true
            },
            metadata: {},
            permissionType: '',
            resultObject: {},
            resultObjects: [],
            showModal: false,
            staffRole: {}
        }
    },
    mutations: {
        setActionHandler (state, actionHandler) {
            state.actionHandler = actionHandler;
        },
        setAlertHandler (state, alertHandler) {
            state.alertHandler = alertHandler;
        },
        setCheckForUnsavedChanges (state, unsavedChanges) {
            state.checkForUnsavedChanges = unsavedChanges;
        },
        setEmbargoInfo (state, embargoInfo) {
            state.embargoInfo = embargoInfo;
        },
        setMetadata (state, metadata) {
            state.metadata = metadata;
        },
        setPermissionType (state, permissionType) {
            state.permissionType = permissionType;
        },
        setResultObject (state, resultObject) {
            state.resultObject = resultObject;
        },
        setResultObjects (state, resultObjects) {
            state.resultObjects = resultObjects;
        },
        setShowModal (state, showModal) {
            state.showModal = showModal;
        },
        setStaffRole (state, staffRole) {
            state.staffRole = staffRole;
        }
    }
});

export default store;