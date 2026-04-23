package edu.unc.lib.boxc.operations.jms.machineGenerated;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;

/**
* Helper methods for serializing and deserializing machine gen requests
* @author snluong
*/
public class MachineGenRequestSerializationHelper {
    private static final ObjectWriter REQUEST_WRITER;
    private static final ObjectReader REQUEST_READER;

    static {
        ObjectMapper mapper = new ObjectMapper();
        REQUEST_WRITER = mapper.writerFor(MachineGenRequest.class);
        REQUEST_READER = mapper.readerFor(MachineGenRequest.class);
    }

    private MachineGenRequestSerializationHelper(){
    }

    /**
     * Transform request into a JSON string
     * @param request machine gen request
     * @return json
     * @throws IOException
     */
    public static String toJson(MachineGenRequest request) throws IOException {
        return REQUEST_WRITER.writeValueAsString(request);
    }

    /**
     * Transform JSON string to a ThumbnailRequest
     * @param json
     * @return machine gen request
     * @throws IOException
     */
    public static MachineGenRequest toRequest(String json) throws IOException {
        return REQUEST_READER.readValue(json);
    }
}
