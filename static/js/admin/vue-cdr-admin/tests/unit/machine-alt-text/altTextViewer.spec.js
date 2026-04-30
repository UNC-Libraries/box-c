import { shallowMount } from '@vue/test-utils';
import { createTestingPinia } from '@pinia/testing';
import altTextViewer from '@/components/machine-alt-text/altTextViewer.vue';

const uuid = '67ff0cb6-c360-439a-a194-b271cd4177e4';
const createSampleReviewAssessment = (overrides = {}) => ({
    concerns_for_review: [
        'Unsupported inferential claim from external metadata'
    ],
    biased_language: 'NO',
    people_first_language: 'N/A',
    unsupported_inferential_claims: 'YES',
    risk_score: 0,
    stereotyping: 'NO',
    ...overrides
});
const createSampleSafetyAssessment = (overrides = {}) => ({
    people_visible: 'NO',
    demographics_described: 'NO',
    misidentification_risk_people: 'LOW',
    minors_present: 'NO',
    named_individuals_claimed: 'NO',
    violent_content: 'NONE',
    racial_violence_oppression: 'NONE',
    nudity: 'NONE',
    sexual_content: 'NONE',
    stereotyping_present: 'NO',
    atrocities_depicted: 'NO',
    symbols_present: {
        misidentification_risk: 'LOW',
        names: [],
        types: ['NONE']
    },
    text_characteristics: {
        legibility: 'ILLEGIBLE',
        text_present: 'INCIDENTAL',
        text_type: 'PRINTED',
        sensitivity: 'NONE'
    },
    reasoning: 'Text present but not fully legible',
    risk_score: 53,
    inconsistency_count: 0,
    ...overrides
});
const sampleReviewAssessment = createSampleReviewAssessment();
const sampleSafetyAssessment = createSampleSafetyAssessment();
const mountViewer = ({ items = [], globalTagCounts = {} } = {}) => {
    return shallowMount(altTextViewer, {
        global: {
            plugins: [createTestingPinia({
                initialState: {
                    'alt-text': {
                        items,
                        globalTagCounts,
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
        it('builds search pane options based on items data', () => {
            const emptyWrapper = mountViewer();
            expect(emptyWrapper.vm.tagPaneOptions).toEqual([]);

            const wrapper = mountViewer({ globalTagCounts: { 'tag-a': 3 } });
            expect(wrapper.vm.tagPaneOptions).toHaveLength(1);
        });

        it('includes custom SearchPanes config in tableOptions', () => {
            const wrapper = mountViewer({ globalTagCounts: { 'tag-a': 3 } });
            const options = wrapper.vm.tableOptions;

            expect(options.searchPanes.columns).toEqual([]);
            expect(options.searchPanes.initCollapsed).toBe(true);
            expect(options.searchPanes.panes[0].header).toBe('Search Tags');
            expect(options.layout.topStart).toBe('searchPanes');
        });

        it('uses default table options for ordering, fixed header, select, and pagination', () => {
            const wrapper = mountViewer({ globalTagCounts: { 'tag-a': 3 } });
            const options = wrapper.vm.tableOptions;

            expect(options.order).toEqual([[1, 'asc']]);
            expect(options.serverSide).toBe(true);
            expect(options.processing).toBe(true);
            expect(typeof options.ajax).toBe('function');
            expect(options.fixedHeader).toBe(true);
            expect(options.select).toBe(true);
            expect(options.pageLength).toBe(25);
            expect(options.layout.topEnd.search.placeholder).toBe('Search');
        });

        it('keeps search panes visible in layout in server-side mode', () => {
            const wrapper = mountViewer();
            expect(wrapper.vm.tableOptions.layout.topStart).toBe('searchPanes');
        });
    });

    describe('columns and helpers', () => {
        it('defines expected column keys in order', () => {
            const wrapper = mountViewer();
            const columns = wrapper.vm.columns;

            expect(columns).toHaveLength(10);
            expect(columns.map((column) => column.data)).toEqual([
                'id',
                'title',
                'mgFullDescription',
                'altText',
                'mgTranscript',
                null,
                'mgSafetyAssessment',
                'mgReviewAssessment',
                'mgContentTags',
                null
            ]);
        });

        it('renders thumbnail and title columns from record id and title', () => {
            const wrapper = mountViewer();
            const row = { id: 'abc-123', title: 'Sample title' };

            const thumbnailCell = wrapper.vm.columns[0].render(row.id, 'display', row);
            const titleCell = wrapper.vm.columns[1].render(row.title, 'display', row);

            expect(thumbnailCell).toContain('/record/abc-123');
            expect(thumbnailCell).toContain('/services/api/thumb/abc-123/small');
            expect(titleCell).toContain('Sample title');
            expect(titleCell).toContain('/record/abc-123');
        });

        it('formats snake_case names into spaced labels', () => {
            const wrapper = mountViewer();
            expect(wrapper.vm.fieldName('misidentification_risk_people')).toBe('misidentification risk people');
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

            const wrapperWithCounts = mountViewer({
                globalTagCounts: {
                    people_visible: 3,
                    named_individuals: 2,
                    demographics: 1,
                    unsupported_claims: 1
                }
            });

            expect(wrapperWithCounts.vm.tagPaneOptions.map((option) => option.label)).toEqual([
                'people visible (3)',
                'named individuals (2)',
                'demographics (1)',
                'unsupported claims (1)'
            ]);
        });

        it('keeps insertion order for tags with equal counts', () => {
            const wrapper = mountViewer({ globalTagCounts: { beta: 2, alpha: 2 } });

            expect(wrapper.vm.tagPaneOptions.map((option) => option.label)).toEqual(['beta (2)', 'alpha (2)']);
        });

        it('builds option matchers that use tag values from the row', () => {
            const wrapper = mountViewer({ globalTagCounts: { 'tag-a': 5 } });
            const tagA = wrapper.vm.tagPaneOptions.find((option) => option.label === 'tag-a (5)');

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

            const listText = wrapper.vm.formatSafetyValue(sampleReviewAssessment.concerns_for_review);
            expect(listText).toContain('<ul>');
            expect(listText).toContain('unsupported inferential claim');

            expect(wrapper.vm.formatSafetyValue([])).toBe('none');
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
