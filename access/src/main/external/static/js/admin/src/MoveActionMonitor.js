define('MoveActionMonitor', ['jquery', 'moment'], function($) {

	var defaultOptions = {
		updateInterval : 5000
	};
	
	function MoveActionMonitor(alertHandler, options) {
		this.options = $.extend({}, defaultOptions, options);
		
		this.alerts = alertHandler;
		// Object containing 
		this.activeMoves = [];
		this.completedMoves = [];
		this.moveObjects = {};
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
		
		// Start refresh loop
		this.update();
	};
	
	MoveActionMonitor.prototype.update = function() {
		var self = this;
		
		$.ajax({
			url : "/services/api/listMoves",
			contentType: "application/json; charset=utf-8",
			dataType: "json"
		}).done(function(data) {
			try {
				// Clean up inactive move operations that have been removed remotely
				self.cleanMoveTombstones(data);
				// Clean up completed move operations
				self.completeMoves(data);
			
				// Indicate new move operations
				self.addMoves(data);
				
			} finally {
				self.activeMoves = data.active;
				self.completedMoves = data.complete;
				// Queue up the next run
				setTimeout($.proxy(self.update, self), self.options.updateInterval);
			}
		}).fail(function() {
			console.error("Failed to retrieve move information from server");
			// Keep refreshing alive, but wait a bit longer
			setTimeout($.proxy(self.update, self), self.options.updateInterval * 2);
		});
	};
	
	// Cleans up leftover data from completed move operations which are no longer being tracked remotely
	MoveActionMonitor.prototype.cleanMoveTombstones = function(remoteMoves) {
		
		if (this.completedMoves.length == 0)
			return;

		// Cleanable tombstones = Completed Local Moves - All Remote Moves
		var cleanable = setDifference(this.completedMoves, remoteMoves.active.concat(remoteMoves.complete));
		if (cleanable.length == 0)
			return;
		
		for (var index in cleanable) {
			var cleanableId = cleanable[index];
			delete this.moveObjects[cleanableId];
			this.removeUserMove(cleanableId);
		}
	};
	
	MoveActionMonitor.prototype.completeMoves = function(remoteMoves) {

		// Completed jobs = Local Moves - Active Remote Moves
		var completed = setDifference(this.activeMoves, remoteMoves.active);
		
		if (completed.length == 0)
			return;
		
		var userMoves = this.getUserMoves();
		// Process the set of completed moves to remove them from local memory and inform the user
		for (var index in completed) {
			var completedMove = completed[index];
		
			var moveDetails = this.moveObjects[completedMove];
			if (moveDetails) {
				// If the user initiated this move, then tell them it has completed
				if (completedMove in userMoves) {
					this.alerts.alertHandler("success", "Moved " + moveDetails.length 
							+ " object" + (moveDetails.length > 1? "s" : "")
							+ " to " + userMoves[completedMove]);
				}
			
				this.cleanupResults(moveDetails);
			}
			
			// If the move was no longer reported on remotely, then purge it locally
			if (remoteMoves.complete.indexOf(completedMove) == -1) {
				delete this.moveObjects[completedMove];
				this.removeUserMove(completedMoved);
			}
		}
	};
	
	// Removes results with ids in the given list of pids
	MoveActionMonitor.prototype.cleanupResults = function(completedPids) {
		for (var pindex in completedPids) {
			var pid = completedPids[pindex];
			var resultEntry = this.resultList.getResultObject(pid);
			if (resultEntry != null) {
				this.resultList.removeResultObject(pid);
				resultEntry.deleteElement();
			}
		}
	};
	
	// Store details newly started moves and mark individual results as moving
	MoveActionMonitor.prototype.addMoves = function(remoteMoves) {
		var self = this;
		
		// New moves are the set of all Remote Moves minus the set of all local moves
		var newMoves = setDifference(remoteMoves.active.concat(remoteMoves.complete),
				this.activeMoves.concat(this.completedMoves));
		
		if (!newMoves || newMoves.length == 0)
			return;
		
		$.ajax({
			url : "/services/api/listMoves/details",
			type : "POST",
			contentType: "application/json; charset=utf-8",
			dataType: "json",
			data : JSON.stringify(newMoves)
		}).done(function(moveMap) {
			if (!moveMap) {
				return;
			}
			
			for (var moveId in moveMap) {
				var details = moveMap[moveId];
				var movedObjects = details.moved;
				
				if (remoteMoves.complete.indexOf(moveId) == -1) {
					// New move in progress, mark relevant results and store the list of objects
					self.moveObjects[moveId] = movedObjects;
			
					// Mark the items being moved
					self.markMoving(movedObjects);
				} else {
					// Operation was complete at first retrieval, cleanup relevant results
					var repRecord = self.resultList.getResultObject(movedObjects[0]);
					if (repRecord && repRecord.metadata.timestamp < details.finishedAt) {
						self.cleanupResults(movedObjects);
					}
				}
			}
		});
	};
	
	MoveActionMonitor.prototype.addMove = function(moveId, pids, destinationTitle) {
		if (!moveId || !pids) {
			return;
		}
		
		try {
			this.moveObjects[moveId] = pids;
			
			var userMoves = this.getUserMoves();
			userMoves[moveId] = destinationTitle;
		
			this.markMoving(pids);
		} finally {
			this.setUserMoves(userMoves);
		}
	};
	
	MoveActionMonitor.prototype.refreshMarked = function() {
		for (var moveId in this.moveObjects) {
			if (this.activeMoves.indexOf(moveId) != -1) {
				this.markMoving(this.moveObjects[moveId]);
			}
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
	
	MoveActionMonitor.prototype.getUserMoves = function() {
		var userMoves = localStorage.getItem("move_actions");
		return userMoves == null? {} : JSON.parse(userMoves);
	};
	
	MoveActionMonitor.prototype.setUserMoves = function(userMoves) {
		localStorage.setItem("move_actions", JSON.stringify(userMoves));
	};
	
	MoveActionMonitor.prototype.removeUserMove = function(id) {
		var moves = this.getUserMoves();
		delete moves[id];
		this.setUserMoves(moves);
	};
	
	return MoveActionMonitor;
});