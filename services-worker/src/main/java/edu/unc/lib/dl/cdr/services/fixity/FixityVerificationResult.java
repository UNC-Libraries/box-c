/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.cdr.services.fixity;

import java.util.Date;

import org.irods.jargon.core.protovalues.ErrorEnum;
import org.irods.jargon.core.exception.JargonException;

import edu.unc.lib.dl.fedora.PID;

/**
 * 
 * @author mdaines
 *
 */
public class FixityVerificationResult {

     public static enum Result { OK, FAILED, ERROR, MISSING }

     private Date time;
     private String objectPath;
     private String resourceName;
     private String jargonVersion;
     private String irodsReleaseVersion;
     private String expectedChecksum;
     private String filePath;
     private Double elapsed;
     private Integer irodsErrorCode;
     private JargonException jargonException;
     private Result result;

     /**
      * The time at which this result was recorded.
      */

     public Date getTime() {
          return time;
     }

     public void setTime(Date time) {
          this.time = time;
     }

     /**
      * The iRODS object for which this result was recorded.
      */

     public String getObjectPath() {
          return objectPath;
     }

     public void setObjectPath(String objectPath) {
          this.objectPath = objectPath;
     }

     /**
      * The resource on which fixity checking was done.
      */

     public String getResourceName() {
          return resourceName;
     }

     public void setResourceName(String resourceName) {
          this.resourceName = resourceName;
     }

     /**
      * The Jargon version used to generate this result.
      */

     public String getJargonVersion() {
          return jargonVersion;
     }

     public void setJargonVersion(String jargonVersion) {
          this.jargonVersion = jargonVersion;
     }

     /**
      * The iRODS release version used to generate this result.
      */

     public String getIrodsReleaseVersion() {
          return irodsReleaseVersion;
     }

     public void setIrodsReleaseVersion(String irodsReleaseVersion) {
          this.irodsReleaseVersion = irodsReleaseVersion;
     }

     /**
      * The expected checksum. If no checksum was recorded or the object was not in the iCAT, this is null.
      */

     public String getExpectedChecksum() {
          return expectedChecksum;
     }

     public void setExpectedChecksum(String expectedChecksum) {
          this.expectedChecksum = expectedChecksum;
     }

     /**
      * The actual file path checked. If the object was not in the iCAT on this resource, this is null.
      */

     public String getFilePath() {
          return filePath;
     }

     public void setFilePath(String filePath) {
          this.filePath = filePath;
     }

     /**
      * Elapsed time for checksum verification in seconds, if performed. Otherwise, null.
      */

     public Double getElapsed() {
          return elapsed;
     }

     public void setElapsed(Double elapsed) {
          this.elapsed = elapsed;
     }

     /**
      * The iRODS error code, if any was recorded. Otherwise, null.
      */

     public Integer getIrodsErrorCode() {
          return irodsErrorCode;
     }

     public void setIrodsErrorCode(Integer irodsErrorCode) {
          this.irodsErrorCode = irodsErrorCode;
     }

     /**
      * The Jargon exception, if any was recorded. Otherwise, null.
      */

     public JargonException getJargonException() {
          return jargonException;
     }

     public void setJargonException(JargonException jargonException) {
          this.jargonException = jargonException;
     }

     /**
      * The result of fixity checking. Possible values:
      * 
      * OK - The checksum of the object was calculated successfully and it matched the checksum recorded in the iCAT.
      * FAILED - The checksum of the object was calculated successfully, but it didn't match the
      * checksum recorded in the iCAT.
      * ERROR - An error occurred while calculating the checksum. A description of the error’s result code
      * can be found using the ierror command, or in the Jargon exception's message field.
      * MISSING - Checksum verification couldn't be performed because the object isn't in the specified resource.
      */

     public Result getResult() {
          return result;
     }

     public void setResult(Result result) {
          this.result = result;
     }

     /**
      * The iRODS error corresponding to the error code. If there is no corresponding iRODS error code, null.
      */

     public ErrorEnum getError() {
          if (irodsErrorCode == null) {
               return null;
          }

          try {
               return ErrorEnum.valueOf(irodsErrorCode.intValue());
          } catch (IllegalArgumentException e) {
               return null;
          }
     }

     /**
      * If the object path has a PID, return it. Otherwise, return null.
      */

     public PID getPID() {

          int pidStart = objectPath.indexOf("uuid_");

          if (pidStart == -1) {
               return null;
          }

          int pidEnd = objectPath.indexOf("+", pidStart);

          if (pidEnd == -1) {
               return new PID(objectPath.substring(pidStart).replace("_", ":"));
          } else {
               return new PID(objectPath.substring(pidStart, pidEnd).replace("_", ":"));
          }

     }

     /**
      * If the object path has a PID and a datastream, return the datastream. Otherwise, return null.
      */

     public String getDatastream() {

          int pidStart = objectPath.indexOf("uuid_");

          if (pidStart == -1) {
               return null;
          }

          int pidEnd = objectPath.indexOf("+", pidStart);

          if (pidEnd == -1) {
               return null;
          }

          int dsidStart = pidEnd + 1;
          int dsidEnd = objectPath.indexOf("+", dsidStart);

          if (dsidEnd == -1) {
               return objectPath.substring(dsidStart);
          } else {
               return objectPath.substring(dsidStart, dsidEnd);
          }
     }

     public String toString() {
          return "FixityVerificationResult [" +
                    "time=" + time + ", " +
                    "objectPath=" + objectPath + ", " +
                    "resourceName=" + resourceName + ", " +
                    "jargonVersion=" + jargonVersion + ", " +
                    "irodsReleaseVersion=" + irodsReleaseVersion + ", " +
                    "expectedChecksum=" + expectedChecksum + ", " +
                    "filePath=" + filePath + ", " +
                    "elapsed=" + elapsed + ", " +
                    "irodsErrorCode=" + irodsErrorCode + ", " +
                    "jargonException=" + jargonException + ", " +
                    "result=" + result +
                    "]";
     }

}
