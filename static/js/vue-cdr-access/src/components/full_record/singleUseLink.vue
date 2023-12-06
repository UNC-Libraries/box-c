<template>
    <div class="header-button-single-download">
        <div class="actionlink single-download">
            <div class="single-use-msg-text" :class="{'display-msg': this.message !== ''}" :style="message_location">
                {{ this.message }}
            </div>
            <a class="button action" id="single-use-link" href="#" @click.prevent="createLink($event)">{{ $t('full_record.download_single_use') }}</a>
            <ul>
                <li v-for="single_use_link in single_use_links">
                    <div class="download-link-wrapper">
                        <div>{{ $t('full_record.created_link', { link: single_use_link.link, expire_time: single_use_link.expires }) }}</div>
                        <a @click.prevent="copyUrl(single_use_link.link)" href="#" class="download button action">Copy</a>
                    </div>
                </li>
            </ul>
        </div>
    </div>
</template>

<script>
import axios from 'axios';

export default {
    name: 'singleUseLink',

    props: {
        uuid: String
    },

    data() {
        return {
            single_use_links: [],
            message: '',
            message_location: { left: 0 }
        }
    },

    methods: {
        createLink(e) {
            axios({
                method: 'post',
                url: `/services/api/single_use_link/create/${this.uuid}`
            }).then((response) => {
                this.single_use_links.push(response.data)
            }).catch((error) => {
                console.log(error);
                this.positionMsg(e.target.id);
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

        positionMsg(id) {
            const button_location = document.getElementById(id).getBoundingClientRect();
            this.message_location.left = `${button_location.left}px`;
        },

        fadeOutMsg() {
            setTimeout(() => {
                this.message = '';
                this.message_location.left = 0;
            }, 3000);
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
        }

        .display-msg {
            background: white;
            border: 1px solid;
            border-radius: 5px;
            display: block;
            height: auto;
            padding: 5px;
            position: absolute;
            text-align: center;
            width: 250px;
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