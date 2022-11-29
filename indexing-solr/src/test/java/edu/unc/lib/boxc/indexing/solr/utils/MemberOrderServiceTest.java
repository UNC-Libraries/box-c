package edu.unc.lib.boxc.indexing.solr.utils;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author bbpennel
 */
public class MemberOrderServiceTest {
    private static final String PARENT_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String CHILD2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";

    private MemberOrderService memberOrderService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        memberOrderService = new MemberOrderService();
        memberOrderService.init();
    }

    @Test
    public void getOrderValueForAdminUnitTest() {
        orderNotSupportedTest(AdminUnit.class, ContentRootObject.class);
    }

    @Test
    public void getOrderValueForCollectionTest() {
        orderNotSupportedTest(CollectionObject.class, AdminUnit.class);
    }

    @Test
    public void getOrderValueForFolderTest() {
        orderNotSupportedTest(FolderObject.class, CollectionObject.class);
    }

    @Test
    public void getOrderValueForWorkTest() {
        orderNotSupportedTest(WorkObject.class, FolderObject.class);
    }

    private void orderNotSupportedTest(Class<? extends RepositoryObject> memberClass,
                                       Class<? extends RepositoryObject> parentClass) {
        var subject = mockRepositoryObject(memberClass, CHILD1_UUID);
        var parent = mockRepositoryObject(parentClass, PARENT_UUID);
        associateWithParent(subject, parent);

        assertNull(memberOrderService.getOrderValue(subject));
    }

    @Test
    public void getOrderValueForFileInUnorderedWorkTest() {
        var subject = mockRepositoryObject(FileObject.class, CHILD1_UUID);
        var parent = mockRepositoryObject(WorkObject.class, PARENT_UUID);
        associateWithParent(subject, parent);

        assertNull(memberOrderService.getOrderValue(subject));
    }

    @Test
    public void getOrderValueWithFileNotListedInOrderTest() {
        var subject1 = mockRepositoryObject(FileObject.class, CHILD1_UUID);
        var subject2 = mockRepositoryObject(FileObject.class, CHILD2_UUID);
        var parent = mockRepositoryObject(WorkObject.class, PARENT_UUID);
        associateWithParent(subject1, parent);
        associateWithParent(subject2, parent);
        when(parent.getMemberOrder()).thenReturn(pidList(CHILD2_UUID));

        assertNull(memberOrderService.getOrderValue(subject1));
        assertEquals(Integer.valueOf(0), memberOrderService.getOrderValue(subject2));
    }

    @Test
    public void getOrderValueForOrderedFileInSingleFileWorkTest() {
        var subject = mockRepositoryObject(FileObject.class, CHILD1_UUID);
        var parent = mockRepositoryObject(WorkObject.class, PARENT_UUID);
        associateWithParent(subject, parent);
        when(parent.getMemberOrder()).thenReturn(pidList(CHILD1_UUID));

        assertEquals(Integer.valueOf(0), memberOrderService.getOrderValue(subject));
    }

    @Test
    public void getOrderValueForInMultiFileWorkTest() {
        var subject = mockRepositoryObject(FileObject.class, CHILD1_UUID);
        var parent = mockRepositoryObject(WorkObject.class, PARENT_UUID);
        associateWithParent(subject, parent);
        when(parent.getMemberOrder()).thenReturn(pidList(
                randomUuid(), randomUuid(), randomUuid(), CHILD1_UUID, randomUuid()));

        assertEquals(Integer.valueOf(3), memberOrderService.getOrderValue(subject));
    }

    @Test
    public void invalidateTest() {
        var subject1 = mockRepositoryObject(FileObject.class, CHILD1_UUID);
        var subject2 = mockRepositoryObject(FileObject.class, CHILD2_UUID);
        var parent = mockRepositoryObject(WorkObject.class, PARENT_UUID);
        associateWithParent(subject1, parent);
        associateWithParent(subject2, parent);
        when(parent.getMemberOrder()).thenReturn(pidList(CHILD2_UUID));

        assertNull(memberOrderService.getOrderValue(subject1));
        assertEquals(Integer.valueOf(0), memberOrderService.getOrderValue(subject2));

        // Change the list of ordered children then invalidate the cache
        when(parent.getMemberOrder()).thenReturn(pidList(CHILD1_UUID, CHILD2_UUID));
        memberOrderService.invalidate(PIDs.get(PARENT_UUID));

        // Now both children should return order values
        assertEquals(Integer.valueOf(0), memberOrderService.getOrderValue(subject1));
        assertEquals(Integer.valueOf(1), memberOrderService.getOrderValue(subject2));
    }

    private <T> T mockRepositoryObject(Class<T> classToMock, String uuid) {
        var subject = mock(classToMock);
        when(((RepositoryObject) subject).getPid()).thenReturn(PIDs.get(uuid));
        return subject;
    }

    private void associateWithParent(RepositoryObject child, RepositoryObject parent) {
        when(child.getParent()).thenReturn(parent);
        when(child.getParentPid()).thenReturn(PIDs.get(PARENT_UUID));
    }

    private List<PID> pidList(String... uuids) {
        return Arrays.stream(uuids).map(PIDs::get).collect(Collectors.toList());
    }

    private String randomUuid() {
        return UUID.randomUUID().toString();
    }
}
