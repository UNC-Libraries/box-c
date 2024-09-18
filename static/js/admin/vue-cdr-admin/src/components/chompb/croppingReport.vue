<!-- Teleport reports into #chompb-admin to get around issues with the admin app not being a Vue SPA -->
<template>
    <teleport to="#chompb-admin">
        <div class="cropping-report">
            <h2 class="chompb-ui has-text-weight-semibold is-size-3 has-text-centered">Cropping Report</h2>
            <div class="buttons has-addons">
                <span class="button is-success is-selected">
                    <a class="download" download="originals.csv" :href="downloadOriginals">
                        Original CSV
                    </a>
                </span>
                <span class="button">
                    <a class="download" download="corrections.csv" :href="downloadProblems">
                        Save Access Surrogate Mappings (with {{ problems.length }} corrections)
                    </a>
                </span>
            </div>
            <div class="columns">
                <div class="column is-half">Filters active - {{ active_filters.length }}</div>
                <div class="column is-half">
                    <div class="buttons has-addons pull-right" @click="setBulkActions($event)">
                        <span class="button is-outlined" id="collapse_all">Collapse All</span>
                        <span class="button is-outlined" id="show_all">Show All</span>
                        <span class="button is-outlined" id="clear_all">Clear All</span>
                    </div>
                </div>
            </div>
            <div class="columns">
                <div class="column is-half">
                   <cropping-actions :bulk-actions="bulk_actions" action-type="class" :cropping-data="dataSet"></cropping-actions>
                </div>
                <div class="column is-half">
                    <cropping-actions class="end" :bulk-actions="bulk_actions" action-type="problem" :cropping-data="dataSet"></cropping-actions>
                </div>
            </div>
            <data-table @click.prevent="markAsProblem($event)" class="table is-striped is-bordered is-fullwidth"
                        :data="dataSet"
                        :columns="columns"
                        :options="tableOptions">
                <thead>
                <tr>
                    <th>Image</th>
                    <th>Path</th>
                    <th>Class</th>
                    <th>Confidence</th>
                    <th>Problem</th>
                    <th></th>
                </tr>
                </thead>
            </data-table>
        </div>
    </teleport>
</template>

<script>
import croppingActions from "@/components/chompb/croppingActions.vue";
import DataTable from 'datatables.net-vue3';
import DataTablesLib from 'datatables.net-bm';

DataTable.use(DataTablesLib);

