/**
 *
 * @param operation_totals
 * @param daily_deposits
 * @param scatter_tip
 * @constructor
 */
function CdrGraphs(operation_totals, deposit_totals, scatter_tip) {
    this.margins = {top: 50, right: 150, bottom: 75, left: 150 };
    this.stringDate = d3.time.format("%b %e, %Y");
    this.height = 500 - this.margins.top - this.margins.bottom;
    this.data_store = {};
    this.operations = operation_totals;
    this.deposits = deposit_totals;
    this.scatter_tip = scatter_tip;
}

/**
 * Create x axis time scales
 * @param data
 * @param width
 * @returns {*}
 */
CdrGraphs.prototype.xScales = function(data, width) {
    return d3.time.scale()
        .domain(d3.extent(data, d3.f("date")))
        .range([0, width]);
};

/**
 * Create y axis scales
 * @param data
 * @param field
 * @param range
 * @returns {*}
 */
CdrGraphs.prototype.yScales = function(data, field, range) {
    return d3.scale.sqrt()
        .domain([d3.max(data, d3.f(field)), 0])
        .range(range);
};

/**
 * Create axises
 * @param scale
 * @param orientation
 * @returns {*}
 */
CdrGraphs.prototype.getAxis = function(scale, orientation) {
    return d3.svg.axis()
        .scale(scale)
        .orient(orientation);
};

/**
 * Draw axises
 * @param selector
 * @param xAxis
 * @param yAxis
 * @param text
 * @returns {*}
 */
CdrGraphs.prototype.showAxises = function(selector, xAxis, yAxis, width, text) {
    var svg = d3.select(selector);

    svg.attr("width", width + this.margins.left + this.margins.right)
        .attr("height", this.height + this.margins.top + this.margins.bottom);

    svg.append("g")
        .attr("class", "x axis")
        .translate([this.margins.left, this.height + this.margins.top]);

    d3.selectAll(selector + " g.x").call(xAxis);

    svg.append("g")
        .attr("class", "y axis")
        .translate([this.margins.left, this.margins.top]);

    d3.selectAll(selector + " g.y").call(yAxis);

    svg.append("text")
        .attr("transform", "rotate(-90)")
        .attr("x", -this.height/2)
        .attr("y", 6)
        .attr("dy", "5em")
        .style("text-anchor", "end")
        .text(text);

    return svg;
};

/**
 * Draw circles for scatter plots
 * @param svg
 * @param data
 * @param xScale
 * @param yScale
 * @param field
 * @returns {*}
 */
CdrGraphs.prototype.drawCircles = function(svg, data, xScale, yScale, field) {
    var _that = this;
    var circles = svg.selectAll("circle")
        .data(data);
    circles.enter().append("circle");

    circles.translate([this.margins.left, this.margins.top])
        .on("mouseover", function(d) {
            var text = (/(time|throughput|duration)/.test(field)) ? _that.tipTextDeposits(d) : _that.tipTextOperations(d);
            _that.tipShow(_that.scatter_tip, text);
            d3.select(this).attr("r", 9).style("stroke-width", 3);
        }).on("mouseout", function(d) {
            _that.tipHide(_that.scatter_tip);
            d3.select(this).attr("r", 4.5).style("stroke-width", 1);
        });

    circles.transition().duration(1000)
        .delay(function(d, i) {
            return i * 5;
        })
        .ease("sin-in-out")
        .attr("cx", function(d) { return xScale(d.date); })
        .attr("cy", function(d) { return yScale(d[field]); })
        .attr("r", 4.5);

    circles.exit().remove();

    return circles;
};

/**
 * Format large numbers with commas & 2 decimal places or just commas if not a decimal number
 * @param number
 * @returns {*}
 */
CdrGraphs.prototype.numFormat = function(number) {
    var format= /\./.test(number) ? d3.format(",.2f") : d3.format(",");
    return format(number);
};

/**
 * Tooltip operations text
 * @param d
 * @returns {string}
 */
CdrGraphs.prototype.tipTextOperations = function(d) {
    var text = "<h5 class='text-center smaller'>" + this.stringDate(d.date) + "</h5>";

    text += "<p class='text-center'>Deposit Metrics</p>" +
        "<ul class='list-unstyled smaller'>" +
            "<li>" + "Files Ingested: " + this.numFormat(d.throughput_files) + "</li>" +
            "<li>" + "Total MB Ingested: " + this.numFormat(d.throughput_bytes) + "</li>" +
            "<li>" + "Avg Filesize (MB): " + this.numFormat(d.avg_filesize) + "</li>" +
        "</ul>" +

        "<p class='text-center'>Operations Metrics</p>" +

        "<ul class='list-unstyled smaller'>" +
        "<li>" + "Moves: " + this.numFormat(d.moves) + "</li>" +
        "<li>" + "Finished Enh: " + this.numFormat(d.finished_enhancements) + "</li>" +
        "<li>" + "Failed Enh: " + this.numFormat(d.failed_enhancements) + "</li>" +
        "</ul>"

    return text;
};

