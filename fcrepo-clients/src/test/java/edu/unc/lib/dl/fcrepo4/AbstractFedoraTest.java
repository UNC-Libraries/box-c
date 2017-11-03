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
package edu.unc.lib.dl.fcrepo4;

import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.mockito.Mock;

/**
 *
 * @author harring
 *
 */
public class AbstractFedoraTest {

    protected static final String FEDORA_BASE = "http://example.com/";

    @Mock
    protected RepositoryObjectDriver driver;
    @Mock
    protected RepositoryPaths repoPaths;
    @Mock
    protected RepositoryObjectFactory repoObjFactory;

    protected RepositoryPIDMinter pidMinter;

    @Before
    public void initBase() {
        initMocks(this);

        pidMinter = new RepositoryPIDMinter();
    }

}
