package edu.unc.lib.boxc.indexing.solr.filter;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.fcrepo.test.AclModelBuilder;
import edu.unc.lib.boxc.search.api.FacetConstants;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import edu.unc.lib.boxc.auth.api.AccessPrincipalConstants;
import edu.unc.lib.boxc.auth.fcrepo.services.InheritedAclFactory;
import edu.unc.lib.boxc.auth.fcrepo.services.ObjectAclFactory;
import edu.unc.lib.boxc.indexing.solr.filter.SetAccessStatusFilter;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;

/**
 * @author harring
 */
public class SetAccessStatusFilterTest {
    private static final String CUSTOM_GROUP = AccessPrincipalConstants.IP_PRINC_NAMESPACE + "special";

    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private IndexDocumentBean idb;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private ContentPathFactory contentPathFactory;
    private InheritedAclFactory inheritedAclFactory;
    private ObjectAclFactory objAclFactory;
    @Mock
    private ContentObject contentObj;
    @Mock
    private CollectionObject parentObj;
    @Mock
    private AdminUnit unitObj;
    @Mock
    private Date date;

    private PID pid;
    private PID parentPid;
    private PID unitPid;

    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    private SetAccessStatusFilter filter;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        pid = PIDs.get(UUID.randomUUID().toString());
        parentPid = PIDs.get(UUID.randomUUID().toString());
        unitPid = PIDs.get(UUID.randomUUID().toString());