/**
 * Tooltip deposits text
 * @param d
 * @returns {string}
 */
CdrGraphs.prototype.tipTextDeposits = function(d) {
    var text = "<h5 class='text-center smaller'>" + this.stringDate(d.date) + "</h5>";

    text += (d.uuid !== undefined) ? d.uuid : "";
    text += "<p class='text-center'>Deposit Metrics</p>" +
        "<ul class='list-unstyled smaller'>";

    if (d.throughput_files !== undefined) {
        text += "<li>" + "Files Ingested: " + this.numFormat(d.throughput_files) + "</li>" +
            "<li>" + "Total MB Ingested: " + this.numFormat(d.throughput_bytes) + "</li>" +
            "<li>" + "Avg Filesize (MB): " + this.numFormat(d.avg_filesize) + "</li>";
    }

    text += "<li>" + "Total Time: " + this.numFormat(d.total_time) + "</li>" +
            "<li>" + "Queued Time: " + this.numFormat(d.queued_duration) + "</li>" +
            "<li>" + "Ingest Time: " + this.numFormat(d.ingest_duration) + "</li>" +
        "</ul>"

    return text;
};

/**
 * Show tooltip
 * @param tip
 * @param text
 */
CdrGraphs.prototype.tipShow = function(tip, text) {
    tip.transition()
        .duration(200)
        .style("opacity", .9);

    tip.html(text)
        .style("top", (d3.event.pageY+8)+"px")
        .style("left", (d3.event.pageX-100)+"px");
};

/**
 * Hide tooltip
 * @param tip
 */
CdrGraphs.prototype.tipHide = function(tip) {
    tip.transition()
        .duration(300)
        .style("opacity", 0);
};

/**
 * Bar width for barcode charts
 * @param width
 * @param data
 * @returns {number}
 */
CdrGraphs.prototype.barWidth = function(width, data) {
    return (width / data.length) - .3;
};

/**
 * Color list for different metric types
 * Color lists are from http://colorbrewer2.org/
 * @param type
 * @returns {Array}
 */
CdrGraphs.prototype.colorList = function(type) {
    switch(type) {
        case "throughput_bytes":
            return ['#fff7bc','#fee391','#fec44f','#fe9929','#ec7014','#cc4c02','#993404','#662506'];
            break;
        case "total_time":
            return ['#e7e1ef','#d4b9da','#c994c7','#df65b0','#e7298a','#ce1256','#980043','#67001f'];
            break;
        case "moves":
            return ['#ece7f2','#d0d1e6','#a6bddb','#74a9cf','#3690c0','#0570b0','#045a8d','#023858'];
            break;
        case "finished_enhancements":
            return ['#e5f5f9','#ccece6','#99d8c9','#66c2a4','#41ae76','#238b45','#006d2c','#00441b'];
            break;
        case "failed_enhancements":
            return ['#fee0d2','#fcbba1','#fc9272','#fb6a4a','#ef3b2c','#cb181d','#a50f15','#67000d'];
            break;
        default:
            return ['#fee0d2','#fcbba1','#fc9272','#fb6a4a','#ef3b2c','#cb181d','#a50f15','#67000d'];
            break;
    }
};

/**
 * Color codes for strip charts
 * @param data
 * @param type
 * @returns {*}
 */
CdrGraphs.prototype.stripColors = function(data, type) {
    var colors = this.colorList(type);

    return d3.scale.quantize()
        .domain([0, d3.max(data, function(d) { return d[type]})])
        .range(colors);
};

/**
 * Create legend
 * @param selector
 * @param type
 * @returns {*}
 */
CdrGraphs.prototype.drawLegend = function(selector, data, type) {
    var scale = this.stripColors(data, type);
    var class_name = "legend-" + type;

    var svg = d3.select(selector)
        .attr("height", 50)
        .attr("width", 1500);

    svg.append("g")
        .attr("class", class_name)
        .attr("width", 1200)
        .translate([this.margins.left, 0]);

    var legend = d3.legend.color()
        .shapeWidth(90)
        .cells(9)
        .orient("horizontal")
        .labelFormat(d3.format(",.4r"));

    legend.scale(scale);

    svg.select("." + class_name)
        .call(legend);

    return svg;
};

