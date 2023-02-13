<template>
    <router-view/>
    <cdrFooter/>
</template>

<script>
import footer from "@/components/cdrFooter.vue";

export default {
    components: {
        cdrFooter
    }
}
</script>