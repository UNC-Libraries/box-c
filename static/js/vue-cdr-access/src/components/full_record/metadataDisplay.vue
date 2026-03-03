<template>
    <div v-if="canViewMetadata && hasLoaded && metadata !== ''">
        <h2 class="full-metadata subtitle">{{ $t('full_record.detailed_metadata') }}</h2>
        <div id="mods_data_display" v-html="metadata"></div>
    </div>
</template>

<script>
import wretch from 'wretch';

export default {
    name: 'metadataDisplay',

    watch: {
        uuid(d) {
            if (this.canViewMetadata) {
                this.loadMetadata();
            }
        }
    },

    props: {
        canViewMetadata: {
            type: Boolean,
            default: false
        },
        uuid: String,
    },

    data() {
        return {
            hasLoaded: false,
            metadata: ''
        }
    },

    methods: {
        loadMetadata() {
            wretch(`/api/record/${this.uuid}/metadataView`)
                .get()
                .text((text) => {
                    this.metadata = text;
                    this.hasLoaded = true;
                })
                .catch((error) => {
                    console.log(error);
                    this.metadata = '';
                    this.hasLoaded = true;
                });
        }
    },

    mounted() {
        if (this.canViewMetadata) {
            this.loadMetadata();
        }
    }
}
</script>

<style scoped>

</style>