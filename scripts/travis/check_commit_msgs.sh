#!/bin/bash

# Script to check the commit messages in the range of commits travis is testing.
# This is heavily based on:
# https://github.com/JensRantil/angular.js/blob/ffe93bb368037049820ac05ef62f8cc7ed379d98/test-commit-msgs.sh

# Check for either a commit or a range (which apparently isn't always a range) and fix as needed.
if [ "$#" -gt 0 ]; then
	RANGE=$1
elif [ "$TRAVIS_COMMIT_RANGE" != "" ]; then
	RANGE=$TRAVIS_COMMIT_RANGE
elif [ "$TRAVIS_COMMIT" != "" ]; then
	RANGE=$TRAVIS_COMMIT
fi


if [ "$RANGE" == "" ]; then
	echo -n "RANGE is empty!"
	exit 1
fi

# Travis sends the ranges with 3 dots. Git only wants 2.
if [[ "$RANGE" == *...* ]]; then
	RANGE=`echo $TRAVIS_COMMIT_RANGE | sed 's/\.\.\./../'`
elif [[ "$RANGE" != *..* ]]; then
	RANGE="$RANGE~..$RANGE"
fi


for sha in `git log --format=oneline "$RANGE" | cut '-d ' -f1`
do
    echo -n "Checking commit message for $sha..."
    git rev-list --format=%B --max-count=1 $sha|awk '
    NR == 2 && !/^(feat|fix|docs|style|refactor|test|chore|perf)\([^)]+\): .+/ {
        print "Incorrect, or no, commit type in subject line. Valid"
        print "types are:"
        print ""
        print " * feat"
        print " * fix"
        print " * docs"
        print " * style"
        print " * refactor"
        print " * test"
        print " * chore"
		print " * perf"
        print ""
        print "Line:"
        print $0
        exit 1
    }
    NR == 2 && /.*\.$/ {
        print "Subject must not end with a period."
        exit 1
    }
    NR == 2 && length($0) > 70 {
        print "Subject line too long."
        exit 1
    }
    NR == 3 && length($0) > 0 {
        print "Second commit message line must be empty."
        exit 1
    }
    NR > 3 && length($0) > 80 {
        print "Too long commit line (>80 characters) in message:"
        print $0
        exit 1
    }
    '
    EXITCODE=$?
    if [ $EXITCODE -ne 0 ]; then
        echo "FAILED."
        echo
        echo "Commit message for $sha is not following commit"
        echo "guidelines. Please see:"
        echo
        echo "https://docs.google.com/document/d/1QrDFcIiPjSLDn3EL15IJygNPiHORgU1_OOAqWjiDU5Y"
        exit $EXITCODE
    else
        echo "OK."
    fi
done
