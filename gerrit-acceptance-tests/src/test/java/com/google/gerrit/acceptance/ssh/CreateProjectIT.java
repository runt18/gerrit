// Copyright (C) 2015 The Android Open Source Project
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

package com.google.gerrit.acceptance.ssh;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assert_;

import com.google.gerrit.acceptance.AbstractDaemonTest;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.project.ProjectState;

import org.junit.Test;

public class CreateProjectIT extends AbstractDaemonTest {

  @Test
  public void withValidGroupName() throws Exception {
    String newGroupName = "newGroup";
    adminSession.put("/groups/" + newGroupName);
    String newProjectName = "newProject";
    sshSession.exec("gerrit create-project --branch master --owner "
        + newGroupName + " " + newProjectName);
    assert_().withFailureMessage(sshSession.getError())
        .that(sshSession.hasError()).isFalse();
    ProjectState projectState =
        projectCache.get(new Project.NameKey(newProjectName));
    assertThat(projectState).isNotNull();
  }

  @Test
  public void withInvalidGroupName() throws Exception {
    String newGroupName = "newGroup";
    adminSession.put("/groups/" + newGroupName);
    String wrongGroupName = "newG";
    String newProjectName = "newProject";
    sshSession.exec("gerrit create-project --branch master --owner "
        + wrongGroupName + " " + newProjectName);
    assert_().withFailureMessage(sshSession.getError())
        .that(sshSession.hasError()).isTrue();
    ProjectState projectState =
        projectCache.get(new Project.NameKey(newProjectName));
    assertThat(projectState).isNull();
  }
}
