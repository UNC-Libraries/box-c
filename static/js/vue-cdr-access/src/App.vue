<template>
    <dcr-header/>
    <router-view/>
    <dcr-footer/>
</template>

<script>
import headerUtils from './mixins/headerUtils';
import dcrHeader from "@/components/dcrHeader.vue";
import dcrFooter from "@/components/dcrFooter.vue";

export default {
    mixins: [headerUtils],

    components: {
        dcrHeader,
        dcrFooter
    }
}
</script>