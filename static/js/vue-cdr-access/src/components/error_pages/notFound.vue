<template>
    <header-small v-if="displayHeader" />
    <div class="container py-5">
        <p>The record you attempted to access either does not exist or you do not have sufficient rights to view it.</p>
        <p>If you have reached this page in error, please <a href="https://library.unc.edu/contact-us/">report</a>
            it to us or return to the previous page in your browser.</p>
        <p>Or if you believe the record exists and would like to get access to it,
            <template v-if="!isLoggedIn">
                try <a :href="loginUrl">logging in (UNC Onyen)</a> or
            </template>
            you may <a href="https://library.unc.edu/contact-us/">Contact Wilson Library for access information</a>.
        </p>
    </div>
</template>

<script>
import headerSmall from "@/components/header/headerSmall.vue";
import loginUrlUtils from "../../mixins/loginUrlUtils";
import { mapState } from 'pinia';
import { useAccessStore } from '../../stores/access';


export default {
    name: 'notFound',

    components: {headerSmall},

    mixins: [loginUrlUtils],

    props: {
        displayHeader: {
            default: true,
            type: Boolean
        }
    },

    computed: {
        ...mapState(useAccessStore,[
            'isLoggedIn'
        ])
    },

    head() {
        return {
            title: 'Page not found'
        }
    }
}
</script>
