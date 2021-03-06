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

package com.google.gerrit.server.notedb;

import static org.junit.Assert.fail;

import com.google.gerrit.common.TimeUtil;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ChangeNotesParserTest extends AbstractChangeNotesTest {
  private TestRepository<InMemoryRepository> testRepo;
  private RevWalk walk;

  @Before
  public void setUpTestRepo() throws Exception {
    testRepo = new TestRepository<>(repo);
    walk = new RevWalk(repo);
  }

  @After
  public void tearDownTestRepo() throws Exception {
    walk.close();
  }

  @Test
  public void parseAuthor() throws Exception {
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Patch-Set: 1\n"
        + "Subject: This is a test change\n");
    assertParseFails(writeCommit("Update change\n"
        + "\n"
        + "Patch-Set: 1\n",
        new PersonIdent("Change Owner", "owner@example.com",
          serverIdent.getWhen(), serverIdent.getTimeZone())));
    assertParseFails(writeCommit("Update change\n"
        + "\n"
        + "Patch-Set: 1\n",
        new PersonIdent("Change Owner", "x@gerrit",
          serverIdent.getWhen(), serverIdent.getTimeZone())));
  }

  @Test
  public void parseStatus() throws Exception {
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Patch-Set: 1\n"
        + "Status: NEW\n"
        + "Subject: This is a test change\n");
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Patch-Set: 1\n"
        + "Status: new\n"
        + "Subject: This is a test change\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Status: OOPS\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Status: NEW\n"
        + "Status: NEW\n");
  }

  @Test
  public void parsePatchSetId() throws Exception {
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Patch-Set: 1\n"
        + "Subject: This is a test change\n");
    assertParseFails("Update change\n"
        + "\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Patch-Set: 1\n");
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Patch-Set: 1\n"
        + "Subject: This is a test change\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: x\n");
  }

  @Test
  public void parseApproval() throws Exception {
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Patch-Set: 1\n"
        + "Label: Label1=+1\n"
        + "Label: Label2=1\n"
        + "Label: Label3=0\n"
        + "Label: Label4=-1\n"
        + "Subject: This is a test change\n");
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Patch-Set: 1\n"
        + "Label: -Label1\n"
        + "Label: -Label1 Other Account <2@gerrit>\n"
        + "Subject: This is a test change\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Label: Label1=X\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Label: Label1 = 1\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Label: X+Y\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Label: Label1 Other Account <2@gerrit>\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Label: -Label!1\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Label: -Label!1 Other Account <2@gerrit>\n");
  }

  @Test
  public void parseSubmitRecords() throws Exception {
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Patch-Set: 1\n"
        + "Subject: This is a test change\n"
        + "Submitted-with: NOT_READY\n"
        + "Submitted-with: OK: Verified: Change Owner <1@gerrit>\n"
        + "Submitted-with: NEED: Code-Review\n"
        + "Submitted-with: NOT_READY\n"
        + "Submitted-with: OK: Verified: Change Owner <1@gerrit>\n"
        + "Submitted-with: NEED: Alternative-Code-Review\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Submitted-with: OOPS\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Submitted-with: NEED: X+Y\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Submitted-with: OK: X+Y: Change Owner <1@gerrit>\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Submitted-with: OK: Code-Review: 1@gerrit\n");
  }

  @Test
  public void parseSubmissionId() throws Exception {
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Patch-Set: 1\n"
        + "Subject: This is a test change\n"
        + "Submission-id: 1-1453387607626-96fabc25");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Submission-id: 1-1453387607626-96fabc25\n"
        + "Submission-id: 1-1453387901516-5d1e2450");
  }

  @Test
  public void parseReviewer() throws Exception {
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Patch-Set: 1\n"
        + "Reviewer: Change Owner <1@gerrit>\n"
        + "CC: Other Account <2@gerrit>\n"
        + "Subject: This is a test change\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Reviewer: 1@gerrit\n");
  }

  @Test
  public void parseTopic() throws Exception {
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Patch-Set: 1\n"
        + "Topic: Some Topic\n"
        + "Subject: This is a test change\n");
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Patch-Set: 1\n"
        + "Topic:\n"
        + "Subject: This is a test change\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Topic: Some Topic\n"
        + "Topic: Other Topic");
  }

  @Test
  public void parseBranch() throws Exception {
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Patch-Set: 1\n"
        + "Subject: This is a test change\n");
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Branch: master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Patch-Set: 1\n"
        + "Subject: This is a test change\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Branch: refs/heads/master\n"
        + "Branch: refs/heads/stable");
  }

  @Test
  public void parseChangeId() throws Exception {
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Patch-Set: 1\n"
        + "Subject: This is a test change\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Change-id: I159532ef4844d7c18f7f3fd37a0b275590d41b1b");
  }

  @Test
  public void parseSubject() throws Exception {
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Subject: Some subject of a change\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-Set: 1\n"
        + "Subject: Some subject of a change\n"
        + "Subject: Some other subject\n");
  }

  @Test
  public void parseCommit() throws Exception {
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Patch-set: 1\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Subject: Some subject of a change\n"
        + "Commit: abcd1234abcd1234abcd1234abcd1234abcd1234");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-set: 1\n"
        + "Branch: refs/heads/master\n"
        + "Subject: Some subject of a change\n"
        + "Commit: abcd1234abcd1234abcd1234abcd1234abcd1234\n"
        + "Commit: deadbeefdeadbeefdeadbeefdeadbeefdeadbeef");
    assertParseFails("Update patch set 1\n"
        + "Uploaded patch set 1.\n"
        + "Patch-set: 1\n"
        + "Branch: refs/heads/master\n"
        + "Subject: Some subject of a change\n"
        + "Commit: beef");
  }

  @Test
  public void parsePatchSetState() throws Exception {
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Patch-set: 1 (PUBLISHED)\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Subject: Some subject of a change\n");
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Patch-set: 1 (DRAFT)\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Subject: Some subject of a change\n");
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Patch-set: 1 (DELETED)\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Subject: Some subject of a change\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-set: 1 (NOT A STATUS)\n"
        + "Branch: refs/heads/master\n"
        + "Subject: Some subject of a change\n");
  }

  @Test
  public void parsePatchSetGroups() throws Exception {
    assertParseSucceeds("Update change\n"
        + "\n"
        + "Patch-set: 1\n"
        + "Branch: refs/heads/master\n"
        + "Change-id: I577fb248e474018276351785930358ec0450e9f7\n"
        + "Commit: abcd1234abcd1234abcd1234abcd1234abcd1234\n"
        + "Subject: Change subject\n"
        + "Groups: a,b,c\n");
    // No patch set commit parsed on which we can set groups.
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-set: 1\n"
        + "Branch: refs/heads/master\n"
        + "Subject: Change subject\n"
        + "Groups: a,b,c\n");
    assertParseFails("Update change\n"
        + "\n"
        + "Patch-set: 1\n"
        + "Branch: refs/heads/master\n"
        + "Commit: abcd1234abcd1234abcd1234abcd1234abcd1234\n"
        + "Subject: Change subject\n"
        + "Groups: a,b,c\n"
        + "Groups: d,e,f\n");
  }

  private RevCommit writeCommit(String body) throws Exception {
    return writeCommit(body, ChangeNoteUtil.newIdent(
        changeOwner.getAccount(), TimeUtil.nowTs(), serverIdent,
        "Anonymous Coward"));
  }

  private RevCommit writeCommit(String body, PersonIdent author)
      throws Exception {
    try (ObjectInserter ins = testRepo.getRepository().newObjectInserter()) {
      CommitBuilder cb = new CommitBuilder();
      cb.setAuthor(author);
      cb.setCommitter(new PersonIdent(serverIdent, author.getWhen()));
      cb.setTreeId(testRepo.tree());
      cb.setMessage(body);
      ObjectId id = ins.insert(cb);
      ins.flush();
      RevCommit commit = walk.parseCommit(id);
      walk.parseBody(commit);
      return commit;
    }
  }

  private void assertParseSucceeds(String body) throws Exception {
    try (ChangeNotesParser parser = newParser(writeCommit(body))) {
      parser.parseAll();
    }
  }

  private void assertParseFails(String body) throws Exception {
    assertParseFails(writeCommit(body));
  }

  private void assertParseFails(RevCommit commit) throws Exception {
    try (ChangeNotesParser parser = newParser(commit)) {
      parser.parseAll();
      fail("Expected parse to fail:\n" + commit.getFullMessage());
    } catch (ConfigInvalidException e) {
      // Expected
    }
  }

  private ChangeNotesParser newParser(ObjectId tip) throws Exception {
    return new ChangeNotesParser(newChange(), tip, walk, repoManager);
  }
}
