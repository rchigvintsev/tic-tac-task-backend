package org.briarheart.orchestra.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(TestController.class)
public class GlobalExceptionHandlingControllerAdviceTest {
    @Autowired
    private MockMvc mvc;

    @Test
    public void shouldReturnBadRequestResponseStatusInCaseOfConstraintViolationException() throws Exception {
        mvc.perform(post("/constraintViolationException")).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnErrorMessagesInResponseBodyInCaseOfConstraintViolationException() throws Exception {
        mvc.perform(post("/constraintViolationException"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.messages").exists());
    }
}