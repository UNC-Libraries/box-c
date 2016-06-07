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
        d.throughput_bytes = _that.coerceToNum(d.throughput_bytes) / 1000000;
        d.throughput_files = _that.coerceToNum(d.throughput_files);
        d.finished = _that.coerceToNum(d.finished);
        d.moves = _that.coerceToNum(d.moves);
        d.image_enh = _that.coerceToNum(d.image_enh);
        d.failed_image_enh = _that.coerceToNum(d.failed_image_enh);
        d.metadata_enh = _that.coerceToNum(d.metadata_enh);
        d.failed_metadata_enh = _that.coerceToNum(d.failed_metadata_enh);
        d.solr_enh = _that.coerceToNum(d.solr_enh);
        d.failed_solr_enh = _that.coerceToNum(d.failed_solr_enh);
        d.fulltext_enh = _that.coerceToNum(d.fulltext_enh);
        d.failed_fulltext_enh = _that.coerceToNum(d.failed_fulltext_enh);
        d.thumbnail_enh = _that.coerceToNum(d.thumbnail_enh);
        d.failed_thumbnail_enh = _that.coerceToNum(d.failed_thumbnail_enh);
        d.finished_all_enh = d.image_enh + d.metadata_enh + d.solr_enh + d.fulltext_enh + d.thumbnail_enh;
        d.failed_all_enh = d.failed_image_enh + d.failed_metadata_enh + d.failed_solr_enh + d.failed_fulltext_enh + d.failed_thumbnail_enh;
        d.failed_deposit = _that.coerceToNum(d.failed_deposit);
        d.failed_deposit_job = _that.coerceToNum(d.failed_deposit_job);
        d.avg_filesize = _that.fileAvg(d);
    });

    var data = this.dateSort(this.operations);
    
    // Metrics by uuid & day, newer deposit date metrics
    this.deposits.forEach(function(d) {
        d.date = (typeof d.date === "object") ? d.date : parseDate(d.date);
        d.ingest_duration = d.ingest_duration / 1000;
        d.queued_duration = d.queued_duration / 1000;
        d.total_time = d.ingest_duration + d.queued_duration;
        d.throughput_bytes = _that.coerceToNum(d.throughput_bytes) / 1000000;
        d.throughput_files = _that.coerceToNum(d.throughput_files);
        d.avg_filesize = _that.fileAvg(d);
    });

    var deposits_filtered = this.dataFilter(this.deposits, "throughput_bytes");
    var deposits_by_uuid = this.dateSort(deposits_filtered);

    var height_range = [0, this.height];
    var brush_height = [0, this.brush_height];

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

    // Recalculate average file sizes after everything is combined
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
    
    // Throughput by UUID
    var yScaleDeposits = this.yScales(deposits_by_uuid, throughput, height_range);

    var xAxisDeposits = this.getAxis(xScaleUUID, "bottom");
    var yAxisDeposits  = this.getAxis(yScaleDeposits, "left");
    
    this.statsDisplay("#throughput-uuid-stats", deposits_by_uuid, throughput);
    var throughput_uuid = this.showAxises("#throughput-deposit", xAxisDeposits , yAxisDeposits , width, "Throughput (MB)");
    this.drawCircles(throughput_uuid, deposits_by_uuid, xScaleUUID, yScaleDeposits, throughput);
    
    // Throughput by Date
    var xScaleBrush = this.xScales(data, width);
    var yScale = this.yScales(throughput_all, throughput, height_range);
    var yScaleBrush = this.yScales(throughput_all, throughput, brush_height);

    var xAxis = this.getAxis(xScale, "bottom");
    var yAxis = this.getAxis(yScale, "left");
    
    this.statsDisplay("#throughput-stats", throughput_all, throughput);
    var throughput_date = this.showAxises("#throughput-date", xAxis, yAxis, width, "Throughput (MB)");
    
    var throughputLineScaleTotals = this.lineGenerator(xScale, yScale, throughput);
    this.appendPath(throughput_date, "throughput-date-line", throughputLineScaleTotals, throughput_all);
    focusHover(throughput_date, throughput_all, "#throughput-date");

    // Add Brush to Throughput by Date
    var yAxisBrush = this.getAxis(yScaleBrush, "left");
    var throughputLineBrush = this.lineGenerator(xScaleBrush, yScaleBrush, throughput);
    var throughput_date_brush = this.showAxises("#throughput-date-brush", xAxis, yAxisBrush, width, "");
    this.appendPath(throughput_date_brush, "throughput-date-brush-line", throughputLineBrush, throughput_all);

    var throughput_params = {
        brushXScale: xScaleBrush,
        xScale: xScale,
        yScale: yScale,
        xAxis: xAxis,
        yAxis: yAxis,
        data: throughput_all,
        field: "throughput_bytes",
        chart_id: "throughput-date"
    };

    var throughputBrush = new CreateBrush(this);
    throughputBrush.selectionBrushing(throughput_date_brush, throughput_params);

    /**
     * File Counts by Deposit & Date
     *
     *
     */

    var throughput_files = "throughput_files";

    // Files by Deposit
    var yScaleFilesUUID = this.yScales(deposits_by_uuid, throughput_files, height_range);

    var xAxisFilesUUID = this.getAxis(xScaleUUID, "bottom");
    var yAxisFilesUUID = this.getAxis(yScaleFilesUUID, "left");

    this.statsDisplay("#files-by-ingest-stats", deposits_by_uuid, throughput_files);
    var throughput_files_uuid = this.showAxises("#files-by-ingest", xAxisFilesUUID, yAxisFilesUUID, width, "Throughput (Files)");
    this.drawCircles(throughput_files_uuid, deposits_by_uuid, xScaleUUID, yScaleFilesUUID, throughput_files);
    this.data_store["files-by-ingest"] = deposits_by_uuid;
    this.chartUpdate("files-uuid", {xScale: xScaleUUID, yScale: yScaleFilesUUID, yAxis: yAxisFilesUUID}, false);

    // Total Files by Day
    var yScaleFiles = this.yScales(throughput_all, throughput_files, height_range);
    var yAxisFiles = this.getAxis(yScaleFiles, "left");
    
    this.statsDisplay("#files-by-day-stats", throughput_all, throughput_files);
    
    var file_totals = this.showAxises("#files-by-day", xAxis, yAxisFiles, width, "Throughput (Files)");
    var fileLineScaleTotals = this.lineGenerator(xScale, yScaleFiles, "throughput_files");
    this.appendPath(file_totals, "files-by-day-line", fileLineScaleTotals, throughput_all);

    focusHover(file_totals, throughput_all, "#files-by-day");
    this.data_store["files-by-day"] = throughput_all;


    // Add Brush to Total Files by Day
    var yScaleFilesBrush = this.yScales(throughput_all, throughput_files, brush_height);
    var yAxisFileBrush = this.getAxis(yScaleFilesBrush, "left");
    var throughputFileBrush = this.lineGenerator(xScaleBrush, yScaleFilesBrush, throughput_files);
    var throughput_file_brush = this.showAxises("#files-by-day-brush", xAxis, yAxisFileBrush, width, "");
    this.appendPath(throughput_file_brush, "files-by-day-brush-line", throughputFileBrush, throughput_all);

    var files_params = {
        brushXScale: xScaleBrush,
        brushYScale: yScaleBrush,
        brushAxis: throughput_file_brush,
        xScale: xScale,
        yScale: yScaleFiles,
        xAxis: xAxis,
        yAxis: yAxisFiles,
        data: throughput_all,
        field: throughput_files,
        chart_id: "files-by-day"
    };

    var fileBrush = new CreateBrush(this);
    var stuff = fileBrush.selectionBrushing(throughput_file_brush, files_params);

    this.chartUpdate("files", files_params, stuff);

    /**
     * Deposit Duration
     *
     *
     */
    
    // Duration Totals by UUID by Day
    var total_time = "total_time";
    var yScaleTotal = this.yScales(deposits_by_uuid, total_time, height_range);
    var xAxisDuration = this.getAxis(xScaleUUID, "bottom");
    var yAxisDuration = this.getAxis(yScaleTotal, "left");
    var duration_date = this.showAxises("#duration-date", xAxisDuration, yAxisDuration, width, "Time (Seconds)");

    this.statsDisplay("#duration-date-stats", deposits_by_uuid, total_time);
    this.drawCircles(duration_date, deposits_by_uuid, xScaleUUID, yScaleTotal, total_time);
    this.data_store["duration-date"] = deposits_by_uuid;
    this.chartUpdate("time-uuid", {xScale: xScaleUUID, yScale: yScaleTotal, yAxis: yAxisDuration}, false);

    // Duration Totals by Day
    var yScaleTotalDay = this.yScales(uuid_all, total_time, height_range);
    var xAxisTotal = this.getAxis(xScaleUUID, "bottom");
    var yAxisTotal = this.getAxis(yScaleTotalDay, "left");
    var all_duration_date = this.showAxises("#duration-total-date", xAxisTotal, yAxisTotal, width, "Time (Seconds)");
    
    this.statsDisplay("#duration-total-date-stats", uuid_all, total_time);
    var durationLineScaleTotals = this.lineGenerator(xScaleUUID, yScaleTotalDay, total_time);
    this.appendPath(all_duration_date, "duration-total-date-line", durationLineScaleTotals, uuid_all);
    focusHover(all_duration_date, uuid_all, "#duration-total-date"); 
    this.data_store["duration-total-date"] = uuid_all;

    // Add brush to Duration Totals by Day
    var xScaleUUIDBrush = this.xScales(uuid_all, width);
    var yScaleTotalDurationBrush = this.yScales(uuid_all, total_time, brush_height);
    var yAxisTotalDurationBrush = this.getAxis(yScaleTotalDurationBrush, "left");
    var totalDurationBrush = this.lineGenerator(xScaleUUIDBrush, yScaleTotalDurationBrush, total_time);
    var total_duration_date_brush = this.showAxises("#duration-total-date-brush", xAxisTotal, yAxisTotalDurationBrush, width, "");
    this.appendPath(total_duration_date_brush, "duration-total-date-brush-line", totalDurationBrush, uuid_all);

    var total_duration_params = {
        brushXScale: xScaleUUIDBrush,
        brushYScale: yScaleTotalDurationBrush,
        brushAxis: total_duration_date_brush,
        xScale: xScaleUUID,
        yScale: yScaleTotalDay,
        xAxis: xAxisTotal,
        yAxis: yAxisTotal,
        data: uuid_all,
        field: total_time,
        chart_id: "duration-total-date"
    };

    var durationBrush = new CreateBrush(this);
    durationBrush.selectionBrushing(total_duration_date_brush, total_duration_params);

    this.chartUpdate("time", total_duration_params, durationBrush);

    /**
     *  Total Deposits by Date
     *
     **/

    var total_deposits = "finished";
    var yScaleTotalDeposits = this.yScales(data, total_deposits, height_range);
    var yAxisTotalDeposits = this.getAxis(yScaleTotalDeposits, "left");
    var total_deposits_date = this.showAxises("#total-deposits-date", xAxis, yAxisTotalDeposits, width, "Total Deposits");

    this.statsDisplay("#total-deposits-stats", data, total_deposits);
    var totalDepositsLineScaleTotals = this.lineGenerator(xScale, yScaleTotalDeposits, total_deposits);
    this.appendPath(total_deposits_date, "total-deposits-date-line", totalDepositsLineScaleTotals, data);
    focusHover(total_deposits_date, data, "#total-deposits-date");

    this.data_store["total-deposits-date"] = data;

    // Add Brush to Total Deposits by Date
    var yScaleTotalDepositsBrush = this.yScales(data, total_deposits, brush_height);
    var yAxisTotalDepositsBrush = this.getAxis(yScaleTotalDepositsBrush, "left");
    var totalDepositsBrush = this.lineGenerator(xScaleBrush, yScaleTotalDepositsBrush, total_deposits);
    var total_deposits_date_brush = this.showAxises("#total-deposits-date-brush", xAxis, yAxisTotalDepositsBrush, width, "");
    this.appendPath(total_deposits_date_brush, "total-deposits-date-brush-line", totalDepositsBrush, data);

    var total_deposits_params = {
        brushXScale: xScaleBrush,
        brushYScale: yScaleTotalDepositsBrush,
        brushAxis: total_deposits_date_brush,
        xScale: xScale,
        yScale: yScaleTotalDeposits,
        xAxis: xAxis,
        yAxis: yAxisTotalDeposits,
        data: data,
        field: total_deposits,
        chart_id: "total-deposits-date"
    };

    var depositsBrush = new CreateBrush(this);
    depositsBrush.selectionBrushing(total_deposits_date_brush, total_deposits_params);

    this.chartUpdate("total-deposits", total_deposits_params, depositsBrush);

    /**
     *  Moves by Date
     *
     **/

    var moves = "moves";
    var yScaleMoves = this.yScales(data, moves, height_range);
    var yAxisMoves = this.getAxis(yScaleMoves, "left");
    var moves_date = this.showAxises("#moves-date", xAxis, yAxisMoves, width, "Move Operations");

    this.statsDisplay("#moves-date-stats", data, moves);
    var movesLineScaleTotals = this.lineGenerator(xScale, yScaleMoves, moves);
    this.appendPath(moves_date, "moves-date-line", movesLineScaleTotals, data);
    focusHover(moves_date, data, "#moves-date");

    // Add Brush to Moves by Date
    var yScaleMovesBrush = this.yScales(data, moves, brush_height);
    var yAxisMovesBrush = this.getAxis(yScaleMovesBrush, "left");
    var throughputMovesBrush = this.lineGenerator(xScaleBrush, yScaleMovesBrush, moves);
    var throughput_moves_brush = this.showAxises("#moves-date-brush", xAxis, yAxisMovesBrush, width, "");
    this.appendPath(throughput_moves_brush, "moves-date-brush-line", throughputMovesBrush, data);

    var moves_params = {
        brushXScale: xScaleBrush,
        xScale: xScale,
        yScale: yScaleMoves,
        xAxis: xAxis,
        yAxis: yAxisMoves,
        data: data,
        field: moves,
        chart_id: "moves-date"
    };

    var movesBrush = new CreateBrush(this);
    movesBrush.selectionBrushing(throughput_moves_brush, moves_params);

    /**
     * Enhancements by Date
     */

    var finished_enh = "finished_all_enh";
    var yScaleFinishedEnh = this.yScales(data, finished_enh, height_range);
    var yAxis_finished_enh = this.getAxis(yScaleFinishedEnh, "left");
    var finished_enh_date = this.showAxises("#enh-date", xAxis, yAxis_finished_enh, width, "Finished Enhancements");
    
    this.statsDisplay("#enh-date-stats", data, finished_enh);
    var endLineScaleTotals = this.lineGenerator(xScale, yScaleFinishedEnh, finished_enh);
    this.appendPath(finished_enh_date, "enh-date-line", endLineScaleTotals, data);
    focusHover(finished_enh_date, data, "#enh-date");
    this.data_store["enh-date"] = throughput_all;

    // Add Brush to Enhancements by Date
    var yScaleFinishedEnhBrush = this.yScales(data, finished_enh, brush_height);
    var yAxisFinishedEnhBrush = this.getAxis(yScaleFinishedEnhBrush, "left");
    var throughputFinishedEnhBrush = this.lineGenerator(xScaleBrush, yScaleFinishedEnhBrush, finished_enh);
    var throughput_finished_enh_brush = this.showAxises("#enh-date-brush", xAxis, yAxisFinishedEnhBrush, width, "");
    this.appendPath(throughput_finished_enh_brush, "enh-date-brush-line", throughputFinishedEnhBrush, data);

    var finished_params = {
        brushXScale: xScaleBrush,
        brushYScale: yScaleFinishedEnhBrush,
        brushAxis: throughput_finished_enh_brush,
        xScale: xScale,
        yScale: yScaleFinishedEnh,
        xAxis: xAxis,
        yAxis: yAxis_finished_enh,
        data: data,
        field: finished_enh,
        chart_id: "enh-date"
    };

    var finishedBrush = new CreateBrush(this);
    finishedBrush.selectionBrushing(throughput_finished_enh_brush, finished_params);

    this.chartUpdate("enh", finished_params, finishedBrush);

    /**
     * Failed Enhancements by Date
     */

    var failed_enh = "failed_all_enh";
    var yScaleFailedEnh = this.yScales(data, failed_enh, height_range);
    var yAxis_failed_enh = this.getAxis(yScaleFailedEnh, "left");
    var failed_enh_date = this.showAxises("#failed-enh-date", xAxis, yAxis_failed_enh, width, "Failed Enhancements");
    
    this.statsDisplay("#failed-enh-stats", data, failed_enh);
    var failedEnhScaleTotals = this.lineGenerator(xScale, yScaleFailedEnh, failed_enh);
    this.appendPath(failed_enh_date, "failed-enh-date-line", failedEnhScaleTotals, data);
    focusHover(failed_enh_date, data, "#failed-enh-date");
    this.data_store["failed-enh-date"] = throughput_all;

    // Add Brush to Failed Enhancements by Date
    var yScaleFailedEnhBrush = this.yScales(data, failed_enh, brush_height);
    var yAxisFailedEnhBrush = this.getAxis(yScaleFailedEnhBrush, "left");
    var throughputFailedEnhBrush = this.lineGenerator(xScaleBrush, yScaleFailedEnhBrush, failed_enh);
    var throughput_failed_enh_brush = this.showAxises("#failed-enh-date-brush", xAxis, yAxisFailedEnhBrush, width, "");
    this.appendPath(throughput_failed_enh_brush, "failed-enh-date-brush-line", throughputFailedEnhBrush, data);

    var failed_params = {
        brushXScale: xScaleBrush,
        brushYScale: yScaleFailedEnhBrush,
        brushAxis: throughput_failed_enh_brush,
        xScale: xScale,
        yScale: yScaleFailedEnh,
        xAxis: xAxis,
        yAxis: yAxis_failed_enh,
        data: data,
        field: failed_enh,
        chart_id: "failed-enh-date"
    };

    var failedBrush = new CreateBrush(this);
    failedBrush.selectionBrushing(throughput_failed_enh_brush, failed_params);

    this.chartUpdate("failed-enh", failed_params, failedBrush);

    // Make graphs visible
    this.hideShow();

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
            var whichScale, whichTip;

            // Duration has only been tracked since deposit by UUID started
            if (/duration/.test(selector)) {
                whichScale = xScaleUUID;
                whichTip = 'deposits';
            } else {
                whichScale = xScale;
                whichTip = 'operations';
            }

            var x0 = whichScale.invert(d3.mouse(this)[0]),
                i = bisectDate(data, x0, 1),
                d0 = data[i - 1],
                d1 = data[i];

            if (d1 === undefined) {
                d1 = Infinity;
            }
            var d = x0 - d0.key > d1.key - x0 ? d1 : d0;

            var transform_values = [(whichScale(d.date) + margins.left), margins.top];
            d3.select(selector + " line.y0").translate(transform_values);

            _that.scatter_tip.transition()
                .duration(100)
                .style("opacity", .9);

            _that.scatter_tip.html(_that.tipType(whichTip, d))
                .style("top", (d3.event.pageY-28)+"px")
                .style("left", (d3.event.pageX-175)+"px");
        }

        return chart;
    }
};