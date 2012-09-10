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

import hudson.FilePath;
import hudson.model.Run;
import hudson.util.RunList;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Generate artifact difference diff response
 *
 * @author ogondza
 */
public abstract class DiffResponse extends Response {

    private static final String MALFORMED_URL = "Malformed url";

    private static final String ILLEGAL_FILE_PATH = "Illegal file path";

    private static final Pattern urlPattern = Pattern.compile(
            "^/(\\d+)/(.*)$"
    );

    protected final StaplerRequest req;
    protected final StaplerResponse rsp;

    protected final String path;
    protected final Run<?, ?> lhsRun;
    protected final Run<?, ?> rhsRun;
    protected final ArtifactDifference diff;

    public DiffResponse(
            final ArtifactDifference diff,
            final StaplerRequest req,
            final StaplerResponse rsp
    ) throws IOException, ServletException {

        this.req = req;
        this.rsp = rsp;

        Matcher matcher = urlPattern.matcher(req.getRestOfPath());

        if (!matcher.matches()) throw new Response.Exception.BadRequest(MALFORMED_URL);

        this.path = getPath(matcher);
        this.lhsRun = diff.getOwner();
        this.rhsRun = getRhsRun(matcher);
        this.diff = diff;
    }

    private Run<?, ?> getRhsRun(final Matcher matcher) throws ServletException {

        final int rhsNumber = Integer.parseInt(matcher.group(1));

        final Run<?, ?> rhsRun = lhsRun.getParent()
                .getBuildByNumber(rhsNumber)
        ;

        if (rhsRun != null) return rhsRun;

        throw new Response.Exception.NotFound("No such build");
    }

    private String getPath(final Matcher matcher) throws ServletException {

        final String path = matcher.group(2);

        if (path.contains("../")) {

            throw new Response.Exception.BadRequest(ILLEGAL_FILE_PATH);
        }

        return path;
    }

    protected List<String> calculateDiff(
            final Run<?, ?> lhsRun,
            final Run<?, ?> rhsRun,
            final String path,
            final StaplerResponse rsp
    ) throws IOException {

        final FilePath lhsFile = new FilePath(lhsRun.getArtifactsDir()).child(path);
        final FilePath rhsFile = new FilePath(rhsRun.getArtifactsDir()).child(path);
        final String lhsPath = getPath(lhsRun, path);
        final String rhsPath = getPath(rhsRun, path);

        return new FilePathDiff().getDiff(
                new FilePathDiff.Entry(lhsFile, lhsPath),
                new FilePathDiff.Entry(rhsFile, rhsPath)
        );
    }

    private String getPath(final Run<?, ?> run, final String path) {

        return String.format("%s/%s", run.getNumber(), path);
    }

    public void generateResponse(
            final StaplerRequest req,
            final StaplerResponse rsp,
            final Object node
    ) throws IOException, ServletException {

        rsp.setCharacterEncoding("UTF-8");

        generate(calculateDiff(lhsRun, rhsRun, path, rsp));
    }

    abstract void generate(final List<String> diff) throws IOException, ServletException;

    /**
     * Plain text response
     *
     * @author ogondza
     */
    public static class Plain extends DiffResponse {

        public Plain(
                final ArtifactDifference diff,
                final StaplerRequest req,
                final StaplerResponse rsp
        ) throws IOException, ServletException {

            super(diff, req, rsp);
        }

        public void generate(final List<String> diff) throws IOException, ServletException {

            rsp.setContentType("text/plain");

            try {

                rsp.getWriter().print(StringUtils.join(diff, "\n"));
            } catch(FileNotFoundException ex) {

                rsp.setContentType("text/html");
                throw new Exception.NotFound("File not found: " + ex.getMessage());
            }
        }
    }

    /**
     * Html Response
     *
     * @author ogondza
     */
    public static class Html extends DiffResponse {

        private static final Map<String, String> decorators = new HashMap<String, String>();
        static {
            decorators.put("+", "new");
            decorators.put("-", "old");
            decorators.put("@@", "pos");
            // Match context lines. Everything else is supposed to be matched by previous patterns
            decorators.put(" ", "con");
        }

        public Html(
                final ArtifactDifference diff,
                final StaplerRequest req,
                final StaplerResponse rsp
        ) throws IOException, ServletException {

            super(diff, req, rsp);
        }

        public void generate(final List<String> lines) throws IOException, ServletException {

            handleRequest();

            req.setAttribute("lhs", lhsRun);
            req.setAttribute("rhs", rhsRun);
            req.setAttribute("buildList", getRelevantBuilds(rhsRun));
            req.setAttribute("diff", lines);
            req.setAttribute("outcome", this);
            req.setAttribute("path", path);
            req.getView(diff,"html.jelly").forward(req, rsp);
        }

        public String getLineClass(final String line) {

            if (line.isEmpty()) return "con";

            for (final Entry<String, String> dec: decorators.entrySet()) {

                if (line.startsWith(dec.getKey())) return dec.getValue();
            }

            throw new IllegalArgumentException(line + " does not look like a diff line");
        }

        /**
         * Get build worth comparing
         *
         * <p>Builds that has an artifact on current filepath
         */
        private RunList<?> getRelevantBuilds(Run<?, ?> run) throws IOException {

            final String artifactDir = run.getArtifactsDir().getCanonicalPath();

            final RunList<?> currentBuilds = run.getParent().getBuilds();
            final RunList<Run<?, ?>> relevantBuilds = new RunList<Run<?, ?>>();
            for (final Run<?, ?> build: currentBuilds) {

                if (hasArtifact(build, artifactDir) || build.equals(run)) {

                  relevantBuilds.add(build);
                }
            }

            return relevantBuilds;
        }

        private boolean hasArtifact(
                final Run<?, ?> build, final String artifactDir
        ) throws IOException {

            for(final Run<?, ?>.Artifact artifact: build.getArtifacts()) {

                if (diff.getFilename(artifact, artifactDir).equals(path)) return true;
            }

            return false;
        }

        private void handleRequest() throws IOException {

            final int lhsNumber = getNumber("lhs", lhsRun);
            final int rhsNumber = getNumber("rhs", rhsRun);

            final String oldUrl = getUrl(lhsRun.getNumber(), rhsRun.getNumber());
            final String newUrl = getUrl(lhsNumber, rhsNumber);

            if (!oldUrl.equals(newUrl)) {

                rsp.sendRedirect(getRedirectUrl(newUrl));
            }
        }

        private int getNumber(final String param, Run<?, ?> run) {

            try {

                return Integer.parseInt(req.getParameter(param));
            } catch (NumberFormatException ex) {

                return run.getNumber();
            }
        }

        private String getUrl(final int lhsNumber, final int rhsNumber) {

            return String.format("%s/artifact-diff/%s/", lhsNumber, rhsNumber);
        }

        private String getRedirectUrl(final String newUrl) {

            return String.format("%s%s%s%s?output=html",
                    Jenkins.getInstance().getRootUrl(),
                    lhsRun.getParent().getUrl(),
                    newUrl,
                    path
            );
        }
    }
}
