<template>
    <div v-if="canViewMetadata && hasLoaded && metadata !== ''">
        <h2 class="full-metadata subtitle">{{ $t('full_record.detailed_metadata') }}</h2>
        <div id="mods_data_display" v-html="metadata"></div>
    </div>
</template>

<script>
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
        async loadMetadata() {
            try {
                const response = await fetch(`/api/record/${this.uuid}/metadataView`);
                if (!response.ok) {
                    const error = new Error('Network response was not ok');
                    error.response = response;
                    throw error;
                }

                this.metadata = await response.text(); // Use .text() for HTML
                this.hasLoaded = true;
            } catch (error) {
                console.log(error);
                this.metadata = '';
                this.hasLoaded = true;
            }
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