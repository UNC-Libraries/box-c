<template>
    <header-small/>
    <div id="about-repo" class="content container mt-4">
        <div class="has-text-centered">
            <h2>{{ $t('about.headers.main') }}</h2>
        </div>
        <p>{{ $t('about.description') }}</p>

        <h3>{{ $t('about.headers.related_collections') }}</h3>

        <p>{{ $t('about.sub_headers.description') }}</p>
        <ul>
            <li><a href="https://library.unc.edu/wilson/search-the-special-collections/">https://library.unc.edu/wilson/search-the-special-collections</a></li>
        </ul>

        <p>{{ $t('about.sub_headers.scans') }}</p>
        <ul>
            <li><a href="https://library.unc.edu/special-collection/">https://library.unc.edu/special-collection</a></li>
        </ul>

        <p>{{ $t('about.sub_headers.docsouth') }}</p>
        <ul>
            <li><a href="https://docsouth.unc.edu/docsouthdata/">https://docsouth.unc.edu/docsouthdata</a></li>
        </ul>

        <p>{{ $t('about.sub_headers.websites') }}</p>
        <ul>
            <li><a href="https://archive-it.org/collections/3491">https://archive-it.org/collections/3491</a></li>
        </ul>

        <h3>{{ $t('about.headers.responsibilities') }}</h3>

        <p>{{ $t('about.responsibilities.text') }}</p>

        <h3>{{ $t('about.headers.usage') }}</h3>

        <p>{{ $t('about.usage.text_one') }}</p>
        <p>{{ $t('about.usage.text_two') }}</p>
        <p v-html="$t('about.usage.text_three')"></p>
        <p v-html="$t('about.usage.text_four')"></p>
        <p>{{ $t('about.usage.text_five') }}</p>
        <p>{{ $t('about.usage.text_six') }}</p>

        <h3>{{ $t('about.headers.preservation') }}</h3>

        <p>{{ $t('about.preservation.text_one') }}</p>
        <p>{{ $t('about.preservation.text_two') }}</p>
        <p>{{ $t('about.preservation.text_three') }}</p>

        <h3>{{ $t('about.headers.more_info') }}</h3>

        <p>{{ $t('about.website') }} <a href="https://library.unc.edu/wilson/">https://library.unc.edu/wilson</a></p>
        <p>{{ $t('about.contact_us') }} <a href="mailto:wilsonlibrary@unc.edu">wilsonlibrary@unc.edu</a></p>
    </div>
</template>

<script>
import headerSmall from '@/components/header/headerSmall.vue';
import analyticsUtils from '../mixins/analyticsUtils';

export default {
    name: "aboutRepository",

    components: {headerSmall},

    mixins: [analyticsUtils],

    head() {
        return {
            title: 'About'
        }
    },

    mounted() {
        this.pageView('About');
    }
}
</script>