<template>
    <div class="slider-styled" id="slider-round"></div>
</template>

<script>
import noUiSlider from 'nouislider';

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
                return [1, full_year]
            }
        },
        rangeValues: {
            type: Object,
            default(props) {
                return {
                    min: 1,
                    max: full_year
                }
            }
        }
    },

    watch: {
        startRange: {
            handler(range) {
                this.slider.set(range);
            },
            deep: true
        },
        rangeValues: {
            handler(range) {
                this.slider.updateOptions({
                    range: range
                });
            },
            deep: true
        }
    },

    mounted() {
        this.slider = noUiSlider.create(document.getElementById('slider-round'), {
            start: this.startRange,
            connect: true,
            handleAttributes: [
                { 'aria-label': 'start' },
                { 'aria-label': 'end' },
            ],
            step: 1,
            tooltips: [formatter, formatter],
            range: this.rangeValues,
            format: formatter
        });
        this.slider.on('change', years => this.$emit('sliderUpdated', years));
    },

    beforeUnmount() {
        this.slider.destroy();
    }
}
</script>

<style lang="scss">
    #slider-round {
        height: 10px;
        margin: 50px auto 25px 10px;
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