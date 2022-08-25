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
package edu.unc.lib.boxc.operations.jms.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;

/**
 * Helper methods for serializing and deserializing order requests
 *
 * @author bbpennel
 */
public class OrderRequestSerializationHelper {
    private static final ObjectWriter MULTI_WRITER;
    private static final ObjectReader MULTI_READER;
    static {
        ObjectMapper mapper = new ObjectMapper();
        MULTI_WRITER = mapper.writerFor(MultiParentOrderRequest.class);
        MULTI_READER = mapper.readerFor(MultiParentOrderRequest.class);
    }

    private OrderRequestSerializationHelper() {
    }

    /**
     * Transform request into a JSON string
     * @param request
     * @return
     * @throws IOException
     */
    public static String toJson(MultiParentOrderRequest request) throws IOException {
        return MULTI_WRITER.writeValueAsString(request);
    }

    /**
     * Transform JSON string to a MultiParentOrderRequest
     * @param json
     * @return
     * @throws IOException
     */
    public static MultiParentOrderRequest toRequest(String json) throws IOException {
        return MULTI_READER.readValue(json);
    }
}
