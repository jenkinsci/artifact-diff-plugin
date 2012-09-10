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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import hudson.model.Job;
import hudson.model.Run;

import java.io.IOException;

import javax.servlet.ServletException;

import org.jenkinsci.plugins.artifactdiff.ArtifactDifference;
import org.jenkinsci.plugins.artifactdiff.DiffResponse;
import org.jenkinsci.plugins.artifactdiff.Response;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


public class ArtifactDifferenceTest {

    @Mock private Job<?, ?> project;
    @Mock private Run<?, ?> run;
    @Mock private StaplerRequest req;
    @Mock private StaplerResponse rsp;

    @Before
    public void initMocks() {

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getArtifactListResponse() throws IOException, ServletException {

        when(req.getRestOfPath()).thenReturn("");

        final HttpResponse response = new ArtifactDifference(run).doDynamic(req, rsp);

        assertThat(response, instanceOf(Response.ArtifactList.class));
    }

    @Test
    public void getPlainDiffResponse() throws IOException, ServletException {

        doReturn(project).when(run).getParent();
        doReturn(run).when(project).getBuildByNumber(1);

        when(req.getRestOfPath()).thenReturn("/1/path");
        when(req.getParameter("output")).thenReturn("plain");

        final HttpResponse response = new ArtifactDifference(run).doDynamic(req, rsp);

        assertThat(response, instanceOf(DiffResponse.Plain.class));
    }

    @Test
    public void getHtmlDiffResponse() throws IOException, ServletException {

        doReturn(project).when(run).getParent();
        doReturn(run).when(project).getBuildByNumber(1);

        when(req.getRestOfPath()).thenReturn("/1/path");
        when(req.getParameter("output")).thenReturn("html");

        final HttpResponse response = new ArtifactDifference(run).doDynamic(req, rsp);

        assertThat(response, instanceOf(DiffResponse.Html.class));
    }

    @Test(expected=Response.Exception.NotFound.class)
    public void getNoSuchBuild() throws IOException, ServletException {

        doReturn(project).when(run).getParent();
        doReturn(null).when(project).getBuildByNumber(1);

        when(req.getRestOfPath()).thenReturn("/1/path");

        final HttpResponse response = new ArtifactDifference(run).doDynamic(req, rsp);

        assertThat(response, instanceOf(DiffResponse.Html.class));
    }

    @Test(expected=Response.Exception.BadRequest.class)
    public void getTraversingPath() throws IOException, ServletException {

        doReturn(project).when(run).getParent();
        doReturn(null).when(project).getBuildByNumber(1);

        when(req.getRestOfPath()).thenReturn("/1/../../../../etc/shadow");

        new ArtifactDifference(run).doDynamic(req, rsp);
    }
}
