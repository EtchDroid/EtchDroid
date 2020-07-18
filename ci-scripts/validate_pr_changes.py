#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Checks pull request quality.
Pull requests that are mostly refactors will (hopefully) be detected and rejected.
"""

import os
import re
import subprocess

from os.path import dirname, basename

GITHUB_BASE_REMOTE = os.environ.get('GITHUB_BASE_REMOTE', 'origin')
GITHUB_BASE_REF = f"{GITHUB_BASE_REMOTE}/{os.environ['GITHUB_BASE_REF']}"
BASE_CHECKOUT = "../base"
IGNORED_USERS = ['weblate']
TRANSLATE_URL = 'https://hosted.weblate.org/projects/etchdroid/app/{lang}/'
MAX_FILES_CHANGED = 15
BLOAT_CHANGES_TOLERANCE_FACTOR = 2.5

INCIDENT_EXPLAINATIONS = {
    'too_many_files':
        "- Too many files:\n"
        "You have changed a lot of files. Pull requests must be simple, with each one contributing a minimal set of\n"
        "changes. Each pull request should perform just one conceptual change, such as 'Fixed bug which caused the\n"
        "program to...' or 'Introduce feature X'.",
    'bloated':
        "- Too many non-code changes:\n"
        "We detected that, in some files, it's quite likely all you did was run 'Reformat file' on Android Studio, or\n"
        "something similar. While order is appreciated, this kind of changes has a very high chance of breaking\n"
        "stuff when merging branches around.\n"
        "Also, they're extremely boring to review and the maintainer would rather spend time doing better activities.\n"
        "If this is the case, please address this, there's a high chance your PR won't be looked at.\n"
        "If you think your changes are actually meaningful and well written, please leave a comment about it.",
    "bypass_weblate":
        "- Translation wasn't performed on Weblate:\n"
        "Translations for EtchDroid MUST be contributed using Weblate: https://hosted.weblate.org/engage/etchdroid/\n"
        "Translations submitted as pull requests won't be accepted any more since they bypass all checks in place in \n"
        "Weblate",
    'unknown':
        "- An unknown error has occurred while performing one or more checks"
}

_insertions_regex = re.compile(r"(\d+) insertion[s]?")
_deletions_regex = re.compile(r"(\d+) deletion[s]?")


class BloatedPullRequest(Exception):
    pass


class BypassedWeblate(Exception):
    pass


def get_changed_files():
    # Sample output:
    #  .github/workflows/build_test.yml       | 54 ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    #  .travis.yml                            | 39 ---------------------------------------
    #  app/src/main/res/values-tr/strings.xml |  2 +-
    #  ci-scripts/.gitignore                  |  2 ++
    #  ci-scripts/setup_sdk.sh                | 34 ++++++++++++++++++++++++++++++++++
    #  ci-scripts/vars.sh                     |  2 ++
    #  6 files changed, 93 insertions(+), 40 deletions(-)
    out = subprocess.check_output(['git', 'diff', '--stat', GITHUB_BASE_REF]).decode(errors='replace')
    lines = out.splitlines()[:-1]
    files = [i.split('|')[0].strip() for i in lines]
    return files


def get_changed_lines(file):
    # Sample output:
    #  app/src/main/res/layout/activity_start.xml | 344 ++++++++++++++++-------------
    #  1 file changed, 195 insertions(+), 149 deletions(-)
    out = subprocess.check_output(['git', 'diff', '--stat', GITHUB_BASE_REF, '--', file]).decode(errors='replace')
    last_line = out.strip().splitlines()[-1]

    insertions = 0
    deletions = 0

    match = _insertions_regex.search(last_line)
    if match and match.group(1):
        insertions = int(match.group(1))

    match = _deletions_regex.search(last_line)
    if match and match.group(1):
        deletions = int(match.group(1))

    return insertions, deletions


def strip_code(text: str) -> str:
    return re.sub(r"[^\w{}\[\]]*", '', text)


def check_bloat_changes(file):
    """
    This check should be triggered by simple reformats.
    Both the old and the new file are read, stripped of everything but alphanumeric, turned to lowercase, then compared.
    If the differences between the two processed files aren't a lot, reject.
    """

    ins, dels = get_changed_lines(file)
    git_avg_changes = (ins + dels) // 2

    old_file = os.path.join(BASE_CHECKOUT, file)

    if not os.path.exists(old_file):
        # File introduced in pull request, can't diff
        return

    with open(file) as f:
        new = set([strip_code(i).lower() for i in f.readlines()])

    with open(old_file) as f:
        old = set([strip_code(i).lower() for i in f.readlines()])

    unchanged = new.intersection(old)
    changed = new.union(old).difference(unchanged)
    changes = len(changed)
    if git_avg_changes != 0:
        bloat_perc = round((git_avg_changes-changes)/git_avg_changes*100, 1)
    else:
        bloat_perc = 0

    if len(changed) * BLOAT_CHANGES_TOLERANCE_FACTOR < git_avg_changes:
        raise BloatedPullRequest(f"Lines changed: roughly {git_avg_changes}, actual code changes: roughly {changes}, bloat changes: {bloat_perc}%")


def check_translations(file):
    parent_dir = basename(dirname(file))

    if basename(file) != "strings.xml":
        # Not strings
        return

    if parent_dir == "values":
        # Main strings set, allowed to change
        return

    if not parent_dir.startswith("values-"):
        # Not strings
        return

    transl_lang = parent_dir.split('-')[1]
    raise BypassedWeblate(f"Translations for '{transl_lang}'. "
                          "Translations must be changed in Weblate: " + TRANSLATE_URL.format(lang=transl_lang))


checks = [check_bloat_changes, check_translations]


def main():
    if os.environ.get('GITHUB_EVENT_NAME', None) != 'pull_request':
        print("Not a pull request. Nothing to do.")
        exit()
    if os.environ.get('GITHUB_ACTOR', None) in IGNORED_USERS:
        print(f"Pull request is from @{os.environ['GITHUB_ACTOR']} and user is in ignore list. Nothing to do.")
        exit()
    if not os.path.exists(BASE_CHECKOUT):
        print(f"Pull request base checkout does not exist: {BASE_CHECKOUT}")
        exit(1)

    incidents = set()
    changed = get_changed_files()

    if len(changed) > MAX_FILES_CHANGED:
        print('Too many files!')
        print(f'  - In this pull request, you changed {len(changed)} files\n')
        incidents.add('too_many_files')

    for file in changed:
        if not os.path.exists(file):
            # File was deleted, ignore
            continue

        err_printed = False
        for check in checks:
            try:
                check(file)
            except Exception as exc:
                if not err_printed:
                    print(f"{file}:")
                    err_printed = True
                print(f"  - {str(exc)}")

                if isinstance(exc, BypassedWeblate):
                    incidents.add('bypass_weblate')
                elif isinstance(exc, BloatedPullRequest):
                    incidents.add('bloated')
                else:
                    incidents.add('unknown')
        if err_printed:
            print()

    if len(incidents) > 0:
        print()
        print("=" * 110)
        print("\nSome checks have failed. Here are some explanations:\n")
        for inc in incidents:
            if inc in INCIDENT_EXPLAINATIONS:
                print(INCIDENT_EXPLAINATIONS[inc], end='\n\n')

        print("=" * 110)
        print("\nNOTE: This is an automated check, keep that in mind. If you think your changes shouldn't have been "
              "flagged,\n"
              "you can ignore this and the maintainer will review them. False positives happen.\n")
        exit(2)
    else:
        print("All checks passed, good job!")


if __name__ == "__main__":
    main()
