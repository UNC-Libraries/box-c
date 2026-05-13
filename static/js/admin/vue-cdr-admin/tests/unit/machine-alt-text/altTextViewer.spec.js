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

const mountViewer = () => {
    return shallowMount(altTextViewer, {
        global: {
            plugins: [createTestingPinia({
                initialState: {
                    'alt-text': {
                        currentUuid: uuid,
                        alertMessage: ''
                    }
                },
                stubActions: true
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
        it('has hasSearchPaneOptions based on contentTagFacets data', () => {
            const wrapper = mountViewer();
            expect(wrapper.vm.hasSearchPaneOptions).toBe(false);

            wrapper.vm.contentTagFacets = [{ value: 'people_visible' }];
            expect(wrapper.vm.hasSearchPaneOptions).toBe(true);
        });

        it('uses default table options for ordering, fixed header, and pagination', () => {
            const wrapper = mountViewer();
            const options = wrapper.vm.tableOptions;

            expect(options.order).toEqual([[1, 'asc']]);
            expect(options.fixedHeader).toBe(true);
            expect(options.pageLength).toBe(25);
            expect(options.layout.topEnd.search.placeholder).toBe('Search');
        });
    });

    describe('ajax options', () => {
        it('builds request params for current API payload', () => {
            const wrapper = mountViewer();
            const dataFn = wrapper.vm.ajaxOptions.data;

            const params = dataFn({ draw: 2, start: 50, length: 25, search: { value: 'dogs' }, order: [] });
            expect(params).toEqual({
                format: 'Image',
                rows: 25,
                page: 3,
                start: 50,
                anywhere: 'dogs',
                rollup: false
            });
            expect(params).not.toHaveProperty('mgContentTags');
            expect(params).not.toHaveProperty('sort');
        });

        it('sends joined mgContentTags when tags are selected', () => {
            const wrapper = mountViewer();
            wrapper.vm.selectedTags = ['people_visible', 'text_present'];

            const params = wrapper.vm.ajaxOptions.data({ draw: 3, start: 0, length: 25, search: { value: '' }, order: [] });
            expect(params.mgContentTags).toBe('people_visible||text_present');
            expect(params.anywhere).toBe('');
        });

        it('maps sort from title and risk score columns to API sort values', () => {
            const wrapper = mountViewer();

            const titleSort = wrapper.vm.ajaxOptions.data({
                draw: 4,
                start: 0,
                length: 25,
                search: { value: '' },
                order: [{ column: 1, dir: 'asc' }]
            });
            expect(titleSort.sort).toBe('title,normal');

            const riskSort = wrapper.vm.ajaxOptions.data({
                draw: 5,
                start: 0,
                length: 25,
                search: { value: '' },
                order: [{ column: 5, dir: 'desc' }]
            });
            expect(riskSort.sort).toBe('mgRiskScore,reverse');
        });

        it('maps datatables draw and result counts in dataFilter', () => {
            const wrapper = mountViewer();
            wrapper.vm.ajaxOptions.data({ draw: 7, start: 0, length: 25, search: { value: '' }, order: [] });

            const transformed = wrapper.vm.ajaxOptions.dataFilter(JSON.stringify({
                resultCount: 2,
                facetFields: []
            }));
            const json = JSON.parse(transformed);

            expect(json.draw).toBe(7);
            expect(json.recordsTotal).toBe(2);
            expect(json.recordsFiltered).toBe(2);
        });

        it('populates tag facets once from MG_CONTENT_TAGS in first response', () => {
            const wrapper = mountViewer();
            wrapper.vm.ajaxOptions.data({ draw: 1, start: 0, length: 25, search: { value: '' }, order: [] });

            const first = JSON.stringify({
                resultCount: 1,
                facetFields: [{
                    name: 'MG_CONTENT_TAGS',
                    values: [{ value: 'people_visible', displayValue: 'People Visible', count: 11 }]
                }]
            });
            wrapper.vm.ajaxOptions.dataFilter(first);
            expect(wrapper.vm.contentTagFacets).toEqual([
                { value: 'people_visible', displayValue: 'People Visible', count: 11 }
            ]);

            const second = JSON.stringify({
                resultCount: 1,
                facetFields: [{
                    name: 'MG_CONTENT_TAGS',
                    values: [{ value: 'text_present', displayValue: 'Text Present', count: 9 }]
                }]
            });
            wrapper.vm.ajaxOptions.dataFilter(second);
            expect(wrapper.vm.contentTagFacets).toEqual([
                { value: 'people_visible', displayValue: 'People Visible', count: 11 }
            ]);
        });
    });

    describe('columns and helpers', () => {
        it('defines expected column keys in order', () => {
            const wrapper = mountViewer();
            const columns = wrapper.vm.columns;

            expect(columns).toHaveLength(9);
            expect(columns.map((column) => column.data)).toEqual([
                'id',
                'title',
                'mgFullDescription',
                'altText',
                'mgTranscript',
                null,
                'mgSafetyAssessment',
                'mgReviewAssessment',
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