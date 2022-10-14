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
package edu.unc.lib.boxc.operations.test;

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.operations.api.order.OrderValidator;
import edu.unc.lib.boxc.operations.impl.order.OrderRequestFactory;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import edu.unc.lib.boxc.operations.jms.order.OrderRequest;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Helper methods for all OrderJob related tests
 * @author snluong
 */
public class OrderTestHelper {
    public static OrderRequest createRequest(OrderOperationType operationType, String parentUuid, String... children) {
        return OrderRequestFactory.createRequest(
                operationType, parentUuid, Arrays.asList(children));
    }

    public static void mockParentType(RepositoryObject parentObj, ResourceType resourceType) {
        when(parentObj.getResourceType()).thenReturn(resourceType);
    }

    public static void assertHasErrors(OrderValidator validator, String... expected) {
        var msg = "Expected errors:\n[" + String.join(",", expected) + "]\nbut errors were:\n" + validator.getErrors();
        assertTrue(msg, validator.getErrors().containsAll(Arrays.asList(expected)));
        assertEquals(msg, expected.length, validator.getErrors().size());
    }
}
