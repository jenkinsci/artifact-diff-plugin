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

import hudson.Extension;
import hudson.model.Action;
import hudson.model.TransientBuildActionFactory;
import hudson.model.Run;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;

import jenkins.model.ModelObjectWithContextMenu.ContextMenu;

import org.jenkinsci.plugins.artifactdiff.Response.Exception;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Artifact difference action
 *
 * @author ogondza
 */
public class ArtifactDifference implements Action {

    private static final List<String> allowedCommands = Arrays.asList("plain", "html");

    private final Run<?, ?> lhsRun;

    public ArtifactDifference(final Run<?, ?> run) {

        if (run == null) throw new IllegalArgumentException("Empty run provided");

        this.lhsRun = run;
    }

    public String getDisplayName() {

        return getTitle();
    }

    public String getTitle() {

        return "Artifact diff";
    }

    public String getIconFileName() {

        return "document.png";
    }

    public String getUrlName() {

        return "artifact-diff";
    }

    public Run<?, ?> getOwner() {

        return lhsRun;
    }

    /**
     * Do not let doDynamic handle "contextMenu"
     */
    public ContextMenu doContextMenu() {

        return null;
    }

    public String getFilename(
            final Run<?, ?>.Artifact artifact, final String artifactDir
    ) throws IOException {

        final String prefix = artifactDir.endsWith("/")
                ? artifactDir
                : artifactDir + "/"
        ;

        final String artifactPath = artifact.getFile().getCanonicalPath();

        assert artifact.getFile().getCanonicalFile().toPath().startsWith(prefix);

        return artifactPath.substring(prefix.length());
    }

    public Response doDynamic(
            final StaplerRequest req,
            final StaplerResponse rsp
    ) throws IOException, ServletException {

        if (req.getRestOfPath().isEmpty()) return new Response.ArtifactList(this);

        return getCommand(req).equals("plain")
                ? new DiffResponse.Plain(this, req, rsp)
                : new DiffResponse.Html(this, req, rsp)
        ;
    }

    public void serve(
            final StaplerRequest req,
            final StaplerResponse rsp,
            final Object node
    ) throws IOException, ServletException {

        try {

            doDynamic(req, rsp).generateResponse(req, rsp, node);
        } catch (Response.Exception ex) {

            ex.send(rsp);
        }
    }

    private String getCommand(final StaplerRequest req) throws Exception {

        final String commandCandidate = req.getParameter("output");

        if (allowedCommands.contains(commandCandidate)) return commandCandidate;

        return "html";
    }

    /**
     * Create action for every build having artifacts
     *
     * @author ogondza
     */
    @Extension
    public static final class ActionFactory extends TransientBuildActionFactory {

        @Override
        public Collection<? extends Action> createFor(final Run target) {

            if (!target.getHasArtifacts()) return Collections.emptyList();

            return Arrays.asList(
                    new ArtifactDifference(target)
            );
        }
    }
}
