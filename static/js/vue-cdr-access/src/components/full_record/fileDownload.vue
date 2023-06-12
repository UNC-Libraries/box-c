<template>
    <div id="image-download-options" v-if="hasPermission(briefObject, 'viewAccessCopies')">
        <button @click="showOptions()" class="button" id="download-images">Download <i class="fas fa-angle-down"></i></button>
        <ul v-if="show_options" :aria-expanded="show_options">
            <li v-if="validSizeOption(800)"><a :href="downloadLink('800')">Small JPG (800px)</a></li>
            <li v-if="validSizeOption(1600)"><a :href="downloadLink('1600')">Medium JPG (1600px)</a></li>
            <li v-if="validSizeOption(2500)"><a :href="downloadLink('2500')">Large JPG (2500px)</a></li>
            <li><a :href="downloadLink('full')">Full Size JPG</a></li>
            <li v-if="hasPermission(briefObject, 'viewOriginal')"><a :href="downloadOriginalLink">Original File</a></li>
        </ul>
    </div>
</template>

<script>
import permissionUtils from '../../mixins/permissionUtils';

export default {
    name: 'fileDownload',

    mixins: [permissionUtils],

    props: {
        briefObject: {
            type: Object,
            default: {}
        }
    },

    data() {
        return {
            show_options: false
        }
    },

    computed: {
        downloadOriginalLink() {
            return `/content/${this.briefObject.id}?dl=true`
        },

        largestImageEdge() {
            const original_file =  this.briefObject.datastream.find(file => file.startsWith('original_file'));
            if (original_file === undefined) {
                return undefined;
            }

            const file_info = original_file.split('|');
            const edge_size = file_info[file_info.length - 1].split('x');
            return edge_size.sort((a, b) => a - b)[edge_size.length - 1];
        }
    },

    methods: {
        closeOptions(e) {
            if (e.keyCode === 27 || (e.target.id !== 'download-images' && !/fa-angle-down/.test(e.target.className))) {
                this.show_options = false;
            }
        },

        showOptions() {
            this.show_options = !this.show_options;
        },

        downloadLink(size) {
            return `/services/api/downloadImage/${this.briefObject.id}/${size}`
        },

        validSizeOption(size) {
            const edge_size = this.largestImageEdge;

            if (edge_size === undefined) {
                return false;
            }

            return size <= edge_size;
        }
    },

    mounted() {
        document.addEventListener('click', this.closeOptions);
        document.addEventListener('keyup', this.closeOptions);
    },

    unmounted() {
        document.removeEventListener('click', this.closeOptions);
        document.removeEventListener('keyup', this.closeOptions);
    }
}
</script>

<style scoped lang="scss">
    #image-download-options {
        display: flex;
        justify-content: flex-end;
        margin: -15px auto 10px auto;
        width: 90.75%;

        button {
            background-color: #1A698C;
            color: white;

            &:hover,
            &:focus {
                background-color: #084b6b;
            }
        }

        ul {
            background-color: white;
            border: 1px solid;
            border-radius: 5px;
            margin-top: 36px;
            padding-top: 5px;
            position: absolute;
            right: 4.6%;
            width: 200px;
            z-index: 100;

            li {
                margin: 0;
                padding: 3px 10px;
            }

            li:hover,
            li:focus {
                background-color: #0A5274;

                a {
                    color: white;
                }
            }

            a {
                color: black;
                text-decoration: none;
            }
        }

        .fa-angle-down {
            margin-left: 8px;
        }
    }
</style>