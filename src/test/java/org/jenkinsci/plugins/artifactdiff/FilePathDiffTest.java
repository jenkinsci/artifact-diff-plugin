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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.FilePath;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FilePath.class})
public class FilePathDiffTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNoSrcFile() throws IOException {

        new FilePathDiff.Entry(null, null);
    }

    @Test
    public void absentFilePath () throws IOException {

        final FilePath filePath = mock(FilePath.class);
        when(filePath.read()).thenThrow(new IOException("Mocked exception"));

        final FilePathDiff.Entry entry = new FilePathDiff.Entry(filePath, "never.used");

        assertThat(entry.getPath(), equalTo("/dev/null"));
        assertThat(entry.getLines(), new IsEmptyCollection<String>());
    }

    @Test
    public void compareNamedAndMissing() throws IOException {

        final List<String> diffLines = new FilePathDiff().getDiff(
                new FilePathDiff.Entry(getFilePath("asdf"), "asdf.file"),
                new FilePathDiff.Entry(getFilePath(null), "empty.file")
        );

        assertThat(diffLines.get(0), equalTo("--- asdf.file"));
        assertThat(diffLines.get(1), equalTo("+++ /dev/null"));
    }

    @Test
    public void compareMissingAndNamed() throws IOException {

        final List<String> diffLines = new FilePathDiff().getDiff(
                new FilePathDiff.Entry(getFilePath(null), "empty.file"),
                new FilePathDiff.Entry(getFilePath("asdf"), "asdf.file")
        );

        assertThat(diffLines.get(0), equalTo("--- /dev/null"));
        assertThat(diffLines.get(1), equalTo("+++ asdf.file"));
    }

    @Test
    public void compareNamedAndNamed() throws IOException {

        final List<String> diffLines = new FilePathDiff().getDiff(
                new FilePathDiff.Entry(getFilePath("asdf"), "asdf.file"),
                new FilePathDiff.Entry(getFilePath("ghjk"), "ghjk.file")
        );

        assertThat(diffLines.get(0), equalTo("--- asdf.file"));
        assertThat(diffLines.get(1), equalTo("+++ ghjk.file"));
    }

    @Test
    public void compareMissingAndMissing() throws IOException {

        final List<String> diffLines = new FilePathDiff().getDiff(
                new FilePathDiff.Entry(getFilePath(null), "asdf.file"),
                new FilePathDiff.Entry(getFilePath(null), "ghjk.file")
        );

        assertThat(diffLines, new IsEmptyCollection<String>());
    }

    @Test
    public void comparison() throws IOException {

        final List<String> diffLines = new FilePathDiff().getDiff(
                new FilePathDiff.Entry(getFilePath("line one\nline 2\nline III"), "src"),
                new FilePathDiff.Entry(getFilePath("line 1\nline 2\nline 3"), "dst")
        );

        final List<String> expected = Arrays.asList(
                "--- src",
                "+++ dst",
                "@@ -1,3 +1,3 @@",
                "-line one",
                "+line 1",
                " line 2",
                "-line III",
                "+line 3"
        );

        assertThat(diffLines, equalTo(expected));
    }

    @Test
    public void useFilePath() throws IOException {

        final FilePath nullPath = getFilePath("line one");
        final FilePath emptyPath = getFilePath("line 1");

        when(nullPath.getRemote()).thenReturn("/remote/null");
        when(emptyPath.getRemote()).thenReturn("/remote/empty");

        final List<String> diffLines = new FilePathDiff().getDiff(
                new FilePathDiff.Entry(nullPath, null),
                new FilePathDiff.Entry(emptyPath, "")
        );

        assertThat(diffLines.get(0), equalTo("--- /remote/null"));
        assertThat(diffLines.get(1), equalTo("+++ /remote/empty"));
    }

    private FilePath getFilePath(final String source) throws IOException {

        final InputStream sourceStream = source == null
                ? null
                : new ByteArrayInputStream(source.getBytes())
        ;


        final FilePath filePath = mock(FilePath.class);
        when(filePath.read()).thenReturn(sourceStream);

        return filePath;
    }
}
