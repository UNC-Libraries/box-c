package edu.unc.lib.dl.fcrepo4;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.mockito.Mock;

public class AbstractFedoraTest {
	
	protected static final String FEDORA_BASE = "http://example.com/";
	
	@Mock
	protected RepositoryObjectDataLoader dataLoader;
	@Mock
	protected Repository repository;

	@Before
	public void initBase() {
		initMocks(this);
		
		PIDs.setRepository(repository);
		when(repository.getFedoraBase()).thenReturn(FEDORA_BASE);
	}

}
