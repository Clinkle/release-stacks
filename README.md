Release Stacks
==============

### Pierce through merges, cherry picks, and other nonsense

Release Stacks is a single file Scala script that helps you organize your [git workflow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow/). When you move commits from one branch to the next, release stacks tells you exactly which commits are changing places.

<style type="text/css">
  .stacks {
    padding: 10px;
    border: 1px solid #D8E6EC;
  }

  .stacks * {
    margin: 0;
    padding: 0;
  }

  .stacks html {
    margin: 10px;
  }

  .stacks h2 {
    padding: 8px 0px;
  }

  .stacks a {
    color: #111;
    display: block;
    padding: 4px 0px;
    overflow: hidden;
    text-decoration: none;
    text-overflow: ellipsis;
    text-shadow: 0px 1px 1px rgba(0, 0, 0, 0.1);
    white-space: nowrap;
  }

  .stacks a:hover {
    background-color: #eee;
  }

  .stacks .warning {
    font-size: 28px;
    color: rgb(150, 0, 0);
  }
</style>

<div class='stacks'>
<div style='width: 49%; display: inline-block; vertical-align: top;'>
  <h1 style='border-bottom: 1px solid;'>develop</h1>
  <div>
    <h2>Alex Quach</h2>

    <a href=''
      style='font-size: 18px;
      color: hsl(0, 0%, 20%)'>
      Fix weird bug. Closes #123.</a>

    <a href=''
      style='font-size: 22px;
      color: hsl(0, 100%, 32%)'>
      Remove dead code.</a>
  </div>
  <div>
    <h2>Mitch Lee</h2>

    <a href=''
      style='font-size: 28px;
      color: hsl(93, 100%, 28%)'>
      Add that feature we wanted.</a>
  </div>
</div>

<div style='width: 49%; display: inline-block; vertical-align: top;'>
  <h1 style='border-bottom: 1px solid;'>release</h1>

  <div>
    <h2>Mitch Lee</h2>

    <a href=''
      style='font-size: 14px;
      color: hsl(0, 0%, 20%)'>
      Update some copy.</a>
  </div>
</div>
</div>

Sample Workflow
---------------

At Clinkle, we prefer to maintain a clean, linear history by using `reset --hard` and `cherry-pick` to keep our branches organized. Here is a more traditional view of the same history from above:

![flow](https://cloud.githubusercontent.com/assets/3643059/6359747/198a634a-bc2a-11e4-907b-1358e23f8713.png)

Each branch is a linear extension of the branches ahead of it. If we need commits on `release` or `master` before we are ready to cut a branch (e.g. for a hotfix), we commit to `develop` first and then proceed to `cherry-pick` the commit forward as needed. As described in the Features section below, Release Stacks will warn you if a commit ever skips a branch.

Features
--------

Each branch displays the commits (grouped by author) that *are no farther in the workflow than that branch*. For example, Alex's commit "Remove dead code." is on the `develop` branch, but has not made its way to `release` or `master` yet. Likewise, "Update some copy." is on `develop` and `release`, but has not made it to `master` yet.

Each commit links to its corresponding page on github. The font **size** is calculated based on the number of lines changed in the commit &mdash; larger commits are represented by larger font sizes. The font **color** is calculated based on the insertion to deletion ratio &mdash; commits that remove lots of code are red, commits that add lots of code are green, and insertion neutral commits are black.

One helpful feature of Release Stacks is to warn you when a commit misses a branch. For example, suppose Alex commits "Hotfix: Unbreak all of the things." straight to master. Release stacks would then show the warning:

```
Warning: commit 'Hotfix: Unbreak all of the things.' is on master, but skipped the branches release and develop!
```

In this way, you can have confidence that your develop branch is strictly ahead of each subsequent branch in your workflow.

Usage
-----

1. Update the branch names.

  At the top of `release-stacks.scala`, replace the line
  ```scala
  val BRANCH_NAMES = List("develop", "release", "master")
  ```
  with your workflow branch names. Each branch should have a corresponding `origin/$branch` on your remote repository.

2. Navigate to your git directory of choice.
  ```bash
  >> cd ~/code/example
  >> git branch
  * develop
    master
    release
  ```

3. Run the script.
  ```bash
  >> scala ~/code/release-stacks/release-stacks.scala > workflow.html
  ```

4. Open the file.
  ```bash
  >> open workflow.html
  ```

5.

![happiness](https://cloud.githubusercontent.com/assets/3643059/6359809/720336a0-bc2a-11e4-8a02-18baf6c13db7.jpg)
