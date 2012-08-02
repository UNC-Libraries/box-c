var Grant = {
	title : 'grant',
	elementTitle : 'grant',
	repeatable : true,
	type : 'none',
	singleton : false,
        attributes : [ group_attr, role_attr ],
	elements : [ ]
};

var EmbargoGroups = {
	title : 'embargoGroups',
	elementTitle : 'embargoGroups',
	repeatable : true,
	type : 'none',
	singleton : false,
        attributes : [ group_attr ],
	elements : [ ]
};

var Embargo = {
	title : 'embargo',
	elementTitle : 'embargo',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ ],
	elements : [ EmbargoGroups ]
};

var Inherit = {
	title : 'inherit',
	elementTitle : 'inherit',
	repeatable : false,
	type : 'selection',
	values : ['', 'inherit permissions', 'do not inherit permissions'],
	singleton : true,
        attributes : [ ],
	elements : [ ]
};
