/**
 *
 * @param operation_totals
 * @param deposit_totals
 * @param scatter_tip
 * @constructor
 */
function CdrGraphs(operation_totals, deposit_totals, scatter_tip) {
    this.margins = {top: 20, right: 150, bottom: 25, left: 150 };
    this.stringDate = d3.time.format("%b %e, %Y");
    this.height = 500 - this.margins.top - this.margins.bottom;
    this.brush_height = 125 - this.margins.top - this.margins.bottom;
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
 * Create axis
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
 * Draw axis
 * @param selector
 * @param xAxis
 * @param yAxis
 * @param text
 * @returns {*}
 */
CdrGraphs.prototype.showAxises = function(selector, xAxis, yAxis, width, text) {
    var height_type;
    var is_brush = /brush/.test(selector);

    if (is_brush) {
        height_type = this.brush_height;
    } else {
        height_type = this.height;
    }
    var svg = d3.select(selector);

    svg.attr("width", width + this.margins.left + this.margins.right)
        .attr("height", height_type + this.margins.top + this.margins.bottom);

    svg.append("g")
        .attr("class", "x axis")
        .translate([this.margins.left, height_type + this.margins.top]);

    d3.selectAll(selector + " g.x").call(xAxis);

    if (!is_brush) {
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
    }

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
    
    circles.enter()
        .append("circle");

    circles.translate([this.margins.left, this.margins.top])
        .on("mouseover", function(d) {
            var text = (/(time|throughput|duration)/.test(field)) ? _that.tipType("deposits", d) : _that.tipType("operations", d);
            _that.tipShow(_that.scatter_tip, text);
            d3.select(this).attr("r", 9).style("stroke-width", 3);
        }).on("mouseout", function(d) {
            _that.tipHide(_that.scatter_tip);
            d3.select(this).attr("r", 4.5).style("stroke-width", 1);
        });

    circles.transition().duration(1000)
        .ease("sin-in-out")
        .attr("cx", function(d) { return xScale(d.date); })
        .attr("cy", function(d) { return yScale(d[field]); })
        .attr("r", 4.5);

    circles.exit().remove();

    return circles;
};

/**
 * Create line path function
 * @param xScale
 * @param yScale
 * @param y
 * @returns {*}
 */
CdrGraphs.prototype.lineGenerator = function(xScale, yScale, y) {
    return d3.svg.line()
        .interpolate("monotone")
        .x(function(d) { return xScale(d.date); })
        .y(function(d) { return yScale(d[y]); });
};

/**
 * Add svg path to a chart
 * @param svg
 * @param id
 * @param scale
 * @param data
 * @returns {*}
 */
CdrGraphs.prototype.appendPath = function(svg, id, scale, data) {
    svg.append("path#" + id)
        .attr("fill", "none")
        .attr("stroke", "lightgray")
        .attr("stroke-width", 2.5)
        .attr("d", scale(data))
        .translate([this.margins.left, this.margins.top]);

    return svg;
};

/**
 * Draw SVG path
 * @param selector
 * @param scale
 * @param data
 * @returns {*}
 */
CdrGraphs.prototype.redrawPath = function(selector, scale, data) {
    return d3.select(selector).transition()
        .duration(1000)
        .ease("sin-in-out")
        .attr("d", scale(data));
};

/**
 * Create a generator for a regression/trendline
 * @param data
 * @param field
 * @returns {Function|*}
 * @private
 */
CdrGraphs.prototype._trendLine = function (data, field) {
    var trend = data.map(function (d) {
        return [+d.date, d[field]];
    });
    var regression = ss.linearRegression(trend);
    return ss.linearRegressionLine(regression);
};

/**
 * Create data for a trend line
 * @param data
 * @param domain
 * @param field
 */
CdrGraphs.prototype.trendLineData = function (data, domain, field) {
    var regression = this._trendLine(data, field);

    return domain.map(function (d) {
        var trendData = {
            date: new Date(d)
        };
        trendData[field + "_trend"] = regression(d);

        return trendData;
    });
};

/**
 * Coerce string values to numbers
 * @param field
 * @returns {number}
 */
CdrGraphs.prototype.coerceToNum = function (field) {
    return (field === "") ? 0 : +field;
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
 * Tool tip operations text
 * @param d
 * @returns {string}
 */
CdrGraphs.prototype.tipTextOperations = function(d) {
    var text = "<h5 class='text-center smaller'>" + this.stringDate(d.date) + "</h5>";

    text += "<p class='text-center'>Metrics</p>" +
        "<ul class='list-unstyled smaller text-center'>" +
            "<li>" + "Files Ingested: " + this.numFormat(d.throughput_files) + "</li>" +
            "<li>" + "Total MB Ingested: " + this.numFormat(d.throughput_bytes) + "</li>" +
            "<li>" + "Avg Filesize (MB): " + this.numFormat(d.avg_filesize) + "</li>" +
            "<li>" + "Moves: " + this.numFormat(d.moves) + "</li>" +
            "<li>" + "Finished Deposits: " + this.numFormat(d.finished) + "</li>" +
            "<li>" + "Failed Deposits: " + this.numFormat(d.failed_deposit) + "</li>" +
        "</ul>";

    text += "<p class='text-center'>Enhancements</p>";

    text += "<ul class='list-unstyled smaller columns text-center'>" +
        "<li class='heading'>" + "Completed</li>" +
        "<li>" + "All: " + this.numFormat(d.finished_all_enh) + "</li>" +
        "<li>" + "Image: " + this.numFormat(d.image_enh) + "</li>" +
        "<li>" + "Metadata: " + this.numFormat(d.metadata_enh) + "</li>" +
        "<li>" + "Solr " + this.numFormat(d.solr_enh) + "</li>" +
        "<li>" + "Fulltext: " + this.numFormat(d.fulltext_enh) + "</li>" +
        "<li>" + "Thumbnail: " + this.numFormat(d.thumbnail_enh) + "</li>" +

        "<li class='heading'>" + "Failed</li>" +
        "<li>" + "All: " + this.numFormat(d.failed_all_enh) + "</li>" +
        "<li>" + "Image: " + this.numFormat(d.failed_image_enh) + "</li>" +
        "<li>" + "Metadata: " + this.numFormat(d.failed_metadata_enh) + "</li>" +
        "<li>" + "Solr: " + this.numFormat(d.failed_solr_enh) + "</li>" +
        "<li>" + "Fulltext: " + this.numFormat(d.failed_fulltext_enh) + "</li>" +
        "<li>" + "Thumbnail: " + this.numFormat(d.failed_thumbnail_enh) + "</li>" +
        "</ul>";

    return text;
};

/**
 * Tool tip deposits text
 * @param d
 * @returns {string}
 */
CdrGraphs.prototype.tipTextDeposits = function(d) {
    var text = "<h5 class='text-center smaller'>" + this.stringDate(d.date) + "</h5>";

    text += (d.uuid !== undefined) ? d.uuid : "";
    text += "<p class='text-center'>Deposit Metrics</p>" +
        "<ul class='list-unstyled smaller text-center'>";

    if (d.throughput_files !== undefined) {
        text += "<li>" + "Files Ingested: " + this.numFormat(d.throughput_files) + "</li>" +
            "<li>" + "Total MB Ingested: " + this.numFormat(d.throughput_bytes) + "</li>" +
            "<li>" + "Avg Filesize (MB): " + this.numFormat(d.avg_filesize) + "</li>";
    }

    text += "<li>" + "Total Time: " + this.numFormat(d.total_time) + "</li>" +
            "<li>" + "Queued Time: " + this.numFormat(d.queued_duration) + "</li>" +
            "<li>" + "Ingest Time: " + this.numFormat(d.ingest_duration) + "</li>" +
        "</ul>";

    return text;
};

/**
 * Figure out which tool tip type to show
 * @param whichTip
 * @param data
 */
CdrGraphs.prototype.tipType = function (whichTip, data) {
    return (whichTip === "deposits") ? this.tipTextDeposits(data) : this.tipTextOperations(data);
};

/**
 * Show tool tip
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
 * Hide show loader/graphs
 */
CdrGraphs.prototype.hideShow = function() {
    d3.select("#loader").classed("hide", true);
    d3.selectAll(".dim").classed("dim", false);
};

/**
 * Update charts
 * @param selector This is a unique class name in the parent div of the selected button group
 * @param params
 * @param brush
 */
CdrGraphs.prototype.chartUpdate = function(selector, params, brush) {
    var _that = this;
    var values;

    d3.selectAll("." + selector).on("click", function(d) {
        var selected_id = d3.event.target.id;
        var selected = "#" + selected_id;
        var text = d3.select(selected).text();
        var type, selected_chart;

        d3.selectAll("button").classed("clicked", false);
        d3.select(selected).classed('clicked', true);

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
        } else if (/^enh/.test(selected_id)) {
            type = selected_id.substr(4);
            selected_chart = "#enh-date";
            values = _that.data_store["enh-date"];
        } else if (/^dep/.test(selected_id)) {
            type = selected_id.substr(4);
            selected_chart = "#total-deposits-date";
            values = _that.data_store["total-deposits-date"];
        } else if (/^failed/.test(selected_id)) {
            type = selected_id;
            selected_chart = "#failed-enh-date";
            values = _that.data_store["failed-enh-date"];
        } else {
            type = selected_id;
            selected_chart = "#duration-date";
            values = _that.data_store["duration-date"];
        }

        // Rescale main chart
        d3.select(selected_chart + "-text").text(text);
        params.yScale.domain([d3.max(values, function(d) { return d[type]}), 0]);

        d3.select(selected_chart + " g.y.axis")
            .transition().duration(1500).ease("sin-in-out")
            .call(params.yAxis);

        // Check to see if a line chart or scatter plot
        var chart = d3.select(selected_chart);
        var lineScale;

        if (d3.select(selected_chart + "-line")[0][0] !== null && !brush) {
            lineScale = _that.lineGenerator(params.xScale, params.yScale, type);
            _that.redrawPath(selected_chart + "-line", lineScale, values);

        } else if (d3.select(selected_chart + "-line")[0][0] !== null && brush) {
            // Update main chart
            params.xScale.domain(d3.extent(values, d3.f('date')));

            lineScale = _that.lineGenerator(params.xScale, params.yScale, type);
            _that.redrawPath(selected_chart + "-line", lineScale, values);

            d3.select(selected_chart + " g.x.axis")
                .transition().duration(1500).ease("sin-in-out")
                .call(params.xAxis);

            // Update its brush chart. Remove old brush & add new one.
            params.brushYScale.domain([d3.max(values, function(d) { return d[type]}), 0]);
            var brushLineScale = _that.lineGenerator(params.xScale, params.brushYScale, type);
            _that.redrawPath(selected_chart + "-brush-line", brushLineScale, values);

          //  d3.select(selected_chart + "-brush g.brush").remove();
            params.field = type;
            params.data = values;
            brush.selectionBrushing(params.brushAxis, params);
        } else {
            _that.drawCircles(chart, values, params.xScale, params.yScale, type);
        }

        // Redisplay stats
        _that.statsDisplay(selected_chart + "-stats", values, type);
    });
};

/**
 * Compute average file size from data object
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
 * Filter data
 * @param data
 * @param value
 * @returns {*}
 */
CdrGraphs.prototype.dataFilter = function(data, value) {
    return data.filter(function(d) {
        return d[value] !== 0;
    });
};

/**
 * Get various stats parameters
 * @param data
 * @param type
 * @returns {{mean: *, median: *, min: *, max: *}}
 * @private
 */
CdrGraphs.prototype._stats = function(data, type) {
    return {
        mean: d3.mean(data, function(d) {
            return d[type];
        }),
        median: d3.median(data, function(d) {
            return d[type];
        }),
        min: d3.min(data, function(d) {
            return d[type];
        }),
        max: d3.max(data, function(d) {
            return d[type];
        })
    };
};

/**
 * Display stats
 * @param selector
 * @param data
 * @param type
 */
CdrGraphs.prototype.statsDisplay = function(selector, data, type) {
    var results = this._stats(data, type);
    var identifier;

    if (/(throughput_bytes|filesize)/.test(type)) {
        identifier = " MB";
    } else if (/(time|duration)/.test(type)) {
        identifier = " Sec";
    } else {
        identifier = "";
    }

    var stats = '<ul class="list-unstyled list-inline text-center">' +
        '<li>Mean: ' + this.numFormat(results.mean.toFixed(2)) + identifier + '</li>' +
        '<li>Median: ' + this.numFormat(results.median.toFixed(2)) + identifier + '</li>' +
        '<li>Min: ' + this.numFormat(results.min.toFixed(4)) + identifier + '</li>' +
        '<li>Max: ' + this.numFormat(results.max.toFixed(2)) + identifier + '</li>' +
    '</ul>';

    d3.select(selector).html(stats);
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

        if (key === "avg_filesize") {
            return; // Don't want to merge these. It will give weird results
        } else if (key !== "date") {
            accepted_value = parseInt(value1) + parseInt(value2);
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


/**
 * Pass in the CdrGraphs object context to get access to its methods
 * @param parent
 * @constructor
 */
function CreateBrush(parent) {
    this.parent = parent;
}

/**
 * Create brush to select subset of elements in a graph
 * Example of required fields in configuration object "params"
 *   {
 *      brushXScale: xScale2,
 *      xScale: xScale,
 *      yScale: yScale,
 *      xAxis: xAxis,
 *      yAxis: yAxis,
 *      data: throughput_all,
 *      field: "throughput_bytes",
 *      chart_id: "throughput-date"
 *   }
 * @param graph
 * @param params

 * @returns {*}
 */
CreateBrush.prototype.selectionBrushing = function(graph, params) {
    var _that = this;
    var brush_height = this.parent.brush_height;

    var brush = d3.svg.brush()
        .x(params.brushXScale)
        .on("brushend", brushed);

    var brushg = graph.append("g")
        .attr("class", "brush")
        .translate([this.parent.margins.left, 0])
        .call(brush);

    brushg.selectAll("rect")
        .attr("height", brush_height)
        .translate([0, this.parent.margins.top]);

    function brushed() {
        var updated, lineScale;

        if (!brush.empty()) {
            params.xScale.domain(brush.extent());
            updated = params.data.filter(function(d) {
                return d.date.getTime() >= brush.extent()[0].getTime() && d.date.getTime() <= brush.extent()[1].getTime() ;
            });
            params.yScale.domain([d3.max(updated, d3.f(params.field)), 0]);
            lineScale = _that.parent.lineGenerator(params.xScale, params.yScale, params.field);
        } else {
            params.xScale.domain(d3.extent(params.data, d3.f('date')));
            updated = params.data;
            params.yScale.domain([d3.max(params.data, d3.f(params.field)), 0]);
            lineScale = _that.parent.lineGenerator(params.xScale, params.yScale, params.field);
        }

        _that.parent.redrawPath("#" + params.chart_id + "-line", lineScale, updated);
        d3.select("#" + params.chart_id +" .x.axis").transition().duration(500).ease("sin-in-out").call(params.xAxis);
        d3.select("#" + params.chart_id +" .y.axis").transition().duration(500).ease("sin-in-out").call(params.yAxis);

        // Rescale trend line on brushing
        if (params.chart_id === "throughput-date") {
            var trendline_data = _that.parent.trendLineData(updated, params.xScale.domain(), params.field);
            var throughputLineScale = _that.parent.lineGenerator(params.xScale, params.yScale, params.field + "_trend");
            _that.parent.redrawPath("#" + params.chart_id + "-trend-line", throughputLineScale, trendline_data);
        }
    }

    return brushg;
};