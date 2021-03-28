package com.miro;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.miro.entities.Widget;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class WidgetControllerTest {

    @Autowired
    private MockMvc mvc;
    private static final ObjectMapper mapper = new JsonMapper();

    @BeforeAll
    private static void init() {
        mapper.registerModule(new JavaTimeModule());
    }

    @Test
    public void getHello() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("Greetings from the Widget Controller!")));
    }

    @Test
    public void testCreateWidgetApi() throws Exception {
        // ingest some data
        String sw1 = "{\"x\": 10, \"y\": 10, \"width\": 3, \"height\" : 40}";
        String ret = "{\"x\": 10, \"y\": 10, \"width\": 3, \"height\" : 40, \"zIndex\" : 1}";
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/widgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sw1)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().json(ret))
                .andReturn();

        // cleanup
        Widget w1 = mapper.readValue(result.getResponse().getContentAsString(), Widget.class);
        deleteWidget(w1);
    }

    @Test
    public void testCreateInvalidWidgetApi() throws Exception {
        // try to save a widget with negative width
        String sw1 = "{\"x\": 10, \"y\": 10, \"width\": -3, \"height\" : 40}";
        mvc.perform(MockMvcRequestBuilders.post("/widgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sw1)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetAllWidgetsApi() throws Exception {
        // ingest some data
        String sw1 = "{\"x\": 10, \"y\": 10, \"width\": 3, \"height\" : 40, \"zIndex\" : 10}";
        String sw2 = "{\"x\": 80, \"y\": 80, \"width\": 3, \"height\" : 40, \"zIndex\" : 8}";
        String sw3 = "{\"x\": 40, \"y\": 40, \"width\": 3, \"height\" : 40, \"zIndex\" : 4}";

        Widget w1 = createWidget(sw1);
        Widget w2 = createWidget(sw2);
        Widget w3 = createWidget(sw3);

        // call the get list api and check the results
        String ret = "[{\"x\": 40, \"y\": 40, \"width\": 3, \"height\" : 40, \"zIndex\" : 4}," +
                "{\"x\": 80, \"y\": 80, \"width\": 3, \"height\" : 40, \"zIndex\" : 8}," +
                "{\"x\": 10, \"y\": 10, \"width\": 3, \"height\" : 40, \"zIndex\" : 10}]";
        mvc.perform(MockMvcRequestBuilders.get("/widgets")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(ret));

        // cleanup
        deleteWidget(w1);
        deleteWidget(w2);
        deleteWidget(w3);
    }

    @Test
    public void testGetById() throws Exception {
        // ingest some data
        String sw1 = "{\"x\": 10, \"y\": 10, \"width\": 3, \"height\" : 40, \"zIndex\" : 10}";
        String sw2 = "{\"x\": 80, \"y\": 80, \"width\": 3, \"height\" : 40, \"zIndex\" : 8}";
        String sw3 = "{\"x\": 40, \"y\": 40, \"width\": 3, \"height\" : 40, \"zIndex\" : 4}";

        Widget w1 = createWidget(sw1);
        Widget w2 = createWidget(sw2);
        Widget w3 = createWidget(sw3);

        String ret = "{\"x\": 80, \"y\": 80, \"width\": 3, \"height\" : 40, \"zIndex\" : 8}";
        mvc.perform(MockMvcRequestBuilders.get("/widgets/" + w2.getId())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(ret));

        // cleanup
        deleteWidget(w1);
        deleteWidget(w2);
        deleteWidget(w3);
    }

    @Test
    public void testDeleteById() throws Exception {
        // ingest some data
        String sw1 = "{\"x\": 10, \"y\": 10, \"width\": 3, \"height\" : 40, \"zIndex\" : 10}";
        String sw2 = "{\"x\": 80, \"y\": 80, \"width\": 3, \"height\" : 40, \"zIndex\" : 8}";
        String sw3 = "{\"x\": 40, \"y\": 40, \"width\": 3, \"height\" : 40, \"zIndex\" : 4}";

        Widget w1 = createWidget(sw1);
        Widget w2 = createWidget(sw2);
        Widget w3 = createWidget(sw3);

        deleteWidget(w2);

        mvc.perform(MockMvcRequestBuilders.get("/widgets/" + w2.getId())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // cleanup
        deleteWidget(w1);
        deleteWidget(w3);
    }

    private void deleteWidget(Widget w) throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/widgets/" + w.getId())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    private Widget createWidget(String w1) throws Exception {
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/widgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(w1)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(), Widget.class);
    }
}