import { shallowMount } from '@vue/test-utils'
import streamingPlayer from '@/components/full_record/streamingPlayer.vue';

const STREAMING_URL_SOUND = 'https://durastream.lib.unc.edu/player?spaceId=open-hls&filename=R1018_audio';
const STREAMING_URL_VIDEO = 'https://durastream.lib.unc.edu/player?spaceId=open-hls&filename=R1018_video';
const recordDataSound = {
    title: 'Test title',
    viewerType: 'streaming',
    streamingType: 'sound',
    streamingUrl: STREAMING_URL_SOUND,
    briefObject: {}
};
const recordDataVideo = {
    title: 'Test title',
    viewerType: 'streaming',
    streamingType: 'video',
    streamingUrl: STREAMING_URL_VIDEO,
    briefObject: {}
};

let wrapper;

describe('streamingPlayer.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(streamingPlayer, {
            props: {
                recordData: recordDataSound
            }
        });
    });

    it('displays an an iframe for the streaming player', () => {
        expect(wrapper.find('#streaming-player').exists()).toBe(true);
    });

    // Default record url is set in jest.config.js, which is where the refUrl comes from here
    it('has a source link for the streaming player', async () => {
        expect(wrapper.find('#streaming-player').attributes('src')).toEqual(`${STREAMING_URL_SOUND}&refUrl=https://localhost/record/73bc003c-9603-4cd9-8a65-93a22520ef6a`);
    });

    it('sets an "audio" class for streaming audio objects', async () => {
        // Streaming file record
        expect(wrapper.find('.audio').exists()).toBe(true);
    });

    it('does not set an "audio" class for streaming video objects', async () => {
        await wrapper.setProps({ recordData: recordDataVideo });
        expect(wrapper.find('.audio').exists()).toBe(false);
    });
});