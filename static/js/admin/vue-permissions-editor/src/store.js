import { createStore } from 'vuex'

// Create a new store instance.
const store = createStore({
    state () {
        return {
            actionHandler: {},
            alertHandler: {},
            checkForUnsavedChanges: false,
            embargoError: '',
            embargoInfo: {},
            metadata: {},
            permissionType: '',
            resultObject: {},
            resultObjects: [],
            showModal: false,
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
        setEmbargoError (state, embargoError) {
            state.embargoError = embargoError;
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
        }
    }
});

export default store;