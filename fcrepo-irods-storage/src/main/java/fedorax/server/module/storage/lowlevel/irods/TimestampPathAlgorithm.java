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
package fedorax.server.module.storage.lowlevel.irods;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

import org.fcrepo.server.errors.LowlevelStorageException;
import org.fcrepo.server.storage.lowlevel.PathAlgorithm;

/**
 * This class was copied wholesale from the original by Bill Niebel as part of the default lowlevel storage module for
 * Fedora Commons.  It has not changed at all yet, but the original had default scope and was unavailable to custom
 * modules.
 * @author Bill Niebel
 */
class TimestampPathAlgorithm extends PathAlgorithm {

    private final String storeBase;

    private static final String[] PADDING = { "", "0", "00", "000" };

    private static final String SEP = File.separator;

    public TimestampPathAlgorithm(Map<String, ?> configuration) {
	super(configuration);
	storeBase = (String) configuration.get("storeBase");
    }

    @Override
    public final String get(String pid) throws LowlevelStorageException {
	return format(encode(pid));
    }

    public String format(String pid) throws LowlevelStorageException {
	GregorianCalendar calendar = new GregorianCalendar();
	String year = Integer.toString(calendar.get(Calendar.YEAR));
	String month = leftPadded(1 + calendar.get(Calendar.MONTH), 2);
	String dayOfMonth = leftPadded(calendar.get(Calendar.DAY_OF_MONTH), 2);
	String hourOfDay = leftPadded(calendar.get(Calendar.HOUR_OF_DAY), 2);
	String minute = leftPadded(calendar.get(Calendar.MINUTE), 2);
	// String second = leftPadded(calendar.get(Calendar.SECOND),2);
	return storeBase + SEP + year + SEP + month + dayOfMonth + SEP + hourOfDay + SEP + minute /*
												   * +
												   * sep
												   * +
												   * second
												   */+ SEP + pid;
    }

    private final String leftPadded(int i, int n) throws LowlevelStorageException {
	if (n > 3 || n < 0 || i < 0 || i > 999) {
	    throw new LowlevelStorageException(true, getClass().getName() + ": faulty date padding");
	}
	int m = i > 99 ? 3 : i > 9 ? 2 : 1;
	int p = n - m;
	return PADDING[p] + Integer.toString(i);
    }
}
