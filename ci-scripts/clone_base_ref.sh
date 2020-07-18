#!/bin/bash
# Checkout pull request base commit for use in validate_pr_changes.py

set +e

echo "Trying to fetch remote repo"
# Try to pull as much of the repo as we can
git fetch origin --unshallow
git fetch origin --recurse-submodules=no
git fetch origin

set -e

repodir="$(basename "$PWD")"
cd ..

echo "Copy repo to ../base"
cp -a "$repodir" "base"

echo "Checkout base reference: $GITHUB_BASE_REF"
cd base
git checkout -b "origin/$GITHUB_BASE_REF"

cd "../$repodir"
