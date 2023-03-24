<template>
    <p v-if="truncateAbstract" class="abstract">{{ truncatedAbstractText }}... <a class="abstract-text" @click.prevent="toggleAbstractDisplay()"
                                                                                  href="#">{{ abstractLinkText }}</a></p>
    <p v-else class="abstract">{{ briefObject.abstractText }} </p>
</template>

<script>
export default {
    name: "abstract",

    data() {
        return {
            showFullAbstract: false
        }
    },

    props: {
        briefObject: Object
    },

    computed: {
        abstractLinkText() {
            return this.showFullAbstract ? this.$t('full_record.read_less') : this.$t('full_record.read_more');
        },

        truncateAbstract() {
            return this.briefObject.abstractText !== undefined &&
                this.briefObject.abstractText.length > 350;
        },

        truncatedAbstractText() {
            if (this.truncateAbstract && !this.showFullAbstract) {
                return this.briefObject.abstractText.substring(0, 350);
            }

            return this.briefObject.abstractText;
        }
    }
}
</script>

<style scoped lang="scss">

</style>