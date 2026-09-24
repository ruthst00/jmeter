/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jmeter.extractor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestBoundaryExtractor {

    private BoundaryExtractor extractor;

    private SampleResult result;

    private JMeterVariables vars;

    private JMeterContext jmctx;

    @BeforeEach
    public void setUp() {
        jmctx = JMeterContextService.getContext();
        extractor = new BoundaryExtractor();
        extractor.setThreadContext(jmctx);
        extractor.setRefName("regVal");
        result = new SampleResult();
        String data = "<company-xmlext-query-ret><row></row></company-xmlext-query-ret>";
        result.setResponseData(data, null);
        result.setResponseHeaders("Header1: Value1\nHeader2: Value2");
        result.setResponseCode("abcd");
        result.setResponseMessage("The quick brown fox");
        vars = new JMeterVariables();
        jmctx.setVariables(vars);
        jmctx.setPreviousResult(result);
    }

    @Test
    public void testNoBoundariesOneVariable() {
        vars.put("content", "one");
        extractor.setMatchNumber(1);
        extractor.setRefName("varname");
        extractor.setScopeVariable("content");
        extractor.setThreadContext(jmctx);
        extractor.process();
        assertEquals("one", vars.get("varname"));
        assertNull(vars.get("varname_1"), "Indexed variable name should be null");
        assertNull(vars.get("varname_matchNr"), "MatchNumber is incorrect");
    }

    @Test
    public void testNoBoundaries() {
        vars.put("content", "one");
        extractor.setMatchNumber(-1);
        extractor.setRefName("varname");
        extractor.setScopeVariable("content");
        extractor.setThreadContext(jmctx);
        extractor.process();
        assertNull(vars.get("varname"), "Non indexed variable name should be null");
        assertEquals("one", vars.get("varname_1"), "First match is incorrect");
        assertEquals("1", vars.get("varname_matchNr"), "MatchNumber is incorrect");
    }

    @Test
    public void testOnlyLeftBoundary() {
        vars.put("content", "one");
        extractor.setLeftBoundary("o");
        extractor.setMatchNumber(-1);
        extractor.setRefName("varname");
        extractor.setScopeVariable("content");
        extractor.setThreadContext(jmctx);
        extractor.process();

        assertNull(vars.get("varname"), "Non indexed variable name should be null");
        assertEquals("ne", vars.get("varname_1"), "First match is incorrect");
        assertEquals("1", vars.get("varname_matchNr"), "MatchNumber is incorrect");
    }

    @Test
    public void testOnlyRightBoundary() {
        vars.put("content", "one");
        extractor.setRightBoundary("e");
        extractor.setMatchNumber(-1);
        extractor.setRefName("varname");
        extractor.setScopeVariable("content");
        extractor.setThreadContext(jmctx);
        extractor.process();
        assertNull(vars.get("varname"), "Non indexed variable name should be null");
        assertEquals("on", vars.get("varname_1"), "First match is incorrect");
        assertEquals("1", vars.get("varname_matchNr"), "MatchNumber is incorrect");
    }

    /**
     * matchNumber=0 means random: when there is exactly one match the result
     * must equal that match (no ambiguity about which one is chosen).
     */
    @Test
    public void testMatchNumberZeroRandomSingleMatch() {
        vars.put("content", "left-VALUE-right");
        extractor.setLeftBoundary("left-");
        extractor.setRightBoundary("-right");
        extractor.setMatchNumber(0);
        extractor.setRefName("varname");
        extractor.setScopeVariable("content");
        extractor.setThreadContext(jmctx);
        extractor.process();
        assertEquals("VALUE", vars.get("varname"),
                "matchNumber=0 (random) with a single match should return that match");
        assertNull(vars.get("varname_matchNr"),
                "matchNr variable should not be set for matchNumber=0");
    }

    /**
     * matchNumber=0 means random: when there are multiple matches the result
     * must be one of the available matches.
     */
    @Test
    public void testMatchNumberZeroRandomMultipleMatches() {
        vars.put("content", "left-A-right left-B-right left-C-right");
        extractor.setLeftBoundary("left-");
        extractor.setRightBoundary("-right");
        extractor.setMatchNumber(0);
        extractor.setRefName("varname");
        extractor.setScopeVariable("content");
        extractor.setThreadContext(jmctx);
        extractor.process();
        String found = vars.get("varname");
        assertNotNull(found, "matchNumber=0 (random) should return a non-null result when matches exist");
        assertTrue("A".equals(found) || "B".equals(found) || "C".equals(found),
                "matchNumber=0 (random) result '" + found + "' should be one of the available matches");
        assertNull(vars.get("varname_matchNr"),
                "matchNr variable should not be set for matchNumber=0");
    }

    /**
     * An empty Match No. field is stored as "" which resolves to 0 via
     * getIntValue(), so it must behave identically to matchNumber=0 (random).
     */
    @Test
    public void testEmptyMatchNumberFieldBehavesLikeZero() {
        vars.put("content", "left-ONLY-right");
        extractor.setLeftBoundary("left-");
        extractor.setRightBoundary("-right");
        // Simulate the GUI leaving the field blank: store an empty string property
        extractor.setMatchNumber("");
        extractor.setRefName("varname");
        extractor.setScopeVariable("content");
        extractor.setThreadContext(jmctx);
        extractor.process();
        assertEquals("ONLY", vars.get("varname"),
                "Empty Match No. field (defaults to 0/random) should return the single available match");
    }
}
