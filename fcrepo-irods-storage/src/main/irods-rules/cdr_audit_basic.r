cdr_audit{

	msiMakeGenQuery( "COLL_NAME, DATA_NAME, RESC_LOC", *cond, *in );
        msiExecGenQuery( *in, *out );
	
	assign( *contCnt, 1 );
	assign( *cntr, 0);
	
	while( *contCnt > 0 ){

		msiGetMoreRows( *in, *out, *contCnt );

		foreach( *out ){

			msiGetValByKey( *out, DATA_NAME, *dataName );
			msiGetValByKey( *out, COLL_NAME, *collName );
			msiGetValByKey( *out, RESC_LOC, *rescLoc);

			remote( *rescLoc , "null"){
				msiDataObjChksum( *collName/*dataName,verifyChksum, *chkSum );
			}		
			assign( *cntr, "*cntr + 1" ); 
		}
	}
	
	writeLine( stdout, "All OK. Count: *cntr" );
}
INPUT *cond="RESC_CLASS_NAME = 'cache' AND COLL_NAME like '/cdrTestZone/home/fedora%%'"             
OUTPUT ruleExecOut

