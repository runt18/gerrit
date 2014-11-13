// Copyright (C) 2014 The Android Open Source Project
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

package com.google.gerrit.server.change;

import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;

@Singleton
public class GetContentType implements RestReadView<FileResource> {
  private final FileContentUtil fileContentUtil;

  @Inject
  GetContentType(FileContentUtil fileContentUtil) {
    this.fileContentUtil = fileContentUtil;
  }

  @Override
  public String apply(FileResource rsrc)
      throws ResourceNotFoundException, IOException {
    return fileContentUtil.getContentType(
        rsrc.getRevision().getControl().getProject().getNameKey(),
        rsrc.getRevision().getPatchSet().getRevision().get(),
        rsrc.getPatchKey().get());
  }
}
