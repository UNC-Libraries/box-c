<template>
    <router-view/>
    <dcr-footer/>
</template>

<script>
import dcrFooter from '@/components/dcrFooter.vue';

export default {
    components: {
        dcrFooter
    }
}
</script>