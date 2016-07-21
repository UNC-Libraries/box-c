d3_queue.queue()
    .defer(d3.csv,"/admin/sendOperationsData")
    .defer(d3.csv,"/admin/sendDepositsData")
    .await(function(error, operation_totals, deposit_totals) {
    var scatter_tip = d3.select("body").append("div")
        .attr("class", "tooltip")
        .style("opacity", 0);

    var drawGraphs = new CdrGraphs(operation_totals, deposit_totals, scatter_tip);
    drawGraphs.draw();
}); 