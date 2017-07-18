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
package fedorax.server.module.storage;

import java.util.Map;

/**
 * @author Gregory Jansen
 *
 */
public interface StagingManagerMBean {
	public Map<String, String> getURLPatternMap();

	public void addURLPattern(String regex, String replacement);

	public void removeURLPattern(String regex);

	public void dumpPatternsToLog();
}
