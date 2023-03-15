import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import audioPlayer from '@/components/full_record/audioPlayer.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';

const dataUrl = 'content/8a2f05e5-d2b7-4857-ae71-c24aa28484c1';
let wrapper, router;

describe('audioPlayer.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        router = createRouter({
            history: createWebHistory(process.env.BASE_URL),
            routes: [
                {
                    path: '/record/:uuid',
                    name: 'displayRecords',
                    component: displayWrapper
                }
            ]
        });

        wrapper = mount(audioPlayer, {
            global: {
                plugins: [i18n, router]
            },
            props: {
                datafileUrl: dataUrl
            }
        });
    });

    it('displays an audio player', () => {
        expect(wrapper.find('audio').exists()).toBe(true);
    });

    it('has an audio source', () => {
        expect(wrapper.find('source').attributes('src')).toEqual(`/${dataUrl}`);
    });

    it('has a download link', () => {
        expect(wrapper.find('a').attributes('href')).toEqual(`/${dataUrl}?dl=true`);
    });
});