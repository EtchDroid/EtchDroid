#!/bin/bash
# Checkout pull request base commit for use in validate_pr_changes.py

set -e

echo "Fetch remote repo"
git fetch origin

repodir="$(basename "$PWD")"
cd ..

echo "Copy repo to ../base"
cp -a "$repodir" "base"

echo "Checkout base reference: $GITHUB_BASE_REF"
cd base
git checkout -b "origin/$GITHUB_BASE_REF"

cd "../$repodir"
