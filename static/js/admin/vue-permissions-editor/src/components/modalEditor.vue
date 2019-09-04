<template ref="permsEditor">
    <div id="modal-permissions-editor">
        <div class="meta-modal">
            <div v-if="showModal" @close="closeModal">
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
                                            <a class="close-icon" href="#" @click.prevent="closeModal">X</a>
                                        </div>
                                    </slot>
                                </div>
                                <div class="modal-body">
                                    <patron-roles v-if="permissionType === 'Patron'"></patron-roles>
                                    <staff-roles v-else
                                                 :container-name="parentContainerName"
                                                 :container-type="metadata.type"
                                                 :uuid="metadata.id"
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
    import patronRoles from './patronRoles';
    import staffRoles from "./staffRoles";

    export default {
        name: 'modalEditor',
        components: {patronRoles, staffRoles},

        data() {
            return {
                metadata: {},
                permissionType: '',
                showModal: false
            };
        },

        computed: {
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
            closeModal(action) {
                this.showModal = false;
            }
        }
    }
</script>

<style lang="scss">
    /* Overrides of common Vue modal styles found here: cdr_ui_styles.scss */
    $unc-blue: #4B9CD3;
    $light-gray: #E1E1E1;

    .modal-header {
        a {
            color: darkslategray;
            float: right;
            font-size: 24px;
            font-weight: bold;
            margin: 3px 0;

            &:hover {
                text-decoration: none;
            }
        }

        div {
            width: 100%;
        }

        h3 {
            font-size: 1.3rem;
            line-height: 1.3rem;

            span {
                color: black;
            }
        }
    }

    .modal-container {
        background-color: $light-gray;

        h3 {
            color: $unc-blue;
        }

        .modal-body {
            background-color: white;
            border-bottom: 1px solid white;
            border-radius: 5px;
            margin: 10px -30px -20px -30px;
            max-height: 650px;
            overflow: auto;
            padding: 20px 0;
        }
    }
</style>