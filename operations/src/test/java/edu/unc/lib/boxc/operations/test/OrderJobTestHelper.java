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

import edu.unc.lib.boxc.operations.impl.order.OrderRequestFactory;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import edu.unc.lib.boxc.operations.jms.order.OrderRequest;

import java.util.Arrays;

/**
 * Helper methods for all OrderJob related tests
 * @author snluong
 */
public class OrderJobTestHelper {
    public static OrderRequest createRequest(OrderOperationType operationType, String parentUuid, String... children) {
        return OrderRequestFactory.createRequest(
                operationType, parentUuid, Arrays.asList(children));
    }
}
