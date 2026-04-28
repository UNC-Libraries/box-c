<template>
    <div class="cf-turnstile mx-auto my-6">
        <div>
            <h1 class="mb-4">Traffic control and bot detection...</h1>
            <vue-turnstile :site-key="siteKey" v-model="token" @error="onTurnstileError" v-if="!error"></vue-turnstile>
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
import fetchUtils from '@/mixins/fetchUtils';
import VueTurnstile from 'vue-turnstile';

export default {
    name: 'cfTurnstile',

    data() {
        return {
            error: false,
            token: ''
        };
    },

    components: {VueTurnstile},

    mixins: [fetchUtils],

    watch: {
        token(token) {
            if (token !== '') {
                this.turnstileCallBack();
            }
        }
    },

    computed: {
        siteKey() {
            return window.turnstileSiteKey;
        }
    },

    methods: {
        async turnstileCallBack() {
            if (this.token !== '') {
                try {
                    const response = await this.fetchWrapper('/api/challenge', true, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ cfTurnstileToken: this.token })
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
        },

        onTurnstileError() {
            this.error = true;
        }
    },

    beforeUnmount() {
        this.error = false;
        this.token = '';
    }
}
</script>

<style scoped>
    .cf-turnstile {
        width: 90%;

        .notification {
            width: 80%;
        }
    }
</style>