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
package org.jenkinsci.plugins.artifactdiff;

import hudson.model.Job;
import hudson.model.Run;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Artifact response
 *
 * @author ogondza
 */
public abstract class Response implements HttpResponse {

    /**
     * List artifacts
     *
     * @author ogondza
     */
    public static final class ArtifactList extends Response {

        private final ArtifactDifference diff;

        public ArtifactList(final ArtifactDifference diff) throws IOException, Exception {

            this.diff = diff;
        }

        public void generateResponse(
                final StaplerRequest req,
                final StaplerResponse rsp,
                final Object node
        ) throws IOException, ServletException {

            req.setAttribute("build", diff.getOwner());
            req.setAttribute("representatives", getRepresentativeBuilds());
            req.getView(diff, "list.jelly").forward(req, rsp);
        }

        private Map<String, Run<?, ?>> getRepresentativeBuilds() {

            final Job<?, ?> project = diff.getOwner().getParent();

            final Map<String, Run<?, ?>> c = new HashMap<String, Run<?, ?>>(5);
            c.put("successful", project.getLastSuccessfulBuild());
            c.put("stable", project.getLastStableBuild());
            c.put("unstable", project.getLastUnstableBuild());
            c.put("unsuccessful", project.getLastUnsuccessfulBuild());
            c.put("failed", project.getLastFailedBuild());

            final Map<String, Run<?, ?>> r = new HashMap<String, Run<?, ?>>(5);
            for (final Map.Entry<String, Run<?, ?>> entry: c.entrySet()) {

                if (entry.getValue() == null) continue;
                if (entry.getValue().equals(diff.getOwner())) continue;

                r.put(entry.getKey(), entry.getValue());
            }

            return r;
        }
    }

    public abstract static class Exception extends ServletException {

        private final int code;

        public Exception(final String msg, final int code) {

            super(msg);

            this.code = code;
        }

        public final void send(final StaplerResponse rsp) throws IOException {

            rsp.sendError(this.code, this.getMessage());
        }

        public static class NotFound extends Exception {

            public NotFound(final String msg) {

                super(msg, HttpServletResponse.SC_NOT_FOUND);
            }
        }

        public static class BadRequest extends Exception {

            public BadRequest(final String msg) {

                super(msg, HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }
}
