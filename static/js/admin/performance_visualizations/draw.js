/**
 * Draw graphs
 */

CdrGraphs.prototype.draw = function() {
    /**
     * Basic setup & data manipulation
     *
     *
     */
    var _that = this;
    var width = window.innerWidth - this.margins.left - this.margins.right;
    var parseDate = d3.time.format("%Y-%m-%d").parse;

    // Fields where metrics are by day only (e.g.) older dates & operations metrics
    this.operations.forEach(function(d) {
        d.date = (typeof d.date === "object") ? d.date : parseDate(d.date);
        d.throughput_bytes = (d.throughput_bytes === "") ? 0 : d.throughput_bytes / 1000000;
        d.throughput_files = (d.throughput_files === "") ? 0 : +d.throughput_files;
        d.moves = (d.moves === "") ? 0 : +d.moves;
        d.finished_enhancements = (d.finished_enhancements === "") ? 0 : +d.finished_enhancements;
        d.failed_enhancements = (d.failed_enhancements === "") ? 0 : +d.failed_enhancements;
    });

    var data = this.dateSort(this.operations);
    
    // Metrics by uuid & day, newer deposit date metrics
    this.deposits.forEach(function(d) {
        d.date = (typeof d.date === "object") ? d.date : parseDate(d.date);
        d.ingest_duration = d.ingest_duration / 1000;
        d.queued_duration = d.queued_duration / 1000;
        d.total_time = d.ingest_duration + d.queued_duration;
        d.throughput_bytes = (d.throughput_bytes === "") ? 0 : d.throughput_bytes / 1000000;
        d.throughput_files = (d.throughput_files === "") ? 0 : +d.throughput_files;
        d.avg_filesize = _that.fileAvg(d);
    });

    var deposits_filtered = this.dataFilter(this.deposits, "throughput_bytes");
    var deposits_by_uuid = this.dateSort(deposits_filtered);

    var height_range = [0, this.height];

    // X-axis scales for charts
    var xScale = this.xScales(data, width);
    var xScaleUUID = this.xScales(deposits_by_uuid, width);

    // Get totals data for charts for uuid metrics & and merge it with totals data from operations metrics
    var throughput_bytes_uuid_totals = this.counts(deposits_by_uuid, "throughput_bytes");
    var throughput_files_uuid_totals = this.counts(deposits_by_uuid, "throughput_files");

    var throughput_combine = this.combined(throughput_bytes_uuid_totals, throughput_files_uuid_totals);
    var throughput_all = this.combined(data, throughput_combine);

    // Duration totals by day
    var combine_deposits_duration = this.combined(
        this.counts(deposits_by_uuid, "ingest_duration"),
        this.counts(deposits_by_uuid, "queued_duration")
    );

    var uuid_all = this.combined(
        combine_deposits_duration,
        this.counts(deposits_by_uuid, "total_time")
    );

    // Only average file sizes after everything is combined
    throughput_all.forEach(function(d) {
        d.avg_filesize = _that.fileAvg(d);
    });

    uuid_all.forEach(function(d) {
        d.avg_filesize = _that.fileAvg(d);
    });


    /**
     *  Scatter plot & Strip plot - megabytes by date
     *
     *
     **/
    var throughput = "throughput_bytes";
    
 // By uuid
    var yScaleDeposits = this.yScales(deposits_by_uuid, throughput, height_range);

    var xAxisDeposits = this.getAxis(xScaleUUID, "bottom");
    var yAxisDeposits  = this.getAxis(yScaleDeposits, "left");
    
    this.statsDisplay("#throughput-uuid-stats", deposits_by_uuid, throughput);
    var throughput_uuid = this.showAxises("#throughput-deposit", xAxisDeposits , yAxisDeposits , width, "Throughput (MB)");
    this.drawCircles(throughput_uuid, deposits_by_uuid, xScaleUUID, yScaleDeposits, throughput);
    
    // By date
    var yScale = this.yScales(throughput_all, throughput, height_range);

    var xAxis = this.getAxis(xScale, "bottom");
    var yAxis = this.getAxis(yScale, "left");
    
    this.statsDisplay("#throughput-stats", throughput_all, throughput);
    var throughput_date = this.showAxises("#throughput-date", xAxis, yAxis, width, "Throughput (MB)");
    
    var throughputLineScaleTotals = this.lineGenerator(xScale, yScale, throughput);
    this.appendPath(throughput_date, "throughput-date-line", throughputLineScaleTotals, throughput_all);
    
   // this.drawCircles(throughput_date, throughput_all, xScale, yScale, throughput);
    focusHover(throughput_date, throughput_all, "#throughput-date");

    this.drawLegend("#throughput-legend", throughput_all, throughput);
    drawStrip("#throughput-date-strip", throughput_all, throughput);

    /**
     * File Counts by Deposit & Date
     *
     *
     */

    var throughput_files = "throughput_files";
    var throughput_avg_size = "avg_filesize";

    // Files by Deposit
    var yScaleFilesUUID = this.yScales(deposits_by_uuid, throughput_files, height_range);

    var xAxisFilesUUID = this.getAxis(xScaleUUID, "bottom");
    var yAxisFilesUUID = this.getAxis(yScaleFilesUUID, "left");

    this.statsDisplay("#files-by-ingest-stats", deposits_by_uuid, throughput_files);
    var throughput_files_uuid = this.showAxises("#files-by-ingest", xAxisFilesUUID, yAxisFilesUUID, width, "Throughput (Files)");
    this.drawCircles(throughput_files_uuid, deposits_by_uuid, xScaleUUID, yScaleFilesUUID, throughput_files);	
    this.data_store["files-by-ingest"] = deposits_by_uuid;
    this.chartUpdate("files-uuid", xScaleUUID, yScaleFilesUUID, yAxisFilesUUID);

    // Total files
    var yScaleFiles = this.yScales(throughput_all, throughput_files, height_range);
    var yAxisFiles = this.getAxis(yScaleFiles, "left");
    
    this.statsDisplay("#files-by-day-stats", throughput_all, throughput_files);
    
    var file_totals = this.showAxises("#files-by-day", xAxis, yAxisFiles, width, "Throughput (Files)");
    var fileLineScaleTotals = this.lineGenerator(xScale, yScaleFiles, "throughput_files");
    this.appendPath(file_totals, "files-by-day-line", fileLineScaleTotals, throughput_all);
    
  //  this.drawCircles(file_totals, throughput_all, xScale, yScaleFiles, throughput_files);
    focusHover(file_totals, throughput_all, "#files-by-day");
    this.data_store["files-by-day"] = throughput_all;
    this.chartUpdate("files", xScale, yScaleFiles, yAxisFiles);

    this.drawLegend("#files-legend", throughput_all, throughput_files);
    drawStrip("#files-strip", throughput_all, throughput_files);


    /**
     * Deposit Duration
     *
     *
     */
    
    // duration totals by deposit by day
    var total_time = "total_time";
    var yScaleTotal = this.yScales(deposits_by_uuid, total_time, height_range);
    var xAxisDuration = this.getAxis(xScaleUUID, "bottom");
    var yAxisDuration = this.getAxis(yScaleTotal, "left");
    var duration_date = this.showAxises("#duration-date", xAxisDuration, yAxisDuration, width, "Time (Seconds)");

    this.statsDisplay("#duration-date-stats", deposits_by_uuid, total_time);
    this.drawCircles(duration_date, deposits_by_uuid, xScaleUUID, yScaleTotal, total_time);
    this.data_store["duration-date"] = deposits_by_uuid;
    this.chartUpdate("time-uuid", xScaleUUID, yScaleTotal, yAxisDuration);

    var yScaleTotalDay = this.yScales(uuid_all, total_time, height_range);
    var xAxisTotal = this.getAxis(xScaleUUID, "bottom");
    var yAxisTotal = this.getAxis(yScaleTotalDay, "left");
    var all_duration_date = this.showAxises("#duration-total-date", xAxisTotal, yAxisTotal, width, "Time (Seconds)");
    
    this.statsDisplay("#duration-total-date-stats", uuid_all, total_time);
    var durationLineScaleTotals = this.lineGenerator(xScaleUUID, yScaleTotalDay, total_time);
    this.appendPath(all_duration_date, "duration-total-date-line", durationLineScaleTotals, uuid_all);
 //   this.drawCircles(all_duration_date, uuid_all, xScaleUUID, yScaleTotalDay, total_time);
    this.data_store["duration-total-date"] = uuid_all;
    this.chartUpdate("time", xScaleUUID, yScaleTotalDay, yAxisTotal);


    /**
     *  Scatter plot & Strip plot - moves by date
     *
     *
     **/

    var moves = "moves";
    var yScaleMoves = this.yScales(data, moves, height_range);
    var yAxisMoves = this.getAxis(yScaleMoves, "left");
    var moves_date = this.showAxises("#moves-date", xAxis, yAxisMoves, width, "Move Operations");
    
    this.statsDisplay("#moves-date-stats", data, moves);
    var movesLineScaleTotals = this.lineGenerator(xScale, yScaleMoves, moves);
    this.appendPath(moves_date, "moves-date-line", movesLineScaleTotals, data);
 //   this.drawCircles(moves_date,  data, xScale, yScaleMoves, moves);
    focusHover(moves_date, data, "#moves-date");

    this.drawLegend("#moves-legend", data, moves);
    drawStrip("#moves-date-strip", data, moves);

    /**
     * Scatter plot & Strip plot - enhancements by date
     */

    var finished_enh = "finished_enhancements";
    var yScaleFinishedEnh = this.yScales(data, finished_enh, height_range);
    var yAxis_finished_enh = this.getAxis(yScaleFinishedEnh, "left");
    var finished_enh_date = this.showAxises("#enh-date", xAxis, yAxis_finished_enh, width, "Finished Enhancements");
    
    this.statsDisplay("#enh-date-stats", data, finished_enh);
    var endLineScaleTotals = this.lineGenerator(xScale, yScaleFinishedEnh, finished_enh);
    this.appendPath(finished_enh_date, "enh-date-line", endLineScaleTotals, data);
 //   this.drawCircles(finished_enh_date,  data, xScale, yScaleFinishedEnh, finished_enh);
    focusHover(finished_enh_date, data, "#enh-date");

    this.drawLegend("#enh-legend", data, finished_enh);
    drawStrip("#enh-date-strip", data, finished_enh);

    /**
     * Scatter plot & Strip plot - failed enhancements by date
     */

    var failed_enh = "failed_enhancements";
    var yScaleFailedEnh = this.yScales(data, failed_enh, height_range);
    var yAxis_failed_enh = this.getAxis(yScaleFailedEnh, "left");
    var failed_enh_date = this.showAxises("#failed-enh-date", xAxis, yAxis_failed_enh, width, "Failed Enhancements");
    
    this.statsDisplay("#failed-enh-stats", data, failed_enh);
    var failedEnhScaleTotals = this.lineGenerator(xScale, yScaleFailedEnh, failed_enh);
    this.appendPath(failed_enh_date, "failed-enh-date-line", failedEnhScaleTotals, data);
  //  this.drawCircles(failed_enh_date,  data, xScale, yScaleFailedEnh, failed_enh);
    focusHover(failed_enh_date, data, "#failed-enh-date");

    this.drawLegend("#failed-enh-legend", data, failed_enh);
    drawStrip("#failed-enh-date-strip", data, failed_enh);

    // Make graphs visible
    this.hideShow();


    /**
     * Draw strip chart
     * @param selector
     * @param data
     * @param field
     * @returns {*}
     */
    function drawStrip(selector, data, field) {
        var strip_color = _that.stripColors(data, field);
        var tip = d3.tip().attr("class", "d3-tip").html(function(d) {
            return _that.tipTextOperations(d);
        });

        var strip = d3.select(selector)
            .attr("width", width + _that.margins.left + _that.margins.right)
            .attr("height", 110)
            .call(tip);

        var add = strip.selectAll("bar")
            .data(data);

        add.enter().append("rect");

        add.attr("x", function(d) { return xScale(d.date); })
            .attr("width", 7)
            .attr("y", 0)
            .attr("height", 80)
            .translate([_that.margins.left, 0])
            .style("fill", function(d) { return strip_color(d[field]); })
            .on("mouseover", function(d) {
                d3.select(this).attr("height", 100);
                tip.show.call(this, d);
            })
            .on("mouseout", function(d) {
                d3.select(this).attr("height", 80);
                tip.hide.call(this, d);
            });

        add.exit().remove();

        return add;
    }

    /**
     * Add overlay line & text
     * @param chart
     * @param data
     * @param selector
     * @returns {*}
     */
    function focusHover(chart, data, selector) {
        var margins = _that.margins;
        var bisectDate = d3.bisector(d3.f("date")).right;

        var focus = chart.append("g")
            .attr("class", "focus")
            .style("display", "none");

        focus.append("line")
            .attr("class", "y0")
            .attr({
                x1: 0,
                y1: 0,
                x2: 0,
                y2: _that.height

            });

        chart.append("rect")
            .attr("class", "overlay")
            .attr("width", width)
            .attr("height", _that.height)
            .on("mouseover touchstart", function() { focus.style("display", null); })
            .on("mouseout touchend", function() {
                focus.style("display", "none");
                _that.scatter_tip.transition()
                    .duration(250)
                    .style("opacity", 0);
            })
            .on("mousemove touchmove", mousemove)
            .translate([margins.left, margins.top]);

        function mousemove() {
            var x0 = xScale.invert(d3.mouse(this)[0]),
                i = bisectDate(data, x0, 1),
                d0 = data[i - 1],
                d1 = data[i];

            if(d1 === undefined) {
                d1 = Infinity;
            }
            var d = x0 - d0.key > d1.key - x0 ? d1 : d0;

            var transform_values = [(xScale(d.date) + margins.left), margins.top];
            d3.select(selector + " line.y0").translate(transform_values);

            _that.scatter_tip.transition()
                .duration(100)
                .style("opacity", .9);

            _that.scatter_tip.html(_that.tipTextOperations(d))
                .style("top", (d3.event.pageY-28)+"px")
                .style("left", (d3.event.pageX-158)+"px");
        }

        return chart;
    }
};