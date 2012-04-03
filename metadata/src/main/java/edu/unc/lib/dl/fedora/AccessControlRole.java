package edu.unc.lib.dl.fedora;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

public enum AccessControlRole {
	patron("patron", Arrays.asList(CDRProperty.permitOriginalsRead, CDRProperty.permitDerivativesRead, CDRProperty.permitMetadataRead)),
	noOriginalsPatron("noOriginalsPatron", Arrays.asList(CDRProperty.permitDerivativesRead, CDRProperty.permitMetadataRead)),
	metadataOnlyPatron("metadataOnlyPatron", Arrays.asList(CDRProperty.permitMetadataRead)),
	curator("curator", Arrays.asList(CDRProperty.permitOriginalsRead, CDRProperty.permitDerivativesRead, CDRProperty.permitMetadataRead,
			CDRProperty.permitOriginalsCreate, CDRProperty.permitDerivativesCreate, CDRProperty.permitMetadataCreate,
			CDRProperty.permitOriginalsUpdate, CDRProperty.permitDerivativesUpdate, CDRProperty.permitMetadataUpdate)),
	admin("admin", Arrays.asList(CDRProperty.permitOriginalsRead, CDRProperty.permitDerivativesRead, CDRProperty.permitMetadataRead,
			CDRProperty.permitOriginalsCreate, CDRProperty.permitDerivativesCreate, CDRProperty.permitMetadataCreate,
			CDRProperty.permitOriginalsUpdate, CDRProperty.permitDerivativesUpdate, CDRProperty.permitMetadataUpdate,
			CDRProperty.permitOriginalsDelete, CDRProperty.permitDerivativesDelete, CDRProperty.permitMetadataDelete));
	
	
	private URI uri;
	private String roleName;
	private List<CDRProperty> permissions;
	//private final static Map<String, List<CDRProperty>> permissionsMap = new HashMap<String, List<CDRProperty>>();
	
	AccessControlRole(String roleName, List<CDRProperty> permissions){
		try {
			this.roleName = roleName;
			this.uri = new URI("http://cdr.unc.edu/definitions/roles#" + roleName);
			this.permissions = permissions;
		} catch (URISyntaxException e) {
			Error x = new ExceptionInInitializerError("Cannot initialize AccessControlRole");
			x.initCause(e);
			throw x;
		}
	}

	public URI getUri() {
		return uri;
	}

	public List<CDRProperty> getPermissions() {
		return permissions;
	}

	public String getRoleName() {
		return roleName;
	}
}
