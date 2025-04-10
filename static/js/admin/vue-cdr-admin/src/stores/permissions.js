import { defineStore } from 'pinia'

export const usePermissionsStore = defineStore({
    id: 'permissions',
    state: () => ({
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
        showPermissionsModal: false,
        staffRole: {}
    }),
    actions: {
        setActionHandler(actionHandler) {
            this.actionHandler = actionHandler;
        },
        setAlertHandler(alertHandler) {
            this.alertHandler = alertHandler;
        },
        setCheckForUnsavedChanges(unsavedChanges) {
            this.checkForUnsavedChanges = unsavedChanges;
        },
        setEmbargoInfo(embargoInfo) {
            this.embargoInfo = embargoInfo;
        },
        setMetadata(metadata) {
            this.metadata = metadata;
        },
        setPermissionType(permissionType) {
            this.permissionType = permissionType;
        },
        setResultObject(resultObject) {
            this.resultObject = resultObject;
        },
        setResultObjects(resultObjects) {
            this.resultObjects = resultObjects;
        },
        setShowPermissionsModal(showPermissionsModal) {
            this.showPermissionsModal = showPermissionsModal;
        },
        setStaffRole (staffRole) {
            this.staffRole = staffRole;
        }
    }
});