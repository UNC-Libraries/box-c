<!--
Wrapper for nouislider in vue
-->
<template>
    <div ref="sliderEl" class="slider-styled" id="slider-round"></div>
</template>

<script>
import noUiSlider from 'nouislider';
import isEqual from 'lodash.isequal';

const formatter = {
    to: value => parseInt(value),
    from: value => parseInt(value)
};
const full_year = new Date().getFullYear();

export default {
    name: 'slider',

    data() {
        return {
            slider: {}
        }
    },

    emits: ['sliderUpdated'],

    props: {
        startRange: {
            type: Array,
            default(props) {
                return [1, full_year];
            }
        },
        rangeValues: {
            type: Object,
            default(props) {
                return {
                    min: 1,
                    max: full_year
                };
            }
        }
    },

    watch: {
        startRange: {
            handler(newRange, oldRange) {
                if (!isEqual(newRange, oldRange)) {
                    this.slider.set(newRange);
                }
            },
            deep: true
        }
    },

    methods: {
        createSlider() {
            this.slider = noUiSlider.create(this.$refs.sliderEl, {
                start: this.startRange,
                range: this.rangeValues,
                connect: true,
                handleAttributes: [
                    { 'aria-label': 'Start Year' },
                    { 'aria-label': 'End Year' },
                ],
                step: 1,
                tooltips: [formatter, formatter],
                format: formatter
            });
        },

        emitInfo(years) {
            this.$emit('sliderUpdated', years);
        }
    },

    mounted() {
        this.createSlider();
        this.slider.on('change', years => this.emitInfo(years));
    },

    beforeUnmount() {
        this.slider.destroy();
    }
}
</script>

<style lang="scss">
    #slider-round {
        height: 10px;
        margin: 50px auto 25px auto;
        width: 90%;

        .noUi-connect {
            background: #1A698C;
        }

        .noUi-handle {
            height: 18px;
            width: 18px;
            top: -5px;
            right: -9px;
            border-radius: 9px;
        }
    }
</style>