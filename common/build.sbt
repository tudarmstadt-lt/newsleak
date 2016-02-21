import divid.{ DividBuild, Dependencies }

Dependencies.commonDeps

assemblyMergeStrategy in assembly := {
  case "META-INF/MANIFEST.MF" => MergeStrategy.rename
  case x => {
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    if (oldStrategy(x) == MergeStrategy.deduplicate) MergeStrategy.first else oldStrategy(x)
  }
}