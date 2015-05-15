define('MoveActionMonitor', [ 'jquery'], function($) {

	var defaultOptions = {
		updateInterval : 1000
	};
	
	function MoveActionMonitor(alertHandler, options) {
		this.options = $.extend({}, defaultOptions, options);
		
		this.alerts = alertHandler;
	}
	
	function setDifference(A, B) {
		var map = {}, C = [];

		for (var i = B.length; i--; )
			map[B[i]] = null;

		for (var i = A.length; i--; ) {
			if (!map.hasOwnProperty(A[i]))
				C.push(A[i]);
		}

		return C;
	}
	
	MoveActionMonitor.prototype.setResultList = function(resultList) {
		this.resultList = resultList;
	};
	
	MoveActionMonitor.prototype.activate = function() {
		this.active = true;
		
		// Mark objects that were previously known to be moving
		var self = this;
		var moveData = this.getMoveData();
		$.each(moveData, function(moveId, moveDetails) {
			self.markMoving(moveDetails.pids);
		});
		
		// Start refresh loop
		this.update();
	};
	
	MoveActionMonitor.prototype.update = function() {
		var self = this;
		
		$.ajax({
			url : "/services/api/listMoves",
			type : "GET",
			contentType: "application/json; charset=utf-8",
			dataType: "json",
			success : function(data) {
				var moveData = self.getMoveData();
				var localMoves = Object.keys(moveData);
				
				var remoteMoves = data == null? [] : data;
				
				try {
					// Clean up completed move operations
					var completedMoves = setDifference(localMoves, remoteMoves);
					self.completeMoves(moveData, completedMoves);
				
					// Indicate new move options
					var newMoves = setDifference(remoteMoves, localMoves);
					self.addMoves(newMoves);
					
				} finally {
					// Queue up the next run
					setTimeout($.proxy(self.update, self), self.options.updateInterval);
				}
			},
			error : function() {
				console.error("Failed to retrieve move information from server");
			}
		});
	};
	
	MoveActionMonitor.prototype.completeMoves = function(moveData, completed) {
		if (completed.length == 0)
			return;
		
		try {
			for (var index in completed) {
				var completedMove = completed[index];
			
				var moveDetails = moveData[completedMove];
				if (moveDetails && moveDetails.pids) {
					// If the user initiated this move, then tell them it has completed
					if (moveDetails.byUser && moveDetails.destinationTitle) {
						this.alerts.alertHandler("success", "Moved " + moveDetails.pids.length 
								+ " object" + (moveDetails.pids.length > 1? "s" : "")
								+ " to " + moveDetails.destinationTitle);
					}
				
					var pids = moveDetails.pids;
					for (var pindex in pids) {
						var pid = pids[pindex];
						var resultEntry = this.resultList.getResultObject(pid);
						if (resultEntry != null) {
							resultEntry.deleteElement();
						}
					}
				}
			
				delete moveData[completedMove];
			}
		} finally {
			this.setMoveData(moveData);
		}
	};
	
	MoveActionMonitor.prototype.addMoves = function(newMoves) {
		var self = this;
		
		for (var index in newMoves) {
			var moveId = newMoves[index];
			
			$.ajax({
				url : "/services/api/listMoves/" + moveId + "/objects",
				type : "GET",
				contentType: "application/json; charset=utf-8",
				dataType: "json",
				success : function(data) {
					if (!data) {
						return;
					}
					
					var moveData = self.getMoveData();
					try {
						self.setMovePids(moveData, moveId, data);
					
						self.markMoving(data);
					} finally {
						self.setMoveData(moveData);
					}
				}
			});
		}
	};
	
	MoveActionMonitor.prototype.addMove = function(moveId, pids, destinationTitle) {
		if (!moveId || !pids) {
			return;
		}
		
		var moveData = this.getMoveData();
		try {
			this.setMovePids(moveData, moveId, pids);
			moveData[moveId].byUser = true;
			moveData[moveId].destinationTitle = destinationTitle;
		
			this.markMoving(pids);
		} finally {
			this.setMoveData(moveData);
		}
	};
	
	MoveActionMonitor.prototype.markMoving = function(pids) {
		for (var pindex in pids) {
			var pid = pids[pindex];
			var resultEntry = this.resultList.getResultObject(pid);
			if (resultEntry){
				resultEntry.setState("moving");
			}
		}
	};
	
	MoveActionMonitor.prototype.setMovePids = function(moveData, moveId, pids) {
		if (!(moveId in moveData)) {
			moveData[moveId] = {};
		}
		moveData[moveId].pids = pids;
	};
	
	MoveActionMonitor.prototype.getMoveData = function() {
		var ongoing = localStorage.getItem("ongoing_move_actions");
		return ongoing == null? {} : JSON.parse(ongoing);
	};
	
	MoveActionMonitor.prototype.setMoveData = function(moveData) {
		localStorage.setItem("ongoing_move_actions", JSON.stringify(moveData));
	};
	
	return MoveActionMonitor;
});