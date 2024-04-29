import { shallowMount } from '@vue/test-utils'
import streamingPlayer from '@/components/full_record/streamingPlayer.vue';
import cloneDeep from 'lodash.clonedeep';

const briefObject = {
    filename: 'R1018_Audio',
    folder: 'open-hls',
    title: 'Test title',
    type: 'audio'
}
let wrapper;

describe('audioPlayer.vue', () => {
    beforeEach(() => {
        wrapper = shallowMount(streamingPlayer, {
            props: {
                briefObject: briefObject
            }
        });
    });

    it('displays an an iframe for the streaming player', () => {
        expect(wrapper.find('#streaming-player').exists()).toBe(true);
    });

    // Default record url is set in jest.config.js, which is where the refUrl comes from here
    it('has a source link for the streaming player', () => {
        expect(wrapper.find('#streaming-player').attributes('src')).toEqual('https://durastream.lib.unc.edu/player?spaceId=open-hls&filename=R1018_Audio&refUrl=https://localhost/record/73bc003c-9603-4cd9-8a65-93a22520ef6a');
    });

    it('sets an "audio" class for streaming audio', () => {
        expect(wrapper.find('.audio').exists()).toBe(true);
    });

    it('does not set an "audio" class for streaming vidio', async () => {
        let updatedRecordData = cloneDeep(briefObject);
        briefObject.type = 'video';
        await wrapper.setProps({ recordData: updatedRecordData });
        expect(wrapper.find('.audio').exists()).toBe(false);
    });
});