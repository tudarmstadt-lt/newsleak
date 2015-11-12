#! /bin/bash

# Script to check wheather the code is well formatted or not.

sbt clean scalariformFormat test:scalariformFormat --warn
git diff --exit-code || (
  echo "ERROR: Scalariform check failed, see differences above."
  echo "To fix, format your sources using sbt scalariformFormat test:scalariformFormat before submitting a pull request."
  echo "Additionally, please squash your commits (eg, use git commit --amend) if you're going to update this pull request."
  false
)
