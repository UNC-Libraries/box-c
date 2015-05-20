define('MoveActionMonitor', [ 'jquery'], function($) {

	var defaultOptions = {
		updateInterval : 5000
	};
	
	function MoveActionMonitor(alertHandler, options) {
		this.options = $.extend({}, defaultOptions, options);
		
		this.alerts = alertHandler;
		this.moveData = {};
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
		$.each(this.moveData, function(moveId, moveDetails) {
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
				var remoteMoves = data == null? {} : data;
				
				try {
					// Clean up inactive move operations that have been removed remotely
					self.cleanMoveTombstones(remoteMoves);
					// Clean up completed move operations
					self.completeMoves(remoteMoves);
				
					// Indicate new move options
					self.addMoves(remoteMoves);
					
				} finally {
					// Queue up the next run
					setTimeout($.proxy(self.update, self), self.options.updateInterval);
				}
			},
			error : function() {
				console.error("Failed to retrieve move information from server");
				// Keep refreshing alive, but wait a bit longer
				setTimeout($.proxy(self.update, self), self.options.updateInterval * 2);
			}
		});
	};
	
	MoveActionMonitor.prototype.cleanMoveTombstones = function(remoteMoves) {

		var localInactive = [];
		for (var moveId in this.moveData) {
			if (this.moveData[moveId].inactive) {
				localInactive.push(moveId);
			}
		}
		
		if (localInactive.length == 0)
			return;

		// Cleanable tombstones = Inactive Local Moves - Remote Moves
		var cleanable = setDifference(localInactive, Object.keys(remoteMoves));
		
		if (cleanable.length == 0)
			return;
		
		for (var index in cleanable) {
			var cleanableId = cleanable[index];
			delete this.moveData[cleanableId];
			this.removeUserMove(cleanableId);
		}
	};
	
	MoveActionMonitor.prototype.completeMoves = function(remoteMoves) {

		// Get the set of move ids which were in progress according to the server
		var remoteActive = [];
		for (var moveId in remoteMoves) {
			if (remoteMoves[moveId]) {
				remoteActive.push(moveId);
			}
		}
		
		// Get the list of move ids considered in progress locally
		var localActive = [];
		for (var moveId in this.moveData) {
			if (!this.moveData[moveId].inactive) {
				localActive.push(moveId);
			}
		}
		
		var localMoves = Object.keys(this.moveData);
		// Completed jobs = Local Moves - Active Remote Moves
		var completed = setDifference(localActive, remoteActive);
		
		if (completed.length == 0)
			return;
		
		var userMoves = this.getUserMoves();
		// Process the set of completed moves to remove them from local memory and inform the user
		for (var index in completed) {
			var completedMove = completed[index];
		
			var moveDetails = this.moveData[completedMove];
			if (moveDetails && moveDetails.pids) {
				// If the user initiated this move, then tell them it has completed
				if (completedMove in userMoves) {
					this.alerts.alertHandler("success", "Moved " + moveDetails.pids.length 
							+ " object" + (moveDetails.pids.length > 1? "s" : "")
							+ " to " + userMoves[completedMove].destinationTitle);
				}
			
				this.cleanupResults(moveDetails.pids)
			}
			
			// If the move is inactive remotely, then store that.  Otherwise, purge the move
			if (completedMove in remoteMoves) {
				this.moveData[completedMove].inactive = true;
			} else {
				delete this.moveData[completedMove];
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
				resultEntry.deleteElement();
			}
		}
	};
	
	// Store details newly started moves and mark individual results as being moved
	MoveActionMonitor.prototype.addMoves = function(remoteMoves) {
		var self = this;
		
		var localMoves = Object.keys(this.moveData);
		var newMoves = setDifference(Object.keys(remoteMoves), localMoves);
		
		if (!newMoves || newMoves.length == 0)
			return;
		
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
					
					// Add in new move operations
					self.setMovePids(moveId, data);
				
					// Mark the items being moved
					self.markMoving(data);
					
					// The operation was inactive at first retrieval, cleanup and discard it
					if (!remoteMoves[moveId]) {
						self.cleanupResults(data);
						self.moveData[moveId].inactive = true;
					}
				}
			});
		}
	};
	
	MoveActionMonitor.prototype.addMove = function(moveId, pids, destinationTitle) {
		if (!moveId || !pids) {
			return;
		}
		
		try {
			this.setMovePids(moveId, pids);
			
			var userMoves = this.getUserMoves();
			userMoves[moveId] = destinationTitle;
		
			this.markMoving(pids);
		} finally {
			this.setUserMoves(userMoves);
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
	
	MoveActionMonitor.prototype.setMovePids = function(moveId, pids) {
		if (!(moveId in this.moveData)) {
			this.moveData[moveId] = {};
		}
		this.moveData[moveId].pids = pids;
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