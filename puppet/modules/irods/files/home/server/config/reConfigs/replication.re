cdrScheduleReplicateObjects() {
  
  *interval = cdrConfigReplicationScheduleInterval
  
  delay("<EF>*interval REPEAT FOR EVER</EF>") {
    cdrReplicateObjects()
  }
  
}

cdrReplicateObjects {

  *objectLimit = cdrConfigReplicationObjectLimit
  *destResc = cdrConfigReplicationDestResc
  *logPath = cdrConfigReplicationLogPath
  *emailToAddr = cdrConfigReplicationEmailToAddr
  
  *replAttrName = cdrReplicationNeededAttrName
  
  if (*destResc == "") {
    writeLine("stdout", "Blank replication destination, exiting")
    break
  }
  
  # Ensure we have a log file to write to
  msiSplitPath(*logPath, *logCollName, *logDataName)
  if (!cdrObjectExists(*logCollName, *logDataName)) {
    msiDataObjCreate(*logPath, "", *f)
    msiDataObjClose(*f, "null")
  }
  
  # Query for objects needing replication
  msiMakeGenQuery("DATA_NAME, COLL_NAME", "META_DATA_ATTR_NAME = '*replAttrName'", *query)
  msiExecGenQuery(*query, *out)
  msiGetContInxFromGenQueryOut(*out, *contInxNew)
  
  *contInxOld = 1
  *objectCount = 0
  *hasErrors = false
  
  while (*contInxOld > 0 && *objectCount < *objectLimit) {
    foreach (*out) {
      msiGetValByKey(*out, "DATA_NAME", *dataName)
      msiGetValByKey(*out, "COLL_NAME", *collName)
      
      *code = cdrReplicateObject(*collName, *dataName, *destResc)
      
      if (*code < 0) {
        cdrAppend(*logPath, "Failed to replicate *collName/*dataName to *destResc (*code)\n")
        *hasErrors = true
      }
      
      *objectCount = *objectCount + 1
      if (*objectCount >= *objectLimit) {
        break
      }
    }
    
    *contInxOld = *contInxNew
    if (*contInxOld > 0) {
      msiGetMoreRows(*query, *out, *contInxNew)
    }
  }
  
  if (*hasErrors) {
    msiSendMail(*emailToAddr, "Replication errors", "The error log is available at *logPath.")
  }

}

cdrReplicateObject(*collName, *dataName, *destRescName) {
  
  *code = 0
  
  *objPath = "*collName/*dataName"

  if (cdrShouldVerifyOrReplicate(*objPath) && cdrObjectExists(*collName, *dataName)) {
    *replOptions = "destRescName=*destRescName++++all="
    *code = errorcode(msiDataObjRepl(*objPath, *replOptions, *status))
    
    if (*code >= 0) {
      cdrClearNeedsReplication(*objPath)
    }
  }
  
  *code
  
}

cdrReplicationNeededAttrName {
  "cdrReplicationNeeded"
}

cdrSetNeedsReplication(*objPath) {
  msiAddKeyVal(*keyval, cdrReplicationNeededAttrName, "true");
  msiSetKeyValuePairsToObj(*keyval, *objPath, "-d")
}

cdrClearNeedsReplication(*objPath) {
  msiAddKeyVal(*keyval, cdrReplicationNeededAttrName, "true");
  msiRemoveKeyValuePairsFromObj(*keyval, *objPath, "-d")
}

cdrObjectExists(*collName, *dataName) {

  msiMakeGenQuery("DATA_ID", "COLL_NAME = '*collName' and DATA_NAME = '*dataName'", *query)
  msiExecGenQuery(*query, *out)

  *any = false
  foreach (*out) {
    *any = true
    break
  }
  
  msiCloseGenQuery(*query, *out)
  
  *any

}

# Append the *string to the file at *path. Note that *path must already exist.

cdrAppend(*path, *string) {

  msiDataObjOpen("objPath=*path++++openFlags=O_RDWR", *f)
  msiDataObjLseek(*f, 0, "SEEK_END", *status)
  msiDataObjWrite(*f, *string, *wlen)
  msiDataObjClose(*f, *status)

}