/**
 * Hide show loader/graphs
 */
CdrGraphs.prototype.hideShow = function() {
    d3.select("#loader").classed("hide", true);
    d3.selectAll(".dim").classed("dim", false);
};

/**
 * Update charts
 * @param xScale
 * @param yScale
 * @param axis
 */
CdrGraphs.prototype.chartUpdate = function(selector, xScale, yScale, axis) {
    var _that = this;
    var values;

    d3.selectAll("." + selector).on("click", function(d) {
        var selected_id = d3.event.target.id;
        var text = d3.select("#" + selected_id).text();
        var type, selected_chart;

        if (/^(all_throughput|all_avg)/.test(selected_id)) {
            type = selected_id.substr(4);
            selected_chart = "#files-by-day";
            values = _that.data_store["files-by-day"];
        } else if (/file/.test(selected_id)) {
            type = selected_id;
            selected_chart = "#files-by-ingest";
            values = _that.data_store["files-by-ingest"];
        } else if (/^all/.test(selected_id)) {
            type = selected_id.substr(4);
            selected_chart = "#duration-total-date";
            values = _that.data_store["duration-total-date"];
        } else {
            type = selected_id;
            selected_chart = "#duration-date";
            values = _that.data_store["duration-date"];
        }

        d3.select(selected_chart + "-text").text(text);
        yScale.domain([d3.max(values, function(d) { return d[type]}), 0]);

        d3.select(selected_chart + " g.y.axis")
            .transition().duration(1500).ease("sin-in-out")
            .call(axis);

        var chart = d3.select(selected_chart);
        _that.drawCircles(chart, values, xScale, yScale, type);
    });
};

/**
 * Comput average file size from data object
 * @param d
 * @returns {number}
 */
CdrGraphs.prototype.fileAvg = function(d) {
    var avg_size = d.throughput_bytes / d.throughput_files;
    return Number.isNaN(avg_size) ? 0 : avg_size;
};

/**
 * Sort data by date so it shows up correctly in graphs
 * @param data
 * @returns {*}
 */
CdrGraphs.prototype.dateSort = function(data) {
    return data.sort(function(a, b) {
        return a.date - b.date;
    });
};

/**
 * Calculate daily totals
 * @param data
 * @param field
 * @returns {Array}
 */
CdrGraphs.prototype.counts = function(data, field) {
    var nested = d3.nest()
        .key(function(d) { return d.date; })
        .rollup(function(values) {
            return d3.sum(values, function(d) {return d[field]; });
        })
        .entries(data);

    // Re-key as default d3.nest() keys are too general
    var counted = [];
    nested.forEach(function(d) {
        var data = {
            date: d.key
        };

        data[field] = +d.values;
        counted.push(data);
    });

    return counted;
};

/**
 * key function for the {combined} method to compare against
 * @returns {Function}
 */
CdrGraphs.prototype.keyFunction = function() {
    function keyFunction(key, value1, value2) {
        var accepted_value;
        if (key !== "date") {
            accepted_value = parseInt(value1) + parseInt(value2);
        } else if (key == "avg_filesize") {
            return; // Don't want to merge these. It will give weird results
        } else {
            accepted_value = (typeof value1 === "object") ? value1 : new Date(value1);
        }

        return accepted_value;
    }

    return keyFunction;
};

/**
 * Combine old throughput data by day total with newer throughput data by uuid/deposit
 * Sum values if not a date
 * Pick the first date if a date as the dates should be the same.
 * @param arr1
 * @param arr2
 * @returns {Array}
 */
CdrGraphs.prototype.combined = function(arr1, arr2) {
    var combined = [];
    var keyFunction = this.keyFunction();

    // Convert date objects to strings, otherwise they never match
    var dateValue = function(date_value) {
        return (typeof date_value === "object") ? date_value.toString() : date_value;
    };

    var duplicate_date = false;
    for (var i=0; i<arr1.length; i++) {
        for (var j=0; j<arr2.length; j++) {
            if(dateValue(arr1[i].date) == dateValue(arr2[j].date)) {
                combined.push(R.mergeWithKey(keyFunction, arr1[i], arr2[j]));
                duplicate_date = true;
                break;
            } else {
                duplicate_date = false;
            }
        }

        if (!duplicate_date) {
            combined.push(arr1[i]);
        }
    }

    return combined;
};