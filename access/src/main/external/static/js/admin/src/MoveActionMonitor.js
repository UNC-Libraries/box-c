define('MoveActionMonitor', [ 'jquery'], function($) {

	var defaultOptions = {
		updateInterval : 5000
	};
	
	function MoveActionMonitor(alertHandler, options) {
		this.options = $.extend({}, defaultOptions, options);
		
		this.alerts = alertHandler;
		this.processedCompletes = [];
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
				
				var remoteMoves = data == null? {} : data;
				
				try {
					
					self.cleanMoveTombstones(moveData, remoteMoves);
					// Clean up completed move operations
					self.completeMoves(moveData, remoteMoves);
				
					// Indicate new move options
					self.addMoves(remoteMoves);
					
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
	
	MoveActionMonitor.prototype.cleanMoveTombstones = function(moveData, remoteMoves) {
		var localInactive = [];
		for (var moveId in moveData) {
			if (moveData[moveId].inactive) {
				localInactive.push(moveId);
			}
		}
		
		if (localInactive.length == 0)
			return;
		
		// Catch moves that completed in another browser window
		var unprocessedComplete = setDifference(localInactive, this.processedCompletes);
		for (var index in unprocessedComplete) {
			this.cleanupResults(moveData[unprocessedComplete[index]].pids);
		}
		
		// Cleanable tombstones = Inactive Local Moves - Remote Moves
		var cleanable = setDifference(localInactive, Object.keys(remoteMoves));
		
		if (cleanable.length == 0)
			return;
		
		try {
			for (var index in cleanable) {
				var cleanableId = cleanable[index];
				delete moveData[cleanableId];
				delete this.processedCompletes[cleanableId];
			}
		} finally {
			this.setMoveData(moveData);
		}
	};
	
	MoveActionMonitor.prototype.completeMoves = function(moveData, remoteMoves) {

		// Get the set of move ids which were in progress according to the server
		var remoteActive = [];
		for (var moveId in remoteMoves) {
			if (remoteMoves[moveId]) {
				remoteActive.push(moveId);
			}
		}
		
		// Get the list of move ids considered in progress locally
		var localActive = [];
		for (var moveId in moveData) {
			if (!moveData[moveId].inactive) {
				localActive.push(moveId);
			}
		}
		
		var localMoves = Object.keys(moveData);
		// Completed jobs = Local Moves - Active Remote Moves
		var completed = setDifference(localActive, remoteActive);
		
		if (completed.length == 0)
			return;
		
		try {
			// Process the set of completed moves to remove them from local memory and inform the user
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
				
					this.cleanupResults(moveDetails.pids)
				}
				
				// If the move is inactive remotely, then store that.  Otherwise, purge the move
				if (completedMove in remoteMoves) {
					moveData[completedMove].inactive = true;
					this.processedCompletes.push(completedMove);
				} else {
					delete moveData[completedMove];
				}
				
			}
		} finally {
			this.setMoveData(moveData);
		}
	};
	
	// Removes results with ids in the given list of pids
	MoveActionMonitor.prototype.cleanupResults = function(completedPids) {
		for (var pindex in completedPids) {
			var pid = completedPids[pindex];
			var resultEntry = this.resultList.getResultObject(pid);
			if (resultEntry != null) {
				resultEntry.deleteElement();
			}
		}
	};
	
	// Store details newly started moves and mark individual results as being moved
	MoveActionMonitor.prototype.addMoves = function(remoteMoves) {
		var self = this;
		var moveData = this.getMoveData();
		
		var localMoves = Object.keys(moveData);
		var newMoves = setDifference(Object.keys(remoteMoves), localMoves);
		
		if (!newMoves || newMoves.length == 0)
			return;
		
		for (var index in newMoves) {
			var moveId = newMoves[index];
			var remoteActive = remoteMoves[moveId];
			
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
						// Add in new move operations
						self.setMovePids(moveData, moveId, data);
					
						// Mark the items being moved
						self.markMoving(data);
						
						// The operation was inactive at first retrieval, cleanup and discard it
						if (!remoteActive) {
							self.cleanupResults(data);
							moveData[moveId].inactive = true;
						}
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