/*
 * Copyright (c) 2021, JetBrains s.r.o.. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @summary Verifies that UNIX socket doesn't throw when opened with
 *          a non-default file system installed. NB: the opened socket isn't
 *          supposed to be useable.
 * @library /test/lib
 * @build TestProvider UnixSocketInNonDefaultFS
 * @run main/othervm -Djava.nio.file.spi.DefaultFileSystemProvider=TestProvider UnixSocketInNonDefaultFS
 */
import java.nio.channels.ServerSocketChannel;
import java.net.StandardProtocolFamily;

public class UnixSocketInNonDefaultFS {
    public static void main(String args[]) throws java.io.IOException {
        ServerSocketChannel server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
    }
}
