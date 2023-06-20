<template>
    <router-view/>
    <dcr-footer/>
</template>

<script>
import dcrFooter from '@/components/dcrFooter.vue';
import matomoUtils from "./mixins/matomoUtils";

export default {
    components: {
        dcrFooter
    },

    mixins: [matomoUtils],

    mounted() {
        this.matomoSetup();
    }
}
</script>