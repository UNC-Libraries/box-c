cdr_audit_test{

	msiGetSystemTime( *sysTime,human );
        writeLine( stdout, "" );
        writeLine( stdout, "Comparing ICAT's MD5s with OS MD5s where *Cond:" );
        writeLine( stdout, "" );

        msiMakeGenQuery( "DATA_PATH, DATA_NAME, RESC_LOC, DATA_REPL_NUM, DATA_RESC_NAME, RESC_CLASS_NAME, DATA_CHECKSUM", *Cond, *GQIn );
        msiExecGenQuery( *GQIn, *GQOut );

        assign( *ContCnt, 1 );
        assign( *ObjCnt, 0 );
        assign( *BadCnt, 0 );

        while( *ContCnt > 0 ){

                msiGetMoreRows( *GQIn, *GQOut, *ContCnt );

                foreach( *GQOut ){

                        msiGetValByKey( *GQOut, DATA_PATH, *DataPath );
                        msiGetValByKey( *GQOut, RESC_LOC, *RescLoc);
                        msiGetValByKey( *GQOut, DATA_RESC_NAME, *RescName );
                        msiGetValByKey( *GQOut, DATA_NAME, *DataName );
                        msiGetValByKey( *GQOut, DATA_CHECKSUM, *DataChecksum );
                        msiGetValByKey( *GQOut, RESC_CLASS_NAME, *RescClassName );
                        msiGetValByKey( *GQOut, DATA_REPL_NUM, *DataReplNum );

                        msiExecCmd(rodsMD5sum,*DataPath,*RescLoc,null,null,*execCmdStdOut );
                        msiGetStdoutInExecCmdOut( *execCmdStdOut, *sysMD5sum );

                        if( *DataChecksum != *sysMD5sum ) then {
                                writeLine( stdout, "Checksum mismatch: *DataPath (# *DataReplNum) on *RescName@*RescLoc (*RescClassName)"  );
                                assign( *BadCnt, "*BadCnt + 1" );

                                if( *sysMD5sum == 66 ) then
                                        writeLine( stdout, "*DataName physical file missing on *RescName?" );

                        }
			
			assign( *ObjCnt, "*ObjCnt + 1" );
                }

        }

        writeLine( stdout, "" );
        writeLine( stdout, "Found *BadCnt bad object/s.");

        writeLine( stdout, "" );
        writeLine( stdout, "Checked *ObjCnt total object/s." );
        writeLine( stdout, "" );

}
INPUT *Cond="RESC_CLASS_NAME = 'cache' AND COLL_NAME like '/cdrTestZone/home/fedora%%'"
OUTPUT ruleExecOut
