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

    components: {
        headerSmall,
        headerHome
    },

    mixins: [headerUtils],

    computed: {
        isHomepage()  {
            const homeUrl = `https://${window.location.host}/`;
            return window.location.href == homeUrl;
        }
    }
}
</script>