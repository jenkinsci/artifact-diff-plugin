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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import difflib.DiffUtils;
import difflib.Patch;

/**
 * Generate FilePath diff
 *
 * @author ogondza
 */
public class FilePathDiff {

    private static final Logger LOGGER = Logger.getLogger(FilePathDiff.class.getName());

    private static final byte CONTEXT = 4;

    public List<String> getDiff(
            final Entry original, final Entry modified
    ) throws IOException {

        final Patch patch = DiffUtils.diff(original.getLines(), modified.getLines());

        return DiffUtils.generateUnifiedDiff(
                original.getPath(), modified.getPath(), original.getLines(), patch, CONTEXT
        );
    }

    /**
     * Comparison entry
     *
     * @author ogondza
     */
    public static class Entry {

        private final FilePath file;
        private final String path;

        private List<String> lines;
        private boolean missing = false;

        public Entry(final FilePath file, final String path) {

            if (file == null) throw new IllegalArgumentException("Empty FilePath");

            this.file = file;
            this.path = path == null || path.isEmpty()
                    ? this.file.getRemote()
                    : path
            ;
        }

        public String getPath() throws IOException {

            getLines();

            return missing
                    ? "/dev/null"
                    : path
            ;
        }

        public List<String> getLines() throws IOException {

            if (lines != null) return lines;

            final InputStream stream = getStream(file);

            try {

                return lines = readLines(stream);
            } finally {

                if (stream != null) {

                    stream.close();
                }
            }
        }

        private InputStream getStream(final FilePath src) {

            try {

                return src.read();
            } catch (FileNotFoundException ex) {

                return null;
            } catch (IOException ex) {

                LOGGER.info(ex.toString());
                return null;
            }
        }

        private List<String> readLines(final InputStream stream) throws IOException {

            if (stream == null) {

                this.missing = true;

                return Collections.emptyList();
            }

            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream)
            );

            final List<String> lines = new ArrayList<String>();
            String line;
            while ((line = reader.readLine()) != null) {

                lines.add(line);
            }

            return Collections.unmodifiableList(lines);
        }
    }
}
