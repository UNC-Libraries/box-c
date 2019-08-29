<template ref="permsEditor">
    <div id="modal-permissions-editor">
        <div class="meta-modal">
            <div v-if="showModal" @close="showModal = false">
                <transition name="modal">
                    <div class="modal-mask">
                        <div class="modal-wrapper">
                            <div class="modal-container">
                                <div class="modal-header columns">
                                    <slot name="header">
                                        <div class="column is-12">
                                            <h3><span>{{ permissionType }} Role Settings for</span> {{ metadata.title }}</h3>
                                            <i class="fa" :class="iconType"></i>
                                            <a class="close-icon" href="#" @click.prevent="showModal = false">X</a>
                                        </div>
                                    </slot>
                                </div>
                                <div class="modal-body">
                                    <patron-roles v-if="permissionType === 'Patron'"></patron-roles>
                                    <staff-roles v-else :uuid="metadata.id"></staff-roles>
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
                } else {
                    return '';
                }
            },
        },

        methods: {
            modalData() {

            }
        }
    }
</script>

<style lang="scss">
    $unc-blue: #4B9CD3;
    $light-gray: #E1E1E1;
    $border-style: 1px solid $light-gray;

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
            display: inline-flex;
            width: 100%;
        }

        h3 {
            font-size: 1.3rem;
            line-height: 1.3rem;
            margin-top: 0;
            text-align: center;
            width: 100%;

            span {
                color: black;
            }
        }
    }

    .modal-body {
        margin: 20px 0;
        text-align: center;
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
    /** New stuff */
    .modal-container {
        background-color: $light-gray;

        h3 {
            color: $unc-blue;
        }

        .modal-body {
            margin: 10px -30px -20px -30px;
            text-align: center;
            padding: 20px 0;
            background-color: white;
        }
    }
</style>