export default {
    name: 'croppingReport',

    components: {croppingActions, DataTable},

    data() {
        return {
            columns: [
                {data: 'Image'},
                {data: 'Path'},
                {data: 'Class'},
                {data: 'Confidence'},
                {data: 'Problem'},
                {data: ''}
            ],
            active_filters: [],
            problems: [],
            bulk_actions: {
                collapse_all: false,
                show_all: false,
                clear_all: false
            },
            dataSet: [[
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0001.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0001.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9998,
                    "bounding_box": [
                        0,
                        0.9236903889973959,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0002.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0002.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9998,
                    "bounding_box": [
                        0,
                        0.9221493530273438,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0003.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0003.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0,
                        0.0933916982014974,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0004.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0004.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0,
                        0.09525485992431641,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0005.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0005.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0.032540206909179685,
                        0.088524169921875,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.088524169921875,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0006.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0006.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0.042962074279785156,
                        0.09277694702148437,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.09277694702148437,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0007.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0007.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0.04094879150390625,
                        0.09246925354003906,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.09246925354003906,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0008.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0008.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0.04370193481445313,
                        0.09322967529296874,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.09322967529296874,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0009.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0009.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9991,
                    "bounding_box": [
                        0,
                        0,
                        0.09261907577514648,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0010.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0010.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9991,
                    "bounding_box": [
                        0,
                        0,
                        0.11748247782389323,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0011.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0011.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0,
                        0.12630569458007812,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0012.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0012.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9987,
                    "bounding_box": [
                        0,
                        0,
                        0.10643062591552735,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0013.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0013.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0.03156593322753906,
                        0.10613547007242839,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.10613547007242839,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0014.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0014.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0,
                        0.09789764404296875,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0015.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0015.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.032874107360839844,
                        0.08642219543457032,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.08642219543457032,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0016.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0016.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0,
                        0.09131301879882812,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0017.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0017.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9987,
                    "bounding_box": [
                        0,
                        0,
                        0.10542217254638672,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0018.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0018.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.999,
                    "bounding_box": [
                        0,
                        0,
                        0.11230127970377604,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0019.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0019.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9979,
                    "bounding_box": [
                        0,
                        0,
                        0.10349014282226562,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0020.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0020.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9985,
                    "bounding_box": [
                        0,
                        0,
                        0.08989662170410156,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0021.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0021.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0,
                        0.10780245463053385,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0022.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0022.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0.03611625671386719,
                        0.10035429000854493,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.10035429000854493,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0023.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0023.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9975,
                    "bounding_box": [
                        0,
                        0.10247493743896484,
                        0.07171953201293946,
                        0.9707792154947916
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.07171953201293946,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0024.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0024.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0,
                        0.09138092041015625,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0025.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0025.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0,
                        0.08463533401489258,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0026.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0026.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9988,
                    "bounding_box": [
                        0,
                        0,
                        0.08701344807942708,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0027.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0027.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0,
                        0.125692138671875,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0028.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0028.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0,
                        0.1255200958251953,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0029.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0029.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0,
                        0.13551928202311198,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0030.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0030.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9987,
                    "bounding_box": [
                        0,
                        0,
                        0.09001461029052735,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0031.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0031.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9998,
                    "bounding_box": [
                        0,
                        0,
                        0.09846160888671875,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0032.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0032.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0,
                        0.10144423166910807,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0033.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0033.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9986,
                    "bounding_box": [
                        0,
                        0,
                        0.10478094100952148,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0034.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0034.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9998,
                    "bounding_box": [
                        0,
                        0.0470025634765625,
                        0.08712032318115234,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.08712032318115234,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0035.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0035.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0,
                        0.1022863515218099,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0036.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0036.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0,
                        0.10242630004882812,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0037.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0037.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0,
                        0.09287931442260743,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0038.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0038.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0,
                        0.08492002487182618,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0039.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0039.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0,
                        0.1067535400390625,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0040.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0040.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9991,
                    "bounding_box": [
                        0,
                        0.02703277587890625,
                        0.09018369674682618,
                        0.973026123046875
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.09018369674682618,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0041.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0041.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0.05879707336425781,
                        0.09790437698364257,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.09790437698364257,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0042.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0042.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0,
                        0.12011019388834636,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0043.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0043.jpg",
                    "predicted_class": 0,
                    "predicted_conf": 0,
                    "bounding_box": "",
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0044.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0044.jpg",
                    "predicted_class": 0,
                    "predicted_conf": 0,
                    "bounding_box": "",
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0045.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0045.jpg",
                    "predicted_class": 0,
                    "predicted_conf": 0,
                    "bounding_box": "",
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0046.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0046.jpg",
                    "predicted_class": 0,
                    "predicted_conf": 0,
                    "bounding_box": "",
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0047.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0047.jpg",
                    "predicted_class": 0,
                    "predicted_conf": 0,
                    "bounding_box": "",
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0048.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0048.jpg",
                    "predicted_class": 0,
                    "predicted_conf": 0,
                    "bounding_box": "",
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0049.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0049.jpg",
                    "predicted_class": 0,
                    "predicted_conf": 0,
                    "bounding_box": "",
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0050.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0050.jpg",
                    "predicted_class": 0,
                    "predicted_conf": 0,
                    "bounding_box": "",
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0051.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0051.jpg",
                    "predicted_class": 0,
                    "predicted_conf": 0,
                    "bounding_box": "",
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0052.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0052.jpg",
                    "predicted_class": 0,
                    "predicted_conf": 0,
                    "bounding_box": "",
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0053.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0053.jpg",
                    "predicted_class": 0,
                    "predicted_conf": 0,
                    "bounding_box": "",
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0054.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0054.jpg",
                    "predicted_class": 0,
                    "predicted_conf": 0,
                    "bounding_box": "",
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0055.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0055.jpg",
                    "predicted_class": 0,
                    "predicted_conf": 0,
                    "bounding_box": "",
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0056.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0056.jpg",
                    "predicted_class": 0,
                    "predicted_conf": 0,
                    "bounding_box": "",
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0057.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0057.jpg",
                    "predicted_class": 0,
                    "predicted_conf": 0,
                    "bounding_box": "",
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0058.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0058.jpg",
                    "predicted_class": 0,
                    "predicted_conf": 0,
                    "bounding_box": "",
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0059.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0059.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0,
                        0.1090746815999349,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0060.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0060.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9984,
                    "bounding_box": [
                        0,
                        0,
                        0.09770600636800131,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0061.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0061.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0,
                        0.10773189544677735,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0062.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0062.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9988,
                    "bounding_box": [
                        0,
                        0,
                        0.10135330200195312,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0063.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0063.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9989,
                    "bounding_box": [
                        0,
                        0,
                        0.09935792922973632,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0064.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0064.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9998,
                    "bounding_box": [
                        0,
                        0,
                        0.09242490768432617,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0065.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0065.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9998,
                    "bounding_box": [
                        0,
                        0.0321038818359375,
                        0.09457765579223633,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.09457765579223633,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0066.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0066.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0,
                        0.09631528854370117,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0067.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0067.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9998,
                    "bounding_box": [
                        0,
                        0,
                        0.104737548828125,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0068.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0068.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0,
                        0.10646226247151692,
                        0.9598787434895834
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.10646226247151692,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0069.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0069.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.999,
                    "bounding_box": [
                        0,
                        0,
                        0.0971346918741862,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0070.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0070.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0,
                        0.11950602213541667,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0071.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0071.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0,
                        0.09878160476684571,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0072.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0072.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0,
                        0.09677042007446289,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0073.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0073.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9986,
                    "bounding_box": [
                        0,
                        0,
                        0.08938179016113282,
                        0.9608913167317709
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.08938179016113282,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0074.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0074.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9988,
                    "bounding_box": [
                        0,
                        0.0501702880859375,
                        0.08266000747680664,
                        0.9621077473958334
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.08266000747680664,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0075.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0075.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.999,
                    "bounding_box": [
                        0.05369895935058594,
                        0,
                        1,
                        0.04760137557983399
                    ],
                    "extended_box": [
                        0,
                        0,
                        1,
                        0.04760137557983399
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0076.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0076.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9999,
                    "bounding_box": [
                        0,
                        0,
                        0.10466747283935547,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0077.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0077.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.03780963897705078,
                        0.09857766469319662,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.09857766469319662,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0078.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0078.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0.0358090591430664,
                        0.0973763656616211,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.0973763656616211,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0079.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0079.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9987,
                    "bounding_box": [
                        0,
                        0.07530853271484375,
                        0.1108025868733724,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.1108025868733724,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0080.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0080.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9998,
                    "bounding_box": [
                        0,
                        0.03118030548095703,
                        0.09441350936889649,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.09441350936889649,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0081.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0081.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.05132804870605469,
                        0.08203596750895183,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.08203596750895183,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0082.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0082.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9998,
                    "bounding_box": [
                        0,
                        0.03935359954833984,
                        0.09221654891967773,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.09221654891967773,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0083.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0083.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.07470561981201172,
                        0.09033218383789063,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.09033218383789063,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0084.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0084.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9998,
                    "bounding_box": [
                        0,
                        0,
                        0.09558422088623048,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0085.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0085.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.027655181884765626,
                        0.09636146545410157,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.09636146545410157,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0086.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0086.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0.03857097625732422,
                        0.08664681752522786,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.08664681752522786,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0087.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0087.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0.058362274169921874,
                        0.09750989913940429,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.09750989913940429,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0088.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0088.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0,
                        0.10207467397054036,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0089.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0089.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0,
                        0.08249730428059895,
                        0.9738406372070313
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.08249730428059895,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0090.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0001/70103_pa0001_0090.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0.026390724182128907,
                        0.09334362030029297,
                        1
                    ],
                    "extended_box": [
                        0,
                        0,
                        0.09334362030029297,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0001.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0001.jpg",
                    "predicted_class": 0,
                    "predicted_conf": 0,
                    "bounding_box": "",
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0002.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0002.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.9058932495117188,
                        0.9730002848307292,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.9058932495117188,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0003.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0003.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.9016975911458334,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0004.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0004.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.9019994099934896,
                        0.9680819702148438,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.9019994099934896,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0005.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0005.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.9080032348632813,
                        0.9526752726236979,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.9080032348632813,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0006.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0006.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9986,
                    "bounding_box": [
                        0,
                        0.9042502848307291,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0007.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0007.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.998,
                    "bounding_box": [
                        0,
                        0.9034236653645833,
                        0.9571439615885416,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.9034236653645833,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0008.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0008.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9987,
                    "bounding_box": [
                        0,
                        0.899986572265625,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0009.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0009.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.999,
                    "bounding_box": [
                        0,
                        0.9082832845052083,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0010.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0010.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.9004475911458333,
                        0.9742169189453125,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.9004475911458333,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0011.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0011.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9998,
                    "bounding_box": [
                        0,
                        0.9047606404622396,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0012.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0012.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9991,
                    "bounding_box": [
                        0,
                        0.9046863810221354,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0013.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0013.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0.9031100463867188,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0014.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0014.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9989,
                    "bounding_box": [
                        0,
                        0.8994932047526042,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0015.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0015.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.8996742757161459,
                        0.9650172932942709,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.8996742757161459,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0016.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0016.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.9009328206380208,
                        0.9680777994791666,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.9009328206380208,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0017.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0017.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0.9011700439453125,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0018.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0018.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.8975862630208333,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0019.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0019.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9988,
                    "bounding_box": [
                        0,
                        0.9009514363606771,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0020.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0020.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.9049782307942709,
                        0.9741115315755209,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.9049782307942709,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0021.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0021.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0.900782470703125,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0022.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0022.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0.8943210856119792,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0023.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0023.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.8953342692057291,
                        0.9698513793945313,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.8953342692057291,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0024.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0024.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0.9015622965494792,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0025.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0025.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.9026572672526042,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0026.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0026.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.8931595865885417,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0027.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0027.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0.8950730387369792,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0028.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0028.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9988,
                    "bounding_box": [
                        0,
                        0.9065480550130208,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0029.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0029.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0.8961144002278646,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0030.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0030.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.8978102620442708,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0031.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0031.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9989,
                    "bounding_box": [
                        0,
                        0.9002366129557292,
                        0.9647115071614584,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.9002366129557292,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0032.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0032.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.8987874348958333,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0033.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0033.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.900089619954427,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0034.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0034.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.8979103597005208,
                        0.9659601847330729,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.8979103597005208,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0035.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0035.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.8954299926757813,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0036.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0036.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.8995187377929688,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0037.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0037.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9991,
                    "bounding_box": [
                        0,
                        0.8964201863606771,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0038.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0038.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0.9018392944335938,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0039.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0039.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.8996072387695313,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0040.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0040.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.896395263671875,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0041.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0041.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0.8959873453776042,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0042.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0042.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.8952785237630209,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0043.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0043.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9991,
                    "bounding_box": [
                        0,
                        0.8958572387695313,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0044.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0044.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9984,
                    "bounding_box": [
                        0,
                        0.8922627766927084,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0045.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0045.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.8983355712890625,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0046.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0046.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9991,
                    "bounding_box": [
                        0,
                        0.9006316121419271,
                        0.97063720703125,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.9006316121419271,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0047.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0047.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.8957704671223958,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0048.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0048.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9998,
                    "bounding_box": [
                        0,
                        0.9079972330729167,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0049.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0049.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.8957269287109375,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0050.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0050.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9998,
                    "bounding_box": [
                        0,
                        0.8964420572916667,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0051.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0051.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.999,
                    "bounding_box": [
                        0,
                        0.8949770100911458,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0052.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0052.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.8959665934244792,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0053.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0053.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.8962509155273437,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0054.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0054.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9987,
                    "bounding_box": [
                        0,
                        0.9048070271809896,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0055.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0055.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9988,
                    "bounding_box": [
                        0,
                        0.9029633585611979,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0056.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0056.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0.8978426106770834,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0057.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0057.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9988,
                    "bounding_box": [
                        0,
                        0.9040041097005208,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0058.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0058.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0.9030805460611979,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0059.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0059.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9991,
                    "bounding_box": [
                        0,
                        0.8973321533203125,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0060.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0060.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0.8985533650716145,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0061.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0061.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.8982601928710937,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0062.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0062.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.8966632080078125,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0063.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0063.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9988,
                    "bounding_box": [
                        0,
                        0.8981380208333334,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0064.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0064.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9998,
                    "bounding_box": [
                        0,
                        0.9040151977539063,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0065.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0065.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.8966898600260417,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0066.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0066.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.8953278605143229,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0067.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0067.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.8941475423177083,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0068.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0068.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0.8954130045572917,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0069.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0069.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9987,
                    "bounding_box": [
                        0,
                        0.8951737467447917,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0070.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0070.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0.9019260660807291,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0071.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0071.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0.8952672322591145,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0072.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0072.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.893754374186198,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0073.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0073.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0.8944851684570313,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0074.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0074.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.8952220662434895,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0075.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0075.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0.8989980061848958,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0076.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0076.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.999,
                    "bounding_box": [
                        0,
                        0.9039908854166666,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0077.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0077.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.999,
                    "bounding_box": [
                        0,
                        0.8978464762369792,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0078.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0078.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9991,
                    "bounding_box": [
                        0,
                        0.8992800903320313,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0079.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0079.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9984,
                    "bounding_box": [
                        0,
                        0.9062210083007812,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0080.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0080.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9989,
                    "bounding_box": [
                        0,
                        0.9013187662760417,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0081.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0081.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9985,
                    "bounding_box": [
                        0,
                        0.8977407836914062,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0082.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0082.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9986,
                    "bounding_box": [
                        0,
                        0.9035025024414063,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0083.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0083.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9988,
                    "bounding_box": [
                        0,
                        0.9019954427083333,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0084.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0084.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.8937140909830729,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0085.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0085.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.8959520467122396,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0086.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0086.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9991,
                    "bounding_box": [
                        0,
                        0.8989152018229166,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0087.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0087.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.8955221557617188,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0088.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0088.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.999,
                    "bounding_box": [
                        0,
                        0.8994551595052084,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0089.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0089.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.8963314819335938,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0090.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0090.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.999,
                    "bounding_box": [
                        0,
                        0.9021028645833333,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0091.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0091.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0.02943645477294922,
                        0.8951920572916666,
                        1,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.8951920572916666,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0092.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0092.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9991,
                    "bounding_box": [
                        0,
                        0.89771728515625,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0093.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0093.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0.8977542114257813,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0094.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0094.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9989,
                    "bounding_box": [
                        0,
                        0.9036039225260417,
                        0.9676418050130209,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.9036039225260417,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0095.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0095.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0.9006299845377604,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0096.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0096.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.999,
                    "bounding_box": [
                        0,
                        0.8970668538411458,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0097.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0097.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.999,
                    "bounding_box": [
                        0,
                        0.9006227620442708,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0098.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0098.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0.8939088948567708,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0099.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0099.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0.03202339172363281,
                        0.8892280069986979,
                        1,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.8892280069986979,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0100.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0100.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9989,
                    "bounding_box": [
                        0,
                        0.9001803588867188,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0101.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0101.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0.8959189860026041,
                        0.9681695556640625,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.8959189860026041,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0102.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0102.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.8904029337565104,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0103.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0103.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9986,
                    "bounding_box": [
                        0,
                        0.8973432413736979,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0104.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0104.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0.8948477172851562,
                        0.9642783610026041,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.8948477172851562,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0105.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0105.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9989,
                    "bounding_box": [
                        0,
                        0.8971697998046875,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0106.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0106.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.8984592692057292,
                        0.9674148559570312,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.8984592692057292,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0107.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0107.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9991,
                    "bounding_box": [
                        0,
                        0.8965260823567708,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0108.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0108.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.8972963460286458,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0109.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0109.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9998,
                    "bounding_box": [
                        0,
                        0.8989453125,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0110.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0110.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.8919598388671875,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0111.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0111.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.8944069417317708,
                        0.9632240804036458,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.8944069417317708,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0112.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0112.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9983,
                    "bounding_box": [
                        0,
                        0.8957216389973959,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0113.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0113.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9991,
                    "bounding_box": [
                        0,
                        0.9022773234049479,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0114.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0114.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.8899086507161459,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0115.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0115.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0.8981203206380208,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0116.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0116.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9991,
                    "bounding_box": [
                        0,
                        0.8952672322591145,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0117.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0117.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9985,
                    "bounding_box": [
                        0,
                        0.9002854410807292,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0118.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0118.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0.8975520833333334,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0119.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0119.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9989,
                    "bounding_box": [
                        0,
                        0.8983407592773438,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0120.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0120.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.8915264892578125,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0121.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0121.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9991,
                    "bounding_box": [
                        0,
                        0.8955045572916667,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0122.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0122.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.8936013793945312,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0123.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0123.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0.8989767456054687,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0124.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0124.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9989,
                    "bounding_box": [
                        0,
                        0.9003836059570313,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0125.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0125.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0.9006697591145834,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0126.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0126.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9988,
                    "bounding_box": [
                        0,
                        0.9015201822916666,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0127.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0127.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0.8981538899739583,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0128.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0128.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.8971360270182291,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0129.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0129.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9989,
                    "bounding_box": [
                        0,
                        0.8986538696289063,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0130.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0130.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.904228515625,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0131.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0131.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.8968081665039063,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0132.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0132.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.9004490152994792,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0133.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0133.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.8952853393554687,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0134.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0134.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9982,
                    "bounding_box": [
                        0,
                        0.893558349609375,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0135.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0135.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.8961563110351562,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0136.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0136.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.999,
                    "bounding_box": [
                        0,
                        0.8989902750651042,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0137.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0137.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.999,
                    "bounding_box": [
                        0,
                        0.9024506632486979,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0138.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0138.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9991,
                    "bounding_box": [
                        0,
                        0.8947134399414063,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0143.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0143.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.9036086018880208,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0139.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0139.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0.9067665608723958,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0140.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0140.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9984,
                    "bounding_box": [
                        0,
                        0.9004909261067708,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0141.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0141.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.8964054361979167,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0142.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0142.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0.8982550048828125,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0144.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0144.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.999,
                    "bounding_box": [
                        0,
                        0.8962203979492187,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0145.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0145.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9988,
                    "bounding_box": [
                        0,
                        0.8982901000976562,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0146.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0146.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.8990720621744792,
                        0.9561868286132813,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.8990720621744792,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0147.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0147.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9998,
                    "bounding_box": [
                        0,
                        0.9028684488932291,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0148.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0148.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9991,
                    "bounding_box": [
                        0,
                        0.9007548014322917,
                        0.9721018473307291,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.9007548014322917,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0149.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0149.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.8961305745442708,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0150.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0150.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0.8985188802083334,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0151.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0151.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9988,
                    "bounding_box": [
                        0,
                        0.9053290812174479,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0152.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0152.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.999,
                    "bounding_box": [
                        0,
                        0.8986555989583334,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0153.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0153.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.8999869791666667,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0154.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0154.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0.8980259195963541,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0155.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0155.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9983,
                    "bounding_box": [
                        0,
                        0.8989711507161459,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0156.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0156.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9987,
                    "bounding_box": [
                        0,
                        0.895746358235677,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0157.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0157.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9991,
                    "bounding_box": [
                        0,
                        0.8960638427734375,
                        0.9691545613606771,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.8960638427734375,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0158.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0158.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.8910585530598958,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0159.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0159.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9992,
                    "bounding_box": [
                        0,
                        0.8960479736328125,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0160.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0160.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.999,
                    "bounding_box": [
                        0,
                        0.8945699055989583,
                        0.9688960774739583,
                        1
                    ],
                    "extended_box": [
                        0,
                        0.8945699055989583,
                        1,
                        1
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0161.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pa0002/70103_pa0002_0161.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9987,
                    "bounding_box": [
                        0,
                        0.8999061075846354,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0001.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0001.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0,
                        0.10493070602416993,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0002.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0002.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0,
                        0.1045760154724121,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0003.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0003.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0.8966636149088542,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0004.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0004.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0,
                        0.10329530715942382,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0005.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0005.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0.8897667439778646,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0006.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0006.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0,
                        0.09993232727050781,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0007.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0007.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0.8915080769856771,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0008.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0008.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0,
                        0.10387649536132812,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0009.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0009.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0.8955137125651041,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0010.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0010.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9993,
                    "bounding_box": [
                        0,
                        0,
                        0.10287078857421875,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0011.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0011.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0.8948977661132812,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0012.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0012.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0,
                        0.10680292765299479,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0013.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0013.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0.8895253499348958,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0014.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0014.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0,
                        0.10411322275797526,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0015.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0015.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0.8862113444010417,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0016.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0016.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0,
                        0.10194959640502929,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0017.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0017.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0.882467041015625,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0018.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0018.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0,
                        0.10586488723754883,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0019.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0019.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0.8816331990559896,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0020.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0020.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0,
                        0.10641872406005859,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0021.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0021.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0.8876132202148438,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0022.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0022.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0,
                        0.10348130544026693,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0023.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0023.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0.8920444742838541,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0024.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0024.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0,
                        0.10595048904418945,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0025.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0025.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0.8945882161458333,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0026.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0026.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0,
                        0.10622926712036133,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0027.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0027.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0.8884724934895833,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0028.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0028.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0,
                        0.10713703155517579,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0029.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0029.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0.8989763387044271,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0030.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0030.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0,
                        0.10325422922770182,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0031.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0031.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0.8884844970703125,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0032.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0032.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0,
                        0.10113012313842773,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0035.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0035.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0.8956266276041667,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0033.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0033.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0.8891387939453125,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0034.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0034.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0,
                        0.10656083424886068,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0036.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0036.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0,
                        0.1070705795288086,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0037.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0037.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0.8893411254882813,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0038.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0038.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0,
                        0.10379470825195312,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0039.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0039.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9998,
                    "bounding_box": [
                        0.8952937825520834,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0040.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0040.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0,
                        0.11672645568847656,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0041.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0041.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0.8871282958984374,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0042.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0042.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0,
                        0.10967912038167317,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0043.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0043.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0.8856601969401041,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0044.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0044.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0,
                        0.10971725463867188,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0045.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0045.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0.9002993774414062,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0046.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0046.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0,
                        0.11585248311360677,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0047.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0047.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0.8785330200195313,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0048.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0048.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0,
                        0.11961072285970052,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0049.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0049.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0.8821809895833334,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0050.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0050.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0,
                        0.11475475311279297,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0051.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0051.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0.8845215861002604,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0052.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0052.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0,
                        0.11347860972086589,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0053.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0053.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0.8814404296875,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0054.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0054.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0,
                        0.11500540415445963,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0055.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0055.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0.8790884399414063,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0056.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0056.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0,
                        0.12015140533447266,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0057.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0057.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0.8929944864908854,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0058.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0058.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0,
                        0.11963325500488281,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0059.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0059.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0.8849500528971355,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0060.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0060.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0,
                        0.10622894287109375,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0061.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0061.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0.8797439575195313,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0062.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0062.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0,
                        0,
                        0.1156592051188151,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0063.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0063.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0.8896842447916666,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0064.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0064.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0,
                        0.11937276204427083,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0065.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0065.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0.8819924926757813,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0066.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0066.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0,
                        0,
                        0.1155804189046224,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0067.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0067.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9995,
                    "bounding_box": [
                        0.8817349243164062,
                        0,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0070.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0070.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9998,
                    "bounding_box": [
                        0,
                        0,
                        0.19094635009765626,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0068.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0068.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9994,
                    "bounding_box": [
                        0,
                        0.8832792154947917,
                        1,
                        1
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0069.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0069.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9997,
                    "bounding_box": [
                        0,
                        0,
                        1,
                        0.07470242818196615
                    ],
                    "extended_box": ""
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0071.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0071.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9996,
                    "bounding_box": [
                        0.1274283218383789,
                        0,
                        1,
                        0.07239668528238932
                    ],
                    "extended_box": [
                        0,
                        0,
                        1,
                        0.07239668528238932
                    ]
                },
                {
                    "original_path": "/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0072.tif",
                    "normalized_path": "/opt/data/color_bars/shared/normalized/mnt/locos/ncc/nccpa/70103_wallace_wpa/70103_pf0001/70103_pf0001_0072.jpg",
                    "predicted_class": 1,
                    "predicted_conf": 0.9623,
                    "bounding_box": [
                        0,
                        0.6192103068033854,
                        1,
                        0.9692531331380209
                    ],
                    "extended_box": [
                        0,
                        0,
                        1,
                        1
                    ]
                }
            ]]
        }
    },

    computed: {
        tableOptions() {
            return {
                columnDefs: this.columnDefs,
                language: {search: '', searchPlaceholder: 'Search'},
                order: [[1, 'asc']]
            }
        },

        columnDefs() {
            const excluded_columns = [0, 5];

            return [
                { orderable: false, targets: excluded_columns },
                { searchable: false, target: excluded_columns },
                {
                    render: (data, type, row) => {
                        return `<img src="${row.normalized_path}" alt="" role="presentation" />`;
                    }, targets: 0
                },
                {
                    render: (data, type, row) => {
                        return row.original_path;
                    }, targets: 1
                },
                {
                    render: (data, type, row) => {
                        return row.pred_class;
                    }, targets: 2
                },
                {
                    render: (data, type, row) => {
                        return row.pred_conf;
                    }, targets: 3
                },
                {
                    render: (data, type, row) => {
                        return row.problem ? "problem detected" : "";
                    }, targets: 4
                },
                {
                    render: (data, type, row) => {
                        return `<button class="button is-outlined incorrect" value="incorrect" data-path="${row.original_path}"` + `
                                data-predicted="${row.pred_class}">Mark incorrect</button>`;
                    }, targets: 5
                }
            ];
        },

        downloadOriginals() {
            let csvContent = "original_path,normalized_path,predicted_class,predicted_conf\n";
            csvContent += this.dataSet.map(d => {
                return `${d.original},${d.image},${d.pred_class},${d.pred_conf}`;
            }).join("\n");
            let data = new Blob([csvContent]);
            return  URL.createObjectURL(data);
        },

        downloadProblems() {
            let csvContent = "path,predicted_class,corrected_class\n"
            csvContent += this.problems.map(d => {
                return `${d.path},${d.predicted},${d.predicted === '1' ? '0' : '1'}`
            }).join("\n");
            let data = new Blob([csvContent]);
            return  URL.createObjectURL(data);
        }
    },

    methods: {
        setBulkActions(e) {
            let action = e.target.id;
            this.bulk_actions[action] = !this.bulk_actions[action];
            if (action === 'collapse_all') {
                this.bulk_actions.show_all = false;
            } else if (action === 'show_all') {
                this.bulk_actions.collapse_all = false;
            }
        },

        /**
         * We can't make updating button classes in the table reactive, so update them as we add/remove items from
         * the problem bin
         * @param e
         */
        markAsProblem(e) {
            if (e.target.classList.contains('incorrect')) {
                const image_path = e.target.dataset.path;
                const marked_as_problem = this.problems.findIndex(d => d.original_path === image_path);

                if (marked_as_problem === -1) {
                    this.problems.push({
                        path: image_path,
                        predicted: e.target.dataset.predicted
                    });
                    e.target.classList.add('is-danger');
                    e.target.classList.remove('is-outlined');
                } else {
                    this.problems.splice(marked_as_problem, 1);
                    e.target.classList.add('is-outlined');
                    e.target.classList.remove('is-danger');
                }
            }
        }
    }
}
</script>

<style lang="scss">
    /* Seems to be a bug with datatables 2.x that if styles are scoped the import isn't picked up */
    @import 'datatables.net-bm';

    .cropping-report {
        width: 96%;
        margin: auto;

        h2 {
            margin: 25px;
        }

        .download {
            text-decoration: none;
            &:hover,
            &:focus {
                text-decoration: none;
            }
            color: black;
        }

        .is-success {
            .download {
                color: white;
            }
        }
    }
</style>