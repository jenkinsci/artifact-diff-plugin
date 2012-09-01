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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.Run;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.jenkinsci.plugins.artifactdiff.ArtifactDifference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(Parameterized.class)
public class GetFilenameTest {

    @Mock private Run<?, ?> run;

    private ArtifactDifference diff;

    private final Run<?, ?>.Artifact artifact;
    private final String artifactDir;
    private final String expected;

    @Parameterized.Parameters
    public static Collection<Object[]> linesAndClasses() throws IOException {

        return Arrays.asList(new Object[][]{
                { getArtifact("/an/artifact/path"), "/", "an/artifact/path" },
                { getArtifact("/an/artifact/path"), "/an", "artifact/path" },
                { getArtifact("/an/artifact/path"), "/an/", "artifact/path" },
                { getArtifact("/an/artifact/path"), "/an/artifact", "path" },
                { getArtifact("/an/artifact/path"), "/an/artifact/", "path" }
        });
    }

    private static Run<?, ?>.Artifact getArtifact(final String path) throws IOException {

        final Run<?, ?>.Artifact artifact = mock(Run.Artifact.class);
        final File file = mock(File.class);

        when(file.getCanonicalPath()).thenReturn(path);
        when(artifact.getFile()).thenReturn(file);

        return artifact;
    }

    public GetFilenameTest(
            final Run<?, ?>.Artifact artifact, final String artifactDir, final String expected
    ) {

        this.artifact = artifact;
        this.artifactDir = artifactDir;
        this.expected = expected;
    }

    @Before
    public void initMocks() {

        MockitoAnnotations.initMocks(this);
        diff = new ArtifactDifference(run);
    }

    @Test
    public void getLineClass() throws IOException {

        assertThat(diff.getFilename(artifact, artifactDir), equalTo(expected));
    }
}
