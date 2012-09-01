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
import hudson.model.Run;

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
public class GetLineClassTest {

    @Mock private Run<?, ?> run;

    private ArtifactDifference diff;

    private final String line;
    private final String className;

    @Parameterized.Parameters
    public static Collection<String[]> linesAndClasses() {
        return Arrays.asList(new String[][]{
                { "", "con" },
                { " ", "con" },
                { " void main(string[] args) {", "con" },
                { "@@ +1,1 -1,1 @@", "pos" },
                { "+", "new" },
                { "+ ", "new" },
                { "+void main(string[] args) {", "new" },
                { "-", "old" },
                { "- ", "old" },
                { "-void main(string[] args) {", "old" },
        });
    }

    public GetLineClassTest(final String line, final String className) {

        this.line = line;
        this.className = className;
    }

    @Before
    public void initMocks() {

        MockitoAnnotations.initMocks(this);
        diff = new ArtifactDifference(run);
    }

    @Test
    public void getLineClass() {

        assertThat(diff.getLineClass(line), equalTo(className));
    }
}
