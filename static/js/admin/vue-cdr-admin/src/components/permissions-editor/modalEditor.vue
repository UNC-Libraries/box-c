<template ref="permsEditor">
    <div id="modal-permissions-editor">
        <div class="meta-modal">
            <div v-if="showModal">
                <transition name="modal">
                    <div class="modal-mask">
                        <div class="modal-wrapper">
                            <div class="modal-container">
                                <div class="modal-header columns">
                                    <slot name="header">
                                        <div class="column is-12">
                                            <h3>
                                                <span>{{ permissionType }} Permission Settings for</span>
                                                {{ metadata.title }} <i class="fa" :class="iconType" aria-hidden="true"></i>
                                            </h3>

                                            <button @click="closeModalCheck" class="ui-button ui-widget ui-state-default ui-corner-all ui-button-icon-only ui-dialog-titlebar-close close-icon" role="button" aria-disabled="false" title="close">
                                                <span class="ui-button-icon-primary ui-icon ui-icon-closethick"></span>
                                                <span class="ui-button-text">close</span>
                                            </button>
                                        </div>
                                    </slot>
                                </div>
                                <div class="modal-body">
                                    <patron-roles v-if="permissionType === 'Patron'"
                                                  @reset-changes-check="resetChangesCheck"
                                                  @show-modal="closeModal">
                                    </patron-roles>
                                    <staff-roles v-else
                                                 @reset-changes-check="resetChangesCheck"
                                                 @show-modal="closeModal">
                                    </staff-roles>
                                </div>
                            </div>
                        </div>
                    </div>
                </transition>
            </div>
        </div>
    </div>
</template>

<script>
    import patronRoles from '@/components/permissions-editor/patronRoles.vue';
    import staffRoles from "@/components/permissions-editor/staffRoles.vue";
    import { mapActions, mapState } from 'pinia';
    import { usePermissionsStore } from '../../stores/permissions';

    export default {
        name: 'modalEditor',
        components: {patronRoles, staffRoles},

        computed: {
            ...mapState(usePermissionsStore, {
                actionHandler: state => state.actionHandler,
                alertHandler: state => state.alertHandler,
                checkForUnsavedChanges: state => state.checkForUnsavedChanges,
                metadata: state => state.metadata,
                permissionType: state => state.permissionType,
                resultObject: state => state.resultObject,
                resultObjects: state => state.resultObjects,
                showModal: state => state.showModal
            }),

            iconType() {
                if (this.metadata.type === 'Collection') {
                    return 'fa-archive';
                } else if (this.metadata.type === 'AdminUnit') {
                    return 'fa-university';
                } else if (this.metadata.type === 'Folder') {
                    return 'fa-folder';
                } else {
                    return '';
                }
            },

            parentContainerName() {
                let record_index = this.metadata.objectPath.findIndex((container) => container.name === this.metadata.title);
                if (record_index === -1) {
                    return '';
                }
                return this.metadata.objectPath[record_index - 1].name;
            }
        },

        methods: {
            ...mapActions(usePermissionsStore, ['setCheckForUnsavedChanges', 'setShowModal']),

            closeModalCheck() {
                this.setCheckForUnsavedChanges(true);
            },

            resetChangesCheck(check_changes) {
                this.setCheckForUnsavedChanges(check_changes);
            },

            closeModal() {
                this.setShowModal(false);
            }
        }
    }
</script>

<style lang="scss">
    /* Overrides of common Vue modal styles found here: cdr_ui_styles.scss */
    $unc-blue: #4B9CD3;
    $light-gray: #E1E1E1;

    .modal-header {
        background-image: linear-gradient(to bottom, #FAFAFA, #EBF5FA);
        border-radius: 5px 5px 0 0;
        margin: -20px 0 -20px -30px;
        padding: 20px 30px;

        button {
            background: #e6e6e6 50% 50% repeat-x;
            border-color: $light-gray;
            margin-top: -6px;
            padding: 2px 5px;
        }

        div {
            width: 100%;
        }

        h3 {
            font-size: 18px;
            line-height: 18px;

            span {
                color: black;
            }
        }
    }

    .modal-container {
        background-color: white;
        max-width: 620px;

        h3 {
            color: $unc-blue;
        }

        .modal-body {
            background-color: white;
            border-radius: 0 0 5px 5px;
            margin: 10px -30px -20px -30px;
            max-height: 550px;
            padding: 20px 0;
        }
    }
</style>