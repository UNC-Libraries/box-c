const record_list = [
        {
            "added": "2017-12-20T13:44:46.154Z",
            "counts": {
                "child": "73"
            },
            "title": "Test Collection",
            "type": "Collection",
            "uri": "https://dcr.lib.unc.edu/record/dd8890d6-5756-4924-890c-48bc54e3edda",
            "id": "dd8890d6-5756-4924-890c-48bc54e3edda",
            "updated": "2018-06-29T18:38:22.588Z",
            "objectPath": [{ pid: "collections" }, { pid: "34e9ce20-0c7a-44a6-9fa4-d7cd27f7c502" }]
        },
        {
            "added": "2018-07-19T20:24:41.477Z",
            "counts": {
                "child": "1"
            },
            "title": "Test Collection 2",
            "type": "Collection",
            "uri": "https://dcr.lib.unc.edu/record/87f54f12-5c50-4a14-bf8c-66cf64b00533",
            "id": "87f54f12-5c50-4a14-bf8c-66cf64b00533",
            "updated": "2018-07-19T20:24:41.477Z",
            "objectPath": [{ pid: "collections" }, { pid: "34e9ce20-0c7a-44a6-9fa4-d7cd27f7c502" }]
        }
    ];

export const response = {
        container: {
            added: "2017-12-20T13:44:46.119Z",
            title: "Test Admin Unit",
            type: "AdminUnit",
            uri: "https://dcr.lib.unc.edu/record/73bc003c-9603-4cd9-8a65-93a22520ef6a",
            id: "73bc003c-9603-4cd9-8a65-93a22520ef6a",
            updated: "2017-12-20T13:44:46.264Z",
        },
        metadata: [...record_list, ...record_list, ...record_list, ...record_list], // Creates 8 returned records
        resultCount: 8,
        facetFields: [],
        filterParameters: {}
    };

export const briefObjectData = {
        briefObject: {
            added: "2023-01-17T13:52:29.596Z",
            counts: {
                child: 4
            },
            created: 1041379200000,
            title: "testCollection",
            type: "Collection",
            contentStatus: [
                "Described"
            ],
            rollup: "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
            objectPath: [
                {
                    pid: "collections",
                    name: "Content Collections Root",
                    container: true
                },
                {
                    pid: "353ee09f-a4ed-461e-a436-18a1bee77b01",
                    name: "testAdminUnit",
                    container: true
                },
                {
                    pid: "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
                    name: "testCollection",
                    container: true
                }
            ],
            datastream: [
                "thumbnail_small|image/png|fc77a9be-b49d-4f4e-b656-1644c9e964fc.png|png|6768|||",
                "thumbnail_large|image/png|fc77a9be-b49d-4f4e-b656-1644c9e964fc.png|png|23535|||",
                "event_log|application/n-triples|event_log.nt|nt|8206|urn:sha1:54fe67d57b965651e813eea1777c7f0332253168||",
                "md_descriptive_history|text/xml|||916|urn:sha1:efb4f2b6226d2932229f0e2b89128ec9a651de71||",
                "md_descriptive|text/xml|md_descriptive.xml|xml|283|urn:sha1:97f7dbdb806f724f9301445820ff1e0c9691cd6b||"
            ],
            ancestorPath: [
                {
                    id: "collections",
                    title: "collections"
                },
                {
                    id: "353ee09f-a4ed-461e-a436-18a1bee77b01",
                    title: "353ee09f-a4ed-461e-a436-18a1bee77b01"
                }
            ],
            _version_: 1760531096449056800,
            permissions: [
                "markForDeletionUnit",
                "move",
                "reindex",
                "destroy",
                "editResourceType",
                "destroyUnit",
                "bulkUpdateDescription",
                "changePatronAccess",
                "runEnhancements",
                "createAdminUnit",
                "ingest",
                "orderMembers",
                "viewOriginal",
                "viewAccessCopies",
                "viewMetadata",
                "viewHidden",
                "assignStaffRoles",
                "markForDeletion",
                "editDescription",
                "createCollection"
            ],
            groupRoleMap: {
                authenticated: [
                    "canViewOriginals"
                ],
                everyone: [
                    "canViewMetadata"
                ]
            },
            id: "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
            updated: "2023-02-21T18:37:17.705Z",
            status: [
                "Patron Settings"
            ],
            timestamp: 1678973288810
        },
        markedForDeletion: false,
        resourceType: "Collection"
    }