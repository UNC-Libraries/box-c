<template>
    <div id="modal-permissions-editor">
        <div class="meta-modal">
            <button id="show-modal" class="button is-medium" @click="showMetadata">
                <i class="fa fa-file-text-o" aria-hidden="true" title="Metadata"></i> Metadata
            </button>

            <div v-if="showModal" @close="showModal = false">
                <transition name="modal">
                    <div class="modal-mask">
                        <div class="modal-wrapper">
                            <div class="modal-container">

                                <div class="modal-header columns">
                                    <slot name="header">
                                        <div class="column is-12">
                                            <h3>{{ title }}</h3>
                                            <button class="button is-small" @click="showModal = false">Close</button>
                                        </div>
                                    </slot>
                                </div>
                                <div class="modal-body">
                                    <patron-permissions v-if="permissionType === 'patron'"></patron-permissions>
                                    <staff-permissions v-else></staff-permissions>
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
    import patronPermissions from './patronPermissions';
    import staffPermissions from "./staffPermissions";

    export default {
        name: 'modalEditor',
        components: {patronPermissions, staffPermissions},
        props: {
            permissionType: String,
            title: String
        },

        data() {
            return {
                showModal: false
            };
        },



        methods: {
            showModal() {

            }
        }
    }
</script>

<style scoped lang="scss">

</style>