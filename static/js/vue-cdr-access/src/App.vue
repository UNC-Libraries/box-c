<template>
    <dcr-header/>
    <router-view/>
    <dcr-footer/>
</template>

<script>
import dcrHeader from "@/components/dcrHeader.vue";
import dcrFooter from "@/components/dcrFooter.vue";

export default {
    components: {
        dcrHeader,
        dcrFooter
    }
}
</script>