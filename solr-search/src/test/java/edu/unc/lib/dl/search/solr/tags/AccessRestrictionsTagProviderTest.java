package edu.unc.lib.dl.search.solr.tags;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.Tag;
import static org.mockito.Mockito.*;

public class AccessRestrictionsTagProviderTest extends Assert {

	@Test
	public void embargoed() {
		AccessRestrictionsTagProvider tagProvider = new AccessRestrictionsTagProvider();
		BriefObjectMetadata metadata = mock(BriefObjectMetadata.class);
		Set<UserRole> roles = new HashSet<UserRole>();
		ObjectAccessControlsBean access = mock(ObjectAccessControlsBean.class);
		when(access.getRoles(any(String[].class))).thenReturn(roles);
		when(metadata.getAccessControlBean()).thenReturn(access);
		when(metadata.getRelations()).thenReturn(Arrays.asList("embargo-until|2084-03-05T00:00:00"));
		
		tagProvider.addTags(metadata, null);
		
		Mockito.verify(metadata, Mockito.atMost(1)).addTag(any(Tag.class));
	}
}
