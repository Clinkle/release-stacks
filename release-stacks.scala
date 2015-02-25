import scala.sys.process._
import scala.xml.Utility
import scala.util.matching.Regex

// An ordered list of the local branch names, from earliest in the release cycle to latest.
val BRANCH_NAMES = List("develop", "release/test", "release/stage", "master")

println(
  """
  <title>Release Stacks</title>
  <link rel="shortcut icon" href="https://clinkle.com/favicon.ico" />
  <style type="text/css">
    * {
      margin: 0;
      padding: 0;
    }

    html {
      margin: 10px;
    }

    h2 {
      padding: 8px 0px 2px;
      border-bottom: 1px solid #D8E6EC;
    }

    a {
      text-decoration: none;
    }

    a:hover {
      background-color: #eee;
    }

    .branch {
      display: inline-block;
      vertical-align: top;
    }

    .branch-header {
      margin: 15px 0;
      border-bottom: 1px solid;
    }

    .author-group {
      margin: 8px 0;
    }

    .commit {
      color: #111;
      display: block;
      padding: 8px 0px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .warning {
      font-size: 28px;
      color: rgb(150, 0, 0);
    }
  </style>
  """
)

case class Branch(name: String) {
  lazy val originName = s"origin/$name"
  lazy val escapedName = Utility.escape(name)
}
val branches = BRANCH_NAMES.map(Branch)
val repoURL = Seq("git", "config", "--get", "remote.origin.url").!!.trim.stripSuffix(".git").replace("git@github.com:", "https://github.com/")

case class Commit(hash: String, author: String, time: Long, message: String, branch: Branch) {
  val isFix = message.toLowerCase.contains("fix") || message.toLowerCase.contains("bug")

  private val shortStat = Seq("git", "show", hash, "--shortstat").!!.split("\n").last.trim

  private def sizeMetric(regex: Regex) = regex.findAllIn(shortStat).matchData.map(_.group(1)).toList.headOption.map(_.toInt).getOrElse(0)
  val insertions = sizeMetric("""(\d+) insertion""".r)
  val deletions = sizeMetric("""(\d+) deletion""".r)
  def size = insertions + deletions
  def insertionsRatio = insertions.toFloat / size
}

def getCommitsBetweenBranches(to: Branch, from: Branch): List[Commit] =
  Seq("git", "log", s"${from.originName}..${to.originName}", "--pretty=%H@@@%an@@@%at@@@%s").!!.split("\n").map(_.trim).filter(_.nonEmpty).map(l => {
    val Array(hash, author, time, message) = l.split("@@@")
    Commit(hash, author, time.toLong, message, to)
  }).toList

def filterOutDuplicates(listOfDifferences: List[List[Commit]]) = {
  listOfDifferences.map(_.filter(c => {
    val branchesPresent = branches.map(_.originName).zipWithIndex.filter({ case (branchOrigin, _) =>
      val cmd = Seq("git", "log", branchOrigin, s"--after=${c.time - 3600 * 24 * 60}", "--pretty=%at%an", s"--author=${c.author}") #| Seq("grep", s"${c.time}${c.author}")
      val exists = cmd.lines_!.nonEmpty
      exists
    }).map(_._2)

    val thisCommitBranch = branches.indexOf(c.branch)
    val isLatestBranch = branchesPresent.forall(_ <= thisCommitBranch) // This commit must not exist on any branches later in the pipeline than this one.

    // Add a warning if the commit skipped any branches.
    if (isLatestBranch && branchesPresent != (0 to thisCommitBranch).toList) {
      val skippedBranches = (0 to thisCommitBranch).diff(branchesPresent).map(i => branches(i))
      println(s"<div class='warning'>Warning: commit '${Utility.escape(c.message)}' is on ${c.branch.escapedName}, but skipped the branches ${skippedBranches.map(_.escapedName).mkString(" and ")}!</div>")
    }

    isLatestBranch
  }))
}

def insertionRatioToColor(iR: Float) = iR match {
  case r if r < 0.33 => "hsl(0, 100%, 32%)"
  case r if r < 0.66 => "hsl(0, 0%, 20%)"
  case _ => "hsl(93, 100%, 28%)"
}

def commitSizeToFontSize(size: Int) = (Math.log(size) + 2).toInt * 4 + 4

def buildHTMLFromCommit(commit: Commit) = s"""
  <a class='commit'
    href='$repoURL/commit/${Utility.escape(commit.hash)}'
    style='font-size: ${commitSizeToFontSize(commit.size)}px;
    color: ${insertionRatioToColor(commit.insertionsRatio)}'>
    ${Utility.escape(commit.message)}</a>
  """

val listOfDifferences = branches.sliding(2).map({ case List(to, from) => getCommitsBetweenBranches(to, from) }).toList
val branchesWithDiffs = branches.zip(filterOutDuplicates(listOfDifferences)).toMap

branchesWithDiffs.mapValues(_.groupBy(_.author).toList).foreach({ case (branch, diff) =>
  println(s"<div class='branch' style='width: ${100 / (branches.size - 1) - 1}%'>")
  println(s"<h1 class='branch-header'>${branch.escapedName}</h1>")
  diff.foreach({ case (author, commits) =>
    println("<div class='author-group'>")
    println(s"<h2>${Utility.escape(author)}</h2>")
    println(commits.sortBy(c => (-commitSizeToFontSize(c.size), c.isFix, -c.time)).map(buildHTMLFromCommit).mkString("\n"))
    println("</div>")
  })
  println("</div>")
})
