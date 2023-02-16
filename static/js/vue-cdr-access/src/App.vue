<template>
    <router-view/>
    <cdr-footer/>
</template>

<script>
import cdrFooter from "@/components/cdrFooter.vue";

export default {
    components: {
        cdrFooter
    }
}
</script>