/*
 * Copyright 2014 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.annotation;

import dorkbox.util.FileUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author dorkbox, llc
 */
public
class CustomClassloaderIterator implements ClassIterator {

    private final Iterator<URL> loaderFilesIterator;
    private ClassFileIterator classFileIterator;

    // have to support
    // 1 - scanning the classpath
    // 2 - scanning a specific package
    public
    CustomClassloaderIterator(List<URL> fileNames, String[] packageNames) throws IOException {
        // if ANY of our filenames DO NOT start with "box", we have to add it as a file, so our iterator picks it up (and if dir, it's
        // children)

        Set<File> files = new HashSet<File>();
        Iterator<URL> iterator = fileNames.iterator();
        while (iterator.hasNext()) {
            URL url = iterator.next();
            if (!url.getProtocol().equals("box")) {
                try {
                    File file = FileUtil.normalize(new File(url.toURI()));
                    files.add(file);
                    iterator.remove();
                } catch (URISyntaxException ex) {
                    throw new IOException(ex.getMessage());
                }
            }
        }

        if (files.isEmpty()) {
            this.classFileIterator = null;
        }
        else {
            this.classFileIterator = new ClassFileIterator(files.toArray(new File[0]), packageNames);
        }


        this.loaderFilesIterator = fileNames.iterator();
    }

    @Override
    public
    String getName() {
        // not needed
        return null;
    }

    @Override
    public
    boolean isFile() {
        if (this.classFileIterator != null) {
            return this.classFileIterator.isFile();
        }

        return false;
    }

    @Override
    public
    InputStream next(FilenameFilter filter) throws IOException {
        if (this.classFileIterator != null) {
            while (true) {
                InputStream next = this.classFileIterator.next(filter);
                if (next == null) {
                    this.classFileIterator = null;
                }
                else {
                    String name = this.classFileIterator.getName();
                    if (name.endsWith(".class")) {
                        return next;
                    }
                }
            }
        }

        if (this.loaderFilesIterator.hasNext()) {
            URL next = this.loaderFilesIterator.next();
            return next.openStream();
        }

        return null;
    }

}
