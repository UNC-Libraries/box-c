<template>
    <div class="meta-modal">
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
                                <slot name="body">
                                    <div id="response-text" v-html="metadata"></div>
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

    export default {
        name: 'modalMetadata',

        props: {
            uuid: '',
            title: ''
        },

        data() {
            return {
                metadata: '',
                showModal: false
            };
        },

        methods: {
            showMetadata(e) {
                e.preventDefault();
                this.retrieveContainerMetadata();
            },

            retrieveContainerMetadata() {
                get(`record/${this.uuid}/metadataView`).then((response) => {
                    this.metadata = response.data;
                    this.showModal = true;
                }).catch(function (error) {
                    console.log(error);
                    this.metadata = '<p>Unable to retrieve metadata for this item</p>';
                    this.showModal = true;
                });
            }
        },

        mounted() {
            const metadata_link = document.getElementById('metadata-modal-link');

            if (metadata_link !== null) {
                metadata_link.addEventListener('click', this.showMetadata);
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