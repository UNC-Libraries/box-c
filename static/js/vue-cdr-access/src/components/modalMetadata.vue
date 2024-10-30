<!--
Displays the MODS descriptive record for an object inside of a modal
-->
<template>
    <modal-window :open-modal="openModal" :title="title">
        <template #body>
            <img v-if="!hasLoaded" :src="nonVueStaticImageUrl('ajax-loader-lg.gif')" alt="data loading icon">
            <div id="response-text" v-if="hasLoaded" v-html="metadata"></div>
        </template>
    </modal-window>
</template>

<script>
    import get from 'axios';
    import modalWindow from "@/components/modalWindow.vue";
    import imageUtils from '../mixins/imageUtils';

    export default {
        name: 'modalMetadata',

        components: { modalWindow },

        mixins: [imageUtils],

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

        data() {
            return {
                hasLoaded: false,
                metadata: ''
            };
        },

        watch: {
            openModal(display) {
                if (display) {
                    this.loadMetadata();
                }
            }
        },

        methods: {
            loadMetadata() {
                this.hasLoaded = false;
                get(`/record/${this.uuid}/metadataView`).then((response) => {
                    this.metadata = response.data;
                    this.hasLoaded = true;
                }).catch((error) => {
                    console.log(error);
                    this.metadata = `<p>${this.$t('modal.error')}</p>`;
                    this.hasLoaded = true;
                });
            }
        }
    }
</script>