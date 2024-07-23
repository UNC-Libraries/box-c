import { shallowMount } from '@vue/test-utils'
import streamingPlayer from '@/components/full_record/streamingPlayer.vue';
import cloneDeep from 'lodash.clonedeep';

const STREAMING_URL = 'https://durastream.lib.unc.edu/player?spaceId=open-hls&filename=R1018';
const STREAMING_URL_SOUND = 'https://durastream.lib.unc.edu/player?spaceId=open-hls&filename=R1018_audio';
const STREAMING_URL_VIDEO = 'https://durastream.lib.unc.edu/player?spaceId=open-hls&filename=R1018_video';
const recordDataFile = {
    briefObject: {
        filename: 'R1018_Audio',
        folder: 'open-hls',
        title: 'Test title',
        viewerType: 'streaming',
        streamingType: 'sound',
        streamingUrl: STREAMING_URL
    }
};
const recordDataWorkSound = {
    title: 'Test title',
    viewerType: 'streaming',
    streamingType: 'sound',
    streamingUrl: STREAMING_URL_SOUND,
    briefObject: {}
};
const recordDataWorkVideo = {
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
                recordData: recordDataFile
            }
        });
    });

    it('displays an an iframe for the streaming player', () => {
        expect(wrapper.find('#streaming-player').exists()).toBe(true);
    });

    // Default record url is set in jest.config.js, which is where the refUrl comes from here
    it('has a source link for the streaming player', async () => {
        // Streaming file record
        expect(wrapper.find('#streaming-player').attributes('src')).toEqual(`${STREAMING_URL}&refUrl=https://localhost/record/73bc003c-9603-4cd9-8a65-93a22520ef6a`);

        // Work with streaming child
        await wrapper.setProps({ recordData: recordDataWorkSound });
        expect(wrapper.find('#streaming-player').attributes('src')).toEqual(`${STREAMING_URL_SOUND}&refUrl=https://localhost/record/73bc003c-9603-4cd9-8a65-93a22520ef6a`);
    });

    it('sets an "audio" class for streaming audio', async () => {
        // Streaming file record
        expect(wrapper.find('.audio').exists()).toBe(true);

        // Work with streaming child
        await wrapper.setProps({ recordData: recordDataWorkSound });
        expect(wrapper.find('.audio').exists()).toBe(true);
    });

    it('does not set an "audio" class for streaming video file objects', async () => {
        let updatedRecordData = cloneDeep(recordDataFile);
        updatedRecordData.briefObject.streamingType = 'video';
        // Streaming file record
        await wrapper.setProps({ recordData: updatedRecordData });
        expect(wrapper.find('.audio').exists()).toBe(false);

        // Work with streaming child
        await wrapper.setProps({ recordData: recordDataWorkVideo });
        expect(wrapper.find('.audio').exists()).toBe(false);
    });
});