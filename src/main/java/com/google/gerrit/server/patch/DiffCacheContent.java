// Copyright (C) 2009 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.server.patch;

import org.spearce.jgit.diff.Edit;
import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.lib.AbbreviatedObjectId;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.patch.CombinedFileHeader;
import org.spearce.jgit.patch.FileHeader;
import org.spearce.jgit.patch.Patch;
import org.spearce.jgit.revwalk.RevTree;
import org.spearce.jgit.revwalk.RevWalk;
import org.spearce.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DiffCacheContent implements Serializable {
  private static final long serialVersionUID = 1L;

  public static DiffCacheContent create(final Repository db,
      final DiffCacheKey key, final FileHeader file)
      throws MissingObjectException, IncorrectObjectTypeException, IOException {
    final ObjectId o;
    final ObjectId n;

    if (file.getOldId() != null && file.getOldId().isComplete()) {
      o = toId(file.getOldId());
    } else {
      String path = key.getSourceFileName();
      if (path == null) {
        path = key.getFileName();
      }
      o = find(db, key.getOldId(), path);
    }

    if (file.getNewId() != null && file.getNewId().isComplete()) {
      n = toId(file.getNewId());
    } else {
      n = find(db, key.getNewId(), key.getFileName());
    }

    return new DiffCacheContent(file, o, n);
  }

  private static ObjectId toId(final AbbreviatedObjectId a) {
    final ObjectId o = a.toObjectId();
    return ObjectId.zeroId().equals(o) ? null : o;
  }

  static ObjectId find(final Repository db, final ObjectId treeIsh,
      final String path) throws MissingObjectException,
      IncorrectObjectTypeException, IOException {
    final RevTree tree = new RevWalk(db).parseTree(treeIsh);
    final TreeWalk tw = TreeWalk.forPath(db, path, tree);
    if (tw == null) {
      return null;
    }
    return tw.getObjectId(0);
  }

  private transient boolean noDifference;
  private transient ObjectId oldId;
  private transient ObjectId newId;
  private transient FileHeader header;
  private transient List<Edit> edits;

  public DiffCacheContent() {
    noDifference = true;
  }

  private DiffCacheContent(final FileHeader h, final ObjectId o,
      final ObjectId n) {
    noDifference = false;
    header = compact(h);
    oldId = o;
    newId = n;

    if (h instanceof CombinedFileHeader || h.getHunks().isEmpty()) {
      edits = Collections.emptyList();
    } else {
      edits = h.toEditList();
    }
  }

  public boolean isNoDifference() {
    return noDifference;
  }

  public ObjectId getOldId() {
    return oldId;
  }

  public ObjectId getNewId() {
    return newId;
  }

  public FileHeader getFileHeader() {
    return header;
  }

  public List<Edit> getEdits() {
    return edits;
  }

  private void writeObject(final ObjectOutputStream out) throws IOException {
    out.writeBoolean(noDifference);
    if (noDifference) {
      return;
    }

    writeId(out, oldId);
    writeId(out, newId);

    final int len = end(header) - header.getStartOffset();
    out.writeInt(len);
    out.write(header.getBuffer(), header.getStartOffset(), len);

    out.writeInt(edits.size());
    for (final Edit e : edits) {
      out.writeInt(e.getBeginA());
      out.writeInt(e.getEndA());
      out.writeInt(e.getBeginB());
      out.writeInt(e.getEndB());
    }
  }

  private static void writeId(final ObjectOutputStream out, final ObjectId o)
      throws IOException {
    final byte[] idBuf = new byte[Constants.OBJECT_ID_LENGTH];
    out.writeBoolean(o != null);
    if (o != null) {
      o.copyRawTo(idBuf, 0);
      out.write(idBuf);
    }
  }

  private void readObject(final ObjectInputStream in) throws IOException {
    noDifference = in.readBoolean();
    if (noDifference) {
      return;
    }

    oldId = readId(in);
    newId = readId(in);

    final byte[] buf = new byte[in.readInt()];
    in.readFully(buf);
    header = parse(buf);

    final int editCount = in.readInt();
    edits = new ArrayList<Edit>(editCount);
    for (int n = editCount - 1; 0 <= n; n--) {
      final int beginA = in.readInt();
      final int endA = in.readInt();
      final int beginB = in.readInt();
      final int endB = in.readInt();
      edits.add(new Edit(beginA, endA, beginB, endB));
    }
  }

  private static ObjectId readId(final ObjectInputStream in) throws IOException {
    if (in.readBoolean()) {
      final byte[] idBuf = new byte[Constants.OBJECT_ID_LENGTH];
      in.readFully(idBuf);
      return ObjectId.fromRaw(idBuf);
    }
    return null;
  }

  private static FileHeader parse(final byte[] buf) {
    final Patch p = new Patch();
    p.parse(buf, 0, buf.length);
    return p.getFiles().get(0);
  }

  private static FileHeader compact(final FileHeader h) {
    final int end = end(h);
    if (h.getStartOffset() == 0 && end == h.getBuffer().length) {
      return h;
    }

    final byte[] buf = new byte[end - h.getStartOffset()];
    System.arraycopy(h.getBuffer(), h.getStartOffset(), buf, 0, buf.length);
    return parse(buf);
  }

  private static int end(final FileHeader h) {
    if (h instanceof CombinedFileHeader) {
      return h.getEndOffset();
    }
    if (!h.getHunks().isEmpty()) {
      return h.getHunks().get(0).getStartOffset();
    }
    return h.getEndOffset();
  }
}