        objAclFactory = new ObjectAclFactory();
        objAclFactory.setRepositoryObjectLoader(repoObjLoader);
        objAclFactory.setCacheMaxSize(10);
        objAclFactory.setCacheTimeToLive(1000);
        objAclFactory.init();
        inheritedAclFactory = new InheritedAclFactory();
        inheritedAclFactory.setObjectAclFactory(objAclFactory);
        inheritedAclFactory.setPathFactory(contentPathFactory);

        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);
        when(dip.getContentObject()).thenReturn(contentObj);
        when(contentObj.getPid()).thenReturn(pid);
        when(contentObj.getParentPid()).thenReturn(parentPid);
        when(parentObj.getPid()).thenReturn(parentPid);
        when(parentObj.getParentPid()).thenReturn(unitPid);
        when(unitObj.getPid()).thenReturn(unitPid);
        when(contentObj.getModel()).thenReturn(ModelFactory.createDefaultModel());
        when(parentObj.getModel()).thenReturn(ModelFactory.createDefaultModel());
        when(unitObj.getModel()).thenReturn(ModelFactory.createDefaultModel());

        when(repoObjLoader.getRepositoryObject(pid)).thenReturn(contentObj);
        when(repoObjLoader.getRepositoryObject(parentPid)).thenReturn(parentObj);
        when(repoObjLoader.getRepositoryObject(unitPid)).thenReturn(unitObj);
        when(contentPathFactory.getAncestorPids(unitPid)).thenReturn(
                Arrays.asList(RepositoryPaths.getContentRootPid()));
        when(contentPathFactory.getAncestorPids(parentPid)).thenReturn(
                Arrays.asList(RepositoryPaths.getContentRootPid(), unitPid));
        when(contentPathFactory.getAncestorPids(pid)).thenReturn(
                Arrays.asList(RepositoryPaths.getContentRootPid(), unitPid, parentPid));

        filter = new SetAccessStatusFilter();
        filter.setObjectAclFactory(objAclFactory);
        filter.setInheritedAclFactory(inheritedAclFactory);
    }

    @Test
    public void testIsMarkedForDeletion() throws Exception {
        Model model = new AclModelBuilder(pid).markForDeletion().model;
        when(contentObj.getModel()).thenReturn(model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.MARKED_FOR_DELETION));
        assertFalse(listCaptor.getValue().contains(FacetConstants.EMBARGOED));
    }

    @Test
    public void testIsObjectEmbargoed() throws Exception {
        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addCanViewOriginals(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC).model);

        when(contentObj.getModel()).thenReturn(new AclModelBuilder(pid)
                .addEmbargoUntil(getNextYear()).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertFalse(listCaptor.getValue().contains(FacetConstants.PUBLIC_ACCESS));
        assertTrue(listCaptor.getValue().contains(FacetConstants.EMBARGOED));
        assertFalse(listCaptor.getValue().contains(FacetConstants.EMBARGOED_PARENT));
        assertFalse(listCaptor.getValue().contains(FacetConstants.MARKED_FOR_DELETION));
        assertFalse(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testIsParentEmbargoed() throws Exception {
        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addCanViewOriginals(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC)
                .addEmbargoUntil(getNextYear()).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.EMBARGOED_PARENT));
        assertFalse(listCaptor.getValue().contains(FacetConstants.EMBARGOED));
        assertFalse(listCaptor.getValue().contains(FacetConstants.PUBLIC_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testPatronSettingsUnit() throws Exception {
        when(dip.getPid()).thenReturn(unitPid);
        when(dip.getContentObject()).thenReturn(unitObj);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertFalse(listCaptor.getValue().contains(FacetConstants.PATRON_SETTINGS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testPatronSettingsNoRolesWork() throws Exception {
        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertFalse(listCaptor.getValue().contains(FacetConstants.PATRON_SETTINGS));
        assertTrue(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testPatronSettingsWithCanViewOriginalsRolesWork() throws Exception {
        when(contentObj.getModel()).thenReturn(new AclModelBuilder(pid)
                .addCanViewOriginals(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertFalse(listCaptor.getValue().contains(FacetConstants.PATRON_SETTINGS));
        assertTrue(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testPatronSettingsWithMetadataRoleWork() throws Exception {
        when(contentObj.getModel()).thenReturn(new AclModelBuilder(pid)
                .addCanViewMetadata(PUBLIC_PRINC)
                .addCanViewMetadata(AUTHENTICATED_PRINC).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.PATRON_SETTINGS));
        assertTrue(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testPatronSettingsWithDifferentRolesWork() throws Exception {
        when(contentObj.getModel()).thenReturn(new AclModelBuilder(pid)
                .addCanViewMetadata(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.PATRON_SETTINGS));
        assertTrue(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testPatronSettingsNoRolesCollection() throws Exception {
        when(dip.getPid()).thenReturn(parentPid);
        when(dip.getContentObject()).thenReturn(parentObj);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.PATRON_SETTINGS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testPatronSettingsWithCanViewOriginalsCollection() throws Exception {
        when(dip.getPid()).thenReturn(parentPid);
        when(dip.getContentObject()).thenReturn(parentObj);

        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addCanViewOriginals(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertFalse(listCaptor.getValue().contains(FacetConstants.PATRON_SETTINGS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testPatronSettingsWithMetadataRoleCollection() throws Exception {
        when(dip.getPid()).thenReturn(parentPid);
        when(dip.getContentObject()).thenReturn(parentObj);

        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addCanViewMetadata(PUBLIC_PRINC)
                .addCanViewMetadata(AUTHENTICATED_PRINC).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.PATRON_SETTINGS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testPatronSettingsWithDifferentRolesCollection() throws Exception {
        when(dip.getPid()).thenReturn(parentPid);
        when(dip.getContentObject()).thenReturn(parentObj);

        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addCanViewMetadata(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.PATRON_SETTINGS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testHasStaffOnlyAccess() throws Exception {
        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addCanViewOriginals(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC).model);
        when(contentObj.getModel()).thenReturn(new AclModelBuilder(pid)
                .addNoneRole(PUBLIC_PRINC)
                .addNoneRole(AUTHENTICATED_PRINC).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.STAFF_ONLY_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.PUBLIC_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testHasPublicAccess() throws Exception {
        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addCanViewOriginals(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC).model);
        when(contentObj.getModel()).thenReturn(new AclModelBuilder(pid)
                .addCanViewOriginals(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.PUBLIC_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.STAFF_ONLY_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testHasInheritedPublicAccess() throws Exception {
        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addCanViewOriginals(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.PUBLIC_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.PATRON_SETTINGS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.STAFF_ONLY_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testHasInheritedPublicAccessAndObjectMetadataAccess() throws Exception {
        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addCanViewOriginals(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC).model);
        when(contentObj.getModel()).thenReturn(new AclModelBuilder(pid)
                .addCanViewMetadata(PUBLIC_PRINC)
                .addCanViewMetadata(AUTHENTICATED_PRINC).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertFalse(listCaptor.getValue().contains(FacetConstants.PUBLIC_ACCESS));
        assertTrue(listCaptor.getValue().contains(FacetConstants.PATRON_SETTINGS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.STAFF_ONLY_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testHasPartialAccess() throws Exception {
        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addCanViewMetadata(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertFalse(listCaptor.getValue().contains(FacetConstants.PUBLIC_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.STAFF_ONLY_ACCESS));
        assertTrue(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testHasMatchingMixedRevokedAccess() throws Exception {
        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addNoneRole(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC).model);

        when(contentObj.getModel()).thenReturn(new AclModelBuilder(pid)
                .addNoneRole(PUBLIC_PRINC).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertFalse(listCaptor.getValue().contains(FacetConstants.PUBLIC_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.STAFF_ONLY_ACCESS));
        assertTrue(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testHasMixedRevokedAccess() throws Exception {
        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addCanViewOriginals(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC).model);

        when(contentObj.getModel()).thenReturn(new AclModelBuilder(pid)
                .addNoneRole(PUBLIC_PRINC).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertFalse(listCaptor.getValue().contains(FacetConstants.PUBLIC_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.STAFF_ONLY_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testHasMixedInheritedNoneAccess() throws Exception {
        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addNoneRole(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertFalse(listCaptor.getValue().contains(FacetConstants.PUBLIC_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.STAFF_ONLY_ACCESS));
        assertTrue(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testParentHasStaffOnlyAccess() throws Exception {
        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addCanManage("managerGroup").model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.PARENT_HAS_STAFF_ONLY_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.PATRON_SETTINGS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.STAFF_ONLY_ACCESS));
        assertTrue(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testPatronPermissions() throws Exception {
        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addCanManage("managerGroup").model);
        when(contentObj.getModel()).thenReturn(new AclModelBuilder(pid)
                .addCanViewMetadata(PUBLIC_PRINC)
                .addCanViewAccessCopies(AUTHENTICATED_PRINC).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.PARENT_HAS_STAFF_ONLY_ACCESS));
        assertTrue(listCaptor.getValue().contains(FacetConstants.PATRON_SETTINGS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.STAFF_ONLY_ACCESS));
        assertTrue(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testNoAccessControlsSet() throws Exception {

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.PARENT_HAS_STAFF_ONLY_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.STAFF_ONLY_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.PUBLIC_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.EMBARGOED));
        assertFalse(listCaptor.getValue().contains(FacetConstants.MARKED_FOR_DELETION));
        assertTrue(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testCollectionWithCustomGroup() throws Exception {
        when(dip.getPid()).thenReturn(parentPid);
        when(dip.getContentObject()).thenReturn(parentObj);

        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addCanViewOriginals(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC)
                .addCanViewOriginals(CUSTOM_GROUP).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.PATRON_SETTINGS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testCollectionWithOnlyCustomGroup() throws Exception {
        when(dip.getPid()).thenReturn(parentPid);
        when(dip.getContentObject()).thenReturn(parentObj);

        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addNoneRole(PUBLIC_PRINC)
                .addNoneRole(AUTHENTICATED_PRINC)
                .addCanViewOriginals(CUSTOM_GROUP).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        List<String> status = listCaptor.getValue();
        assertTrue(status.contains(FacetConstants.PATRON_SETTINGS));
        assertFalse(status.contains(FacetConstants.STAFF_ONLY_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testFolderWithCustomGroup() throws Exception {
        redefineContentObject(FolderObject.class);

        when(contentObj.getModel()).thenReturn(new AclModelBuilder(pid)
                .addNoneRole(PUBLIC_PRINC)
                .addNoneRole(AUTHENTICATED_PRINC)
                .addCanViewOriginals(CUSTOM_GROUP).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        List<String> status = listCaptor.getValue();
        assertTrue(status.contains(FacetConstants.PATRON_SETTINGS));
        assertFalse(status.contains(FacetConstants.STAFF_ONLY_ACCESS));
        assertTrue(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testFolderMinimalStaffOnlyWithInheritedCustomGroup() throws Exception {
        redefineContentObject(FolderObject.class);

        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addCanViewMetadata(PUBLIC_PRINC)
                .addCanViewAccessCopies(AUTHENTICATED_PRINC)
                .addCanViewOriginals(CUSTOM_GROUP).model);

        when(contentObj.getModel()).thenReturn(new AclModelBuilder(pid)
                .addNoneRole(PUBLIC_PRINC)
                .addNoneRole(AUTHENTICATED_PRINC).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        List<String> status = listCaptor.getValue();
        assertTrue(status.contains(FacetConstants.PATRON_SETTINGS));
        assertTrue(status.contains(FacetConstants.STAFF_ONLY_ACCESS));
        assertTrue(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testFolderTotallyStaffOnlyWithInheritedCustomGroup() throws Exception {
        redefineContentObject(FolderObject.class);

        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addCanViewMetadata(PUBLIC_PRINC)
                .addCanViewAccessCopies(AUTHENTICATED_PRINC)
                .addCanViewOriginals(CUSTOM_GROUP).model);

        when(contentObj.getModel()).thenReturn(new AclModelBuilder(pid)
                .addNoneRole(PUBLIC_PRINC)
                .addNoneRole(AUTHENTICATED_PRINC)
                .addNoneRole(CUSTOM_GROUP).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        List<String> status = listCaptor.getValue();
        assertTrue(status.contains(FacetConstants.PATRON_SETTINGS));
        assertTrue(status.contains(FacetConstants.STAFF_ONLY_ACCESS));
        assertTrue(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testFolderInheritAssignmentsDirectNoneExceptCustomDirectly() throws Exception {
        redefineContentObject(FolderObject.class);

        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addCanViewMetadata(PUBLIC_PRINC)
                .addCanViewAccessCopies(AUTHENTICATED_PRINC)
                .addCanViewOriginals(CUSTOM_GROUP).model);

        when(contentObj.getModel()).thenReturn(new AclModelBuilder(pid)
                .addNoneRole(PUBLIC_PRINC)
                .addNoneRole(AUTHENTICATED_PRINC)
                .addCanViewOriginals(CUSTOM_GROUP).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        List<String> status = listCaptor.getValue();
        assertTrue(status.contains(FacetConstants.PATRON_SETTINGS));
        assertFalse(status.contains(FacetConstants.STAFF_ONLY_ACCESS));
        assertTrue(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    @Test
    public void testFolderInheritAssignmentsOnlyCustomDirectly() throws Exception {
        redefineContentObject(FolderObject.class);

        when(parentObj.getModel()).thenReturn(new AclModelBuilder(parentPid)
                .addCanViewMetadata(PUBLIC_PRINC)
                .addCanViewAccessCopies(AUTHENTICATED_PRINC)
                .addCanViewOriginals(CUSTOM_GROUP).model);

        when(contentObj.getModel()).thenReturn(new AclModelBuilder(pid)
                .addCanViewOriginals(CUSTOM_GROUP).model);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        List<String> status = listCaptor.getValue();
        assertTrue(status.contains(FacetConstants.PATRON_SETTINGS));
        assertFalse(status.contains(FacetConstants.STAFF_ONLY_ACCESS));
        assertTrue(listCaptor.getValue().contains(FacetConstants.INHERITED_PATRON_SETTINGS));
    }

    private void redefineContentObject(Class<? extends ContentObject> clazz) {
        contentObj = mock(clazz);
        when(dip.getContentObject()).thenReturn(contentObj);
        when(contentObj.getPid()).thenReturn(pid);
        when(repoObjLoader.getRepositoryObject(pid)).thenReturn(contentObj);
    }

    private Calendar getNextYear() {
        Date dt = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(dt);
        c.add(Calendar.DATE, 365);
        return c;
    }
}
