<template>
    <div class="meta-modal"  v-if="openModal" @close="closeModal()">
        <div>
            <transition name="modal">
                <div class="modal-mask">
                    <div class="modal-wrapper">
                        <div class="modal-container">

                            <div class="modal-header columns">
                                <slot name="header">
                                    <div class="column is-12">
                                        <h3>{{ title }}</h3>
                                        <button class="button" @click="closeModal()">
                                            <i class="fa fa-times-circle"></i>
                                        </button>
                                    </div>
                                </slot>
                            </div>

                            <div class="modal-body" id="mods_data_display">
                                <slot name="body"></slot>
                            </div>
                        </div>
                    </div>
                </div>
            </transition>
        </div>
    </div>
</template>

<script>

export default {
    name: 'modalWindow',

    props: {
        openModal: {
            type: Boolean,
            default: false
        },
        title:  {
            type: String,
            default: ''
        }
    },

    emits: ['display-modal'],

    data() {
        return {
            hasLoaded: false
        };
    },

    methods: {
        closeModal() {
            this.$emit('display-modal', false);
        }
    }
}
</script>

<style scoped lang="scss">
/* See cdr_vue_modal_styles.scss. Modal styles moved there so they can be used across Vue applications */
</style>