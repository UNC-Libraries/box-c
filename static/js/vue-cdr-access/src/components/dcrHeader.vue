<template>
    <header-home v-if="isHomepage"/>
    <header-small v-else/>
</template>

<script>
import headerUtils from '../mixins/headerUtils';
import headerHome from "./header/headerHome.vue";
import headerSmall from "./header/headerSmall.vue";
export default {
    name: "dcrHeader",
    mixins: [headerUtils],
    components: {
        headerSmall,
        headerHome
    },
    computed: {
        isHomepage()  {
            Boolean(window.location.href === this.homeUrl);
        }
    }
};
</script>

<style scoped lang="scss">

</style>