import { shallowMount } from '@vue/test-utils';
import { vi } from 'vitest';
import { createTestingPinia } from '@pinia/testing';
import altTextViewer from '@/components/machine-alt-text/altTextViewer.vue';

const sanitizeSpy = vi.fn((text) => text);

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
    beforeEach(() => {
        sanitizeSpy.mockClear();
        globalThis.DOMPurify = { sanitize: sanitizeSpy };
    });

    afterAll(() => {
        delete globalThis.DOMPurify;
    });

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

            expect(options.order).toEqual([[0, 'asc']]);
            expect(options.fixedHeader).toBe(true);
            expect(options.pageLength).toBe(25);
            expect(options.layout.topEnd.search.placeholder).toBe('Search');
        });

        it('includes column visibility and reset controls in table layout', () => {
            const wrapper = mountViewer();
            const options = wrapper.vm.tableOptions;

            expect(options.layout.top2Start.buttons[0].extend).toBe('colvis');
            expect(options.layout.top2End.buttons[0].text).toBe('Clear Filters and Reload Data');
            expect(typeof options.layout.top2End.buttons[0].action).toBe('function');
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
                rollup: false,
                getFacets: true,
                facetLimits: `mgContentTags${encodeURIComponent(':')}50`,
                facetSelect: 'mgContentTags'
            });
            expect(params).not.toHaveProperty('mgContentTags');
            expect(params).not.toHaveProperty('sort');
        });

        it('sends joined mgContentTags when tags are selected', () => {
            const wrapper = mountViewer();
            wrapper.vm.selectedTags = ['people_visible', 'text_present'];

            const params = wrapper.vm.ajaxOptions.data({ draw: 3, start: 0, length: 25, search: { value: '' }, order: [] });
            expect(params.mgContentTags).toBe('people_visible%7C%7Ctext_present');
            expect(params.anywhere).toBe('');
        });

        it('maps sort from title and risk score columns to API sort values', () => {
            const wrapper = mountViewer();

            const titleSort = wrapper.vm.ajaxOptions.data({
                draw: 4,
                start: 0,
                length: 25,
                search: { value: '' },
                order: [{ column: 0, dir: 'asc' }]
            });
            expect(titleSort.sort).toBe('title,normal');

            const riskSort = wrapper.vm.ajaxOptions.data({
                draw: 5,
                start: 0,
                length: 25,
                search: { value: '' },
                order: [{ column: 7, dir: 'desc' }]
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

        it('refreshes tag facets from MG_CONTENT_TAGS on each response', () => {
            const wrapper = mountViewer();
            wrapper.vm.ajaxOptions.data({ draw: 1, start: 0, length: 25, search: { value: '' }, order: [] });

            const first = JSON.stringify({
                resultCount: 1,
                facetFields: [{
                    name: 'MG_CONTENT_TAGS',
                    values: [{ value: 'people_visible', displayValue: 'People Visible', count: 11 }]
                }]
            });
            const firstParsed = JSON.parse(wrapper.vm.ajaxOptions.dataFilter(first));
            wrapper.vm.ajaxOptions.dataSrc(firstParsed);
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
            const secondParsed = JSON.parse(wrapper.vm.ajaxOptions.dataFilter(second));
            wrapper.vm.ajaxOptions.dataSrc(secondParsed);
            expect(wrapper.vm.contentTagFacets).toEqual([
                { value: 'text_present', displayValue: 'Text Present', count: 9 }
            ]);
        });
    });

    describe('columns and helpers', () => {
        it('defines expected column keys in order', () => {
            const wrapper = mountViewer();
            const columns = wrapper.vm.columns;

            expect(columns).toHaveLength(10);
            expect(columns.map((column) => column.data)).toEqual([
                'id',
                'mgFullDescription',
                'fullDescription',
                'mgAltText',
                'altText',
                'mgTranscript',
                'transcript',
                null,
                'mgSafetyAssessment',
                'mgReviewAssessment'
            ]);
        });

        it('renders thumbnail markup and uses image filename for column sorting', () => {
            const wrapper = mountViewer();
            const row = { id: 'abc-123', title: 'Sample title' };

            const displayCell = wrapper.vm.columns[0].render(row.id, 'display', row);
            const sortCell = wrapper.vm.columns[0].render(row.id, 'sort', row);

            expect(displayCell).toContain('/record/abc-123');
            expect(displayCell).toContain('/services/api/thumb/abc-123/large');
            expect(displayCell).toContain('<figcaption>Sample title</figcaption>');
            expect(sortCell).toBe('Sample title');
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

    describe('successful row edits', () => {
        it('patches the matching datatable row and clears the successful edit flag', () => {
            const row = {
                _data: { id: 'abc-123', altText: 'Old text' },
                data(newData) {
                    if (newData) {
                        this._data = newData;
                        return this;
                    }
                    return this._data;
                }
            };

            const ctx = {
                $refs: {
                    alt_text_table: {
                        dt: {
                            rows() {
                                return {
                                    every(callback) {
                                        callback.call(row);
                                    }
                                };
                            }
                        }
                    }
                },
                clearLastSuccessfulEdit: vi.fn()
            };

            altTextViewer.methods.applySuccessfulEdit.call(ctx, { id: 'abc-123', field: 'altText', value: 'New text' });

            expect(row._data.altText).toBe('New text');
            expect(ctx.clearLastSuccessfulEdit).toHaveBeenCalled();
        });

        it('does not patch rows when no matching id is found and still clears the successful edit flag', () => {
            const row = {
                _data: { id: 'different-id', altText: 'Old text' },
                data(newData) {
                    if (newData) {
                        this._data = newData;
                        return this;
                    }
                    return this._data;
                }
            };

            const ctx = {
                $refs: {
                    alt_text_table: {
                        dt: {
                            rows() {
                                return {
                                    every(callback) {
                                        callback.call(row);
                                    }
                                };
                            }
                        }
                    }
                },
                clearLastSuccessfulEdit: vi.fn()
            };

            altTextViewer.methods.applySuccessfulEdit.call(ctx, { id: 'abc-123', field: 'altText', value: 'New text' });

            expect(row._data.altText).toBe('Old text');
            expect(ctx.clearLastSuccessfulEdit).toHaveBeenCalled();
        });

        it('clears successful edit and exits when the Datatables API is missing or edit payload is incomplete', () => {
            const clearLastSuccessfulEdit = vi.fn();
            const noApiCtx = { $refs: {}, clearLastSuccessfulEdit };
            const withApiCtx = {
                $refs: {
                    alt_text_table: {
                        dt: {
                            rows() {
                                return {
                                    every() {}
                                };
                            }
                        }
                    }
                },
                clearLastSuccessfulEdit
            };

            altTextViewer.methods.applySuccessfulEdit.call(noApiCtx, { id: 'abc-123', field: 'altText', value: 'New text' });
            altTextViewer.methods.applySuccessfulEdit.call(withApiCtx, { id: 'abc-123', value: 'New text' });
            altTextViewer.methods.applySuccessfulEdit.call(withApiCtx, { field: 'altText', value: 'New text' });

            expect(clearLastSuccessfulEdit).toHaveBeenCalledTimes(3);
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

        it('does not render edit link for machine-generated fields in longText', () => {
            const wrapper = mountViewer();
            const machineText = wrapper.vm.longText('short text', 'mgAltText');
            expect(machineText).not.toContain('data-action="edit"');
        });

        it('formats safety values for arrays, objects, and empty values', () => {
            const wrapper = mountViewer();

            const listText = wrapper.vm.formatSafetyValue(sampleReviewAssessment.concerns_for_review);
            expect(listText).toContain('<ul>');
            expect(listText).toContain('unsupported inferential claim');

            expect(wrapper.vm.formatSafetyValue([])).toBe('none');
            expect(wrapper.vm.formatSafetyValue(null)).toBe('None');
            expect(wrapper.vm.formatSafetyValue(undefined)).toBe('None');
            expect(wrapper.vm.formatSafetyValue('')).toBe('None');
            expect(wrapper.vm.formatSafetyValue(sampleReviewAssessment.stereotyping)).toBe('no');

            const objectText = wrapper.vm.formatSafetyValue(sampleSafetyAssessment);
            expect(objectText).toContain('people visible');
            expect(objectText).toContain('symbols present');
        });

        it('formats nested objects inside arrays without flattening to object strings', () => {
            const wrapper = mountViewer();
            const nestedArray = [{ concern_level: 'HIGH' }];

            const text = wrapper.vm.formatSafetyValue(nestedArray);
            expect(text).toContain('concern level');
            expect(text).toContain('high');
            expect(text).not.toContain('[object Object]');
        });

        it('renders structured safety data list', () => {
            const wrapper = mountViewer();
            const rendered = wrapper.vm.renderSafetyData(sampleSafetyAssessment);
            expect(rendered).toContain('<ul class="is-capitalized">');
            expect(rendered).toContain('people visible');
            expect(rendered).toContain('symbols present');
            expect(sanitizeSpy).toHaveBeenCalled();
        });

        it('renders structured safety data list for null and empty inputs', () => {
            const wrapper = mountViewer();

            const renderedNull = wrapper.vm.renderSafetyData(null);
            const renderedEmpty = wrapper.vm.renderSafetyData({});

            expect(renderedNull).toContain('<ul class="is-capitalized">');
            expect(renderedEmpty).toContain('<ul class="is-capitalized">');
            expect(sanitizeSpy).toHaveBeenCalledTimes(2);
        });
    });
});