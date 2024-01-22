<template>
    <div class="header-button-single-download">
        <div class="actionlink single-download">
            <div class="single-use-msg-text" :class="{'display-msg': this.message !== ''}">
                {{ this.message }}
            </div>
            <a class="button action" id="single-use-link" href="#" @click.prevent="createLink()">{{ $t('full_record.download_single_use') }}</a>
            <ul>
                <li v-for="single_use_link in single_use_links">
                    <div class="download-link-wrapper">
                        <div>{{ $t('full_record.created_link', { link: single_use_link.accessCode, expire_time: single_use_link.expires }) }}</div>
                        <a @click.prevent="copyUrl(single_use_link.link)" href="#" class="download button action">Copy Link</a>
                    </div>
                </li>
            </ul>
        </div>
    </div>
</template>

<script>
import axios from 'axios';
import {formatDistanceToNow} from "date-fns";
import {toDate} from "date-fns";

export default {
    name: 'singleUseLink',

    props: {
        uuid: String
    },

    watch: {
        '$route.path': {
            handler() {
                this.single_use_links = []
            }
        }
    },

    data() {
        return {
            single_use_links: [],
            message: ''
        }
    },

    methods: {
        createLink() {
            axios({
                method: 'post',
                url: `/services/api/single_use_link/create/${this.uuid}`
            }).then((response) => {
                let basePath = window.location.hostname;
                let accessCode = response.data.key;
                this.single_use_links.push({"link": this.generateUrl(basePath, accessCode),
                                            "accessCode": accessCode.substring(0, 8),
                                            "expires": this.formatTimestamp(response.data.expires)
                                            });
            }).catch((error) => {
                console.log(error);
                this.message = this.$t('full_record.created_link_failed', { uuid: this.uuid});
                this.fadeOutMsg();
            });
        },

        async copyUrl(text) {
            try {
                await navigator.clipboard.writeText(text);
                this.message = this.$t('full_record.copied_link', { text: text});
            } catch(err) {
                this.message = this.$t('full_record.copied_link_failed', { text: text});
            }
            this.fadeOutMsg();
        },

        fadeOutMsg() {
            setTimeout(() => this.message = '', 3000);
        },

        formatTimestamp(timestamp) {
            return formatDistanceToNow(toDate(parseInt(timestamp)));
        },

        generateUrl(basePath, accessCode) {
            return "https://" + basePath + "/services/api/single_use_link/" + accessCode;
        }
    }
}
</script>

<style scoped lang="scss">
    .header-button-single-download {
        text-align: right;

        .single-download {
            display: block;

            a {
                float: right;
                max-width: 210px;
            }

            ul {
                margin-top: 10px;

                li {
                    .download-link-wrapper {
                        align-items: center;
                        display: inline-flex;
                        margin: 5px auto;

                        div {
                            background-color: white;
                            padding: 15px;
                        }
                    }
                }
            }
        }

        .single-use-msg-text {
            display: none;
            word-break: break-word;
            padding: 5px;
        }

        .display-msg {
            background: white;
            border: 1px solid;
            border-radius: 5px;
            display: block;
            height: auto;
            padding: 5px;
            position: fixed;
            right: 10px;
            text-align: center;
            top: 10px;
            width: auto;
            z-index: 599;
        }
    }

    @media (max-width: 768px) {
        .header-button-single-download {
            text-align: left;

            .single-download {
                display: block;

                a {
                    float: none;
                }

                ul {
                    li {
                        margin-left: 0;
                    }
                }
            }
        }
    }
</style>