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
    public void testUpdateWidgetApi() throws Exception {
        String sw1 = "{\"x\": 10, \"y\": 10, \"width\": 3, \"height\" : 40, \"zIndex\" : 10}";
        String sw2 = "{\"x\": 50, \"y\": 50, \"width\": 55, \"height\" : 55, \"zIndex\" : 20}";
        Widget w1 = createWidget(sw1);
        Widget w2 = createWidget(sw2);

        /* The widget id does not change */
        String updatedWidget = "{ \"id\": 100, \"y\": 70, \"width\": 77, \"zIndex\" : 20}";
        String ret = "{ \"id\" : " + w1.getId() + ", \"x\": 10, \"y\": 70, \"width\": 77, \"height\" : 40, \"zIndex\" : 20}";
        mvc.perform(MockMvcRequestBuilders.put("/widgets/" + w1.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedWidget)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(ret));

        /* check that the z-index of the widget w2 has been modified (shift) */
        ret = "{ \"id\" : " + w2.getId() + ", \"x\": 50, \"y\": 50, \"width\": 55, \"height\" : 55, \"zIndex\" : 21}";
        mvc.perform(MockMvcRequestBuilders.get("/widgets/" + w2.getId())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(ret));

        mvc.perform(MockMvcRequestBuilders.get("/widgets/" + w2.getId() + 1)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // cleanup
        deleteWidget(w1);
        deleteWidget(w2);
    }

    @Test
    public void testUpdateWidgetApiException() throws Exception {
        String sw1 = "{\"x\": 10, \"y\": 10, \"width\": 3, \"height\" : 40, \"zIndex\" : 10}";
        Widget w1 = createWidget(sw1);

        /* Expected to fail (return 400) as the width cannot be negative */
        String updatedWidget = "{ \"y\": 70, \"width\": -77, \"zIndex\" : 20}";
        mvc.perform(MockMvcRequestBuilders.put("/widgets/" + w1.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedWidget)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // cleanup
        deleteWidget(w1);
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
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(ret));

        // cleanup
        deleteWidget(w1);
        deleteWidget(w2);
        deleteWidget(w3);
    }

    @Test
    public void testGetAllWidgetsWithPaginationApi() throws Exception {
        // ingest some data
        String sw1 = "{\"x\": 10, \"y\": 10, \"width\": 3, \"height\" : 10, \"zIndex\" : 80}";
        String sw2 = "{\"x\": 20, \"y\": 20, \"width\": 3, \"height\" : 20, \"zIndex\" : 20}";
        String sw3 = "{\"x\": 30, \"y\": 30, \"width\": 3, \"height\" : 30, \"zIndex\" : 30}";
        String sw4 = "{\"x\": 40, \"y\": 40, \"width\": 3, \"height\" : 40, \"zIndex\" : 40}";
        String sw5 = "{\"x\": 50, \"y\": 50, \"width\": 3, \"height\" : 50, \"zIndex\" : 50}";
        String sw6 = "{\"x\": 60, \"y\": 60, \"width\": 3, \"height\" : 60, \"zIndex\" : 60}";
        String sw7 = "{\"x\": 70, \"y\": 70, \"width\": 3, \"height\" : 70, \"zIndex\" : 70}";
        String sw8 = "{\"x\": 80, \"y\": 80, \"width\": 3, \"height\" : 80, \"zIndex\" : 10}";

        Widget w1 = createWidget(sw1);
        Widget w2 = createWidget(sw2);
        Widget w3 = createWidget(sw3);
        Widget w4 = createWidget(sw4);
        Widget w5 = createWidget(sw5);
        Widget w6 = createWidget(sw6);
        Widget w7 = createWidget(sw7);
        Widget w8 = createWidget(sw8);

        // call the get list api and check the results
        String ret = "[{\"x\": 50, \"y\": 50, \"width\": 3, \"height\" : 50, \"zIndex\" : 50}," +
                "{\"x\": 60, \"y\": 60, \"width\": 3, \"height\" : 60, \"zIndex\" : 60}," +
                "{\"x\": 70, \"y\": 70, \"width\": 3, \"height\" : 70, \"zIndex\" : 70}," +
                "{\"x\": 10, \"y\": 10, \"width\": 3, \"height\" : 10, \"zIndex\" : 80} ]";
        mvc.perform(MockMvcRequestBuilders.get("/widgets")
                .param("size", "4")
                .param("page", "2")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(ret));

        // cleanup
        deleteWidget(w1);
        deleteWidget(w2);
        deleteWidget(w3);
        deleteWidget(w4);
        deleteWidget(w5);
        deleteWidget(w6);
        deleteWidget(w7);
        deleteWidget(w8);
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