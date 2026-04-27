import { shallowMount } from '@vue/test-utils';
import { createTestingPinia } from '@pinia/testing';
import altTextViewer from '@/components/machine-alt-text/altTextViewer.vue';

const uuid = '67ff0cb6-c360-439a-a194-b271cd4177e4';
const createSampleReviewAssessment = (overrides = {}) => ({
    concerns_for_review: [],
    people_first_language: 'N/A',
    risk_score: 0,
    stereotyping: 'NO',
    ...overrides
});
const createSampleSafetyAssessment = (overrides = {}) => ({
    people_visible: 'NO',
    sexual_content: 'NONE',
    symbols_present: {
        misidentification_risk: 'LOW',
        names: [],
        types: ['NONE']
    },
    text_characteristics: {
        legibility: 'N/A',
        text_present: 'NO',
        text_type: 'N/A'
    },
    ...overrides
});
const sampleReviewAssessment = createSampleReviewAssessment();
const sampleSafetyAssessment = createSampleSafetyAssessment();
const mountViewer = (items = []) => {
    return shallowMount(altTextViewer, {
        global: {
            plugins: [createTestingPinia({
                initialState: {
                    'alt-text': {
                        items,
                        currentUuid: uuid,
                        alertMessage: ''
                    }
                },
                stubActions: false
            })],
            stubs: {
                teleport: true
            },
            mocks: {
                $route: {
                    params: {
                        uuid: uuid
                    }
                }
            }
        }
    });
};

describe('altTextViewer.vue', () => {
    describe('computed flags/options', () => {
        it('reports hasItems and hasSearchPaneOptions based on items data', () => {
            const emptyWrapper = mountViewer([]);
            expect(emptyWrapper.vm.hasItems).toBe(false);
            expect(emptyWrapper.vm.hasSearchPaneOptions).toBe(false);

            const wrapper = mountViewer([{ mgContentTags: ['tag-a'] }]);
            expect(wrapper.vm.hasItems).toBe(true);
            expect(wrapper.vm.hasSearchPaneOptions).toBe(true);
        });

        it('includes custom SearchPanes config in tableOptions', () => {
            const wrapper = mountViewer([{ mgContentTags: ['tag-a'] }]);
            const options = wrapper.vm.tableOptions;

            expect(options.searchPanes.columns).toEqual([]);
            expect(options.searchPanes.initCollapsed).toBe(true);
            expect(options.searchPanes.panes[0].header).toBe('Search Tags');
            expect(options.layout.topStart).toBe('searchPanes');
        });
    });

    describe('getTags', () => {
        it('returns tag values as-is when array exists', () => {
            const wrapper = mountViewer();
            const row = {
                mgContentTags: ['People', 'tag-a']
            };

            const result = wrapper.vm.getTags(row);
            expect(result).toEqual(['People', 'tag-a']);
        });

        it('returns an empty array when tags are missing', () => {
            const wrapper = mountViewer();
            expect(wrapper.vm.getTags({})).toEqual([]);
            expect(wrapper.vm.getTags(null)).toEqual([]);
        });
    });

    describe('tagPaneOptions', () => {
        it('counts each tag once per row and sorts by descending count', () => {
            const wrapper = mountViewer([
                { mgContentTags: ['test', 'boxy', 'apples'] },
                { mgContentTags: ['test', 'boxy', 'cars'] },
                { mgContentTags: ['test'] }
            ]);

            expect(wrapper.vm.tagPaneOptions.map((option) => option.label)).toEqual(['test', 'boxy', 'apples', 'cars']);
        });

        it('keeps insertion order for tags with equal counts', () => {
            const wrapper = mountViewer([
                { mgContentTags: ['beta', 'alpha'] },
                { mgContentTags: ['alpha', 'beta'] }
            ]);

            expect(wrapper.vm.tagPaneOptions.map((option) => option.label)).toEqual(['beta', 'alpha']);
        });

        it('builds option matchers that use tag values from the row', () => {
            const wrapper = mountViewer([
                { mgContentTags: ['tag-a'] }
            ]);
            const tagA = wrapper.vm.tagPaneOptions.find((option) => option.label === 'tag-a');

            expect(tagA.value({ mgContentTags: ['tag-a'] })).toBe(true);
            expect(tagA.value({ mgContentTags: ['other'] })).toBe(false);
        });
    });

    describe('longText and safety rendering', () => {
        it('renders short and long text variants in longText', () => {
            const wrapper = mountViewer();

            const short = wrapper.vm.longText('short text', 'altText');
            expect(short).toContain('short text');
            expect(short).toContain('data-action="edit"');
            expect(short).not.toContain('View All');

            const long = wrapper.vm.longText('x'.repeat(260), 'mgFullDescription');
            expect(long).toContain('View All');
            expect(long).toContain('data-action="view"');
            expect(long).toContain('data-action-field="mgFullDescription"');
        });

        it('formats safety values for arrays, objects, and empty values', () => {
            const wrapper = mountViewer();

            expect(wrapper.vm.formatSafetyValue(sampleReviewAssessment.concerns_for_review)).toBe('none');
            expect(wrapper.vm.formatSafetyValue(null)).toBe('None');
            expect(wrapper.vm.formatSafetyValue(sampleReviewAssessment.stereotyping)).toBe('no');

            const objectText = wrapper.vm.formatSafetyValue(sampleSafetyAssessment);
            expect(objectText).toContain('people visible');
            expect(objectText).toContain('symbols present');
        });

        it('renders structured safety data list', () => {
            const wrapper = mountViewer();
            const rendered = wrapper.vm.renderSafetyData(sampleSafetyAssessment);
            expect(rendered).toContain('<ul class="is-capitalized">');
            expect(rendered).toContain('people visible');
            expect(rendered).toContain('symbols present');
        });
    });
});
