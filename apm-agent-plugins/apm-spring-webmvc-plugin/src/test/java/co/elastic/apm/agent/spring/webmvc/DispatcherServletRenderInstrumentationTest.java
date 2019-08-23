/*-
 * #%L
 * Elastic APM Java agent
 * %%
 * Copyright (C) 2018 - 2019 Elastic and contributors
 * %%
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * #L%
 */
package co.elastic.apm.agent.spring.webmvc;

import co.elastic.apm.agent.AbstractInstrumentationTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class DispatcherServletRenderInstrumentationTest extends AbstractInstrumentationTest {

    private static MockMvc mockMvc;

    @BeforeClass
    public static void setUpAll() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new MessageController())
            .setViewResolvers(jspViewResolver())
            .build();
    }

    private static ViewResolver jspViewResolver() {
        InternalResourceViewResolver bean = new InternalResourceViewResolver();
        bean.setPrefix("/static/");
        bean.setSuffix(".jsp");
        return bean;
    }

    @Controller
    public static class MessageController {
        @GetMapping("/test")
        public ModelAndView test() {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("message-view");
            return modelAndView;
        }
    }

    @Test
    public void testCallOfDispatcherServletWithNonNullModelAndView() throws Exception {
        reporter.reset();
        mockMvc.perform(get("/test"));
        assertEquals(1, reporter.getTransactions().size());
        assertEquals(1, reporter.getSpans().size());
        assertEquals("DispatcherServlet#render message-view", reporter.getSpans().get(0).getNameAsString());
    }
}
