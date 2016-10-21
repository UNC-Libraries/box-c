package edu.unc.lib.deposit;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.mockito.Mock;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;

public abstract class AbstractDepositJobTest {
	
	@Mock
	private Repository repo;

	@Before
	public void setUp() throws Exception {
		initMocks(this);
		PIDs.setRepository(repo);
		when(repo.getFedoraBase()).thenReturn("http://www.fcrepo.com/");
	}

}
