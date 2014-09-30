# Return the name of the fixity timestamp attribute

cdrFixityAttrName {
  "cdrFixityTimestamp"
}

# Set the fixity timestamp of the object to the current iCAT timestamp

cdrTouchFixityTimestamp(*path) {
  msiGetIcatTime(*time, "unix")
  msiAddKeyVal(*keyval, cdrFixityAttrName, str(int(*time)))
  msiSetKeyValuePairsToObj(*keyval, *path, "-d")
}

# Set the fixity timestamp of the object to zero

cdrResetFixityTimestamp(*path) {
  msiAddKeyVal(*keyval, cdrFixityAttrName, "0")
  msiSetKeyValuePairsToObj(*keyval, *path, "-d")
}
