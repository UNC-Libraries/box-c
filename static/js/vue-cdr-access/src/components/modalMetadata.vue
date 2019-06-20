<template>
    <div class="meta-modal">
        <button id="show-modal" class="button is-medium" @click="showModal = true">
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

        watch: {
            uuid(d) {
                this.retrieveContainerMetadata();
            }
        },

        methods: {
            retrieveContainerMetadata() {
                get(`record/${this.uuid}/metadataView`).then((response) => {
                    this.metadata = response.data;
                }).catch(function (error) {
                    console.log(error);
                });
            }
        }
    }
</script>

<style lang="scss">
    .modal-mask {
        position: fixed;
        z-index: 9998;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, .5);
        display: table;
        transition: opacity .3s ease;
    }

    .modal-wrapper {
        display: table-cell;
        vertical-align: middle;
    }

    .modal-container {
        width: auto;
        margin: 0 auto;
        max-width: 720px;
        padding: 20px 30px;
        background-color: #fff;
        border-radius: 5px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, .33);
        transition: all .3s ease;
    }

    .modal-header {
        width: 100%;
        text-align: center;

        button {
            float: right;
            margin: auto 0 auto auto;
        }

        div {
            display: inline-flex;
        }

        h3 {
            font-size: 2rem;
            margin-top: 0;
            text-align: center;
            text-transform: capitalize;
            width: 100%;
        }
    }

    .modal-body {
        margin: 20px 0;
        text-align: center;

        table {
            td {
                font-size: 18px;
                padding-bottom: 5px;
                padding-left: 15px;

                p {
                    font-size: 18px;
                }
            }
        }
    }

    /*
     * The following styles are auto-applied to elements with
     * transition="modal" when their visibility is toggled
     * by Vue.js.
     */

    .modal-enter {
        opacity: 0;
    }

    .modal-leave-active {
        opacity: 0;
    }

    .modal-enter .modal-container,
    .modal-leave-active .modal-container {
        -webkit-transform: scale(1.1);
        transform: scale(1.1);
    }

    .meta-modal {
        button {
            color: white;
            background-color: #007FAE;

            &:hover {
                color: white;
                opacity: .9;
            }
        }

    }

    @media screen and (max-width: 768px) {
        .modal-container {
            margin: 0 25px;
        }

        .modal-header h3 {
            font-size: 1.3rem;
            width: 85%;
        }
    }
</style>