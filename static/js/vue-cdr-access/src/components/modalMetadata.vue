<!--
Displays the MODS descriptive record for an object inside of a modal
-->
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
                                        <button class="button is-small" @click="closeModal()">{{ $t('modal.close') }}</button>
                                    </div>
                                </slot>
                            </div>

                            <div class="modal-body" id="mods_data_display">
                                <slot name="body">
                                    <img v-if="!hasLoaded" :src="nonVueStaticImageUrl('ajax-loader-lg.gif')" alt="data loading icon">
                                    <div id="response-text" v-if="hasLoaded" v-html="metadata"></div>
                                </slot>
                            </div>
                        </div>
                    </div>
                </div>
            </transition>
        </div>
    </div>
</template>

<script>
    import get from 'axios';
    import imageUtils from '../mixins/imageUtils';

    export default {
        name: 'modalMetadata',

        props: {
            openModal: {
                type: Boolean,
                default: false
            },
            uuid: {
                type: String,
                default: ''
            },
            title:  {
                type: String,
                default: ''
            }
        },

        emits: ['display-metadata'],

        mixins: [imageUtils],

        data() {
            return {
                hasLoaded: false,
                metadata: ''
            };
        },

        watch: {
            openModal(display) {
                if (display) {
                    if (this.metadata === '') {
                        this.loadMetadata();
                    }
                }
            }
        },

        methods: {
            loadMetadata() {
                get(`/record/${this.uuid}/metadataView`).then((response) => {
                    this.metadata = response.data;
                    this.hasLoaded = true;
                }).catch((error) => {
                    console.log(error);
                    this.metadata = `<p>${this.$t('modal.error')}</p>`;
                    this.hasLoaded = true;
                });
            },

            closeModal() {
                this.$emit('display-metadata', false);
            }
        }
    }
</script>

<style scoped lang="scss">
    .meta-modal {
        float: right;
    }

    @media screen and (max-width: 768px) {
        .meta-modal {
            float: none;
        }
    }
/* See cdr_vue_modal_styles.scss. Modal styles moved there so they can be used across Vue applications */
</style>