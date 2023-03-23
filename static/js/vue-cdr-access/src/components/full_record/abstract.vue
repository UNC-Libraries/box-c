<template>
    <p v-if="truncateAbstract" class="abstract">{{ truncatedAbstractText }}... <a class="abstract-text" @click.prevent="toggleAbstractDisplay()"
                                                                                  href="#">{{ abstractLinkText }}</a></p>
    <p v-else class="abstract">{{ recordData.briefObject.abstractText }}</p>
</template>

<script>
import fullRecordUtils from "../../mixins/fullRecordUtils";

export default {
    name: "abstract",

    mixins: [fullRecordUtils],

    data() {
        return {
            showFullAbstract: false
        }
    },

    props: {
        briefObject: Object
    }
}
</script>

<style scoped lang="scss">

</style>