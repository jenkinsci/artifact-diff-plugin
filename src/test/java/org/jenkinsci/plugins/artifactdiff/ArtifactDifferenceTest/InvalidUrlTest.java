/*
 * The MIT License
 *
 * Copyright (c) 2012 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.artifactdiff.ArtifactDifferenceTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.model.Run;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.jenkinsci.plugins.artifactdiff.ArtifactDifference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(Parameterized.class)
public class InvalidUrlTest {

    @Mock private Run<?, ?> run;
    @Mock private StaplerRequest req;
    @Mock private StaplerResponse rsp;

    private final String urlCandidate;
    private final String message;

    @Before
    public void initMocks() {

        MockitoAnnotations.initMocks(this);
    }

    public InvalidUrlTest(final String urlCandidate, final String message) {

        this.urlCandidate = urlCandidate;
        this.message = message;
    }

    @Parameterized.Parameters
    public static Collection<String[]> invalidUrls() {
        return Arrays.asList(new String[][]{
                { "/3", "Malformed url" },
                { "/3.14/README", "Malformed url" },
//                { "/3/", "Illegal file path" }, ??? Show files that differ between builds ???
                { "/3/../", "Illegal file path" },
        });
    }

    @Test
    public void useInvalidUrl() throws IOException, ServletException {

        final StringWriter writer = new StringWriter();

        when(rsp.getWriter()).thenReturn(new PrintWriter(writer));

        generate(
                new ArtifactDifference(run),
                getRequestStub(req, urlCandidate),
                rsp
        );

        verify(rsp).sendError(HttpServletResponse.SC_BAD_REQUEST, message);
    }

    private static StaplerRequest getRequestStub(final StaplerRequest req, final String path) {

        when(req.getRestOfPath()).thenReturn(path);

        return req;
    }

    private static void generate(
            final ArtifactDifference diff,
            final StaplerRequest req,
            final StaplerResponse rsp
    ) throws IOException, ServletException {

        diff.serve(req, rsp, null);
    }
}
