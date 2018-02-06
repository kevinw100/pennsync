/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.pennsync.client;
import com.pennsync.client.ClientLedger;
import com.pennsync.client.CreateEvent;
import com.pennsync.client.FileSystemChanges;
import com.pennsync.client.ModifyEvent;

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.util.*;

/**
 * Example to watch a directory (or tree) for changes to files.
 */

public class WatchDir {

    private final WatchService watcher;
    private final Map<WatchKey,Path> keys;
    private boolean trace = false;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        if (trace) {
            Path prev = keys.get(key);
            if (prev == null) {
                System.out.format("register: %s\n", dir);
            } else {
                if (!dir.equals(prev)) {
                    System.out.format("update: %s -> %s\n", prev, dir);
                }
            }
        }
        keys.put(key, dir);
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start, List<Path> visited) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException
            {
                register(dir);
                visited.add(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }


    /**
     * Creates a WatchService and registers the given directory
     */
    WatchDir(Path dir) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey,Path>();

        System.out.format("Scanning %s ...\n", dir);
        registerAll(dir, new ArrayList<>());

        System.out.println("Done.");

        // enable trace after initial registration
        this.trace = true;
    }

    /**
     * Transforms a list of paths (from adding a directory/file) to make them all "created"
     * @param created
     * @return
     */
    private List<FileSystemChanges> mapToCreateEvent(List<Path> created){
        ArrayList<FileSystemChanges> changes = new ArrayList<>();
        for(Path p : created){
            changes.add(new CreateEvent(p));
        }
        return changes;
    }

    /**
     * Process all events for keys queued to the watcher
     */
    //void processEvents(Path baseDir, ClientLedger ledgerInstance) {
    void processEvents() {
        for (;;) {

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                // print out event
                //System.out.format("%s: %s\n", event.kind().name(), child);

                // Different cases for the different events
                if (kind == ENTRY_CREATE) {
                    System.out.format("%s: %s\n", event.kind().name(), child);
                    FileSystemChanges createChange = new CreateEvent(child.toAbsolutePath());
                    List<Path> createdPaths = new ArrayList<Path>();
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child, createdPaths);
                        }
                    } catch (IOException x) {
                        // ignore to keep sample readable
                    }
                    List<FileSystemChanges> createEvents = mapToCreateEvent(createdPaths);
                    createEvents.add(createChange);
                    /*
                        TODO: Make connection with the com.pennsync.server, check to see if created files are in conflict,
                        TODO: com.pennsync.server sends back set of conflicting files
                     */

                    /*
                        TODO: for each conflicting file, say that you won't be sync-ing the files, and demand a re-name (potential problem where a conflicting file won't be marked as conflicting if the program crashes before a rename)
                     */
                    //TODO: Stub this out
                    List<FileSystemChanges> nonConflicting = new ArrayList<>();


                } else if (kind == ENTRY_DELETE) {
                    System.out.format("%s: %s\n", event.kind().name(), child);
                    /*
                     * TODO: Handle file deletion in the directory
                     */

                } else if (kind == ENTRY_MODIFY) {
                    System.out.format("%s: %s\n", event.kind().name(), child);
                    /*
                     * TODO: Handle file modification in the directory
                     */
                    FileSystemChanges modifyEvent = new ModifyEvent(child.toAbsolutePath());
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

}
