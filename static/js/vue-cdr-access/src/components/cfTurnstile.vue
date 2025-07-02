<template>
    <div class="cf-turnstile mx-auto my-6">
        <div>
            <h1 class="mb-4">Traffic control and bot detection...</h1>
            <vue-turnstile :site-key="siteKey" v-model="token" @error="error" v-if="turnstileCallBack()"></vue-turnstile>
            <p>We strive to make our content freely available. If this check is preventing you from making use of our resources,
                make sure you have cookies enabled. If you still have trouble, please
                <a href=" https://library.unc.edu/report-blocked-catalog-access/?supportID=cloudflare_turnstile_dcr">get in touch</a>.</p>
        </div>
        <div class="notification is-danger is-light" role="alert" v-if="error">
            <i class="fa fa-exclamation-triangle" aria-hidden="true"></i>
            Check failed. Sorry, something has gone wrong, or your traffic looks unusual to us. You can try refreshing this page to try again.
            If you still have trouble, please <a href="https://library.unc.edu/report-blocked-catalog-access/?supportID=cloudflare_turnstile_dcr">get in touch</a>.
        </div>
    </div>
</template>

<script>
import axios from "axios";
import VueTurnstile from 'vue-turnstile';

export default {
    name: "cfTurnstile",

    data() {
        return {
            error: false,
            token: null
        };
    },

    components: {VueTurnstile},

    computed: {
        siteKey() {
            return window.turnstileSiteKey;
        }
    },

    methods: {
        async turnstileCallBack() {
            if (this.token !== null) {
                try {
                    const response = await axios.post('/api/challenge', JSON.stringify({ cfTurnstileToken: this.token }),{
                        headers: {
                            'Content-Type': 'application/json'
                        }
                    });

                    if (response.data['success']) {
                        // replace the challenge page in history
                        this.$router.replace({ path: this.$route.redirectedFrom.path, query: this.$route.redirectedFrom.query });
                    }
                } catch (error) {
                    this.error = true;
                    console.error('Error:', error);
                }
            }
        }
    },

    beforeUnmount() {
        this.error = false;
        this.token = null;
    }
}
</script>

<style scoped lang="scss">
    .cf-turnstile {
        width: 90%;

        .notification {
            width: 80%;
        }
    }
</style>