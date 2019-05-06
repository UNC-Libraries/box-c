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
                                    <h3 class="column is-10">{{ containerInfo.title }}</h3>
                                    <button class="column is-2 button" @click="showModal = false">(X) Close</button>
                                </slot>
                            </div>

                            <div class="modal-body">
                                <slot name="body">
                                    default body
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
    define(['Vue'], function(Vue) {
        Vue.component('modalMetadata', {
            props: {
                containerInfo: Object,
                metadata: Array
            },

            template: template,

            data: function() {
                return {
                    showModal: false
                };
            }
        });
    });
</script>

<style>
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
        width: 800px;
        margin: 0 auto;
        padding: 20px 30px;
        background-color: #fff;
        border-radius: 5px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, .33);
        transition: all .3s ease;
    }

    .modal-header {
        width: 100%;
        text-align: center;
    }

    .modal-header h3 {
        font-size: 2rem;
        margin-top: 0;
        text-transform: capitalize;
    }

    .modal-body {
        margin: 20px 0;
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

    .meta-modal button {
        color: white;
        background-color: #007FAE;
    }

    .meta-modal button:hover {
        color: white;
        opacity: .9;
    }

    .modal-header button {
        margin: auto;
        padding: 0;
    }
</style>